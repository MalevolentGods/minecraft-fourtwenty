#!/usr/bin/env python3
"""Couches in the 64 terpene colours and Natural Hemp: hemp seats with a back, arm rests and wooden
legs that join each other like stairs (CouchBlock).

Models are drawn for a couch whose back faces east (vanilla's stair base orientation, so the
blockstate turns them exactly as vanilla turns stairs): the seat faces west, the sitter's left is
south. Parts, put together by a multipart blockstate:
- legs: four dark oak legs (every couch);
- seat: a hemp cushion with a softened (stepped) front edge on each open side: straight couches
  have one open side (the front), outer corners two, inner corners none;
- back: along the east side (straight), round the inside of the corner (inner), or a corner post
  (outer); a softened top;
- arm_left / arm_right: on a straight couch's open ends (CouchBlock's LEFT_ARM / RIGHT_ARM).
Every edge where a couch meets the next runs to the block boundary, so joined couches read as one
sofa. Textures: the colour's hemp block (fourtwenty:block/hemp/<code>), dark oak planks.

Writes per couch: child models of the six coloured parts, a multipart blockstate, an item model (a
straight couch with both arms), loot, a recipe (3 of the colour's hemp stairs over 2 sticks make 3),
names and the fourtwenty:couches tag. Run again after edits.
"""
import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'
TERP = json.loads((ROOT / 'tools/terpenes.json').read_text(encoding='utf-8'))
WOOD = 'minecraft:block/dark_oak_planks'
SOFT = 0.5            # the softened edge: each step in from an open side


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def box(f, t, tex):
    """An element with every face textured; UVs come from the element's position."""
    return {'from': list(f), 'to': list(t), 'faces': {d: {'texture': tex} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}


def legs():
    out = []
    for x in (1, 13):
        for z in (1, 13):
            out.append(box((x, 0, z), (x + 2, 3, z + 2), '#wood'))
    return out


def seat(open_west, open_north):
    """The cushion: a base and a pad stepped in from the open sides."""
    bx, bz = (SOFT if open_west else 0), (SOFT if open_north else 0)
    px, pz = (2 * SOFT if open_west else 0), (2 * SOFT if open_north else 0)
    return [box((bx, 3, bz), (16, 7, 16), '#cushion'), box((px, 7, pz), (16, 8, 16), '#cushion')]


def back(kind):
    """The back (y 8-15) and its softened top (y 15-15.5), which runs to the sides that join on."""
    if kind == 'straight':
        return [box((12, 8, 0), (16, 15, 16), '#cushion'), box((12 + SOFT, 15, 0), (16 - SOFT, 15.5, 16), '#cushion')]
    if kind == 'inner':          # along the east and south sides; joins on to the north and west
        return [box((12, 8, 0), (16, 15, 16), '#cushion'), box((0, 8, 12), (12, 15, 16), '#cushion'),
                box((12 + SOFT, 15, 0), (16 - SOFT, 15.5, 16 - SOFT), '#cushion'),
                box((0, 15, 12 + SOFT), (12 + SOFT, 15.5, 16 - SOFT), '#cushion')]
    # outer: a corner post that the backs either side (east and south neighbours) run into
    return [box((12, 8, 12), (16, 15, 16), '#cushion'), box((12 + SOFT, 15, 12 + SOFT), (16, 15.5, 16), '#cushion')]


def arm(side):
    """An arm rest on the seat, from the front to the back, on the south (left) or north (right) end."""
    z0, z1 = (13, 16) if side == 'left' else (0, 3)
    return [box((1, 8, z0), (12, 12.5, z1), '#cushion'), box((1 + SOFT, 12.5, z0 + SOFT), (12, 13, z1 - SOFT), '#cushion')]


PARTS = {
    'seat_straight': lambda: seat(True, False),
    'seat_inner': lambda: seat(False, False),
    'seat_outer': lambda: seat(True, True),
    'back_straight': lambda: back('straight'),
    'back_inner': lambda: back('inner'),
    'back_outer': lambda: back('outer'),
    'arm_left': lambda: arm('left'),
    'arm_right': lambda: arm('right'),
}
COLOURED = [p for p in PARTS]                         # every part but the legs uses the hemp
FACINGS = {'east': 0, 'south': 1, 'west': 2, 'north': 3}
SHAPES = ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right')


def turns(facing, shape):
    """Vanilla's stair blockstate rotation (CouchBlock.turns)."""
    return (FACINGS[facing] - (1 if shape.endswith('_left') else 0)) % 4


def blockstate(block):
    parts = [{'apply': {'model': 'fourtwenty:block/couch_legs'}}]
    for facing in FACINGS:
        for shape in SHAPES:
            kind = shape.split('_')[0]
            y = 90 * turns(facing, shape)
            for part in (f'seat_{kind}', f'back_{kind}'):
                apply = {'model': f'fourtwenty:block/{block}_{part}', 'uvlock': True}
                if y:
                    apply['y'] = y
                parts.append({'when': {'facing': facing, 'shape': shape}, 'apply': apply})
        y = 90 * turns(facing, 'straight')
        for side in ('left', 'right'):
            apply = {'model': f'fourtwenty:block/{block}_arm_{side}', 'uvlock': True}
            if y:
                apply['y'] = y
            parts.append({'when': {'facing': facing, 'shape': 'straight', f'{side}_arm': 'true'}, 'apply': apply})
    return {'multipart': parts}


def generate():
    write(ASSETS / 'models/block/couch_legs.json', {'parent': 'minecraft:block/block', 'textures': {'particle': WOOD, 'wood': WOOD},
                                                    'elements': legs()})
    for part, make in PARTS.items():
        write(ASSETS / f'models/block/couch_{part}.json', {'parent': 'minecraft:block/block', 'textures': {'particle': '#cushion'},
                                                          'elements': make()})
    couches = [(c['code'], c['name'], f"fourtwenty:block/hemp/{c['code']}", f"fourtwenty:hemp_{c['code']}_stairs") for c in TERP['colors']]
    couches.append(('natural', TERP['natural']['name'], 'fourtwenty:block/hemp/natural', 'fourtwenty:natural_hemp_stairs'))
    names = collections.OrderedDict()
    item_elements = legs() + seat(True, False) + back('straight') + arm('left') + arm('right')
    for code, name, hemp, stairs in couches:
        block = f'couch_{code}'
        for part in COLOURED:
            write(ASSETS / f'models/block/{block}_{part}.json', {'parent': f'fourtwenty:block/couch_{part}', 'textures': {'cushion': hemp}})
        write(ASSETS / f'blockstates/{block}.json', blockstate(block))
        write(ASSETS / f'models/item/{block}.json', {'parent': 'minecraft:block/block',
            'textures': {'cushion': hemp, 'wood': WOOD, 'particle': hemp}, 'elements': item_elements})
        write(DATA / f'loot_table/blocks/{block}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
            {'type': 'minecraft:item', 'name': f'fourtwenty:{block}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        write(DATA / f'recipe/{block}.json', {'type': 'minecraft:crafting_shaped', 'category': 'decorations', 'group': 'fourtwenty_couch',
            'pattern': ['SSS', 'T T'], 'key': {'S': {'item': stairs}, 'T': {'item': 'minecraft:stick'}},
            'result': {'id': f'fourtwenty:{block}', 'count': 3}})
        names[block] = f'{name} Couch' if code != 'natural' else f'{name} Couch'
    write(DATA / 'tags/item/couches.json', {'replace': False, 'values': [f'fourtwenty:couch_{c[0]}' for c in couches]})
    write(DATA / 'tags/block/couches.json', {'replace': False, 'values': [f'fourtwenty:couch_{c[0]}' for c in couches]})
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    for block, label in names.items():
        data[f'block.fourtwenty.{block}'] = label
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{len(couches)} couches, {len(PARTS) + 1} part models')


if __name__ == '__main__':
    generate()
