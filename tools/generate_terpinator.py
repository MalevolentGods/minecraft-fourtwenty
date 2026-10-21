#!/usr/bin/env python3
"""The Terpinator's model, textures (256x256), blockstate, loot, recipe and tag, plus the English
text for its screen, guide and toast.

A brass-and-walnut still. The cabinet: walnut panels in a raised frame with corner posts, on an
iron plinth with brass feet; on its front the three numbered blend ports (brass flanges, iron
nozzles, engraved plates), on its east side the method dial (a face in the four base colours, a
pointer and knob), on its west side the press's spoked crank, a vent grille behind. On the brass
deck: a copper hopper for buds (front left), the still (a brass-framed glass chamber of amber
terpene under a copper dome and valve wheel), a coil condenser joined to the dome by a swan neck
(back right), a resin tank with a sight glass (back left), and a pressure gauge and status lamp
(front right). While it works (LIT) the terpene and the lamp glow.

Round parts are Minecraft "pixel circles" (three overlapping boxes); round faces (gauge, dial,
wheel, port mouths) are textures with transparent corners. Render it to check:
    blender -b --python tools/render_block_model.py -- <model.json> <out>

    python3 tools/generate_terpinator.py [--preview DIR]
"""
import argparse
import collections
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'
TEX = ASSETS / 'textures/block/terpinator'
S = 256


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, f):
    return tuple(clamp(v * f) for v in c[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ---- textures --------------------------------------------------------------------------------
# Every texture is S x S (256). Patterns are laid out in 64-unit design space (multiply by K) so
# they keep their proportions; the extra resolution goes into fine detail (wood fibres, brushed
# streaks, domed rivets, crisp dial ticks and lettering).

K = S // 64
WALNUT = (78, 52, 34)
WALNUT_LIGHT = (104, 72, 46)
BRASS = (196, 156, 74)
COPPER = (186, 106, 64)
IRON = (62, 64, 68)
AMBER = (216, 140, 32)
BASE_COLORS = [(107, 90, 76), (201, 138, 18), (47, 122, 71), (110, 89, 168)]   # Myr, Lim, Pin, Lav


def new(color=(0, 0, 0), alpha=255):
    return Image.new('RGBA', (S, S), color + (alpha,))


def k(*vs):
    """Design units (0..64) to pixels."""
    return [v * K for v in vs]


class Wood:
    """Walnut grain: broad figure in design units plus per-pixel fibres along the grain."""

    def __init__(self, rng):
        self.rng = rng
        self.phase = rng.uniform(0, 6)
        self.fibre = [rng.uniform(-1, 1) for _ in range(S)]           # one tone per pixel column
        self.pores = {(rng.randrange(S), rng.randrange(S)) for _ in range(S * 3)}

    def at(self, base, x, y, strength=1.0, board=0):
        u, v = x / K, y / K
        ph = self.phase + board * 1.7
        figure = math.sin(v * 0.33 + ph + math.sin(u * 0.7 + ph) * 1.4) * 0.07 + math.sin(v * 1.9 + u * 0.3) * 0.02
        f = 1 + strength * figure + 0.035 * self.fibre[x] + self.rng.uniform(-0.02, 0.02)
        if (x, y) in self.pores:
            f *= 0.82
        return shade(base, f)


def walnut_panel(rng):
    """A recessed panel: a bevelled border (lit top and left) round a darker field of vertical grain."""
    img = new()
    px = img.load()
    wood = Wood(rng)
    bevel = 5 * K
    for y in range(S):
        for x in range(S):
            b = min(x, y, S - 1 - x, S - 1 - y)
            if b < bevel:
                lit = (x == b and x <= y) or (y == b and y < x) or (b == x or b == y)
                lit = min(x, y) <= min(S - 1 - x, S - 1 - y)
                c = shade(wood.at(WALNUT_LIGHT, x, y), 1.12 if lit else 0.78)
                if b < K:
                    c = shade(c, 0.72)                           # the frame's shadow line
                elif b >= bevel - K:
                    c = shade(c, 0.9)                            # the panel's lip
            else:
                c = wood.at(shade(WALNUT, 0.9), x, y, board=x // (21 * K))
            px[x, y] = c + (255,)
    return img


def walnut_frame(rng):
    img = new()
    px = img.load()
    wood = Wood(rng)
    for y in range(S):
        for x in range(S):
            px[x, y] = wood.at(WALNUT_LIGHT, x, y, 1.2) + (255,)
    return img


def brushed(rng, base, patina=0.0, rows=True):
    """Brushed metal: a fine streak per pixel row (or column), a broad sheen, and verdigris specks."""
    img = new()
    px = img.load()
    streak = [rng.uniform(-0.05, 0.05) for _ in range(S)]
    for y in range(S):
        for x in range(S):
            f = 1 + (streak[y] if rows else streak[x]) + rng.uniform(-0.015, 0.015) + 0.06 * math.sin((x + y) / (18.0 * K))
            px[x, y] = shade(base, f) + (255,)
    for _ in range(int(patina * 30 * K)):
        cx, cy, r = rng.randrange(S), rng.randrange(S), rng.uniform(0.5, 1.6) * K / 2
        for y in range(int(cy - r), int(cy + r) + 1):
            for x in range(int(cx - r), int(cx + r) + 1):
                if math.hypot(x - cx, y - cy) <= r:
                    xx, yy = x % S, y % S
                    px[xx, yy] = mix(px[xx, yy][:3], (96, 150, 120), 0.4) + (255,)
    for _ in range(6 * K):                                       # light scratches
        x0, y0 = rng.randrange(S), rng.randrange(S)
        length, dy = rng.randint(4, 14) * K // 2, rng.uniform(-0.3, 0.3)
        for i in range(length):
            x, y = (x0 + i) % S, int(y0 + i * dy) % S
            px[x, y] = shade(px[x, y][:3], 1.12) + (255,)
    return img


def rivet(px, cx, cy, base, r=None):
    """A domed rivet: lit top-left, shadowed bottom-right."""
    r = r or 1.2 * K
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            d = math.hypot(x - cx, y - cy)
            if 0 <= x < S and 0 <= y < S:
                if d <= r:
                    light = 1.25 - 0.45 * ((x - cx) + (y - cy)) / (2 * r) - 0.2 * d / r
                    px[x, y] = shade(base, light) + (255,)
                elif d <= r + 1 and x >= cx and y >= cy:
                    px[x, y] = shade(px[x, y][:3], 0.75) + (255,)


def copper_plate(rng):
    """Hammered copper with a row of domed rivets at top and bottom."""
    img = brushed(rng, COPPER, patina=0.6, rows=False)
    px = img.load()
    for _ in range(40):
        cx, cy, r = rng.randrange(S), rng.randrange(6 * K, S - 6 * K), rng.randint(2, 4) * K
        for y in range(cy - r, cy + r + 1):
            for x in range(cx - r, cx + r + 1):
                d = math.hypot(x - cx, y - cy) / r
                if d <= 1:
                    xx = x % S
                    px[xx, y] = shade(px[xx, y][:3], 1 + 0.07 * (0.5 - d) * (1 if x < cx else -1)) + (255,)
    for y in (3.5 * K, S - 3.5 * K):
        for x in range(4 * K, S, 8 * K):
            rivet(px, x, y, COPPER)
    return img


def pipe(rng):
    """Copper pipe: lit along its length, with a joint band every half block."""
    img = new()
    px = img.load()
    for y in range(S):
        for x in range(S):
            f = 0.8 + 0.32 * math.sin(math.pi * (y + 0.5) / S) + rng.uniform(-0.015, 0.015)
            band = (x / K) % 32
            if band < 3:
                f *= 1.15 + (0.08 if band < 1 else 0)
            elif band < 3.5:
                f *= 0.8
            px[x, y] = shade(COPPER, f) + (255,)
    return img


def iron(rng):
    img = new()
    px = img.load()
    for y in range(S):
        for x in range(S):
            px[x, y] = shade(IRON, 1 + 0.08 * math.sin(x / (9.0 * K)) + rng.uniform(-0.035, 0.035)) + (255,)
    return img


def deck(rng):
    """The deck: brushed brass plate with a riveted border and two seams."""
    img = brushed(rng, BRASS, patina=0.3)
    px = img.load()
    for i in range(S):
        for e in range(K):
            for edge in (e, S - 1 - e):
                px[i, edge] = shade(BRASS, 0.75) + (255,)
                px[edge, i] = shade(BRASS, 0.75) + (255,)
        for seam in (21 * K, 42 * K):
            for w in range(K // 2):
                px[seam + w, i] = shade(BRASS, 0.8) + (255,)
                px[seam + K // 2 + w, i] = shade(BRASS, 1.14) + (255,)
    for i in range(4 * K, S, 8 * K):
        for (x, y) in ((i, 3 * K), (i, S - 3 * K), (3 * K, i), (S - 3 * K, i)):
            rivet(px, x, y, BRASS, r=0.9 * K)
    return img


def glass():
    img = new((214, 236, 236), 34)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=(236, 248, 248, 140), width=K)
    for x, a in ((7, 120), (8, 90), (13, 60)):
        d.line(k(x, 6, x, 58), fill=(255, 255, 255, a), width=max(1, K // 2))
    d.line(k(50, 10, 54, 22), fill=(255, 255, 255, 60), width=max(1, K // 2))
    return img


def liquid(rng, lit):
    img = new()
    px = img.load()
    for y in range(S):
        for x in range(S):
            u, v = x / K, y / K
            f = (1.25 if lit else 1.0) * (1.08 - 0.25 * v / 64) + 0.05 * math.sin(u / 5 + v / 9) + rng.uniform(-0.025, 0.025)
            px[x, y] = shade(AMBER, f) + (230,)
    for _ in range((22 if lit else 10) * K):                   # bubbles: small bright beads, rising
        bx, by, r = rng.uniform(2, S - 3), rng.uniform(2, S - 3), rng.uniform(0.4, 1.2) * K
        for y in range(int(by - r), int(by + r) + 1):
            for x in range(int(bx - r), int(bx + r) + 1):
                d = math.hypot(x - bx, y - by) / r
                if d <= 1 and 0 <= x < S and 0 <= y < S:
                    px[x, y] = mix(px[x, y][:3], (255, 238, 190), 0.75 * (1 - d * 0.6)) + (235,)
    return img


def disc(size_frac=1.0):
    """Whether (x, y) is inside a centred circle of the given fraction of the texture."""
    r = S / 2 * size_frac - 0.5
    return lambda x, y: math.hypot(x - S / 2 + 0.5, y - S / 2 + 0.5) <= r


def polar(a_deg, r):
    """A point at an angle (degrees) and radius (design units) from the centre, in pixels."""
    a = math.radians(a_deg)
    return (S / 2 + r * K * math.cos(a), S / 2 + r * K * math.sin(a))


def gauge(rng):
    img = new(alpha=0)
    px = img.load()
    inside, face = disc(), disc(0.82)
    for y in range(S):
        for x in range(S):
            if not inside(x, y):
                continue
            if face(x, y):
                px[x, y] = shade((236, 228, 204), 1 - 0.08 * (y / S) + rng.uniform(-0.01, 0.01)) + (255,)
            else:
                a = math.atan2(y - S / 2, x - S / 2)
                px[x, y] = shade(BRASS, 1 + 0.25 * math.cos(a + 2.4) + rng.uniform(-0.02, 0.02)) + (255,)
    d = ImageDraw.Draw(img)
    for i in range(41):                                       # ticks over 270 degrees: majors, halves, minors
        a = 135 + i * 270 / 40
        r0 = 17.5 if i % 10 == 0 else 20 if i % 5 == 0 else 22
        d.line([*polar(a, r0), *polar(a, 24)], fill=(40, 34, 28, 255), width=K if i % 5 == 0 else max(1, K // 2))
    for i in range(9):                                        # the red zone
        a = 135 + 216 + i * 6
        d.line([*polar(a, 21), *polar(a, 24)], fill=(180, 30, 24, 255), width=K)
    for i, label in enumerate('01234'):                        # numbers by the major ticks
        x, y = polar(135 + i * 67.5, 13.5)
        draw_digits(d, label, x, y, 0.55 * K, (60, 48, 36, 255))
    a = 135 + 150
    d.line([*polar(a + 180, 4), *polar(a, 19)], fill=(170, 24, 18, 255), width=K)
    d.ellipse([S / 2 - 2.5 * K, S / 2 - 2.5 * K, S / 2 + 2.5 * K, S / 2 + 2.5 * K], fill=shade(BRASS, 0.8) + (255,))
    d.arc([10 * K, 10 * K, 54 * K, 54 * K], 200, 250, fill=(255, 255, 255, 140), width=K)    # glass glint
    return img


def dial(rng):
    """The method dial: a face in the four base colours (Myr, Lim, Pin, Lav clockwise from the top), a hub."""
    img = new(alpha=0)
    px = img.load()
    inside, face = disc(), disc(0.84)
    for y in range(S):
        for x in range(S):
            if not inside(x, y):
                continue
            a = (math.degrees(math.atan2(x - S / 2 + 0.5, -(y - S / 2 + 0.5))) + 360 + 45) % 360   # 0 at top, clockwise
            if face(x, y):
                px[x, y] = shade(BASE_COLORS[int(a // 90)], 1.05 - 0.15 * (y / S) + rng.uniform(-0.015, 0.015)) + (255,)
            else:
                px[x, y] = shade(BRASS, 1 + 0.25 * math.cos(math.radians(a) + 1) + rng.uniform(-0.02, 0.02)) + (255,)
    d = ImageDraw.Draw(img)
    for i in range(4):                                        # dividers between the four methods
        a = i * 90 - 45 - 90
        d.line([*polar(a, 6), *polar(a, 27)], fill=shade(BRASS, 0.7) + (255,), width=2 * K)
    for i in range(32):                                       # a ticked rim
        d.line([*polar(i * 360 / 32, 25), *polar(i * 360 / 32, 27)], fill=shade(BRASS, 0.6) + (255,), width=max(1, K // 2))
    d.ellipse([S / 2 - 7 * K, S / 2 - 7 * K, S / 2 + 7 * K, S / 2 + 7 * K], fill=shade(BRASS, 0.85) + (255,))
    d.ellipse([S / 2 - 4 * K, S / 2 - 4 * K, S / 2 + 4 * K, S / 2 + 4 * K], fill=shade(IRON, 1.2) + (255,))
    return img


def wheel(rng):
    """The press crank: a brass rim and six spokes, open between."""
    img = new(alpha=0)
    d = ImageDraw.Draw(img)
    d.ellipse(k(1, 1, 62, 62), outline=shade(BRASS, 0.72) + (255,), width=7 * K)
    d.ellipse(k(2.5, 2.5, 60.5, 60.5), outline=shade(BRASS, 1.12) + (255,), width=2 * K)
    d.ellipse(k(7, 7, 56, 56), outline=shade(BRASS, 0.6) + (255,), width=K)
    for i in range(6):
        a = i * 60 + 15
        d.line([*polar(a, 4), *polar(a, 27)], fill=shade(BRASS, 0.75) + (255,), width=4 * K)
        d.line([*polar(a - 2, 6), *polar(a - 1, 26)], fill=shade(BRASS, 1.15) + (255,), width=K)
    d.ellipse(k(22, 22, 42, 42), fill=shade(IRON, 1.1) + (255,))
    d.ellipse(k(27, 27, 37, 37), fill=shade(BRASS, 1.0) + (255,))
    return img


def mouth(rng):
    """A port's mouth: a dark throat inside a lit iron lip."""
    img = new(alpha=0)
    px = img.load()
    inside = disc()
    for y in range(S):
        for x in range(S):
            if inside(x, y):
                r = math.hypot(x - S / 2 + 0.5, y - S / 2 + 0.5) / (S / 2)
                if r > 0.72:
                    c = shade(IRON, 1.35 - 0.45 * r - 0.25 * ((x + y) / S - 1))
                else:
                    c = shade((22, 18, 16), 0.55 + 0.7 * r)
                px[x, y] = c + (255,)
    return img


DIGITS = {'0': ['###', '#.#', '#.#', '#.#', '###'], '1': ['.#.', '##.', '.#.', '.#.', '###'],
          '2': ['##.', '..#', '.#.', '#..', '###'], '3': ['##.', '..#', '.#.', '..#', '##.'], '4': ['#.#', '#.#', '###', '..#', '..#']}


def draw_digits(d, text, cx, cy, cell, fill):
    """Little stamped digits (3x5 cells) centred at (cx, cy)."""
    w = len(text) * 4 * cell - cell
    for n, ch in enumerate(text):
        for ry, row in enumerate(DIGITS[ch]):
            for rx, bit in enumerate(row):
                if bit == '#':
                    x = cx - w / 2 + (n * 4 + rx) * cell
                    y = cy - 2.5 * cell + ry * cell
                    d.rectangle([x, y, x + cell - 1, y + cell - 1], fill=fill)


def plates(rng):
    """Three engraved brass plates (32x16 design units each, stacked): 1, 2, 3."""
    img = new(alpha=0)
    px = img.load()
    d = ImageDraw.Draw(img)
    for i, digit in enumerate('123'):
        y0 = i * 16 * K
        d.rectangle([0, y0, 32 * K - 1, y0 + 16 * K - 1], fill=shade(BRASS, 0.68) + (255,))
        for y in range(y0 + K // 2, y0 + 16 * K - K // 2):
            for x in range(K // 2, 32 * K - K // 2):
                px[x, y] = shade(BRASS, 1.05 + 0.08 * math.sin(y / 3.0) * 0.3 + rng.uniform(-0.02, 0.02)) + (255,)
        d.line([K // 2, y0 + K // 2, 32 * K - K, y0 + K // 2], fill=shade(BRASS, 1.3) + (255,), width=max(1, K // 2))
        for rx in (3.5, 28.5):
            rivet(px, rx * K, y0 + 8 * K, BRASS, r=K)
        draw_digits(d, digit, 16 * K, y0 + 8 * K, 2 * K, (64, 44, 18, 255))
    return img


def lamp(lit):
    img = new()
    px = img.load()
    c = (120, 255, 140) if lit else (44, 92, 52)
    for y in range(S):
        for x in range(S):
            r = math.hypot(x / K - 24, y / K - 22) / 44
            px[x, y] = shade(c, (1.3 if lit else 1.1) - 0.6 * r) + (255,)
    return img


def hopper_inside(rng):
    """Looking into the hopper: a dark copper throat holding a heap of buds."""
    img = new()
    px = img.load()
    for y in range(S):
        for x in range(S):
            edge = min(x, y, S - 1 - x, S - 1 - y) / (8 * K)
            px[x, y] = shade((44, 28, 18), (0.7 + 0.3 * min(1, edge)) * (1 + rng.uniform(-0.05, 0.05))) + (255,)
    d = ImageDraw.Draw(img)
    for _ in range(30):
        x, y, r = rng.uniform(8, 56) * K, rng.uniform(8, 56) * K, rng.uniform(4, 7) * K
        green = shade((70, 124, 52), rng.uniform(0.8, 1.15))
        d.ellipse([x - r, y - r, x + r, y + r], fill=shade(green, 0.7) + (255,))
        d.ellipse([x - r * 0.85, y - r * 0.9, x + r * 0.7, y + r * 0.6], fill=green + (255,))
        for _ in range(int(6 * K)):                            # trichomes and orange pistils
            hx, hy = x + rng.uniform(-r, r) * 0.8, y + rng.uniform(-r, r) * 0.8
            tone = (230, 226, 210) if rng.random() < 0.7 else (214, 128, 52)
            d.point((hx, hy), fill=tone + (255,))
    return img


def sight(rng):
    img = new()
    px = img.load()
    for y in range(S):
        for x in range(S):
            u, v = x / K, y / K
            c = shade(AMBER, 1.05 - 0.2 * v / 64 + rng.uniform(-0.02, 0.02)) if v > 18 else shade((60, 52, 44), 1 + rng.uniform(-0.03, 0.03))
            if 18 <= v < 19:
                c = mix(c, (255, 230, 170), 0.6)                      # the meniscus
            if 8 <= u < 10:
                c = mix(c, (255, 255, 255), 0.45)
            px[x, y] = c + (255,)
    return img


def grille(rng):
    img = iron(rng)
    d = ImageDraw.Draw(img)
    for y in range(6, 60, 8):
        d.rectangle(k(6, y, 57, y + 3), fill=(18, 18, 20, 255))
        d.rectangle(k(6, y + 4, 57, y + 4.5), fill=shade(IRON, 1.4) + (255,))
    for x, y in ((3, 3), (60, 3), (3, 60), (60, 60)):
        rivet(img.load(), x * K, y * K, IRON, r=1.2 * K)
    return img


def textures():
    TEX.mkdir(parents=True, exist_ok=True)
    rng = random.Random(4200)
    for old in TEX.glob('*.png'):
        old.unlink()
    out = {
        'panel': walnut_panel(rng), 'frame': walnut_frame(rng), 'brass': brushed(rng, BRASS, patina=0.4),
        'copper': copper_plate(rng), 'pipe': pipe(rng), 'iron': iron(rng), 'glass': glass(),
        'liquid': liquid(rng, False), 'liquid_lit': liquid(rng, True), 'gauge': gauge(rng), 'dial': dial(rng),
        'wheel': wheel(rng), 'deck': deck(rng), 'mouth': mouth(rng), 'plates': plates(rng), 'lamp': lamp(False),
        'lamp_lit': lamp(True), 'hopper': hopper_inside(rng), 'sight': sight(rng), 'grille': grille(rng),
    }
    for name, img in out.items():
        img.save(TEX / f'{name}.png')
    return list(out)


# ---- model -----------------------------------------------------------------------------------

def auto_uv(face, f, t):
    """Vanilla's automatic UV: the face's position within the block."""
    (x0, y0, z0), (x1, y1, z1) = f, t
    uv = {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
          'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
          'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}[face]
    return [min(16, max(0, v)) for v in uv]


ALL = ('north', 'south', 'east', 'west', 'up', 'down')


def box(f, t, tex, faces=ALL, overrides=None, rot=None, light=False):
    """An element; {@code overrides} maps a face to (texture, uv) to override; rot = (axis, angle, origin)."""
    e = {'from': [round(v, 4) for v in f], 'to': [round(v, 4) for v in t], 'faces': {}}
    for face in faces:
        tx, uv = (overrides or {}).get(face, (tex, None))
        e['faces'][face] = {'texture': tx, 'uv': uv if uv is not None else auto_uv(face, f, t)}
    if rot:
        axis, angle, origin = rot
        e['rotation'] = {'angle': angle, 'axis': axis, 'origin': origin}
    if light:
        e['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    return e


# A round part as a "pixel circle": a centre bar, two shoulders and two caps (half-widths
# 0.5 x 0.29, 0.41 x 0.12, 0.29 x 0.09 of its width). They only touch, never overlap, so no two
# faces share a plane and flicker (z-fighting); each piece skips its face that meets the centre.
DISC = [(0.50, -0.29, 0.29, None), (0.41, 0.29, 0.41, -1), (0.41, -0.41, -0.29, 1), (0.29, 0.41, 0.50, -1), (0.29, -0.50, -0.41, 1)]


def pixel_circle(axes, centre, w, t0, t1, tex, faces, light=False):
    """axes: (a axis, b axis, thickness axis) as 0/1/2; faces named per world axis."""
    names = {0: ('west', 'east'), 1: ('down', 'up'), 2: ('north', 'south')}
    a_axis, b_axis, t_axis = axes
    out = []
    for half_a, b0, b1, inward in DISC:
        f, t = [0, 0, 0], [0, 0, 0]
        f[a_axis], t[a_axis] = centre[0] - w * half_a, centre[0] + w * half_a
        f[b_axis], t[b_axis] = centre[1] + w * b0, centre[1] + w * b1
        f[t_axis], t[t_axis] = t0, t1
        skip = None if inward is None else names[b_axis][0 if inward < 0 else 1]
        out.append(box(f, t, tex, tuple(x for x in faces if x != skip), light=light))
    return out


def cylinder_y(cx, cz, w, y0, y1, tex, faces=ALL, light=False):
    """A vertical round part."""
    return pixel_circle((0, 2, 1), (cx, cz), w, y0, y1, tex, faces, light)


def cylinder_z(cx, cy, w, z0, z1, tex, faces=ALL):
    """A round part facing north/south (its axis along z)."""
    return pixel_circle((0, 1, 2), (cx, cy), w, z0, z1, tex, faces)


def cylinder_x(cy, cz, w, x0, x1, tex, faces=ALL):
    """A round part facing east/west (its axis along x)."""
    return pixel_circle((1, 2, 0), (cy, cz), w, x0, x1, tex, faces)


def elements(lit):
    E = []
    # --- The cabinet -------------------------------------------------------------------------
    E.append(box([1, 0.15, 1], [15, 1, 15], '#iron'))                                      # plinth (clear of the feet's soles)
    for x in (0.5, 13.5):
        for z in (0.5, 13.5):
            E += cylinder_y(x + 1, z + 1, 2.2, 0, 1.4, '#brass')                             # feet
    E.append(box([1.75, 1.15, 1.75], [14.25, 7.5, 14.25], '#panel'))                       # the panels (top hidden by the rails)
    for x in (1.25, 13.25):
        for z in (1.25, 13.25):
            E.append(box([x, 1, z], [x + 1.5, 8, z + 1.5], '#frame'))                       # corner posts
    for y0, y1 in ((1, 1.75), (7.0, 8)):                                                    # rails, top and bottom
        E.append(box([2.75, y0, 1.25], [13.25, y1, 1.75], '#frame'))
        E.append(box([2.75, y0, 14.25], [13.25, y1, 14.75], '#frame'))
        E.append(box([1.25, y0, 2.75], [1.75, y1, 13.25], '#frame'))
        E.append(box([14.25, y0, 2.75], [14.75, y1, 13.25], '#frame'))
    for x in (6.0, 9.75):                                                                   # front mullions between the ports
        E.append(box([x, 1.75, 1.3], [x + 0.25, 7, 1.75], '#frame'))
    E.append(box([0.75, 8, 0.75], [15.25, 8.75, 15.25], '#brass', overrides={'up': ('#deck', [0, 0, 16, 16])}))   # the deck
    for f, t in (([0.75, 8.75, 0.75], [15.25, 9.0, 1.1]), ([0.75, 8.75, 14.9], [15.25, 9.0, 15.25]),
                 ([0.75, 8.75, 1.1], [1.1, 9.0, 14.9]), ([14.9, 8.75, 1.1], [15.25, 9.0, 14.9])):
        E.append(box(f, t, '#brass'))                                                   # its raised lip
    E.append(box([1.0, 7.65, 1.0], [15.0, 7.82, 15.0], '#iron'))                           # its shadow line, clear of panel and posts
    # Front: three ports (flange, nozzle, mouth) under engraved plates 1, 2, 3.
    for i, cx in enumerate((3.875, 7.875, 11.875)):
        cy = 3.9
        E += cylinder_z(cx, cy, 3.0, 0.9, 1.75, '#brass')
        E += cylinder_z(cx, cy, 1.9, 0.35, 0.9, '#iron')
        E.append(box([cx - 0.95, cy - 0.95, 0.15], [cx + 0.95, cy + 0.95, 0.2], '#mouth', faces=('north',),
                     overrides={'north': ('#mouth', [0, 0, 16, 16])}))
        E.append(box([cx - 1.0, 5.85, 1.45], [cx + 1.0, 6.85, 1.75], '#brass',
                     overrides={'north': ('#plates', [0, i * 4, 8, i * 4 + 4])}))
    # East: the method dial (bezel, face, pointer, knob).
    E += cylinder_x(4.5, 8, 3.6, 14.6, 15.3, '#brass')
    E.append(box([15.4, 2.7, 6.2], [15.45, 6.3, 9.8], '#dial', faces=('east',), overrides={'east': ('#dial', [0, 0, 16, 16])}))
    E.append(box([15.45, 4.35, 7.85], [15.75, 6.0, 8.15], '#iron'))   # pointer
    E.append(box([15.6, 4.1, 7.6], [16.0, 4.9, 8.4], '#brass'))                         # the knob (too small to round)
    # West: the press's crank wheel and hub.
    E.append(box([0.55, 2.0, 5.5], [0.6, 7.0, 10.5], '#wheel', faces=('west', 'east'),
                 overrides={'west': ('#wheel', [0, 0, 16, 16]), 'east': ('#wheel', [16, 0, 0, 16])}))
    E += cylinder_x(4.5, 8, 1.4, 0.2, 1.25, '#iron')
    E.append(box([-0.3, 6.3, 7.75], [0.4, 6.8, 8.25], '#brass'))                          # the crank handle, clear of the wheel
    # Back: a vent grille.
    E.append(box([3.25, 2.25, 14.75], [12.75, 6.5, 15.0], '#grille', faces=('south', 'east', 'west', 'up', 'down')))
    # --- On the deck ---------------------------------------------------------------------------
    # The hopper (front left): an iron throat, a copper funnel holding buds, a brass rim.
    E += cylinder_y(3.5, 3.5, 1.6, 8.75, 9.75, '#iron')
    E.append(box([2.3, 9.75, 2.3], [4.7, 10.35, 4.7], '#copper'))
    E.append(box([1.9, 10.35, 1.9], [5.1, 10.95, 5.1], '#copper'))
    E.append(box([1.5, 10.95, 1.5], [5.5, 11.85, 5.5], '#copper', overrides={'up': ('#hopper', [0, 0, 16, 16])}))
    for f, t in (([1.25, 11.75, 1.25], [5.75, 12.25, 1.75]), ([1.25, 11.75, 5.25], [5.75, 12.25, 5.75]),
                 ([1.25, 11.75, 1.75], [1.75, 12.25, 5.25]), ([5.25, 11.75, 1.75], [5.75, 12.25, 5.25])):
        E.append(box(f, t, '#brass'))
    # The still (centre): a brass base ring, a glass chamber of terpene in a brass frame, a top
    # ring, a copper dome, a cap and a valve wheel.
    cx, cz = 8.75, 8.5
    E += cylinder_y(cx, cz, 6.4, 8.75, 9.6, '#brass')
    E.append(box([cx - 2.35, 9.75, cz - 2.35], [cx + 2.35, 12.6, cz + 2.35], '#liquid', light=lit))
    E.append(box([cx - 2.6, 9.6, cz - 2.6], [cx + 2.6, 14.0, cz + 2.6], '#glass'))
    for dx in (-2.75, 2.25):
        for dz in (-2.75, 2.25):
            E.append(box([cx + dx, 9.4, cz + dz], [cx + dx + 0.5, 14.2, cz + dz + 0.5], '#brass'))    # frame posts, into the rings
    E += cylinder_y(cx, cz, 6.4, 14.0, 14.6, '#brass')
    E += cylinder_y(cx, cz, 4.6, 14.6, 15.4, '#copper')
    E += cylinder_y(cx, cz, 2.2, 15.4, 15.9, '#brass')
    E.append(box([cx - 0.25, 15.9, cz - 0.25], [cx + 0.25, 16.4, cz + 0.25], '#iron'))         # valve stem
    # The valve wheel: a brass rim, a cross of iron spokes (the crossing one split, so they only
    # touch) and an iron hub over the stem.
    for f, t in (([cx - 1.45, 16.4, cz - 1.45], [cx + 1.45, 16.7, cz - 1.15]), ([cx - 1.45, 16.4, cz + 1.15], [cx + 1.45, 16.7, cz + 1.45]),
                 ([cx - 1.45, 16.4, cz - 1.15], [cx - 1.15, 16.7, cz + 1.15]), ([cx + 1.15, 16.4, cz - 1.15], [cx + 1.45, 16.7, cz + 1.15])):
        E.append(box(f, t, '#brass'))
    E.append(box([cx - 1.15, 16.45, cz - 0.15], [cx - 0.4, 16.62, cz + 0.15], '#iron'))
    E.append(box([cx + 0.4, 16.45, cz - 0.15], [cx + 1.15, 16.62, cz + 0.15], '#iron'))
    E.append(box([cx - 0.15, 16.45, cz - 1.15], [cx + 0.15, 16.62, cz - 0.4], '#iron'))
    E.append(box([cx - 0.15, 16.45, cz + 0.4], [cx + 0.15, 16.62, cz + 1.15], '#iron'))
    E.append(box([cx - 0.4, 16.3, cz - 0.4], [cx + 0.4, 16.85, cz + 0.4], '#iron'))
    # The condenser (back right): a copper tube wound with coil rings, a cap; the swan neck from the dome.
    kx, kz = 13.25, 13.0
    E += cylinder_y(kx, kz, 1.3, 8.75, 14.2, '#pipe')
    for i, y in enumerate((9.35, 10.3, 11.25, 12.2, 13.15)):
        off = (-0.2, 0.0, 0.2, 0.0)[i % 4], (0.0, 0.2, 0.0, -0.2)[i % 4]
        E += cylinder_y(kx + off[0], kz + off[1], 3.6, y, y + 0.45, '#pipe')
    E += cylinder_y(kx, kz, 2.4, 14.2, 14.6, '#brass')
    E.append(box([kx - 0.4, 14.6, kz - 0.4], [kx + 0.4, 15.5, kz + 0.4], '#pipe'))
    # The swan neck leaves the dome at its edge (clear of the cap, whose top it would otherwise sit
    # just under) and runs to the condenser, its top well clear of both the dome and the cap.
    dist = math.hypot(kx - cx, kz - cz)
    ux, uz = (kx - cx) / dist, (kz - cz) / dist
    sx, sz = cx + ux * 1.9, cz + uz * 1.9
    neck = math.hypot(kx - sx, kz - sz)
    mx, mz = (kx + sx) / 2, (kz + sz) / 2
    E.append(box([mx - neck / 2, 15.0, mz - 0.4], [mx + neck / 2, 15.65, mz + 0.4], '#pipe', rot=('y', -45, [mx, 15.3, mz])))
    # The resin tank (back left), its sight glass and lid; a pipe to the still.
    tx, tz = 3.4, 12.4
    E += cylinder_y(tx, tz, 3.8, 8.75, 12.6, '#copper')
    E.append(box([tx - 0.7, 9.4, tz - 2.1], [tx + 0.7, 12.0, tz - 2.05], '#sight', faces=('north',),
                 overrides={'north': ('#sight', [0, 0, 16, 16])}))
    E += cylinder_y(tx, tz, 3.2, 12.6, 13.1, '#brass')
    E.append(box([tx + 1.6, 10.0, tz - 0.35], [cx - 2.6, 10.7, tz + 0.35], '#pipe'))
    # The gauge (front right) on its stand, and the status lamp.
    gx, gy = 13.0, 11.6
    E.append(box([gx - 0.45, 8.75, 2.75], [gx + 0.45, gy - 1.2, 3.65], '#iron'))
    E += cylinder_z(gx, gy, 3.0, 2.35, 3.2, '#brass')
    E.append(box([gx - 1.5, gy - 1.5, 2.15], [gx + 1.5, gy + 1.5, 2.2], '#gauge', faces=('north',),
                 overrides={'north': ('#gauge', [0, 0, 16, 16])}))
    E.append(box([14.15, 8.75, 1.2], [14.85, 9.55, 1.9], '#brass'))                     # lamp base, inside the deck lip
    E.append(box([14.25, 9.55, 1.3], [14.75, 10.05, 1.8], '#lamp', light=lit))
    return E


def mirror_x(elements):
    """Mirrors a north-facing layout left to right as seen from the front (x -> 16 - x)."""
    out = []
    for e in elements:
        e = json.loads(json.dumps(e))
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        e['from'], e['to'] = [round(16 - x1, 4), y0, z0], [round(16 - x0, 4), y1, z1]
        faces = {}
        for face, spec in e['faces'].items():
            face2 = {'east': 'west', 'west': 'east'}.get(face, face)
            custom = spec['uv'] != auto_uv(face, [x0, y0, z0], [x1, y1, z1])
            if not custom:
                spec['uv'] = auto_uv(face2, e['from'], e['to'])
            faces[face2] = spec
        e['faces'] = faces
        if 'rotation' in e:
            r = e['rotation']
            r['origin'] = [round(16 - r['origin'][0], 4), r['origin'][1], r['origin'][2]]
            if r['axis'] in ('y', 'z'):
                r['angle'] = -r['angle']
        out.append(e)
    return out


TRANSLUCENT = ('#glass', '#liquid')


def model():
    """A NeoForge composite: the solid machine as cutout, only the glass and terpene translucent.
    (One translucent model sorts all its faces back to front by their centres, which on a model this
    detailed draws solid faces over nearer ones: flicker and see-through sides.)"""
    for lit in (False, True):
        textures = dict({'particle': 'fourtwenty:block/terpinator/brass'},
                        **{k: f'fourtwenty:block/terpinator/{k}' for k in (
                            'panel', 'frame', 'brass', 'copper', 'pipe', 'iron', 'glass', 'gauge', 'dial', 'wheel', 'mouth',
                            'plates', 'hopper', 'sight', 'grille', 'deck')},
                        liquid=f'fourtwenty:block/terpinator/liquid{"_lit" if lit else ""}',
                        lamp=f'fourtwenty:block/terpinator/lamp{"_lit" if lit else ""}')
        els = mirror_x(elements(lit))
        solid = [e for e in els if not any(f['texture'] in TRANSLUCENT for f in e['faces'].values())]
        clear = [e for e in els if any(f['texture'] in TRANSLUCENT for f in e['faces'].values())]
        write(ASSETS / f'models/block/terpinator{"_lit" if lit else ""}.json', {
            'parent': 'minecraft:block/block', 'loader': 'neoforge:composite', 'ambientocclusion': False,
            'textures': textures,
            'children': {
                'machine': {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
                            'textures': textures, 'elements': solid},
                'glass': {'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent', 'ambientocclusion': False,
                          'textures': textures, 'elements': clear}},
            'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
                        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
                        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
                        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
                        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
                        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}})
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    write(ASSETS / 'blockstates/terpinator.json', {'variants': {
        f'facing={f},lit={str(lit).lower()}': dict({'model': f'fourtwenty:block/terpinator{"_lit" if lit else ""}'}, **({'y': r} if r else {}))
        for f, r in rot.items() for lit in (False, True)}})
    write(ASSETS / 'models/item/terpinator.json', {'parent': 'fourtwenty:block/terpinator'})


def data():
    write(DATA / 'loot_table/blocks/terpinator.json', {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'fourtwenty:terpinator'}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    # Glass over copper either side of a brewing stand, over copper either side of a weed bud.
    write(DATA / 'recipe/terpinator.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['GGG', 'CSC', 'CBC'],
        'key': {'G': {'item': 'minecraft:glass'}, 'C': {'item': 'minecraft:copper_ingot'}, 'S': {'item': 'minecraft:brewing_stand'},
                'B': {'item': 'fourtwenty:weed_bud'}},
        'result': {'id': 'fourtwenty:terpinator', 'count': 1}})
    tag = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'
    current = json.loads(tag.read_text(encoding='utf-8')) if tag.exists() else {'replace': False, 'values': []}
    current['values'] = sorted(set(current['values']) | {'fourtwenty:terpinator'})
    write(tag, current)


TEXT = {
    'block.fourtwenty.terpinator': 'Terpinator',
    'container.fourtwenty.terpinator': 'Terpinator',
    'itemGroup.fourtwenty.hemp_and_terpenes': 'Hemp & Terpenes',
    'item.fourtwenty.terpene.unknown.base': 'Undiscovered Base Terpene',
    'item.fourtwenty.terpene.unknown.accord': 'Undiscovered Accord',
    'item.fourtwenty.terpene.unknown.final': 'Undiscovered Terpene',
    'item.fourtwenty.terpene.unknown.resin': 'Undiscovered Resin',
    'item.fourtwenty.terpene.unknown.hint': 'Make one in a Terpinator, or get one from a friend, to learn its name.',
    'item.fourtwenty.terpene.roles': 'Sets the family (%s), hue (%s) or tone (%s)',
    'item.fourtwenty.terpene.accord': 'Accord · %s. Finish it with a base in port 3.',
    'item.fourtwenty.terpene.final': 'Surround with 8 hemp blocks to dye them.',
    'gui.fourtwenty.terpinator.guide': 'Guide',
    'gui.fourtwenty.terpinator.unknown': '???',
    'gui.fourtwenty.terpinator.method': '%s makes %s',
    'gui.fourtwenty.terpinator.method.heat': 'Heat',
    'gui.fourtwenty.terpinator.method.cold': 'Cold',
    'gui.fourtwenty.terpinator.method.pressure': 'Pressure',
    'gui.fourtwenty.terpinator.method.time': 'Time',
    'gui.fourtwenty.terpinator.step.press': 'Put weed buds in the press (top left) and press Start. Two buds make one resin.',
    'gui.fourtwenty.terpinator.step.refine': 'Pick a method on the dial, then Start the refiner: resin becomes a base terpene.',
    'gui.fourtwenty.terpinator.step.all_bases': 'Refine all four bases: stop the refiner, empty its output, change the method. (%s of 4)',
    'gui.fourtwenty.terpinator.step.accord': 'Put two base terpenes in ports 1 and 2, then Start the blender to make an accord.',
    'gui.fourtwenty.terpinator.step.finish': 'Put your accord in port 1 and a base in port 3 to finish a colour.',
    'gui.fourtwenty.terpinator.step.free': 'You know %s of 64 colours. The Guide shows what\'s left.',
    'gui.fourtwenty.terpinator.advice.port1_first': 'Every blend starts in port 1.',
    'gui.fourtwenty.terpinator.advice.add_port2': 'Put a second base in port 2.',
    'gui.fourtwenty.terpinator.advice.clear_port3': 'Two bases make an accord: empty port 3.',
    'gui.fourtwenty.terpinator.advice.clear_port2': 'An accord uses ports 1 and 2: empty 2.',
    'gui.fourtwenty.terpinator.advice.add_port3': 'Add a base in port 3: it sets the tone.',
    'gui.fourtwenty.terpinator.advice.empty': 'Bases in ports 1 and 2 make an accord.',
    'gui.fourtwenty.terpinator.start': 'Start',
    'gui.fourtwenty.terpinator.stop': 'Stop',
    'gui.fourtwenty.terpinator.stage.press': 'Press',
    'gui.fourtwenty.terpinator.stage.refine': 'Refine',
    'gui.fourtwenty.terpinator.stage.blend': 'Blend',
    'gui.fourtwenty.terpinator.status.stopped': 'stopped',
    'gui.fourtwenty.terpinator.status.running': 'running',
    'gui.fourtwenty.terpinator.status.needs_buds': 'needs buds',
    'gui.fourtwenty.terpinator.status.needs_resin': 'needs resin',
    'gui.fourtwenty.terpinator.status.needs_terpenes': 'waiting for terpenes',
    'gui.fourtwenty.terpinator.status.full': 'output full',
    'gui.fourtwenty.terpinator.status.empty_output': 'empty output first',
    'gui.fourtwenty.terpinator.refuse.output': 'Empty the output slot before refining, so one run never mixes two bases.',
    'gui.fourtwenty.terpinator.refuse.locked': 'Stop the refiner to change the method.',
    'gui.fourtwenty.guide.title': 'Terpinator Guide',
    'gui.fourtwenty.guide.progress': '%s of %s terpenes discovered',
    'gui.fourtwenty.guide.first_session': 'First session',
    'gui.fourtwenty.guide.step.press': 'Press buds into resin',
    'gui.fourtwenty.guide.step.refine': 'Refine a base terpene',
    'gui.fourtwenty.guide.step.all_bases': 'Refine all four bases',
    'gui.fourtwenty.guide.step.accord': 'Blend an accord',
    'gui.fourtwenty.guide.step.finish': 'Finish your first colour',
    'gui.fourtwenty.guide.legend': 'What each base does',
    'gui.fourtwenty.guide.port': 'Port %s',
    'gui.fourtwenty.guide.rule': 'Port 1 picks the family, port 2 the hue, port 3 the tone. Learn one row and you can predict the rest.',
    'gui.fourtwenty.guide.map': 'The 64 colours',
    'gui.fourtwenty.guide.columns': 'Columns, by port 3: pale, bright, dusky, deep',
    'gui.fourtwenty.guide.how.base': 'Refine resin by %s',
    'gui.fourtwenty.guide.how.accord': 'Port 1: %s, port 2: %s',
    'gui.fourtwenty.guide.hue': '%s, in the %s family',
    'gui.fourtwenty.guide.how.final': 'Finish %s with %s (%s)',
    'gui.fourtwenty.guide.dye': 'Dyes hemp: surround it with 8 hemp blocks',
    'gui.fourtwenty.toast.discovered': 'Terpene discovered',
}


def lang():
    path = ASSETS / 'lang/en_us.json'
    data = json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    data.update(TEXT)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def preview(out, names):
    """Every texture side by side, for checking by eye."""
    out.mkdir(parents=True, exist_ok=True)
    sheet = Image.new('RGBA', (S * 10, S * 2), (40, 44, 42, 255))
    for i, name in enumerate(names):
        sheet.alpha_composite(Image.open(TEX / f'{name}.png'), ((i % 10) * S, (i // 10) * S))
    sheet.resize((S * 10 * 2, S * 2 * 2), Image.NEAREST).save(out / 'terpinator_textures.png')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview')
    args = ap.parse_args()
    names = textures()
    model()
    data()
    lang()
    if args.preview:
        preview(Path(args.preview), names)
    print(f'terpinator generated: {len(elements(False))} elements, {len(names)} textures')
