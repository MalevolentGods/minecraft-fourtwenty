#!/usr/bin/env python3
"""Tie-dye art (specs/018-tie-dye-shack research R1, R3): the 8 patterns as four greyscale slot masks each, and the models that layer
them. Every pixel belongs to exactly one slot; the masks are white-ish fabric with soft shading, tinted in game by the colourway's colour
for that slot (tint index = slot), so 8 patterns cover all 16 colourways. Slot edges are dithered so they read as dye bleeding.

Writes, under src/main/resources:
- assets/fourtwenty/textures/block/tie_dye/<pattern>_<slot>.png (16x16)
- the tie-dyed hemp block: blockstate (pattern 0-7), a model per pattern (4 cutout layers), the item model (overrides on
  fourtwenty:pattern), the loot table (copying the look from the block entity), the name
- a preview sheet of every pattern in every colourway: build/tie_dye_preview.png

Run again after edits.
"""
import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
TEX = ASSETS / 'textures/block/tie_dye'
PATTERNS = ['crumple', 'stripes', 'bullseye', 'spiral', 'sunburst', 'shibori', 'heart', 'mandala']
SLOTS = 4
SIZE = 16


def noise(x, y, seed):
    """Deterministic value noise in [0, 1)."""
    n = (x * 374761393 + y * 668265263 + seed * 982451653) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65536.0


def smooth(x, y, scale, seed):
    """Smooth noise: bilinear value noise on a grid of {scale} pixels."""
    gx, gy = x / scale, y / scale
    x0, y0 = math.floor(gx), math.floor(gy)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = noise(x0, y0, seed), noise(x0 + 1, y0, seed)
    c, d = noise(x0, y0 + 1, seed), noise(x0 + 1, y0 + 1, seed)
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def field(pattern, x, y, size):
    """A cyclic value whose quarter-bands are the slots: what makes each pattern look like itself."""
    c = (size - 1) / 2
    dx, dy = (x - c) / size, (y - c) / size
    r = math.hypot(dx, dy)
    a = math.atan2(dy, dx)
    wobble = smooth(x, y, size / 4, 7) - 0.5
    if pattern == 'crumple':
        # warped blotches: sample smooth noise at a position pushed around by more noise
        wx = x + (smooth(x, y, size / 3, 21) - 0.5) * size * 0.5
        wy = y + (smooth(x, y, size / 3, 22) - 0.5) * size * 0.5
        return smooth(wx, wy, size / 2.6, 11) * 2.0 + smooth(wx, wy, size / 5.5, 3) * 0.5
    if pattern == 'stripes':
        return (x + y) / size * 1.5 + wobble * 0.35
    if pattern == 'bullseye':
        return r * 4.2 + wobble * 0.3
    if pattern == 'spiral':
        return a / (2 * math.pi) * 2 + r * 3.0 + wobble * 0.15
    if pattern == 'sunburst':
        # 8 rays from the middle, alternating, with a core: slot = ray parity + 2 x (outside the core)
        ray = int(((a + math.pi) / (2 * math.pi) * 8 + r * 1.5) % 2)
        return (ray + (2 if r > 0.2 else 0)) / 4 + 0.125
    if pattern == 'shibori':
        # folded diamonds in a 2x2 grid of tiles: rings by diamond distance, offset on alternate tiles
        t = size / 2
        u, v = (x % t - (t - 1) / 2) / t, (y % t - (t - 1) / 2) / t
        alt = (int(x // t) + int(y // t)) % 2
        d = abs(u) + abs(v)
        return (int(d * 4) % 2 + 2 * alt) / 4 + 0.125
    if pattern == 'heart':
        # nested hearts: the smallest heart that holds the pixel sets its ring
        def inside(px, py, s):
            hx, hy = px / s, -py / s + 0.3
            return (hx * hx + hy * hy - 1) ** 3 - hx * hx * hy ** 3 < 0
        s = 0.05
        while s < 3 and not inside(dx * 2.4, dy * 2.4, s):
            s += 0.05
        return s * 1.2
    if pattern == 'mandala':
        return r * 3.5 + math.cos(a * 8) * 0.12 + math.cos(a * 4 + r * 9) * 0.08 + wobble * 0.15
    raise ValueError(pattern)


def slots(pattern, size=SIZE, seed=1):
    """The slot of every pixel, with a little noise at the edges (dye bleeding) and fabric shading."""
    grid, shade = [], []
    for y in range(size):
        row, srow = [], []
        for x in range(size):
            jitter = 0.04 if pattern in ('sunburst', 'shibori') else 0.12
            v = field(pattern, x, y, size) + (noise(x, y, seed + 17) - 0.5) * jitter
            q = v * SLOTS
            slot = int(math.floor(q)) % SLOTS
            edge = abs((q % 1) - 0.5) * 2          # 1 mid-band, 0 at a band edge
            fabric = 0.86 + smooth(x, y, 2, seed + 5) * 0.1 + noise(x, y, seed + 9) * 0.04
            srow.append(min(1.0, fabric * (0.9 + 0.1 * edge)))   # dye pools darker at the edges
            row.append(slot)
        grid.append(row)
        shade.append(srow)
    return grid, shade


def masks(pattern, size=SIZE):
    grid, shade = slots(pattern, size)
    out = []
    for k in range(SLOTS):
        img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        px = img.load()
        for y in range(size):
            for x in range(size):
                if grid[y][x] == k:
                    g = int(255 * shade[y][x])
                    px[x, y] = (g, g, g, 255)
        out.append(img)
    return out


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def cube_model(textures):
    """A full block of four coplanar cutout layers, slot k tinted k. Disjoint masks: no z-fighting under cutout."""
    elements = []
    for k in range(SLOTS):
        faces = {d: {'uv': [0, 0, 16, 16], 'texture': f'#s{k}', 'tintindex': k, 'cullface': d}
                 for d in ('down', 'up', 'north', 'south', 'west', 'east')}
        elements.append({'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces})
    tex = {f's{k}': t for k, t in enumerate(textures)}
    tex['particle'] = textures[0]
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': elements}


def hemp():
    for i, p in enumerate(PATTERNS):
        write_json(ASSETS / f'models/block/tie_dyed_hemp_{p}.json',
                   cube_model([f'fourtwenty:block/tie_dye/{p}_{k}' for k in range(SLOTS)]))
    write_json(ASSETS / 'blockstates/tie_dyed_hemp.json',
               {'variants': {f'pattern={i}': {'model': f'fourtwenty:block/tie_dyed_hemp_{p}'} for i, p in enumerate(PATTERNS)}})
    write_json(ASSETS / 'models/item/tie_dyed_hemp.json', {
        'parent': f'fourtwenty:block/tie_dyed_hemp_{PATTERNS[0]}',
        'overrides': [{'predicate': {'fourtwenty:pattern': i / 8}, 'model': f'fourtwenty:block/tie_dyed_hemp_{p}'}
                      for i, p in enumerate(PATTERNS) if i > 0]})
    write_json(RES / 'data/fourtwenty/loot_table/blocks/tie_dyed_hemp.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'fourtwenty:tie_dyed_hemp', 'functions': [
            {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['fourtwenty:tie_dye']}]}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def lang(entries):
    path = ASSETS / 'lang/en_us.json'
    cur = json.loads(path.read_text(encoding='utf-8'))
    cur.update(entries)
    path.write_text('{\n' + ',\n'.join(f'  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}' for k, v in cur.items()) + '\n}\n',
                    encoding='utf-8')


def preview():
    """Every pattern (rows) in every colourway (columns), at 4x, for looking at."""
    data = json.loads((RES / 'data/fourtwenty/tie_dye/colourways.json').read_text(encoding='utf-8'))['colourways']
    pal = palette()
    scale, gap = 4, 4
    w = len(data) * (SIZE * scale + gap) + gap
    h = len(PATTERNS) * (SIZE * scale + gap) + gap
    sheet = Image.new('RGB', (w, h), (40, 34, 30))
    for row, p in enumerate(PATTERNS):
        ms = masks(p)
        for col, cw in enumerate(data):
            tile = Image.new('RGB', (SIZE, SIZE))
            tp = tile.load()
            for k, m in enumerate(ms):
                rgb = pal.get(cw['colours'][k % len(cw['colours'])], (200, 185, 143))
                mp = m.load()
                for y in range(SIZE):
                    for x in range(SIZE):
                        if mp[x, y][3]:
                            g = mp[x, y][0] / 255
                            tp[x, y] = tuple(int(c * g) for c in rgb)
            sheet.paste(tile.resize((SIZE * scale, SIZE * scale), Image.NEAREST), (gap + col * (SIZE * scale + gap), gap + row * (SIZE * scale + gap)))
    out = ROOT / 'build/tie_dye_preview.png'
    out.parent.mkdir(exist_ok=True)
    sheet.save(out)
    return out


def palette():
    """Colour code -> RGB, from tools/terpenes.json (the hemp colours and the Minecraft colours, as the game's palette)."""
    d = json.loads((ROOT / 'tools/terpenes.json').read_text(encoding='utf-8'))
    out = {}
    for c in d['colors'] + d['minecraft']:
        h = c['hex'].lstrip('#')
        out[c['code']] = tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))
    return out


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    for p in PATTERNS:
        for k, img in enumerate(masks(p)):
            img.save(TEX / f'{p}_{k}.png')
    hemp()
    lang({'block.fourtwenty.tie_dyed_hemp': 'Tie-Dyed Hemp'})
    print(f'wrote {len(PATTERNS) * SLOTS} masks and the tie-dyed hemp block; preview at {preview().relative_to(ROOT)}')


if __name__ == '__main__':
    main()
