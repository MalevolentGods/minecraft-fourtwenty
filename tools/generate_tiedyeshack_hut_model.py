#!/usr/bin/env python3
"""The Tie Dye Shack hut block as a MineColonies-style diorama (specs/018-tie-dye-shack research R10): a little plank shed with a
dye vat out front, and on top a drying line hung with tie-dyed cloths in rainbow wool.

Same construction as the Weed College's (tools/generate_weedcollege_hut_model.py): a composite block model of small boxes. The front
faces north (the blockstate turns it to the placer).

Writes src/main/resources/assets/fourtwenty/models/block/tiedyeshack.json. Run again after edits.
"""
import json
from pathlib import Path

from generate_dispensary_hut_model import box
from hut_model_check import check, strip_hidden

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / 'src/main/resources/assets/fourtwenty/models/block/tiedyeshack.json'
CLOTHS = ['red', 'orange', 'yellow', 'lime', 'light_blue', 'purple', 'magenta']


def generate():
    solid = []

    # --- Bottom block: the shed. -------------------------------------------------------------------
    solid.append(box('floor', (1, 0, 2), (15, 1, 15), 'floor'))
    solid.append(box('back wall', (1.5, 1, 12.5), (14.5, 11, 14.5), 'plank', skip=('down',)))
    solid.append(box('left wall', (1.5, 1, 6), (3.5, 11, 12.5), 'plank', skip=('down',)))
    solid.append(box('right wall', (12.5, 1, 6), (14.5, 11, 12.5), 'plank', skip=('down',)))
    for i, (x, z) in enumerate(((1, 5.5), (13, 5.5), (1, 12), (13, 12))):
        solid.append(box(f'post {i}', (x, 1, z), (x + 2, 12, z + 3), 'log', skip=('down',)))
    solid.append(box('roof', (0.5, 12, 4.5), (15.5, 13.5, 15.5), 'roof'))
    solid.append(box('awning', (2, 10.5, 2.5), (14, 11.5, 5.5), 'awning'))
    # the dye vat out front, full of dye
    solid.append(box('vat', (5, 1, 2.5), (11, 6, 7), 'vat'))
    solid.append(box('dye', (5.6, 5, 3.1), (10.4, 5.6, 6.4), 'dye', skip=('down',)))
    # a stirring stick leaning in the vat
    solid.append(box('stick', (9, 5.6, 4.4), (9.6, 10, 5), 'log'))
    # a folded stack of cloth on the floor inside
    for i, colour in enumerate(('red', 'yellow', 'light_blue')):
        solid.append(box(f'folded {colour}', (4.5, 1 + i, 8.5), (8, 2 + i, 11.5), colour))

    # --- Top block: the drying line with tie-dyed cloths. --------------------------------------------
    solid.append(box('line post left', (1.5, 13.5, 8), (2.5, 27, 9), 'log'))
    solid.append(box('line post right', (13.5, 13.5, 8), (14.5, 27, 9), 'log'))
    solid.append(box('line', (2.5, 26, 8.3), (13.5, 26.4, 8.7), 'rope'))
    for i, colour in enumerate(CLOTHS):
        x = 2.8 + i * 1.55
        drop = 6 + (i % 3)
        solid.append(box(f'cloth {colour}', (x, 26 - drop, 8.2), (x + 1.45, 26, 8.8), colour))

    strip_hidden(solid)
    check(solid)           # no two faces in one plane: that is z-fighting in the game

    textures = {
        'floor': 'minecraft:block/spruce_planks', 'plank': 'minecraft:block/birch_planks', 'log': 'minecraft:block/stripped_dark_oak_log',
        'roof': 'minecraft:block/dark_oak_planks', 'awning': 'minecraft:block/white_wool', 'vat': 'minecraft:block/iron_block',
        'dye': 'minecraft:block/purple_concrete', 'rope': 'minecraft:block/white_wool',
    }
    for colour in set(CLOTHS) | {'red', 'yellow', 'light_blue'}:
        textures[colour] = f'minecraft:block/{colour}_wool'
    model = {
        'credit': 'FourTwenty tools/generate_tiedyeshack_hut_model.py',
        'parent': 'block/block',
        'ambientocclusion': False,
        'loader': 'neoforge:composite',
        'textures': {'particle': 'minecraft:block/birch_planks'},
        'display': {
            'head': {'translation': [0, 10.5, 0]},
            'gui': {'rotation': [30, 225, 0], 'translation': [0, -3.5, 0], 'scale': [0.42, 0.42, 0.42]},
            'ground': {'translation': [0, 2, 0], 'scale': [0.2, 0.2, 0.2]},
            'fixed': {'translation': [0, -3, 0], 'scale': [0.4, 0.4, 0.4]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 1.5, 1], 'scale': [0.28, 0.28, 0.28]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, -1, 0], 'scale': [0.3, 0.3, 0.3]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, -1, 0], 'scale': [0.3, 0.3, 0.3]},
        },
        'children': {'solid': {'render_type': 'minecraft:solid', 'textures': textures, 'elements': solid}},
    }
    MODEL.write_text(json.dumps(model, indent=2) + '\n')
    print(f'wrote {MODEL.relative_to(ROOT)}: {len(solid)} elements')


if __name__ == '__main__':
    generate()
