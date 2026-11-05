#!/usr/bin/env python3
"""The + and − terminal studs on powered blocks: a red stud marked + and a black stud marked −, one model per side of a block facing north.

    python3 tools/generate_terminals.py

client/PortModel draws them on each block's terminal faces (Terminal.front and Terminals), turned with the block. Drawn here from scratch.
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
SIGNS = {'plus': ((196, 40, 40), True), 'minus': ((28, 28, 32), False)}
WHITE = (240, 240, 236)
RIM = (214, 172, 64)


def texture(sign):
    colour, vertical = SIGNS[sign]
    img = Image.new('RGBA', (16, 16), RIM + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, 14, 14], fill=colour + (255,))
    d.rectangle([3, 7, 12, 8], fill=WHITE + (255,))           # the bar of the sign
    if vertical:
        d.rectangle([7, 3, 8, 12], fill=WHITE + (255,))       # and the + upright
    out = ASSETS / 'textures/block/port'
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / f'terminal_{sign}.png')


# the stud on each side of a block facing north: a 4 x 4 boss, 1 pixel proud of the block's face, at the face's middle
STUDS = {
    'north': ([6, 6, 0], [10, 10, 1], 'north'),
    'south': ([6, 6, 15], [10, 10, 16], 'south'),
    'west': ([0, 6, 6], [1, 10, 10], 'west'),
    'east': ([15, 6, 6], [16, 10, 10], 'east'),
}


def model(sign, side):
    frm, to, outer = STUDS[side]
    faces = {}
    for face in ('north', 'south', 'east', 'west', 'up', 'down'):
        faces[face] = {'uv': [0, 0, 16, 16] if face == outer else [0, 0, 1, 1], 'texture': '#mark'}
    out = ASSETS / 'models/block/port'
    out.mkdir(parents=True, exist_ok=True)
    (out / f'terminal_{sign}_{side}.json').write_text(json.dumps({
        'parent': 'block/block',
        'textures': {'mark': f'fourtwenty:block/port/terminal_{sign}', 'particle': f'fourtwenty:block/port/terminal_{sign}'},
        'elements': [{'from': frm, 'to': to, 'faces': faces}]}, indent=2) + '\n')


def main():
    for sign in SIGNS:
        texture(sign)
        for side in STUDS:
            model(sign, side)
    print('wrote the terminal studs')


if __name__ == '__main__':
    main()
