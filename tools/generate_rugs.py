#!/usr/bin/env python3
"""Rugs in the 64 terpene colours and Natural Hemp: rugs that join into larger rugs, each colour with
its own pattern.

A rug (RugBlock) knows which of its eight neighbours are the same rug. Each quarter of its model is
one of five cases, drawn for the north-west quarter and turned for the others by the blockstate:
outer corner (rounded, inset a pixel so the floor shows), north edge, west edge, inner corner (the
notch of an L) and interior. So the border runs only round the outside of the whole rug and the
pattern flows across the inside.

Patterns: each colour gets its own field pattern and border motif, from rug motifs (diamonds,
squares, lattices, rosettes, crosses, stars, fans; zigzags, dots, chevrons, bars), coloured from its
own hue's other tones in the palette. A field is a function of (min, max) of the distances from the
block's centre, so it has the square's full symmetry: it looks the same in every turned quarter and
meets seamlessly across quarters and blocks. Border motifs run along the edge and are mirrored
about the middle of the block, for the same reason.

Writes per rug: 5 quarter models + 5 textures (32x32, the quarter at 4 px per pixel), a multipart
blockstate, an item model (the four outer corners), loot, recipes (2 hemp -> 3; a terpene in the
middle of 8 rugs recolours them), names and the fourtwenty:rugs tag. Run again after edits.
"""
import collections
import hashlib
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'
TERP = json.loads((ROOT / 'tools/terpenes.json').read_text(encoding='utf-8'))
Q = 8                 # a quarter is 8x8 pixels
PX = 4                # texture pixels per model pixel
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
        # Everything but the notch [0, inset]^2 (the corner the missing diagonal neighbour leaves).
        return [box(inset, 0, Q, inset, ('up', 'west'), y0, y1), box(0, inset, Q, Q, ('up', 'north'), y0, y1)]
    # Outer corner: bands down the rounded corner (centred where the full-size corner's is), then the rest.
    c = I + R
    els = []
    span = c - inset
    steps = [inset + span * f for f in (0, 0.1, 0.2, 0.3, 0.5, 0.8, 1.0)]
    for a, b in zip(steps, steps[1:]):
        t = c - (a + b) / 2
        x0 = c - math.sqrt(max(0.0, radius * radius - t * t))
        els.append(box(x0, a, Q, b, ('up', 'north', 'west'), y0, y1))
    els.append(box(inset, c, Q, Q, ('up', 'west'), y0, y1))
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
    if code == 'natural':
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


class Pattern:
    """A field pattern g(a, b) (a = min, b = max of the distances from the block centre, 0..8) and a
    border motif, chosen per colour from rug motifs; returns 0 field, 1 accent, 2 deep, 3 light."""

    FIELDS = ['diamonds', 'squares', 'lattice', 'rosette', 'cross', 'star', 'fans', 'medallion', 'checker', 'grid']
    BORDERS = ['zigzag', 'dots', 'chevron', 'bars', 'waves', 'plain']

    def __init__(self, index):
        rng = random.Random(4200 + index * 7919)
        self.field = self.FIELDS[index % len(self.FIELDS)]
        self.second = self.FIELDS[(index * 3 + 1 + index // len(self.FIELDS)) % len(self.FIELDS)]
        self.border = self.BORDERS[(index // 2 + index) % len(self.BORDERS)]
        self.p = [rng.choice((2.0, 2.5, 3.0, 4.0)), rng.choice((1.0, 1.5, 2.0)), rng.uniform(0.6, 1.1), rng.randint(0, 3)]

    def motif(self, kind, a, b, p):
        d = a + b                                           # diamond distance from the centre
        r = math.hypot(a, b)
        period, width, thin, k = p
        if kind == 'diamonds':
            return 1 if (d % (period * 2)) < width else 0
        if kind == 'squares':
            return 1 if (b % period) < width * 0.6 else 0
        if kind == 'lattice':                               # a trellis meeting at the block corners
            return 1 if min((d % 8), 8 - (d % 8)) < thin or abs(a - b) < thin * 0.8 else 0
        if kind == 'rosette':
            return 1 if (r % period) < width * 0.7 else (2 if r < 1.2 else 0)
        if kind == 'cross':
            return 1 if a < width * 0.6 and b < 6.5 else 0
        if kind == 'star':
            return 1 if (b - a) < thin * 1.5 and d < 7 else (2 if d < 2 else 0)
        if kind == 'fans':                                  # quarter rosettes at the block corners
            cr = math.hypot(8 - a, 8 - b)
            return 1 if cr < 5 and (cr % 1.6) < 0.7 else 0
        if kind == 'medallion':
            if d < 3:
                return 2 if d < 1.4 else 3
            return 1 if abs(d - 5) < thin * 0.7 or abs(b - 6) < thin * 0.5 else 0
        if kind == 'checker':
            return 1 if (int(a // 2) + int(b // 2)) % 2 == 0 and b < 7.5 else 0
        if kind == 'grid':
            return 1 if (a % 4) < thin * 0.7 or (b % 4) < thin * 0.7 else 0
        return 0

    def field_value(self, a, b):
        v = self.motif(self.field, a, b, self.p)
        if v:
            return v
        w = self.motif(self.second, a, b, [self.p[0] * 1.5, self.p[1] * 0.6, self.p[2] * 0.6, self.p[3]])
        return 3 if w == 1 else w

    def border_value(self, d, s):
        """d: 0..BW in from the rug's edge; s: 0..8 along it (mirrored about the block middle)."""
        if d < 0.75:
            return 4                                        # the rim (and the bevel): a soft mid tone
        if abs(d - (BW - 0.35)) < 0.3:
            return 3                                        # the pinstripe on the inside
        t = d - 0.5                                         # 0..~2.2 across the band
        if self.border == 'zigzag':
            tri = abs(((s / 2) % 2) - 1)
            return 3 if abs(t - (0.3 + 1.4 * tri)) < 0.35 else 1
        if self.border == 'dots':
            return 3 if math.hypot((s % 2) - 1, t - 1.0) < 0.55 else 1
        if self.border == 'chevron':
            return 3 if abs(t - abs((s % 4) - 2) * 0.8) < 0.3 else 1
        if self.border == 'bars':
            return 3 if (s % 2) < 0.5 and 0.3 < t < 1.8 else 1
        if self.border == 'waves':
            return 3 if abs(t - (1.0 + 0.6 * math.sin(s * math.pi / 2))) < 0.3 else 1
        return 1


def texture(case, pattern, colors, rng):
    base, accent, deep, light = colors
    rim = mix(mix(deep, accent, 0.5), base, 0.25)
    roles = {0: base, 1: accent, 2: deep, 3: light, 4: rim}
    img = Image.new('RGB', (Q * PX, Q * PX))
    px = img.load()
    for py in range(Q * PX):
        for pxx in range(Q * PX):
            x, z = (pxx + 0.5) / PX, (py + 0.5) / PX
            d, along = depth(case, x, z)
            if d < 0:
                col = rim                                   # outside the rug: only its sides sample this
            elif d < BW:
                col = roles[pattern.border_value(d, along)]
            else:
                a, b = sorted((abs(x - 8), abs(z - 8)))
                col = roles[pattern.field_value(a, b)]
            calm = 0.45 if d < 1.0 else 1.0                 # a quieter weave near the edge (less glint)
            weave = calm * ((0.025 if (pxx + py) % 2 == 0 else -0.025) + rng.uniform(-0.012, 0.012))
            px[pxx, py] = tuple(clamp(v * (1 + weave) + 255 * weave * (0.3 - 0.25 * lum(col))) for v in col)
    return img


# ---- everything ------------------------------------------------------------------------------

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
    rugs = [(c['code'], c['name'], c['hex']) for c in TERP['colors']] + [('natural', TERP['natural']['name'], TERP['natural']['hex'])]
    names, seen = collections.OrderedDict(), {}
    for i, (code, name, hexc) in enumerate(rugs):
        pattern = Pattern(i)
        colors = palette_for(code, hexc)
        rng = random.Random(9000 + i)
        digest = hashlib.sha1()
        for case in CASES:
            img = texture(case, pattern, colors, rng)
            img.save(tex / f'{code}_{case}.png')
            digest.update(img.tobytes())
            write(ASSETS / f'models/block/rug_{code}_{case}.json', {'parent': f'fourtwenty:block/rug_{case}',
                                                                    'textures': {'rug': f'fourtwenty:block/rug/{code}_{case}'}})
        seen.setdefault((pattern.field, pattern.second, pattern.border, tuple(pattern.p)), []).append(code)
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
    generate()
