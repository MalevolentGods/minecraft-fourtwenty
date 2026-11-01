#!/usr/bin/env python3
"""Everything data for Boundless Inspiration and the powered machines: models, blockstates, textures, recipes, loot and tags.

    python3 tools/generate_power.py

A tier reads the same on every block: Puff is hemp-brown and green, Toke copper and teal, Rip steel and violet. Models are boxes from
tools/generate_processing.py, which refuses overlapping coplanar faces.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

from generate_processing import ASSETS, ROOT, block_model, box

DATA = ROOT / 'src/main/resources/data'
TEX = ASSETS / 'textures/block/power'
TIERS = {
    'puff': {'body': (138, 100, 62), 'accent': (96, 196, 72)},
    'toke': {'body': (172, 104, 60), 'accent': (60, 196, 180)},
    'rip': {'body': (118, 124, 138), 'accent': (168, 100, 232)},
    'blaze': {'body': (150, 112, 44), 'accent': (255, 140, 40)},
}
WICKS = {'hemp_wick': 'puff', 'rolled_wick': 'toke', 'glass_wick': 'rip', 'gold_wick': 'blaze'}
PLOTS = {'closet_grow': 'puff', 'grow_tent': 'puff', 'hydro_bed': 'toke', 'aero_pod': 'rip', 'cosmic_kush_pod': 'blaze'}
HOOKAHS = ('double_hose', 'big_bertha', 'grand_hookah')       # the Hookah upgraded: their models come from tools/generate_hookah_model.py
MACHINES = {'snip_station': 'puff', 'bud_barber': 'toke', 'trim_reaper': 'rip', 'dry_box': 'puff', 'cure_closet': 'toke', 'the_humidor': 'rip',
            'pheno_chamber': 'toke', 'pheno_lab': 'rip', 'pheno_cathedral': 'blaze'}
FACINGS = (('north', 0), ('east', 90), ('south', 180), ('west', 270))
SIZE = 32


def shade(colour, seed, spread=6):
    rng = random.Random(seed)
    img = Image.new('RGBA', (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            d = rng.randint(-spread, spread)
            px[x, y] = tuple(max(0, min(255, c + d)) for c in colour) + (255,)
    return img


def plate(tier):
    """A body panel: riveted edges."""
    t = TIERS[tier]
    img = shade(t['body'], tier)
    d = ImageDraw.Draw(img)
    dark = tuple(int(c * 0.6) for c in t['body']) + (255,)
    d.rectangle([0, 0, SIZE - 1, SIZE - 1], outline=dark)
    for x, y in ((2, 2), (29, 2), (2, 29), (29, 29)):
        d.rectangle([x, y, x + 1, y + 1], fill=tuple(min(255, c + 60) for c in t['body']) + (255,))
    return img


def tinted(tier, lit):
    t = TIERS[tier]
    return t['accent'] if lit else tuple(int(c * 0.45) for c in t['accent'])


def glow(tier, lit):
    """A plain panel of the accent colour: tanks, bowls and lit trim."""
    return shade(tinted(tier, lit), tier + str(lit), 12)


def wick(tier, lit, offset=0):
    base = {'puff': (176, 150, 104), 'toke': (214, 200, 170), 'rip': (190, 214, 222), 'blaze': (226, 184, 92)}[tier]
    img = shade(base, 'w' + tier)
    d = ImageDraw.Draw(img)
    for i in range(-SIZE - 6, SIZE + 6, 6):
        d.line([(i + offset, 0), (i + offset + SIZE, SIZE)], fill=(tinted(tier, lit) if lit else tuple(int(c * 0.72) for c in base)) + (255,), width=2)
    return img


def strip(frames, path, frametime):
    """A vertical sheet of frames and its .mcmeta: Minecraft animates it by itself."""
    sheet = Image.new('RGBA', (SIZE, SIZE * len(frames)))
    for i, frame in enumerate(frames):
        sheet.paste(frame, (0, i * SIZE))
    sheet.save(path)
    Path(str(path) + '.mcmeta').write_text(json.dumps({'animation': {'frametime': frametime, 'interpolate': True}}) + '\n')


def pulse(tier, frame, frames):
    """The accent colour breathing brighter and dimmer."""
    k = 0.72 + 0.28 * (0.5 + 0.5 * math.cos(2 * math.pi * frame / frames))
    return shade(tuple(min(255, int(c * k)) for c in tinted(tier, True)), tier + 'lit', 12)


def front(name, lit, frame=0):
    tier = MACHINES[name]
    img = plate(tier)
    d = ImageDraw.Draw(img)
    a = tinted(tier, lit) + (255,)
    ink = (24, 26, 30, 255)
    d.rectangle([5, 5, 26, 26], fill=(34, 36, 42, 255), outline=ink)
    if name in ('snip_station', 'bud_barber', 'trim_reaper'):     # blades
        n = {'snip_station': 1, 'bud_barber': 2, 'trim_reaper': 3}[name]
        for i in range(n):
            y = (9 + i * 7 if n > 1 else 15) + (frame % 2 if lit else 0)
            spread = 3 + frame if lit else 3
            d.line([(8, y), (23, y + spread)], fill=(214, 220, 226, 255), width=2)
            d.line([(8, y + spread), (23, y)], fill=a, width=1)
    elif name.startswith('pheno'):                                 # a double helix, turning while it works
        for y in range(7, 25):
            wave = math.sin((y + frame * 2) / 3)
            left, right = int(15.5 + wave * 6), int(15.5 - wave * 6)
            d.point([(left, y), (left + 1, y)], fill=a)
            d.point([(right, y), (right + 1, y)], fill=(214, 220, 226, 255))
            if y % 3 == 0:
                d.line([(min(left, right) + 2, y), (max(left, right) - 1, y)], fill=(90, 96, 104, 255))
    else:                                                          # slats and hanging buds
        n = {'dry_box': 3, 'cure_closet': 4, 'the_humidor': 5}[name]
        for i in range(n):
            x = 8 + i * (16 // n)
            d.line([(x + 1, 7), (x + 1, 24)], fill=(90, 76, 58, 255))
            sway = (frame + i) % 4 // 2 if lit else 0
            d.ellipse([x - 1 + sway, 12 + i % 2 * 5, x + 3 + sway, 16 + i % 2 * 5], fill=a)
    return img


def variants(name, models):
    """models(lit) -> model name; a block with FACING and LIT."""
    return {'variants': {f'facing={f},lit={str(lit).lower()}': {'model': f'fourtwenty:block/{models(lit)}', **({'y': y} if y else {})}
                         for f, y in FACINGS for lit in (False, True)}}


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')


def item_model(name, parent=None):
    write(ASSETS / f'models/item/{name}.json', {'parent': parent or f'fourtwenty:block/{name}'})


def model(name, elements, textures, particle):
    block_model(name, textures, elements, particle)


def lit_pair(name, elements, textures):
    """A block's model, off and on: `glow` is the texture that lights up."""
    for lit in (False, True):
        tex = {k: (v[lit] if isinstance(v, dict) else v) for k, v in textures.items()}
        model(f'{name}_lit' if lit else name, elements, tex, '#body')
    write(ASSETS / f'blockstates/{name}.json', variants(name, lambda lit: f'{name}_lit' if lit else name))
    item_model(name)


def ensure(path):
    path.mkdir(parents=True, exist_ok=True)
    return path


def textures():
    TEX.mkdir(parents=True, exist_ok=True)
    for tier in TIERS:
        plate(tier).save(TEX / f'{tier}_body.png')
        for lit in (False, True):
            suffix = '_lit' if lit else ''
            if lit:
                strip([pulse(tier, i, 8) for i in range(8)], TEX / f'{tier}_glow_lit.png', 3)
                strip([wick(tier, True, i) for i in range(6)], TEX / f'{tier}_wick_lit.png', 2)
            else:
                glow(tier, False).save(TEX / f'{tier}_glow.png')
                wick(tier, False).save(TEX / f'{tier}_wick.png')
    for name in MACHINES:
        front(name, False).save(TEX / f'{name}_front.png')
        strip([front(name, True, i) for i in range(4)], TEX / f'{name}_front_lit.png', 3)
    bot = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(bot)
    d.rectangle([0, 0, 23, 11], fill=(196, 150, 44, 255))                 # the body, in brass
    for x in range(1, 24, 4):
        d.rectangle([x, 0, x + 1, 11], fill=(32, 34, 38, 255))            # stripes
    d.rectangle([12, 5, 15, 8], fill=(123, 255, 90, 255))                 # the eye, on the front
    d.rectangle([24, 0, 29, 2], fill=(150, 156, 164, 255))                # the stinger
    d.rectangle([0, 12, 10, 22], fill=(200, 236, 226, 130))               # glass wings
    bot.save(ensure(ASSETS / 'textures/entity') / 'buzz_bot.png')
    icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(icon)
    d.ellipse([3, 6, 13, 13], fill=(196, 150, 44, 255), outline=(24, 26, 30, 255))
    for x in (6, 9):
        d.line([(x, 7), (x, 12)], fill=(32, 34, 38, 255))
    d.point((12, 9), fill=(123, 255, 90, 255))
    d.ellipse([3, 2, 8, 6], fill=(200, 236, 226, 170))
    d.ellipse([8, 2, 13, 6], fill=(200, 236, 226, 170))
    (ASSETS / 'textures/item').mkdir(exist_ok=True)
    icon.save(ASSETS / 'textures/item/buzz_bot.png')
    write(ASSETS / 'models/item/buzz_bot.json', {'parent': 'item/generated', 'textures': {'layer0': 'fourtwenty:item/buzz_bot'}})
    glass = shade((176, 214, 196), 'glass', 8)
    ImageDraw.Draw(glass).line([(6, 3), (6, 28)], fill=(232, 244, 238, 255), width=2)
    glass.save(TEX / 'glass.png')
    ash = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(ash)
    rng = random.Random(420)
    d.ellipse([2, 7, 13, 14], fill=(84, 84, 88, 255))
    for _ in range(26):
        ash.putpixel((rng.randint(3, 12), rng.randint(8, 13)), (rng.choice((64, 120, 150)),) * 3 + (255,))
    (ASSETS / 'textures/item').mkdir(exist_ok=True)
    ash.save(ASSETS / 'textures/item/ash.png')
    write(ASSETS / 'models/item/ash.json', {'parent': 'item/generated', 'textures': {'layer0': 'fourtwenty:item/ash'}})


def t(tier, what, lit=False):
    return f'fourtwenty:block/power/{tier}_{what}{"_lit" if lit else ""}'


def both(tier, what):
    return {False: t(tier, what), True: t(tier, what, True)}


def wicks():
    for name, tier in WICKS.items():
        for lit in (False, True):
            suffix = '_lit' if lit else ''
            tex = {'wick': t(tier, 'wick', lit)}
            model(f'{name}_core{suffix}', [box([6, 6, 6], [10, 10, 10], 'wick')], tex, '#wick')
            model(f'{name}_arm{suffix}', [box([6, 6, 0], [10, 10, 6], 'wick')], tex, '#wick')
            # The wire tapers as it arrives; the block draws the rest (tools/generate_ports.py).
            model(f'{name}_plug{suffix}', [box([6, 6, 3], [10, 10, 6], 'wick'), box([6.75, 6.75, 0], [9.25, 9.25, 3], 'wick')], tex, '#wick')
        parts = []
        for lit in (False, True):
            suffix = '_lit' if lit else ''
            flag = str(lit).lower()
            parts.append({'when': {'lit': flag}, 'apply': {'model': f'fourtwenty:block/{name}_core{suffix}'}})
            for side, rot in (('north', {}), ('east', {'y': 90}), ('south', {'y': 180}), ('west', {'y': 270}), ('up', {'x': 270}), ('down', {'x': 90})):
                parts.append({'when': {side: 'wire', 'lit': flag}, 'apply': {'model': f'fourtwenty:block/{name}_arm{suffix}', **rot}})
                parts.append({'when': {side: 'plug', 'lit': flag}, 'apply': {'model': f'fourtwenty:block/{name}_plug{suffix}', **rot}})
        write(ASSETS / f'blockstates/{name}.json', {'multipart': parts})
        model(name + '_item', [box([2, 6, 6], [14, 10, 10], 'wick')], {'wick': t(tier, 'wick')}, '#wick')
        item_model(name, f'fourtwenty:block/{name}_item')


def stashes():
    lit_pair('stash_jar', [box([4, 0, 4], [12, 10, 12], 'tank'), box([5, 10, 5], [11, 12, 11], 'body')], {'body': t('puff', 'body'), 'tank': both('puff', 'glow')})
    lit_pair('stash_box', [box([1, 0, 1], [15, 11, 15], 'body'), box([1, 11, 1], [15, 14, 15], 'tank'), box([7, 8, 0.5], [9, 12, 1], 'body')],
             {'body': t('toke', 'body'), 'tank': both('toke', 'glow')})
    lit_pair('mother_stash', [box([0, 0, 0], [16, 4, 16], 'body'), box([0, 4, 0], [16, 12, 16], 'tank'), box([0, 12, 0], [16, 16, 16], 'body')],
             {'body': t('rip', 'body'), 'tank': both('rip', 'glow')})
    states = {}
    lit_pair('the_vault', [box([0, 0, 0], [16, 16, 16], 'body', north='tank'), box([5, 5, -0.5], [11, 11, 0], 'plate')],
             {'body': t('blaze', 'body'), 'tank': both('blaze', 'glow'), 'plate': t('rip', 'body')})
    for tier in ('toke', 'rip', 'blaze'):
        for lit in (False, True):
            suffix = '_lit' if lit else ''
            model(f'hit_box_{tier}{suffix}', [box([0, 0, 0], [16, 16, 16], 'body', north='plug')],
                  {'body': t(tier, 'body'), 'plug': t(tier, 'glow', lit)}, '#body')
            for f, y in FACINGS:
                states[f'facing={f},lit={str(lit).lower()},tier={tier}'] = {'model': f'fourtwenty:block/hit_box_{tier}{suffix}', **({'y': y} if y else {})}
    write(ASSETS / 'blockstates/hit_box.json', {'variants': states})
    item_model('hit_box', 'fourtwenty:block/hit_box_toke')


def machines():
    common = lambda tier, name: {'body': t(tier, 'body'), 'front': {False: f'fourtwenty:block/power/{name}_front', True: f'fourtwenty:block/power/{name}_front_lit'}, 'glow': both(tier, 'glow')}
    full = lambda top: box([0, 0, 0], [16, top, 16], 'body', north='front')
    features = {
        'snip_station': [full(12), box([12, 12, 6], [15, 16, 10], 'body'), box([4, 13, 7], [12, 14, 9], 'glow')],
        'bud_barber': [full(14), box([2, 14, 3], [14, 16, 6], 'glow'), box([2, 14, 10], [14, 16, 13], 'glow')],
        'trim_reaper': [full(13), box([2, 13, 2], [14, 16, 14], 'glow')],
        'dry_box': [full(16)],
        'cure_closet': [box([1, 0, 1], [15, 16, 15], 'body', north='front'), box([12, 6, 0.5], [13, 10, 1], 'glow')],
        'the_humidor': [box([0, 2, 0], [16, 16, 16], 'body', north='front'), *[box([x, 0, z], [x + 2, 2, z + 2], 'glow') for x in (1, 13) for z in (1, 13)]],
        'pheno_chamber': [full(14), *[box([x, 14, z], [x + 3, 16, z + 3], 'glow') for x in (2, 11) for z in (2, 11)]],
        'pheno_lab': [full(13), box([2, 13, 2], [14, 16, 5], 'glow'), box([2, 13, 11], [14, 16, 14], 'glow')],
        'pheno_cathedral': [full(12), *[box([x, 12, z], [x + 2, 16, z + 2], 'glow') for x in (1, 13) for z in (1, 13)], box([1, 12, 7], [3, 15, 9], 'glow'), box([13, 12, 7], [15, 15, 9], 'glow')],
    }
    for name, tier in MACHINES.items():
        lit_pair(name, features[name], common(tier, name))


def soil(name, colour, speck, seed):
    img = shade(colour, seed, 10)
    px = img.load()
    rng = random.Random(seed)
    for _ in range(90):
        px[rng.randint(0, SIZE - 1), rng.randint(0, SIZE - 1)] = speck + (255,)
    img.save(TEX / f'{name}.png')


def plots():
    soil('soil', (70, 48, 30), (104, 76, 46), 'soil')
    soil('rockwool', (196, 190, 170), (232, 228, 212), 'wool')
    soil('mist', (170, 214, 226), (236, 248, 252), 'mist')
    soil('stardust', (46, 24, 84), (255, 214, 96), 'stars')
    soil('board', (24, 44, 34), (206, 226, 210), 'board')
    tops = {'closet_grow': 'soil', 'grow_tent': 'soil', 'hydro_bed': 'rockwool', 'aero_pod': 'mist', 'cosmic_kush_pod': 'stardust'}
    for name, tier in PLOTS.items():
        rail = [box([0, 13, 0], [16, 15, 1], 'glow'), box([0, 13, 15], [16, 15, 16], 'glow'), box([0, 13, 1], [1, 15, 15], 'glow'), box([15, 13, 1], [16, 15, 15], 'glow')]
        elements = [box([0, 0, 0], [16, 13, 16], 'body'), box([1, 13, 1], [15, 15, 15], 'top')] + rail
        lit_pair(name, elements, {'body': t(tier, 'body'), 'glow': both(tier, 'glow'), 'top': f'fourtwenty:block/power/{tops[name]}'})
    lit_pair('think_tank', [box([0, 0, 0], [16, 8, 16], 'body'), box([1, 8, 13], [15, 16, 15], 'board'), box([2, 8, 2], [8, 11, 8], 'glow')],
             {'body': t('puff', 'body'), 'glow': both('puff', 'glow'), 'board': 'fourtwenty:block/power/board'})


def recipes():
    item = lambda i: {'item': i if ':' in i else f'minecraft:{i}'}
    book = {
        'hemp_wick': (6, ['SRS'], {'S': 'string', 'R': 'fourtwenty:rough_bud'}),
        'rolled_wick': (6, ['WWW', 'PCP', 'WWW'], {'W': 'fourtwenty:hemp_wick', 'P': 'paper', 'C': 'copper_ingot'}),
        'glass_wick': (4, ['WGW', 'GDG', 'WGW'], {'W': 'fourtwenty:rolled_wick', 'G': 'glass_pane', 'D': 'glowstone_dust'}),
        'gold_wick': (4, ['WGW', 'GBG', 'WGW'], {'W': 'fourtwenty:glass_wick', 'G': 'gold_ingot', 'B': 'blaze_powder'}),
        'grand_hookah': (1, ['DHD', 'WHW'], {'D': 'diamond_block', 'H': 'fourtwenty:big_bertha', 'W': 'fourtwenty:gold_wick'}),
        'the_vault': (1, ['GMG', 'MNM', 'GMG'], {'G': 'gold_block', 'M': 'fourtwenty:mother_stash', 'N': 'diamond_block'}),
        'closet_grow': (1, ['PRP', 'DDD'], {'P': 'oak_planks', 'R': 'fourtwenty:rough_bud', 'D': 'dirt'}),
        'grow_tent': (1, ['GLG', 'WCW'], {'G': 'green_wool', 'L': 'glowstone', 'W': 'fourtwenty:hemp_wick', 'C': 'fourtwenty:closet_grow'}),
        'hydro_bed': (1, ['BCB', 'WHW'], {'B': 'water_bucket', 'C': 'copper_block', 'W': 'fourtwenty:rolled_wick', 'H': 'fourtwenty:grow_tent'}),
        'aero_pod': (1, ['GDG', 'WHW'], {'G': 'glass', 'D': 'diamond', 'W': 'fourtwenty:glass_wick', 'H': 'fourtwenty:hydro_bed'}),
        'cosmic_kush_pod': (1, ['EDE', 'WHW'], {'E': 'end_stone', 'D': 'diamond_block', 'W': 'fourtwenty:gold_wick', 'H': 'fourtwenty:aero_pod'}),
        'think_tank': (1, ['BLB', 'IWI'], {'B': 'book', 'L': 'lectern', 'I': 'iron_ingot', 'W': 'fourtwenty:hemp_wick'}),
        'double_hose': (1, ['GHG', 'WWW'], {'G': 'gold_ingot', 'H': 'fourtwenty:weed_bong', 'W': 'fourtwenty:rolled_wick'}),
        'big_bertha': (1, ['DHD', 'WHW'], {'D': 'diamond', 'H': 'fourtwenty:double_hose', 'W': 'fourtwenty:glass_wick'}),
        'stash_jar': (1, ['GIG', 'GBG', 'GGG'], {'G': 'glass', 'I': 'iron_ingot', 'B': 'fourtwenty:weed_bud'}),
        'stash_box': (1, ['IJI', 'JCJ', 'IJI'], {'I': 'iron_ingot', 'J': 'fourtwenty:stash_jar', 'C': 'copper_block'}),
        'mother_stash': (1, ['GBG', 'BDB', 'GBG'], {'G': 'gold_block', 'B': 'fourtwenty:stash_box', 'D': 'diamond_block'}),
        'hit_box': (1, ['IWI', 'RCR', 'IWI'], {'I': 'iron_ingot', 'W': 'fourtwenty:rolled_wick', 'R': 'redstone', 'C': 'copper_block'}),
        'snip_station': (1, ['ISI', 'RTR', 'IWI'], {'I': 'iron_ingot', 'S': 'shears', 'R': 'redstone', 'T': 'fourtwenty:trim_tray', 'W': 'fourtwenty:hemp_wick'}),
        'bud_barber': (1, ['CSC', 'RTR', 'CWC'], {'C': 'copper_ingot', 'S': 'shears', 'R': 'redstone', 'T': 'fourtwenty:snip_station', 'W': 'fourtwenty:rolled_wick'}),
        'trim_reaper': (1, ['DSD', 'RTR', 'DWD'], {'D': 'diamond', 'S': 'shears', 'R': 'redstone', 'T': 'fourtwenty:bud_barber', 'W': 'fourtwenty:glass_wick'}),
        'dry_box': (1, ['IVI', 'RTR', 'IWI'], {'I': 'iron_ingot', 'V': 'iron_bars', 'R': 'redstone', 'T': 'fourtwenty:curing_rack', 'W': 'fourtwenty:hemp_wick'}),
        'cure_closet': (1, ['CVC', 'RTR', 'CWC'], {'C': 'copper_ingot', 'V': 'iron_bars', 'R': 'redstone', 'T': 'fourtwenty:dry_box', 'W': 'fourtwenty:rolled_wick'}),
        'the_humidor': (1, ['DVD', 'RTR', 'DWD'], {'D': 'diamond', 'V': 'glass', 'R': 'redstone', 'T': 'fourtwenty:cure_closet', 'W': 'fourtwenty:glass_wick'}),
        'buzz_bot': (1, ['WHW', 'ICI'], {'W': 'fourtwenty:glass_wick', 'H': 'honeycomb', 'I': 'iron_ingot', 'C': 'copper_block'}),
        'pheno_chamber': (1, ['CGC', 'RTR', 'CWC'], {'C': 'copper_ingot', 'G': 'glass', 'R': 'redstone', 'T': 'flower_pot', 'W': 'fourtwenty:rolled_wick'}),
        'pheno_lab': (1, ['DGD', 'RTR', 'DWD'], {'D': 'diamond', 'G': 'glass', 'R': 'redstone', 'T': 'fourtwenty:pheno_chamber', 'W': 'fourtwenty:glass_wick'}),
        'pheno_cathedral': (1, ['BGB', 'RTR', 'BWB'], {'B': 'diamond_block', 'G': 'gold_block', 'R': 'redstone_block', 'T': 'fourtwenty:pheno_lab', 'W': 'fourtwenty:gold_wick'}),
    }
    for name, (count, pattern, key) in book.items():
        write(DATA / f'fourtwenty/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
                                                       'key': {k: item(v) for k, v in key.items()},
                                                       'result': {'id': f'fourtwenty:{name}', 'count': count}, 'show_notification': True})


def data():
    blocks = list(WICKS) + list(HOOKAHS) + ['stash_jar', 'stash_box', 'mother_stash', 'the_vault', 'hit_box', 'think_tank'] + list(MACHINES) + list(PLOTS)
    for name in blocks:
        write(DATA / f'fourtwenty/loot_table/blocks/{name}.json', {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fourtwenty:{name}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    joined = [n for n in blocks if n not in WICKS and n != 'closet_grow'] + ['weed_bong', 'terpinator', 'terpinator_lab', 'inspiration_infuser', 'inspiration_infuser_lab']
    write(DATA / 'fourtwenty/tags/block/bi_blocks.json', {'replace': False, 'values': [f'fourtwenty:{n}' for n in joined]})
    path = DATA / 'minecraft/tags/block/mineable/pickaxe.json'
    tag = json.loads(path.read_text()) if path.exists() else {'replace': False, 'values': []}
    tag['values'] = [v for v in tag['values'] if v != 'fourtwenty:single_hose_hookah']
    for n in blocks:
        if n not in WICKS and f'fourtwenty:{n}' not in tag['values']:
            tag['values'].append(f'fourtwenty:{n}')
    write(path, tag)


if __name__ == '__main__':
    textures()
    wicks()
    stashes()
    plots()
    machines()
    recipes()
    data()
    print('ok')
