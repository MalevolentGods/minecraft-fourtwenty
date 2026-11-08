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
from PIL import Image, ImageDraw, ImageFont

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
    if side == 'left':      # the model's left is east (+x)
        return [box([16 - depth, lo, lo], [16, hi, hi], '#brass'), box([15.2, 6, 6], [15.8, 10, 10], f'#lead_{kind}')]
    if side == 'right':
        return [box([0, lo, lo], [depth, hi, hi], '#brass'), box([0.2, 6, 6], [0.8, 10, 10], f'#lead_{kind}')]
    if side == 'top':
        return [box([lo, 16 - depth, lo], [hi, 16, hi], '#brass'), box([6, 15.2, 6], [10, 15.8, 10], f'#lead_{kind}')]
    if side == 'bottom':
        return [box([lo, 0, lo], [hi, depth, hi], '#brass'), box([6, 0.2, 6], [10, 0.8, 10], f'#lead_{kind}')]
    raise ValueError(side)


LIME = (155, 212, 42)


def plaque_texture(name, draw):
    """An engraved plaque: lime lines on dark steel, drawn at its own proportions (7 by 1.9) and stretched to a square, as its face
    shows the whole texture."""
    w, h = 256, 70
    img = Image.new('RGB', (w, h), (23, 25, 29))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, w - 2, h - 2], outline=(60, 64, 72), width=2)
    draw(d, w, h)
    img.resize((256, 256), Image.NEAREST).save(TEX / f'{name}.png', optimize=True)


def font(size):
    return ImageFont.load_default(size=size)


def bezier(p0, p1, p2, n=24):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0], (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1])
            for t in (i / n for i in range(n + 1))]


def gate(kind):
    """The gate's symbol, as in a schematic: inputs on the left, output on the right, a bubble for the inverted ones."""
    def draw(d, w, h):
        lw, cy, s = 5, h // 2, h * 0.36
        x0, x1 = w // 2 - 46, w // 2 + 34      # the body's back and tip
        bubble = kind in ('NOT', 'NAND', 'NOR', 'XNOR')
        back = x0                               # where the inputs reach the body
        if kind in ('NOT', 'BUFFER', 'TRI'):
            d.polygon([(x0, cy - s), (x1, cy), (x0, cy + s)], outline=LIME, width=lw)
            if kind == 'TRI':
                d.line([(w // 2 - 6, cy - s - 2), (w // 2 - 6, cy - s * 0.45)], fill=LIME, width=lw)
            tip = x1
        elif kind in ('AND', 'NAND'):
            r = s
            flat = x1 - r
            d.line([(x0, cy - s), (flat, cy - s)], fill=LIME, width=lw)
            d.line([(x0, cy + s), (flat, cy + s)], fill=LIME, width=lw)
            d.line([(x0, cy - s), (x0, cy + s)], fill=LIME, width=lw)
            d.arc([flat - r, cy - s, flat + r, cy + s], -90, 90, fill=LIME, width=lw)
            tip = flat + r
        else:   # the OR family: a concave back, two curves meeting at the tip
            top = bezier((x0, cy - s), (x0 + 0.62 * (x1 - x0), cy - s), (x1, cy))
            bottom = bezier((x0, cy + s), (x0 + 0.62 * (x1 - x0), cy + s), (x1, cy))
            backc = bezier((x0, cy - s), (x0 + 18, cy), (x0, cy + s))
            for line in (top, bottom, backc):
                d.line(line, fill=LIME, width=lw, joint='curve')
            if kind in ('XOR', 'XNOR'):
                d.line(bezier((x0 - 10, cy - s), (x0 + 8, cy), (x0 - 10, cy + s)), fill=LIME, width=lw, joint='curve')
            back = x0 + 8
            tip = x1
        if bubble:
            d.ellipse([tip + 1, cy - 7, tip + 15, cy + 7], outline=LIME, width=4)
            tip += 15
        d.line([(tip, cy), (w - 18, cy)], fill=LIME, width=lw)
        if kind in ('NOT', 'BUFFER', 'TRI'):
            d.line([(18, cy), (x0, cy)], fill=LIME, width=lw)
        else:
            d.line([(18, cy - s * 0.5), (back, cy - s * 0.5)], fill=LIME, width=lw)
            d.line([(18, cy + s * 0.5), (back, cy + s * 0.5)], fill=LIME, width=lw)
    return draw


def words(text, size=44):
    def draw(d, w, h):
        f = font(size)
        box_ = d.textbbox((0, 0), text, font=f)
        d.text(((w - (box_[2] - box_[0])) / 2 - box_[0], (h - (box_[3] - box_[1])) / 2 - box_[1]), text, fill=LIME, font=f)
    return draw


def square_wave(d, w, h):
    y0, y1, x = h - 18, 18, 22
    pts = []
    for i in range(4):
        pts += [(x, y0), (x, y1), (x + 26, y1), (x + 26, y0), (x + 52, y0)]
        x += 52
    d.line(pts, fill=LIME, width=5)


def opamp(d, w, h):
    cx, cy, s = w // 2, h // 2, h * 0.38
    d.polygon([(cx - 40, cy - s), (cx + 40, cy), (cx - 40, cy + s)], outline=LIME, width=5)
    # + by the top input, − by the bottom, drawn as strokes
    d.line([(cx - 34, cy - 12), (cx - 22, cy - 12)], fill=LIME, width=4)
    d.line([(cx - 28, cy - 18), (cx - 28, cy - 6)], fill=LIME, width=4)
    d.line([(cx - 34, cy + 12), (cx - 22, cy + 12)], fill=LIME, width=4)
    d.line([(18, cy - 12), (cx - 40, cy - 12)], fill=LIME, width=5)
    d.line([(18, cy + 12), (cx - 40, cy + 12)], fill=LIME, width=5)
    d.line([(cx + 40, cy), (w - 18, cy)], fill=LIME, width=5)


def header_texture():
    """An IDC header's face: nine gold sockets across it, evenly, so it reads the same either way round."""
    img = Image.new('RGB', (256, 256), (18, 20, 24))
    d = ImageDraw.Draw(img)
    for k in range(9):
        x = (2 + k) / 12 * 256
        d.rectangle([x - 7, 100, x + 7, 156], fill=(40, 42, 48))
        d.rectangle([x - 4, 112, x + 4, 144], fill=(200, 160, 60))
    d.rectangle([0, 0, 255, 255], outline=(70, 74, 82), width=6)
    img.save(TEX / 'lab_header.png', optimize=True)


def flat_face(element, face, tex):
    """Show a whole texture on one face (a plaque or header face), whatever its size."""
    element['faces'][face] = {'uv': [0, 0, 16, 16], 'texture': tex}
    return element


def nug_body(cid, layout, symbol_tex):
    """A Nug: a steel body, a lip, a glass dome (its crystal or pendulum the renderer's), a plaque engraved with what it is in front."""
    solid = [box([4, 4, 4], [12, 10, 12], '#body'), box([3.8, 9.6, 3.8], [12.2, 10.2, 12.2], '#trim'), box([5, 13.8, 5], [11, 14.4, 11], '#trim'),
             flat_face(box([4.5, 4.4, 3.8], [11.5, 6.2, 4.05], '#dark'), 'north', '#plaque')]
    if layout == 'none':
        # the Ticker's pendulum hangs from a pivot block under the cap
        solid.append(box([7.3, 13.1, 7.3], [8.7, 13.75, 8.7], '#dark'))
    solid += gland('top', 'vdd', 1.6) + gland('bottom', 'gnd', 4) + gland('front', 'out', 4)
    for face in {'one': ('back',), 'twoin': ('left', 'right'), 'none': ()}[layout]:
        solid += gland(face, 'in', 4)
    glass = [box([4.6, 10.2, 4.6], [11.4, 13.8, 11.4], '#glass')]
    return composite(solid, glass, {'plaque': f'fourtwenty:block/component/{symbol_tex}'})


NUGS = {'not_nug': ('one', 'NOT'), 'buffer_nug': ('one', 'BUFFER'), 'and_nug': ('twoin', 'AND'), 'or_nug': ('twoin', 'OR'),
        'nand_nug': ('twoin', 'NAND'), 'nor_nug': ('twoin', 'NOR'), 'xor_nug': ('twoin', 'XOR'), 'xnor_nug': ('twoin', 'XNOR'),
        'tri_state_nug': ('twoin', 'TRI'), 'latch_nug': ('twoin', 'SR'), 'flip_flop_nug': ('twoin', 'D FF'), 'ticker': ('none', 'WAVE')}

# bus chips: their faces (front, back, left, right): 'bus' for a header, a pin role for a gland, None for nothing
CHIPS = {
    'puff_pass_nug': ({'front': 'bus', 'back': 'in', 'left': 'in', 'right': 'in'}, 'SHIFT'),
    'pack_nug': ({'front': 'out', 'back': 'bus', 'left': 'in', 'right': 'in'}, 'LOAD'),
    'sampler_nug': ({'front': 'out', 'back': 'bus', 'left': 'bus'}, 'MUX'),
    'dealer_nug': ({'front': 'bus', 'back': 'in', 'left': 'bus'}, 'DEMUX'),
    'blend_nug': ({'front': 'bus', 'back': 'bus', 'left': 'bus'}, 'LOGIC'),
    'stacker_nug': ({'front': 'bus', 'back': 'bus', 'left': 'bus', 'right': 'in'}, 'ADD'),
    'scale_nug': ({'front': 'bus', 'back': 'bus', 'left': 'bus'}, 'CMP'),
    'jar_nug': ({'front': 'bus', 'back': 'bus', 'left': 'in', 'right': 'in'}, 'REG'),
}


def header(side):
    """An IDC header on a bus face, framing the Ribbon (x 3 to 14 at y 7 to 9) where it plugs in."""
    if side == 'front':
        return flat_face(box([2.5, 6, 0], [14.5, 10, 3], '#dark'), 'north', '#header')
    if side == 'back':
        return flat_face(box([2.5, 6, 13], [14.5, 10, 16], '#dark'), 'south', '#header')
    if side == 'left':
        # a hair taller than the front and back headers, so where two meet at a corner they cross instead of fighting
        return flat_face(box([13, 5.9, 2.5], [16, 10.1, 14.5], '#dark'), 'east', '#header')
    return flat_face(box([0, 5.9, 2.5], [3, 10.1, 14.5], '#dark'), 'west', '#header')


def chip(cid, faces, label):
    """A bus chip: a steel slab, a lip, nine resin beads under a glass strip (the renderer lights them), a label plate, headers and glands."""
    solid = [box([3, 4, 3], [13, 10, 13], '#body'), box([2.8, 9.6, 2.8], [13.2, 10.2, 13.2], '#trim'),
             flat_face(box([10.6, 10.2, 4], [12.4, 10.5, 12], '#dark'), 'up', '#label')]
    solid += gland('top', 'vdd', 5.8) + gland('bottom', 'gnd', 4)
    for face, role in faces.items():
        solid += [header(face)] if role == 'bus' else gland(face, role, 3)
    glass = [box([3.6, 10.2, 4], [5.4, 10.9, 12], '#glass')]
    return composite(solid, glass, {'header': 'fourtwenty:block/component/lab_header', 'label': f'fourtwenty:block/component/lab_label_{cid}'})


def fet(cid):
    """A three-legged glass bulb: source behind, drain in front, gate on its left; the renderer lights its resin core with its current."""
    solid = [box([5.5, 4.4, 5.5], [10.5, 5, 10.5], '#trim'), box([5.5, 11, 5.5], [10.5, 11.6, 10.5], '#trim')]
    solid += gland('back', 'in', 5) + gland('front', 'out', 5) + gland('left', 'gate', 5)
    glass = [box([5, 5, 5], [11, 11, 11], '#glass')]
    return composite(solid, glass)


def hype_amp():
    """A steel body with its op-amp symbol in front and a level window on top (the renderer raises a bar of light with its output)."""
    solid = [box([4, 4, 4], [12, 10, 12], '#body'), box([3.8, 9.6, 3.8], [12.2, 10.2, 12.2], '#trim'),
             flat_face(box([4.5, 4.4, 3.8], [11.5, 6.2, 4.05], '#dark'), 'north', '#plaque')]
    solid += gland('top', 'vdd', 5.8) + gland('bottom', 'gnd', 4) + gland('front', 'out', 4) + gland('left', 'in', 4) + gland('right', 'in', 4)
    glass = [box([4.4, 10.2, 4.4], [6, 11, 11.6], '#glass')]
    return composite(solid, glass, {'plaque': 'fourtwenty:block/component/lab_plaque_hype_amp'})


def numb_nug():
    """A digit: a steel bezel the full width of its space (so a row sits flush as one screen), dark glass in front; its segments, and the
    lever of the hex/decimal toggle on its back, are the renderer's. Power top and bottom, an IDC header on its back."""
    solid = [box([0, 2, 5], [16, 14, 12], '#body'), box([0, 2, 4.3], [16, 3, 5], '#trim'), box([0, 13, 4.3], [16, 14, 5], '#trim'),
             box([0, 3, 4.8], [16, 13, 5], '#screen'), flat_face(box([2.5, 6, 12], [14.5, 10, 16], '#dark'), 'south', '#header'),
             box([6.6, 10.6, 12], [9.4, 13.6, 12.4], '#dark')]
    solid += gland('top', 'vdd', 2) + gland('bottom', 'gnd', 2)
    glass = [box([0, 3, 4.4], [16, 13, 4.6], '#glass', faces=('north',))]
    return composite(solid, glass, {'header': 'fourtwenty:block/component/lab_header', 'screen': 'fourtwenty:block/component/lab_screen'})


# ---- mounted on a Rolling Tray: a plate the full face of its space, its back on the tray (z 13 to 16), a trim frame round its front ----

def plate(z=13, depth=0.5):
    """A panel plate: steel the full face, a trim frame standing proud round its edge (no back faces where it sits on the plate, and its
    sides only where they show), so plates side by side read as one panel."""
    f = z - depth
    return [box([0, 0, z], [16, 16, 16], '#body'),
            box([0, 15, f], [16, 16, z], '#trim', faces=('north', 'east', 'west', 'up', 'down')),
            box([0, 0, f], [16, 1, z], '#trim', faces=('north', 'east', 'west', 'up', 'down')),
            box([0, 1, f], [1, 15, z], '#trim', faces=('north', 'east', 'west')),
            box([15, 1, f], [16, 15, z], '#trim', faces=('north', 'east', 'west'))]


def puff_button_mounted():
    """A dark collar in the plate; the brass cap (the renderer's) stands out of it and pushes in."""
    return composite(plate() + [box([5, 5, 12], [11, 11, 13], '#dark', faces=('north', 'east', 'west', 'up', 'down'))], [])


def toggle_joint_mounted():
    """A trim boss the lever (the renderer's) pivots on, flipping up for on; its lamp beside it is the renderer's too."""
    return composite(plate() + [box([6, 6, 11.6], [10, 10, 13], '#trim', faces=('north', 'east', 'west', 'up', 'down'))], [])


def numb_nug_mounted():
    """The digit's screen the full face of the plate, deeper than a switch's (z 12 to 16), trim above and below, dark glass in front."""
    solid = [box([0, 0, 12], [16, 16, 16], '#body'),
             box([0, 15, 11.3], [16, 16, 12], '#trim', faces=('north', 'east', 'west', 'up', 'down')),
             box([0, 0, 11.3], [16, 1, 12], '#trim', faces=('north', 'east', 'west', 'up', 'down')),
             box([0, 1, 11.8], [16, 15, 12], '#screen', faces=('north',))]
    glass = [box([0, 1, 11.4], [16, 15, 11.6], '#glass', faces=('north',))]
    return composite(solid, glass, {'screen': 'fourtwenty:block/component/lab_screen'})


MOUNTED = {'puff_button': puff_button_mounted, 'toggle_joint': toggle_joint_mounted, 'numb_nug': numb_nug_mounted}


def rolling_tray():
    """The Rolling Tray: a steel board. Its front a brass-framed socket the part sits in, four screws; its sides the board's rails, a red
    line and a black; its back an IDC header with the channel's band (resistor colour code, the carry grey, none steel)."""
    front = Image.fromarray(np.clip(brushed('#4A4E56', 12), 0, 255).astype(np.uint8))
    d = ImageDraw.Draw(front)
    d.rectangle([18, 18, 237, 237], outline=(194, 154, 68), width=10)
    d.rectangle([30, 30, 225, 225], fill=(30, 32, 37))
    for x, y in ((8, 8), (232, 8), (8, 232), (232, 232)):
        d.ellipse([x - 6 + 6, y - 6 + 6, x + 6 + 6, y + 6 + 6], fill=(150, 154, 160))
    d.rectangle([0, 0, 255, 255], outline=(110, 114, 122), width=4)
    front.save(TEX / 'rolling_tray_front.png', optimize=True)
    side = Image.fromarray(np.clip(brushed('#5A5E66', 12), 0, 255).astype(np.uint8))
    d = ImageDraw.Draw(side)
    d.rectangle([0, 92, 255, 104], fill=(208, 40, 40))
    d.rectangle([0, 152, 255, 164], fill=(24, 24, 28))
    d.rectangle([0, 0, 255, 255], outline=(110, 114, 122), width=4)
    side.save(TEX / 'rolling_tray_side.png', optimize=True)
    code = [0x1E1E1E, 0x7A4A21, 0xD02020, 0xF08020, 0xF0D020, 0x30A040, 0x2050D0, 0x8040C0, 0x808080]
    for k in list(range(9)) + [None]:
        back = Image.fromarray(np.clip(brushed('#2C2F35', 10), 0, 255).astype(np.uint8))
        d = ImageDraw.Draw(back)
        for i in range(9):
            x = (2 + i) / 12 * 256
            d.rectangle([x - 7, 100, x + 7, 156], fill=(40, 42, 48))
            d.rectangle([x - 4, 112, x + 4, 144], fill=(200, 160, 60))
        band = (120, 124, 132) if k is None else tuple(int(code[k] >> sh & 255) for sh in (16, 8, 0))
        d.rectangle([40, 196, 215, 222], fill=band, outline=(70, 74, 82), width=3)
        d.rectangle([0, 0, 255, 255], outline=(110, 114, 122), width=4)
        name = 'rolling_tray_back' + ('' if k is None else f'_c{k}')
        back.save(TEX / f'{name}.png', optimize=True)
        tex = {'north': 'fourtwenty:block/component/rolling_tray_front', 'south': f'fourtwenty:block/component/{name}',
               'east': 'fourtwenty:block/component/rolling_tray_side', 'west': 'fourtwenty:block/component/rolling_tray_side',
               'up': 'fourtwenty:block/component/rolling_tray_side', 'down': 'fourtwenty:block/component/rolling_tray_side',
               'particle': 'fourtwenty:block/component/rolling_tray_side'}
        write(ASSETS / f'models/block/{name.replace("_back", "")}.json', {'parent': 'minecraft:block/cube', 'textures': tex})
    turns = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    variants = {}
    for facing, turn in turns.items():
        for k in range(10):
            variants[f'channel={k},facing={facing}'] = {'model': 'fourtwenty:block/rolling_tray' + ('' if k == 9 else f'_c{k}'), **turn}
    write(ASSETS / 'blockstates/rolling_tray.json', {'variants': variants})
    write(ASSETS / 'models/item/rolling_tray.json', {'parent': 'fourtwenty:block/rolling_tray'})


def family_textures():
    header_texture()
    for cid, (layout, kind) in NUGS.items():
        draw = square_wave if kind == 'WAVE' else words(kind) if kind in ('SR', 'D FF') else gate(kind)
        plaque_texture(f'lab_plaque_{cid}', draw)
    plaque_texture('lab_plaque_hype_amp', opamp)
    for cid, (_, label) in CHIPS.items():
        # read along the plate's length (z), so drawn sideways
        img = Image.new('RGB', (256, 58), (23, 25, 29))
        d = ImageDraw.Draw(img)
        d.rectangle([1, 1, 254, 56], outline=(60, 64, 72), width=2)
        words(label, 40)(d, 256, 58)
        img.rotate(90, expand=True).resize((256, 256), Image.NEAREST).save(TEX / f'lab_label_{cid}.png', optimize=True)
    # Slow Burner: white rolling paper, a brown filter tip
    paper = rgb('#F6F3EA')[None, None, :] + RNG.normal(0, 3, (S, S, 1))
    paper[:, ::16, :] -= 7
    save(paper, 'lab_paper_white')
    filt = rgb('#B07A3E')[None, None, :] + RNG.normal(0, 10, (S, S, 1))
    save(filt, 'lab_filter')



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
    return composite(solid, glass, {'paper': 'fourtwenty:block/component/lab_paper_white', 'crutch': 'fourtwenty:block/component/lab_filter'})


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
    family_textures()
    for cid, (layout, kind) in NUGS.items():
        LAB[cid] = (lambda c=cid, l=layout: nug_body(c, l, f'lab_plaque_{c}'))
    for cid, (faces, _) in CHIPS.items():
        LAB[cid] = (lambda c=cid, f=faces: chip(c, f, None))
    LAB['sativa_fet'] = lambda: fet('sativa_fet')
    LAB['indica_fet'] = lambda: fet('indica_fet')
    LAB['hype_amp'] = hype_amp
    LAB['numb_nug'] = numb_nug
    save(rgb('#6FD38A')[None, None, :].repeat(S, 0).repeat(S, 1) + RNG.normal(0, 6, (S, S, 1)), 'lab_zen')
    for cid, make in LAB.items():
        model = make()
        check(cid, model)
        write(ASSETS / f'models/block/component/{cid}.json', model)
        on = ASSETS / f'models/block/component/{cid}_on.json'
        if on.exists():
            write(on, model)
    for cid, make in MOUNTED.items():
        model = make()
        check(cid + '_mounted', model)
        write(ASSETS / f'models/block/component/{cid}_mounted.json', model)
    supplies()
    # last, so the textures before it keep their noise
    rolling_tray()
    print(f'wrote {len(LAB)} lab components, {len(MOUNTED)} mounted plates and the Rolling Tray')


if __name__ == '__main__':
    main()
