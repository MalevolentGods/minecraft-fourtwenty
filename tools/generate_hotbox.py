#!/usr/bin/env python3
"""The Hotbox's textures: a dark hemp-slat crate with a bud-green frame, a seven-point leaf on the sides and top.

    python3 tools/generate_hotbox.py

Two states, the leaf dim when idle and glowing lime when it's holding its chunks. Drawn large, then brought down to 32 pixels.
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parent.parent / 'src/main/resources/assets/fourtwenty/textures/block/hotbox'
S, K = 32, 8                # final size, drawing scale
BIG = S * K
SLAT = (40, 58, 34)
SLAT_LIGHT = (52, 74, 42)
GAP = (22, 32, 20)
FRAME = (88, 112, 44)
FRAME_DARK = (60, 80, 34)
CORNER = (168, 200, 72)


def crate(rows=True):
    img = Image.new('RGB', (BIG, BIG), GAP)
    d = ImageDraw.Draw(img)
    slats = 5
    pitch = BIG / slats
    for i in range(slats):
        a, b = int(i * pitch + K * 0.6), int((i + 1) * pitch - K * 0.6)
        box = (0, a, BIG, b) if rows else (a, 0, b, BIG)
        d.rectangle(box, fill=SLAT if i % 2 else SLAT_LIGHT)
    return img


def frame(img):
    d = ImageDraw.Draw(img)
    w = K * 3
    d.rectangle((0, 0, BIG - 1, BIG - 1), outline=FRAME, width=w)
    d.rectangle((w, w, BIG - 1 - w, BIG - 1 - w), outline=FRAME_DARK, width=K // 2)
    for x, y in ((0, 0), (BIG - w, 0), (0, BIG - w), (BIG - w, BIG - w)):
        d.rectangle((x, y, x + w - 1, y + w - 1), fill=CORNER)


def leaflet(cx, cy, angle, length, width):
    """A serrated blade from the leaf's heart, as a polygon."""
    pts_l, pts_r = [], []
    steps = 14
    for i in range(steps + 1):
        t = i / steps
        half = width * math.sin(math.pi * t ** 0.75) * (1.0 + (0.28 if i % 2 else -0.05))
        px, py = t * length, half
        ca, sa = math.cos(angle), math.sin(angle)
        pts_l.append((cx + px * ca - py * sa, cy + px * sa + py * ca))
        pts_r.append((cx + px * ca + py * sa, cy + px * sa - py * ca))
    return pts_l + pts_r[::-1]


def leaf(size=1.0):
    """The leaf as a mask, its stem at the bottom."""
    mask = Image.new('L', (BIG, BIG), 0)
    d = ImageDraw.Draw(mask)
    cx, cy = BIG / 2, BIG * 0.68
    r = BIG * 0.5 * size
    for deg, length, width in ((0, 1.08, 0.075), (31, 0.98, 0.07), (-31, 0.98, 0.07), (62, 0.8, 0.06), (-62, 0.8, 0.06),
                               (98, 0.5, 0.045), (-98, 0.5, 0.045)):
        d.polygon(leaflet(cx, cy, math.radians(deg - 90), r * length, r * width), fill=255)
    d.line((cx, cy, cx, cy + BIG * 0.17 * size), fill=255, width=int(K * 1.1))
    return mask


def glow(img, mask, core, halo, amount):
    blurred = mask.filter(ImageFilter.GaussianBlur(K * 2.4)).point(lambda v: int(v * amount))
    img.paste(Image.new('RGB', img.size, halo), (0, 0), blurred)
    img.paste(Image.new('RGB', img.size, core), (0, 0), mask)
    veins = Image.new('L', img.size, 0)
    d = ImageDraw.Draw(veins)
    cx, cy = BIG / 2, BIG * 0.68
    for deg, length in ((0, 0.42), (32, 0.36), (-32, 0.36), (66, 0.28), (-66, 0.28)):
        a = math.radians(deg - 90)
        d.line((cx, cy, cx + math.cos(a) * BIG * length, cy + math.sin(a) * BIG * length), fill=120, width=K // 2)
    dark = tuple(int(c * 0.72) for c in core)
    img.paste(Image.new('RGB', img.size, dark), (0, 0), Image.composite(veins, Image.new('L', img.size, 0), mask))


def save(img, name):
    OUT.mkdir(parents=True, exist_ok=True)
    img.resize((S, S), Image.LANCZOS).quantize(48, dither=Image.Dither.NONE).convert('RGB').save(OUT / f'{name}.png', optimize=True)


def main():
    for lit, suffix in ((False, ''), (True, '_lit')):
        core, halo, amount = ((186, 255, 78), (130, 220, 40), 0.8) if lit else ((92, 128, 46), (60, 90, 34), 0.2)
        side = crate(True)
        glow(side, leaf(), core, halo, amount)
        frame(side)
        save(side, f'side{suffix}')
        top = crate(False)
        glow(top, leaf(0.78), core, halo, amount)
        frame(top)
        save(top, f'top{suffix}')
    bottom = crate(True)
    frame(bottom)
    save(bottom, 'bottom')


if __name__ == '__main__':
    main()
