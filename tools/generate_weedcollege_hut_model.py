#!/usr/bin/env python3
"""The Weed College hut block as a MineColonies-style diorama (specs/017-weed-college FR-002, research R12): a little
mud-brick schoolhouse, two blocks tall, with a chalkboard over the door and a glass greenhouse on its roof with a weed
plant growing inside.

Same construction as the Dispensary's and the Bud Barn's (tools/generate_dispensary_hut_model.py): a composite block model
of small boxes split into solid / cutout children. The front faces north (the blockstate turns it to the placer).

Writes src/main/resources/assets/fourtwenty/models/block/weedcollege.json. Run again after edits.
"""
import json
from pathlib import Path

from generate_dispensary_hut_model import box
from hut_model_check import check, strip_hidden

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / 'src/main/resources/assets/fourtwenty/models/block/weedcollege.json'


def generate():
    solid, cutout = [], []

    # --- Bottom block: the schoolhouse. -----------------------------------------------------------
    solid.append(box('walls', (1, 0, 3), (15, 14, 14), 'wall'))
    for i, (x, z) in enumerate(((0.5, 2.5), (13.5, 2.5), (0.5, 12.5), (13.5, 12.5))):
        solid.append(box(f'corner post {i}', (x, 0, z), (x + 2, 15.5, z + 1.9), 'beam'))
    solid.append(box('eave band', (0.8, 14, 2.8), (15.2, 15, 14.2), 'plank'))
    # the door, the user's signature: stripped spruce in a diorite frame
    solid.append(box('door frame', (5.8, 0, 2.55), (10.2, 6.8, 3), 'frame'))
    solid.append(box('door', (6.5, 0, 2.4), (9.5, 6.2, 2.9), 'door'))
    solid.append(box('step', (5.5, 0, 1.2), (10.5, 0.8, 2.35), 'frame'))
    # the chalkboard over the door, with chalk on it and a bud drawn in the middle
    solid.append(box('chalkboard', (3.5, 7.5, 2.7), (12.5, 12.5, 3), 'board'))
    for i, (x1, x2, y, z) in enumerate(((4.3, 6.8, 11.3, 2.66), (4.3, 6.2, 10.2, 2.64), (9.4, 11.7, 11.3, 2.62), (10, 11.7, 9.1, 2.6))):
        solid.append(box(f'chalk {i}', (x1, y, z), (x2, y + 0.35, z + 0.05), 'chalk'))
    cutout.append({'name': 'bud on the board', 'from': [6.6, 7.9, 2.65], 'to': [9.4, 12.1, 2.65], 'shade': False,
                   'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#bud'}}})
    # windows on the sides
    for i, (x, skip) in enumerate(((0.9, ('east',)), (14.6, ('west',)))):
        solid.append(box(f'window {i}', (x, 5, 6), (x + 0.5, 10, 11), 'pane', skip=skip))

    # --- Top block: the greenhouse on the roof. ----------------------------------------------------
    solid.append(box('greenhouse floor', (1.5, 15, 3.5), (14.5, 16, 13.5), 'floor'))
    solid.append(box('plant pot', (6, 16, 7), (10, 18, 10.5), 'pot'))
    cutout.append({'name': 'weed plant x', 'from': [6, 18, 8.75], 'to': [10, 25, 8.75], 'shade': False,
                   'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#plant'}, 'south': {'uv': [0, 0, 16, 16], 'texture': '#plant'}}})
    cutout.append({'name': 'weed plant z', 'from': [8, 18, 6.75], 'to': [8, 25, 10.75], 'shade': False,
                   'faces': {'east': {'uv': [0, 0, 16, 16], 'texture': '#plant'}, 'west': {'uv': [0, 0, 16, 16], 'texture': '#plant'}}})
    # Tinted glass (translucent), so the greenhouse reads as glass and not as empty air (the user, 2026-11-27): walls up to a
    # timber frame, then a stepped glass roof that the ridge beam rests on.
    translucent = []
    translucent.append(box('greenhouse glass', (2, 16, 4), (14, 23, 13), 'glass', skip=('down', 'up')))
    for i, (x, z) in enumerate(((1.7, 3.7), (13.6, 3.7), (1.7, 12.6), (13.6, 12.6))):
        solid.append(box(f'greenhouse post {i}', (x, 16, z), (x + 0.7, 23, z + 0.7), 'beam'))
    solid.append(box('frame front', (1.7, 23, 3.7), (14.3, 23.7, 4.4), 'beam'))
    solid.append(box('frame back', (1.7, 23, 12.6), (14.3, 23.7, 13.3), 'beam'))
    solid.append(box('frame left', (1.7, 23, 4.4), (2.4, 23.7, 12.6), 'beam'))
    solid.append(box('frame right', (13.6, 23, 4.4), (14.3, 23.7, 12.6), 'beam'))
    translucent.append(box('roof glass 1', (2.4, 23.7, 4.4), (13.6, 24.7, 12.6), 'glass', skip=('down',)))
    translucent.append(box('roof glass 2', (4, 24.7, 4.6), (12, 25.7, 12.4), 'glass', skip=('down',)))
    translucent.append(box('roof glass 3', (5.6, 25.7, 4.8), (10.4, 26.7, 12.2), 'glass', skip=('down',)))
    solid.append(box('ridge beam', (7.4, 26.7, 4.3), (8.6, 27.4, 12.7), 'beam'))

    everything = solid + cutout + translucent
    strip_hidden(everything)
    check(everything)           # no two faces in one plane: that is z-fighting in the game

    model = {
        'credit': 'FourTwenty tools/generate_weedcollege_hut_model.py',
        'parent': 'block/block',
        'ambientocclusion': False,
        'loader': 'neoforge:composite',
        'textures': {'particle': 'minecraft:block/mud_bricks'},
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
                    'wall': 'minecraft:block/mud_bricks', 'beam': 'minecraft:block/dark_oak_log',
                    'plank': 'minecraft:block/spruce_planks', 'frame': 'minecraft:block/polished_diorite',
                    'door': 'minecraft:block/stripped_spruce_log', 'board': 'minecraft:block/black_concrete',
                    'chalk': 'minecraft:block/white_concrete', 'pane': 'minecraft:block/light_blue_stained_glass',
                    'floor': 'minecraft:block/stripped_spruce_log', 'pot': 'minecraft:block/terracotta',
                },
                'elements': solid,
            },
            'cutout': {
                'render_type': 'minecraft:cutout',
                'textures': {'bud': 'fourtwenty:item/weed_bud', 'plant': 'fourtwenty:block/weed_crop_stage2'},
                'elements': cutout,
            },
            'translucent': {
                'render_type': 'minecraft:translucent',
                'textures': {'glass': 'minecraft:block/light_blue_stained_glass'},
                'elements': translucent,
            },
        },
    }
    MODEL.write_text(json.dumps(model, indent=2) + '\n')
    print(f'wrote {MODEL.relative_to(ROOT)}: {len(solid)} solid, {len(cutout)} cutout, {len(translucent)} translucent elements')


if __name__ == '__main__':
    generate()
