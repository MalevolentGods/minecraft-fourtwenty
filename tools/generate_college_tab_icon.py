#!/usr/bin/env python3
"""The Weed College Board tab's icon (specs/017-weed-college US2): a little chalkboard in a wooden frame with a bud drawn on it and
chalk lines either side, like the board over the College's door. 32x32, pixel art.

Writes src/main/resources/assets/fourtwenty/textures/gui/college_board_tab.png. Run again after edits.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/fourtwenty/textures/gui/college_board_tab.png'
BUD = ROOT / 'src/main/resources/assets/fourtwenty/textures/item/weed_bud.png'

FRAME, FRAME_DARK, FRAME_LIGHT = (124, 84, 48, 255), (86, 56, 30, 255), (160, 116, 70, 255)
BOARD, BOARD_LIGHT = (38, 52, 44, 255), (52, 70, 58, 255)
CHALK, CHALK_DIM = (232, 232, 224, 255), (176, 182, 170, 255)


def generate():
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = img.load()
    # the frame, with a light top-left edge and a dark bottom-right one
    for x in range(2, 30):
        for y in range(5, 27):
            px[x, y] = FRAME
    for x in range(2, 30):
        px[x, 5] = FRAME_LIGHT
        px[x, 26] = FRAME_DARK
    for y in range(5, 27):
        px[2, y] = FRAME_LIGHT
        px[29, y] = FRAME_DARK
    # the slate, a touch lighter at the top where the light catches it
    for x in range(4, 28):
        for y in range(7, 25):
            px[x, y] = BOARD_LIGHT if y < 9 else BOARD
    # chalk lines either side of the bud
    for x0, x1, y in ((6, 10, 10), (6, 9, 13), (6, 10, 16), (22, 26, 10), (23, 26, 13), (22, 25, 16)):
        for x in range(x0, x1):
            px[x, y] = CHALK if (x + y) % 3 else CHALK_DIM
    # the chalk ledge and a stub of chalk on it
    for x in range(5, 27):
        px[x, 25] = FRAME_DARK
    px[20, 24], px[21, 24] = CHALK, CHALK
    # the bud, scaled down into the middle of the board
    bud = Image.open(BUD).convert('RGBA').resize((12, 12), Image.NEAREST)
    img.alpha_composite(bud, (10, 9))
    img.save(OUT)
    print(f'wrote {OUT.relative_to(ROOT)}')


if __name__ == '__main__':
    generate()
