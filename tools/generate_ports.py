#!/usr/bin/env python3
"""Where a Wick meets a block. Each block has its own fitting for each side, shaped round what's on that side.

    python3 tools/generate_ports.py

Writes models/block/port/<block>_<side>[_lit].json. client/PortModel.java draws the ones whose side has a Wick beside it, turned with the block, so
sides here are as for a block facing north. A Wick tapers as it arrives; from the block's edge a thin lead, straight or angled, runs to a gland on
the body. A side with nothing to meet (the crop on top of a plot) gets an empty model.
"""
import copy
import json

from generate_processing import ASSETS, box, coplanar

SIDES = ('north', 'east', 'south', 'west')
FACE_TURN = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north', 'up': 'up', 'down': 'down'}
TIER_TEX = {'puff': 'puff', 'toke': 'toke', 'rip': 'rip', 'blaze': 'blaze'}
METALS = {'weed_bong': ('brass', 'puff'), 'double_hose': ('copper', 'toke'), 'big_bertha': ('steel', 'rip'), 'grand_hookah': ('gold', 'blaze')}


def angled(x, y, z, length, angle, axis='x', tex='lead', w=1):
    """A thin lead running in along +z from (x, y, z), tilted by angle (22.5 or 45) round axis: positive drops it."""
    e = box([x - w, y - w, z], [x + w, y + w, z + length], tex)
    e['rotation'] = {'origin': [x, y, z], 'axis': axis, 'angle': -angle}
    return e


def side_flange(tex='plate', ring='collar'):
    """On a full face: a flange standing proud, with the live ring in it."""
    return [box([5, 5, -0.5], [11, 11, 0], tex), box([6.5, 6.5, -0.75], [9.5, 9.5, -0.5], ring)]


def small_ring():
    """On a face with art on it: only the ring, so the art stays readable."""
    return [box([6.25, 6.25, -0.5], [9.75, 9.75, 0], 'collar')]


def up_flange(y=16):
    return [box([5, y, 5], [11, y + 0.5, 11], 'plate'), box([6.5, y + 0.5, 6.5], [9.5, y + 0.75, 9.5], 'collar')]


def down_flange():
    return [box([5, -0.5, 5], [11, 0, 11], 'plate'), box([6.5, -0.75, 6.5], [9.5, -0.5, 9.5], 'collar')]


def stem_up(top, y=16):
    """Down from the block's top edge to a body that stops at {@code top}."""
    return [box([7, top + 0.75, 7], [9, y, 9], 'lead'), box([6, top, 6], [10, top + 0.75, 10], 'plate')]


def full(front=None, up=None, down=None):
    """A block that fills its space: flanges, with a quieter ring on the front."""
    sides = {s: side_flange() for s in SIDES}
    sides['north'] = front if front is not None else small_ring()
    sides['up'] = up if up is not None else up_flange()
    sides['down'] = down if down is not None else down_flange()
    return sides


def hookah():
    """A lead dips from wire height to a gland on the cabinet, below its top. On top, a yoke splits round the bowl to the tray."""
    sides = {}
    side = [angled(8, 8, 0, 3.8, 22.5), box([6, 5, 3.25], [10, 8, 3.75], 'plate')]
    for s in SIDES:
        sides[s] = side
    yoke = []
    for x, angle in ((5.9, 45), (10.1, -45)):
        e = box([x - 0.5, 13.3, 7.5], [x + 0.5, 16.4, 8.5], 'lead')
        e['rotation'] = {'origin': [8, 16, 8], 'axis': 'z', 'angle': angle}
        yoke.append(e)
    sides['up'] = yoke + [box([7, 15.75, 7], [9, 16, 9], 'plate')]
    sides['down'] = [box([6, -0.25, 6], [10, 0, 10], 'plate')]
    return sides


def jar():
    """A lead runs in to a gland on the glass, with a strap round it to hold it; on top it comes down onto the lid."""
    side = [box([7, 7, 0], [9, 9, 3.75], 'lead'), box([5.75, 5.75, 3.6], [10.25, 10.25, 4.2], 'plate'), box([3.9, 7, 3.8], [12.1, 8, 4.1], 'plate')]
    return {**{s: side for s in SIDES}, 'up': stem_up(12), 'down': [box([6, -0.25, 6], [10, 0, 10], 'plate')]}


def box_stash():
    """The Stash Box stands a pixel in, and its front has a latch: that gland sits low, under it."""
    side = [box([5.5, 5.5, 0.5], [10.5, 10.5, 1.25], 'plate'), box([7, 7, 0], [9, 9, 0.5], 'lead')]
    front = [box([6, 4.5, 0.5], [10, 7.75, 1.25], 'plate'), angled(8, 8, 0, 1.6, 45)]
    return {'north': front, 'east': side, 'south': side, 'west': side, 'up': stem_up(14), 'down': down_flange()}


def inset_one(front_note=None):
    """A cabinet standing a pixel in from its space."""
    side = [box([5.5, 5.5, 0.5], [10.5, 10.5, 1.25], 'plate'), box([7, 7, 0], [9, 9, 0.5], 'lead')]
    return {'north': [box([6.25, 6.25, 0.5], [9.75, 9.75, 1.25], 'collar'), box([7, 7, 0], [9, 9, 0.5], 'lead')],
            'east': side, 'south': side, 'west': side, 'up': up_flange(), 'down': down_flange()}


def snip():
    """Its bench stops at 12 and the blade runs across the middle, so the lead comes down behind the blade."""
    return full(up=[box([7, 12.75, 10], [9, 14.5, 12], 'lead'), box([6, 12, 9.5], [10, 12.75, 12.5], 'plate'), rotated_top()])


def rotated_top():
    """From the top centre back to the lead behind the blade."""
    e = box([7.25, 14.25, 8], [8.75, 15.75, 12], 'lead')
    e['rotation'] = {'origin': [8, 16, 8], 'axis': 'x', 'angle': -45}
    return e


def barber():
    """Straight down the gap between the two rollers."""
    return full(up=[box([7, 14.5, 7], [9, 16, 9], 'lead'), box([6.5, 14, 6.5], [9.5, 14.5, 9.5], 'plate')])


def humidor():
    """It stands on feet, so underneath a lead rises between them."""
    return full(down=[box([7, 0, 7], [9, 1.25, 9], 'lead'), box([6, 1.25, 6], [10, 2, 10], 'plate')])


def plot():
    """Top is for the crop."""
    return full(front=side_flange(), up=[])


def think_tank():
    """The desk stops at 8, so side glands stand up the face and brace back onto the desk; the chalkboard is at the back."""
    side = [box([6, 4.5, -0.5], [10, 10, 0], 'plate'), box([6.5, 8, 0], [9.5, 9.5, 1.5], 'plate'), box([7, 7, -0.75], [9, 9, -0.5], 'collar')]
    back = [box([7, 7, 15], [9, 9, 16], 'lead')]
    top = [box([7, 8.75, 9], [9, 16, 11], 'lead'), box([6, 8, 8.5], [10, 8.75, 11.5], 'plate')]
    return {'north': side, 'east': side, 'west': side, 'south': back, 'up': top, 'down': down_flange()}


def generic():
    """The Terpinator and Infuser sit in from their space: a short lead in to a small gland."""
    side = [box([7, 7, 0], [9, 9, 2.5], 'lead'), box([6, 6, 2.5], [10, 10, 3], 'plate')]
    return {**{s: side for s in SIDES}, 'up': [box([7, 12, 7], [9, 16, 9], 'lead')], 'down': [box([6, -0.25, 6], [10, 0, 10], 'plate')]}


def hit_box(high, low):
    """The front is the high tier, a wide fitting in its colour. Every other face is the tier below, smaller."""
    sides = {s: [box([6, 6, -0.5], [10, 10, 0], 'low_plate'), box([7, 7, -0.75], [9, 9, -0.5], 'low_ring')] for s in SIDES}
    sides['north'] = [box([4, 4, -0.5], [12, 12, 0], 'plate'), box([5.5, 5.5, -0.75], [10.5, 10.5, -0.5], 'collar')]
    sides['up'] = [box([6, 16, 6], [10, 16.5, 10], 'low_plate'), box([7, 16.5, 7], [9, 16.75, 9], 'low_ring')]
    sides['down'] = [box([6, -0.5, 6], [10, 0, 10], 'low_plate'), box([7, -0.75, 7], [9, -0.5, 9], 'low_ring')]
    return sides


def turned(element, quarter):
    e = copy.deepcopy(element)
    for _ in range(quarter):
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        e['from'], e['to'] = [16 - z1, y0, x0], [16 - z0, y1, x1]
        e['faces'] = {FACE_TURN[f]: v for f, v in e['faces'].items()}
        r = e.get('rotation')
        if r:
            ox, oy, oz = r['origin']
            r['origin'] = [16 - oz, oy, ox]
            r['axis'], r['angle'] = ('x', -r['angle']) if r['axis'] == 'z' else ('z', r['angle'])
    return e


def write(name, side, elements, textures, lit):
    if side in SIDES:
        elements = [turned(e, SIDES.index(side)) for e in elements]
    clash = coplanar([e for e in elements if 'rotation' not in e])
    assert not clash, f'{name} {side} z-fights: {clash}'
    tex = {k: v for k, v in textures.items()}
    model = {'parent': 'block/block', 'textures': {'particle': tex['plate'], **tex}, 'elements': elements}
    (ASSETS / 'models/block/port').mkdir(exist_ok=True)
    (ASSETS / f'models/block/port/{name}_{side}{"_lit" if lit else ""}.json').write_text(json.dumps(model, indent=1) + '\n')


def block(name, tier, sides, plate=None, extra=None):
    for lit in (False, True):
        tex = {'plate': plate or f'fourtwenty:block/power/{tier}_body',
               'collar': f'fourtwenty:block/power/{tier}_glow{"_lit" if lit else ""}',
               'lead': f'fourtwenty:block/power/{tier}_wick{"_lit" if lit else ""}', **(extra(lit) if extra else {})}
        for side in ('north', 'east', 'south', 'west', 'up', 'down'):
            write(name, side, sides.get(side, []), tex, lit)


def main():
    for name, (metal, tier) in METALS.items():
        block(name, tier, hookah(), plate=f'fourtwenty:block/hookah/{metal}')
    block('stash_jar', 'puff', jar())
    block('stash_box', 'toke', box_stash())
    block('mother_stash', 'rip', full())
    block('the_vault', 'blaze', full())
    block('snip_station', 'puff', snip())
    block('bud_barber', 'toke', barber())
    block('trim_reaper', 'rip', full())
    block('dry_box', 'puff', full())
    block('cure_closet', 'toke', inset_one())
    block('the_humidor', 'rip', humidor())
    for name, tier in (('grow_tent', 'puff'), ('hydro_bed', 'toke'), ('aero_pod', 'rip'), ('cosmic_kush_pod', 'blaze')):
        block(name, tier, plot())
    block('think_tank', 'puff', think_tank())
    block('terpinator', 'rip', generic())
    block('terpinator_lab', 'rip', generic())
    block('inspiration_infuser', 'puff', generic())
    block('inspiration_infuser_lab', 'puff', generic())
    for high, low in (('toke', 'puff'), ('rip', 'toke'), ('blaze', 'rip')):
        extra = lambda lit, l=low: {'low_plate': f'fourtwenty:block/power/{l}_body', 'low_ring': f'fourtwenty:block/power/{l}_glow{"_lit" if lit else ""}'}
        block(f'hit_box_{high}', high, hit_box(high, low), extra=extra)
    print('ok')


if __name__ == '__main__':
    main()
