#!/usr/bin/env python3
"""Roads in three tiers (HempPathBlock): cobble path, paved road, yellow brick road.

A road is a 15/16-high path block. Like the rugs, each quarter of its top is one of five cases, by which
of its neighbours carry on the same road (RoadShape; the client's RoadModel picks the parts as the
chunk is drawn, so the blockstates name only a fallback):
  outer  both of the quarter's outer sides open (a corner of the whole road)
  ns     the quarter's north/south side open (an edge running east-west)
  ew     its east/west side open (an edge running north-south)
  inner  both sides joined but the diagonal open (the notch of an L)
  full   field
Unlike a rug, a road's shape never changes (it's a full slab), so nothing is turned: each case has
one texture drawn for the whole block in world orientation (borders along the north and south edges
for ns, round all four corners for outer...) and each quarter shows its own quarter of it. The field
repeats every block, so the paving runs on unbroken across a whole road, and borders line up block
to block along an edge.

Sides are a cut through the road (the surface's own edge on top, what it's laid on, the footing),
with corner quoins where two open sides meet (a road's outer corners). The underside is structural (foundation flagstones, a concrete coffer grid, a brick bed)
and joins like the top, with a rim round the outside.

Writes per tier: 5 top and 5 underside textures (64x64, 4 px per model pixel), two side textures
(plain and with corner quoins), the part models (4 quarters x 5 cases, each as a road's 0-15/16 box,
a bottom slab's 0-7/16 and a top slab's 8-15/16; and caps to a full block under another road),
blockstates, item models (a
lone block: outer corners all round), loot, recipes, names, and the fourtwenty:paths / mineable
tags. A double slab is a road's quarters. Stairs are plain models of separate steps, each bordered
all round on top and on its front (stair_model), from step textures per arrangement of high quarters. A top-down preview of a sample road per
tier goes to the path given with --preview. Run again after edits.
"""
import collections
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'
T = 64                 # texture size
PX = T // 16           # texture pixels per model pixel
BW = 3 * PX            # border band width (3 model pixels)
CASES = ('outer', 'ns', 'ew', 'inner', 'full')
TIERS = [('cobble_path', 'Cobble Path'), ('paved_road', 'Paved Road'), ('yellow_brick_road', 'Yellow Brick Road')]
QUARTERS = {  # quarter: (x0, z0, its north/south side, its east/west side, its diagonal)
    'nw': (0, 0, 'north', 'west', 'northwest'), 'ne': (8, 0, 'north', 'east', 'northeast'),
    'sw': (0, 8, 'south', 'west', 'southwest'), 'se': (8, 8, 'south', 'east', 'southeast'),
}


def write(path, obj, compact=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(obj, separators=(',', ':'), ensure_ascii=False) if compact else json.dumps(obj, indent=2, ensure_ascii=False)
    path.write_text(text + '\n', encoding='utf-8')


def clamp(v):
    return max(0, min(255, int(round(v))))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(clamp(v * f) for v in c)


def pdist(ax, ay, bx, by, period=T):
    """Distance on the torus: the field repeats every block."""
    dx = abs(ax - bx) % period
    dy = abs(ay - by) % period
    return math.hypot(min(dx, period - dx), min(dy, period - dy)), ((bx - ax + period / 2) % period - period / 2, (by - ay + period / 2) % period - period / 2)


# ---- fields (period 64, so they tile across blocks) -------------------------------------------

def cobble_field(rng):
    """Rounded fieldstones of mixed sizes in packed earth, lit from the north-west."""
    seeds = []
    n = 6
    for i in range(n):
        for j in range(n):
            x = (i + 0.5 + rng.uniform(-0.32, 0.32)) * T / n
            y = (j + 0.5 + rng.uniform(-0.32, 0.32)) * T / n
            base = rng.choice([(138, 132, 122), (150, 142, 128), (122, 118, 112), (160, 150, 134), (132, 124, 110),
                               (118, 126, 100), (164, 138, 104)])          # greys, taupes, a mossy one, an ochre one
            seeds.append((x, y, base, rng.uniform(0.92, 1.08)))
    earth, earth_dark, grass = (96, 78, 58), (74, 60, 44), (88, 98, 60)      # grass: a muted olive, close to the earth
    img = Image.new('RGB', (T, T))
    px = img.load()
    for y in range(T):
        for x in range(T):
            ds = sorted(((pdist(x + 0.5, y + 0.5, sx, sy)[0], k) for k, (sx, sy, _, _) in enumerate(seeds)))
            (d1, k1), (d2, _) = ds[0], ds[1]
            sx, sy, base, tone = seeds[k1]
            edge = d2 - d1
            if edge < 2.2:                                  # the joint: packed earth, a little grass
                c = earth if edge > 1.1 else earth_dark
                if rng.random() < 0.03:
                    c = grass
            else:
                _, (vx, vy) = pdist(sx, sy, x + 0.5, y + 0.5)
                r = max(1e-6, d1 + edge * 0.5)
                light = 1.0 - 0.22 * (vx + vy) / (r * 1.4)   # brighter toward the north-west of each stone
                rim = min(1.0, (edge - 2.2) / 3.0)           # darker at the stone's rounded edge
                c = shade(base, tone * (0.82 + 0.18 * rim) * light)
                if rng.random() < 0.05:
                    c = shade(c, rng.uniform(0.9, 1.1))
            px[x, y] = c
    return img


def paved_field(rng):
    """Smooth pale flagstones in a running bond (32 x 16 px, rows staggered by 16), fine dark joints."""
    img = Image.new('RGB', (T, T))
    px = img.load()
    tones = {}
    for row in range(4):
        for col in range(2):
            tones[(row, col)] = rng.choice([(178, 178, 172), (186, 184, 176), (170, 170, 166), (190, 188, 182), (174, 176, 170)])
    for y in range(T):
        row = y // 16
        for x in range(T):
            xs = (x + (16 if row % 2 else 0)) % T
            col = xs // 32
            u, v = xs % 32, y % 16
            base = tones[(row, col)]
            if u == 0 or v == 0:
                c = (112, 112, 110)                         # the joint
            elif u == 1 or v == 1:
                c = shade(base, 1.07)                        # the lit edge just inside it
            elif u == 31 or v == 15:
                c = shade(base, 0.9)                         # the shaded edge
            else:
                c = shade(base, rng.uniform(0.97, 1.03))     # a faint speckle
            px[x, y] = c
    return img


def brick_field(rng):
    """Golden bricks (16 x 8 px) in a running bond: butter to amber, a rare emerald, the odd sparkle."""
    img = Image.new('RGB', (T, T))
    px = img.load()
    golds = [(222, 192, 104), (230, 202, 120), (212, 180, 96), (226, 198, 132), (206, 172, 92), (234, 210, 144)]
    bricks = {}
    for row in range(T // 8):
        for col in range(T // 16):
            c = rng.choice(golds)
            if rng.random() < 0.02:
                c = (92, 160, 104)                           # an emerald brick, now and then
            bricks[(row, col)] = c
    mortar = (196, 172, 128)
    for y in range(T):
        row = y // 8
        for x in range(T):
            xs = (x + (8 if row % 2 else 0)) % T
            col = xs // 16
            u, v = xs % 16, y % 8
            base = bricks[(row, col)]
            if u == 0 or v == 0:
                c = mortar
            elif v == 1 or u == 1:
                c = shade(base, 1.08)
            elif v == 7 or u == 15:
                c = shade(base, 0.86)
            else:
                c = shade(base, rng.uniform(0.96, 1.04))
                if rng.random() < 0.006:
                    c = (255, 250, 228)                      # a sparkle
            px[x, y] = c
    return img


# ---- borders: a band along an edge, drawn for the north edge and turned for the others -------

def cobble_band(rng):
    """Rounded edging stones of alternating lengths bedded in earth, grass tufts along the outside."""
    band = {}
    stones = [(1, 17), (19, 31), (33, 47), (49, 63)]
    tones = [(124, 116, 104), (136, 126, 110), (116, 112, 106), (130, 120, 102)]
    earth, grass = (88, 72, 52), (88, 124, 54)
    for x in range(T):
        for d in range(BW):
            c = earth
            if d <= 1 and rng.random() < 0.45:
                c = grass if rng.random() < 0.8 else (110, 146, 66)
            for k, (a, b) in enumerate(stones):
                if a <= x < b and 1 <= d <= BW - 2:
                    # A rounded rectangle: inside if the corner-rounded distance fits.
                    cx = min(x - a, b - 1 - x)
                    cy = min(d - 1, BW - 2 - d)
                    r = 2.5
                    if cx >= r or cy >= r or math.hypot(r - cx, r - cy) <= r:
                        light = 1.1 if d <= 3 else (0.86 if d >= BW - 4 else 1.0)
                        c = shade(tones[k], light * rng.uniform(0.96, 1.04))
            band[(x, d)] = c
    return band


def paved_band(rng):
    """A granite kerb: long bevelled stones, a dark gutter line on the road side."""
    band = {}
    for x in range(T):
        u = x % 32
        for d in range(BW):
            if d == BW - 1:
                c = (84, 84, 86)                              # the gutter between kerb and paving
            elif u == 0:
                c = (78, 78, 80)                              # joint between kerb stones
            elif d == 0:
                c = (96, 96, 100)                             # the kerb's outer face
            elif d <= 2:
                c = (150, 150, 154)                           # the kerb's lit bevel
            else:
                c = shade((124, 124, 128), rng.uniform(0.96, 1.04))
            band[(x, d)] = c
    return band


def brick_band(rng):
    """Amber soldier-course bricks; a cream trim with a gold scroll wave and a diamond at each crest."""
    band = {}
    ambers = [(186, 124, 56), (198, 136, 64), (176, 114, 50)]
    trim, gold, deep = (232, 214, 172), (214, 168, 58), (176, 120, 40)
    t0 = BW - 5                                         # the trim's first row (5 rows deep)
    for x in range(T):
        brick = x // 4
        wave = t0 + 2 + round(1.4 * math.sin(2 * math.pi * x / 16))
        for d in range(BW):
            if d >= t0:
                c = trim
                if d == wave:
                    c = gold                            # the scroll
                if x % 16 == 4 and d in (t0, t0 + 1):
                    c = deep if d == t0 else gold       # a diamond over each crest
                if x % 16 in (3, 5) and d == t0 + 1:
                    c = gold
            elif x % 4 == 0 or d == 0:
                c = (150, 98, 46) if d else (126, 80, 36)
            else:
                c = shade(ambers[brick % 3], 1.05 if d < 3 else (0.92 if d > t0 - 3 else 1.0))
            band[(x, d)] = c
    return band


def corner_rosette():
    """An eight-petal gold-and-cream rosette for the yellow brick road's outer corners (BW x BW)."""
    out = {}
    c = (BW - 1) / 2
    for y in range(BW):
        for x in range(BW):
            r = math.hypot(x - c, y - c)
            ang = math.atan2(y - c, x - c)
            petal = 0.5 + 0.5 * math.cos(ang * 8)
            if r < 1.5:
                out[(x, y)] = (176, 120, 40)
            elif r < 2.3:
                out[(x, y)] = (240, 214, 140)
            elif r < 2.6 + 3.2 * petal:
                out[(x, y)] = (214, 168, 58) if r > 3.6 else (236, 200, 104)
            else:
                out[(x, y)] = (186, 124, 56)
    return out


def apply_band(px, band, edge):
    """Paint a band (keyed (along, depth) for the north edge) on one edge of the block."""
    for (a, d), c in band.items():
        if edge == 'north':
            px[a, d] = c
        elif edge == 'south':
            px[T - 1 - a, T - 1 - d] = c
        elif edge == 'west':
            px[d, T - 1 - a] = c
        else:                                                   # east
            px[T - 1 - d, a] = c


def corner_square(px, field_px, corner, painter):
    x0 = 0 if 'w' in corner else T - BW
    y0 = 0 if 'n' in corner else T - BW
    for y in range(BW):
        for x in range(BW):
            px[x0 + x, y0 + y] = painter(x if 'w' in corner else BW - 1 - x, y if 'n' in corner else BW - 1 - y)


def case_texture(field, band, corner_paint, case, inner_paint):
    img = field.copy()
    px = img.load()
    if case in ('ns', 'outer'):
        apply_band(px, band, 'north')
        apply_band(px, band, 'south')
    if case in ('ew', 'outer'):
        apply_band(px, band, 'west')
        apply_band(px, band, 'east')
    if case == 'outer':
        for corner in ('nw', 'ne', 'sw', 'se'):
            corner_square(px, None, corner, corner_paint)
    if case == 'inner':
        for corner in ('nw', 'ne', 'sw', 'se'):
            corner_square(px, None, corner, inner_paint)
    return img


EDGE_BITS = {'north': 1, 'east': 2, 'south': 4, 'west': 8}       # RoadShape.N, E, S, W
CORNER_BITS = {'nw': 1, 'ne': 2, 'sw': 4, 'se': 8}                 # RoadShape's notch bits, >> 4


def quarter_tile(field, band, corner_paint, inner_paint, q, edges, notches):
    """One quarter of a road surface's top (32 x 32, cut from where it sits in the block, so the paving
    runs on): the border along the given edges (block edges or the quarter's inner ones, wherever the
    surface ends or steps), mitred where two meet, and the inner-corner notch at the given corners,
    where two joined edges meet a gap. RoadModel picks one per quarter from the blocks round it."""
    img = field.copy()
    px = img.load()
    H = T // 2
    x0, y0 = QUARTERS[q][0] * PX, QUARTERS[q][1] * PX
    for a in range(H):
        for k in range(BW):
            if 'north' in edges:
                px[x0 + a, y0 + k] = band[(x0 + a, k)]
            if 'south' in edges:
                px[x0 + a, y0 + H - 1 - k] = band[(T - 1 - x0 - a, k)]
            if 'west' in edges:
                px[x0 + k, y0 + a] = band[(T - 1 - y0 - a, k)]
            if 'east' in edges:
                px[x0 + H - 1 - k, y0 + a] = band[(y0 + a, k)]
    for corner in CORNER_BITS:
        ns, ew = ('north' if corner[0] == 'n' else 'south'), ('west' if corner[1] == 'w' else 'east')
        if ns in edges and ew in edges:
            paint = corner_paint(corner)
        elif corner in notches:
            paint = inner_paint
        else:
            continue
        cx = x0 if ew == 'west' else x0 + H - BW
        cy = y0 if ns == 'north' else y0 + H - BW
        for y in range(BW):
            for x in range(BW):
                px[cx + x, cy + y] = paint(x if ew == 'west' else BW - 1 - x, y if ns == 'north' else BW - 1 - y)
    return img.crop((x0, y0, x0 + H, y0 + H))


SW = 2 * PX            # a step front's top and bottom border: 2 model pixels, the road's 3 drawn thinner


def front_tile(field, band, thin, half, low, high):
    """A step's front, one quarter wide (32 x 32, the half of the block's width it sits in): thin
    borders along its top and bottom, the road's own at the ends given (low: its u = 0 end), where it
    doesn't run on into the next front or round a corner."""
    x0 = half * (T // 2)
    img = field.crop((x0, 0, x0 + T // 2, T // 2))
    px = img.load()
    for x in range(T // 2):
        for k in range(SW):
            px[x, k] = thin[(x0 + x, k)]
            px[x, T // 2 - 1 - k] = thin[(T - 1 - x0 - x, k)]
    for y in range(T // 2):
        for k in range(BW):
            if low:
                px[k, y] = band[(T - 1 - y, k)]
            if high:
                px[T // 2 - 1 - k, y] = band[(y, k)]
    return img


def tops_and_fronts(block, tier, tex_dir, field, band, mitre, rosette, inner):
    """Every quarter top (per quarter: each set of bordered edges, each set of notches its joined
    corners allow; the yellow brick road's rosette variant 'r' at a quarter's block corner) and every
    step front tile, named as RoadModel asks for them."""
    for old in list(tex_dir.glob(f'{block}_step_*.png')) + list(tex_dir.glob(f'{block}_top_*.png')) + list(tex_dir.glob(f'{block}_front_*.png')):
        old.unlink()
    for q in QUARTERS:
        for e in range(16):
            edges = {d for d, b in EDGE_BITS.items() if e & b}
            free = [c for c in CORNER_BITS if ('north' if c[0] == 'n' else 'south') not in edges and ('west' if c[1] == 'w' else 'east') not in edges]
            for n in range(16):
                notches = {c for c, b in CORNER_BITS.items() if n & b}
                if not notches <= set(free):
                    continue
                name = f'{block}_top_{q}_{e:x}{n:x}'
                quarter_tile(field, band, lambda c: mitre, inner, q, edges, notches).save(tex_dir / f'{name}.png')
                outer = {'north' if q[0] == 'n' else 'south', 'west' if q[1] == 'w' else 'east'}
                if rosette and outer <= edges:                  # a road's block corner: the rosette
                    quarter_tile(field, band, lambda c, q=q: rosette if c == q else mitre, inner, q, edges, notches).save(tex_dir / f'{name}r.png')
    thin = {(a, d): band[(a, round(d * (BW - 1) / (SW - 1)))] for a in range(T) for d in range(SW)}
    for half in (0, 1):
        for ends in range(4):
            front_tile(field, band, thin, half, ends & 1, ends & 2).save(tex_dir / f'{block}_front_{half}{ends}.png')


def strata(tier, rng):
    """A side face as a cut through the road (64 x 64; rows 0-3 sit above the 15/16 top and never show).
    The surface's own edge on top, then what it's laid on, down to the footing."""
    img = Image.new('RGB', (T, T))
    px = img.load()
    for y in range(T):
        for x in range(T):
            n = rng.uniform(0.95, 1.05)
            if tier == 0:                                       # cobble: edging stones, earth, gravel, footing stones
                if y < 16:
                    u = x % 16
                    top = 4 + round(2.5 * (1 - math.cos(2 * math.pi * u / 16)) / 2)   # rounded stone tops
                    c = (84, 70, 52) if y < top or u in (0, 15) else shade((124, 116, 104), 1.08 if y < top + 3 else 0.96)
                elif y < 30:
                    c = (96, 78, 58) if rng.random() > 0.1 else (128, 120, 106)       # packed earth, pebbles
                elif y < 46:
                    c = rng.choice([(116, 108, 98), (98, 92, 86), (132, 124, 112)])    # gravel bed
                else:
                    u = (x + (12 if (y // 9) % 2 else 0)) % 24
                    c = (70, 60, 50) if u == 0 or y in (46, 55) else (96, 92, 88)       # rough footing stones
            elif tier == 1:                                     # paved: kerb, mortar bed, crushed stone, concrete footing
                if y < 16:
                    c = (78, 78, 80) if x % 32 == 0 else ((150, 150, 154) if y < 7 else (124, 124, 128))
                elif y < 19:
                    c = (92, 92, 96)                                                     # mortar bed
                elif y < 42:
                    c = rng.choice([(140, 138, 134), (122, 120, 118), (152, 150, 146)])  # crushed stone base
                else:
                    c = (104, 104, 108) if x % 16 == 0 or y == 42 else (132, 132, 136)  # concrete footing, formwork lines
            else:                                               # yellow brick: soldier bricks, trim, footing, plinth
                if y < 16:
                    c = (150, 98, 46) if x % 4 == 0 else shade((198, 136, 64), 1.06 if y < 7 else 0.96)
                elif y < 20:
                    c = (214, 168, 58) if y == 18 and x % 8 == 4 else (232, 214, 172)    # cream trim, gold dots
                elif y < 54:
                    row = (y - 20) // 8
                    xs = (x + (8 if row % 2 else 0)) % T
                    c = (196, 172, 128) if (y - 20) % 8 == 0 or xs % 16 == 0 else ((216, 186, 104) if (xs // 16 + row) % 3 else (204, 172, 96))
                else:
                    c = (150, 96, 54) if y == 54 or x % 16 == 0 else (172, 112, 64)      # terracotta plinth
            px[x, y] = shade(c, n)
    return img


def quoin(tier, x, y):
    """The colour of a corner quoin at column x (0 = the corner, BW wide) of a side face's row y, or None."""
    if x >= BW:
        return None
    if tier == 0:                                               # stacked rough stones
        course = (y - 4) // 10
        edge = (y - 4) % 10 == 0 or x == BW - 1
        return (70, 58, 44) if edge else shade((118, 110, 98) if course % 2 else (106, 100, 92), 1.06 if x < 3 else 0.97)
    if tier == 1:                                               # a chamfered granite corner block
        if x == BW - 1 or y in (16, 42):
            return (84, 84, 88)
        return (160, 160, 164) if x < 2 else (138, 138, 142)
    course = (y - 4) // 8                                       # classic long-and-short cream quoins, gold edged
    width = BW if course % 2 == 0 else BW - 5
    if x >= width:
        return None
    if (y - 4) % 8 == 0 or x == width - 1:
        return (214, 168, 58)
    return (236, 222, 186) if x > 0 else (246, 236, 208)


def side_texture(tier, rng, corners):
    """A side face's texture. With corners, a quoin stands at both ends (each side quarter shows the
    half with its own corner, at that end)."""
    img = strata(tier, rng)
    if corners:
        px = img.load()
        for y in range(4, T):
            for x in range(BW):
                for xx in (x, T - 1 - x):
                    c = quoin(tier, x, y)
                    if c is not None:
                        px[xx, y] = c
    return img


# A cut's body, per tier: (first row below the surface's edge, first footing row, the body's repeat):
# earth over gravel, crushed stone, brick courses (two, for the bond).
BODY = [(16, 46, 14), (19, 42, 23), (20, 54, 16)]
STACKS = {'u': (True, False), 'd': (False, True), 'ud': (True, True)}


def stacked_side(img, tier, up, down, corners):
    """A side for a road in a stack: with a road block on it (up) the body runs on to the top instead
    of the surface's edge; standing on one (down) it runs on to the bottom instead of the footing. So a
    stack's sides read as one wall, bordered only round the outside, as its tops are."""
    top_end, foot, period = BODY[tier]
    out = img.copy()
    src, px = img.load(), out.load()
    for y in range(T):
        if up and y < top_end:
            row = top_end + (y - top_end) % period
        elif down and y >= foot:
            row = foot - period + (y - foot) % period
        else:
            continue
        for x in range(T):
            px[x, y] = src[x, row]
    if corners:                                                 # the quoins run the whole height
        for y in range(T):
            for x in range(BW):
                for xx in (x, T - 1 - x):
                    c = quoin(tier, x, y % (T - 4) + 4 if y < 4 else y)
                    if c is not None:
                        px[xx, y] = c
    return out


# ---- the underside: structural, and joining like the top -------------------------------------

def cobble_under(rng):
    """Big foundation flagstones, irregular, bedded in dark earth."""
    seeds = [((i + 0.5 + rng.uniform(-0.3, 0.3)) * T / 3, (j + 0.5 + rng.uniform(-0.3, 0.3)) * T / 3,
              rng.choice([(92, 88, 84), (102, 96, 88), (86, 84, 82)])) for i in range(3) for j in range(3)]
    img = Image.new('RGB', (T, T))
    px = img.load()
    for y in range(T):
        for x in range(T):
            ds = sorted(pdist(x + 0.5, y + 0.5, sx, sy)[0] for sx, sy, _ in seeds)
            k = min(range(len(seeds)), key=lambda i: pdist(x + 0.5, y + 0.5, seeds[i][0], seeds[i][1])[0])
            px[x, y] = (62, 52, 42) if ds[1] - ds[0] < 1.6 else shade(seeds[k][2], rng.uniform(0.95, 1.05))
    return img


def paved_under(rng):
    """A concrete coffer grid: bevelled 32-pixel panels between ribs."""
    img = Image.new('RGB', (T, T))
    px = img.load()
    for y in range(T):
        for x in range(T):
            u, v = x % 32, y % 32
            if u < 3 or v < 3:
                c = (146, 146, 150)                                 # the rib
            elif u < 5 or v < 5:
                c = (112, 112, 116)                                 # the bevel down into the coffer
            else:
                c = shade((124, 124, 128), rng.uniform(0.97, 1.03))
            px[x, y] = c
    return img


def brick_under(rng):
    """Deeper ochre bricks in a stretcher bond: the road's brick bed seen from below."""
    img = Image.new('RGB', (T, T))
    px = img.load()
    for y in range(T):
        row = y // 8
        for x in range(T):
            xs = (x + (8 if row % 2 else 0)) % T
            if y % 8 == 0 or xs % 16 == 0:
                c = (150, 118, 76)
            else:
                c = shade((184, 140, 72) if (xs // 16 + row) % 3 else (172, 128, 64), rng.uniform(0.96, 1.04))
            px[x, y] = c
    return img


def under_band(tier):
    """The rim round the outside of the underside (keyed (along, depth) for the north edge)."""
    band = {}
    for x in range(T):
        for d in range(BW):
            if tier == 0:
                c = (74, 64, 54) if (x // 6 + d // 4) % 2 else (84, 74, 62)       # a rubble rim
                if d == BW - 1:
                    c = (58, 48, 40)
            elif tier == 1:
                c = (96, 96, 100) if d == 0 or d == BW - 1 else ((154, 154, 158) if d < 3 else (136, 136, 140))   # perimeter beam
            else:
                c = (232, 214, 172) if d in (BW - 3, BW - 2) else ((126, 80, 36) if d == 0 or x % 4 == 0 else (186, 124, 56))   # amber rim, cream bead
            band[(x, d)] = c
    return band


# ---- models -----------------------------------------------------------------------------------

def quarter_model(q, sides):
    """A quarter of a road: its top and bottom, and the outer sides given (the open ones)."""
    x0, z0, ns, ew, _ = QUARTERS[q]
    x1, z1 = x0 + 8, z0 + 8
    side_uv = {'north': [16 - x1, 1, 16 - x0, 16], 'south': [x0, 1, x1, 16], 'west': [z0, 1, z1, 16], 'east': [16 - z1, 1, 16 - z0, 16]}
    faces = {'up': {'texture': '#top', 'uv': [x0, z0, x1, z1]},
             'down': {'texture': '#bottom', 'uv': [x0, 16 - z1, x1, 16 - z0], 'cullface': 'down'}}
    for side in sides:
        faces[side] = {'texture': '#side', 'uv': side_uv[side], 'cullface': side}
    return {'from': [x0, 0, z0], 'to': [x1, 15, z1], 'faces': faces}


def open_sides(q, case):
    """The quarter's outer sides. All are drawn, joined or not: a side beside a stair's lower tread
    shows above it, and between two roads the faces meet back to back, hidden. (The case still picks
    the texture: corner quoins on an outer corner.)"""
    _, _, ns, ew, _ = QUARTERS[q]
    return [ns, ew]


def case_for(conn, q):
    """The case of quarter q for a set of connected directions (the preview uses this)."""
    _, _, ns, ew, diag = QUARTERS[q]
    a, b = ns in conn, ew in conn
    if not a and not b:
        return 'outer'
    if not a:
        return 'ns'
    if not b:
        return 'ew'
    return 'full' if diag in conn else 'inner'


# ---- slabs and stairs -------------------------------------------------------------------------

def box_faces(q, y0, y1, up_tex, down_tex, sides, extra=()):
    """A quarter box from y0 to y1: its top and bottom, the given outer sides and any inner faces (risers)."""
    x0, z0 = QUARTERS[q][0], QUARTERS[q][1]
    x1, z1 = x0 + 8, z0 + 8
    v0, v1 = 16 - y1, 16 - y0                                   # the side texture pinned to the block, as on a road's full side
    span = {'north': (16 - x1, 16 - x0), 'south': (x0, x1), 'west': (z0, z1), 'east': (16 - z1, 16 - z0)}
    faces = {'up': {'texture': up_tex, 'uv': [x0, z0, x1, z1]},
             'down': {'texture': down_tex, 'uv': [x0, 16 - z1, x1, 16 - z0]}}
    if y0 == 0:
        faces['down']['cullface'] = 'down'
    for side in sides:
        u0, u1 = span[side]
        faces[side] = {'texture': '#side', 'uv': [u0, v0, u1, v1], 'cullface': side}
    for side in extra:                                          # a riser inside the block: no cullface
        u0, u1 = span[side]
        faces[side] = {'texture': '#side', 'uv': [u0, v0, u1, v1]}
    return {'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': faces}


SLAB_Y = {'bottom': (0, 7), 'top': (8, 15), 'double': (0, 15)}


def slab_part(q, case, kind):
    y0, y1 = SLAB_Y[kind]
    return box_faces(q, y0, y1, '#top', '#bottom', open_sides(q, case))


def stair_model(block):
    """A lone road stair, rising east (the item, and the blockstate's stand-in: RoadModel draws placed
    ones): its tops bordered all round the block, its step fronts bordered strips."""
    path = f'fourtwenty:block/path/{block}'
    textures = {'top': f'{path}_outer', 'bottom': f'{path}_bottom_outer', 'side': f'{path}_side_corner', 'particle': f'{path}_side',
                'front0': f'{path}_front_01', 'front1': f'{path}_front_12'}
    elements = []
    for q in QUARTERS:
        x0, z0, ns, ew, _ = QUARTERS[q]
        high = ew == 'east'
        el = box_faces(q, 0, 15 if high else 7, '#top', '#bottom', [ns, ew])
        front = {'texture': f'#front{z0 // 8}', 'uv': [0, 0, 16, 16]}
        if high:                                                # the riser, above the tread to its west
            elements.append({'from': [x0, 7, z0], 'to': [x0 + 8, 15, z0 + 8], 'faces': {'west': front}})
        else:
            el['faces']['west'] = dict(front, cullface='west')
        elements.append(el)
    return {'parent': 'minecraft:block/block', 'ambientocclusion': True, 'textures': textures, 'elements': elements}


def fallback(model):
    """A blockstate naming one model for every state: RoadModel replaces it."""
    return {'variants': {'': {'model': f'fourtwenty:block/{model}'}}}


def slabs_and_stairs(block, label, names):
    slab = f'{block}_slab'
    write(ASSETS / f'models/block/{slab}.json', {'parent': 'minecraft:block/block', 'ambientocclusion': True, 'textures': {
        'top': f'fourtwenty:block/path/{block}_outer', 'bottom': f'fourtwenty:block/path/{block}_bottom_outer',
        'side': f'fourtwenty:block/path/{block}_side_corner', 'particle': f'fourtwenty:block/path/{block}_side'},
        'elements': [slab_part(q, 'outer', 'bottom') for q in QUARTERS]})
    write(ASSETS / f'blockstates/{slab}.json', fallback(slab))
    write(ASSETS / f'models/item/{slab}.json', {'parent': f'fourtwenty:block/{slab}'})
    write(DATA / f'loot_table/blocks/{slab}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
        'type': 'minecraft:item', 'name': f'fourtwenty:{slab}', 'functions': [
            {'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [{'condition': 'minecraft:block_state_property',
             'block': f'fourtwenty:{slab}', 'properties': {'type': 'double'}}]},
            {'function': 'minecraft:explosion_decay'}]}]}]})
    write(DATA / f'recipe/{slab}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['RRR'],
        'key': {'R': {'item': f'fourtwenty:{block}'}}, 'result': {'id': f'fourtwenty:{slab}', 'count': 6}})
    write(DATA / f'recipe/{slab}_from_stonecutting.json', {'type': 'minecraft:stonecutting',
        'ingredient': {'item': f'fourtwenty:{block}'}, 'result': {'id': f'fourtwenty:{slab}', 'count': 2}})
    names[slab] = f'{label} Slab'
    # Stairs.
    stairs = f'{block}_stairs'
    write(ASSETS / f'models/block/{stairs}.json', stair_model(block))
    write(ASSETS / f'blockstates/{stairs}.json', fallback(stairs))
    write(ASSETS / f'models/item/{stairs}.json', {'parent': f'fourtwenty:block/{stairs}'})
    write(DATA / f'loot_table/blocks/{stairs}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
        {'type': 'minecraft:item', 'name': f'fourtwenty:{stairs}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    write(DATA / f'recipe/{stairs}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['R  ', 'RR ', 'RRR'],
        'key': {'R': {'item': f'fourtwenty:{block}'}}, 'result': {'id': f'fourtwenty:{stairs}', 'count': 4}})
    write(DATA / f'recipe/{stairs}_from_stonecutting.json', {'type': 'minecraft:stonecutting',
        'ingredient': {'item': f'fourtwenty:{block}'}, 'result': {'id': f'fourtwenty:{stairs}', 'count': 1}})
    names[stairs] = f'{label} Stairs'
    return [slab, stairs]


# ---- generate ---------------------------------------------------------------------------------

def generate(preview=None):
    tex_dir = ASSETS / 'textures/block/path'
    tex_dir.mkdir(parents=True, exist_ok=True)
    for q in QUARTERS:                                          # the old shared quarter parents
        old = ASSETS / f'models/block/path_quarter_{q}.json'
        if old.exists():
            old.unlink()
    names = collections.OrderedDict()
    extra_blocks = []
    builders = [(cobble_field, cobble_band), (paved_field, paved_band), (brick_field, brick_band)]
    textures = {}
    for tier, ((block, label), (field_fn, band_fn)) in enumerate(zip(TIERS, builders)):
        rng = random.Random(4200 + tier)
        field = field_fn(rng)
        band = band_fn(rng)
        fpx = field.load()
        # Outer corners: the band mitred round the corner (or the yellow brick road's rosette). Inner
        # corners: the band wrapped round the notch, its outer edge at the corner point.
        mitre = lambda x, y, b=band: b[(x, y)] if x >= y else b[(y, x)]
        corner_rosette_paint = lambda x, y, r=corner_rosette(): r[(x, y)]
        corner_paint = corner_rosette_paint if tier == 2 else mitre
        inner_paint = lambda x, y, b=band, f=fpx: b[(8 + x + y, max(x, y))] if max(x, y) < BW else f[x, y]
        textures[block] = {}
        under, uband = [cobble_under, paved_under, brick_under][tier](rng), under_band(tier)
        upx = under.load()
        under_mitre = lambda x, y, b=uband: b[(x, y)] if x >= y else b[(y, x)]
        under_inner = lambda x, y, b=uband, f=upx: b[(8 + x + y, max(x, y))] if max(x, y) < BW else f[x, y]
        for case in CASES:
            img = case_texture(field, band, corner_paint, case, inner_paint)
            img.save(tex_dir / f'{block}_{case}.png')
            textures[block][case] = img
            case_texture(under, uband, under_mitre, case, under_inner).save(tex_dir / f'{block}_bottom_{case}.png')
        tops_and_fronts(block, tier, tex_dir, field, band, mitre, corner_rosette_paint if tier == 2 else None, inner_paint)
        for corners, img in ((False, side_texture(tier, rng, False)), (True, side_texture(tier, rng, True))):
            name = f'{block}_side' + ('_corner' if corners else '')
            img.save(tex_dir / f'{name}.png')
            for v, (up, down) in STACKS.items():
                stacked_side(img, tier, up, down, corners).save(tex_dir / f'{name}_{v}.png')
        # Models: RoadModel draws every placed road, slab and stair from the textures above; the
        # blockstates name a plain block model (a lone block), which the items use too.
        for old in (ASSETS / 'models/block').glob(f'{block}_*.json'):
            old.unlink()
        write(ASSETS / f'models/block/{block}.json', {'parent': 'minecraft:block/block', 'ambientocclusion': True, 'textures': {
            'top': f'fourtwenty:block/path/{block}_outer', 'bottom': f'fourtwenty:block/path/{block}_bottom_outer',
            'side': f'fourtwenty:block/path/{block}_side_corner', 'particle': f'fourtwenty:block/path/{block}_side'},
            'elements': [quarter_model(q, open_sides(q, 'outer')) for q in QUARTERS]})
        write(ASSETS / f'blockstates/{block}.json', fallback(block))
        write(ASSETS / f'models/item/{block}.json', {'parent': f'fourtwenty:block/{block}'})
        write(DATA / f'loot_table/blocks/{block}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
            {'type': 'minecraft:item', 'name': f'fourtwenty:{block}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        names[block] = label
        extra_blocks.extend(slabs_and_stairs(block, label, names))
    # Recipes: a cheap path, upgraded tier by tier.
    write(DATA / 'recipe/cobble_path.json', {'type': 'minecraft:crafting_shapeless', 'category': 'building', 'ingredients': [
        {'item': 'minecraft:cobblestone'}, {'item': 'minecraft:cobblestone'}, {'item': 'minecraft:gravel'}, {'item': 'minecraft:gravel'},
        {'item': 'fourtwenty:weed_bud'}], 'result': {'id': 'fourtwenty:cobble_path', 'count': 4}})
    write(DATA / 'recipe/paved_road.json', {'type': 'minecraft:crafting_shaped', 'category': 'building',
        'pattern': ['PPP', 'PSP', 'PPP'], 'key': {'P': {'item': 'fourtwenty:cobble_path'}, 'S': {'item': 'minecraft:smooth_stone'}},
        'result': {'id': 'fourtwenty:paved_road', 'count': 8}})
    write(DATA / 'recipe/yellow_brick_road.json', {'type': 'minecraft:crafting_shaped', 'category': 'building',
        'pattern': ['PPP', 'PGP', 'PPP'], 'key': {'P': {'item': 'fourtwenty:paved_road'}, 'G': {'item': 'minecraft:gold_ingot'}},
        'result': {'id': 'fourtwenty:yellow_brick_road', 'count': 8}})
    every = [f'fourtwenty:{b}' for b, _ in TIERS] + [f'fourtwenty:{b}' for b in extra_blocks]
    write(DATA / 'tags/item/paths.json', {'replace': False, 'values': every})
    write(DATA / 'tags/block/paths.json', {'replace': False, 'values': every})
    for kind in ('slabs', 'stairs'):                            # vanilla's tags, so they behave as slabs and stairs
        ids = [f'fourtwenty:{b}' for b in extra_blocks if b.endswith('_slab' if kind == 'slabs' else '_stairs')]
        for reg in ('block', 'item'):
            f = RES / f'data/minecraft/tags/{reg}/{kind}.json'
            current = json.loads(f.read_text(encoding='utf-8')) if f.exists() else {'replace': False, 'values': []}
            current['values'] = sorted(set(current['values']) | set(ids))
            write(f, current)
    pick = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'
    current = json.loads(pick.read_text(encoding='utf-8'))
    current['values'] = sorted(set(current['values']) | set(every))
    write(pick, current)
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    for block, label in names.items():
        data[f'block.fourtwenty.{block}'] = label
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    if preview:
        save_preview(preview, textures)
    print(f'{len(TIERS)} roads with slabs and stairs ({len(extra_blocks)} more blocks)')


def save_preview(path, textures):
    """A top-down sample road per tier: a crossroads with an L, a wide plaza and a lone block."""
    layout = ['..........', '....##....', '....##....', '##########', '##########', '....##....', '....###...', '....###..#', '..........']
    rows = len(layout)
    cols = len(layout[0])
    cells = {(x, z) for z, row in enumerate(layout) for x, ch in enumerate(row) if ch == '#'}
    dirs = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0),
            'northeast': (1, -1), 'northwest': (-1, -1), 'southeast': (1, 1), 'southwest': (-1, 1)}
    scale = 2
    out = Image.new('RGB', ((cols * T + 16) * len(textures), rows * T), (60, 120, 50))
    for i, (block, tex) in enumerate(textures.items()):
        ox = i * (cols * T + 16)
        for (x, z) in cells:
            conn = {d for d, (dx, dz) in dirs.items() if (x + dx, z + dz) in cells}
            for q, (qx, qz, *_rest) in QUARTERS.items():
                case = case_for(conn, q)
                piece = tex[case].crop((qx * PX, qz * PX, (qx + 8) * PX, (qz + 8) * PX))
                out.paste(piece, (ox + x * T + qx * PX, z * T + qz * PX))
    out = out.resize((out.width // scale, out.height // scale), Image.NEAREST)
    out.save(path)


if __name__ == '__main__':
    args = sys.argv[1:]
    generate(args[args.index('--preview') + 1] if '--preview' in args else None)
