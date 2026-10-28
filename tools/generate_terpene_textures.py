#!/usr/bin/env python3
"""Textures for hemp blocks and terpene items, from tools/terpenes.json.

Hemp blocks (64x64, seamless): a basket weave of 2-pixel threads that vary a little in shade
along their length (slubs), a faint fibre fuzz, and a barely-there soft gradient. Almost all the
detail is at weave scale, which averages out, so a wall reads as one even colour from medium
distance (no block-sized blotches to repeat) and as woven hemp up close. It tiles seamlessly
unrotated; rotating or mirroring it would put the threads out of line at block edges.

Terpene items (32x32): a glass vessel whose shape is the tier (a base is a stoppered vial, an
accord a round flask, a final terpene a tall essence bottle) holding the terpene's colour; the
resin (Sticky Situation) is an amber drop. Seeded, so reruns give the same images.

    python3 tools/generate_terpene_textures.py [--preview DIR]
"""
import argparse
import colorsys
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
DATA = json.loads((ROOT / 'tools/terpenes.json').read_text(encoding='utf-8'))
BLOCKS = ROOT / 'src/main/resources/assets/fourtwenty/textures/block/hemp'
ITEMS = ROOT / 'src/main/resources/assets/fourtwenty/textures/item/terpene'
S = 64


def rgb(hex_):
    return tuple(int(hex_[i:i + 2], 16) for i in (1, 3, 5))


def clamp(v):
    return max(0, min(255, int(round(v))))


def periodic_noise(rng, cells, size=S):
    """Smooth value noise on a cells x cells lattice that wraps, so it tiles."""
    lattice = [[rng.uniform(-1, 1) for _ in range(cells)] for _ in range(cells)]
    out = [[0.0] * size for _ in range(size)]
    step = size / cells
    for y in range(size):
        gy, fy = divmod(y / step, 1)
        gy = int(gy)
        sy = fy * fy * (3 - 2 * fy)
        for x in range(size):
            gx, fx = divmod(x / step, 1)
            gx = int(gx)
            sx = fx * fx * (3 - 2 * fx)
            a, b = lattice[gy % cells][gx % cells], lattice[gy % cells][(gx + 1) % cells]
            c, d = lattice[(gy + 1) % cells][gx % cells], lattice[(gy + 1) % cells][(gx + 1) % cells]
            out[y][x] = (a + (b - a) * sx) + ((c + (d - c) * sx) - (a + (b - a) * sx)) * sy
    return out


def threads(rng, size=S):
    """One shade per thread position along its length: a base tone plus slubs (thicker, lighter runs)."""
    out = []
    for _ in range(size):
        base = rng.uniform(-0.6, 0.6)
        line = [base + rng.uniform(-0.25, 0.25) for _ in range(size)]
        for _ in range(rng.randint(1, 3)):
            start, length, lift = rng.randrange(size), rng.randint(4, 11), rng.uniform(0.5, 1.1) * rng.choice((1, -1))
            for i in range(length):
                t = math.sin(math.pi * (i + 0.5) / length)
                line[(start + i) % size] += lift * t
        out.append(line)
    return out


def weave(x, y):
    """A basket weave of 2-pixel threads: over a 2x2 cell, either the warp (vertical) or the weft
    (horizontal) thread is on top, rounded across its width and dipping where it goes under.
    Every 4x4 averages to zero, so from a distance it's one flat colour."""
    cx, cy, ix, iy = x // 2, y // 2, x % 2, y % 2
    if (cx + cy) % 2 == 0:            # warp on top: lit on its left, dipping at the cell's top and bottom
        across, along = ix, iy
    else:                             # weft on top: lit on its top
        across, along = iy, ix
    return (0.9 if across == 0 else -0.6) + (-0.3 if along == 1 else 0.0)


def hemp(hex_, seed):
    rng = random.Random(seed)
    low = periodic_noise(rng, 4)
    warp, weft = threads(rng), threads(rng)
    fuzz = [[rng.uniform(-1, 1) for _ in range(S)] for _ in range(S)]
    base = rgb(hex_)
    lum = (0.2126 * base[0] + 0.7152 * base[1] + 0.0722 * base[2]) / 255
    img = Image.new('RGB', (S, S))
    px = img.load()
    for y in range(S):
        for x in range(S):
            cx, cy = x // 2, y // 2
            thread = warp[cx * 2 % S][y] if (cx + cy) % 2 == 0 else weft[cy * 2 % S][x]
            # Detail lives at weave scale (it averages out with distance); the soft gradient is
            # kept very faint so nothing repeats block to block.
            f = 0.034 * weave(x, y) + 0.008 * thread + 0.010 * fuzz[y][x] + 0.006 * low[y][x]
            # A share of the colour plus a little absolute light, so blacks still show the weave.
            px[x, y] = tuple(clamp(c * (1 + f) + 255 * f * (0.30 - 0.25 * lum)) for c in base)
    return img


# ---- items -------------------------------------------------------------------------------

def shade(c, f):
    return tuple(clamp(v * f) for v in c)


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


GLASS_EDGE = (46, 58, 62)
GLASS_FILL = (200, 225, 230)
CORK = (150, 108, 70)
CAP = (60, 62, 66)


def vessel(kind):
    """(inside, liquid_top) mask functions for a 32x32 vessel: inside(x, y) -> bool."""
    if kind == 'vial':                     # base: a stoppered test-tube vial, centred
        def inside(x, y):
            if 9 <= y <= 27 and 12 <= x <= 19:
                return y < 25 or (x - 15.5) ** 2 + (y - 25) ** 2 <= 13
            return False
        return inside, 13, ('cork', 12, 19, 5, 8)
    if kind == 'flask':                    # accord: a round flask with a short neck
        def inside(x, y):
            if 6 <= y <= 13 and 13 <= x <= 18:
                return True
            return (x - 15.5) ** 2 + (y - 20.5) ** 2 <= 70
        return inside, 15, ('cork', 13, 18, 3, 5)
    def inside(x, y):                      # final: a tall essence bottle with shoulders
        if 7 <= y <= 9 and 13 <= x <= 18:
            return True
        if 10 <= y <= 12:
            half = 3 + (y - 9) * 1.4
            return abs(x - 15.5) <= half
        return 13 <= y <= 28 and 9 <= x <= 22 and not ((y == 28) and x in (9, 22))
    return inside, 14, ('cap', 12, 19, 4, 6)


def item(kind, hex_, seed):
    rng = random.Random(seed)
    inside, top, (stopper, sx0, sx1, sy0, sy1) = vessel(kind)
    liquid = rgb(hex_)
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = img.load()
    inner = {(x, y) for x in range(32) for y in range(32) if inside(x, y)}
    xs = [x for x, _ in inner]
    left, right = min(xs), max(xs)
    for (x, y) in inner:
        edge = any((x + dx, y + dy) not in inner for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        t = (x - left) / max(1, right - left)
        if edge:
            px[x, y] = GLASS_EDGE + (255,)
        elif y >= top:
            c = shade(liquid, 1.12 - 0.32 * t)             # lit from the left
            if y == top:
                c = mix(c, (255, 255, 255), 0.35)          # the meniscus
            c = shade(c, 1 + rng.uniform(-0.02, 0.02))
            px[x, y] = c + (255,)
        else:
            px[x, y] = mix(GLASS_FILL, liquid, 0.15) + (110,)
    # A highlight down the glass on the left.
    for (x, y) in inner:
        if x == left + 2 and top - 3 <= y <= top + 9 and px[x, y][3] and px[x, y][:3] != GLASS_EDGE:
            px[x, y] = mix(px[x, y][:3], (255, 255, 255), 0.55) + (255,)
    stop = CORK if stopper == 'cork' else CAP
    for y in range(sy0, sy1 + 1):
        for x in range(sx0, sx1 + 1):
            edge = x in (sx0, sx1) or y in (sy0, sy1)
            px[x, y] = (shade(stop, 0.7) if edge else shade(stop, 1.05 - 0.2 * (x - sx0) / max(1, sx1 - sx0))) + (255,)
    return img


def resin(seed):
    rng = random.Random(seed)
    amber = (196, 128, 32)
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = img.load()
    def inside(x, y):                      # a drop: round bottom, pointed top
        if y >= 16:
            return (x - 15.5) ** 2 + (y - 19) ** 2 <= 72
        return abs(x - 15.5) <= (y - 4) * 0.62 and y >= 5
    cells = {(x, y) for x in range(32) for y in range(32) if inside(x, y)}
    for (x, y) in cells:
        edge = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        d = math.hypot(x - 12, y - 14) / 14
        c = shade(amber, 0.62) if edge else shade(amber, 1.25 - 0.45 * d + rng.uniform(-0.03, 0.03))
        px[x, y] = c + (255,)
    for x, y in ((11, 13), (12, 12), (11, 14), (12, 13), (13, 11)):
        px[x, y] = (255, 236, 190, 255)
    return img


def generate(preview=None):
    BLOCKS.mkdir(parents=True, exist_ok=True)
    ITEMS.mkdir(parents=True, exist_ok=True)
    blocks = [('natural', DATA['natural']['hex'])] + [(c['code'], c['hex']) for c in DATA['colors'] + DATA['minecraft']]
    for i, (code, hex_) in enumerate(blocks):
        hemp(hex_, 7000 + i).save(BLOCKS / f'{code}.png')
    finals = {c['code']: c['hex'] for c in DATA['colors']}
    for i, b in enumerate(DATA['bases']):
        item('vial', b['hex'], 100 + i).save(ITEMS / f'{b["code"]}.png')
    for i, a in enumerate(DATA['accords']):
        # An accord shows its row's bright tone (port 3 = Pinewise).
        item('flask', finals[a['code'] + '_pin'], 200 + i).save(ITEMS / f'{a["code"]}.png')
    for i, c in enumerate(DATA['colors'] + DATA['minecraft']):
        item('bottle', c['hex'], 300 + i).save(ITEMS / f'{c["code"]}.png')
    resin(9).save(ITEMS / 'resin.png')
    print(f'{len(blocks)} hemp textures, {4 + 16 + 64 + 16 + 1} item textures')
    if preview:
        write_previews(Path(preview), blocks)


def write_previews(out, blocks):
    """Contact sheets for checking by eye: a close-up wall, a far wall, and every item."""
    out.mkdir(parents=True, exist_ok=True)
    tiles = {code: Image.open(BLOCKS / f'{code}.png') for code, _ in blocks}
    for code in ('natural', 'myr_pin_pin', 'lav_pin_pin', 'myr_pin_myr', 'myr_pin_lim', 'lim_lav_pin'):
        wall = Image.new('RGB', (S * 3, S * 3))
        for j in range(3):
            for i in range(3):
                wall.paste(tiles[code], (i * S, j * S))
        wall.resize((S * 6, S * 6), Image.NEAREST).save(out / f'close_{code}.png')
        mid = Image.new('RGB', (S * 12, S * 12))
        for j in range(12):
            for i in range(12):
                mid.paste(tiles[code], (i * S, j * S))
        mid.resize((S * 6, S * 6), Image.BOX).save(out / f'mid_{code}.png')   # 12 blocks, 32 px each
    far = Image.new('RGB', (16 * 8 * 8, 16 * 8 * 10))
    for n, (code, _) in enumerate(blocks[1:]):
        gx, gy = n % 8, n // 8
        patch = Image.new('RGB', (S * 8, S * 8))
        for j in range(8):
            for i in range(8):
                patch.paste(tiles[code], (i * S, j * S))
        far.paste(patch.resize((128, 128), Image.BOX), (gx * 128, gy * 128))
    far.save(out / 'far_all.png')
    sheet = Image.new('RGBA', (32 * 12, 32 * 8), (40, 44, 42, 255))
    names = sorted(p.name for p in ITEMS.glob('*.png'))
    for n, name in enumerate(names):
        icon = Image.open(ITEMS / name)
        sheet.alpha_composite(icon, ((n % 12) * 32, (n // 12) * 32))
    sheet.resize((32 * 12 * 3, 32 * 8 * 3), Image.NEAREST).save(out / 'items.png')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview')
    generate(ap.parse_args().preview)
