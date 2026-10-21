#!/usr/bin/env python3
"""Find z-fighting in a block model: pairs of faces that face the same way, lie in the same
plane or nearly (closer than GAP pixels: at a distance the depth buffer can't separate them) and
overlap with real area (the game can't decide which to draw, so they flicker).

    python3 tools/check_model_overlaps.py <model.json> [...]

Rotated elements are checked against other rotated elements with the same rotation.
"""
import json
import sys

GAP = 0.1      # pixels; 0.1 px = 1/160 block holds up to ~80 blocks away
AXIS = {'east': (0, 1), 'west': (0, 0), 'up': (1, 1), 'down': (1, 0), 'south': (2, 1), 'north': (2, 0)}


def faces(e):
    f, t = e['from'], e['to']
    rot = json.dumps(e.get('rotation'), sort_keys=True)
    for name in e['faces']:
        axis, hi = AXIS[name]
        plane = t[axis] if hi else f[axis]
        others = [i for i in range(3) if i != axis]
        rect = [(f[i], t[i]) for i in others]
        yield name, rot, round(plane, 4), rect


def overlap(a, b):
    area = 1.0
    for (a0, a1), (b0, b1) in zip(a, b):
        d = min(a1, b1) - max(a0, b0)
        if d <= 1e-4:
            return 0
        area *= d
    return area


def check(path):
    model = json.load(open(path, encoding='utf-8'))
    els = list(model.get('elements', []))
    for child in model.get('children', {}).values():      # a NeoForge composite: check across all its parts
        els += child.get('elements', [])
    found = []
    allf = [(i, *fc) for i, e in enumerate(els) for fc in faces(e)]
    for n, (i, name, rot, plane, rect) in enumerate(allf):
        for (j, name2, rot2, plane2, rect2) in allf[n + 1:]:
            if i != j and name == name2 and rot == rot2 and abs(plane - plane2) < GAP:
                area = overlap(rect, rect2)
                if area > 1e-3:
                    found.append((i, j, name, plane, round(area, 3)))
    return els, found


if __name__ == '__main__':
    bad = 0
    for path in sys.argv[1:]:
        els, found = check(path)
        for i, j, name, plane, area in found:
            print(f'{path}: elements {i} and {j}: {name} faces at {plane} overlap by {area} px^2 '
                  f'({els[i]["from"]}..{els[i]["to"]} / {els[j]["from"]}..{els[j]["to"]})')
        bad += len(found)
        print(f'{path}: {len(els)} elements, {len(found)} overlapping face pairs')
    sys.exit(1 if bad else 0)
