"""Convert the Midnight Machine album for the mod (spec 014 T001).

    python tools/convert_album.py [album dir]

Each "<N> - <Title>.wav" in the album folder becomes assets/fourtwenty/sounds/midnight_machine/trackN.ogg: mono (so it
plays from the Builder's position and fades with distance), 44.1 kHz, Vorbis q4 (ffmpeg's libvorbis). Writes
assets/fourtwenty/midnight_machine/album.json with each track's title and exact length, keeping any beat maps already
in it. The WAVs never enter the repo. Fails if the album would add more than 20 MB.
"""
import json
import os
import re
import subprocess
import sys
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ALBUM = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r'C:\Users\Ignus\OneDrive\Desktop\ATLemental - Midnight Machine')
SOUNDS = ROOT / 'src/main/resources/assets/fourtwenty/sounds/midnight_machine'
MANIFEST = ROOT / 'src/main/resources/assets/fourtwenty/midnight_machine/album.json'
LIMIT = 20 * 1024 * 1024


def main():
    SOUNDS.mkdir(parents=True, exist_ok=True)
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    old = json.loads(MANIFEST.read_text(encoding='utf-8')) if MANIFEST.exists() else {'tracks': []}
    kept = {t['id']: t for t in old.get('tracks', [])}
    tracks = []
    total = 0
    for wav in sorted(ALBUM.glob('*.wav'), key=lambda p: int(re.match(r'(\d+)', p.name).group(1))):
        m = re.match(r'(\d+)\s*-\s*(.+)\.wav$', wav.name)
        n, title = int(m.group(1)), m.group(2).strip()
        out = SOUNDS / f'track{n}.ogg'
        subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-i', str(wav), '-ac', '1', '-ar', '44100',
                        '-c:a', 'libvorbis', '-q:a', '4', str(out)], check=True)
        with wave.open(str(wav)) as w:
            length_ms = round(w.getnframes() * 1000 / w.getframerate())
        size = out.stat().st_size
        total += size
        track = dict(kept.get(f'track{n}', {}))
        track.update({'id': f'track{n}', 'title': title, 'length_ms': length_ms})
        tracks.append(track)
        print(f'track{n}: {title:30} {length_ms / 60000:5.2f} min  {size / 1e6:5.2f} MB')
    MANIFEST.write_text(json.dumps({'title': 'Midnight Machine', 'artist': 'ATLemental', 'tracks': tracks}, indent=2) + '\n', encoding='utf-8')
    print(f'total {total / 1e6:.2f} MB in {len(tracks)} tracks')
    if total > LIMIT:
        sys.exit(f'too big: {total / 1e6:.1f} MB > 20 MB')


if __name__ == '__main__':
    main()
