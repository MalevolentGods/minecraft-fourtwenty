#!/usr/bin/env python3
"""The Dispensary hut block as a MineColonies-style diorama: a full-size shop counter, two blocks tall.

Like the native hut blocks (e.g. the cook's), it is a composite block model of small boxes using
vanilla and FourTwenty textures, split into solid / cutout / translucent children so glass and the
bud logo render correctly. The front faces north (the blockstate rotates it to the placer).

Writes src/main/resources/assets/fourtwenty/models/block/dispensary.json. Run again after edits.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / 'src/main/resources/assets/fourtwenty/models/block/dispensary.json'


def box(name, frm, to, tex, faces=None, skip=()):
    """An axis-aligned box with UVs taken from its own extents (no stretching).

    UVs must stay within 0..16, or faces sample neighbouring textures in the atlas. Boxes in the
    upper block (y >= 16) map relative to that block; a box spanning both blocks stretches the
    texture vertically over its full height.
    """
    (x1, y1, z1), (x2, y2, z2) = frm, to
    if y1 < 16 < y2:
        v1, v2 = 0, 16
    else:
        base = 16 if y1 >= 16 else 0
        v1, v2 = 16 - (y2 - base), 16 - (y1 - base)
    uv = {
        'north': [16 - x2, v1, 16 - x1, v2],
        'south': [x1, v1, x2, v2],
        'east': [16 - z2, v1, 16 - z1, v2],
        'west': [z1, v1, z2, v2],
        'up': [x1, z1, x2, z2],
        'down': [x1, 16 - z2, x2, 16 - z1],
    }
    out = {}
    for face, coords in uv.items():
        if face in skip:
            continue
        out[face] = {'uv': [round(c, 3) for c in coords], 'texture': '#' + (faces or {}).get(face, tex)}
    return {'name': name, 'from': list(frm), 'to': list(to), 'faces': out}


def jar(name, x, y, z, w=2, h=3):
    """Green-tinted glass stash jar with a lid: returns (translucent, solid) elements."""
    glass = box(name + ' glass', (x, y, z), (x + w, y + h, z + w), 'jar')
    lid = box(name + ' lid', (x - 0.1, y + h, z - 0.1), (x + w + 0.1, y + h + 0.5, z + w + 0.1), 'lid')
    return glass, [lid]


def generate():
    solid, cutout, translucent = [], [], []

    # --- Bottom block: a full-size counter. ------------------------------------------------------
    solid.append(box('kick plate', (0.5, 0, 1.5), (15.5, 1, 11.5), 'kick'))
    solid.append(box('counter', (0, 1, 1), (16, 14, 12), 'wood'))
    solid.append(box('counter top', (0, 14, 0), (16, 16, 12.5), 'top'))
    # The bud icon as the shop's logo, large on the counter front facing customers.
    cutout.append({'name': 'bud logo', 'from': [3.5, 2.5, 0.95], 'to': [12.5, 11.5, 0.95], 'shade': False,
                   'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#bud'}}})

    # --- Back panel rising into the block above: shelf of jars and the green cross. ---------------
    solid.append(box('back panel', (0, 0, 12.5), (16, 31, 16), 'wall'))
    solid.append(box('shelf', (1, 20, 10.5), (15, 21, 12.5), 'wood'))
    for i, x in enumerate((1.75, 5.25, 8.75, 12.25)):
        glass, parts = jar(f'shelf jar {i}', x, 21, 10.75)
        translucent.append(glass)
        solid.extend(parts)
    solid.append(box('cross vertical', (7, 24.5, 12), (9, 31, 12.5), 'cross'))
    solid.append(box('cross horizontal', (4.75, 26.75, 12), (11.25, 28.75, 12.5), 'cross'))

    # --- On the counter (top at y 16). ------------------------------------------------------------
    # Cash register with a tilted-up screen.
    solid.append(box('register base', (1.5, 16, 3), (6, 17.5, 8.5), 'metal'))
    solid.append(box('register keys', (2, 17.5, 3.5), (5.5, 18, 6), 'keys'))
    solid.append(box('register screen', (2, 17.5, 7), (5.5, 21, 7.5), 'screen'))
    # A big stash jar.
    glass, parts = jar('counter jar', 7, 16, 4, w=3, h=5)
    translucent.append(glass)
    solid.extend(parts)
    # Tall glass bong: base with water, tube, mouthpiece flare, clay bowl.
    translucent.append(box('bong base', (11.5, 16, 4), (14.5, 18, 7), 'tube'))
    translucent.append(box('bong water', (11.75, 16.1, 4.25), (14.25, 17.5, 6.75), 'water'))
    translucent.append(box('bong tube', (12.5, 18, 5), (13.5, 25, 6), 'tube'))
    translucent.append(box('bong mouthpiece', (12.2, 24.5, 4.7), (13.8, 25.5, 6.3), 'tube'))
    solid.append(box('bong stem', (13.5, 18.8, 5.25), (14.5, 19.3, 5.75), 'clay'))
    solid.append(box('bong bowl', (14, 19.3, 4.9), (15, 20.3, 6.1), 'clay'))

    model = {
        'credit': 'FourTwenty tools/generate_dispensary_hut_model.py',
        'parent': 'block/block',
        'ambientocclusion': False,
        'loader': 'neoforge:composite',
        'textures': {'particle': 'minecraft:block/dark_oak_planks'},
        # Two blocks tall: shrink the item so it fits inventory slots and hands.
        'display': {
            'head': {'translation': [0, 10.5, 0]},
            'gui': {'rotation': [30, 225, 0], 'translation': [0, -3.5, 0], 'scale': [0.42, 0.42, 0.42]},
            'ground': {'translation': [0, 2, 0], 'scale': [0.2, 0.2, 0.2]},
            'fixed': {'translation': [0, -3, 0], 'scale': [0.4, 0.4, 0.4]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 1.5, 1], 'scale': [0.28, 0.28, 0.28]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, -1, 0], 'scale': [0.3, 0.3, 0.3]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, -1, 0], 'scale': [0.3, 0.3, 0.3]},
        },
        'children': {
            'solid': {
                'render_type': 'minecraft:solid',
                'textures': {
                    'floor': 'minecraft:block/smooth_stone', 'wall': 'minecraft:block/quartz_block_bottom',
                    'wood': 'minecraft:block/dark_oak_planks', 'top': 'minecraft:block/polished_andesite',
                    'cross': 'minecraft:block/lime_concrete', 'metal': 'minecraft:block/iron_block',
                    'screen': 'minecraft:block/black_concrete',
                    'lid': 'minecraft:block/spruce_planks', 'clay': 'minecraft:block/terracotta',
                    'kick': 'minecraft:block/black_concrete', 'keys': 'minecraft:block/light_gray_concrete',
                },
                'elements': solid,
            },
            'cutout': {
                'render_type': 'minecraft:cutout',
                'textures': {'bud': 'fourtwenty:item/weed_bud'},
                'elements': cutout,
            },
            'translucent': {
                'render_type': 'minecraft:translucent',
                'textures': {'glass': 'minecraft:block/glass', 'water': 'minecraft:block/light_blue_stained_glass',
                             'jar': 'minecraft:block/lime_stained_glass', 'tube': 'minecraft:block/white_stained_glass'},
                'elements': translucent,
            },
        },
    }
    MODEL.write_text(json.dumps(model, indent=2) + '\n')
    print(f'wrote {MODEL.relative_to(ROOT)}: {len(solid)} solid, {len(cutout)} cutout, {len(translucent)} translucent elements')


if __name__ == '__main__':
    generate()
