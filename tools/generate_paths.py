#!/usr/bin/env python3
"""Roads in three tiers (HempPathBlock): cobble path, paved road, yellow brick road.

A road is a 15/16-high path block. Like the rugs, it knows which of its eight neighbours are the same
road, and each quarter of its top is one of five cases:
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

Writes per tier: 5 top textures (64x64, 4 px per model pixel) and a side texture, 20 quarter models
(4 quarters x 5 cases), a multipart blockstate, an item model (a lone road: four outer corners), loot,
a recipe, names, and the fourtwenty:paths / mineable tags. A top-down preview of a sample road per
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


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


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
    earth, earth_dark, grass = (96, 78, 58), (74, 60, 44), (92, 120, 58)
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
                if rng.random() < 0.07:
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


def side_texture(tier, rng):
    """The road's sides: the surface's top edge over its bed."""
    img = Image.new('RGB', (T, T))
    px = img.load()
    for y in range(T):
        for x in range(T):
            if tier == 0:
                c = (96, 78, 58) if y > 6 else (130, 124, 112)
                if y > 6 and rng.random() < 0.12:
                    c = (128, 120, 106)
            elif tier == 1:
                c = (150, 150, 154) if y < 8 else ((112, 112, 116) if (y // 16 + x // 32) % 2 else (120, 120, 124))
                if y >= 8 and (y % 16 == 8 or x % 32 == 0):
                    c = (88, 88, 92)
            else:
                row = (y - 8) // 8
                xs = (x + (8 if row % 2 else 0)) % T
                c = (208, 132, 48) if y < 8 else ((236, 196, 74) if (xs // 16 + row) % 3 else (226, 180, 60))
                if y >= 8 and ((y - 8) % 8 == 0 or xs % 16 == 0):
                    c = (238, 226, 196)
            px[x, y] = shade(c, rng.uniform(0.97, 1.03))
    return img


# ---- models -----------------------------------------------------------------------------------

def quarter_model(q):
    x0, z0, ns, ew, _ = QUARTERS[q]
    x1, z1 = x0 + 8, z0 + 8
    side_uv = {'north': [16 - x1, 1, 16 - x0, 16], 'south': [x0, 1, x1, 16], 'west': [z0, 1, z1, 16], 'east': [16 - z1, 1, 16 - z0, 16]}
    faces = {'up': {'texture': '#top', 'uv': [x0, z0, x1, z1]},
             'down': {'texture': '#side', 'uv': [x0, z0, x1, z1], 'cullface': 'down'}}
    for side in (ns, ew):
        faces[side] = {'texture': '#side', 'uv': side_uv[side], 'cullface': side}
    return {'from': [x0, 0, z0], 'to': [x1, 15, z1], 'faces': faces}


def condition(q, case):
    _, _, ns, ew, diag = QUARTERS[q]
    return {'outer': {ns: 'false', ew: 'false'}, 'ns': {ns: 'false', ew: 'true'}, 'ew': {ns: 'true', ew: 'false'},
            'inner': {ns: 'true', ew: 'true', diag: 'false'}, 'full': {ns: 'true', ew: 'true', diag: 'true'}}[case]


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


# ---- generate ---------------------------------------------------------------------------------

def generate(preview=None):
    tex_dir = ASSETS / 'textures/block/path'
    tex_dir.mkdir(parents=True, exist_ok=True)
    names = collections.OrderedDict()
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
        corner_paint = (lambda x, y, r=corner_rosette(): r[(x, y)]) if tier == 2 else mitre
        inner_paint = lambda x, y, b=band, f=fpx: b[(8 + x + y, max(x, y))] if max(x, y) < BW else f[x, y]
        textures[block] = {}
        for case in CASES:
            img = case_texture(field, band, corner_paint, case, inner_paint)
            img.save(tex_dir / f'{block}_{case}.png')
            textures[block][case] = img
        side_texture(tier, rng).save(tex_dir / f'{block}_side.png')
        # Models: one per quarter and case.
        parts = []
        for q in QUARTERS:
            write(ASSETS / f'models/block/path_quarter_{q}.json', {'parent': 'minecraft:block/block', 'ambientocclusion': True,
                                                                    'textures': {'particle': '#side'}, 'elements': [quarter_model(q)]})
            for case in CASES:
                write(ASSETS / f'models/block/{block}_{q}_{case}.json', {'parent': f'fourtwenty:block/path_quarter_{q}', 'textures': {
                    'top': f'fourtwenty:block/path/{block}_{case}', 'side': f'fourtwenty:block/path/{block}_side'}})
                parts.append({'when': condition(q, case), 'apply': {'model': f'fourtwenty:block/{block}_{q}_{case}'}})
        write(ASSETS / f'blockstates/{block}.json', {'multipart': parts})
        write(ASSETS / f'models/item/{block}.json', {'parent': 'minecraft:block/block', 'textures': {
            'top': f'fourtwenty:block/path/{block}_outer', 'side': f'fourtwenty:block/path/{block}_side', 'particle': f'fourtwenty:block/path/{block}_side'},
            'elements': [quarter_model(q) for q in QUARTERS]})
        write(DATA / f'loot_table/blocks/{block}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
            {'type': 'minecraft:item', 'name': f'fourtwenty:{block}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        names[block] = label
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
    write(DATA / 'tags/item/paths.json', {'replace': False, 'values': [f'fourtwenty:{b}' for b, _ in TIERS]})
    write(DATA / 'tags/block/paths.json', {'replace': False, 'values': [f'fourtwenty:{b}' for b, _ in TIERS]})
    pick = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'
    current = json.loads(pick.read_text(encoding='utf-8'))
    current['values'] = sorted(set(current['values']) | {f'fourtwenty:{b}' for b, _ in TIERS})
    write(pick, current)
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    for block, label in names.items():
        data[f'block.fourtwenty.{block}'] = label
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    if preview:
        save_preview(preview, textures)
    print(f'{len(TIERS)} roads, {len(TIERS) * 4 * len(CASES)} quarter models')


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
