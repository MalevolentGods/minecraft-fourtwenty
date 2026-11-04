#!/usr/bin/env python3
"""The Kush Suit's art: armour layers, item icons and models, and the four Re-Up Stations.

    python3 tools/generate_kush_suit.py

Everything is drawn here from scratch (no Minecraft texture is copied or recoloured). The suit is woven hemp green with gold trim; each
icon is a small flat drawing of its piece; each Re-Up Station is a hemp-and-metal pad with its tier's colour on the front panel.
"""
import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
HEMP = (58, 92, 44)
HEMP_HI = (92, 140, 64)
HEMP_LO = (36, 60, 30)
GOLD = (214, 172, 64)
GOLD_HI = (248, 222, 130)
OUTLINE = (20, 22, 26)
TIERS = {'puff': (96, 196, 72), 'toke': (60, 196, 180), 'rip': (168, 100, 232), 'blaze': (255, 140, 40)}


def weave(img, box, rng):
    """Hemp weave over a box: a two-tone basket pattern with a little noise."""
    x0, y0, x1, y1 = box
    px = img.load()
    for x in range(x0, x1):
        for y in range(y0, y1):
            base = HEMP_HI if (x // 2 + y // 2) % 2 == 0 else HEMP
            jitter = rng.randint(-8, 8)
            px[x, y] = tuple(max(0, min(255, c + jitter)) for c in base) + (255,)


def trim(img, box):
    x0, y0, x1, y1 = box
    d = ImageDraw.Draw(img)
    d.rectangle([x0, y0, x1 - 1, y1 - 1], outline=GOLD + (255,))


def armour_layers():
    rng = random.Random(420)
    # The humanoid armour layout (64x32): each part's faces, as boxes. Head, body, arms, legs.
    parts = [(0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32)]
    for layer, keep in ((1, parts), (2, [(16, 16, 40, 32), (0, 16, 16, 32)])):
        img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
        for box in keep:
            weave(img, box, rng)
            trim(img, box)
        # a gold chest emblem and a band at the waist
        if layer == 1:
            d = ImageDraw.Draw(img)
            d.rectangle([22, 22, 25, 25], fill=GOLD_HI + (255,))
        else:
            ImageDraw.Draw(img).line([16, 20, 39, 20], fill=GOLD + (255,))
        out = ASSETS / 'textures/models/armor'
        out.mkdir(parents=True, exist_ok=True)
        img.save(out / f'kush_layer_{layer}.png')


def icon(name, draw):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw(ImageDraw.Draw(img))
    # outline every filled pixel's empty neighbours
    px = img.load()
    filled = {(x, y) for x in range(16) for y in range(16) if px[x, y][3] > 0}
    for x in range(16):
        for y in range(16):
            if (x, y) not in filled and any((x + a, y + b) in filled for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = OUTLINE + (255,)
    img.save(ASSETS / 'textures/item' / f'{name}.png')
    (ASSETS / 'models/item' / f'{name}.json').write_text(json.dumps({'parent': 'item/generated', 'textures': {'layer0': f'fourtwenty:item/{name}'}}, indent=2) + '\n')


def helm(d):
    d.rectangle([3, 3, 12, 11], fill=HEMP + (255,))
    d.rectangle([4, 6, 11, 8], fill=(120, 200, 255, 255))   # the visor
    d.line([3, 3, 12, 3], fill=GOLD + (255,))


def plate(d):
    d.rectangle([3, 2, 12, 13], fill=HEMP + (255,))
    d.rectangle([5, 2, 10, 4], fill=(0, 0, 0, 0))
    d.rectangle([6, 7, 9, 10], fill=GOLD_HI + (255,))
    d.rectangle([1, 4, 2, 9], fill=(150, 150, 150, 255))     # the Puff Pack's tanks
    d.rectangle([13, 4, 14, 9], fill=(150, 150, 150, 255))


def greaves(d):
    d.rectangle([4, 2, 11, 5], fill=HEMP + (255,))
    d.rectangle([4, 5, 6, 13], fill=HEMP + (255,))
    d.rectangle([9, 5, 11, 13], fill=HEMP + (255,))
    d.line([4, 2, 11, 2], fill=GOLD + (255,))


def boots(d):
    d.rectangle([2, 6, 6, 12], fill=HEMP + (255,))
    d.rectangle([9, 6, 13, 12], fill=HEMP + (255,))
    d.rectangle([2, 12, 6, 13], fill=GOLD + (255,))
    d.rectangle([9, 12, 13, 13], fill=GOLD + (255,))


def blazer(d):
    d.rectangle([2, 7, 9, 11], fill=HEMP + (255,))
    d.rectangle([9, 8, 13, 10], fill=(150, 150, 150, 255))   # the barrel
    d.point([14, 9], fill=(255, 140, 40, 255))               # the glow
    d.line([2, 7, 9, 7], fill=GOLD + (255,))


def dab_cell(d):
    d.rectangle([5, 3, 10, 13], fill=(70, 74, 80, 255))
    d.rectangle([6, 5, 9, 11], fill=(96, 196, 72, 255))
    d.rectangle([7, 2, 8, 3], fill=GOLD + (255,))


def stations():
    rng = random.Random(42)
    for tier, colour in TIERS.items():
        name = f're_up_station_{tier}'
        side = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        weave(side, (0, 0, 16, 16), rng)
        d = ImageDraw.Draw(side)
        d.rectangle([0, 4, 15, 15], fill=(70, 74, 80, 255))
        d.rectangle([4, 7, 11, 12], fill=colour + (255,))
        d.line([0, 4, 15, 4], fill=GOLD + (255,))
        top = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        weave(top, (0, 0, 16, 16), rng)
        ImageDraw.Draw(top).rectangle([0, 0, 15, 15], outline=GOLD + (255,))
        ImageDraw.Draw(top).rectangle([6, 6, 9, 9], fill=colour + (255,))
        side.save(ASSETS / 'textures/block' / f'{name}_side.png')
        top.save(ASSETS / 'textures/block' / f'{name}_top.png')
        model = {'parent': 'block/block', 'textures': {'side': f'fourtwenty:block/{name}_side', 'top': f'fourtwenty:block/{name}_top',
                                                        'particle': f'fourtwenty:block/{name}_side'},
                 'elements': [{'from': [0, 0, 0], 'to': [16, 12, 16], 'faces': {
                     'north': {'uv': [0, 4, 16, 16], 'texture': '#side'}, 'south': {'uv': [0, 4, 16, 16], 'texture': '#side'},
                     'east': {'uv': [0, 4, 16, 16], 'texture': '#side'}, 'west': {'uv': [0, 4, 16, 16], 'texture': '#side'},
                     'up': {'uv': [0, 0, 16, 16], 'texture': '#top'}, 'down': {'uv': [0, 0, 16, 16], 'texture': '#side', 'cullface': 'down'}}}]}
        (ASSETS / 'models/block' / f'{name}.json').write_text(json.dumps(model, indent=2) + '\n')
        (ASSETS / 'models/item' / f'{name}.json').write_text(json.dumps({'parent': f'fourtwenty:block/{name}'}, indent=2) + '\n')
        variants = {}
        for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            for lit in ('false', 'true'):
                variants[f'facing={facing},lit={lit}'] = {'model': f'fourtwenty:block/{name}', 'y': y} if y else {'model': f'fourtwenty:block/{name}'}
        (ASSETS / 'blockstates' / f'{name}.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')


def main():
    armour_layers()
    for name, draw in (('hotbox_helm', helm), ('puff_plate', plate), ('couchlock_greaves', greaves), ('moon_boots', boots),
                       ('blazer', blazer), ('dab_cell', dab_cell)):
        icon(name, draw)
    stations()
    print('wrote the Kush Suit art')


if __name__ == '__main__':
    main()
