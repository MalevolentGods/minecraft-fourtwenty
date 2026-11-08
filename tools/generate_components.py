#!/usr/bin/env python3
"""Electronics (docs/electronics/audit-and-plan.md): art, models, blockstates, item models, recipes, loot and lang for the components and
Hemp Thread.

    python3 tools/generate_components.py

Each component is a 12-pixel body with a lead on every pin face: red on top (VDD), black underneath (GND), gold out the front of a logic
part, silver for inputs and for both ends of a two-pin part. Its front shows what it is (a resistor's bands, a Nug's label); a logic part
or a switch has an "on" look as well. Models face north; the blockstate turns them.
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
DATA = ROOT / 'src/main/resources/data'
TEX = ASSETS / 'textures/block/component'

# id, display name, layout, look
TYPES = [
    ('resinator', 'Resinator', 'two', 'resistor'),
    ('kief_cap', 'Kief Cap', 'two', 'capacitor'),
    ('spliff_coil', 'Spliff Coil', 'two', 'coil'),
    ('one_hitter', 'One-Hitter', 'two', 'diode'),
    ('zen_hitter', 'Zen Hitter', 'two', 'zener'),
    ('toggle_joint', 'Toggle Joint', 'two', 'toggle'),
    ('kill_switch', 'Kill Switch', 'two', 'kill'),
    ('puff_button', 'Puff Button', 'two', 'button'),
    ('slow_burner', 'Slow Burner', 'two', 'burner'),
    ('not_nug', 'NOT Nug', 'one', 'NOT'),
    ('buffer_nug', 'Buffer Nug', 'one', 'BUF'),
    ('and_nug', 'AND Nug', 'twoin', 'AND'),
    ('or_nug', 'OR Nug', 'twoin', 'OR'),
    ('nand_nug', 'NAND Nug', 'twoin', 'NAN'),
    ('nor_nug', 'NOR Nug', 'twoin', 'NOR'),
    ('xor_nug', 'XOR Nug', 'twoin', 'XOR'),
    ('xnor_nug', 'XNOR Nug', 'twoin', 'XNR'),
    ('tri_state_nug', 'Tri-State Nug', 'twoin', 'TRI'),
    ('latch_nug', 'Latch Nug', 'twoin', 'LAT'),
    ('flip_flop_nug', 'Flip-Flop Nug', 'twoin', 'DFF'),
    ('ticker', 'Ticker', 'none', 'TIK'),
    ('sativa_fet', 'Sativa FET', 'fet', 'nfet'),
    ('indica_fet', 'Indica FET', 'fet', 'pfet'),
    ('hype_amp', 'Hype Amp', 'amp', 'AMP'),
    ('puff_pass_nug', 'Puff-Pass Nug', 'puffpass', 'PUF'),
    ('pack_nug', 'Pack Nug', 'pack', 'PAK'),
    ('sampler_nug', 'Sampler Nug', 'sampler', 'MUX'),
    ('dealer_nug', 'Dealer Nug', 'dealer', 'DMX'),
    ('blend_nug', 'Blend Nug', 'blend', 'BIT'),
    ('stacker_nug', 'Stacker Nug', 'stacker', 'ADD'),
    ('scale_nug', 'Scale Nug', 'scale', 'CMP'),
    ('jar_nug', 'Jar Nug', 'jar', 'REG'),
    ('numb_nug', 'Numb Nug', 'display', 'NUM'),
    ('dankoder', 'Dankoder', 'dankoder', 'ENC'),
]

# the resistor colour code for channels 0 to 7, the carry grey: an Eighth Ribbon's stripes and a Roach Clip's band
CHANNELS = [(0x1E, 0x1E, 0x1E), (0x7A, 0x4A, 0x21), (0xD0, 0x20, 0x20), (0xF0, 0x80, 0x20), (0xF0, 0xD0, 0x20), (0x30, 0xA0, 0x40),
            (0x20, 0x50, 0xD0), (0x80, 0x40, 0xC0), (0x80, 0x80, 0x80)]

TOOLTIPS = {
    'two': 'Two pins, back and front: any Wick or Hemp Thread on either end joins it.',
    'one': 'Input at the back, output in front; VDD on top, GND underneath. Logic at the supply it gets: 30% and 70% of it are its thresholds.',
    'twoin': 'Inputs on the left and right, output in front; VDD on top, GND underneath. Logic at the supply it gets: 30% and 70% of it are its thresholds.',
    'none': 'Output in front; VDD on top, GND underneath.',
    'fet': 'Source at the back, drain in front, gate on the left (the green lead).',
    'amp': 'Inputs + on the left and − on the right, output in front; VDD on top, GND underneath.',
    'puffpass': 'Data at the back, clock on the left, latch on the right; the 8 outputs and the carry on the bus face in front, for an Eighth Ribbon. VDD on top, GND underneath.',
    'pack': 'Load on the left, clock on the right, the 8 inputs on the bus face at the back, serial out in front. VDD on top, GND underneath.',
    'sampler': 'The 8 inputs on the bus face at the back, select (channels 0 to 2) on the bus face on the left, output in front. VDD on top, GND underneath.',
    'dealer': 'Input at the back, select (channels 0 to 2) on the bus face on the left, the 8 outputs on the bus face in front. VDD on top, GND underneath.',
    'blend': 'Bus A at the back, bus B on the left, the result on the bus in front. VDD on top, GND underneath.',
    'stacker': 'Bus A at the back, bus B on the left, carry in on the right, the result in front with the carry out on its C wire. VDD on top, GND underneath.',
    'scale': 'Bus A at the back, bus B on the left; in front, channel 0 is A < B, 1 is A = B, 2 is A > B. VDD on top, GND underneath.',
    'jar': 'Bus in at the back, clock on the left, release on the right, bus out in front. VDD on top, GND underneath.',
    'dankoder': 'Keys 0 to 7 on the bus face on the left, 8 to F on the right; the number out in front (channels 0 to 3), the carry high while a key is down. Its back on a Rolling Tray reads the whole board instead, power and all. VDD on top, GND underneath.',
    'display': 'A byte in on the bus face at the back (an Eighth Ribbon), VDD on top, GND underneath. Numb Nugs side by side, facing the same way, are one display: only one of them needs power and a Ribbon.',
}
EXTRA = {
    'resinator': 'A resistor, rated 10 W: past that it burns out in smoke. Use it to step its value (sneak to step back).',
    'kief_cap': 'A capacitor, in Garcias. Use it to step its value (sneak to step back).',
    'spliff_coil': 'An inductor, in Marleys. Use it to step its value (sneak to step back).',
    'one_hitter': 'A diode: current flows only from its back to its banded front.',
    'zen_hitter': 'A Zener diode: wired banded end to +, it holds its voltage. Use it to step its voltage.',
    'toggle_joint': 'A switch: use it to flip it. Mounted on a Rolling Tray, sneak-use it with an empty hand to give it a label, a glow and the number a Dankoder reads it as.',
    'kill_switch': 'A switch closed while it has a redstone signal.',
    'puff_button': 'A button: closed for a second when used. Mounted on a Rolling Tray, sneak-use it with an empty hand to give it a label, a glow and the number a Dankoder reads it as (shown on its cap until it has a label).',
    'dankoder': 'A keypad encoder: while a key is down it gives that key\'s number; let go, it keeps the last one. Two down, the lower wins. Against a board of Rolling Trays it numbers the buttons and toggles on it in reading order, top row first, left to right; each key\'s own screen (sneak-use it with an empty hand) can give it any number 0 to F instead.',
    'slow_burner': 'Lets through a steady current, its setting, whatever the voltage across it once that is a Kb or so: a Kief Cap fills or drains at a steady rate through it. It makes no current of its own; a supply drives it. Use it to step its current (sneak to step back).',
    'tri_state_nug': 'Left is A, right is enable: passes A while enabled, lets go of its output otherwise.',
    'latch_nug': 'An SR latch: left sets it, right resets it.',
    'flip_flop_nug': 'A D flip-flop: left is D, right is the clock; it takes D on each rising edge.',
    'ticker': 'A clock: a square wave. Use it to step its period (sneak to step back).',
    'sativa_fet': 'An N-channel MOSFET: a gate more than 1 Kb above its source lets current run from drain to source, more the higher it goes.',
    'indica_fet': 'A P-channel MOSFET: a gate more than 1 Kb below its source lets current run from source to drain, more the lower it goes.',
    'puff_pass_nug': 'A shift register: each rising clock moves its 8 bits up one and takes data in at bit 0; a rising latch shows them all at once. The carry is bit 7, for the next one\'s data.',
    'pack_nug': 'A shift register the other way: load takes its 8 inputs, then each rising clock sends the next out, bit 7 first.',
    'sampler_nug': 'A multiplexer: its output follows the input the select picks (0 to 7).',
    'dealer_nug': 'A demultiplexer: the output the select picks (0 to 7) follows its input; the others are 0.',
    'blend_nug': 'Bitwise logic on two buses, all nine wires at once: AND, OR, XOR, NAND, NOR, XNOR, NOT A or A. Use it to step the operation.',
    'stacker_nug': 'Adds two bytes and the carry in (or subtracts B and the borrow from A), the carry (or borrow) out on the C wire, so two make 16 bits. Use it to switch add and subtract.',
    'scale_nug': 'Weighs two bytes against each other: exactly one of its outputs is high.',
    'numb_nug': 'A digit of a display: the nine wires of a Ribbon, the byte and its carry, 0 to 511 in decimal or 000 to 1FF in hex. Use its back to switch. The one with power and data sets the whole row.',
    'jar_nug': 'Keeps a byte (nine bits): takes its inputs on each rising clock and drives them out until release is 1, when it lets go, so several can share one bus.',
    'hype_amp': 'An op-amp: drives its output to 100,000 times the difference of its inputs, as far as its supply allows. With Resinators for feedback it amplifies by their ratio.',
}

BODY = {'resistor': (196, 170, 120), 'capacitor': (52, 92, 160), 'coil': (70, 54, 40), 'diode': (30, 30, 34), 'zener': (30, 30, 34),
        'toggle': (110, 112, 118), 'kill': (120, 40, 36), 'button': (110, 112, 118), 'burner': (237, 230, 211), 'nfet': (44, 74, 48), 'pfet': (74, 48, 84)}
CHIP = (26, 30, 30)
LEAD = {'in': (190, 192, 198), 'out': (220, 176, 60), 'vdd': (200, 40, 40), 'gnd': (24, 24, 26), 'gate': (90, 200, 90)}
# a bus face: a wide flat connector, the Ribbon's stripes on it
BUS_LEADS = {'front': ([3, 7, 0], [14, 9, 2]), 'back': ([3, 7, 14], [14, 9, 16]), 'left': ([14, 7, 3], [16, 9, 14]), 'right': ([0, 7, 3], [2, 9, 14])}

# a 3x5 pixel font for the Nug labels
FONT = {
    'A': ['010', '101', '111', '101', '101'], 'C': ['111', '100', '100', '100', '111'], 'E': ['111', '100', '110', '100', '111'],
    'G': ['111', '100', '101', '101', '111'], 'B': ['110', '101', '110', '101', '110'], 'D': ['110', '101', '101', '101', '110'],
    'F': ['111', '100', '110', '100', '100'], 'I': ['111', '010', '010', '010', '111'], 'K': ['101', '110', '100', '110', '101'],
    'L': ['100', '100', '100', '100', '111'], 'M': ['101', '111', '111', '101', '101'], 'P': ['110', '101', '110', '100', '100'], 'N': ['101', '111', '111', '111', '101'], 'O': ['111', '101', '101', '101', '111'],
    'R': ['110', '101', '110', '101', '101'], 'T': ['111', '010', '010', '010', '010'], 'U': ['101', '101', '101', '101', '111'],
    'X': ['101', '101', '010', '101', '101'],
}


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + '\n', encoding='utf-8')


def side_texture(colour, name):
    img = Image.new('RGBA', (16, 16), colour + (255,))
    d = ImageDraw.Draw(img)
    hi = tuple(min(255, c + 30) for c in colour)
    d.rectangle([0, 0, 15, 15], outline=hi + (255,))
    save(img, TEX / f'{name}.png')


def front_texture(cid, look, on):
    if look in BODY:
        img = Image.new('RGBA', (16, 16), BODY[look] + (255,))
        d = ImageDraw.Draw(img)
        if look == 'resistor':
            pass  # its bands are rings round the body, coloured from its value (client/WickColours)
        elif look == 'capacitor':
            d.rectangle([11, 0, 13, 15], fill=(220, 220, 230, 255))
            d.text((3, 4), '', fill=(255, 255, 255, 255))
        elif look == 'coil':
            for y in range(1, 15, 3):
                d.line([1, y, 14, y + 1], fill=(200, 110, 60, 255))
        elif look in ('diode', 'zener'):
            d.rectangle([1, 11, 14, 13], fill=((200, 200, 205) if look == 'diode' else (90, 200, 90)) + (255,))
        elif look == 'toggle':
            d.rectangle([6, 6, 9, 9], fill=(60, 60, 64, 255))
            d.line([7, 7, 7, 1] if on else [7, 8, 7, 14], fill=(230, 230, 230, 255), width=2)
        elif look == 'kill':
            d.rectangle([4, 4, 11, 11], fill=((90, 220, 90) if on else (40, 20, 20)) + (255,))
        elif look == 'button':
            d.rectangle([4, 4, 11, 11], fill=((240, 220, 120) if on else (160, 140, 80)) + (255,))
        elif look in ('nfet', 'pfet'):
            # a power transistor: a black package under a drilled metal tab, N or P on it, a dot lit while its channel conducts
            d.rectangle([1, 1, 14, 14], fill=(30, 30, 34, 255))
            d.rectangle([1, 1, 14, 4], fill=(176, 178, 184, 255))
            d.rectangle([7, 2, 8, 3], fill=(60, 60, 64, 255))
            ink = (120, 230, 120) if look == 'nfet' else (200, 130, 230)
            for r, row in enumerate(FONT['N' if look == 'nfet' else 'P']):
                for c, bit in enumerate(row):
                    if bit == '1':
                        d.point([6 + c, 7 + r], fill=ink + (255,))
            d.rectangle([11, 11, 12, 12], fill=(ink if on else (50, 50, 50)) + (255,))
        d.rectangle([0, 0, 15, 15], outline=(255, 255, 255, 60))
        save(img, TEX / f'{cid}_front{"_on" if on else ""}.png')
        return
    img = Image.new('RGBA', (16, 16), CHIP + (255,))
    d = ImageDraw.Draw(img)
    for x in (2, 6, 10, 14):
        d.point([x, 0], fill=(180, 180, 186, 255))
        d.point([x, 15], fill=(180, 180, 186, 255))
    x0 = 2
    for ch in look[:3]:
        for r, row in enumerate(FONT[ch]):
            for c, bit in enumerate(row):
                if bit == '1':
                    d.point([x0 + c, 4 + r], fill=(230, 230, 220, 255))
        x0 += 4
    if look == 'AMP':
        # + on the left input's side, − on the right's, as you face it
        d.line([2, 11, 4, 11], fill=(230, 90, 90, 255))
        d.line([3, 10, 3, 12], fill=(230, 90, 90, 255))
        d.line([11, 11, 13, 11], fill=(150, 170, 230, 255))
    d.rectangle([7, 11, 8, 12], fill=((120, 255, 90) if on else (40, 70, 40)) + (255,))
    save(img, TEX / f'{cid}_front{"_on" if on else ""}.png')


def lead_textures():
    save(Image.new('RGBA', (16, 16), (255, 255, 255, 255)), TEX / 'band.png')
    save(Image.new('RGBA', (16, 16), (212, 175, 55, 255)), TEX / 'band_gold.png')
    for name, c in LEAD.items():
        save(Image.new('RGBA', (16, 16), c + (255,)), TEX / f'lead_{name}.png')
    side_texture(CHIP, 'chip_side')
    ribbon_texture()
    for look, c in BODY.items():
        side_texture(c, f'{look}_side')


def box(frm, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    uv = {'north': [x0, 16 - y1, x1, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0], 'east': [z0, 16 - y1, z1, 16 - y0],
          'west': [z0, 16 - y1, z1, 16 - y0], 'up': [x0, z0, x1, z1], 'down': [x0, z0, x1, z1]}
    return {'from': frm, 'to': to, 'faces': {f: {'uv': uv[f], 'texture': tex if isinstance(tex, str) else tex[f]} for f in faces}}


# pin leads for a model facing north: front is north, back south, left (clockwise of north) east, right west
LEADS = {'front': ([7, 7, 0], [9, 9, 2]), 'back': ([7, 7, 14], [9, 9, 16]), 'left': ([14, 7, 7], [16, 9, 9]), 'right': ([0, 7, 7], [2, 9, 9]),
         'top': ([7, 14, 7], [9, 16, 9]), 'bottom': ([7, 0, 7], [9, 2, 9])}
PINS = {'two': [('back', 'in'), ('front', 'in')], 'one': [('top', 'vdd'), ('bottom', 'gnd'), ('front', 'out'), ('back', 'in')],
        'twoin': [('top', 'vdd'), ('bottom', 'gnd'), ('front', 'out'), ('left', 'in'), ('right', 'in')],
        'amp': [('top', 'vdd'), ('bottom', 'gnd'), ('front', 'out'), ('left', 'in'), ('right', 'in')],
        'fet': [('back', 'in'), ('front', 'out'), ('left', 'gate')],
        'puffpass': [('top', 'vdd'), ('bottom', 'gnd'), ('back', 'in'), ('left', 'in'), ('right', 'in'), ('front', 'bus')],
        'pack': [('top', 'vdd'), ('bottom', 'gnd'), ('left', 'in'), ('right', 'in'), ('back', 'bus'), ('front', 'out')],
        'sampler': [('top', 'vdd'), ('bottom', 'gnd'), ('back', 'bus'), ('left', 'bus'), ('front', 'out')],
        'dealer': [('top', 'vdd'), ('bottom', 'gnd'), ('back', 'in'), ('left', 'bus'), ('front', 'bus')],
        'blend': [('top', 'vdd'), ('bottom', 'gnd'), ('back', 'bus'), ('left', 'bus'), ('front', 'bus')],
        'stacker': [('top', 'vdd'), ('bottom', 'gnd'), ('back', 'bus'), ('left', 'bus'), ('right', 'in'), ('front', 'bus')],
        'scale': [('top', 'vdd'), ('bottom', 'gnd'), ('back', 'bus'), ('left', 'bus'), ('front', 'bus')],
        'jar': [('top', 'vdd'), ('bottom', 'gnd'), ('back', 'bus'), ('left', 'in'), ('right', 'in'), ('front', 'bus')],
        'display': [('top', 'vdd'), ('bottom', 'gnd'), ('back', 'bus')],
        'dankoder': [('top', 'vdd'), ('bottom', 'gnd'), ('left', 'bus'), ('right', 'bus'), ('front', 'bus')], 'none': [('top', 'vdd'), ('bottom', 'gnd'), ('front', 'out')]}


# the parts that mount flush on a Rolling Tray (ComponentType.mountable)
MOUNTABLE = ('puff_button', 'toggle_joint', 'numb_nug')


def component(cid, layout, look):
    logic = look not in BODY
    side = 'chip_side' if logic else f'{look}_side'
    states = (False, True) if logic or look in ('toggle', 'kill', 'button', 'nfet', 'pfet') else (False,)
    for on in states:
        front_texture(cid, look, on)
        s = '_on' if on else ''
        tex = {'front': f'fourtwenty:block/component/{cid}_front{s}', 'side': f'fourtwenty:block/component/{side}',
               'particle': f'fourtwenty:block/component/{side}'}
        for kind in LEAD:
            tex[f'lead_{kind}'] = f'fourtwenty:block/component/lead_{kind}'
        if any(kind == 'bus' for _, kind in PINS[layout]):
            tex['lead_bus'] = 'fourtwenty:block/component/ribbon'
        body = box([2, 2, 2], [14, 14, 14], {'north': '#front', 'south': '#side', 'east': '#side', 'west': '#side', 'up': '#side', 'down': '#side'})
        elements = [body] + [box(*(BUS_LEADS[face] if kind == 'bus' else LEADS[face]), f'#lead_{kind}') for face, kind in PINS[layout]]
        if look == 'resistor':
            # four bands round the body along its axis (north to south): two digits and the multiplier tinted from its value, then gold
            tex['band'] = 'fourtwenty:block/component/band'
            tex['gold'] = 'fourtwenty:block/component/band_gold'
            for i, z in enumerate((4, 6.5, 9, 12)):
                ring = box([1.9, 1.9, z], [14.1, 14.1, z + 1.5], '#band' if i < 3 else '#gold', faces=('east', 'west', 'up', 'down'))
                if i < 3:
                    for f in ring['faces'].values():
                        f['tintindex'] = i
                elements.append(ring)
        write(ASSETS / f'models/block/component/{cid}{s}.json', {'parent': 'block/block', 'textures': tex, 'elements': elements})
    # keyed on on and off only: client/ComponentModel turns the model to any of the 24 orientations; a Resinator's value only tints
    variants = {f'on={on}': {'model': f'fourtwenty:block/component/{cid}{"_on" if on == "true" and len(states) == 2 else ""}'} for on in ('false', 'true')}
    if cid in MOUNTABLE:
        # mounted on a Rolling Tray: a plate (generate_component_lab.py), the same either way; the renderer draws what moves
        variants = {f'mounted=false,{k}': v for k, v in variants.items()}
        for on in ('false', 'true'):
            variants[f'mounted=true,on={on}'] = {'model': f'fourtwenty:block/component/{cid}_mounted'}
    write(ASSETS / f'blockstates/{cid}.json', {'variants': variants})
    write(ASSETS / f'models/item/{cid}.json', {'parent': f'fourtwenty:block/component/{cid}'})


# The Eighth Ribbon is 11 pixels wide, from 3 to 14: a jacket pixel each side and the nine channels between. Its stripes are fixed in
# the world, not turned with the model: along a north-south run channel k is at x = 4 + k, along an east-west run at z = 4 + k, so runs
# line up block to block. Its core is drawn for its neighbours: straight through, a corner (two turn flat, mitred on the diagonal; the
# other two can't turn flat without the wires crossing, so they fold over, as real ribbon cable does, the far triangle open), or a
# T or cross (the straight run on top, the branch tucked under it).
JACKET = (150, 150, 156)
EDGE = (120, 120, 126)
R0, R1 = 3, 14


def stripe(k):
    """Colour at offset k (0 to 10) across the ribbon: jacket at the edges, channel k-1 between."""
    return JACKET if k <= 0 or k >= 10 else CHANNELS[k - 1]


def core_pixel(mask, u, v):
    n, e, s_, w = mask
    ns, ew = n or s_, e or w
    if (n and s_) or (ns and not ew) or not (ns or ew):
        return stripe(u)
    if (e and w) or (ew and not ns):
        return stripe(v)
    if n and w:
        return stripe(u) if v <= u else stripe(v)
    if s_ and e:
        return stripe(u) if v >= u else stripe(v)
    # a fold: the crease along the diagonal a shade darker
    crease = lambda c: tuple(int(x * 0.7) for x in c)
    if n and e:
        return None if v > u else crease(stripe(u)) if v == u else stripe(u)
    return None if v < u else crease(stripe(u)) if v == u else stripe(u)   # south and west


def mask_name(mask):
    return ''.join(c for c, on in zip('nesw', mask) if on) or 'none'


def ribbon_texture():
    ns = Image.new('RGBA', (16, 16), JACKET + (255,))
    ew = Image.new('RGBA', (16, 16), JACKET + (255,))
    for k in range(11):
        for t in range(16):
            ns.putpixel((R0 + k, t), stripe(k) + (255,))
            ew.putpixel((t, R0 + k), stripe(k) + (255,))
    save(ns, TEX / 'ribbon.png')
    save(ew, TEX / 'ribbon_ew.png')
    save(Image.new('RGBA', (16, 16), EDGE + (255,)), TEX / 'ribbon_edge.png')
    for m in range(16):
        mask = tuple(bool(m >> i & 1) for i in range(4))
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for u in range(11):
            for v in range(11):
                c = core_pixel(mask, u, v)
                if c is not None:
                    img.putpixel((R0 + u, R0 + v), c + (255,))
        save(img, TEX / f'ribbon_core_{mask_name(mask)}.png')
    brass = Image.new('RGBA', (16, 16), (196, 160, 70, 255))
    bd = ImageDraw.Draw(brass)
    bd.rectangle([0, 0, 15, 15], outline=(150, 118, 46, 255))
    for x in (3, 7, 11):
        bd.line([x, 2, x, 13], fill=(226, 196, 110, 255))
    save(brass, TEX / 'roach_clip.png')


def flat(frm, to, top):
    """A flat piece of ribbon: {top} on top and underneath, the jacket's edge round its sides."""
    e = box(frm, to, '#edge')
    for f in ('up', 'down'):
        e['faces'][f]['texture'] = top
    return e


def ribbons():
    """The Eighth Ribbon: a core drawn for its neighbours and fixed arms; the Roach Clip: a brass clip with its channel's band (tinted)."""
    base = {'edge': 'fourtwenty:block/component/ribbon_edge', 'ns': 'fourtwenty:block/component/ribbon',
            'ew': 'fourtwenty:block/component/ribbon_ew', 'particle': 'fourtwenty:block/component/ribbon'}

    def model(name, elements, extra=None):
        write(ASSETS / f'models/block/{name}.json', {'parent': 'block/block', 'render_type': 'minecraft:cutout',
              'textures': {**base, **(extra or {})}, 'elements': elements})
    arms = {
        'north': flat([R0, 7, 0], [R1, 9, R0], '#ns'), 'south': flat([R0, 7, R1], [R1, 9, 16], '#ns'),
        'east': flat([R1, 7, R0], [16, 9, R1], '#ew'), 'west': flat([0, 7, R0], [R0, 9, R1], '#ew'),
    }
    for side, e in arms.items():
        model(f'eighth_ribbon_{side}', [e])
    # up and down: the ribbon stood on edge, wide along x (a north-south run turned up) or along z (an east-west one); its stripes run up
    def upright(y0, y1, axis):
        if axis == 'x':
            e = box([R0, y0, 7], [R1, y1, 9], '#edge')
            for f in ('north', 'south'):
                e['faces'][f]['texture'] = '#ns'
        else:
            e = box([7, y0, R0], [9, y1, R1], '#edge')
            # channel k at z = 4 + k on both faces: the east face's u runs the other way, so its uv is turned round
            e['faces']['west'] = {'uv': [R0, 16 - y1, R1, 16 - y0], 'texture': '#ns'}
            e['faces']['east'] = {'uv': [R1, 16 - y1, R0, 16 - y0], 'texture': '#ns'}
        return e
    for axis in ('x', 'z'):
        for side, (y0, y1) in (('up', (9, 16)), ('down', (0, 7))):
            model(f'eighth_ribbon_{side}_{axis}', [upright(y0, y1, axis)])
        model(f'eighth_ribbon_core_vertical_{axis}', [upright(7, 9, axis)])
    # a bend from flat to upright: the flat part only from its arm to the middle, where the upright part leaves (no stub past the bend)
    for side, (f, t, top) in {'n': ([R0, 7, R0], [R1, 9, 9], '#ns'), 's': ([R0, 7, 7], [R1, 9, R1], '#ns'),
                              'e': ([7, 7, R0], [R1, 9, R1], '#ew'), 'w': ([R0, 7, R0], [9, 9, R1], '#ew')}.items():
        model(f'eighth_ribbon_core_bend_{side}', [flat(f, t, top)])
    for m in range(16):
        mask = tuple(bool(m >> i & 1) for i in range(4))
        name = mask_name(mask)
        if name in ('ne', 'sw'):
            # a fold: only the half the ribbon covers, built of one-pixel strips stepping along the crease, each with its own edge
            strips = []
            for v in range(11):
                u0, u1 = (v, 11) if name == 'ne' else (0, v + 1)
                strips.append(flat([R0 + u0, 7, R0 + v], [R0 + u1, 9, R0 + v + 1], '#core'))
            model(f'eighth_ribbon_core_{name}', strips, {'core': f'fourtwenty:block/component/ribbon_core_{name}'})
            continue
        model(f'eighth_ribbon_core_{name}', [flat([R0, 7, R0], [R1, 9, R1], '#core')], {'core': f'fourtwenty:block/component/ribbon_core_{name}'})
    band = box([3.9, 7, 3.9], [12.1, 9, 12.1], '#band', faces=('north', 'south', 'east', 'west'))
    for f in band['faces'].values():
        f['tintindex'] = 0
    clip_tex = {'clip': 'fourtwenty:block/component/lab_brass', 'band': 'fourtwenty:block/component/band',
                'particle': 'fourtwenty:block/component/lab_brass'}
    spring = box([12.6, 6.8, 6.4], [13.6, 9.2, 9.6], '#band')
    for f in spring['faces'].values():
        f['tintindex'] = 0
    write(ASSETS / 'models/block/roach_clip_core.json', {'parent': 'block/block', 'textures': clip_tex,
          'elements': [box([3.4, 9.3, 3.4], [12.6, 10.4, 12.6], '#clip'), box([3.4, 5.6, 3.4], [12.6, 6.7, 12.6], '#clip'),
                       box([12.6, 5.6, 5.4], [13.6, 6.8, 10.6], '#clip'), box([12.6, 9.2, 5.4], [13.6, 10.4, 10.6], '#clip'), spring]})
    sides = ('north', 'east', 'south', 'west')
    for cid in ('eighth_ribbon', 'roach_clip'):
        parts = [{'apply': {'model': 'fourtwenty:block/roach_clip_core'}}] if cid == 'roach_clip' else []
        if True:
            for m in range(16):
                mask = tuple(bool(m >> i & 1) for i in range(4))
                when = {side: str(on).lower() for side, on in zip(sides, mask)}
                if m == 0:
                    # nothing level with it: stood on edge (turned its run's way) if it runs up or down, else lying north to south
                    for axis in ('x', 'z'):
                        parts.append({'when': {'OR': [{**when, 'up': 'true', 'vaxis': axis}, {**when, 'down': 'true', 'vaxis': axis}]},
                                      'apply': {'model': f'fourtwenty:block/eighth_ribbon_core_vertical_{axis}'}})
                    parts.append({'when': {**when, 'up': 'false', 'down': 'false'}, 'apply': {'model': 'fourtwenty:block/eighth_ribbon_core_none'}})
                    continue
                if sum(mask) == 1:
                    # one flat arm: a bend if it also runs up or down, else a straight end
                    name = mask_name(mask)
                    parts.append({'when': {'OR': [{**when, 'up': 'true'}, {**when, 'down': 'true'}]},
                                  'apply': {'model': f'fourtwenty:block/eighth_ribbon_core_bend_{name}'}})
                    parts.append({'when': {**when, 'up': 'false', 'down': 'false'}, 'apply': {'model': f'fourtwenty:block/eighth_ribbon_core_{name}'}})
                    continue
                parts.append({'when': when, 'apply': {'model': f'fourtwenty:block/eighth_ribbon_core_{mask_name(mask)}'}})
        for side in sides:
            parts.append({'when': {side: 'true'}, 'apply': {'model': f'fourtwenty:block/eighth_ribbon_{side}'}})
        for side in ('up', 'down'):
            for axis in ('x', 'z'):
                parts.append({'when': {side: 'true', 'vaxis': axis}, 'apply': {'model': f'fourtwenty:block/eighth_ribbon_{side}_{axis}'}})
        write(ASSETS / f'blockstates/{cid}.json', {'multipart': parts})
    write(ASSETS / 'models/item/eighth_ribbon.json', {'parent': 'block/block', 'textures': base,
          'display': {'gui': {'rotation': [30, 45, 0], 'scale': [0.8, 0.8, 0.8]}}, 'elements': [flat([R0, 7, 0], [R1, 9, 16], '#ns')]})
    write(ASSETS / 'models/item/roach_clip.json', {'parent': 'fourtwenty:block/roach_clip_core'})


def twister():
    """The Twister's icon: a steel wrench with a lime hemp grip."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    steel, dark, grip, hi = (186, 190, 198, 255), (96, 100, 108, 255), (120, 180, 60, 255), (226, 230, 236, 255)
    for i in range(7):
        d.point([4 + i, 11 - i], fill=steel)
        d.point([5 + i, 11 - i], fill=dark)
    for i in range(4):
        d.point([2 + i, 13 - i], fill=grip)
        d.point([3 + i, 13 - i], fill=grip)
    d.rectangle([10, 2, 13, 5], fill=steel)
    d.rectangle([11, 3, 12, 4], fill=(0, 0, 0, 0))
    d.point([13, 2], fill=(0, 0, 0, 0))
    d.point([10, 2], fill=hi)
    save(img, ASSETS / 'textures/item/twister.png')
    write(ASSETS / 'models/item/twister.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'fourtwenty:item/twister'}})


def thread():
    """Hemp Thread: a Wick's thin sibling, the same multipart shape at a third the width."""
    # braided hemp, 64 pixels: three strands twisting along the thread, a shade between them
    n = 64
    img = Image.new('RGBA', (n, n), (0, 0, 0, 255))
    px = img.load()
    import random
    rnd = random.Random(420)
    for y in range(n):
        for x in range(n):
            strand = ((x * 2 + y) // 8) % 3
            base = [(196, 172, 118), (170, 146, 96), (214, 192, 138)][strand]
            edge = ((x * 2 + y) % 8) in (0, 7)
            k = rnd.randint(-10, 10) - (26 if edge else 0)
            px[x, y] = (max(0, base[0] + k), max(0, base[1] + k), max(0, base[2] + k), 255)
    save(img, ASSETS / 'textures/block/power/hemp_thread.png')
    # carrying current: the whole braid warmed toward lime, not a band (on a thread two pixels thick a band fills the face)
    lit = Image.blend(img, Image.new('RGBA', (n, n), (170, 240, 110, 255)), 0.32)
    save(lit, ASSETS / 'textures/block/power/hemp_thread_lit.png')
    # the knot: darker twine wound round the join, turn by turn
    knot = Image.new('RGBA', (n, n), (0, 0, 0, 255))
    kp = knot.load()
    for y in range(n):
        for x in range(n):
            turn = (y + x // 16) % 6
            base = (120, 92, 52) if turn in (0, 5) else (150, 118, 70)
            k = rnd.randint(-8, 8)
            kp[x, y] = (base[0] + k, base[1] + k, base[2] + k, 255)
    save(knot, ASSETS / 'textures/block/power/hemp_thread_knot.png')
    save(Image.blend(knot, Image.new('RGBA', (n, n), (170, 240, 110, 255)), 0.32), ASSETS / 'textures/block/power/hemp_thread_knot_lit.png')

    def tinted(boxes):
        for e in boxes:
            for f in e['faces'].values():
                f['tintindex'] = 0
        return boxes
    for lit_ in (False, True):
        s = '_lit' if lit_ else ''
        tex = {'wick': f'fourtwenty:block/power/hemp_thread{s}', 'particle': f'fourtwenty:block/power/hemp_thread{s}'}
        write(ASSETS / f'models/block/hemp_thread_core{s}.json', {'parent': 'block/block', 'textures': tex, 'elements': tinted([box([7, 7, 7], [9, 9, 9], '#wick')])})
        write(ASSETS / f'models/block/hemp_thread_arm{s}.json', {'parent': 'block/block', 'textures': tex, 'elements': tinted([box([7, 7, 0], [9, 9, 7], '#wick')])})
        write(ASSETS / f'models/block/hemp_thread_plug{s}.json', {'parent': 'block/block', 'textures': tex, 'elements': tinted([box([7, 7, 0], [9, 9, 7], '#wick')])})
    # a knot, a wrapped bead, where three or more threads (or pins) meet
    for lit_ in (False, True):
        s = '_lit' if lit_ else ''
        tex = {'wick': f'fourtwenty:block/power/hemp_thread_knot{s}', 'particle': f'fourtwenty:block/power/hemp_thread{s}'}
        write(ASSETS / f'models/block/hemp_thread_knot{s}.json', {'parent': 'block/block', 'textures': tex, 'elements': tinted([box([6.6, 6.6, 6.6], [9.4, 9.4, 9.4], '#wick')])})
    import itertools
    sides = ('north', 'east', 'south', 'west', 'up', 'down')
    parts = []
    for lit_ in ('false', 'true'):
        s = '_lit' if lit_ == 'true' else ''
        parts.append({'when': {'OR': [{**{side: 'wire|plug' for side in trio}, 'lit': lit_} for trio in itertools.combinations(sides, 3)]},
                      'apply': {'model': f'fourtwenty:block/hemp_thread_knot{s}'}})
        parts.append({'when': {'lit': lit_}, 'apply': {'model': f'fourtwenty:block/hemp_thread_core{s}'}})
        for side, rot in (('north', {}), ('east', {'y': 90}), ('south', {'y': 180}), ('west', {'y': 270}), ('up', {'x': 270}), ('down', {'x': 90})):
            parts.append({'when': {side: 'wire', 'lit': lit_}, 'apply': {'model': f'fourtwenty:block/hemp_thread_arm{s}', **rot}})
            parts.append({'when': {side: 'plug', 'lit': lit_}, 'apply': {'model': f'fourtwenty:block/hemp_thread_plug{s}', **rot}})
    write(ASSETS / 'blockstates/hemp_thread.json', {'multipart': parts})
    write(ASSETS / 'models/item/hemp_thread.json', {'parent': 'block/block', 'textures': {'wick': 'fourtwenty:block/power/hemp_thread',
          'particle': 'fourtwenty:block/power/hemp_thread'}, 'elements': tinted([box([2, 7, 7], [14, 9, 9], '#wick')])})


def data():
    ids = ['hemp_thread', 'eighth_ribbon', 'roach_clip', 'rolling_tray'] + [cid for cid, _, _, _ in TYPES]
    for cid in ids:
        write(DATA / f'fourtwenty/loot_table/blocks/{cid}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
            {'type': 'minecraft:item', 'name': f'fourtwenty:{cid}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    # recipes, simple for now (the mod's crafting balance comes later): Hemp Thread from a Hemp Wick; a Resinator and a NOT Nug from
    # basic parts; every other part by putting one of its family alone in the grid, which turns it into the next of the family
    write(DATA / 'fourtwenty/recipe/hemp_thread.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
          'ingredients': [{'item': 'fourtwenty:hemp_wick'}], 'result': {'id': 'fourtwenty:hemp_thread', 'count': 4}})
    write(DATA / 'fourtwenty/recipe/eighth_ribbon.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
          'ingredients': [{'item': 'fourtwenty:hemp_thread'}, {'item': 'fourtwenty:hemp_thread'}, {'item': 'fourtwenty:hemp_thread'},
                          {'item': 'minecraft:paper'}], 'result': {'id': 'fourtwenty:eighth_ribbon', 'count': 4}})
    write(DATA / 'fourtwenty/recipe/rolling_tray.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
          'ingredients': [{'item': 'minecraft:iron_ingot'}, {'item': 'minecraft:copper_ingot'}, {'item': 'fourtwenty:eighth_ribbon'}],
          'result': {'id': 'fourtwenty:rolling_tray', 'count': 4}})
    write(DATA / 'fourtwenty/recipe/twister.json', {'type': 'minecraft:crafting_shapeless', 'category': 'tools',
          'ingredients': [{'item': 'minecraft:iron_ingot'}, {'item': 'minecraft:iron_ingot'}, {'item': 'fourtwenty:hemp_thread'}],
          'result': {'id': 'fourtwenty:twister', 'count': 1}})
    write(DATA / 'fourtwenty/recipe/roach_clip.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
          'ingredients': [{'item': 'fourtwenty:eighth_ribbon'}, {'item': 'minecraft:copper_ingot'}, {'item': 'minecraft:iron_nugget'}],
          'result': {'id': 'fourtwenty:roach_clip', 'count': 2}})
    write(DATA / 'fourtwenty/recipe/resinator.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
          'ingredients': [{'item': 'minecraft:copper_ingot'}, {'item': 'minecraft:coal'}, {'item': 'fourtwenty:hemp_thread'}],
          'result': {'id': 'fourtwenty:resinator', 'count': 4}})
    write(DATA / 'fourtwenty/recipe/not_nug.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
          'ingredients': [{'item': 'minecraft:copper_ingot'}, {'item': 'minecraft:redstone_torch'}, {'item': 'fourtwenty:hemp_thread'}],
          'result': {'id': 'fourtwenty:not_nug', 'count': 4}})
    passives = [cid for cid, _, layout, _ in TYPES if layout == 'two']
    logic = [cid for cid, _, layout, _ in TYPES if layout != 'two']
    for family in (passives, logic):
        for i, cid in enumerate(family):
            nxt = family[(i + 1) % len(family)]
            write(DATA / f'fourtwenty/recipe/{nxt}_from_{cid}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
                  'ingredients': [{'item': f'fourtwenty:{cid}'}], 'result': {'id': f'fourtwenty:{nxt}', 'count': 1}})
    pick = DATA / 'minecraft/tags/block/mineable/pickaxe.json'
    tag = json.loads(pick.read_text(encoding='utf-8'))
    for cid in ids:
        if f'fourtwenty:{cid}' not in tag['values']:
            tag['values'].append(f'fourtwenty:{cid}')
    write(pick, tag)
    lang_path = ASSETS / 'lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    lang['block.fourtwenty.hemp_thread'] = 'Hemp Thread'
    lang['block.fourtwenty.eighth_ribbon'] = 'Eighth Ribbon'
    lang['tooltip.fourtwenty.item.eighth_ribbon'] = 'Nine signal wires in one: channels 0 to 7, striped in the resistor colour code, and the carry in grey. Joins other Ribbons, Roach Clips and the bus faces of the Puff-Pass, Pack, Sampler and Dealer Nugs.'
    lang['block.fourtwenty.roach_clip'] = 'Roach Clip'
    lang['tooltip.fourtwenty.item.roach_clip'] = 'Clips one channel of an Eighth Ribbon out to a Wick, Hemp Thread or a single pin; the rest pass straight through. Use it to step the channel (sneak to step back); its band shows which.'
    lang['message.fourtwenty.roach_clip.channel'] = 'Roach Clip: channel %s'
    lang['block.fourtwenty.rolling_tray'] = 'Rolling Tray'
    lang['tooltip.fourtwenty.item.rolling_tray'] = ('A panel board. Put a Puff Button, Toggle Joint or Numb Nug on its front and it mounts flush, no poles needed; a lamp in front of it is its lamp. '
        'Trays side by side share power: a red Wick on any feeds them all, a black their return. Each part\'s signal comes out of its own tray\'s back (a Wick or Hemp Thread, '
        'or a Ribbon for a Numb Nug), so a grid of parts is still wired one by one. Use it to put its part on a channel of the board\'s bus too (sneak to step back).')
    lang['message.fourtwenty.rolling_tray.channel'] = 'Rolling Tray: channel %s'
    lang['gui.fourtwenty.key_label.number'] = 'Dankoder number: %s'
    lang['gui.fourtwenty.key_label.number_auto'] = 'Dankoder number: its place'
    lang['gui.fourtwenty.key_label'] = 'Key Label'
    lang['gui.fourtwenty.key_label.text'] = 'Up to 4 characters on its face'
    lang['gui.fourtwenty.key_label.no_glow'] = 'No glow: plain ink'
    lang['message.fourtwenty.rolling_tray.no_channel'] = 'Rolling Tray: no channel'
    lang['item.fourtwenty.twister'] = 'Twister'
    lang['tooltip.fourtwenty.item.twister'] = 'Turns a component a quarter turn about the face you use it on; sneak to turn it back. Any way a part can point is a few twists away, and it never flips or steps what it turns.'
    lang['tooltip.fourtwenty.item.hemp_thread'] = 'A thin single wire for signals: any colour joins any component pin it touches. Rated for a hundredth of a Hemp Wick.'
    for cid, name, layout, _ in TYPES:
        lang[f'block.fourtwenty.{cid}'] = name
        lang[f'tooltip.fourtwenty.item.{cid}'] = (EXTRA.get(cid, '') + ' ' + TOOLTIPS[layout]).strip()
    lang['message.fourtwenty.component.value'] = '%s: %s'
    lang['message.fourtwenty.component.redstone'] = 'A Kill Switch follows redstone: power it to close it'
    lang['message.fourtwenty.meter.component'] = '%s · %s'
    out = '{\n' + ',\n'.join(f'  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}' for k, v in lang.items()) + '\n}\n'
    lang_path.write_text(out, encoding='utf-8')


def main():
    lead_textures()
    for cid, _, layout, look in TYPES:
        component(cid, layout, look)
    thread()
    twister()
    ribbons()
    data()
    print(f'wrote {len(TYPES)} components and Hemp Thread')


if __name__ == '__main__':
    main()
