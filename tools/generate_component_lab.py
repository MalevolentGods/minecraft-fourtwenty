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
    """No plinth (the user, 2026-11-07): a part turns to any of 24 orientations, so it floats centred in its space, its glands reaching
    the faces. Kept as a function so the parts read the same."""
    return []


def gland(side, kind, depth=2.5, y=8):
    """A brass gland on a pin face, its collar in the pin's role colour, centred at y 8 where a Wick or thread arrives."""
    lo, hi = 8 - 1.6, 8 + 1.6
    if side == 'front':
        return [box([lo, y - 1.6, 0], [hi, y + 1.6, depth], '#brass'), box([6, y - 2, 0.2], [10, y + 2, 0.8], f'#lead_{kind}')]
    if side == 'back':
        return [box([lo, y - 1.6, 16 - depth], [hi, y + 1.6, 16], '#brass'), box([6, y - 2, 15.2], [10, y + 2, 15.8], f'#lead_{kind}')]
    raise ValueError(side)


def stands(z0, z1, top):
    """No stands without a plinth: the glands hold the part."""
    return []


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
        # closed rings: their ends too, or you'd see down the gap between band and rod
        solid.append(box([6.3, 6.3, z], [9.7, 9.7, z + 1.2], '#band' if i < 3 else '#gold', tint=i if i < 3 else None))
    # the glass hugs the bands, a finger's width out
    glass = [box([5.8, 5.8, 3], [10.2, 10.2, 13], '#glass')]
    return composite(solid, glass, {'band': 'fourtwenty:block/component/band', 'gold': 'fourtwenty:block/component/band_gold'})


def kief_cap():
    """A glass jar lying between its pins, a steel cap at each end; the renderer fills it with kief as it charges."""
    solid = [box([4, 4, 2.2], [12, 12, 3], '#body'), box([4, 4, 13], [12, 12, 13.8], '#body'),
             box([4.5, 4.5, 1.8], [11.5, 11.5, 2.2], '#trim'), box([4.5, 4.5, 13.8], [11.5, 11.5, 14.2], '#trim')]
    solid += gland('front', 'in', 1.8) + gland('back', 'in', 1.8)
    glass = [box([4.5, 4.5, 3], [11.5, 11.5, 13], '#glass')]
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
    # a solid bowl, its top charred, a brass rim standing round it (an open box showed through: it had no inside)
    bowl = box([6, 5.5, 2.2], [10, 10.5, 5.6], '#brass')
    bowl['faces']['up']['texture'] = '#char'
    solid += [bowl, box([6, 10.5, 2.2], [10, 10.9, 2.6], '#brass'), box([6, 10.5, 5.2], [10, 10.9, 5.6], '#brass'),
              box([6, 10.5, 2.6], [6.4, 10.9, 5.2], '#brass'), box([9.6, 10.5, 2.6], [10, 10.9, 5.2], '#brass'),
              box([6.6, 6.6, 5.6], [9.4, 9.4, 6.6], f'#{band}')]
    glass = [box([6.75, 6.75, 6.6], [9.25, 9.25, 13.4], '#glass')]
    return composite(solid, glass, {'char': 'fourtwenty:block/component/lab_char', band: f'fourtwenty:block/component/{"lead_in" if band == "silver" else "lab_zen"}'})


def toggle_joint():
    """A steel housing between the glands, a trim plate on top; its brass lever (the renderer's) swings over the pivot."""
    solid = [box([5, 5, 4], [11, 10, 12], '#body'), box([4.5, 10, 3.5], [11.5, 10.6, 12.5], '#trim'), box([6.4, 10.6, 6.9], [9.6, 11.6, 9.1], '#dark')]
    solid += gland('front', 'in', 4) + gland('back', 'in', 4)
    return composite(solid, [])


def puff_button():
    """A steel housing, a collar on top the mushroom button (the renderer's) sinks into."""
    # the trim stops at the housing's ends, short of the glands (their tops are level with it)
    solid = [box([5, 5, 4], [11, 9, 12], '#body'), box([4.5, 9, 4], [11.5, 9.6, 12], '#trim'), box([6.4, 9.6, 6.4], [9.6, 10, 9.6], '#dark')]
    solid += gland('front', 'in', 4) + gland('back', 'in', 4)
    return composite(solid, [])


def kill_switch():
    """A relay under glass: a copper coil and a contact on a steel base; its armature (the renderer's) snaps down onto the contact."""
    solid = [box([5, 5, 4], [11, 5.8, 12], '#body'), box([6, 5.8, 5], [8.2, 9, 11], '#coil'), box([9.6, 5.8, 9.6], [10.4, 8.0, 10.4], '#brass'),
             box([9.2, 5.8, 5], [10.4, 9.6, 5.8], '#brass')]
    solid += gland('front', 'in', 4) + gland('back', 'in', 4)
    # no floor: the steel base is the floor, and the coil and contact stand on it
    glass = [box([4.8, 5.85, 4], [11.2, 11.2, 12], '#glass', faces=('north', 'south', 'east', 'west', 'up'))]
    return composite(solid, glass, {'coil': 'fourtwenty:block/component/lab_coil'})


def slow_burner():
    """A rolled joint in a glass sleeve, burning slowly from its front end (the renderer lights its cherry with its current)."""
    solid = [box([6.7, 6.7, 3.6], [9.3, 9.3, 12.4], '#paper'), box([6.5, 6.5, 11.4], [9.5, 9.5, 12.6], '#crutch')]
    solid += gland('front', 'in', 3) + gland('back', 'in', 3)
    glass = [box([5.8, 5.8, 3], [10.2, 10.2, 13], '#glass')]
    return composite(solid, glass, {'paper': 'fourtwenty:block/component/lab_paper', 'crutch': 'fourtwenty:block/component/lab_brass'})


LAB = {
    'slow_burner': slow_burner,
    'toggle_joint': toggle_joint,
    'puff_button': puff_button,
    'kill_switch': kill_switch,
    'resinator': resinator,
    'kief_cap': kief_cap,
    'spliff_coil': spliff_coil,
    'one_hitter': lambda: hitter('silver'),
    'zen_hitter': lambda: hitter('zen'),
}


# ---- the creative supplies ---------------------------------------------------------------------------------------------------------

TIERS = ('puff', 'toke', 'rip', 'blaze')


def smoke():
    """Smoke that never settles: 32 frames of slow swirls, looping, for the Bottomless Stash's jar (an animated texture)."""
    n, frames = 64, 32
    y, x = np.mgrid[0:n, 0:n] / n * 2 * np.pi
    sheet = np.zeros((n * frames, n, 4))
    for f in range(frames):
        t = f / frames * 2 * np.pi
        v = (np.sin(x * 1 + np.sin(y * 2 + t) * 1.4 + t) + np.sin(y * 1 - np.cos(x * 2 - t) * 1.2 + 2 * t)
             + 0.6 * np.sin((x + y) * 2 + np.sin(x - t) + t) + 0.4 * np.sin((x - y) * 3 - 2 * t))
        v = (v - v.min()) / (v.max() - v.min())
        frame = np.zeros((n, n, 4))
        frame[..., :3] = (200 + 55 * v)[..., None] * np.array([0.93, 0.92, 1.0])
        frame[..., 3] = 60 + 150 * v ** 1.6
        sheet[f * n:(f + 1) * n] = frame
    TEX.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.clip(sheet, 0, 255).astype(np.uint8), 'RGBA').save(TEX / 'lab_smoke.png', optimize=True)
    (TEX / 'lab_smoke.png.mcmeta').write_text(json.dumps({'animation': {'frametime': 3, 'interpolate': True}}) + '\n', encoding='utf-8')


def bottomless(tier, lit):
    """A glass jar of smoke on a steel plinth, a band of its tier's colour round the lid."""
    glow = f'fourtwenty:block/power/{tier}_glow{"_lit" if lit else ""}'
    solid = [box([1, 0, 1], [15, 1.5, 15], '#body'), box([1.5, 1.5, 1.5], [14.5, 2, 14.5], '#trim'),
             box([2.6, 13, 2.6], [13.4, 14.6, 13.4], '#body'),
             box([2.5, 13.5, 2.5], [13.5, 14.1, 13.5], '#tier', faces=('north', 'south', 'east', 'west')),
             box([6.5, 14.6, 6.5], [9.5, 15.4, 9.5], '#trim')]
    glass = [box([3.3, 2.1, 3.3], [12.7, 12.8, 12.7], '#smoke'), box([3, 2, 3], [13, 13, 13], '#glass')]
    return composite(solid, glass, {'tier': glow, 'smoke': 'fourtwenty:block/component/lab_smoke'})


def bench_rig(lit):
    """A steel lab cabinet: a dark front panel (its readouts drawn by client/BenchRigRenderer), two knobs, red and black binding posts,
    a carrying handle at the back of its top, a light line along its foot."""
    light = f'fourtwenty:block/infuser/light{"" if lit else "_off"}'
    solid = [box([1, 0, 2], [15, 13.5, 15], '#body'), box([2, 1, 1.5], [14, 12.8, 2], '#dark', faces=('north', 'east', 'west', 'up', 'down')),
             box([2.5, 9.4, 1.3], [13.5, 12.6, 1.5], '#screen', faces=('north', 'east', 'west', 'up', 'down')),
             box([3.5, 2.2, 0.8], [5.5, 4.2, 1.5], '#trim'), box([6.2, 2.2, 0.8], [8.2, 4.2, 1.5], '#trim'),
             box([10, 2.6, 0.9], [11.4, 4, 1.5], '#lead_vdd'), box([12, 2.6, 0.9], [13.4, 4, 1.5], '#lead_gnd'),
             box([3, 13.5, 12], [4, 15, 13], '#trim'), box([12, 13.5, 12], [13, 15, 13], '#trim'), box([3, 15, 12], [13, 16, 13], '#trim'),
             box([3, 0.4, 1.75], [13, 1, 2], '#light', faces=('north', 'up', 'down', 'east', 'west'))]
    return composite(solid, [], {'light': light, 'screen': 'fourtwenty:block/component/lab_screen'})


def supplies():
    smoke()
    screen = rgb('#0B1210')[None, None, :] + RNG.normal(0, 3, (S, S, 1))
    screen[::4, :, :] += 4
    save(screen, 'lab_screen')
    variants = lambda name: {f'facing={f},lit={l}': {'model': f'fourtwenty:block/{name}{"_lit" if l == "true" else ""}', **({'y': y} if y else {})}
                             for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)) for l in ('false', 'true')}
    for tier in TIERS:
        for lit in (False, True):
            write(ASSETS / f'models/block/bottomless_stash_{tier}{"_lit" if lit else ""}.json', bottomless(tier, lit))
        write(ASSETS / f'blockstates/bottomless_stash_{tier}.json', {'variants': variants(f'bottomless_stash_{tier}')})
        write(ASSETS / f'models/item/bottomless_stash_{tier}.json', {'parent': f'fourtwenty:block/bottomless_stash_{tier}_lit'})
    for lit in (False, True):
        write(ASSETS / f'models/block/bench_rig{"_lit" if lit else ""}.json', bench_rig(lit))
    write(ASSETS / 'blockstates/bench_rig.json', {'variants': variants('bench_rig')})
    write(ASSETS / 'models/item/bench_rig.json', {'parent': 'fourtwenty:block/bench_rig_lit'})


def check(name, model):
    """No two faces in the same plane overlapping (they flicker as they fight): fail the build, not the player's eyes."""
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from generate_processing import coplanar
    elements = [e for child in model.get('children', {}).values() for e in child['elements'] if 'rotation' not in e]
    clash = coplanar(elements)
    assert not clash, f'{name} z-fights: {clash}'


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + '\n', encoding='utf-8')


def main():
    textures()
    save(rgb('#6FD38A')[None, None, :].repeat(S, 0).repeat(S, 1) + RNG.normal(0, 6, (S, S, 1)), 'lab_zen')
    for cid, make in LAB.items():
        model = make()
        check(cid, model)
        write(ASSETS / f'models/block/component/{cid}.json', model)
        on = ASSETS / f'models/block/component/{cid}_on.json'
        if on.exists():
            write(on, model)
    supplies()
    print(f'wrote {len(LAB)} lab components')


if __name__ == '__main__':
    main()
