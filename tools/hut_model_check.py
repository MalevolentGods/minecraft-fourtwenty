#!/usr/bin/env python3
"""Z-fighting check for hand-built block models (the hut diorama generators).

Two faces fight when they point the same way, lie in the same plane and overlap: the game can't decide which to draw and
flickers between them. Boxes that merely touch (one's top on another's bottom) don't fight, and neither do faces pointing
opposite ways. Every generator that writes a composite model calls `check(elements)` before writing, so a new overlap
fails the run, naming both boxes, instead of reaching the game.

    from hut_model_check import check
    check(solid + cutout + translucent)       # raises SystemExit listing every fight

Elements are the model's own dicts: {'name', 'from', 'to', 'faces': {direction: {...}}}.
"""
import sys

# direction -> (axis index of the plane, which of the other two axes span the face)
AXES = {'north': (2, 0, 1), 'south': (2, 0, 1), 'east': (0, 2, 1), 'west': (0, 2, 1), 'up': (1, 0, 2), 'down': (1, 0, 2)}
SIDE = {'north': 0, 'south': 1, 'west': 0, 'east': 1, 'down': 0, 'up': 1}     # which end of the box the face is on
EPS = 1e-4


def _faces(element):
    lo = [min(a, b) for a, b in zip(element['from'], element['to'])]
    hi = [max(a, b) for a, b in zip(element['from'], element['to'])]
    for direction in element.get('faces', {}):
        plane, u, v = AXES[direction]
        coord = (hi if SIDE[direction] else lo)[plane]
        yield direction, round(coord, 4), (lo[u], hi[u]), (lo[v], hi[v])


def fights(elements):
    """Every pair of same-direction, same-plane, overlapping faces, as messages."""
    seen = {}
    out = []
    for e in elements:
        for direction, coord, ur, vr in _faces(e):
            for other, ou, ov in seen.get((direction, coord), []):
                if min(ur[1], ou[1]) - max(ur[0], ou[0]) > EPS and min(vr[1], ov[1]) - max(vr[0], ov[0]) > EPS:
                    out.append(f"'{e.get('name', '?')}' and '{other}' both draw their {direction} face at {coord}")
            seen.setdefault((direction, coord), []).append((e.get('name', '?'), ur, vr))
    return out


def strip_hidden(elements):
    """Drops the faces nobody can see: a bottom resting on the block's floor (y 0) and any face of a box pressed flat inside another.
    Only the ground ones are dropped here; the rest are the generator's job to avoid, and `check` reports them."""
    for e in elements:
        if min(e['from'][1], e['to'][1]) == 0:
            e.get('faces', {}).pop('down', None)


def check(elements):
    problems = fights(elements)
    if problems:
        print(f'{len(problems)} z-fighting face pair(s):', file=sys.stderr)
        for p in problems:
            print('  ' + p, file=sys.stderr)
        raise SystemExit(1)


if __name__ == '__main__':
    import json
    model = json.load(open(sys.argv[1], encoding='utf-8'))
    check([e for child in model.get('children', {}).values() for e in child['elements']] + model.get('elements', []))
    print('no z-fighting')
