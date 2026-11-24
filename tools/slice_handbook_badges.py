#!/usr/bin/env python3
"""Cuts the user's sheet of 14 embroidered badge pictures (tools/art/handbook_badges.png, made with an image generator from the
prompt in specs/016-weedcraft-handbook) into one round transparent patch per badge: textures/gui/handbook/patches/<badge>.png (128x128).

    python3 tools/slice_handbook_badges.py

The patches show large in the book, on the sash and in the PDF. Reading order of the sheet is the badges' order in the book.
"""
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SHEET = ROOT / 'tools/art/handbook_badges.png'
OUT = ROOT / 'src/main/resources/assets/fourtwenty/textures/gui/handbook/patches'
SIZE = 128

ORDER = ['ganja_gardening', 'plant_breeding', 'bud_processing', 'kitchen_chemistry', 'munchies',
         'safe_sesh', 'colour_theory', 'terpination', 'inspiration', 'inspoweednetics',
         'bits_and_bytes', 'hempcraft', 'community_sesh', 'lifted_building']
# centres measured on the 1536x1024 sheet: three rows of 5, 5 and 4
CENTRES = [(165, 168), (475, 168), (773, 168), (1071, 168), (1373, 168),
           (165, 497), (475, 497), (771, 497), (1069, 497), (1376, 497),
           (285, 823), (605, 823), (925, 823), (1247, 823)]
RADIUS = 150
INSET = 3          # pixels trimmed inside each badge's own edge


def circles(sheet):
    """Each badge's true centre and radius: the deepest point of the not-background area near each rough centre, and how deep it is."""
    import numpy as np
    from scipy import ndimage
    a = np.asarray(sheet).astype(int)
    background = np.median(a[:8, :8].reshape(-1, 3), axis=0)
    cloth = ndimage.binary_fill_holes(np.abs(a - background).sum(axis=2) > 40)
    depth = ndimage.distance_transform_edt(cloth)
    out = []
    for cx, cy in CENTRES:
        win = depth[cy - 60:cy + 60, cx - 60:cx + 60]
        y, x = np.unravel_index(np.argmax(win), win.shape)
        out.append((cx - 60 + x, cy - 60 + y, float(win[y, x])))
    return out


def main():
    sheet = Image.open(SHEET).convert('RGB')
    OUT.mkdir(parents=True, exist_ok=True)
    big = 4
    for badge, (cx, cy, depth) in zip(ORDER, circles(sheet)):
        r = int(depth) - INSET                     # inside the stitched rim, clear of the sheet's background
        crop = sheet.crop((cx - r, cy - r, cx + r, cy + r)).convert('RGBA')
        mask = Image.new('L', (crop.width * big, crop.height * big), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, mask.width - 1, mask.height - 1), fill=255)
        mask = mask.resize(crop.size, Image.LANCZOS)
        crop.putalpha(mask)
        crop.resize((SIZE, SIZE), Image.LANCZOS).save(OUT / f'{badge}.png', optimize=True)
        print(badge, cx, cy, r)


if __name__ == '__main__':
    main()
