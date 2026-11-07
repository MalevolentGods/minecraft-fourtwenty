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
]

TOOLTIPS = {
    'two': 'Two pins, back and front: any Wick or Hemp Thread on either end joins it.',
    'one': 'Input at the back, output in front; VDD on top, GND underneath. Logic at the supply it gets: 30% and 70% of it are its thresholds.',
    'twoin': 'Inputs on the left and right, output in front; VDD on top, GND underneath. Logic at the supply it gets: 30% and 70% of it are its thresholds.',
    'none': 'Output in front; VDD on top, GND underneath.',
}
EXTRA = {
    'resinator': 'A resistor, rated 10 W: past that it burns out in smoke. Use it to step its value (sneak to step back).',
    'kief_cap': 'A capacitor, in Garcias. Use it to step its value (sneak to step back).',
    'spliff_coil': 'An inductor, in Marleys. Use it to step its value (sneak to step back).',
    'one_hitter': 'A diode: current flows only from its back to its banded front.',
    'zen_hitter': 'A Zener diode: wired banded end to +, it holds its voltage. Use it to step its voltage.',
    'toggle_joint': 'A switch: use it to flip it.',
    'kill_switch': 'A switch closed while it has a redstone signal.',
    'puff_button': 'A button: closed for a second when used.',
    'tri_state_nug': 'Left is A, right is enable: passes A while enabled, lets go of its output otherwise.',
    'latch_nug': 'An SR latch: left sets it, right resets it.',
    'flip_flop_nug': 'A D flip-flop: left is D, right is the clock; it takes D on each rising edge.',
    'ticker': 'A clock: a square wave. Use it to step its period (sneak to step back).',
}

BODY = {'resistor': (196, 170, 120), 'capacitor': (52, 92, 160), 'coil': (70, 54, 40), 'diode': (30, 30, 34), 'zener': (30, 30, 34),
        'toggle': (110, 112, 118), 'kill': (120, 40, 36), 'button': (110, 112, 118)}
CHIP = (26, 30, 30)
LEAD = {'in': (190, 192, 198), 'out': (220, 176, 60), 'vdd': (200, 40, 40), 'gnd': (24, 24, 26)}

# a 3x5 pixel font for the Nug labels
FONT = {
    'A': ['010', '101', '111', '101', '101'], 'B': ['110', '101', '110', '101', '110'], 'D': ['110', '101', '101', '101', '110'],
    'F': ['111', '100', '110', '100', '100'], 'I': ['111', '010', '010', '010', '111'], 'K': ['101', '110', '100', '110', '101'],
    'L': ['100', '100', '100', '100', '111'], 'N': ['101', '111', '111', '111', '101'], 'O': ['111', '101', '101', '101', '111'],
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
            for x, c in ((3, (120, 60, 20)), (6, (20, 20, 20)), (9, (220, 40, 40)), (12, (210, 170, 40))):
                d.rectangle([x, 1, x + 1, 14], fill=c + (255,))
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
    d.rectangle([7, 11, 8, 12], fill=((120, 255, 90) if on else (40, 70, 40)) + (255,))
    save(img, TEX / f'{cid}_front{"_on" if on else ""}.png')


def lead_textures():
    for name, c in LEAD.items():
        save(Image.new('RGBA', (16, 16), c + (255,)), TEX / f'lead_{name}.png')
    side_texture(CHIP, 'chip_side')
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
        'twoin': [('top', 'vdd'), ('bottom', 'gnd'), ('front', 'out'), ('left', 'in'), ('right', 'in')], 'none': [('top', 'vdd'), ('bottom', 'gnd'), ('front', 'out')]}


def component(cid, layout, look):
    logic = look not in BODY
    side = 'chip_side' if logic else f'{look}_side'
    states = (False, True) if logic or look in ('toggle', 'kill', 'button') else (False,)
    for on in states:
        front_texture(cid, look, on)
        s = '_on' if on else ''
        tex = {'front': f'fourtwenty:block/component/{cid}_front{s}', 'side': f'fourtwenty:block/component/{side}',
               'particle': f'fourtwenty:block/component/{side}'}
        for kind in LEAD:
            tex[f'lead_{kind}'] = f'fourtwenty:block/component/lead_{kind}'
        body = box([2, 2, 2], [14, 14, 14], {'north': '#front', 'south': '#side', 'east': '#side', 'west': '#side', 'up': '#side', 'down': '#side'})
        elements = [body] + [box(LEADS[face][0], LEADS[face][1], f'#lead_{kind}') for face, kind in PINS[layout]]
        write(ASSETS / f'models/block/component/{cid}{s}.json', {'parent': 'block/block', 'textures': tex, 'elements': elements})
    variants = {}
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for on in ('false', 'true'):
            model = f'fourtwenty:block/component/{cid}{"_on" if on == "true" and len(states) == 2 else ""}'
            variants[f'facing={facing},on={on}'] = {'model': model, **({'y': y} if y else {})}
    write(ASSETS / f'blockstates/{cid}.json', {'variants': variants})
    write(ASSETS / f'models/item/{cid}.json', {'parent': f'fourtwenty:block/component/{cid}'})


def thread():
    """Hemp Thread: a Wick's thin sibling, the same multipart shape at a third the width."""
    img = Image.new('RGBA', (16, 16), (214, 196, 150, 255))
    d = ImageDraw.Draw(img)
    for y in range(0, 16, 3):
        d.line([0, y, 15, y], fill=(184, 164, 118, 255))
    save(img, ASSETS / 'textures/block/power/hemp_thread.png')
    lit = img.copy()
    ImageDraw.Draw(lit).line([0, 8, 15, 8], fill=(150, 255, 120, 255))
    save(lit, ASSETS / 'textures/block/power/hemp_thread_lit.png')

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
    parts = []
    for lit_ in ('false', 'true'):
        s = '_lit' if lit_ == 'true' else ''
        parts.append({'when': {'lit': lit_}, 'apply': {'model': f'fourtwenty:block/hemp_thread_core{s}'}})
        for side, rot in (('north', {}), ('east', {'y': 90}), ('south', {'y': 180}), ('west', {'y': 270}), ('up', {'x': 270}), ('down', {'x': 90})):
            parts.append({'when': {side: 'wire', 'lit': lit_}, 'apply': {'model': f'fourtwenty:block/hemp_thread_arm{s}', **rot}})
            parts.append({'when': {side: 'plug', 'lit': lit_}, 'apply': {'model': f'fourtwenty:block/hemp_thread_plug{s}', **rot}})
    write(ASSETS / 'blockstates/hemp_thread.json', {'multipart': parts})
    write(ASSETS / 'models/item/hemp_thread.json', {'parent': 'block/block', 'textures': {'wick': 'fourtwenty:block/power/hemp_thread',
          'particle': 'fourtwenty:block/power/hemp_thread'}, 'elements': tinted([box([2, 7, 7], [14, 9, 9], '#wick')])})


def data():
    ids = ['hemp_thread'] + [cid for cid, _, _, _ in TYPES]
    for cid in ids:
        write(DATA / f'fourtwenty/loot_table/blocks/{cid}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
            {'type': 'minecraft:item', 'name': f'fourtwenty:{cid}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    # recipes, simple for now (the mod's crafting balance comes later): Hemp Thread from a Hemp Wick; a Resinator and a NOT Nug from
    # basic parts; every other part by putting one of its family alone in the grid, which turns it into the next of the family
    write(DATA / 'fourtwenty/recipe/hemp_thread.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
          'ingredients': [{'item': 'fourtwenty:hemp_wick'}], 'result': {'id': 'fourtwenty:hemp_thread', 'count': 4}})
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
    data()
    print(f'wrote {len(TYPES)} components and Hemp Thread')


if __name__ == '__main__':
    main()
