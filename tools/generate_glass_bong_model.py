#!/usr/bin/env python3
"""The glass bong block model: a frosted beaker bong with water, a lime-accented tube and a bowl.

A composite model (translucent glass + solid bud), built from small boxes like the Dispensary hut
block. The bowl faces north (the blockstate turns it toward whoever placed it).

Writes src/main/resources/assets/fourtwenty/models/block/glass_bong.json. Run again after edits.
"""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / 'src/main/resources/assets/fourtwenty/models/block/glass_bong.json'
TEXTURES = ROOT / 'src/main/resources/assets/fourtwenty/textures/block'

# Frosted glass tints: (r, g, b, fill alpha, edge alpha). Raise the alphas for less see-through glass.
GLASS = {
    'bong_glass': (226, 234, 238, 150, 215),
    'bong_water': (64, 140, 210, 175, 200),
    'bong_lime': (128, 204, 52, 170, 220),
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
    element = {'name': name, 'from': list(frm), 'to': list(to),
               'faces': {face: {'uv': [round(c, 3) for c in coords], 'texture': '#' + tex}
                         for face, coords in uv.items() if face not in skip}}
    if rotation:
        element['rotation'] = rotation
    return element


def generate():
    glass, solid = [], []
    # Beaker base with water.
    glass.append(box('base', (5, 0, 5), (11, 4, 11), 'glass'))
    glass.append(box('water', (5.3, 0.2, 5.3), (10.7, 3, 10.7), 'water'))
    glass.append(box('neck', (6, 4, 6), (10, 5, 10), 'glass'))
    # Tube, pinch ring, mouthpiece.
    glass.append(box('tube', (6.5, 5, 6.5), (9.5, 14.5, 9.5), 'glass'))
    glass.append(box('pinch ring', (6.2, 9, 6.2), (9.8, 9.6, 9.8), 'accent'))
    glass.append(box('mouthpiece', (6, 14.5, 6), (10, 15.4, 10), 'glass'))
    glass.append(box('mouthpiece lip', (6, 15.4, 6), (10, 15.7, 10), 'accent'))
    # Downstem angled up and out of the front, ending in the bowl.
    glass.append(box('downstem', (7.5, 2.2, 2.2), (8.5, 3, 5.5), 'glass',
                     rotation={'angle': 22.5, 'axis': 'x', 'origin': [8, 2.6, 5.5]}))
    # Open frosted bowl: thin walls (faces only render from outside, so a hollow box would look
    # see-through from inside) around a base; the packed bud fills the inside.
    glass.append(box('bowl base', (6.75, 3.4, 1), (9.25, 3.75, 3.5), 'glass'))
    glass.append(box('bowl front', (6.75, 3.75, 1), (9.25, 5, 1.35), 'glass'))
    glass.append(box('bowl back', (6.75, 3.75, 3.15), (9.25, 5, 3.5), 'glass'))
    glass.append(box('bowl left', (6.75, 3.75, 1.35), (7.1, 5, 3.15), 'glass'))
    glass.append(box('bowl right', (8.9, 3.75, 1.35), (9.25, 5, 3.15), 'glass'))
    solid.append(box('packed bud', (7.1, 3.75, 1.35), (8.9, 5.3, 3.15), 'bud'))   # mounded just above the rim

    model = {
        'credit': 'FourTwenty tools/generate_glass_bong_model.py',
        'parent': 'block/block',
        'ambientocclusion': False,
        'loader': 'neoforge:composite',
        'textures': {'particle': 'fourtwenty:block/bong_glass'},
        'children': {
            'solid': {'render_type': 'minecraft:solid',
                      'textures': {'bud': 'minecraft:block/moss_block'},
                      'elements': solid},
            'translucent': {'render_type': 'minecraft:translucent',
                            'textures': {'glass': 'fourtwenty:block/bong_glass',
                                         'water': 'fourtwenty:block/bong_water',
                                         'accent': 'fourtwenty:block/bong_lime'},
                            'elements': glass},
        },
    }
    for child in model['children'].values():
        for element in child['elements']:
            for face in element['faces'].values():
                assert all(0 <= c <= 16 for c in face['uv']), (element['name'], face)
    MODEL.write_text(json.dumps(model, indent=2) + '\n')
    for name, tint in GLASS.items():
        png(TEXTURES / f'{name}.png', frosted(*tint))
    print(f'wrote {MODEL.relative_to(ROOT)}: {len(glass)} glass, {len(solid)} solid elements')


if __name__ == '__main__':
    generate()
