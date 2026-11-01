#!/usr/bin/env python3
"""The hookah (block id weed_bong): models for all its states, around the hand-painted cabinet art.

    python3 tools/generate_hookah_model.py

The cabinet keeps the original paintings (weed_bong_<state>.png sides, weed_bong_top, weed_bong_bottom), each laid whole
on its face, but stands a little inside the block (9 x 9 x 9, square so nothing stretches) instead of filling it; the whole hookah
fits in the block. On the brass socket in the top
painting stands the hookah proper: a brass collar, a stem, a charcoal tray, a clay bowl with coals (glowing while
lit), and a hose that loops off the stem and hangs down the side to a brass mouthpiece. Smoke puffs off the bowl while
it's lit (WeedBongBlock#animateTick). Writes models/block/weed_bong_<state>.json and textures/block/hookah/*.png.

Double Hose, Big Bertha and the Grand Hookah are this same hookah upgraded, the same cabinet paintings and state set, with the hose repeated round the stem
(2 and 4 of them) and its brass swapped for copper, steel or gold: models/block/double_hose_<state>.json and so on for big_bertha and grand_hookah, and their
blockstates, which follow weed_bong.json.
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

import generate_infuser_model as INF

ASSETS = INF.ASSETS
S = 64
STATES = ['empty', 'water'] + [f'weed_{i}' for i in range(1, 5)] + [f'water_weed_{i}' for i in range(1, 5)] + [f'lit_{i}' for i in range(1, 5)]
SIDES = ('north', 'south', 'east', 'west')


def save(img, name):
    p = ASSETS / f'textures/block/hookah/{name}.png'
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p, optimize=True)


def textures():
    rgb, noise = INF.rgb, INF.tile_noise
    y = np.arange(S)[:, None, None] / S
    # brass, the colour of the painted socket ring: a soft polished band, seamless
    a, b = rgb('#FFE27A'), rgb('#B07C14')
    band = 0.5 + 0.5 * np.cos(2 * np.pi * y)
    save(Image.fromarray(np.clip((b + (a - b) * band ** 1.4) * (1 + noise((S, S), (0.5, 12), 21)[..., None] * 0.03), 0, 255).astype(np.uint8)), 'brass')
    # the stem: darker antique brass
    save(Image.fromarray(np.clip(rgb('#8A6418') * (0.9 + 0.2 * band) * (1 + noise((S, S), (0.5, 10), 22)[..., None] * 0.04), 0, 255).astype(np.uint8)), 'stem')
    # the clay bowl: terracotta with a little grain
    save(Image.fromarray(np.clip(rgb('#B2552E') * (1 + noise((S, S), 1.2, 23)[..., None] * 0.06 + noise((S, S), 8, 24)[..., None] * 0.05), 0, 255).astype(np.uint8)), 'clay')
    # coals: black lumps with ash; lit, glowing orange cracks
    lumps = noise((S, S), 3, 25)
    coal = rgb('#2A2624') * (1 + lumps[..., None] * 0.25) + (np.clip(noise((S, S), 0.8, 26), 1.6, 3) - 1.6)[..., None] * 60
    save(Image.fromarray(np.clip(coal, 0, 255).astype(np.uint8)), 'coals')
    cracks = np.exp(-(np.abs(lumps) / 0.18) ** 2)
    lit = coal * (1 - cracks[..., None]) + (rgb('#FF8A1E') * (0.8 + 0.4 * cracks[..., None])) * cracks[..., None]
    lit = lit + np.clip(lumps, 0, None)[..., None] * rgb('#7A1E08') * 0.5
    save(Image.fromarray(np.clip(lit, 0, 255).astype(np.uint8)), 'coals_lit')
    # the hose: deep teal braided leather
    x = np.arange(S)[None, :, None] / S
    braid = 0.5 + 0.5 * np.sin(2 * np.pi * (x * 8 + y * 8))
    save(Image.fromarray(np.clip(rgb('#1F5E5A') * (0.8 + 0.3 * braid) * (1 + noise((S, S), 0.7, 27)[..., None] * 0.04), 0, 255).astype(np.uint8)), 'hose')


def el(x0, y0, z0, x1, y1, z1, tex, faces=SIDES + ('up', 'down'), glow=False, rot=None):
    """An element with every face's UV pinned to the whole texture: the parts are small, and a face left to auto-UV
    above the block's top would read past its texture into the rest of the atlas."""
    return INF.el(x0, y0, z0, x1, y1, z1, tex, glow=glow, rot=rot, faces=faces, uv=[0, 0, 16, 16])


def hose():
    """A port off the stem, down at 45 degrees past the cabinet's edge, then straight down to the mouthpiece. It's its own piece: the game leaves it off
    any side with a Wick on it (client/PortModel), and it's back when the Wick goes."""
    return [
        el(8.5, 11.6, 7.6, 9.5, 12.4, 8.4, 'brass', faces=('north', 'south', 'up', 'down')),
        el(9.3, 11.6, 7.65, 14.5, 12.4, 8.35, 'hose',
           rot={'origin': [9.5, 12.0, 8], 'axis': 'z', 'angle': -45}),
        el(12.75, 3.0, 7.6, 13.55, 8.75, 8.4, 'hose', faces=SIDES + ('up',)),
        el(12.6, 1.4, 7.45, 13.7, 3.0, 8.55, 'brass'),
    ]


def model(state):
    lit = state.startswith('lit')
    full = [0, 0, 16, 16]
    out = [
        # the cabinet: the paintings whole on each face (square, so nothing stretches), inside the block
        {'from': [3.5, 0, 3.5], 'to': [12.5, 9, 12.5], 'faces': {
            **{f: {'texture': '#side', 'uv': full} for f in SIDES},
            'up': {'texture': '#top', 'uv': full}, 'down': {'texture': '#bottom', 'uv': full, 'cullface': 'down'}}},
        # brass collar on the socket, the stem with a knuckle, the charcoal tray
        el(7.1, 9, 7.1, 8.9, 9.7, 8.9, 'brass', faces=SIDES + ('up',)),
        el(7.5, 9.7, 7.5, 8.5, 13, 8.5, 'stem', faces=SIDES),
        el(7.25, 10.6, 7.25, 8.75, 11.1, 8.75, 'brass', faces=SIDES + ('up', 'down')),
        el(5.6, 13, 5.6, 10.4, 13.3, 10.4, 'brass'),
        # the clay bowl, wider at the top, coals in it (all of it inside the block)
        el(6.9, 13.3, 6.9, 9.1, 14.1, 9.1, 'clay', faces=SIDES),
        el(6.4, 14.1, 6.4, 9.6, 15.5, 9.6, 'clay'),                       # its top is the clay rim round the coals
        el(6.75, 15.5, 6.75, 9.25, 15.7, 9.25, 'coals_lit' if lit else 'coals', glow=lit, faces=SIDES + ('up',)),
    ]
    tex = {'particle': 'fourtwenty:block/weed_bong_top', 'side': f'fourtwenty:block/weed_bong_{state}',
           'top': 'fourtwenty:block/weed_bong_top', 'bottom': 'fourtwenty:block/weed_bong_bottom'}
    tex.update({n: f'fourtwenty:block/hookah/{n}' for n in ('brass', 'stem', 'clay', 'coals', 'coals_lit', 'hose')})
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'render_type': 'minecraft:cutout_mipped',
            'textures': tex, 'elements': out,
            'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, -2.5, 0], 'scale': [0.5, 0.5, 0.5]},
                        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
                        'fixed': {'rotation': [0, 0, 0], 'translation': [0, -2, 0], 'scale': [0.45, 0.45, 0.45]},
                        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 1.5, 0], 'scale': [0.3, 0.3, 0.3]},
                        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, -1, 0], 'scale': [0.32, 0.32, 0.32]}}}


FACE_TURN = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north', 'up': 'up', 'down': 'down'}


def turned(element, quarter):
    """An element turned clockwise (from above) by quarter turns round the block's centre."""
    e = json.loads(json.dumps(element))
    for _ in range(quarter):
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        e['from'], e['to'] = [16 - z1, y0, x0], [16 - z0, y1, x1]
        e['faces'] = {FACE_TURN[f]: v for f, v in e['faces'].items()}
        r = e.get('rotation')
        if r:
            ox, oy, oz = r['origin']
            r['origin'] = [16 - oz, oy, ox]
            # turning the scene sends the x axis to z and the z axis to minus x
            r['axis'], r['angle'] = ('x', -r['angle']) if r['axis'] == 'z' else ('z', r['angle'])
    return e


def upgraded(state, metal):
    m = model(state)
    m['textures']['brass'] = f'fourtwenty:block/hookah/{metal}'
    return m


def hose_part(metal, quarter):
    m = model('empty')
    tex = {k: v for k, v in m['textures'].items() if k in ('brass', 'hose')}
    tex['brass'] = f'fourtwenty:block/hookah/{metal}'
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'render_type': 'minecraft:cutout_mipped',
            'textures': {'particle': tex['brass'], **tex}, 'elements': [turned(e, quarter) for e in hose()]}


def recoloured(name, tint):
    """The brass, retinted, keeping its polish."""
    a = np.asarray(Image.open(ASSETS / 'textures/block/hookah/brass.png').convert('RGB')).astype(np.float32)
    grey = a.mean(-1, keepdims=True) / 255
    save(Image.fromarray(np.clip(grey * np.array(tint, dtype=np.float32), 0, 255).astype(np.uint8)), name)


if __name__ == '__main__':
    textures()
    recoloured('copper', (235, 140, 84))
    recoloured('steel', (205, 214, 232))
    recoloured('gold', (255, 214, 96))
    for st in STATES:
        (ASSETS / f'models/block/weed_bong_{st}.json').write_text(json.dumps(model(st), indent=1) + '\n')
    states = (ASSETS / 'blockstates/weed_bong.json').read_text()
    (ASSETS / 'models/block/port').mkdir(exist_ok=True)
    for name, turns, metal in (('weed_bong', [0], 'brass'), ('double_hose', [0, 2], 'copper'), ('big_bertha', [0, 1, 2, 3], 'steel'), ('grand_hookah', [0, 1, 2, 3], 'gold')):
        for q in turns:
            side = ('east', 'south', 'west', 'north')[q]
            (ASSETS / f'models/block/port/{name}_hose_{side}.json').write_text(json.dumps(hose_part(metal, q), indent=1) + '\n')
        held = upgraded('empty', metal)
        held['elements'] += [turned(e, q) for q in turns for e in hose()]
        (ASSETS / f'models/block/{name}_item.json').write_text(json.dumps(held, indent=1) + '\n')
        (ASSETS / f'models/item/{name}.json').write_text(json.dumps({'parent': f'fourtwenty:block/{name}_item'}, indent=2) + '\n')
        if name == 'weed_bong':
            continue
        for st in STATES:
            (ASSETS / f'models/block/{name}_{st}.json').write_text(json.dumps(upgraded(st, metal), indent=1) + '\n')
        (ASSETS / f'blockstates/{name}.json').write_text(states.replace('block/weed_bong_', f'block/{name}_'))
    print(len(STATES), 'hookah models, three tiers')
