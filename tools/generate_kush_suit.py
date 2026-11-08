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


# The suit's painted sheet (the user's art, 2026-11-08): helmet front and sides, chest with both shoulders, two boots, the leggings. Each
# box is a panel of it, in the sheet's pixels; armour_from_sheet() fits each onto its faces of the armour layout.
SHEET = Path(__file__).resolve().parent / 'art/kush_suit_sheet.png'
SCALE = 4   # 256x128: four pixels to each of the layout's 64x32
CROPS = {
    'helm_front': (290, 15, 600, 268), 'helm_top': (330, 15, 560, 110), 'helm_right': (12, 28, 228, 258), 'helm_left': (657, 28, 874, 258),
    'chest': (300, 280, 590, 612), 'chest_top': (300, 282, 590, 340), 'chest_side': (290, 285, 335, 610),
    'arm': (612, 280, 870, 340), 'arm_out': (800, 300, 870, 515), 'arm_in': (615, 300, 700, 515), 'arm_low': (612, 450, 870, 520),
    'medallion': (698, 346, 802, 450),
    'boot_front': (140, 625, 300, 870), 'boot_outer': (50, 625, 140, 870), 'boot_inner': (300, 625, 400, 870),
    'leg_front': (1110, 275, 1340, 868), 'leg_outer': (920, 275, 1100, 868), 'leg_inner': (1590, 275, 1757, 868),
    'belt': (1200, 69, 1450, 300), 'belt_side': (1015, 69, 1110, 300),
}
DARK = (24, 26, 30, 255)


def armour_from_sheet():
    """Both armour layers cut from the sheet: layer 1 the helmet, chestplate (body and arms) and boots (the legs' lower half); layer 2
    the leggings (the legs, and the belt round the body's lower part). Faces nobody sees (undersides) are plain dark plate."""
    sheet = Image.open(SHEET).convert('RGBA')

    def put(img, crop, face, turn=0, flip=False):
        u0, v0, u1, v1 = face
        part = sheet.crop(CROPS[crop] if isinstance(crop, str) else crop)
        if turn:
            part = part.rotate(turn, expand=True)
        if flip:
            part = part.transpose(Image.FLIP_LEFT_RIGHT)
        w, h = (u1 - u0) * SCALE, (v1 - v0) * SCALE
        part = part.resize((w, h), Image.LANCZOS)
        # the sheet's see-through gaps between panels are plate here: a face is never a hole
        back = Image.new('RGBA', (w, h), DARK)
        img.paste(Image.alpha_composite(back, part), (u0 * SCALE, v0 * SCALE))

    def dark(img, face):
        u0, v0, u1, v1 = face
        img.paste(Image.new('RGBA', ((u1 - u0) * SCALE, (v1 - v0) * SCALE), DARK), (u0 * SCALE, v0 * SCALE))

    out = ASSETS / 'textures/models/armor'
    out.mkdir(parents=True, exist_ok=True)
    one = Image.new('RGBA', (64 * SCALE, 32 * SCALE), (0, 0, 0, 0))
    # helmet: top, (bottom), right, front, left, back
    put(one, 'helm_top', (8, 0, 16, 8))
    dark(one, (16, 0, 24, 8))
    put(one, 'helm_right', (0, 8, 8, 16))
    put(one, 'helm_front', (8, 8, 16, 16))
    put(one, 'helm_left', (16, 8, 24, 16))
    put(one, 'helm_right', (24, 8, 32, 16), flip=True)
    # chest: top, bottom, right, front, left, back
    put(one, 'chest_top', (20, 16, 28, 20))
    dark(one, (28, 16, 36, 20))
    put(one, 'chest_side', (16, 20, 20, 32))
    put(one, 'chest', (20, 20, 28, 32))
    put(one, 'chest_side', (28, 20, 32, 32), flip=True)
    put(one, 'chest', (32, 20, 40, 32), flip=True)
    # arms (the left mirrors the right): the shoulder's medallion at the top of the outer face, its lightning strip below; the strips
    # either side of the medallion, upright as they're painted, round the rest
    put(one, 'arm', (44, 16, 48, 20))
    dark(one, (48, 16, 52, 20))
    put(one, 'medallion', (40, 20, 44, 24))
    put(one, 'arm_out', (40, 24, 44, 32))
    put(one, 'arm_in', (44, 20, 48, 32))
    put(one, 'arm_out', (48, 20, 52, 32), flip=True)
    put(one, 'arm_in', (52, 20, 56, 32), flip=True)
    # boots: the legs' lower half (rows 26 to 32), a dark sole
    dark(one, (8, 16, 12, 20))
    put(one, 'boot_outer', (0, 26, 4, 32))
    put(one, 'boot_front', (4, 26, 8, 32))
    put(one, 'boot_inner', (8, 26, 12, 32))
    put(one, 'boot_front', (12, 26, 16, 32), flip=True)
    one.save(out / 'kush_layer_1.png')

    two = Image.new('RGBA', (64 * SCALE, 32 * SCALE), (0, 0, 0, 0))
    # leggings: the belt round the body's lower part (rows 24 to 32)
    put(two, 'belt_side', (16, 24, 20, 32))
    put(two, 'belt', (20, 24, 28, 32))
    put(two, 'belt_side', (28, 24, 32, 32), flip=True)
    put(two, 'belt', (32, 24, 40, 32), flip=True)
    # the legs: top, (bottom), outer, front, inner, back
    put(two, 'leg_front', (4, 16, 8, 20))
    dark(two, (8, 16, 12, 20))
    put(two, 'leg_outer', (0, 20, 4, 32))
    put(two, 'leg_front', (4, 20, 8, 32))
    put(two, 'leg_inner', (8, 20, 12, 32))
    put(two, 'leg_outer', (12, 20, 16, 32), flip=True)
    two.save(out / 'kush_layer_2.png')


def armour_layers():
    if SHEET.exists():
        armour_from_sheet()
        return
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
