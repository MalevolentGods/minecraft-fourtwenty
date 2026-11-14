"""Beat maps and VU-bar levels for the Midnight Machine album (spec 014 T020).

    python tools/beatmap_album.py [album dir] [clicks out dir]

For each "<N> - <Title>.wav" it works out, with numpy and scipy only:
- **onset strength:** spectral flux of a log-magnitude STFT;
- **tempo:** autocorrelation of the onset envelope, weighted toward 120 BPM;
- **beats:** dynamic-programming beat tracking (Ellis 2007), tempo-locked but free to follow the music;
- **accents:** the beats whose onset strength is in the top 12% of the track (the big hits she spins on);
- **VU bands:** six log-spaced frequency bands from 40 Hz to 12 kHz, every 50 ms, as 0-255 levels normalised per
  track, with a VU-meter release so bars fall smoothly.

Writes bpm, beats_ms and accents_ms into assets/fourtwenty/midnight_machine/album.json, and the bands to
assets/fourtwenty/midnight_machine/trackN.bands (6 bytes a frame, frame k = k*50 ms). For checking by ear it writes
<clicks dir>/trackN_clicks.wav: the track with a click on every beat (a higher one on accents).
tools/beatmap_overrides.json may set {"trackN": {"bpm": 124.0, "offset_ms": 35}} for a track the tracker gets wrong.
"""
import json
import os
import re
import sys
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.ndimage import maximum_filter1d

ROOT = Path(__file__).resolve().parents[1]
ALBUM = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r'C:\Users\Ignus\OneDrive\Desktop\ATLemental - Midnight Machine')
CLICKS = Path(sys.argv[2]) if len(sys.argv) > 2 else Path(os.environ.get('TEMP', '.')) / 'mm_clicks'
OUT = ROOT / 'src/main/resources/assets/fourtwenty/midnight_machine'
OVERRIDES = ROOT / 'tools/beatmap_overrides.json'

N_FFT, HOP = 2048, 512
BAND_EDGES = np.geomspace(40, 12000, 7)
BAND_MS = 50
ACCENT_SHARE = 0.12


def load_mono(path):
    sr, x = wavfile.read(path)
    x = x.astype(np.float32)
    if x.ndim > 1:
        x = x.mean(axis=1)
    peak = np.max(np.abs(x)) or 1.0
    return sr, x / peak


def stft_mag(x):
    window = np.hanning(N_FFT).astype(np.float32)
    frames = 1 + (len(x) - N_FFT) // HOP
    out = np.empty((frames, N_FFT // 2 + 1), dtype=np.float32)
    step = 2048
    for i in range(0, frames, step):
        idx = np.arange(i, min(frames, i + step))[:, None] * HOP + np.arange(N_FFT)[None, :]
        out[i:i + len(idx)] = np.abs(np.fft.rfft(x[idx] * window, axis=1))
    return out


def onset_envelope(mag):
    logm = np.log1p(100 * mag)
    flux = np.maximum(0, np.diff(logm, axis=0)).sum(axis=1)
    flux = np.concatenate([[0], flux])
    flux -= np.convolve(flux, np.ones(16) / 16, mode='same')   # remove the slow trend
    flux = np.maximum(flux, 0)
    return flux / (flux.std() or 1)


def tempo(env, fps, override=None):
    if override:
        return override
    env = env - env.mean()
    ac = np.correlate(env, env, mode='full')[len(env) - 1:]
    lags = np.arange(1, len(ac))
    bpm = 60 * fps / lags
    keep = (bpm >= 60) & (bpm <= 180)
    weight = np.exp(-0.5 * (np.log2(bpm[keep] / 120) / 0.9) ** 2)
    scores = ac[1:][keep] * weight
    return float(bpm[keep][np.argmax(scores)])


def track_beats(env, fps, bpm, tightness=100.0):
    period = 60 * fps / bpm
    n = len(env)
    score = env.copy()
    back = np.full(n, -1)
    lo, hi = int(round(period / 2)), int(round(2 * period))
    offsets = np.arange(-hi, -lo + 1)
    penalty = -tightness * np.log(-offsets / period) ** 2
    for t in range(hi, n):
        cand = score[t + offsets] + penalty
        k = int(np.argmax(cand))
        score[t] = env[t] + cand[k]
        back[t] = t + offsets[k]
    # end on the best-scoring frame in the last period, walk back
    t = n - hi + int(np.argmax(score[n - hi:]))
    beats = []
    while t >= 0:
        beats.append(t)
        t = back[t]
    return np.array(beats[::-1])


def bands(mag, sr):
    freqs = np.fft.rfftfreq(N_FFT, 1 / sr)
    frame_ms = HOP * 1000 / sr
    per = BAND_MS / frame_ms
    count = int(len(mag) / per)
    levels = np.zeros((count, 6), dtype=np.float32)
    for b in range(6):
        sel = (freqs >= BAND_EDGES[b]) & (freqs < BAND_EDGES[b + 1])
        energy = mag[:, sel].mean(axis=1)
        for k in range(count):
            a, z = int(k * per), max(int(k * per) + 1, int((k + 1) * per))
            levels[k, b] = energy[a:z].mean()
    db = 20 * np.log10(levels + 1e-6)
    out = np.zeros_like(db)
    for b in range(6):   # normalise each band: its quiet floor to 0, its loud peaks to 1
        lo, hi = np.percentile(db[:, b], 5), np.percentile(db[:, b], 99.5)
        out[:, b] = np.clip((db[:, b] - lo) / max(1e-6, hi - lo), 0, 1)
    for k in range(1, len(out)):   # a VU meter: rises at once, falls back gently
        out[k] = np.maximum(out[k], out[k - 1] * 0.82)
    return (out * 255).round().astype(np.uint8)


def clicks(x, sr, beats_ms, accents_ms):
    y = 0.5 * x.copy()
    t = np.arange(int(0.03 * sr)) / sr
    for ms, freq in [(m, 1000) for m in beats_ms] + [(m, 2400) for m in accents_ms]:
        i = int(ms * sr / 1000)
        burst = 0.6 * np.sin(2 * np.pi * freq * t) * np.exp(-t * 120)
        y[i:i + len(burst)] += burst[:max(0, len(y) - i)]
    return (np.clip(y, -1, 1) * 32767).astype(np.int16)


def main():
    overrides = json.loads(OVERRIDES.read_text(encoding='utf-8')) if OVERRIDES.exists() else {}
    manifest_path = OUT / 'album.json'
    manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
    by_id = {t['id']: t for t in manifest['tracks']}
    CLICKS.mkdir(parents=True, exist_ok=True)
    for wav in sorted(ALBUM.glob('*.wav'), key=lambda p: int(re.match(r'(\d+)', p.name).group(1))):
        n = int(re.match(r'(\d+)', wav.name).group(1))
        tid = f'track{n}'
        sr, x = load_mono(wav)
        mag = stft_mag(x)
        env = onset_envelope(mag)
        fps = sr / HOP
        ov = overrides.get(tid, {})
        bpm = tempo(env, fps, ov.get('bpm'))
        frames = track_beats(env, fps, bpm)
        offset = ov.get('offset_ms', 0)
        beats_ms = [int(round(f * 1000 / fps)) + offset for f in frames]
        strength = maximum_filter1d(env, 5)[frames]
        cut = np.quantile(strength, 1 - ACCENT_SHARE) if len(strength) else 0
        accents_ms = [b for b, s in zip(beats_ms, strength) if s >= cut]
        levels = bands(mag, sr)
        (OUT / f'{tid}.bands').write_bytes(levels.tobytes())
        by_id[tid].update({'bpm': round(bpm, 2), 'beats_ms': beats_ms, 'accents_ms': accents_ms, 'bands_ms': BAND_MS})
        wavfile.write(CLICKS / f'{tid}_clicks.wav', sr, clicks(x, sr, beats_ms, accents_ms))
        ivals = np.diff(beats_ms)
        print(f'{tid}: {bpm:6.1f} BPM, {len(beats_ms)} beats (gap {np.median(ivals):.0f} ms median, {np.std(ivals):.0f} ms spread), '
              f'{len(accents_ms)} accents, {len(levels)} band frames')
    manifest_path.write_text(json.dumps(manifest, indent=1) + '\n', encoding='utf-8')
    print('clicks in', CLICKS)


if __name__ == '__main__':
    main()
