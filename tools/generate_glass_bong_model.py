#!/usr/bin/env python3
"""The glass bong block model: a frosted beaker bong with water, a lime-accented tube and a bowl.

A composite model (translucent glass + solid bud), built from small boxes like the Dispensary hut
block. The bowl faces north (the blockstate turns it toward whoever placed it).

Writes src/main/resources/assets/fourtwenty/models/block/glass_bong.json. Run again after edits.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / 'src/main/resources/assets/fourtwenty/models/block/glass_bong.json'


def box(name, frm, to, tex, rotation=None):
    """An axis-aligned box with UVs taken from its own extents (all within one block, so 0..16)."""
    (x1, y1, z1), (x2, y2, z2) = frm, to
    uv = {
        'north': [16 - x2, 16 - y2, 16 - x1, 16 - y1],
        'south': [x1, 16 - y2, x2, 16 - y1],
        'east': [16 - z2, 16 - y2, 16 - z1, 16 - y1],
        'west': [z1, 16 - y2, z2, 16 - y1],
        'up': [x1, z1, x2, z2],
        'down': [x1, 16 - z2, x2, 16 - z1],
    }
    element = {'name': name, 'from': list(frm), 'to': list(to),
               'faces': {face: {'uv': [round(c, 3) for c in coords], 'texture': '#' + tex} for face, coords in uv.items()}}
    if rotation:
        element['rotation'] = rotation
    return element


def generate():
    glass, solid = [], []
    # Beaker base with water.
    glass.append(box('base', (5, 0, 5), (11, 4, 11), 'glass'))
    glass.append(box('water', (5.3, 0.2, 5.3), (10.7, 3, 10.7), 'water'))
    glass.append(box('neck', (6, 4, 6), (10, 5, 10), 'glass'))
    # Tube, pinch ring, mouthpiece.
    glass.append(box('tube', (6.5, 5, 6.5), (9.5, 14.5, 9.5), 'glass'))
    glass.append(box('pinch ring', (6.2, 9, 6.2), (9.8, 9.6, 9.8), 'accent'))
    glass.append(box('mouthpiece', (6, 14.5, 6), (10, 15.4, 10), 'glass'))
    glass.append(box('mouthpiece lip', (6, 15.4, 6), (10, 15.7, 10), 'accent'))
    # Downstem angled up and out of the front, ending in the bowl.
    glass.append(box('downstem', (7.5, 2.2, 2.2), (8.5, 3, 5.5), 'glass',
                     rotation={'angle': 22.5, 'axis': 'x', 'origin': [8, 2.6, 5.5]}))
    glass.append(box('bowl', (6.75, 3.4, 1), (9.25, 5, 3.5), 'accent'))
    solid.append(box('packed bud', (7.1, 4.6, 1.35), (8.9, 4.95, 3.15), 'bud'))

    model = {
        'credit': 'FourTwenty tools/generate_glass_bong_model.py',
        'parent': 'block/block',
        'ambientocclusion': False,
        'loader': 'neoforge:composite',
        'textures': {'particle': 'minecraft:block/white_stained_glass'},
        'children': {
            'solid': {'render_type': 'minecraft:solid',
                      'textures': {'bud': 'minecraft:block/moss_block'},
                      'elements': solid},
            'translucent': {'render_type': 'minecraft:translucent',
                            'textures': {'glass': 'minecraft:block/white_stained_glass',
                                         'water': 'minecraft:block/light_blue_stained_glass',
                                         'accent': 'minecraft:block/lime_stained_glass'},
                            'elements': glass},
        },
    }
    for child in model['children'].values():
        for element in child['elements']:
            for face in element['faces'].values():
                assert all(0 <= c <= 16 for c in face['uv']), (element['name'], face)
    MODEL.write_text(json.dumps(model, indent=2) + '\n')
    print(f'wrote {MODEL.relative_to(ROOT)}: {len(glass)} glass, {len(solid)} solid elements')


if __name__ == '__main__':
    generate()
