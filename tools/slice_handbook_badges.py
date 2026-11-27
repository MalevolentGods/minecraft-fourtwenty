#!/usr/bin/env python3
"""Cuts the user's sheet of 14 embroidered badge pictures (tools/art/handbook_badges.png, made with an image generator from the
prompt in specs/016-weedcraft-handbook) into one round transparent patch per badge: textures/gui/handbook/patches/<badge>.png (128x128),
and the sheet of six rank shields (tools/art/handbook_ranks.png, left to right Tenderbud to Golden Eagle) into
textures/gui/handbook/ranks/<rank>.png (128x128, the shield centred on a transparent square).

    python3 tools/slice_handbook_badges.py

The patches show large in the book, on the sash and in the PDF. Reading order of the sheet is the badges' order in the book.
"""
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SHEET = ROOT / 'tools/art/handbook_badges.png'
OUT = ROOT / 'src/main/resources/assets/fourtwenty/textures/gui/handbook/patches'
SIZE = 128
ITEM = ROOT / 'src/main/resources/assets/fourtwenty/textures/item'
ICONS = [(ROOT / 'tools/art/stoner_badge_sash.png', ITEM / 'stoner_badge_sash.png', 0),     # the user's painted item icons
         (ROOT / 'tools/art/handbook_icon.png', ITEM / 'handbook.png', 30)]                  # (picture, icon, degrees clockwise)

ORDER = ['ganja_gardening', 'plant_breeding', 'bud_processing', 'kitchen_chemistry', 'munchies',
         'safe_sesh', 'colour_theory', 'terpination', 'inspiration', 'inspoweednetics',
         'bits_and_bytes', 'hempcraft', 'community_sesh', 'lifted_building']
# centres measured on the 1536x1024 sheet: three rows of 5, 5 and 4
CENTRES = [(165, 168), (475, 168), (773, 168), (1071, 168), (1373, 168),
           (165, 497), (475, 497), (771, 497), (1069, 497), (1376, 497),
           (285, 823), (605, 823), (925, 823), (1247, 823)]
RANK_SHEET = ROOT / 'tools/art/handbook_ranks.png'
RANK_OUT = ROOT / 'src/main/resources/assets/fourtwenty/textures/gui/handbook/ranks'
RANKS = ['tenderbud', 'second_toke', 'first_hit', 'starseed', 'high_life', 'golden_eagle']
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


def cut_out(im):
    """Alpha for one of the user's pictures: the off-white background flooded away from the edges, softened by a pixel."""
    import numpy as np
    from scipy import ndimage
    a = np.asarray(im.convert('RGB')).astype(int)
    background = np.median(a[:8, :8].reshape(-1, 3), axis=0)
    near = np.abs(a - background).sum(axis=2) < 45
    lab, _ = ndimage.label(near)
    edge_labels = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    outside = np.isin(lab, list(edge_labels))
    return ~outside


def rank_shields():
    """Each shield on the rank sheet, left to right: the largest pieces of not-background, cut out and centred on a square."""
    import numpy as np
    from scipy import ndimage
    im = Image.open(RANK_SHEET).convert('RGB')
    inside = ndimage.binary_fill_holes(cut_out(im))
    lab, n = ndimage.label(inside)
    sizes = ndimage.sum(inside, lab, range(1, n + 1))
    boxes = ndimage.find_objects(lab)
    keep = sorted((int(l) for l in np.argsort(sizes)[-len(RANKS):] + 1), key=lambda l: boxes[l - 1][1].start)
    RANK_OUT.mkdir(parents=True, exist_ok=True)
    for rank, l in zip(RANKS, keep):
        ys, xs = boxes[l - 1]
        mask = Image.fromarray(((lab == l) * 255).astype('uint8')).filter(ImageFilter.GaussianBlur(1.2))
        shield = im.convert('RGBA')
        shield.putalpha(mask)
        shield = shield.crop((xs.start, ys.start, xs.stop, ys.stop))
        side = int(max(shield.size) * 1.02)
        square = Image.new('RGBA', (side, side), (0, 0, 0, 0))
        square.alpha_composite(shield, ((side - shield.width) // 2, (side - shield.height) // 2))
        square.resize((SIZE, SIZE), Image.LANCZOS).save(RANK_OUT / f'{rank}.png', optimize=True)
        print('rank', rank, xs.start, ys.start, xs.stop - xs.start, ys.stop - ys.start)


def painted_icon(art, out, turn=0):
    """An item icon from one of the user's pictures: the off-white background flooded away from the edges, cropped, centred on a
    square and saved at 256x256 like the mod's other painted icons."""
    import numpy as np
    from scipy import ndimage
    im = Image.open(art).convert('RGB')
    a = np.asarray(im).astype(int)
    background = np.median(a[:8, :8].reshape(-1, 3), axis=0)
    near = np.abs(a - background).sum(axis=2) < 45
    lab, _ = ndimage.label(near)
    edge_labels = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    outside = np.isin(lab, list(edge_labels))
    alpha = Image.fromarray(np.where(outside, 0, 255).astype('uint8')).filter(ImageFilter.GaussianBlur(1.2))
    rgba = im.convert('RGBA')
    rgba.putalpha(alpha)
    if turn:                                  # degrees clockwise
        rgba = rgba.rotate(-turn, resample=Image.BICUBIC, expand=True)
    rgba = rgba.crop(rgba.getbbox())
    side = int(max(rgba.size) * 1.04)
    square = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    square.alpha_composite(rgba, ((side - rgba.width) // 2, (side - rgba.height) // 2))
    icon = square.resize((256, 256), Image.LANCZOS)
    # items are drawn with hard edges: every pixel fully solid or fully clear, or a soft rim shows as see-through specks
    icon.putalpha(icon.getchannel('A').point(lambda v: 255 if v >= 128 else 0))
    icon.save(out, optimize=True)
    print('icon', out.name)


def main():
    for art, out, turn in ICONS:
        painted_icon(art, out, turn)
    rank_shields()
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
