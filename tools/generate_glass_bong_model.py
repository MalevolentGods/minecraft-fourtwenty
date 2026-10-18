#!/usr/bin/env python3
"""Glass bong block models (every variety) and their frosted glass textures.

Each model is a composite (translucent glass + solid bud) built from small boxes like the
Dispensary hut block. All varieties share the beaker, water, tube, mouthpiece and the open frosted
bowl packed with bud; they differ in proportions, glass tint, accent colour and one signature
detail. The bowl faces north (the blockstate turns it toward whoever placed it).

Writes src/main/resources/assets/fourtwenty/models/block/<variety>.json and
textures/block/bong_*.png. Run again after edits.
"""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / 'src/main/resources/assets/fourtwenty/models/block'
TEXTURES = ROOT / 'src/main/resources/assets/fourtwenty/textures/block'

# Frosted glass tints: (r, g, b, fill alpha, edge alpha). Raise the alphas for less see-through glass.
GLASS = {
    'bong_glass': (226, 234, 238, 150, 215),
    'bong_water': (64, 140, 210, 175, 200),
    'bong_lime': (128, 204, 52, 170, 220),
    'bong_smoke': (120, 124, 128, 165, 220),
    'bong_phantom_glass': (196, 232, 236, 150, 215),
    'bong_deep_water': (30, 96, 168, 185, 210),
    'bong_tan': (196, 156, 104, 185, 225),
    'bong_aqua': (72, 196, 180, 175, 220),
    'bong_ember': (236, 110, 32, 190, 230),
    'bong_gold': (240, 196, 48, 190, 230),
    'bong_amethyst': (160, 104, 220, 180, 225),
    'bong_teal': (64, 168, 172, 175, 220),
}


def png(path, pixels):
    """Write an RGBA PNG (rows of (r, g, b, a) tuples) with no dependencies."""
    height, width = len(pixels), len(pixels[0])

    def chunk(name, data):
        return struct.pack('>I', len(data)) + name + data + struct.pack('>I', zlib.crc32(name + data))
    raw = b''.join(b'\0' + bytes(c for p in row for c in p) for row in pixels)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def frosted(r, g, b, fill, edge):
    """A 16x16 frosted glass tile: a brighter, more opaque rim and a soft diagonal highlight."""
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                row.append((min(255, r + 20), min(255, g + 20), min(255, b + 20), edge))
            elif x - y in (3, 4) or x - y in (-6,):
                row.append((min(255, r + 28), min(255, g + 28), min(255, b + 28), min(255, fill + 30)))
            else:
                shade = ((x * 3 + y * 5) % 4) * 3
                row.append((max(0, r - shade), max(0, g - shade), max(0, b - shade), fill))
        pixels.append(row)
    return pixels


def box(name, frm, to, tex, rotation=None, skip=()):
    """An axis-aligned box with UVs taken from its own extents (all within one block, so 0..16)."""
    (x1, y1, z1), (x2, y2, z2) = frm, to
    uv = {
        'north': [16 - x2, 16 - y2, 16 - x1, 16 - y1],
        'south': [x1, 16 - y2, x2, 16 - y1],
        'east': [16 - z2, 16 - y2, 16 - z1, 16 - y1],
        'west': [z1, 16 - y2, z2, 16 - y1],
        'up': [x1, z1, x2, z2],
        'down': [x1, 16 - z2, x2, 16 - z1],
    }
    element = {'name': name, 'from': [round(v, 3) for v in frm], 'to': [round(v, 3) for v in to],
               'faces': {face: {'uv': [round(min(16, max(0, c)), 3) for c in coords], 'texture': '#' + tex}
                         for face, coords in uv.items() if face not in skip}}
    if rotation:
        element['rotation'] = rotation
    return element


# Per-variety design: base half-width and height, tube half-width and top, ring heights, textures.
DESIGNS = {
    'glass_bong': dict(base=3, base_h=4, tube=1.5, top=14.5, rings=(9,), glass='bong_glass', water='bong_water', accent='bong_lime'),
    'jackrabbit_bong': dict(base=2.5, base_h=3.5, tube=1.25, top=14.6, rings=(7, 11), glass='bong_glass', water='bong_water', accent='bong_tan'),
    'deep_sea_bong': dict(base=4, base_h=4.5, tube=1.5, top=13.5, rings=(10,), glass='bong_glass', water='bong_deep_water', accent='bong_aqua',
                          water_level=0.85),
    'forge_bong': dict(base=3.5, base_h=4, tube=2, top=13.5, rings=(7, 10.5), glass='bong_smoke', water='bong_water', accent='bong_ember'),
    'owl_bong': dict(base=3, base_h=4, tube=1.5, top=14.5, rings=(8,), glass='bong_glass', water='bong_water', accent='bong_gold'),
    'sage_bong': dict(base=3, base_h=4, tube=1.5, top=14.5, rings=(9,), glass='bong_glass', water='bong_water', accent='bong_amethyst'),
    'phantom_bong': dict(base=3, base_h=4, tube=1.5, top=14.5, rings=(9,), glass='bong_phantom_glass', water='bong_water', accent='bong_teal'),
}


def signature(path, d, glass):
    """Each specialty's distinguishing detail."""
    t, top = d['tube'], d['top']
    if path == 'jackrabbit_bong':      # two "ears" rising from the mouthpiece
        glass.append(box('left ear', (8 - t - 0.6, top + 1.2, 7.6), (8 - t + 0.2, min(16, top + 1.2 + 1.3), 8.4), 'accent'))
        glass.append(box('right ear', (8 + t - 0.2, top + 1.2, 7.6), (8 + t + 0.6, min(16, top + 1.2 + 1.3), 8.4), 'accent'))
    elif path == 'deep_sea_bong':      # a percolator inside the tube
        glass.append(box('percolator', (8 - t + 0.3, 6.5, 8 - t + 0.3), (8 + t - 0.3, 8.5, 8 + t - 0.3), 'accent'))
    elif path == 'owl_bong':           # two gold "eyes" on the front near the top
        for i, x in enumerate((8 - 0.9, 8 + 0.15)):
            glass.append(box(f'eye {i}', (x, 12, 8 - t - 0.3), (x + 0.75, 12.75, 8 - t), 'accent'))
    elif path == 'sage_bong':          # crystal shards on the tube
        for i, (x, y, z, angle) in enumerate(((8 + t, 7, 7.6, 22.5), (8 - t - 1, 10, 7.7, -22.5), (8 + t, 12, 8.1, -22.5))):
            glass.append(box(f'crystal {i}', (x, y, z), (x + 1, y + 1.6, z + 0.7), 'accent',
                             rotation={'angle': angle, 'axis': 'z', 'origin': [x + 0.5, y, z + 0.35]}))
    elif path == 'phantom_bong':       # wings on either side of the tube
        glass.append(box('left wing', (8 - t - 3, 9.5, 7.85), (8 - t, 12, 8.15), 'accent',
                         rotation={'angle': -22.5, 'axis': 'z', 'origin': [8 - t, 10.5, 8]}))
        glass.append(box('right wing', (8 + t, 9.5, 7.85), (8 + t + 3, 12, 8.15), 'accent',
                         rotation={'angle': 22.5, 'axis': 'z', 'origin': [8 + t, 10.5, 8]}))


def model(path, d):
    glass, solid = [], []
    b, bh, t, top = d['base'], d['base_h'], d['tube'], d['top']
    lo, hi = 8 - b, 8 + b
    # Beaker base with water.
    glass.append(box('base', (lo, 0, lo), (hi, bh, hi), 'glass'))
    glass.append(box('water', (lo + 0.3, 0.2, lo + 0.3), (hi - 0.3, round(bh * d.get('water_level', 0.75), 3), hi - 0.3), 'water'))
    glass.append(box('neck', (8 - t - 0.5, bh, 8 - t - 0.5), (8 + t + 0.5, bh + 1, 8 + t + 0.5), 'glass'))
    # Tube, rings, mouthpiece.
    glass.append(box('tube', (8 - t, bh + 1, 8 - t), (8 + t, top, 8 + t), 'glass'))
    for i, y in enumerate(d['rings']):
        glass.append(box(f'ring {i}', (8 - t - 0.3, y, 8 - t - 0.3), (8 + t + 0.3, y + 0.6, 8 + t + 0.3), 'accent'))
    glass.append(box('mouthpiece', (8 - t - 0.5, top, 8 - t - 0.5), (8 + t + 0.5, top + 0.9, 8 + t + 0.5), 'glass'))
    glass.append(box('mouthpiece lip', (8 - t - 0.5, top + 0.9, 8 - t - 0.5), (8 + t + 0.5, min(16, top + 1.2), 8 + t + 0.5), 'accent'))
    # Downstem angled up and out of the front, ending in the open frosted bowl. Faces only render
    # from outside, so the bowl is thin walls (a hollow box would look see-through from inside).
    glass.append(box('downstem', (7.5, 2.2, 2.2), (8.5, 3, lo + 0.5), 'glass',
                     rotation={'angle': 22.5, 'axis': 'x', 'origin': [8, 2.6, lo + 0.5]}))
    glass.append(box('bowl base', (6.75, 3.4, 1), (9.25, 3.75, 3.5), 'glass'))
    glass.append(box('bowl front', (6.75, 3.75, 1), (9.25, 5, 1.35), 'glass'))
    glass.append(box('bowl back', (6.75, 3.75, 3.15), (9.25, 5, 3.5), 'glass'))
    glass.append(box('bowl left', (6.75, 3.75, 1.35), (7.1, 5, 3.15), 'glass'))
    glass.append(box('bowl right', (8.9, 3.75, 1.35), (9.25, 5, 3.15), 'glass'))
    solid.append(box('packed bud', (7.1, 3.75, 1.35), (8.9, 5.3, 3.15), 'bud'))   # mounded just above the rim
    signature(path, d, glass)
    return {
        'credit': 'FourTwenty tools/generate_glass_bong_model.py',
        'parent': 'block/block',
        'ambientocclusion': False,
        'loader': 'neoforge:composite',
        'textures': {'particle': f"fourtwenty:block/{d['glass']}"},
        'children': {
            'solid': {'render_type': 'minecraft:solid',
                      'textures': {'bud': 'minecraft:block/moss_block'},
                      'elements': solid},
            'translucent': {'render_type': 'minecraft:translucent',
                            'textures': {'glass': f"fourtwenty:block/{d['glass']}",
                                         'water': f"fourtwenty:block/{d['water']}",
                                         'accent': f"fourtwenty:block/{d['accent']}"},
                            'elements': glass},
        },
    }


def generate():
    for path, design in DESIGNS.items():
        data = model(path, design)
        for child in data['children'].values():
            for element in child['elements']:
                assert all(-16 <= v <= 32 for v in element['from'] + element['to']), (path, element['name'])
                for face in element['faces'].values():
                    assert all(0 <= c <= 16 for c in face['uv']), (path, element['name'], face)
        (MODELS / f'{path}.json').write_text(json.dumps(data, indent=2) + '\n')
        print(f"wrote {path}: {len(data['children']['translucent']['elements'])} glass, {len(data['children']['solid']['elements'])} solid elements")
    for name, tint in GLASS.items():
        png(TEXTURES / f'{name}.png', frosted(*tint))


if __name__ == '__main__':
    generate()
