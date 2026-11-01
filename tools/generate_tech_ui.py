#!/usr/bin/env python3
"""Art for the Boundless Inspiration screens: a panel per tier and the shared atlas.

    python3 tools/generate_tech_ui.py [--sheet out.png]

textures/gui/tech_<tier>.png is a 176x166 panel: brushed dark metal, the tier's colour along a beaded edge, a faint leaf, and the player's
inventory slots. textures/gui/tech_atlas.png holds what client/TechScreen.java draws; the layout constants there and here must match:

    gauge frame        0,0    14x54      gauge fill frames   16,0   12x54 each, 4 of them, bubbles rising
    arrow frame        0,56   24x16      arrow fill frames   24,56  24x8 blades + 24x8 waves, 4 of them
    slot               0,76   18x18      smoke puffs         0,96   32x32 each, 4 of them
"""
import math
import random
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
GUI = ROOT / 'src/main/resources/assets/fourtwenty/textures/gui'
TIERS = {'puff': (96, 196, 72), 'toke': (60, 196, 180), 'rip': (168, 100, 232)}


def brushed(w, h, seed):
    rng = np.random.default_rng(seed)
    row = rng.normal(0, 3.2, (h, 1))
    grain = np.repeat(row, w, axis=1) + rng.normal(0, 1.4, (h, w))
    base = np.array([35, 38, 43], dtype=np.float32)
    rgb = np.clip(base[None, None, :] + grain[..., None], 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([rgb, np.full((h, w), 255, np.uint8)]), 'RGBA')


def slot(d, x, y):
    d.rectangle([x, y, x + 17, y + 17], fill=(16, 18, 21, 255))
    d.line([(x, y), (x + 16, y)], fill=(8, 9, 11, 255))
    d.line([(x, y), (x, y + 16)], fill=(8, 9, 11, 255))
    d.line([(x + 17, y + 1), (x + 17, y + 17)], fill=(74, 79, 87, 255))
    d.line([(x + 1, y + 17), (x + 17, y + 17)], fill=(74, 79, 87, 255))


def leaf(img, cx, cy, size, colour):
    layer = Image.new('RGBA', img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for angle, scale in ((-90, 1), (-55, 0.82), (-125, 0.82), (-20, 0.6), (-160, 0.6), (15, 0.32), (165, 0.32)):
        a = math.radians(angle)
        length = size * scale
        tip = (cx + math.cos(a) * length, cy + math.sin(a) * length)
        side = a + math.pi / 2
        w = length * 0.13
        mid = (cx + math.cos(a) * length * 0.45, cy + math.sin(a) * length * 0.45)
        d.polygon([(cx, cy), (mid[0] + math.cos(side) * w, mid[1] + math.sin(side) * w), tip,
                   (mid[0] - math.cos(side) * w, mid[1] - math.sin(side) * w)], fill=colour + (22,))
    img.alpha_composite(layer)


def panel(name, colour):
    w, h = 176, 166
    img = brushed(w, h, sum(colour))
    d = ImageDraw.Draw(img)
    dark = tuple(int(c * 0.45) for c in colour) + (255,)
    d.rectangle([0, 0, w - 1, h - 1], outline=dark)
    d.rectangle([1, 1, w - 2, h - 2], outline=colour + (255,))
    d.rectangle([2, 2, w - 3, h - 3], outline=(20, 22, 25, 255))
    d.rectangle([3, 15, w - 4, 15], fill=dark)
    leaf(img, 88, 62, 42, colour)
    d = ImageDraw.Draw(img)
    for x, y in ((4, 4), (w - 7, 4), (4, h - 7), (w - 7, h - 7)):
        d.ellipse([x, y, x + 3, y + 3], fill=tuple(min(255, c + 70) for c in colour) + (255,))
    d.rectangle([7, 80, w - 8, 80], fill=dark)
    for row in range(3):
        for col in range(9):
            slot(d, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        slot(d, 7 + col * 18, 141)
    img.save(GUI / f'tech_{name}.png')
    # the small panel for screens with no inventory uses this art's top, so it needs a closing edge there too
    return img


def gauge_frame(d):
    d.rectangle([0, 0, 13, 53], fill=(14, 15, 18, 255), outline=(92, 98, 108, 255))
    for y in range(8, 50, 8):
        d.line([(10, y), (12, y)], fill=(92, 98, 108, 255))


def gauge_fill(img, i):
    x0 = 16 + i * 12
    rng = random.Random(420 + i)
    for y in range(54):
        t = y / 53
        c = (int(70 + 80 * (1 - t)), int(200 + 50 * (1 - t)), int(50 + 40 * (1 - t)), 255)
        for x in range(10):
            img.putpixel((x0 + x, y), c)
    d = ImageDraw.Draw(img)
    for b in range(9):
        x = x0 + rng.randint(1, 8)
        y = (rng.randint(0, 53) - i * 7) % 54
        d.ellipse([x - 1, y - 1, x + 1, y + 1], outline=(214, 255, 196, 255))


def arrows(img, d):
    d.rounded_rectangle([0, 56, 23, 71], 3, fill=(14, 15, 18, 255), outline=(92, 98, 108, 255))
    for i in range(4):
        x0 = 24 + i * 24
        for y in range(8):                       # blades: stripes running along
            for x in range(24):
                on = (x + y + i * 3) % 6 < 3
                img.putpixel((x0 + x, 56 + y), (214, 226, 232, 255) if on else (120, 212, 92, 255))
        for x in range(24):                      # waves: drying heat
            y = 4 + int(2.5 * math.sin((x + i * 3) / 3))
            for row in range(8):
                img.putpixel((x0 + x, 64 + row), (150, 236, 110, 255) if abs(row - y) <= 2 else (64, 140, 56, 255))


def smoke(img):
    for i in range(4):
        rng = random.Random(30 + i)
        puff = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
        d = ImageDraw.Draw(puff)
        for _ in range(7):
            r = rng.randint(4, 8)
            x, y = rng.randint(8, 23), rng.randint(8, 23)
            g = rng.randint(190, 240)
            d.ellipse([x - r, y - r, x + r, y + r], fill=(g, g, g, 120))
        img.alpha_composite(puff.filter(ImageFilter.GaussianBlur(2)), (i * 32, 96))


def atlas():
    img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    gauge_frame(d)
    for i in range(4):
        gauge_fill(img, i)
    arrows(img, d)
    slot(d, 0, 76)
    smoke(img)
    img.save(GUI / 'tech_atlas.png')
    return img


def main():
    GUI.mkdir(parents=True, exist_ok=True)
    panels = [panel(n, c) for n, c in TIERS.items()]
    a = atlas()
    if '--sheet' in sys.argv:
        out = sys.argv[sys.argv.index('--sheet') + 1]
        sheet = Image.new('RGBA', (3 * 176 + 256, 256), (60, 60, 66, 255))
        for i, p in enumerate(panels):
            sheet.alpha_composite(p, (i * 176, 0))
        sheet.alpha_composite(a, (3 * 176, 0))
        sheet.save(out)
    print('ok')


if __name__ == '__main__':
    main()
