#!/usr/bin/env python3
"""Assets and recipes for FourTwenty's side tables (nightstands), one per vanilla wood.

The model is FourTwenty's own design: a slim top overhanging the body by a pixel, a body with a
drawer front standing half a pixel proud and a small brass knob, four slim legs in the wood's
stripped log, and a low shelf between them. Every part is textured with vanilla textures (the
wood's planks, its stripped log, a gold block for the knob), so there's no new art. Writes the
template and per-wood models, blockstates (the drawer faces FACING), item models, loot tables,
recipes, the axe-mineable tag and English names. Run again after edits.
"""
import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'crimson', 'warped']


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + '\n', encoding='utf-8')


def box(frm, to, texture, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    """An element whose faces all use one texture; UVs come from the element's own position."""
    return {'from': frm, 'to': to, 'faces': {f: {'texture': texture} for f in faces}}


# The drawer faces north in the model; blockstates turn it to FACING.
ELEMENTS = [
    box([1, 14, 1], [15, 16, 15], '#top'),                 # the top, overhanging the body by a pixel
    box([2, 8, 2], [14, 14, 14], '#body'),                 # the body
    box([3, 9, 1.5], [13, 13, 2], '#top', ('north', 'east', 'west', 'up', 'down')),  # the drawer front
    box([7.5, 10.5, 1], [8.5, 11.5, 1.5], '#knob', ('north', 'east', 'west', 'up', 'down')),
    box([2, 0, 2], [4, 8, 4], '#legs'), box([12, 0, 2], [14, 8, 4], '#legs'),
    box([2, 0, 12], [4, 8, 14], '#legs'), box([12, 0, 12], [14, 8, 14], '#legs'),
    box([4, 2, 4], [12, 3, 12], '#body'),                  # the low shelf between the legs
    box([4, 2, 2.5], [12, 3, 4], '#body'), box([4, 2, 12], [12, 3, 13.5], '#body'),
    box([2.5, 2, 4], [4, 3, 12], '#body'), box([12, 2, 4], [13.5, 3, 12], '#body'),
]
ROTATION = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def log_texture(wood):
    return f'minecraft:block/stripped_{wood}_stem' if wood in ('crimson', 'warped') else f'minecraft:block/stripped_{wood}_log'


def title(wood): return ' '.join(part.capitalize() for part in wood.split('_'))


def generate():
    write(ASSETS / 'models/block/side_table.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': '#top'},
        'elements': ELEMENTS})
    names = {}
    for wood in WOODS:
        name = f'{wood}_side_table'
        write(ASSETS / f'models/block/{name}.json', {'parent': 'fourtwenty:block/side_table', 'textures': {
            'top': f'minecraft:block/{wood}_planks', 'body': f'minecraft:block/{wood}_planks',
            'legs': log_texture(wood), 'knob': 'minecraft:block/gold_block'}})
        write(ASSETS / f'blockstates/{name}.json', {'variants': {
            f'facing={f}': ({'model': f'fourtwenty:block/{name}', 'y': r} if r else {'model': f'fourtwenty:block/{name}'})
            for f, r in ROTATION.items()}})
        write(ASSETS / f'models/item/{name}.json', {'parent': f'fourtwenty:block/{name}'})
        write(DATA / f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{
            'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fourtwenty:{name}'}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        # Cheap furniture with a bud in it: slabs for the top, planks for the body and legs -> 2.
        write(DATA / f'recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building',
            'pattern': ['SSS', 'PBP', 'P P'],
            'key': {'S': {'item': f'minecraft:{wood}_slab'}, 'P': {'item': f'minecraft:{wood}_planks'},
                    'B': {'item': 'fourtwenty:weed_bud'}},
            'result': {'id': f'fourtwenty:{name}', 'count': 2}})
        names[name] = f'{title(wood)} Side Table'
    write(RES / 'data/minecraft/tags/block/mineable/axe.json',
          {'replace': False, 'values': [f'fourtwenty:{w}_side_table' for w in WOODS]})
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    for name, label in names.items():
        data[f'block.fourtwenty.{name}'] = label
        data[f'item.fourtwenty.{name}'] = label
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{len(names)} side tables generated')


if __name__ == '__main__':
    generate()
