#!/usr/bin/env python3
"""Rugs in the 64 terpene colours, the 16 Minecraft colours and Natural Hemp: rugs that join into larger rugs, each colour with
its own pattern.

A rug (RugBlock) knows which of its eight neighbours are the same rug. Each quarter of its model is
one of five cases, drawn for the north-west quarter and turned for the others by the blockstate:
outer corner (rounded, inset a pixel so the floor shows), north edge, west edge, inner corner (the
notch of an L) and interior. So the border runs only round the outside of the whole rug and the
pattern flows across the inside.

Designs (tools/rug_designs.py): each colour has its own field and border, themed on its name (begonias,
waves, a fairy ring of toadstools, stars and moons, plaid...), drawn as smooth shapes. A rug's quarters
are one quarter's texture turned, so a field is drawn once and repeated under the quarter turns about
the block's centre (and from the neighbouring blocks), which makes every block alike and every motif
at a corner or edge join up with its neighbours'. Every pixel averages SS x SS samples: smooth edges.

Writes per rug: 5 quarter models + 5 textures (128x128, the quarter at 16 px per pixel, edges anti-aliased), a multipart
blockstate, an item model (the four outer corners), loot, recipes (2 hemp -> 3; a terpene in the
middle of 8 rugs recolours them), names and the fourtwenty:rugs tag. Run again after edits.
"""
import collections
import json
import math
import multiprocessing
import sys
import random
from pathlib import Path

import numpy as np
from PIL import Image

import rug_designs as D

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'
TERP = json.loads((ROOT / 'tools/terpenes.json').read_text(encoding='utf-8'))
Q = 8                 # a quarter is 8x8 pixels
PX = 16               # texture pixels per model pixel (128x128 a quarter)
SS = 4                # samples per side for an anti-aliased pixel
I = 1.0               # the inset from the block edge on an open side
R = 2.5               # the outer corner radius
BW = 3.0              # the border's width
CASES = ('outer', 'edge_n', 'edge_w', 'inner', 'full')


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def clamp(v):
    return max(0, min(255, int(round(v))))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(clamp(v * f) for v in c)


def lum(c):
    return (0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]) / 255


# ---- geometry (the north-west quarter, x and z in 0..8) ---------------------------------------

def box(x0, z0, x1, z1, faces, y0=0.0, y1=1.0):
    """One rug element: its top shows its own place in the quarter's texture, its sides plain rim colour."""
    # Sides show plain rim colour: the quarter's top-left corner lies outside the rug in every case
    # with an edge (that is what the texture paints rim there), so a side never picks up the border's
    # pattern (that was the specks on rounded corners and the stripy, turned-looking edges).
    rim = [0, 0, 0.5, 0.5]
    uvs = {'up': ([x0 * 2, z0 * 2, x1 * 2, z1 * 2], 0),
           'north': (rim, 0), 'south': (rim, 0), 'west': (rim, 0), 'east': (rim, 0)}
    faces_out = {}
    for f in faces:
        uv, rot = uvs[f]
        spec = {'texture': '#rug', 'uv': [round(v, 4) for v in uv]}
        if rot:
            spec['rotation'] = rot
        faces_out[f] = spec
    return {'from': [round(x0, 4), round(y0, 4), round(z0, 4)], 'to': [round(x1, 4), round(y1, 4), round(z1, 4)], 'faces': faces_out}


def region(case, inset, radius, y0, y1):
    """The rug's footprint in this quarter at one inset (the edge on open sides), as touching boxes."""
    if case == 'full':
        return [box(0, 0, Q, Q, ('up',), y0, y1)]
    if case == 'edge_n':
        return [box(0, inset, Q, Q, ('up', 'north'), y0, y1)]
    if case == 'edge_w':
        return [box(inset, 0, Q, Q, ('up', 'west'), y0, y1)]
    if case == 'inner':
        # Everything but the notch [0, inset]^2, in three pieces that meet corner to corner (two pieces
        # left a T-junction where the notch's walls meet: a hairline crack).
        return [box(inset, 0, Q, inset, ('up', 'west'), y0, y1), box(0, inset, inset, Q, ('up', 'north'), y0, y1),
                box(inset, inset, Q, Q, ('up',), y0, y1)]
    # Outer corner, built on a grid that's the same both ways: the corner square [inset, c]^2 in cells
    # (kept where they fall inside the curve), strips beside it and a block for the rest. Every piece
    # meets its neighbours corner to corner, so there are no T-junctions (hairline cracks that flicker
    # with the sky behind them), and the quarter's edges carry the same vertices both ways, so turned
    # neighbouring quarters line up too.
    c = I + R
    span = c - inset
    p = [inset + span * f for f in (0, 0.1, 0.2, 0.3, 0.5, 0.8, 1.0)]
    n = len(p) - 1
    inside = [[math.hypot(c - (p[k] + p[k + 1]) / 2, c - (p[j] + p[j + 1]) / 2) <= radius for k in range(n)] for j in range(n)]
    els = []
    for j in range(n):
        for k in range(n):
            if not inside[j][k]:
                continue
            faces = ['up']
            if j == 0 or not inside[j - 1][k]:
                faces.append('north')
            if k == 0 or not inside[j][k - 1]:
                faces.append('west')
            els.append(box(p[k], p[j], p[k + 1], p[j + 1], tuple(faces), y0, y1))
    for j in range(n):
        els.append(box(c, p[j], Q, p[j + 1], ('up', 'north') if j == 0 else ('up',), y0, y1))
    for k in range(n):
        els.append(box(p[k], c, p[k + 1], Q, ('up', 'west') if k == 0 else ('up',), y0, y1))
    els.append(box(c, c, Q, Q, ('up',), y0, y1))
    return els


BEVEL = 0.5           # the outer half pixel of the rug is half as tall: the edge steps down to the floor


def quarter(case):
    """One case: a half-height base out to the edge and the full-height top inset a further half pixel,
    so every open edge is a soft step rather than a wall. Faces only where the rug's edge is."""
    if case == 'full':
        return region('full', I, R, 0, 1)
    return region(case, I, R, 0, 0.5) + region(case, I + BEVEL, R - BEVEL, 0.5, 1)


# Quarters: (blockstate y turn, the side NW's north turns into, the side NW's west turns into, diagonal).
QUARTERS = [(0, 'north', 'west', 'northwest'), (90, 'east', 'north', 'northeast'),
            (180, 'south', 'east', 'southeast'), (270, 'west', 'south', 'southwest')]


def multipart(model):
    parts = []
    for y, a, b, diag in QUARTERS:
        conditions = {'outer': {a: 'false', b: 'false'}, 'edge_n': {a: 'false', b: 'true'}, 'edge_w': {a: 'true', b: 'false'},
                      'inner': {a: 'true', b: 'true', diag: 'false'}, 'full': {a: 'true', b: 'true', diag: 'true'}}
        for case, when in conditions.items():
            apply = {'model': f'{model}_{case}'}
            if y:
                apply['y'] = y
            parts.append({'when': when, 'apply': apply})
    return {'multipart': parts}


def turned(elements, k):
    """The elements turned k quarter turns clockwise about the block's centre (for the item model)."""
    out = []
    for e in elements:
        e = json.loads(json.dumps(e))
        for _ in range(k):
            (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
            e['from'], e['to'] = [16 - z1, y0, x0], [16 - z0, y1, x1]
            e['faces'] = {{'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}.get(f, f): v for f, v in e['faces'].items()}
            if 'up' in e['faces']:
                e['faces']['up']['rotation'] = (e['faces']['up'].get('rotation', 0) + 90) % 360
        out.append(e)
    return out


# ---- patterns ----------------------------------------------------------------------------------

def depth(case, x, z):
    """Distance in from the rug's edge (inf in the interior), and the coordinate along that edge."""
    if case == 'full':
        return math.inf, 0
    if case == 'edge_n':
        return z - I, x
    if case == 'edge_w':
        return x - I, z
    if case == 'inner':
        if x >= I and z >= I:
            return math.hypot(x - I, z - I), (x if x > z else z)
        return (x - I, z) if x >= I else (z - I, x)
    c = I + R
    if x < c and z < c:
        return R - math.hypot(c - x, c - z), (x if z < x else z)
    return min(x - I, z - I), (x if z - I < x - I else z)


def siblings(code):
    """The colour's own hue in all four tones (pale, bright, dusky, deep), from the codex."""
    fam, hue, _ = code.split('_')
    hexes = {c['code']: c['hex'] for c in TERP['colors']}
    return [rgb(hexes[f'{fam}_{hue}_{t}']) for t in ('lim', 'pin', 'lav', 'myr')]


def palette_for(code, hexc):
    """(field, accent, deep, light) for a rug."""
    base = rgb(hexc)
    if code == 'natural' or code.startswith('mc_'):
        return base, shade(base, 0.78), shade(base, 0.55), mix(base, (250, 244, 228), 0.6)
    pale, bright, dusky, deep = siblings(code)
    others = [t for t in (pale, bright, dusky, deep) if t != base]
    # The accent: the sibling furthest in lightness; deep and light for lines and outlines.
    accent = max(others, key=lambda t: abs(lum(t) - lum(base)))
    darkest = min((pale, bright, dusky, deep), key=lum)
    lightest = max((pale, bright, dusky, deep), key=lum)
    deep_c = darkest if lum(darkest) < lum(base) - 0.05 else shade(base, 0.6)
    light_c = lightest if lum(lightest) > lum(base) + 0.05 else mix(base, (250, 248, 240), 0.55)
    return base, accent, deep_c, light_c


# The quarter turns about the block's centre (a rug's quarters are one quarter's texture turned, so a
# design need only turn into itself, not mirror: motifs swirl rather than make mandalas), and the three
# neighbouring blocks whose motifs can reach into a north-west quarter.
TURNS = [(1, 0, 0, 1), (0, -1, 1, 0), (-1, 0, 0, -1), (0, 1, -1, 0)]
NEIGHBOURS = [(0, 0), (16, 0), (0, 16), (16, 16)]
ROLES = ('field', 'accent', 'deep', 'light', 'leaf', 'stem', 'extra', 'rim')


def colours_for(code, hexc):
    """Every role's colour: the rug's own tones, then the design's choices from the palette."""
    hexes = {c['code']: c['hex'] for c in TERP['colors']}
    base, accent, deep, light = palette_for(code, hexc)
    out = {'field': base, 'accent': accent, 'deep': deep, 'light': light, 'leaf': rgb(hexes['pin_lim_lav']), 'stem': rgb(hexes['pin_lim_myr'])}
    tones = dict(zip(('pale', 'bright', 'dusky', 'deep'), siblings(code))) if code != 'natural' and not code.startswith('mc_') else {}
    for role, ref in D.DESIGNS[code][4].items():
        out[role] = base if ref == 'self' else tones[ref] if ref in tones else rgb(ref if ref.startswith('#') else hexes[ref])
    out.setdefault('extra', out['accent'])
    out.setdefault('band', out['deep'] if lum(base) > 0.35 else mix(out['deep'], out['accent'], 0.35))
    out['rim'] = shade(out['band'], 0.85)
    return out


def depths(case, X, Z):
    """depth() for arrays: distance in from the rug's edge (inf inside) and the coordinate along it."""
    if case == 'full':
        return np.full_like(X, np.inf), np.zeros_like(X)
    if case == 'edge_n':
        return Z - I, X
    if case == 'edge_w':
        return X - I, Z
    if case == 'inner':
        corner = (X >= I) & (Z >= I)
        d = np.where(corner, np.hypot(X - I, Z - I), np.where(X >= I, X - I, Z - I))
        s = np.where(corner, np.where(X > Z, X, Z), np.where(X >= I, Z, X))
        return d, s
    c = I + R
    round_ = (X < c) & (Z < c)
    d = np.where(round_, R - np.hypot(c - X, c - Z), np.minimum(X - I, Z - I))
    s = np.where(round_, np.where(Z < X, X, Z), np.where(Z - I < X - I, X, Z))
    return d, s


def field_roles(code, n):
    """The field's role at every sample of the north-west quarter (n x n): the design repeated under the
    square's symmetries and from the neighbouring blocks."""
    fn, params = D.FIELDS[D.DESIGNS[code][0]], D.DESIGNS[code][1]
    g = (np.arange(n) + 0.5) / n * Q - 8                          # block coordinates of the NW quarter
    X, Z = np.meshgrid(g, g)
    layers = None
    for tx, tz in NEIGHBOURS:
        px, pz = X + tx, Z + tz
        for a, b, c, d in TURNS:
            ls = fn(a * px + b * pz, c * px + d * pz, params)
            if layers is None:
                layers = [[np.asarray(m, bool).copy(), r] for m, r in ls]
            else:
                for k, (m, r) in enumerate(ls):
                    layers[k][0] |= m
    out = np.zeros((n, n), np.int8)
    for m, r in layers:
        out[m] = r
    return out


def texture(case, code, colours, field, rng):
    """One quarter: the field, the border over it, every edge anti-aliased (SS x SS samples a pixel)."""
    n = Q * PX * SS
    g = (np.arange(n) + 0.5) / n * Q
    X, Z = np.meshgrid(g, g)
    d, s = depths(case, X, Z)
    roles = field.copy()
    band = d < BW
    roles[band] = 8                                               # the border's band
    for m, r in D.BORDERS[D.DESIGNS[code][2]](d, s, D.DESIGNS[code][3]):
        roles[m & band & (d >= 0.75) & (d < BW - 0.25)] = r
    roles[d < 0.75] = D.RIM                                       # the rim, and outside the rug (only its sides sample that)
    lut = np.array([colours[k] for k in ROLES] + [colours['band']], np.float64)
    m = Q * PX
    rgbs = lut[roles].reshape(m, SS, m, SS, 3).mean(axis=(1, 3))
    # A fine, quiet weave: a twill of threads a model pixel apart, and a little grain.
    yy, xx = np.mgrid[0:m, 0:m]
    twill = np.sin((xx + yy) * np.pi / 2) * 0.012 + np.sin((xx - yy) * np.pi / 4) * 0.006
    grain = np.array([rng.uniform(-0.006, 0.006) for _ in range(m * m)]).reshape(m, m)
    weave = np.where(depths(case, (xx + 0.5) / PX, (yy + 0.5) / PX)[0] < 1.0, 0.5, 1.0) * (twill + grain)
    l = (0.2126 * rgbs[..., 0] + 0.7152 * rgbs[..., 1] + 0.0722 * rgbs[..., 2]) / 255
    out = rgbs * (1 + weave[..., None]) + 255 * (weave * (0.3 - 0.25 * l))[..., None]
    return Image.fromarray(np.clip(np.round(out), 0, 255).astype(np.uint8), 'RGB')


# ---- everything ------------------------------------------------------------------------------

def render(i, code, hexc):
    """Rug i's five quarter textures."""
    colours, rng = colours_for(code, hexc), random.Random(9000 + i)
    field = field_roles(code, Q * PX * SS)
    return [texture(case, code, colours, field, rng) for case in CASES]


def preview(codes, out):
    """A 3 x 2 block rug of each code, side by side, from the written textures (quarters turned as the blockstate turns them)."""
    tex = ASSETS / 'textures/block/rug'
    n = Image.open(tex / f'{codes[0]}_full.png').size[0]
    W, H = 3, 2
    sheet = Image.new('RGB', (len(codes) * (W * 2 * n + n // 2), H * 2 * n), (40, 40, 40))
    for k, code in enumerate(codes):
        for gz in range(H * 2):
            for gx in range(W * 2):
                # this quarter's two outward neighbours and diagonal, in the NW frame after turning
                sx, sz = (-1 if gx % 2 == 0 else 1), (-1 if gz % 2 == 0 else 1)
                inside = lambda x, z: 0 <= x < W * 2 and 0 <= z < H * 2
                ns, ew, dg = inside(gx, gz + sz), inside(gx + sx, gz), inside(gx + sx, gz + sz)
                turns = {(-1, -1): 0, (1, -1): 1, (1, 1): 2, (-1, 1): 3}[(sx, sz)]
                a, b = (ns, ew) if turns % 2 == 0 else (ew, ns)     # the NW frame's north and west
                case = 'outer' if not a and not b else 'edge_n' if not a else 'edge_w' if not b else 'full' if dg else 'inner'
                img = Image.open(tex / f'{code}_{case}.png').rotate(-90 * turns)
                sheet.paste(img, (k * (W * 2 * n + n // 2) + gx * n, gz * n))
    sheet.save(out)


def generate():
    tex = ASSETS / 'textures/block/rug'
    tex.mkdir(parents=True, exist_ok=True)
    for old in tex.glob('*.png'):
        old.unlink()
    old = ASSETS / 'models/block/rug.json'
    if old.exists():
        old.unlink()
    for code in [c['code'] for c in TERP['colors']] + ['natural']:      # the old one-piece rug models
        stale = ASSETS / f'models/block/rug_{code}.json'
        if stale.exists():
            stale.unlink()
    for case in CASES:
        write(ASSETS / f'models/block/rug_{case}.json', {'parent': 'minecraft:block/block',   # ambient occlusion on: soft contact
                                                        'textures': {'particle': '#rug'}, 'elements': quarter(case)})
    rugs = [(c['code'], c['name'], c['hex']) for c in TERP['colors']] + [('natural', TERP['natural']['name'], TERP['natural']['hex'])] + \
           [(c['code'], c['name'], c['hex']) for c in TERP['minecraft']]      # after Natural: the grain is seeded by position
    names, seen = collections.OrderedDict(), {}
    with multiprocessing.Pool() as pool:
        images = pool.starmap(render, [(i, code, hexc) for i, (code, _, hexc) in enumerate(rugs)])
    for i, (code, name, hexc) in enumerate(rugs):
        for case, img in zip(CASES, images[i]):
            img.save(tex / f'{code}_{case}.png')
            write(ASSETS / f'models/block/rug_{code}_{case}.json', {'parent': f'fourtwenty:block/rug_{case}',
                                                                    'textures': {'rug': f'fourtwenty:block/rug/{code}_{case}'}})
        seen.setdefault(json.dumps(D.DESIGNS[code][:4], default=str), []).append(code)
        block = f'rug_{code}'
        write(ASSETS / f'blockstates/{block}.json', multipart(f'fourtwenty:block/{block}'))
        outer = quarter('outer')
        for e in outer:
            for f in e['faces'].values():
                f['texture'] = '#rug'
        write(ASSETS / f'models/item/{block}.json', {'parent': 'minecraft:block/block',
            'textures': {'rug': f'fourtwenty:block/rug/{code}_outer', 'particle': f'fourtwenty:block/rug/{code}_outer'},
            'elements': [e for k in range(4) for e in turned(outer, k)]})
        write(DATA / f'loot_table/blocks/{block}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
            {'type': 'minecraft:item', 'name': f'fourtwenty:{block}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        hemp = 'natural_hemp' if code == 'natural' else f'hemp_{code}'
        write(DATA / f'recipe/{block}.json', {'type': 'minecraft:crafting_shaped', 'category': 'decorations', 'group': 'fourtwenty_rug',
            'pattern': ['HH'], 'key': {'H': {'item': f'fourtwenty:{hemp}'}}, 'result': {'id': f'fourtwenty:{block}', 'count': 3}})
        if code != 'natural':
            write(DATA / f'recipe/{block}_dyeing.json', {'type': 'minecraft:crafting_shaped', 'category': 'decorations',
                'group': 'fourtwenty_rug_dye', 'pattern': ['RRR', 'RTR', 'RRR'],
                'key': {'R': {'tag': 'fourtwenty:rugs'}, 'T': {'item': f'fourtwenty:terpene_{code}'}},
                'result': {'id': f'fourtwenty:{block}', 'count': 8}})
        names[block] = f'{name} Rug'
    write(DATA / 'tags/item/rugs.json', {'replace': False, 'values': [f'fourtwenty:rug_{code}' for code, _, _ in rugs]})
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    for block, label in names.items():
        data[f'block.fourtwenty.{block}'] = label
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    dup = [codes for codes in seen.values() if len(codes) > 1]
    print(f'{len(rugs)} rugs, {len(seen)} distinct patterns' + (f'; shared: {dup}' if dup else ''))


if __name__ == '__main__':
    if '--preview' in sys.argv:
        i = sys.argv.index('--preview')
        preview(sys.argv[i + 2:], sys.argv[i + 1])
    else:
        generate()
