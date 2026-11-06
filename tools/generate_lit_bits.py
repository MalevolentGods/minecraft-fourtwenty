#!/usr/bin/env python3
"""Art and models for Lit Bits, the Weedweeno, the Sticky Finger and Bit Chits.

    python3 tools/generate_lit_bits.py

Everything is drawn here from scratch. A Lit Bit is a glass shell in a dark frame; its glowing core is drawn by client/LitBitRenderer
from lit_bit_core.png, in the lamp's colour. The Weedweeno is a little circuit board on a hemp base.
"""
import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
FRAME, FRAME_HI = (34, 36, 42), (70, 74, 84)
GOLD = (214, 172, 64)
BOARD, TRACE, CHIP = (28, 92, 52), (190, 160, 70), (24, 24, 28)
HEMP, HEMP_HI = (58, 92, 44), (92, 140, 64)
INK = (20, 22, 26)


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def lit_bit():
    shell = Image.new('RGBA', (16, 16), (200, 220, 230, 46))   # faint glass
    d = ImageDraw.Draw(shell)
    d.rectangle([0, 0, 15, 15], outline=FRAME + (255,))
    d.rectangle([1, 1, 14, 14], outline=FRAME_HI + (255,))
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        d.point([x, y], fill=GOLD + (255,))
    d.line([3, 2, 6, 2], fill=(255, 255, 255, 110))                # a glint
    save(shell, ASSETS / 'textures/block/lit_bit_shell.png')
    core = Image.new('RGBA', (16, 16), (255, 255, 255, 255))
    dc = ImageDraw.Draw(core)
    for i in range(8):
        shade = 255 - i * 6
        dc.rectangle([i, i, 15 - i, 15 - i], outline=(shade, shade, shade, 255))
    save(core, ASSETS / 'textures/block/lit_bit_core.png')
    faces = {f: {'uv': [0, 0, 16, 16], 'texture': '#shell'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}
    write(ASSETS / 'models/block/lit_bit.json', {
        'parent': 'block/block', 'render_type': 'minecraft:translucent',
        'textures': {'shell': 'fourtwenty:block/lit_bit_shell', 'particle': 'fourtwenty:block/lit_bit_shell'},
        'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces}]})
    write(ASSETS / 'models/item/lit_bit.json', {'parent': 'fourtwenty:block/lit_bit'})


def weedweeno():
    rng = random.Random(420)
    top = Image.new('RGBA', (16, 16), BOARD + (255,))
    d = ImageDraw.Draw(top)
    for y in (3, 6, 10, 13):
        d.line([1, y, 14, y], fill=TRACE + (255,))
    for x in (4, 11):
        d.line([x, 1, x, 14], fill=TRACE + (255,))
    d.rectangle([5, 5, 10, 10], fill=CHIP + (255,))                 # the chip
    for x in range(5, 11, 2):
        d.point([x, 4], fill=GOLD + (255,))
        d.point([x, 11], fill=GOLD + (255,))
    d.point([7, 7], fill=(123, 255, 90, 255))                       # its power light
    for _ in range(6):
        d.point([rng.randint(1, 14), rng.randint(1, 14)], fill=(150, 150, 150, 255))
    d.rectangle([0, 0, 15, 15], outline=GOLD + (255,))
    save(top, ASSETS / 'textures/block/weedweeno_top.png')
    side = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    ds = ImageDraw.Draw(side)
    ds.rectangle([0, 10, 15, 15], fill=HEMP + (255,))
    ds.line([0, 10, 15, 10], fill=BOARD + (255,))
    ds.line([0, 11, 15, 11], fill=HEMP_HI + (255,))
    for x in range(1, 16, 3):
        ds.point([x, 13], fill=GOLD + (255,))
    save(side, ASSETS / 'textures/block/weedweeno_side.png')
    faces = {f: {'uv': [1, 10, 15, 16], 'texture': '#side'} for f in ('north', 'south', 'east', 'west')}
    faces['up'] = {'uv': [1, 1, 15, 15], 'texture': '#top'}
    faces['down'] = {'uv': [1, 1, 15, 15], 'texture': '#side', 'cullface': 'down'}
    write(ASSETS / 'models/block/weedweeno.json', {
        'parent': 'block/block', 'textures': {'top': 'fourtwenty:block/weedweeno_top', 'side': 'fourtwenty:block/weedweeno_side',
                                              'particle': 'fourtwenty:block/weedweeno_top'},
        'elements': [{'from': [1, 0, 1], 'to': [15, 6, 15], 'faces': faces}]})
    write(ASSETS / 'models/item/weedweeno.json', {'parent': 'fourtwenty:block/weedweeno'})


def blockstates():
    for name in ('lit_bit', 'weedweeno'):
        parts = [{'when': {'facing': f}, 'apply': {'model': f'fourtwenty:block/{name}', **({'y': y} if y else {})}}
                 for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))]
        write(ASSETS / f'blockstates/{name}.json', {'multipart': parts})


def icon(name, draw):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw(ImageDraw.Draw(img))
    px = img.load()
    filled = {(x, y) for x in range(16) for y in range(16) if px[x, y][3] > 0}
    for x in range(16):
        for y in range(16):
            if (x, y) not in filled and any((x + a, y + b) in filled for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = INK + (255,)
    save(img, ASSETS / f'textures/item/{name}.png')
    write(ASSETS / f'models/item/{name}.json', {'parent': 'item/generated', 'textures': {'layer0': f'fourtwenty:item/{name}'}})


def finger(d):
    glove = (120, 200, 90, 255)
    d.rectangle([3, 8, 11, 13], fill=glove)       # the palm
    d.rectangle([9, 2, 11, 8], fill=glove)        # the pointing finger
    d.rectangle([2, 7, 3, 10], fill=glove)        # the thumb
    d.rectangle([9, 1, 11, 2], fill=(240, 230, 120, 255))   # the sticky tip
    d.line([4, 13, 10, 13], fill=GOLD + (255,))


def chit(d):
    d.rectangle([3, 2, 12, 13], fill=(236, 228, 206, 255))  # the slip
    for y in (4, 6, 8, 10):
        d.line([5, y, 10 if y != 8 else 8, y], fill=(60, 120, 70, 255))
    d.rectangle([3, 2, 12, 3], fill=(123, 255, 90, 255))


def main():
    lit_bit()
    weedweeno()
    blockstates()
    icon('sticky_finger', finger)
    icon('bit_chit', chit)
    print('wrote the Lit Bit art')


if __name__ == '__main__':
    main()
