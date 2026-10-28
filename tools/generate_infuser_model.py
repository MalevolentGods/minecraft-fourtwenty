#!/usr/bin/env python3
"""The Inspiration Infuser block, in two styles (Steel and Lab): models, textures, blockstates, item models.

    python3 tools/generate_infuser_model.py

Same geometry as the approved concept renders: a plinth with a lime light line, an octagonal basin (eight bars, every
other one turned 45 degrees) holding a round glowing pool, a polished lip and a low glass rim, a rail of resin jars
along the back and a dispense arm over the pool. A NeoForge composite: the machine as mipmapped cutout, the glass and
jars translucent (one translucent model mis-sorts its faces). Light strips and the pool are fullbright. The front
faces north; the blockstate turns it to face the player.
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
S = 256
RNG = np.random.default_rng(420)

STYLES = {
    'steel': dict(body='#555B63', dark='#17191D', trim=('#CDD2D8', '#4C525A'), glass=('#22302A', 0.38), brushed=True),
    'lab': dict(body='#F4F3EF', dark='#D3D7DA', trim=('#FFFFFF', '#A9B0B8'), glass=('#EEF6F6', 0.26), brushed=False),
}
RESINS = ['#F7F7F2', '#141414', '#FCD200', '#FF7A00', '#B5121B', '#C8209E']
LIME = '#9BD42A'


def rgb(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], float)


def save(img, name):
    p = ASSETS / f'textures/block/infuser/{name}.png'
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p, optimize=True)


def tile_noise(shape, sigma, seed):
    """Seamless noise: blurred with wrap-around so the texture tiles across faces."""
    from scipy.ndimage import gaussian_filter
    r = np.random.default_rng(seed).normal(0, 1, shape)
    n = gaussian_filter(r, sigma, mode='wrap')
    return (n - n.mean()) / (n.std() + 1e-9)


def textures(style, st):
    # Seamless materials: the model is mapped by position, so a face shows whatever part of the block it sits in,
    # and anything baked into a texture's border would land in random places. Edges come from the geometry.
    base = rgb(st['body'])
    # light from above: side faces map the block's height down the texture, so they're brighter at the top (a soft,
    # wrapping curve, so the top and bottom rows still meet)
    v = np.arange(S)[:, None, None] / S
    lift = 1 + (0.06 if st['brushed'] else 0.025) * np.cos(np.pi * v)
    if st['brushed']:
        fine = tile_noise((S, S), (0.5, 40), 1)            # long, fine horizontal brushing
        broad = tile_noise((S, S), (60, 60), 2)
        body = base * lift * (1 + fine[..., None] * 0.022 + broad[..., None] * 0.018)
    else:
        broad = tile_noise((S, S), (60, 60), 3)
        body = base * lift * (1 + broad[..., None] * 0.008)
    save(Image.fromarray(np.clip(body, 0, 255).astype(np.uint8)), f'{style}_body')
    grain = tile_noise((S, S), 0.9, 5)
    dark = rgb(st['dark']) * (1 + grain[..., None] * (0.025 if st['brushed'] else 0.006) + tile_noise((S, S), 40, 6)[..., None] * 0.02)
    save(Image.fromarray(np.clip(dark, 0, 255).astype(np.uint8)), f'{style}_dark')
    a, b = rgb(st['trim'][0]), rgb(st['trim'][1])
    y = np.arange(S)[:, None]
    band = 0.5 + 0.5 * np.cos(2 * np.pi * y / S)            # one soft reflection band per block, seamless
    trim = b + (a - b) * band[..., None] ** 1.6
    trim = trim + 26 * np.exp(-((((y - S * 0.22) % S) - S / 2 + S / 2) / (S * 0.03)) ** 2)[..., None] * 0
    trim = trim * (1 + tile_noise((S, S), (0.5, 40), 7)[..., None] * 0.012)
    save(Image.fromarray(np.clip(trim * np.ones((S, S, 1)), 0, 255).astype(np.uint8)), f'{style}_trim')
    g, ga = rgb(st['glass'][0]), st['glass'][1]
    img = np.zeros((S, S, 4))
    img[..., :3] = g
    yy, xx = np.mgrid[0:S, 0:S]
    glint = (np.exp(-((((xx + yy * 0.6) % S) - S * 0.5) / (S * 0.04)) ** 2) * 0.14)   # a diagonal glint, wrapping
    img[..., :3] = img[..., :3] * (1 - glint[..., None]) + 255 * glint[..., None]
    img[..., 3] = np.clip(ga * 255 + glint * 255 * 0.4, 0, 255)
    save(Image.fromarray(img.astype(np.uint8), 'RGBA'), f'{style}_glass')


def shared_textures():
    y, x = np.mgrid[0:S, 0:S]
    c = (S - 1) / 2
    lime, pale = rgb(LIME), rgb('#D8FF8A')
    # light strip: bright core
    d = np.abs(y - c) / c
    strip = pale * (1 - d[..., None]) + lime * d[..., None]
    save(Image.fromarray(np.clip(strip, 0, 255).astype(np.uint8)), 'light')
    # the pool: a round glowing liquid with slow swirls; transparent outside the circle
    r = np.hypot(x - c, y - c) / c
    ang = np.arctan2(y - c, x - c)
    # smooth glowing liquid: a soft bright centre, two faint ripples, a bright meniscus at the edge
    caustic = tile_noise((S, S), 18, 8)
    glow = np.clip(1 - r, 0, 1) ** 1.5 * 0.5 + np.exp(-((r - 0.38) / 0.03) ** 2) * 0.1 + np.exp(-((r - 0.66) / 0.03) ** 2) * 0.08         + np.exp(-((r - 0.95) / 0.035) ** 2) * 0.55
    col = lime * (0.9 + 0.03 * caustic[..., None]) + (pale - lime) * glow[..., None]
    img = np.zeros((S, S, 4))
    img[..., :3] = np.clip(col, 0, 255)
    img[..., 3] = np.where(r <= 1, 255, 0)
    save(Image.fromarray(img.astype(np.uint8), 'RGBA'), 'pool')
    for i, h in enumerate(RESINS):
        base = rgb(h)
        t = np.linspace(0, 1, S)[:, None, None]
        save(Image.fromarray(np.clip(base * (1.12 - 0.3 * t) * np.ones((S, S, 1)), 0, 255).astype(np.uint8)), f'resin_{i}')


# ------------------------------------------------------------------ geometry (pixels, Minecraft axes: x east, y up, z south)

def el(x0, y0, z0, x1, y1, z1, tex, glow=False, rot=None, faces=('north', 'south', 'east', 'west', 'up', 'down'), uv=None):
    e = {'from': [round(x0, 4), round(y0, 4), round(z0, 4)], 'to': [round(x1, 4), round(y1, 4), round(z1, 4)],
         'faces': {f: ({'texture': '#' + tex, 'uv': uv} if uv else {'texture': '#' + tex}) for f in faces}}
    if rot: e['rotation'] = rot
    if glow: e['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    return e


def octo(cx, cz, r, y0, y1, thick, tex, step=0.0, faces=('north', 'south', 'east', 'west', 'up')):
    """Eight bars round an octagon (apothem r to the bars' centre line); odd ones turned 45 degrees about y. Bars
    cross at the corners, so where their tops show, odd ones stand {@code step} higher (coplanar faces flicker)."""
    side = 2 * (r + thick / 2) * math.tan(math.radians(22.5))
    out = []
    for k in range(8):
        th = math.radians(45 * k)
        mx, mz = cx + math.cos(th) * r, cz + math.sin(th) * r
        # a bar whose long side runs along x, centred on (mx, mz); turned so it's tangent to the octagon
        top = y1 + (step if k % 2 else 0)
        e = el(mx - side / 2, y0, mz - thick / 2, mx + side / 2, top, mz + thick / 2, tex, faces=faces)
        a = (45 * k + 90) % 180              # tangent direction in degrees (0 = along x)
        if a == 90:
            e['from'][0], e['to'][0] = round(mx - thick / 2, 4), round(mx + thick / 2, 4)
            e['from'][2], e['to'][2] = round(mz - side / 2, 4), round(mz + side / 2, 4)
        elif a in (45, 135):
            e['rotation'] = {'origin': [round(mx, 4), round((y0 + top) / 2, 4), round(mz, 4)], 'axis': 'y', 'angle': -45 if a == 45 else 45}
        out.append(e)
    return out


def ring(cx, cz, r, y0, y1, tex):
    w = r * 0.83
    return [el(cx - r, y0, cz - w * 0.62, cx + r, y1, cz + w * 0.62, tex),
            el(cx - w * 0.62, y0, cz - r, cx + w * 0.62, y1, cz + r, tex),
            el(cx - w, y0, cz - w, cx + w, y1, cz + w, tex)]


SIDES = ('north', 'south', 'east', 'west')


def geometry():
    machine, glass = [], []
    # plinth with the lime light line (faces that touch another part are left out: they'd flicker)
    machine += [el(0, 0, 0, 16, 1.2, 16, 'dark', faces=SIDES + ('down',)),
                el(0, 1.2, 0, 16, 1.55, 16, 'light', glow=True, faces=SIDES),
                el(0, 1.55, 0, 16, 2.5, 16, 'body', faces=SIDES + ('up',)),
                el(0.5, 2.5, 0.5, 15.5, 3, 15.5, 'dark', faces=SIDES + ('up',))]
    # the basin: octagonal walls, a dark floor under the round pool, a polished lip, a low glass rim
    cx, cz = 8, 7
    machine += octo(cx, cz, 5.6, 3, 7.4, 1.4, 'body', faces=SIDES)          # walls: the lip covers their tops
    # floor under the pool: a square and the same square turned 45 degrees (an eight-point star that stays inside the walls)
    machine.append(el(cx - 4.45, 6.75, cz - 4.45, cx + 4.45, 6.8, cz + 4.45, 'dark', faces=('up',)))
    machine.append(el(cx - 4.45, 6.55, cz - 4.45, cx + 4.45, 6.6, cz + 4.45, 'dark', faces=('up',),
                      rot={'origin': [cx, 6.575, cz], 'axis': 'y', 'angle': 45}))
    # the pool: one face with the whole round texture on it (its square corners are clear and sit inside the walls)
    machine.append(el(cx - 5.2, 6.9, cz - 5.2, cx + 5.2, 7.15, cz + 5.2, 'pool', glow=True, faces=('up',), uv=[0, 0, 16, 16]))
    machine += octo(cx, cz, 5.6, 7.4, 7.8, 1.6, 'trim', step=0.12)
    glass += octo(cx, cz, 6.0, 7.8, 9.7, 0.3, 'glass', faces=SIDES)
    # a thin polished frame on the glass: a cap along the top and a slim post at every corner
    machine += octo(cx, cz, 6.0, 9.7, 9.95, 0.5, 'trim', step=0.12)
    vr = 6.0 / math.cos(math.radians(22.5))
    for k in range(8):
        th = math.radians(45 * k + 22.5)
        px, pz = cx + math.cos(th) * vr, cz + math.sin(th) * vr
        machine.append(el(px - 0.32, 7.8, pz - 0.32, px + 0.32, 9.7, pz + 0.32, 'trim', faces=SIDES))
    # resin jars on a rail along the back
    machine += [el(1, 3, 13.4, 15, 4, 15.4, 'dark', faces=SIDES + ('up',)),
                el(1, 9.2, 15, 15, 9.7, 15.6, 'trim', faces=SIDES + ('up',))]
    for i in range(6):
        x = 1.6 + i * 2.25
        glass.append(el(x, 4, 13.6, x + 1.6, 8.8, 15.2, 'glass', faces=SIDES + ('up',)))
        machine.append(el(x + 0.25, 4.1, 13.85, x + 1.35, 7.2 - (i % 3) * 0.6, 14.95, f'resin_{i}', faces=SIDES + ('up',)))
        machine.append(el(x + 0.1, 8.8, 13.7, x + 1.5, 9.15, 15.1, 'trim', faces=SIDES + ('up',)))
    # the dispense head: a post at the back, an arm over the pool, a nozzle with a lime tip, a thin light line on the arm
    machine += [el(7, 3, 15.4, 9, 14.5, 16, 'dark'),
                el(7.1, 13.2, 6.2, 8.9, 14.4, 15.4, 'body', faces=('north', 'east', 'west', 'up', 'down')),
                el(7.4, 11.4, 6.4, 8.6, 13.2, 7.6, 'trim', faces=SIDES),
                el(7.65, 10.9, 6.65, 8.35, 11.4, 7.35, 'light', glow=True, faces=SIDES + ('down',)),
                el(7.75, 14.4, 6.3, 8.25, 14.55, 15.3, 'light', glow=True, faces=SIDES + ('up',))]
    return machine, glass


def write_models():
    machine, glass = geometry()
    for style in STYLES:
        name = 'inspiration_infuser' if style == 'steel' else 'inspiration_infuser_lab'
        tex = {'particle': f'fourtwenty:block/infuser/{style}_body', 'body': f'fourtwenty:block/infuser/{style}_body',
               'dark': f'fourtwenty:block/infuser/{style}_dark', 'trim': f'fourtwenty:block/infuser/{style}_trim',
               'glass': f'fourtwenty:block/infuser/{style}_glass', 'light': 'fourtwenty:block/infuser/light',
               'pool': 'fourtwenty:block/infuser/pool'}
        tex.update({f'resin_{i}': f'fourtwenty:block/infuser/resin_{i}' for i in range(6)})
        model = {'parent': 'minecraft:block/block', 'loader': 'neoforge:composite', 'ambientocclusion': False, 'textures': tex,
                 'children': {
                     'machine': {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout_mipped', 'ambientocclusion': False,
                                 'textures': tex, 'elements': machine},
                     'glass': {'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent', 'ambientocclusion': False,
                               'textures': tex, 'elements': glass}},
                 'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
                             'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
                             'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
                             'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
                             'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}}
        (ASSETS / f'models/block/{name}.json').write_text(json.dumps(model, indent=1) + '\n')
        (ASSETS / f'models/item/{name}.json').write_text(json.dumps({'parent': f'fourtwenty:block/{name}'}, indent=2) + '\n')
        variants = {f'facing={f}': {'model': f'fourtwenty:block/{name}', **({'y': y} if y else {})}
                    for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
        (ASSETS / f'blockstates/{name}.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    print('models', len(machine), 'machine elements,', len(glass), 'glass elements')


if __name__ == '__main__':
    for style, st in STYLES.items():
        textures(style, st)
    shared_textures()
    write_models()
