#!/usr/bin/env python3
"""The Terpinator's model, textures (64x64), blockstate, loot, recipe and tag, plus the English
text for its screen, guide and toast.

A dark walnut cabinet with a brass deck; on its front, the three numbered blend ports and a
pressure gauge; on top, a glass still holding amber terpene under a brass dome, with a copper
condenser coil down the side. While it works (LIT) the liquid glows.

    python3 tools/generate_terpinator.py [--preview DIR]
"""
import argparse
import collections
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'
TEX = ASSETS / 'textures/block/terpinator'
S = 64


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, f):
    return tuple(clamp(v * f) for v in c[:3]) + tuple(c[3:])


# ---- textures --------------------------------------------------------------------------------

WALNUT = (74, 50, 34)
BRASS = (190, 150, 70)
COPPER = (184, 104, 64)
AMBER = (214, 140, 34)


def walnut(rng, front=False):
    img = Image.new('RGBA', (S, S))
    px = img.load()
    for x in range(S):
        board = x // 16
        tone = 1 + (board % 2) * 0.05 - 0.03
        phase = rng.uniform(0, 6.28)
        for y in range(S):
            grain = math.sin(y * 0.35 + phase + math.sin(x * 0.9 + board) * 1.3) * 0.06
            edge = -0.18 if x % 16 in (0, 15) else 0
            px[x, y] = shade(WALNUT, tone + grain + edge + rng.uniform(-0.03, 0.03)) + (255,)
    d = ImageDraw.Draw(img)
    # The cabinet's faces are 9 pixels tall and sample rows 28..63: brass trim at both ends of that band.
    for y0 in (28, S - 3):
        d.rectangle([0, y0, S - 1, y0 + 2], fill=shade(BRASS, 0.95) + (255,))
        d.line([0, y0, S - 1, y0], fill=shade(BRASS, 1.2) + (255,))
    if front:
        # Three blend ports (brass rings, dark mouths) with 1, 2, 3 brass studs above, and a gauge.
        for i, cx in enumerate((11, 26, 41)):
            cy = 51
            d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], fill=shade(BRASS, 0.8) + (255,))
            d.ellipse([cx - 5, cy - 5, cx + 5, cy + 5], fill=shade(BRASS, 1.1) + (255,))
            d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(28, 22, 18, 255))
            d.point((cx - 1, cy - 2), fill=(70, 56, 44, 255))
            for k in range(i + 1):
                sx = cx - i * 2 + k * 4
                d.rectangle([sx, 39, sx + 1, 40], fill=shade(BRASS, 1.25) + (255,))
        d.ellipse([47, 33, 61, 47], fill=shade(BRASS, 0.85) + (255,))
        d.ellipse([49, 35, 59, 45], fill=(232, 226, 206, 255))
        d.line([54, 40, 57, 37], fill=(150, 30, 20, 255), width=1)
        d.point((54, 40), fill=(40, 30, 20, 255))
    return img


def metal(rng, base, brushed=True):
    img = Image.new('RGBA', (S, S))
    px = img.load()
    rows = [rng.uniform(-0.05, 0.05) for _ in range(S)]
    for y in range(S):
        for x in range(S):
            f = 1 + rows[y] + rng.uniform(-0.025, 0.025) + 0.08 * math.sin((x + y) / 20)
            px[x, y] = shade(base, f) + (255,)
    return img


def glass():
    img = Image.new('RGBA', (S, S), (210, 235, 235, 40))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=(230, 245, 245, 150))
    d.line([6, 4, 6, 56], fill=(255, 255, 255, 120), width=2)
    d.line([11, 6, 11, 30], fill=(255, 255, 255, 70), width=1)
    return img


def liquid(rng, lit):
    img = Image.new('RGBA', (S, S))
    px = img.load()
    for y in range(S):
        for x in range(S):
            f = (1.25 if lit else 1.0) + 0.06 * math.sin(x / 5 + y / 9) + rng.uniform(-0.03, 0.03)
            px[x, y] = shade(AMBER, f) + (225,)
    for _ in range(14 if lit else 6):                       # bubbles
        bx, by = rng.randrange(4, S - 4), rng.randrange(4, S - 4)
        ImageDraw.Draw(img).ellipse([bx, by, bx + 2, by + 2], outline=(255, 226, 160, 255))
    return img


def textures():
    TEX.mkdir(parents=True, exist_ok=True)
    rng = random.Random(420)
    walnut(rng).save(TEX / 'cabinet.png')
    walnut(rng, front=True).save(TEX / 'front.png')
    metal(rng, BRASS).save(TEX / 'brass.png')
    metal(rng, COPPER).save(TEX / 'copper.png')
    glass().save(TEX / 'glass.png')
    liquid(rng, False).save(TEX / 'liquid.png')
    liquid(rng, True).save(TEX / 'liquid_lit.png')


# ---- model -----------------------------------------------------------------------------------

def box(f, t, tex, faces=None, light=None):
    faces = faces or {'north': tex, 'south': tex, 'east': tex, 'west': tex, 'up': tex, 'down': tex}
    e = {'from': f, 'to': t, 'faces': {}}
    for face, texture in faces.items():
        (x0, y0, z0), (x1, y1, z1) = f, t
        uv = {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
              'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
              'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}[face]
        e['faces'][face] = {'texture': texture, 'uv': uv}
    if light:
        e['neoforge_data'] = {'block_light': light, 'sky_light': light}
    return e


def elements(lit):
    cab = '#cabinet'
    parts = [
        # The cabinet, its front (ports and gauge) facing north, and the brass deck.
        box([1, 0, 1], [15, 9, 15], cab, {'north': '#front', 'south': cab, 'east': cab, 'west': cab, 'up': cab, 'down': cab}),
        box([0.5, 9, 0.5], [15.5, 10, 15.5], '#brass'),
        # Four brass feet.
        *[box([x, 0, z], [x + 2, 1, z + 2], '#brass') for x in (0.5, 13.5) for z in (0.5, 13.5)],
        # The still: amber terpene in a glass chamber, a brass dome and spout.
        box([4.5, 10, 4.5], [11.5, 13, 11.5], '#liquid', light=12 if lit else None),
        box([4, 10, 4], [12, 15, 12], '#glass'),
        box([4.5, 15, 4.5], [11.5, 16, 11.5], '#brass'),
        box([6.5, 16, 6.5], [9.5, 16.75, 9.5], '#brass'),
        # The condenser: a copper pipe off the dome, down the side in three coils.
        box([11.5, 15, 7.25], [14, 15.75, 8.75], '#copper'),
        box([13.25, 10, 7.25], [14, 15, 8.75], '#copper'),
        *[box([12.5, y, 6.5], [14.75, y + 0.75, 9.5], '#copper') for y in (10.75, 12.25, 13.75)],
    ]
    return parts


def model():
    for lit in (False, True):
        write(ASSETS / f'models/block/terpinator{"_lit" if lit else ""}.json', {
            'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent', 'ambientocclusion': False,
            'textures': {'particle': 'fourtwenty:block/terpinator/brass', 'cabinet': 'fourtwenty:block/terpinator/cabinet',
                         'front': 'fourtwenty:block/terpinator/front', 'brass': 'fourtwenty:block/terpinator/brass',
                         'copper': 'fourtwenty:block/terpinator/copper', 'glass': 'fourtwenty:block/terpinator/glass',
                         'liquid': f'fourtwenty:block/terpinator/liquid{"_lit" if lit else ""}'},
            'elements': elements(lit)})
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    write(ASSETS / 'blockstates/terpinator.json', {'variants': {
        f'facing={f},lit={str(lit).lower()}': dict({'model': f'fourtwenty:block/terpinator{"_lit" if lit else ""}'}, **({'y': r} if r else {}))
        for f, r in rot.items() for lit in (False, True)}})
    write(ASSETS / 'models/item/terpinator.json', {'parent': 'fourtwenty:block/terpinator'})


def data():
    write(DATA / 'loot_table/blocks/terpinator.json', {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'fourtwenty:terpinator'}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    # Glass over copper either side of a brewing stand, over copper either side of a weed bud.
    write(DATA / 'recipe/terpinator.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['GGG', 'CSC', 'CBC'],
        'key': {'G': {'item': 'minecraft:glass'}, 'C': {'item': 'minecraft:copper_ingot'}, 'S': {'item': 'minecraft:brewing_stand'},
                'B': {'item': 'fourtwenty:weed_bud'}},
        'result': {'id': 'fourtwenty:terpinator', 'count': 1}})
    tag = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'
    current = json.loads(tag.read_text(encoding='utf-8')) if tag.exists() else {'replace': False, 'values': []}
    current['values'] = sorted(set(current['values']) | {'fourtwenty:terpinator'})
    write(tag, current)


TEXT = {
    'block.fourtwenty.terpinator': 'Terpinator',
    'container.fourtwenty.terpinator': 'Terpinator',
    'itemGroup.fourtwenty.hemp_and_terpenes': 'Hemp & Terpenes',
    'item.fourtwenty.terpene.unknown.base': 'Undiscovered Base Terpene',
    'item.fourtwenty.terpene.unknown.accord': 'Undiscovered Accord',
    'item.fourtwenty.terpene.unknown.final': 'Undiscovered Terpene',
    'item.fourtwenty.terpene.unknown.resin': 'Undiscovered Resin',
    'item.fourtwenty.terpene.unknown.hint': 'Make one in a Terpinator, or get one from a friend, to learn its name.',
    'item.fourtwenty.terpene.roles': 'Sets the family (%s), hue (%s) or tone (%s)',
    'item.fourtwenty.terpene.accord': 'Accord · %s. Finish it with a base in port 3.',
    'item.fourtwenty.terpene.final': 'Surround with 8 hemp blocks to dye them.',
    'gui.fourtwenty.terpinator.guide': 'Guide',
    'gui.fourtwenty.terpinator.unknown': '???',
    'gui.fourtwenty.terpinator.method': '%s makes %s',
    'gui.fourtwenty.terpinator.method.heat': 'Heat',
    'gui.fourtwenty.terpinator.method.cold': 'Cold',
    'gui.fourtwenty.terpinator.method.pressure': 'Pressure',
    'gui.fourtwenty.terpinator.method.time': 'Time',
    'gui.fourtwenty.terpinator.step.press': 'Put weed buds in the press (top left). Two buds make one resin.',
    'gui.fourtwenty.terpinator.step.refine': 'The resin refines into a base terpene. Pick a method below the arrow.',
    'gui.fourtwenty.terpinator.step.all_bases': 'Refine all four bases: try each method. (%s of 4)',
    'gui.fourtwenty.terpinator.step.accord': 'Put two base terpenes in ports 1 and 2 to blend an accord.',
    'gui.fourtwenty.terpinator.step.finish': 'Put your accord in port 1 and a base in port 3 to finish a colour.',
    'gui.fourtwenty.terpinator.step.free': 'You know %s of 64 colours. The Guide shows what\'s left.',
    'gui.fourtwenty.terpinator.advice.port1_first': 'Every blend starts in port 1.',
    'gui.fourtwenty.terpinator.advice.add_port2': 'Add a second base in port 2 for an accord.',
    'gui.fourtwenty.terpinator.advice.clear_port3': 'Two bases make an accord: empty port 3.',
    'gui.fourtwenty.terpinator.advice.clear_port2': 'An accord fills ports 1 and 2: empty port 2.',
    'gui.fourtwenty.terpinator.advice.add_port3': 'Add a base in port 3: it sets the tone.',
    'gui.fourtwenty.guide.title': 'Terpinator Guide',
    'gui.fourtwenty.guide.progress': '%s of %s terpenes discovered',
    'gui.fourtwenty.guide.first_session': 'First session',
    'gui.fourtwenty.guide.step.press': 'Press buds into resin',
    'gui.fourtwenty.guide.step.refine': 'Refine a base terpene',
    'gui.fourtwenty.guide.step.all_bases': 'Refine all four bases',
    'gui.fourtwenty.guide.step.accord': 'Blend an accord',
    'gui.fourtwenty.guide.step.finish': 'Finish your first colour',
    'gui.fourtwenty.guide.legend': 'What each base does',
    'gui.fourtwenty.guide.port': 'Port %s',
    'gui.fourtwenty.guide.rule': 'Port 1 picks the family, port 2 the hue, port 3 the tone. Learn one row and you can predict the rest.',
    'gui.fourtwenty.guide.map': 'The 64 colours',
    'gui.fourtwenty.guide.columns': 'Columns, by port 3: pale, bright, dusky, deep',
    'gui.fourtwenty.guide.how.base': 'Refine resin by %s',
    'gui.fourtwenty.guide.how.accord': 'Port 1: %s, port 2: %s',
    'gui.fourtwenty.guide.hue': '%s, in the %s family',
    'gui.fourtwenty.guide.how.final': 'Finish %s with %s (%s)',
    'gui.fourtwenty.guide.dye': 'Dyes hemp: surround it with 8 hemp blocks',
    'gui.fourtwenty.toast.discovered': 'Terpene discovered',
}


def lang():
    path = ASSETS / 'lang/en_us.json'
    data = json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    data.update(TEXT)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def preview(out):
    """The front and top as a flat sketch, for checking the textures by eye."""
    out.mkdir(parents=True, exist_ok=True)
    sheet = Image.new('RGBA', (S * 7, S), (40, 44, 42, 255))
    for i, name in enumerate(('front', 'cabinet', 'brass', 'copper', 'glass', 'liquid', 'liquid_lit')):
        sheet.alpha_composite(Image.open(TEX / f'{name}.png'), (i * S, 0))
    sheet.resize((S * 7 * 3, S * 3), Image.NEAREST).save(out / 'terpinator_textures.png')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview')
    args = ap.parse_args()
    textures()
    model()
    data()
    lang()
    if args.preview:
        preview(Path(args.preview))
    print('terpinator generated')
