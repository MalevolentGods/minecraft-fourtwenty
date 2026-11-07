#!/usr/bin/env python3
"""The electronics in the Infuser's look (docs/electronics/style-pass.md): models and textures for the redesigned components.

    python3 tools/generate_component_lab.py      (run after tools/generate_components.py, whose plain models these replace)

Each component stands on a steel plinth (the Infuser's own 256-pixel steel), a slot along its front where its light line glows. Pins are
brass glands with a collar in their role's colour, centred where a Wick or Hemp Thread meets them. Glass is a translucent child of a NeoForge
composite, everything else cutout, as the Infuser's model is. What moves or glows with the circuit is drawn by client/ComponentRenderer from
what the part shows (domain/electronics/Shown): this file makes only what stands still. Models face north; the blockstate turns them.
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
TEX = ASSETS / 'textures/block/component'
S = 256
RNG = np.random.default_rng(4200)

# the Infuser's steel, glass and trim
INFUSER = {'body': 'fourtwenty:block/infuser/steel_body', 'dark': 'fourtwenty:block/infuser/steel_dark',
           'trim': 'fourtwenty:block/infuser/steel_trim', 'glass': 'fourtwenty:block/infuser/steel_glass'}
LEADS = {k: f'fourtwenty:block/component/lead_{k}' for k in ('in', 'out', 'vdd', 'gnd', 'gate')}


def rgb(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], float)


def save(arr, name):
    TEX.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).save(TEX / f'{name}.png', optimize=True)


def brushed(base, spread=16, along=1):
    """Brushed metal: streaks along one axis, fine grain over them."""
    streak = RNG.normal(0, spread, (S, 1)).repeat(S, 1)
    streak = (streak + np.roll(streak, 1, 0)) / 2
    grain = RNG.normal(0, spread * 0.35, (S, S))
    shade = streak + grain
    if along == 0:
        shade = shade.T
    return rgb(base)[None, None, :] + shade[:, :, None]


def textures():
    save(brushed('#C29A44', 18), 'lab_brass')
    save(brushed('#B5653A', 16), 'lab_copper_plain')
    # copper winding: turns of round wire, a highlight on each
    y = np.arange(S)[:, None].repeat(S, 1)
    turn = 0.5 + 0.5 * np.cos((y % 16) / 16 * 2 * np.pi)
    copper = rgb('#8E4A26')[None, None, :] + (turn[:, :, None] * rgb('#E59A64')[None, None, :] * 0.55) + RNG.normal(0, 6, (S, S, 1))
    save(copper, 'lab_coil')
    # spliff paper: off-white, faint laid lines, a few hemp flecks
    paper = rgb('#EDE6D3')[None, None, :] + RNG.normal(0, 5, (S, S, 1))
    paper[:, ::12, :] -= 10
    flecks = RNG.random((S, S)) > 0.992
    paper[flecks] = rgb('#7C8A3E')
    save(paper, 'lab_paper')
    # kief: gold dust with bright grains (drawn fullbright by the renderer, tinted by charge)
    k = RNG.random((S, S))
    kief = rgb('#D6A93C')[None, None, :] + (k[:, :, None] - 0.5) * 70
    kief[k > 0.95] = rgb('#FFF6BE')
    save(kief, 'lab_kief')
    # a plain soft white for glows the renderer tints
    g = 235 + RNG.normal(0, 8, (S, S))
    save(np.stack([g, g, g], -1), 'lab_glow')
    # the dark inside of a one-hitter's bowl
    save(rgb('#1A1410')[None, None, :] + RNG.normal(0, 6, (S, S, 1)), 'lab_char')


def face_uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
            'west': [z0, 16 - y1, z1, 16 - y0], 'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}[face]


def box(f, t, tex, faces=('north', 'south', 'east', 'west', 'up', 'down'), tint=None):
    out = {'from': list(f), 'to': list(t), 'faces': {}}
    for face in faces:
        uv = [max(0, min(16, v)) for v in face_uv(face, f, t)]
        out['faces'][face] = {'uv': uv, 'texture': tex}
        if tint is not None:
            out['faces'][face]['tintindex'] = tint
    return out


def plinth():
    """The steel plinth every part stands on, and the slot along its front where its light line glows."""
    return [box([2, 0, 2], [14, 1.5, 14], '#body'), box([2.5, 1.5, 2.5], [13.5, 2, 13.5], '#trim'),
            box([3, 0.5, 1.75], [13, 1.15, 2], '#dark', faces=('north', 'up', 'down', 'east', 'west'))]


def gland(side, kind, depth=2.5, y=8):
    """A brass gland on a pin face, its collar in the pin's role colour, centred at y 8 where a Wick or thread arrives."""
    lo, hi = 8 - 1.6, 8 + 1.6
    if side == 'front':
        return [box([lo, y - 1.6, 0], [hi, y + 1.6, depth], '#brass'), box([6, y - 2, 0.2], [10, y + 2, 0.8], f'#lead_{kind}')]
    if side == 'back':
        return [box([lo, y - 1.6, 16 - depth], [hi, y + 1.6, 16], '#brass'), box([6, y - 2, 15.2], [10, y + 2, 15.8], f'#lead_{kind}')]
    raise ValueError(side)


def stands(z0, z1, top):
    """Two little steel posts from the plinth up to a part held level, at z0 and z1."""
    return [box([7.25, 2, z0 - 0.6], [8.75, top, z0 + 0.6], '#body'), box([7.25, 2, z1 - 0.6], [8.75, top, z1 + 0.6], '#body')]


def composite(solid, glass, extra_tex=None):
    tex = {'particle': INFUSER['body'], **INFUSER, 'brass': 'fourtwenty:block/component/lab_brass', **{f'lead_{k}': v for k, v in LEADS.items()},
           **(extra_tex or {})}
    children = {'machine': {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout_mipped', 'ambientocclusion': False,
                            'textures': tex, 'elements': solid}}
    if glass:
        children['glass'] = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent', 'ambientocclusion': False,
                             'textures': tex, 'elements': glass}
    return {'parent': 'minecraft:block/block', 'loader': 'neoforge:composite', 'ambientocclusion': False, 'textures': tex, 'children': children}


# ---- the passives: two pins, back (south) to front (north), the part held level between them on two posts ----------------------------

def resinator():
    """A glass tube over a resin rod (the renderer draws the rod, warmed by its heat); its value's bands as rings round the rod."""
    solid = plinth() + stands(4.5, 11.5, 5) + gland('front', 'in', 3) + gland('back', 'in', 3)
    for i, z in enumerate((4.6, 6.4, 8.2, 10.6)):
        solid.append(box([6.3, 6.3, z], [9.7, 9.7, z + 1.2], '#band' if i < 3 else '#gold', faces=('east', 'west', 'up', 'down'), tint=i if i < 3 else None))
    glass = [box([5, 5, 3], [11, 11, 13], '#glass')]
    return composite(solid, glass, {'band': 'fourtwenty:block/component/band', 'gold': 'fourtwenty:block/component/band_gold'})


def kief_cap():
    """A glass jar with a steel lid; the renderer fills it with kief as it charges."""
    solid = plinth() + [box([3.7, 13, 3.7], [12.3, 14.5, 12.3], '#body'), box([4.2, 14.5, 4.2], [11.8, 15, 11.8], '#trim'),
                        box([4.2, 2, 4.2], [11.8, 2.2, 11.8], '#dark')]
    solid += gland('front', 'in', 4) + gland('back', 'in', 4)
    glass = [box([4, 2, 4], [12, 13, 12], '#glass')]
    return composite(solid, glass)


def spliff_coil():
    """Copper wound round a rolled spliff; the renderer rings it with light as current runs."""
    solid = plinth() + stands(4.5, 11.5, 6.5) + gland('front', 'in', 2.6) + gland('back', 'in', 2.6)
    solid.append(box([6.5, 6.5, 2.6], [9.5, 9.5, 13.4], '#paper'))
    for z in (3.6, 5.2, 6.8, 8.4, 10.0, 11.6):
        solid.append(box([6, 6, z], [10, 10, z + 1.1], '#coil', faces=('east', 'west', 'up', 'down', 'north', 'south')))
    return composite(solid, [], {'paper': 'fourtwenty:block/component/lab_paper', 'coil': 'fourtwenty:block/component/lab_coil'})


def hitter(band):
    """A glass one-hitter pipe, its brass bowl at the front (the cathode, banded); the renderer lights the bowl while it conducts."""
    solid = plinth() + stands(6, 11.5, 6.75) + gland('front', 'in', 2.2) + gland('back', 'in', 2.6)
    solid += [box([6, 5.5, 2.2], [10, 10.5, 5.6], '#brass', faces=('north', 'south', 'east', 'west', 'down')),
              box([6.4, 10.3, 2.6], [9.6, 10.4, 5.2], '#char', faces=('up',)),
              box([6.6, 6.6, 5.6], [9.4, 9.4, 6.6], f'#{band}')]
    glass = [box([6.75, 6.75, 6.6], [9.25, 9.25, 13.4], '#glass')]
    return composite(solid, glass, {'char': 'fourtwenty:block/component/lab_char', band: f'fourtwenty:block/component/{"lead_in" if band == "silver" else "lab_zen"}'})


LAB = {
    'resinator': resinator,
    'kief_cap': kief_cap,
    'spliff_coil': spliff_coil,
    'one_hitter': lambda: hitter('silver'),
    'zen_hitter': lambda: hitter('zen'),
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + '\n', encoding='utf-8')


def main():
    textures()
    save(rgb('#6FD38A')[None, None, :].repeat(S, 0).repeat(S, 1) + RNG.normal(0, 6, (S, S, 1)), 'lab_zen')
    for cid, make in LAB.items():
        model = make()
        write(ASSETS / f'models/block/component/{cid}.json', model)
        on = ASSETS / f'models/block/component/{cid}_on.json'
        if on.exists():
            write(on, model)
    print(f'wrote {len(LAB)} lab components')


if __name__ == '__main__':
    main()
