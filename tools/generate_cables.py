#!/usr/bin/env python3
"""Twin Wick and Daisy Wick (specs/009-twin-daisy-wicks): textures, models, blockstates, item models, recipes, loot and lang.

    python3 tools/generate_cables.py

A Twin Wick is a twisted red-and-black pair; a Daisy Wick is the pair side by side, with a brass band where it plugs into a block. Both
plug in with a fork: a red prong and a black prong off a crossbar. The knot at the middle is the tier's own Wick material, and a small
stripe on top takes the cable's dye colour (tint 0), so the cores stay red and black whatever the colour.
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
DATA = ROOT / 'src/main/resources/data'
TIERS = {'hemp': 'puff', 'rolled': 'toke', 'glass': 'rip', 'gold': 'blaze'}
TIER_NAME = {'hemp': 'Hemp', 'rolled': 'Rolled', 'glass': 'Glass', 'gold': 'Gold'}
RED, RED_HI, RED_LIT = (150, 30, 30), (196, 58, 52), (255, 96, 70)
BLACK, BLACK_HI, BLACK_LIT = (28, 28, 32), (62, 62, 70), (120, 255, 110)
BRASS, BRASS_HI = (170, 132, 52), (222, 186, 92)
SIDES = ('north', 'east', 'south', 'west', 'up', 'down')
ROT = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + '\n', encoding='utf-8')


def strand(name, base, hi, lit):
    img = Image.new('RGBA', (16, 16), base + (255,))
    d = ImageDraw.Draw(img)
    for y in range(0, 16, 4):
        d.line([0, y, 15, y], fill=hi + (255,))
    if lit:
        for y in range(2, 16, 4):
            d.line([0, y, 15, y], fill=lit + (255,))
    save(img, ASSETS / f'textures/block/power/{name}.png')


def twist(lit):
    """Red and black running diagonally round each other, a 4-pixel pitch."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for x in range(16):
        for y in range(16):
            red = ((x + y) // 4) % 2 == 0
            edge = (x + y) % 4 == 0
            c = (RED_HI if edge else RED) if red else (BLACK_HI if edge else BLACK)
            if lit and edge:
                c = RED_LIT if red else BLACK_LIT
            px[x, y] = c + (255,)
    save(img, ASSETS / f'textures/block/power/cable_twist{"_lit" if lit else ""}.png')


def plain(name, base, hi):
    img = Image.new('RGBA', (16, 16), base + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=hi + (255,))
    for x in range(2, 16, 4):
        d.line([x, 0, x, 15], fill=hi + (255,))
    save(img, ASSETS / f'textures/block/power/{name}.png')


def textures():
    for lit in (False, True):
        s = '_lit' if lit else ''
        strand(f'cable_red{s}', RED, RED_HI, RED_LIT if lit else None)
        strand(f'cable_black{s}', BLACK, BLACK_HI, BLACK_LIT if lit else None)
        twist(lit)
    plain('cable_band', BRASS, BRASS_HI)
    stripe = Image.new('RGBA', (16, 16), (232, 228, 214, 255))
    save(stripe, ASSETS / 'textures/block/power/cable_stripe.png')


def face(uv, tex, tint=False):
    f = {'uv': uv, 'texture': tex}
    if tint:
        f['tintindex'] = 0
    return f


def box(frm, to, tex, tint=False):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    return {'from': frm, 'to': to, 'faces': {
        'north': face([x0, 16 - y1, x1, 16 - y0], tex, tint), 'south': face([x0, 16 - y1, x1, 16 - y0], tex, tint),
        'east': face([z0, 16 - y1, z1, 16 - y0], tex, tint), 'west': face([z0, 16 - y1, z1, 16 - y0], tex, tint),
        'up': face([x0, z0, x1, z1], tex, tint), 'down': face([x0, z0, x1, z1], tex, tint)}}


def model(name, textures, elements):
    write(ASSETS / f'models/block/cable/{name}.json', {'parent': 'block/block', 'textures': {'particle': textures['knot'], **textures}, 'elements': elements})


def core(kind):
    els = [box([5, 6, 5], [11, 10, 11], '#knot'), box([6.5, 10, 6.5], [9.5, 10.25, 9.5], '#stripe', tint=True)]
    return els


def arm(kind):
    """The north arm, from the knot out to the block's edge."""
    if kind == 'twin':
        return [box([5.5, 6.5, 0], [10.5, 9.5, 5], '#twist')]
    return [box([5, 7, 0], [7.5, 9, 5], '#red'), box([8.5, 7, 0], [11, 9, 5], '#black')]


def plug(kind):
    """The north arm into a block: a crossbar (a brass band on a Daisy Wick) and a red and a black prong to the block's face."""
    bar = box([4.5, 6.5, 3], [11.5, 9.5, 5], '#band' if kind == 'daisy' else '#knot')
    return [bar, box([5, 7, 0], [7, 9, 3], '#red'), box([9, 7, 0], [11, 9, 3], '#black')]


def blocks():
    names = []
    for prefix, tier in TIERS.items():
        for kind in ('twin', 'daisy'):
            name = f'{prefix}_{kind}_wick'
            names.append((name, prefix, kind))
            for lit in (False, True):
                s = '_lit' if lit else ''
                tex = {'knot': f'fourtwenty:block/power/{tier}_wick{s}', 'stripe': 'fourtwenty:block/power/cable_stripe',
                       'twist': f'fourtwenty:block/power/cable_twist{s}', 'red': f'fourtwenty:block/power/cable_red{s}',
                       'black': f'fourtwenty:block/power/cable_black{s}', 'band': 'fourtwenty:block/power/cable_band'}
                model(f'{name}_core{s}', tex, core(kind))
                model(f'{name}_arm{s}', tex, arm(kind))
                model(f'{name}_plug{s}', tex, plug(kind))
                if not lit:
                    item = core(kind) + [turn(e, 'east') for e in arm(kind)] + [turn(e, 'west') for e in arm(kind)]
                    write(ASSETS / f'models/item/{name}.json', {'parent': 'block/block', 'textures': {'particle': tex['knot'], **tex},
                                                                'elements': item, 'display': WICK_DISPLAY})
            parts = []
            for lit in ('false', 'true'):
                s = '_lit' if lit == 'true' else ''
                parts.append({'when': {'lit': lit}, 'apply': {'model': f'fourtwenty:block/cable/{name}_core{s}'}})
                for side in SIDES:
                    for link in ('wire', 'plug'):
                        part = 'arm' if link == 'wire' else 'plug'
                        parts.append({'when': {side: link, 'lit': lit}, 'apply': {'model': f'fourtwenty:block/cable/{name}_{part}{s}', **ROT[side], 'uvlock': False}})
            write(ASSETS / f'blockstates/{name}.json', {'multipart': parts})
    return names


def turn(element, side):
    """An element of a north arm turned to face east or west (for the item model)."""
    import copy
    e = copy.deepcopy(element)
    (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
    if side == 'east':
        e['from'], e['to'] = [16 - z1, y0, x0], [16 - z0, y1, x1]
    else:
        e['from'], e['to'] = [z0, y0, 16 - x1], [z1, y1, 16 - x0]
    return e


WICK_DISPLAY = {'gui': {'rotation': [30, 225, 0], 'scale': [0.9, 0.9, 0.9]}, 'ground': {'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
                'fixed': {'scale': [0.8, 0.8, 0.8]}, 'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
                'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.5, 0.5, 0.5]}}


def coloured(item, colour):
    """An ingredient: {@code item} in a colour (its block_state component), matched with NeoForge's component ingredient."""
    return {'type': 'neoforge:components', 'items': item, 'components': {'minecraft:block_state': {'colour': colour}}}


def data(names):
    for name, prefix, kind in names:
        wick = f'fourtwenty:{prefix}_wick'
        keep = {'function': 'minecraft:copy_state', 'block': f'fourtwenty:{name}', 'properties': ['colour'], 'conditions': [{'condition': 'minecraft:inverted',
                'term': {'condition': 'minecraft:block_state_property', 'block': f'fourtwenty:{name}', 'properties': {'colour': 'none'}}}]}
        write(DATA / f'fourtwenty/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
            {'type': 'minecraft:item', 'name': f'fourtwenty:{name}', 'functions': [keep]}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        # every Wick of a tier is that tier's Wick in another arrangement: a red one and a black one side by side make a Twin
        # Wick, the red above the black a Daisy Wick
        red, black = coloured(wick, 'red'), coloured(wick, 'black')
        pattern = ['RB'] if kind == 'twin' else ['R', 'B']
        write(DATA / f'fourtwenty/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': pattern,
                                                        'key': {'R': red, 'B': black}, 'result': {'id': f'fourtwenty:{name}', 'count': 1}})
        if kind == 'twin':
            # and the red and black Wicks themselves: eight of the tier's Wicks round a dye
            for colour in ('red', 'black'):
                write(DATA / f'fourtwenty/recipe/{colour}_{prefix}_wick.json', {
                    'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': ['WWW', 'WDW', 'WWW'],
                    'key': {'W': {'item': wick}, 'D': {'item': f'minecraft:{colour}_dye'}},
                    'result': {'id': wick, 'count': 8, 'components': {'minecraft:block_state': {'colour': colour}}}})
    ids = [f'fourtwenty:{n}' for n, _, _ in names]
    write(DATA / 'fourtwenty/tags/block/bi_cables.json', {'replace': False, 'values': ids})
    pick = DATA / 'minecraft/tags/block/mineable/pickaxe.json'
    tag = json.loads(pick.read_text(encoding='utf-8'))
    for i in ids:
        if i not in tag['values']:
            tag['values'].append(i)
    write(pick, tag)
    lang_path = ASSETS / 'lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    for name, prefix, kind in names:
        lang[f'block.fourtwenty.{name}'] = f'{TIER_NAME[prefix]} {"Twin" if kind == "twin" else "Daisy"} Wick'
        lang[f'tooltip.fourtwenty.item.{name}'] = (
            'A two-core cable: every block it touches is wired across it, + to + and − to −, in parallel, whichever way the block faces.'
            if kind == 'twin' else
            'A two-core cable: the blocks along it are wired one after another, in series, and it closes the loop itself. It can\'t branch.')
    lang.update({
        'message.fourtwenty.meter.twin': '%s between its cores · %s on +, %s on − (rated %s) · %s a core · loses %s',
        'message.fourtwenty.meter.daisy': '%s through the string (rated %s) · %s between its cores here · %s a core · loses %s',
        'message.fourtwenty.meter.daisy_branch': 'A Daisy line can\'t branch: it carries nothing until each cable has at most two neighbours',
        'message.fourtwenty.meter.daisy_ring': 'A Daisy line can\'t close on itself: it needs two ends',
    })
    out = '{\n' + ',\n'.join(f'  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}' for k, v in lang.items()) + '\n}\n'
    lang_path.write_text(out, encoding='utf-8')


def main():
    textures()
    names = blocks()
    data(names)
    print(f'wrote {len(names)} cables')


if __name__ == '__main__':
    main()
