#!/usr/bin/env python3
"""The Bud Barn hut block as a MineColonies-style diorama: a small open barn, two blocks tall, with a drying rack
hung with buds, a trim table and a hay bale, and the bud logo on a board above the door.

Same construction as the Dispensary's (tools/generate_dispensary_hut_model.py): a composite block model of small boxes
split into solid / cutout children. The front faces north (the blockstate turns it to the placer).

Writes src/main/resources/assets/fourtwenty/models/block/budbarn.json. Run again after edits.
"""
import json
from pathlib import Path

from generate_dispensary_hut_model import box

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / 'src/main/resources/assets/fourtwenty/models/block/budbarn.json'
HAY = {'up': 'hay_top', 'down': 'hay_top'}


def generate():
    solid, cutout = [], []

    # --- Bottom block: an open barn frame standing on the ground (no base plate). ----------------
    solid.append(box('back wall', (1, 0, 12), (15, 17, 14), 'wall'))
    solid.append(box('left wall', (1, 0, 2), (2.5, 17, 12), 'plank'))
    solid.append(box('right wall', (13.5, 0, 2), (15, 17, 12), 'plank'))
    # posts a hair deeper than the walls they stand against, so no two faces share a plane
    for i, (x, z, d) in enumerate(((0.5, 0.5, 1.5), (14, 0.5, 1.5), (0.5, 12.5, 1.6), (14, 12.5, 1.6))):
        solid.append(box(f'post {i}', (x, 0, z), (x + 1.5, 18, z + d), 'beam'))
    # The sign over the open front, with the bud logo.
    solid.append(box('sign board', (3, 11.5, 0.5), (13, 16, 1.5), 'plank'))
    cutout.append({'name': 'bud logo', 'from': [5.5, 11.9, 0.45], 'to': [10.5, 15.6, 0.45], 'shade': False,
                   'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#bud'}}})

    # --- The roof: a stepped gable running front to back, into the block above. -----------------
    solid.append(box('roof 1', (0, 17, 0), (16, 19, 14), 'roof'))
    solid.append(box('roof 2', (2, 19, 0.5), (14, 21, 13.5), 'roof'))
    solid.append(box('roof 3', (4, 21, 1), (12, 23, 13), 'roof'))
    solid.append(box('roof 4', (6, 23, 1.5), (10, 25, 12.5), 'roof'))
    solid.append(box('ridge', (7.25, 25, 1.5), (8.75, 26, 12.5), 'beam'))
    solid.append(box('gable', (4, 19, 12), (12, 22, 13.9), 'wall'))

    # --- Inside: a drying rack of hanging buds. --------------------------------------------------
    solid.append(box('rack rail', (2.5, 14, 6), (13.5, 15, 7), 'beam'))
    for i, x in enumerate((3.5, 6, 8.5, 11)):
        solid.append(box(f'string {i}', (x + 0.3, 11, 6.4), (x + 0.7, 14, 6.6), 'string'))
        solid.append(box(f'hung bud {i}', (x - 0.4, 7.5, 6), (x + 1.4, 11, 7), 'bud_block'))
    # A hay bale against the back wall.
    solid.append(box('hay bale', (2.5, 0, 9), (7.5, 6, 12), 'hay', faces=HAY))
    # A trim table with buds on it.
    solid.append(box('table top', (9, 7, 8), (14, 8, 12), 'plank'))
    for i, (x, z) in enumerate(((9.2, 8.2), (12.8, 8.2), (9.2, 11.2), (12.8, 11.2))):
        solid.append(box(f'table leg {i}', (x, 0, z), (x + 0.8, 7, z + 0.6), 'beam'))
    solid.append(box('table buds 1', (9.8, 8, 9), (11.6, 9.2, 10.6), 'bud_block'))
    solid.append(box('table buds 2', (12, 8, 9.8), (13.2, 8.9, 11), 'bud_block'))

    model = {
        'credit': 'FourTwenty tools/generate_budbarn_hut_model.py',
        'parent': 'block/block',
        'ambientocclusion': False,
        'loader': 'neoforge:composite',
        'textures': {'particle': 'minecraft:block/dark_oak_planks'},
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
                    'wall': 'minecraft:block/red_terracotta',
                    'plank': 'minecraft:block/spruce_planks', 'beam': 'minecraft:block/dark_oak_log',
                    'roof': 'minecraft:block/dark_oak_planks', 'string': 'minecraft:block/white_wool',
                    'bud_block': 'minecraft:block/moss_block', 'hay': 'minecraft:block/hay_block_side',
                    'hay_top': 'minecraft:block/hay_block_top',
                },
                'elements': solid,
            },
            'cutout': {
                'render_type': 'minecraft:cutout',
                'textures': {'bud': 'fourtwenty:item/weed_bud'},
                'elements': cutout,
            },
        },
    }
    MODEL.write_text(json.dumps(model, indent=2) + '\n')
    print(f'wrote {MODEL.relative_to(ROOT)}: {len(solid)} solid, {len(cutout)} cutout elements')


if __name__ == '__main__':
    generate()
