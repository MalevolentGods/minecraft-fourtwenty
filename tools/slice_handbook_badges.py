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


def main():
    sheet = Image.open(SHEET).convert('RGB')
    OUT.mkdir(parents=True, exist_ok=True)
    big = 4
    for badge, (cx, cy) in zip(ORDER, CENTRES):
        crop = sheet.crop((cx - RADIUS - 2, cy - RADIUS - 2, cx + RADIUS + 2, cy + RADIUS + 2)).convert('RGBA')
        mask = Image.new('L', (crop.width * big, crop.height * big), 0)
        d = ImageDraw.Draw(mask)
        m = 6 * big
        d.ellipse((m, m, mask.width - m, mask.height - m), fill=255)
        mask = mask.resize(crop.size, Image.LANCZOS)
        crop.putalpha(mask)
        crop.resize((SIZE, SIZE), Image.LANCZOS).save(OUT / f'{badge}.png', optimize=True)
        print(badge)


if __name__ == '__main__':
    main()
