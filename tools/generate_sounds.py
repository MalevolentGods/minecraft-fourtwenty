#!/usr/bin/env python3
"""Sound effects for the tech screens, the Trim Tray and burning Wicks, synthesized here so they can be regenerated: nothing sampled, nothing licensed.

    python3 tools/generate_sounds.py        (needs ffmpeg with libvorbis on the PATH)

Writes assets/fourtwenty/sounds/<name><n>.ogg (mono, as positional sounds must be) and sounds.json.
"""
import json
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
RATE = 44_100


def t(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def noise(rng, seconds):
    return rng.normal(0, 1, int(RATE * seconds))


def lowpass(x, cutoff):
    a = 1 / (1 + RATE / (2 * np.pi * cutoff))
    out = np.empty_like(x)
    y = 0.0
    for i, v in enumerate(x):
        y += a * (v - y)
        out[i] = y
    return out


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def mix(length, *parts):
    out = np.zeros(int(RATE * length))
    for start, sound in parts:
        i = int(RATE * start)
        n = min(len(sound), len(out) - i)
        out[i:i + n] += sound[:n]
    return out


def snip(rng):
    """Two blades closing: a bright tick and a short ring."""
    def blade(pitch):
        s = t(0.07)
        return highpass(noise(rng, 0.07), 3000) * np.exp(-s * 180) * 0.6 + np.sin(2 * np.pi * pitch * s) * np.exp(-s * 90) * 0.35
    return mix(0.2, (0, blade(rng.uniform(2800, 3400))), (0.07, blade(rng.uniform(3600, 4200))))


def burn_out(rng):
    """A wick letting go: a crack, then fizz."""
    s = t(1.2)
    fizz = highpass(noise(rng, 1.2), 2500) * np.exp(-s * 3.5) * 0.45
    crackle = np.where(rng.random(len(s)) > 0.9985, rng.uniform(-1, 1, len(s)), 0) * np.exp(-s * 2)
    crack = highpass(noise(rng, 0.03), 800) * np.exp(-t(0.03) * 120) * 0.9
    return mix(1.2, (0, fizz + crackle), (0, crack))


def overload(rng):
    """Mains hum and sizzle."""
    s = t(0.8)
    hum = (np.sin(2 * np.pi * 120 * s) + 0.5 * np.sin(2 * np.pi * 240 * s) + 0.25 * np.sign(np.sin(2 * np.pi * 360 * s))) * 0.14
    sizzle = highpass(noise(rng, 0.8), 3500) * (rng.random(len(s)) > 0.5) * 0.3
    return (hum + sizzle) * np.sin(np.pi * s / 0.8) ** 0.5


def switch(rng):
    """A heavy toggle."""
    s = t(0.16)
    return np.sin(2 * np.pi * 160 * s) * np.exp(-s * 40) * 0.6 + highpass(noise(rng, 0.16), 1800) * np.exp(-s * 160) * 0.5


SOUNDS = {'snip': (snip, 3), 'burn_out': (burn_out, 2), 'overload': (overload, 2), 'switch': (switch, 2)}


def write(path, samples):
    peak = np.max(np.abs(samples)) or 1
    pcm = (samples / peak * 0.9 * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / 'out.wav'
        with wave.open(str(wav), 'wb') as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(RATE)
            f.writeframes(pcm.tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', str(wav), '-c:a', 'libvorbis', '-q:a', '4', str(path)], check=True)


def main():
    out = ASSETS / 'sounds'
    out.mkdir(parents=True, exist_ok=True)
    book = {}
    for name, (make, count) in SOUNDS.items():
        for i in range(1, count + 1):
            write(out / f'{name}{i}.ogg', make(np.random.default_rng(420 + i)))
        book[name] = {'subtitle': f'subtitles.fourtwenty.{name}', 'sounds': [f'fourtwenty:{name}{i}' for i in range(1, count + 1)]}
    (ASSETS / 'sounds.json').write_text(json.dumps(book, indent=2) + '\n')
    print('ok')


if __name__ == '__main__':
    main()
