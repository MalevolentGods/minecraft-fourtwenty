#!/usr/bin/env python3
"""The Handbook's art (specs/016-weedcraft-handbook): the open book, its page arrows and ribbon, and the item.

    python3 tools/generate_handbook_art.py

Writes textures/gui/handbook/book.png (a 512x256 sheet: the open spread at 0,0 is 360x230; arrows and the back
button below it), textures/gui/handbook/cover.png (the user's cover art, tools/art/handbook_cover.png, fitted), and
textures/item/handbook.png. The spread is drawn, not painted: leather with stitching, two cream pages with a soft gutter,
gold corner flourishes and a Kind Bud Green ribbon, so it can be regenerated and restyled.
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
GUI = ROOT / 'src/main/resources/assets/fourtwenty/textures/gui/handbook'
ITEM = ROOT / 'src/main/resources/assets/fourtwenty/textures/item'
COVER = ROOT / 'tools/art/handbook_cover.png'

W, H = 540, 345            # the spread, in book units (drawn at a whole number of screen pixels each)
LEATHER = (38, 58, 36)
LEATHER_DARK = (24, 38, 23)
STITCH = (196, 170, 104)
PAPER = (244, 234, 208)
PAPER_EDGE = (226, 212, 178)
GOLD = (214, 178, 84)
GOLD_DARK = (150, 116, 44)
RIBBON = (124, 197, 64)     # Kind Bud Green
RIBBON_DARK = (78, 140, 38)
INK = (58, 46, 32)

rng = random.Random(420)


def noise(img, box, amount, seed):
    r = random.Random(seed)
    px = img.load()
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = px[x, y]
            d = r.randint(-amount, amount)
            px[x, y] = (max(0, min(255, c[0] + d)), max(0, min(255, c[1] + d)), max(0, min(255, c[2] + d)), c[3])


def flourish(d, x, y, sx, sy):
    """A small gold corner ornament: an L of two lines with a leaf at the bend, mirrored by sx, sy."""
    for i in range(14):
        d.point((x + sx * i, y), fill=GOLD)
        d.point((x, y + sy * i), fill=GOLD)
    for i in range(8):
        d.point((x + sx * (i + 3), y + sy * 3), fill=GOLD_DARK)
        d.point((x + sx * 3, y + sy * (i + 3)), fill=GOLD_DARK)
    # a tiny five-point leaf at the corner
    cx, cy = x + sx * 6, y + sy * 6
    for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (sx * 2, sy * 2), (sx, sy)):
        d.point((cx + dx, cy + dy), fill=RIBBON)


def spread():
    img = Image.new('RGBA', (1024, 512), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # leather boards, rounded
    d.rounded_rectangle((0, 0, W - 1, H - 1), radius=8, fill=LEATHER + (255,))
    noise(img, (0, 0, W, H), 6, 1)
    d.rounded_rectangle((0, 0, W - 1, H - 1), radius=8, outline=LEATHER_DARK + (255,), width=2)
    for x in range(10, W - 10, 4):                      # stitching
        d.point((x, 4), fill=STITCH)
        d.point((x, H - 5), fill=STITCH)
    for y in range(10, H - 10, 4):
        d.point((4, y), fill=STITCH)
        d.point((W - 5, y), fill=STITCH)
    # page stack edges, then the two pages
    for k in range(3, 0, -1):
        d.rectangle((8 - k, 8 + k, W // 2 - 1, H - 9 + k), fill=PAPER_EDGE + (255,))
        d.rectangle((W // 2, 8 + k, W - 9 + k, H - 9 + k), fill=PAPER_EDGE + (255,))
    d.rectangle((8, 8, W // 2 - 1, H - 10), fill=PAPER + (255,))
    d.rectangle((W // 2, 8, W - 9, H - 10), fill=PAPER + (255,))
    noise(img, (8, 8, W - 8, H - 9), 3, 2)
    # the gutter's soft shadow
    shade = Image.new('L', (W, H), 0)
    sd = ImageDraw.Draw(shade)
    for i in range(14):
        a = int(70 * (1 - i / 14) ** 2)
        sd.line((W // 2 - i, 8, W // 2 - i, H - 10), fill=a)
        sd.line((W // 2 + i, 8, W // 2 + i, H - 10), fill=a)
    dark = Image.new('RGBA', (W, H), (60, 44, 20, 255))
    img.paste(dark, (0, 0), shade)
    d = ImageDraw.Draw(img)
    # gold corners
    flourish(d, 13, 13, 1, 1)
    flourish(d, W // 2 - 6, 13, -1, 1)
    flourish(d, 13, H - 15, 1, -1)
    flourish(d, W // 2 - 6, H - 15, -1, -1)
    flourish(d, W // 2 + 5, 13, 1, 1)
    flourish(d, W - 14, 13, -1, 1)
    flourish(d, W // 2 + 5, H - 15, 1, -1)
    flourish(d, W - 14, H - 15, -1, -1)
    # the ribbon: a tail hanging out below the gutter, clear of the text
    rx = W // 2 + 6
    d.rectangle((rx, H - 12, rx + 7, H + 20), fill=RIBBON + (255,))
    d.line((rx + 7, H - 12, rx + 7, H + 20), fill=RIBBON_DARK + (255,))
    d.polygon([(rx, H + 21), (rx + 7, H + 21), (rx + 3, H + 16)], fill=(0, 0, 0, 0))
    # buttons below: next, previous (normal and hovered), back
    arrow(d, 0, 400, False, False)
    arrow(d, 23, 400, False, True)
    arrow(d, 46, 400, True, False)
    arrow(d, 69, 400, True, True)
    back(d, 92, 400, False)
    back(d, 107, 400, True)
    return img


def arrow(d, x, y, left, hover):
    """An 18x10 arrow in ink, gold when hovered."""
    col = (GOLD if hover else INK) + (255,)
    pts = [(0, 3), (11, 3), (11, 0), (17, 5), (11, 10 - 1), (11, 6), (0, 6)]
    if left:
        pts = [(17 - px, py) for px, py in pts]
    d.polygon([(x + px, y + py) for px, py in pts], fill=col)


def back(d, x, y, hover):
    """A 13x11 curled back arrow."""
    col = (GOLD if hover else INK) + (255,)
    d.arc((x + 2, y + 1, x + 12, y + 10), 270, 180, fill=col, width=2)
    d.polygon([(x, y + 4), (x + 5, y), (x + 5, y + 8)], fill=col)


def cover():
    src = Image.open(COVER).convert('RGBA')
    # the closed book is drawn 266x345 book units; twice that here for crispness
    return src.resize((532, 690), Image.LANCZOS)


def item():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((3, 1, 13, 14), fill=LEATHER + (255,))
    d.rectangle((3, 1, 4, 14), fill=LEATHER_DARK + (255,))       # spine
    d.rectangle((13, 2, 14, 14), fill=PAPER + (255,))             # page edges
    d.line((13, 14, 4, 14), fill=PAPER_EDGE + (255,))
    d.rectangle((6, 3, 11, 3), fill=GOLD + (255,))                 # title bar
    d.rectangle((6, 12, 11, 12), fill=GOLD + (255,))
    leaf = [(8, 4), (8, 5), (8, 6), (8, 7), (8, 8),                 # the middle finger
            (7, 6), (6, 5), (9, 6), (10, 5),                         # the two beside it
            (7, 8), (6, 8), (5, 7), (9, 8), (10, 8), (11, 7),        # the outer two
            (8, 9), (8, 10)]                                         # the stem
    for px, py in leaf:
        d.point((px, py), fill=RIBBON + (255,))
    d.line((11, 15, 11, 15), fill=RIBBON_DARK + (255,))
    return img


def main():
    GUI.mkdir(parents=True, exist_ok=True)
    spread().save(GUI / 'book.png')
    cover().save(GUI / 'cover.png')
    item().save(ITEM / 'handbook.png')
    print('wrote book.png, cover.png, item handbook.png')


if __name__ == '__main__':
    main()
