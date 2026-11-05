#!/usr/bin/env python3
"""The Pave Pen and Pave Pen Pro icons and item models.

    python3 tools/generate_pave_pen.py

A vape pen lying corner to corner: a cobble-path roller tip at the bottom left, a dark body with a glowing window, metal rings,
a mouthpiece and a wisp of smoke at the top right. The Pro swaps the glow for emerald and the rings for gold.
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
OUTLINE = (20, 22, 26, 255)

PLAIN = {'glow': (96, 196, 72), 'glow_hi': (176, 240, 150), 'ring': (201, 201, 201), 'ring_hi': (240, 240, 240)}
PRO = {'glow': (47, 208, 122), 'glow_hi': (160, 248, 200), 'ring': (232, 176, 74), 'ring_hi': (255, 226, 150)}

# Along the pen from the tip (0) to the mouthpiece (11): what each step is made of.
PARTS = ['tip', 'tip', 'tip', 'ring', 'body', 'body', 'glow', 'glow', 'body', 'ring', 'mouth', 'mouth']


def colour(part, upper, c):
    """The colour of one pixel: the upper pixel of each step catches the light."""
    if part == 'tip':
        return (176, 176, 170) if upper else (118, 118, 112)
    if part == 'ring':
        return c['ring_hi'] if upper else c['ring']
    if part == 'glow':
        return c['glow_hi'] if upper else c['glow']
    if part == 'mouth':
        return (227, 230, 223) if upper else (150, 154, 150)
    return (78, 82, 90) if upper else (52, 55, 60)


def draw(c):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    filled = set()
    for i, part in enumerate(PARTS):
        x, y = 2 + i, 13 - i
        for dx, dy, upper in ((0, 0, False), (1, 0, False), (0, -1, True), (1, -1, True)):
            p = (x + dx, y + dy)
            if 0 <= p[0] < 16 and 0 <= p[1] < 16:
                px[p] = colour(part, upper, c) + (255,)
                filled.add(p)
    # A speck of grit on the roller.
    for p in ((3, 12), (2, 13)):
        px[p] = (92, 92, 86, 255)
    for x in range(16):
        for y in range(16):
            if (x, y) in filled:
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = OUTLINE
    # Smoke off the mouthpiece.
    for (x, y), a in (((15, 1), 150), ((14, 0), 110), ((15, 0), 70)):
        if (x, y) not in filled and px[x, y][3] == 0:
            px[x, y] = (210, 218, 210, a)
    return img


def model(name):
    return {'parent': 'item/handheld', 'textures': {'layer0': f'fourtwenty:item/{name}'}}


def main():
    for name, c in (('pave_pen', PLAIN), ('pave_pen_pro', PRO)):
        draw(c).save(ASSETS / 'textures/item' / f'{name}.png')
        (ASSETS / 'models/item' / f'{name}.json').write_text(json.dumps(model(name), indent=2) + '\n', encoding='utf-8')
        print('wrote', name)


if __name__ == '__main__':
    main()
