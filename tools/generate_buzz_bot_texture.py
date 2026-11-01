#!/usr/bin/env python3
"""The Buzz Bot's texture and icon, cut from the parts sheet.

    python3 tools/generate_buzz_bot_texture.py

tools/source_textures/buzz_bot_sheet.png is the user's sheet of loose parts. This places each part on the faces of the model in client/BuzzBotRenderer.java:
the layout below is that model's box UVs, in model pixels, drawn at SCALE texture pixels each.
"""
from pathlib import Path

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
SHEET = ROOT / 'tools/source_textures/buzz_bot_sheet.png'
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
SCALE = 8
SIZE = 64 * SCALE

# parts of the sheet: left, top, right, bottom
FACE = (15, 100, 400, 395)
FACE_AND_ANTENNAE = (15, 20, 400, 395)
LEAF_PLATE = (430, 55, 675, 330)
TANK = (700, 25, 1020, 365)
STRIPES = (1060, 20, 1240, 380)
HEAD_SIDE = (715, 420, 815, 655)
HEAD_BACK = (845, 490, 1000, 670)
THORAX_SIDE = (15, 420, 135, 655)
THORAX_FRONT = (140, 425, 275, 655)
THORAX_BACK = (415, 420, 690, 690)
UNDERSIDE = (1035, 530, 1240, 640)
ABDOMEN = (440, 850, 655, 1235)
LEG = (20, 920, 105, 1230)
ANTENNA = (275, 25, 395, 150)
LEFT_WING = (15, 670, 515, 940)
RIGHT_WING = (575, 680, 1105, 940)
DARK = (1160, 770, 1235, 880)


def clear_black(part, opacity=255):
    """The sheet's background goes; what's left takes {@code opacity}."""
    part = part.convert('RGBA')
    pixels = part.load()
    for y in range(part.height):
        for x in range(part.width):
            r, g, b, _ = pixels[x, y]
            stray = opacity < 255 and b < r * 0.6               # bits of the neighbouring parts, which are yellow; the wings aren't
            pixels[x, y] = (r, g, b, 0 if max(r, g, b) < 40 or stray else opacity)
    return part


def main():
    sheet = Image.open(SHEET).convert('RGB')
    out = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))

    def put(box, u, v, w, h, change=None, alpha=None):
        part = sheet.crop(box)
        part = change(part) if change else part
        part = part.resize((w * SCALE, h * SCALE), Image.BOX)
        if alpha:
            part = clear_black(part, alpha)
        out.paste(part, (u * SCALE, v * SCALE))

    # head: 6 wide, 6 tall, 4 deep, at (0, 0)
    put(LEAF_PLATE, 4, 0, 6, 4)
    put(UNDERSIDE, 10, 0, 6, 4)
    put(HEAD_SIDE, 0, 4, 4, 6, ImageOps.mirror)
    put(FACE, 4, 4, 6, 6)
    put(HEAD_SIDE, 10, 4, 4, 6)
    put(HEAD_BACK, 14, 4, 6, 6)
    # thorax: 7 wide, 6 tall, 6 deep, at (0, 12)
    put(TANK, 6, 12, 7, 6)
    put(UNDERSIDE, 13, 12, 7, 6)
    put(THORAX_SIDE, 0, 18, 6, 6)
    put(THORAX_FRONT, 6, 18, 7, 6)
    put(THORAX_SIDE, 13, 18, 6, 6, ImageOps.mirror)
    put(THORAX_BACK, 19, 18, 7, 6)
    # abdomen: 5 wide, 5 tall, 8 deep, at (0, 26). Its stinger end goes away from the thorax.
    put(ABDOMEN, 8, 26, 5, 8, ImageOps.flip)
    put(ABDOMEN, 13, 26, 5, 8)
    rings = lambda part: part.rotate(90, expand=True)
    put(STRIPES, 0, 34, 8, 5, rings)
    put(STRIPES, 8, 34, 5, 5)
    put(STRIPES, 13, 34, 8, 5, rings)
    put(DARK, 21, 34, 5, 5)
    # stinger, antennae, legs: small boxes, the same part on every face
    put(DARK, 28, 0, 6, 3)
    put(ANTENNA, 28, 4, 4, 4)
    put(LEG, 28, 10, 4, 4)
    # wings: flat, top and bottom alike
    for v, box, flip in ((0, LEFT_WING, False), (8, RIGHT_WING, False)):
        put(box, 39, v, 10, 7, alpha=175)
        put(box, 49, v, 10, 7, ImageOps.flip, alpha=175)

    (ASSETS / 'textures/entity').mkdir(exist_ok=True)
    out.save(ASSETS / 'textures/entity/buzz_bot.png', optimize=True)
    icon = clear_black(sheet.crop(FACE_AND_ANTENNAE)).resize((64, 64), Image.BOX)
    icon.save(ASSETS / 'textures/item/buzz_bot.png', optimize=True)
    print('ok')


if __name__ == '__main__':
    main()
