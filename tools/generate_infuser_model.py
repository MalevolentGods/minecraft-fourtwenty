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
    'steel': dict(body='#7A8088', dark='#1C1F23', trim=('#C9CED5', '#5F656D'), glass=('#2A3236', 0.42), brushed=True),
    'lab': dict(body='#F1F0EC', dark='#D2D6D9', trim=('#FFFFFF', '#AEB4BB'), glass=('#EEF6F6', 0.24), brushed=False),
}
RESINS = ['#F7F7F2', '#141414', '#FCD200', '#FF7A00', '#B5121B', '#C8209E']
LIME = '#9BD42A'


def rgb(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], float)


def save(img, name):
    p = ASSETS / f'textures/block/infuser/{name}.png'
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p, optimize=True)


def edge(a, width=10, k=0.82):
    """A soft darkened border, so every face reads as a crisp machined edge."""
    y, x = np.mgrid[0:S, 0:S]
    d = np.minimum(np.minimum(x, S - 1 - x), np.minimum(y, S - 1 - y))
    f = np.clip(d / width, 0, 1)
    return a * (k + (1 - k) * f[..., None])


def textures(style, st):
    base = rgb(st['body'])
    if st['brushed']:
        streaks = RNG.normal(0, 1, (S, 1)) * 0.6 + RNG.normal(0, 1, (S, S)) * 0.25
        streaks = np.array(Image.fromarray(((streaks - streaks.min()) / np.ptp(streaks) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur((6, 0)))) / 255 - 0.5
        body = base * (1 + streaks[..., None] * 0.18)
    else:
        y = np.linspace(0, 1, S)[:, None, None]
        body = base * (1.02 - 0.05 * y) * np.ones((S, S, 1))
    save(Image.fromarray(np.clip(edge(body), 0, 255).astype(np.uint8)), f'{style}_body')
    dark = rgb(st['dark']) * (1 + RNG.normal(0, 0.03, (S, S, 1)))
    save(Image.fromarray(np.clip(edge(dark, 8, 0.75), 0, 255).astype(np.uint8)), f'{style}_dark')
    a, b = rgb(st['trim'][0]), rgb(st['trim'][1])
    t = np.linspace(0, 1, S)[:, None, None]
    trim = a * (1 - t) + b * t
    trim = trim + 40 * np.exp(-((np.arange(S)[:, None, None] - S * 0.3) / (S * 0.05)) ** 2)   # a bright polished band
    save(Image.fromarray(np.clip(trim * np.ones((S, S, 1)), 0, 255).astype(np.uint8)), f'{style}_trim')
    g, ga = rgb(st['glass'][0]), st['glass'][1]
    img = np.zeros((S, S, 4))
    img[..., :3] = g
    img[..., 3] = ga * 255
    y, x = np.mgrid[0:S, 0:S]
    streak = np.exp(-(((x - y * 0.55) - S * 0.35) / (S * 0.06)) ** 2) * 0.35   # a diagonal glint
    img[..., :3] = img[..., :3] * (1 - streak[..., None]) + 255 * streak[..., None]
    img[..., 3] = np.clip(img[..., 3] + streak * 255 * 0.6, 0, 255)
    rim = np.minimum(np.minimum(x, S - 1 - x), np.minimum(y, S - 1 - y)) < 6
    img[rim, 3] = np.maximum(img[rim, 3], 150)
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
    swirl = 0.5 + 0.5 * np.sin(ang * 3 + r * 9)
    col = lime * (0.78 + 0.18 * swirl[..., None]) + (pale - lime) * np.clip(1 - r * 1.3, 0, 1)[..., None] * 0.5
    img = np.zeros((S, S, 4))
    img[..., :3] = np.clip(col, 0, 255)
    img[..., 3] = np.where(r <= 1, 255, 0)
    save(Image.fromarray(img.astype(np.uint8), 'RGBA'), 'pool')
    for i, h in enumerate(RESINS):
        base = rgb(h)
        t = np.linspace(0, 1, S)[:, None, None]
        save(Image.fromarray(np.clip(base * (1.12 - 0.3 * t) * np.ones((S, S, 1)), 0, 255).astype(np.uint8)), f'resin_{i}')


# ------------------------------------------------------------------ geometry (pixels, Minecraft axes: x east, y up, z south)

def el(x0, y0, z0, x1, y1, z1, tex, glow=False, rot=None, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    e = {'from': [round(x0, 4), round(y0, 4), round(z0, 4)], 'to': [round(x1, 4), round(y1, 4), round(z1, 4)],
         'faces': {f: {'texture': '#' + tex} for f in faces}}
    if rot: e['rotation'] = rot
    if glow: e['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    return e


def octo(cx, cz, r, y0, y1, thick, tex):
    """Eight bars round an octagon (apothem r to the bars' centre line); odd ones turned 45 degrees about y."""
    side = 2 * (r + thick / 2) * math.tan(math.radians(22.5))
    out = []
    for k in range(8):
        th = math.radians(45 * k)
        mx, mz = cx + math.cos(th) * r, cz + math.sin(th) * r
        # a bar whose long side runs along x, centred on (mx, mz); turned so it's tangent to the octagon
        top = y1 + (0.2 if k % 2 else 0)
        e = el(mx - side / 2, y0, mz - thick / 2, mx + side / 2, top, mz + thick / 2, tex,
               faces=('north', 'south', 'east', 'west', 'up'))
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
    machine += octo(cx, cz, 5.6, 3, 7.4, 1.4, 'body')
    machine.append(el(cx - 5.3, 6.75, cz - 5.3, cx + 5.3, 6.8, cz + 5.3, 'dark', faces=('up',)))
    machine.append(el(cx - 5.2, 6.9, cz - 5.2, cx + 5.2, 7.15, cz + 5.2, 'pool', glow=True, faces=('up',)))
    machine += octo(cx, cz, 5.75, 7.6, 8.0, 1.25, 'trim')
    glass += octo(cx, cz, 6.2, 8.2, 9.5, 0.3, 'glass')
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
