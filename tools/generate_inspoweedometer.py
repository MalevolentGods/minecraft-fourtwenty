#!/usr/bin/env python3
"""The Inspoweedometer's icon and item model: a hand-held hemp-green meter with a gold rim, a pale dial and a needle.

    python3 tools/generate_inspoweedometer.py

Drawn here from scratch; no Minecraft texture is copied.
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
HEMP, HEMP_LO = (58, 92, 44), (36, 60, 30)
GOLD, DIAL = (214, 172, 64), (248, 222, 130)
INK, RED = (20, 22, 26), (140, 47, 47)


def main():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 1, 12, 14], fill=HEMP + (255,), outline=INK + (255,))      # the body
    d.rectangle([4, 2, 11, 8], fill=DIAL + (255,), outline=GOLD + (255,))       # the dial, gold rim
    d.line([5, 7, 9, 4], fill=RED + (255,))                                       # the needle
    d.point([7, 7], fill=INK + (255,))                                           # its pivot
    d.rectangle([5, 10, 6, 11], fill=GOLD + (255,))                               # two terminals
    d.rectangle([9, 10, 10, 11], fill=HEMP_LO + (255,))
    d.line([4, 13, 11, 13], fill=HEMP_LO + (255,))
    img.save(ASSETS / 'textures/item/inspoweedometer.png')
    (ASSETS / 'models/item/inspoweedometer.json').write_text(
        json.dumps({'parent': 'item/generated', 'textures': {'layer0': 'fourtwenty:item/inspoweedometer'}}, indent=2) + '\n')
    print('wrote the Inspoweedometer')


if __name__ == '__main__':
    main()
