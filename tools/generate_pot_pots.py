#!/usr/bin/env python3
"""Pot Pots: terpene-concrete plant pots in the 64 colours (PotPotBlock), and with a weed plant in them,
Potted Pot Pots (PottedPotPotBlock).

Every colour gets a pot of its own design, picked by its code's three bases (lav, lim, myr, pin):
the first picks the body (a tub, a taper, a belly, an urn), the second the rim (none, a lip, a roll,
a collar) and the third the pattern on it (stripes, dots, a zigzag, ribs), all in the colour's
concrete with a contrasting shade for the pattern. 4 x 4 x 4 = 64, so no two pots are alike. The soil
sits at 6/16 in every pot, so the eight plant stages (the crop's own textures, scaled to the pot) are
shared models, added to the pot by the potted blockstate.

Writes per colour: the pot's side texture, its model, blockstates for both blocks, item models (the
potted one shows a young plant), loot, the recipe (3 of the concrete in a V round a block of dirt),
names and the pickaxe tag; and the shared soil texture and plant models.
    python tools/generate_pot_pots.py [--preview out.png]
"""
import collections
import json
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_palette_blocks import ASSETS, DATA, RES, TERP, concrete_texture, lum, rgb, vary, write   # noqa: E402

S = 64                   # texture size (4 px per model pixel)
SOIL_Y = 6               # the soil's height in every pot
# body: [(y0, y1, width)] stacked, and the top band's width (walls one pixel thick, SOIL_Y to 8)
BODIES = {
    'lav': ('tub', [(0, 6, 10)], 10),
    'lim': ('taper', [(0, 2, 8), (2, 4, 9), (4, 6, 10)], 11),
    'myr': ('belly', [(0, 1, 8), (1, 5, 12), (5, 6, 10)], 9),
    'pin': ('urn', [(0, 1, 9), (1, 2, 6), (2, 6, 11)], 11),
}
RIMS = {'lav': 'none', 'lim': 'lip', 'myr': 'roll', 'pin': 'collar'}
PATTERNS = {'lav': 'stripes', 'lim': 'dots', 'myr': 'zigzag', 'pin': 'ribs'}
TOP = 8                  # the band's top


def box(x0, y0, z0, x1, y1, z1, faces=('north', 'south', 'east', 'west', 'up', 'down'), tex='#side'):
    return {'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': {f: {'texture': tex} for f in faces}}


def ring(width, y0, y1, t=1.0):
    """Four walls round a square of the given outer width, centred, t thick."""
    a, b = 8 - width / 2, 8 + width / 2
    return [box(a, y0, a, b, y1, a + t), box(a, y0, b - t, b, y1, b),
            box(a, y0, a + t, a + t, y1, b - t), box(b - t, y0, a + t, b, y1, b - t)]


def pot_elements(code):
    body_base, rim_base, _ = code.split('_')
    _, body, band = BODIES[body_base]
    els = []
    for i, (y0, y1, w) in enumerate(body):
        faces = ['north', 'south', 'east', 'west', 'up'] + (['down'] if i == 0 else [])
        a, b = 8 - w / 2, 8 + w / 2
        els.append(box(a, y0, a, b, y1, b, faces))
    els += ring(band, SOIL_Y, TOP)
    rim = RIMS[rim_base]
    if rim == 'lip':
        els += ring(band + 2, TOP - 1, TOP)
    elif rim == 'roll':
        els += ring(band + 2, TOP - 1.5, TOP + 0.5)
    elif rim == 'collar':
        els += ring(band, TOP, TOP + 1.5)
    s = band / 2 - 1                                   # the soil, inside the band, just above the body's top
    els.append({'from': [8 - s, SOIL_Y, 8 - s], 'to': [8 + s, SOIL_Y + 0.05, 8 + s], 'faces': {'up': {'texture': '#soil'}}})
    return els


def plant_elements(tex='#plant'):
    """The crop's stage texture as a cross, scaled to 12 wide and tall, standing in the soil."""
    face = {'texture': tex, 'uv': [0, 0, 16, 16]}
    out = []
    for angle in (45, -45):
        out.append({'from': [2, SOIL_Y, 8], 'to': [14, SOIL_Y + 12, 8], 'shade': False,
                    'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': angle, 'rescale': True},
                    'faces': {'north': dict(face), 'south': dict(face)}})
    return out


def side_texture(c, code, seed):
    img = concrete_texture(c, seed)
    px = img.load()
    accent = vary(c, -0.24) if lum(c) > 0.32 else vary(c, 0.4)
    pattern = PATTERNS[code.split('_')[2]]
    for y in range(S):
        for x in range(S):
            on = False
            if pattern == 'stripes':
                on = y % 16 < 4
            elif pattern == 'dots':
                cx = (x + (8 if (y // 16) % 2 else 0)) % 16 - 8
                cy = y % 16 - 8
                on = cx * cx + cy * cy <= 9
            elif pattern == 'zigzag':
                tri = abs((x % 16) - 8)                      # 0..8
                on = abs(y - (40 + tri)) < 3
            elif pattern == 'ribs':
                on = x % 12 < 3
            if on:
                px[x, y] = accent
    return img


def soil_texture():
    rng = random.Random(420)
    img = Image.new('RGB', (S, S))
    px = img.load()
    for y in range(S):
        for x in range(S):
            r = rng.random()
            base = (74, 52, 34) if r > 0.12 else (58, 40, 26) if r > 0.04 else (104, 82, 58)
            px[x, y] = vary(base, rng.uniform(-0.06, 0.06))
    return img


def generate(preview=None):
    names = collections.OrderedDict()
    tex = ASSETS / 'textures/block/pot_pot'
    tex.mkdir(parents=True, exist_ok=True)
    soil_texture().save(tex / 'soil.png')
    for n in range(8):
        write(ASSETS / f'models/block/potted_weed_stage{n}.json', {
            'render_type': 'minecraft:cutout', 'ambientocclusion': False,
            'textures': {'plant': f'fourtwenty:block/weed_crop_stage{n}', 'particle': f'fourtwenty:block/weed_crop_stage{n}'},
            'elements': plant_elements()})
    colors = [(c['code'], c['name'], c['hex']) for c in TERP['colors']]
    designs = set()
    sheet = []
    for i, (code, name, hexc) in enumerate(colors):
        c = rgb(hexc)
        b, r, p = code.split('_')
        designs.add((BODIES[b][0], RIMS[r], PATTERNS[p]))
        img = side_texture(c, code, 9000 + i)
        img.save(tex / f'{code}.png')
        sheet.append(img)
        pot, potted = f'pot_pot_{code}', f'potted_pot_pot_{code}'
        textures = {'side': f'fourtwenty:block/pot_pot/{code}', 'soil': 'fourtwenty:block/pot_pot/soil',
                    'particle': f'fourtwenty:block/pot_pot/{code}'}
        # No ambient occlusion (as vanilla's flower pot): the potted blockstate takes it from the pot, its first
        # part, and with it on the plant was shaded dark as if in a corner.
        write(ASSETS / f'models/block/{pot}.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
                                                   'textures': textures, 'elements': pot_elements(code)})
        write(ASSETS / f'blockstates/{pot}.json', {'variants': {'': {'model': f'fourtwenty:block/{pot}'}}})
        write(ASSETS / f'blockstates/{potted}.json', {'multipart': [{'apply': {'model': f'fourtwenty:block/{pot}'}}] + [
            {'when': {'age': str(n)}, 'apply': {'model': f'fourtwenty:block/potted_weed_stage{n}'}} for n in range(8)]})
        write(ASSETS / f'models/item/{pot}.json', {'parent': f'fourtwenty:block/{pot}'})
        write(ASSETS / f'models/item/{potted}.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
                                                      'textures': dict(textures, plant='fourtwenty:block/weed_crop_stage3'),
                                                      'elements': pot_elements(code) + plant_elements()})
        for block in (pot, potted):
            write(DATA / f'loot_table/blocks/{block}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
                {'type': 'minecraft:item', 'name': f'fourtwenty:{block}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        write(DATA / f'recipe/{pot}.json', {'type': 'minecraft:crafting_shaped', 'category': 'decorations', 'pattern': ['CDC', ' C '],
                                           'key': {'C': {'item': f'fourtwenty:concrete_{code}'}, 'D': {'item': 'minecraft:dirt'}},
                                           'result': {'id': f'fourtwenty:{pot}', 'count': 1}})
        names[pot] = f'{name} Pot Pot'
        names[potted] = f'{name} Potted Pot Pot'
    assert len(designs) == 64, f'only {len(designs)} different designs'
    pick = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'
    current = json.loads(pick.read_text(encoding='utf-8'))
    current['values'] = sorted(set(current['values']) | {f'fourtwenty:{k}' for k in names})
    write(pick, current)
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    for k, v in names.items():
        data[f'block.fourtwenty.{k}'] = v
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    if preview:
        out = Image.new('RGB', (S * 8, S * 8))
        for i, img in enumerate(sheet):
            out.paste(img, ((i % 8) * S, (i // 8) * S))
        out.save(preview)
    print(f'{len(colors)} pot pots, {len(designs)} designs')


if __name__ == '__main__':
    args = sys.argv[1:]
    generate(args[args.index('--preview') + 1] if '--preview' in args else None)
