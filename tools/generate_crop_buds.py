#!/usr/bin/env python3
"""The buds on a flowering weed crop, in the strain's colour.

    python3 tools/generate_crop_buds.py

Lifts the bud fill out of the last two crop stages into a grey overlay, leaving the ink and the pistils on the plant below, and writes a cross model
with the overlay on tintindex 0. client/StrainTint.java colours it from the block state.
"""
import colorsys
import json
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/fourtwenty'
SCALE = 1024 / 480
# (x, y, rx, ry) round each bud, in the 480 wide art
BUDS = {6: [(240, 100, 42, 42), (140, 215, 36, 40), (335, 170, 36, 36)],
        7: [(235, 125, 62, 85), (150, 225, 50, 70), (345, 175, 52, 62)]}


def on_bud(x, y, stage):
    return any(((x / SCALE - cx) / rx) ** 2 + ((y / SCALE - cy) / ry) ** 2 <= 1 for cx, cy, rx, ry in BUDS[stage])


def overlay(stage):
    im = Image.open(ASSETS / f'textures/block/weed_crop_stage{stage}.png').convert('RGBA')
    px = im.load()
    fill = [(x, y) for y in range(im.height) for x in range(im.width) if px[x, y][3] >= 128 and on_bud(x, y, stage) and is_bud(px[x, y])]
    bright = sorted(px[p][1] for p in fill)[len(fill) * 99 // 100]
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    grey = out.load()
    for p in fill:
        v = min(255, px[p][1] * 255 // bright)
        grey[p] = (v, v, v, 255)
    out.save(ASSETS / f'textures/block/weed_crop_buds_stage{stage}.png', optimize=True)
    return len(fill), bright


def is_bud(rgba):
    h, _, v = colorsys.rgb_to_hsv(rgba[0] / 255, rgba[1] / 255, rgba[2] / 255)
    return 0.17 < h < 0.245 and v > 0.45


def cross(tex, tint):
    face = {'uv': [0, 0, 16, 16], 'texture': tex}
    if tint:
        face = {**face, 'tintindex': 0}
    return [{'from': [0.8, 0, 8], 'to': [15.2, 16, 8], 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45, 'rescale': True}, 'shade': False,
             'faces': {'north': face, 'south': face}},
            {'from': [8, 0, 0.8], 'to': [8, 16, 15.2], 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45, 'rescale': True}, 'shade': False,
             'faces': {'west': face, 'east': face}}]


def main():
    for stage in BUDS:
        print(stage, overlay(stage))
        model = {'ambientocclusion': False, 'render_type': 'cutout',
                 'textures': {'particle': f'fourtwenty:block/weed_crop_stage{stage}', 'cross': f'fourtwenty:block/weed_crop_stage{stage}',
                              'buds': f'fourtwenty:block/weed_crop_buds_stage{stage}'},
                 'elements': cross('#cross', False) + cross('#buds', True)}
        (ASSETS / f'models/block/weed_crop_stage{stage}.json').write_text(json.dumps(model, indent=2) + '\n')


if __name__ == '__main__':
    main()
