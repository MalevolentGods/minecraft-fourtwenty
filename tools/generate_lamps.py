#!/usr/bin/env python3
"""Assets and recipes for FourTwenty's coloured lamps (all 16 dye colours).

Per colour: a Lamp (lit while powered), an Inverted Lamp (lit unless powered) and a Lamp Trim (a
thin always-lit plate on any face). Writes blockstates, models, item models, loot tables, recipes
and English names. Run again after edits.

The lamp textures (textures/block/<colour>_lamp_on.png / _off.png) are ProjectRed's illumar lamp
textures (MIT License, Copyright (c) MrTJP; see licenses/ProjectRed-LICENSE.txt), copied as-is;
this script doesn't draw them.
"""
import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'

# Vanilla dye colours (DyeColor order).
COLORS = {
    'white': (0xF9, 0xFF, 0xFE), 'orange': (0xF9, 0x80, 0x1D), 'magenta': (0xC7, 0x4E, 0xBD), 'light_blue': (0x3A, 0xB3, 0xDA),
    'yellow': (0xFE, 0xD8, 0x3D), 'lime': (0x80, 0xC7, 0x1F), 'pink': (0xF3, 0x8B, 0xAA), 'gray': (0x6C, 0x74, 0x77),
    'light_gray': (0x9D, 0x9D, 0x97), 'cyan': (0x16, 0x9C, 0x9C), 'purple': (0x89, 0x32, 0xB8), 'blue': (0x3C, 0x44, 0xAA),
    'brown': (0x83, 0x54, 0x32), 'green': (0x5E, 0x9C, 0x16), 'red': (0xB0, 0x2E, 0x26), 'black': (0x3A, 0x3A, 0x40),
}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + '\n')


def loot(name):
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fourtwenty:{name}'}],
                                                  'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}


def title(color): return ' '.join(part.capitalize() for part in color.split('_'))


def generate():
    names = {}
    # The trim template: a 2-pixel plate on the bottom face (FACING=down); blockstates rotate it.
    write(ASSETS / 'models/block/lamp_trim.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': '#lamp'},
        'elements': [{'from': [0, 0, 0], 'to': [16, 2, 16], 'faces': {
            'down': {'uv': [0, 0, 16, 16], 'texture': '#lamp', 'cullface': 'down'},
            'up': {'uv': [0, 0, 16, 16], 'texture': '#lamp'},
            'north': {'uv': [0, 14, 16, 16], 'texture': '#lamp'}, 'south': {'uv': [0, 14, 16, 16], 'texture': '#lamp'},
            'west': {'uv': [0, 14, 16, 16], 'texture': '#lamp'}, 'east': {'uv': [0, 14, 16, 16], 'texture': '#lamp'}}}]})
    trim_rotation = {'down': {}, 'up': {'x': 180}, 'north': {'x': 270}, 'south': {'x': 270, 'y': 180},
                     'west': {'x': 270, 'y': 270}, 'east': {'x': 270, 'y': 90}}
    for color, rgb in COLORS.items():
        for lit in (True, False):
            state = 'on' if lit else 'off'
            assert (ASSETS / f'textures/block/{color}_lamp_{state}.png').exists(), f'missing {color} lamp texture'
            write(ASSETS / f'models/block/{color}_lamp_{state}.json',
                  {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'fourtwenty:block/{color}_lamp_{state}'}})
        for lamp, item_state in ((f'{color}_lamp', 'off'), (f'{color}_inverted_lamp', 'on')):
            write(ASSETS / f'blockstates/{lamp}.json', {'variants': {
                'lit=true': {'model': f'fourtwenty:block/{color}_lamp_on'},
                'lit=false': {'model': f'fourtwenty:block/{color}_lamp_off'}}})
            write(ASSETS / f'models/item/{lamp}.json', {'parent': f'fourtwenty:block/{color}_lamp_{item_state}'})
            write(DATA / f'loot_table/blocks/{lamp}.json', loot(lamp))
        trim = f'{color}_lamp_trim'
        write(ASSETS / f'models/block/{trim}.json', {'parent': 'fourtwenty:block/lamp_trim',
                                                     'textures': {'lamp': f'fourtwenty:block/{color}_lamp_on'}})
        write(ASSETS / f'blockstates/{trim}.json', {'variants': {
            f'facing={d}': {'model': f'fourtwenty:block/{trim}', **r} for d, r in trim_rotation.items()}})
        write(ASSETS / f'models/item/{trim}.json', {'parent': f'fourtwenty:block/{trim}'})
        write(DATA / f'loot_table/blocks/{trim}.json', loot(trim))
        # Recipes from Overworld resources, a little effort but no grind: 6 glass around a torch
        # with a dye above and redstone below -> 4 lamps; inverted = lamp + redstone torch;
        # 1 inverted lamp -> 8 trims.
        write(DATA / f'recipe/{color}_lamp.json', {'type': 'minecraft:crafting_shaped',
            'pattern': ['GDG', 'GTG', 'GRG'],
            'key': {'G': {'item': 'minecraft:glass'}, 'D': {'item': f'minecraft:{color}_dye'},
                    'T': {'item': 'minecraft:torch'}, 'R': {'item': 'minecraft:redstone'}},
            'result': {'id': f'fourtwenty:{color}_lamp', 'count': 4}})
        write(DATA / f'recipe/{color}_inverted_lamp.json', {'type': 'minecraft:crafting_shapeless', 'ingredients': [
            {'item': f'fourtwenty:{color}_lamp'}, {'item': 'minecraft:redstone_torch'}],
            'result': {'id': f'fourtwenty:{color}_inverted_lamp', 'count': 1}})
        write(DATA / f'recipe/{trim}.json', {'type': 'minecraft:crafting_shapeless', 'ingredients': [
            {'item': f'fourtwenty:{color}_inverted_lamp'}], 'result': {'id': f'fourtwenty:{trim}', 'count': 8}})
        names[f'{color}_lamp'] = f'{title(color)} Lamp'
        names[f'{color}_inverted_lamp'] = f'{title(color)} Inverted Lamp'
        names[trim] = f'{title(color)} Lamp Trim'
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    for name, label in names.items():
        data[f'block.fourtwenty.{name}'] = label
        data[f'item.fourtwenty.{name}'] = label
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{len(names)} lamp blocks generated')


if __name__ == '__main__':
    generate()
