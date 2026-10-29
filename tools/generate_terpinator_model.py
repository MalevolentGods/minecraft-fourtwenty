#!/usr/bin/env python3
"""The Terpinator block, in two styles (Steel and Lab), matching the Inspiration Infuser: models, textures, blockstates.

    python3 tools/generate_terpinator_model.py

Same geometry as the approved concept renders: a plinth with the lime light line; a tower at the back holding the
still, its window at the front showing the terpene level; an open bud hopper on top with a lime ring; a glass
condenser tube at the back right; a tray of three vials in front; a control panel at the front right with a black
glass display. Two models per style: idle (the liquid dim, the display dark) and working (lit: glowing liquid and
display). Materials (body, dark, trim, glass, light) are the Infuser's (tools/generate_infuser_model.py). A NeoForge
composite, the glass translucent. The front faces north; the blockstate turns it to face the player.
"""
import json

import numpy as np
from PIL import Image

import generate_infuser_model as INF

ASSETS, S, el, rgb, SIDES = INF.ASSETS, INF.S, INF.el, INF.rgb, INF.SIDES
VIALS = ['#2F6BE0', '#D7262E', '#F2C80F']
NAMES = {'steel': 'terpinator', 'lab': 'terpinator_lab'}


def save(img, name):
    p = ASSETS / f'textures/block/terpinator/{name}.png'
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p, optimize=True)


def textures():
    y, x = np.mgrid[0:S, 0:S]
    lime, pale, deep = rgb(INF.LIME), rgb('#D8FF8A'), rgb('#3E5A10')
    # working liquid: bright at the surface, deeper below, a few bubbles (seamless across faces)
    t = (y / S)[..., None]
    col = pale * (1 - t) * 0.5 + lime * (0.5 + 0.5 * t) * (1 - 0.25 * t)
    caustic = INF.tile_noise((S, S), 16, 11)[..., None]
    col = col * (1 + caustic * 0.025)
    for _ in range(9):
        bx, by, r = INF.RNG.uniform(0, S), INF.RNG.uniform(0, S), INF.RNG.uniform(3, 7)
        d = np.hypot(((x - bx + S / 2) % S) - S / 2, ((y - by + S / 2) % S) - S / 2)
        ring = np.exp(-((d - r) / 1.2) ** 2)
        col = col + (255 - col) * (ring * 0.3)[..., None]
    save(Image.fromarray(np.clip(col, 0, 255).astype(np.uint8)), 'liquid')
    # idle liquid: the same, settled and dim
    idle = deep * (0.85 + 0.15 * (1 - t)) * (1 + caustic * 0.05)
    save(Image.fromarray(np.clip(idle, 0, 255).astype(np.uint8)), 'liquid_idle')
    for i, h in enumerate(VIALS):
        base = rgb(h)
        tt = np.linspace(0, 1, S)[:, None, None]
        save(Image.fromarray(np.clip(base * (1.15 - 0.35 * tt) * np.ones((S, S, 1)), 0, 255).astype(np.uint8)), f'vial_{i}')


def geometry(lit):
    machine, glass = [], []
    liquid = 'liquid' if lit else 'liquid_idle'
    # plinth with the lime light line
    machine += [el(0, 0, 0, 16, 1.2, 16, 'dark', faces=SIDES + ('down',)),
                el(0, 1.2, 0, 16, 1.55, 16, 'light', glow=True, faces=SIDES),
                el(0, 1.55, 0, 16, 2.5, 16, 'body', faces=SIDES + ('up',)),
                el(0.5, 2.5, 0.5, 15.5, 3, 15.5, 'dark', faces=SIDES + ('up',))]
    # the tower and a polished band round its top
    machine += [el(1.5, 3, 5, 10.5, 12.8, 14, 'body', faces=SIDES),
                el(1.4, 12.8, 4.9, 10.6, 13.2, 14.1, 'trim', faces=SIDES + ('up',))]
    # polished edges up the tower's corners, as the concept's bevels catch the light
    for (x, z) in ((1.5, 5), (10.5, 5), (1.5, 14), (10.5, 14)):
        machine.append(el(x - 0.2, 3.6 if z == 5 else 3, z - 0.2, x + 0.2, 12.8, z + 0.2, 'trim', faces=SIDES))
    # the window: a polished frame, the still behind smoked glass (dark below, the terpene level above)
    machine += [el(2.6, 4.3, 4.5, 9.4, 4.5, 5.0, 'trim', faces=('north', 'east', 'west', 'up', 'down')),
                el(2.6, 11.2, 4.5, 9.4, 11.5, 5.0, 'trim', faces=('north', 'east', 'west', 'up', 'down')),
                el(2.6, 4.5, 4.5, 2.8, 11.2, 5.0, 'trim', faces=('north', 'east', 'west')),
                el(9.2, 4.5, 4.5, 9.4, 11.2, 5.0, 'trim', faces=('north', 'east', 'west')),
                el(2.8, 4.5, 4.85, 9.2, 7.6, 4.9, 'dark', faces=('north',)),
                el(2.8, 7.6, 4.85, 9.2, 11.2, 4.9, liquid, glow=lit, faces=('north',))]
    glass.append(el(2.8, 4.5, 4.6, 9.2, 11.2, 4.65, 'glass', faces=('north', 'south')))
    # the bud hopper on top: a stepped funnel, an open rim with a polished cap, a lime ring round the throat
    machine += [el(2.5, 13.2, 6, 9.5, 14.4, 13, 'dark', faces=SIDES),
                el(1.8, 14.4, 5.3, 10.2, 14.9, 13.7, 'body', faces=SIDES + ('down',))]
    for (x0, z0, x1, z1) in ((1.6, 5.1, 10.4, 6.1), (1.6, 12.9, 10.4, 13.9), (1.6, 6.1, 2.6, 12.9), (9.4, 6.1, 10.4, 12.9)):
        machine += [el(x0, 14.9, z0, x1, 15.9, z1, 'body', faces=SIDES),
                    el(x0, 15.9, z0, x1, 16.0, z1, 'trim', faces=SIDES + ('up',))]
    machine += [el(2.6, 14.9, 6.1, 9.4, 15.05, 12.9, 'dark', faces=('up',)),
                el(2.9, 15.2, 6.4, 9.1, 15.3, 12.6, 'light', glow=True, faces=('up',)),
                el(3.6, 15.3, 7.1, 8.4, 15.7, 11.9, 'dark', faces=SIDES + ('up',))]
    # the condenser: a glass tube at the back right with polished caps, a pipe over to the tower
    machine += [el(11.8, 3, 9.5, 14.8, 4, 12.5, 'trim', faces=SIDES + ('up',)),
                el(12.5, 4, 10.2, 14.1, 11.6, 11.8, liquid, glow=lit, faces=SIDES + ('up',)),
                el(11.8, 12.5, 9.5, 14.8, 13.3, 12.5, 'trim', faces=SIDES + ('up', 'down')),
                el(10.5, 11.5, 10.4, 12.2, 12.3, 11.6, 'trim', faces=('north', 'south', 'up', 'down'))]
    glass.append(el(12.2, 4, 9.9, 14.4, 12.5, 12.1, 'glass', faces=SIDES))
    # the vial tray in front, three vials of terpene in it
    machine += [el(1.5, 3, 1, 10.5, 3.6, 5, 'dark', faces=('north', 'east', 'west', 'up')),
                el(1.5, 3.6, 1, 10.5, 4.2, 1.6, 'trim', faces=SIDES + ('up',))]
    for i in range(3):
        x = 2.2 + i * 2.8
        machine += [el(x + 0.25, 3.6, 2.45, x + 1.55, 6.4, 3.75, f'vial_{i}', faces=SIDES + ('up',)),
                    el(x - 0.05, 7.6, 2.15, x + 1.85, 8.1, 4.05, 'trim', faces=SIDES + ('up', 'down'))]
        glass.append(el(x, 3.6, 2.2, x + 1.8, 7.6, 4.0, 'glass', faces=SIDES))
    # the control panel at the front right: black glass display, lime readout while it works
    machine += [el(11.3, 3, 2.2, 15.2, 9.6, 7.8, 'body', faces=SIDES + ('up',)),
                el(11.6, 4.4, 2.0, 14.9, 8.8, 2.2, 'dark', faces=('north', 'east', 'west', 'up', 'down')),
                el(12.3, 6.0, 1.9, 14.2, 6.7, 2.0, 'light' if lit else liquid, glow=lit, faces=('north', 'east', 'west', 'up', 'down')),
                el(12.3, 7.2, 1.9, 13.4, 7.6, 2.0, 'light' if lit else liquid, glow=lit, faces=('north', 'east', 'west', 'up', 'down'))]
    return machine, glass


DISPLAY = {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
           'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
           'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
           'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
           'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}


def write_models():
    for style, name in NAMES.items():
        tex = {'particle': f'fourtwenty:block/infuser/{style}_body', 'body': f'fourtwenty:block/infuser/{style}_body',
               'dark': f'fourtwenty:block/infuser/{style}_dark', 'trim': f'fourtwenty:block/infuser/{style}_trim',
               'glass': f'fourtwenty:block/infuser/{style}_glass', 'light': 'fourtwenty:block/infuser/light',
               'liquid': 'fourtwenty:block/terpinator/liquid', 'liquid_idle': 'fourtwenty:block/terpinator/liquid_idle'}
        tex.update({f'vial_{i}': f'fourtwenty:block/terpinator/vial_{i}' for i in range(3)})
        for lit in (False, True):
            machine, glass = geometry(lit)
            model = {'parent': 'minecraft:block/block', 'loader': 'neoforge:composite', 'ambientocclusion': False, 'textures': tex,
                     'children': {
                         'machine': {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout_mipped', 'ambientocclusion': False,
                                     'textures': tex, 'elements': machine},
                         'glass': {'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent', 'ambientocclusion': False,
                                   'textures': tex, 'elements': glass}},
                     'display': DISPLAY}
            (ASSETS / f'models/block/{name}{"_lit" if lit else ""}.json').write_text(json.dumps(model, indent=1) + '\n')
        (ASSETS / f'models/item/{name}.json').write_text(json.dumps({'parent': f'fourtwenty:block/{name}'}, indent=2) + '\n')
        variants = {}
        for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            for lit in (False, True):
                variants[f'facing={f},lit={str(lit).lower()}'] = {'model': f'fourtwenty:block/{name}{"_lit" if lit else ""}', **({'y': y} if y else {})}
        (ASSETS / f'blockstates/{name}.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    m, g = geometry(True)
    print('models', len(m), 'machine elements,', len(g), 'glass elements')


if __name__ == '__main__':
    old = ASSETS / 'textures/block/terpinator'
    if old.exists():
        for p in old.glob('*.png'):
            p.unlink()           # the old brass still's textures
    textures()
    write_models()
