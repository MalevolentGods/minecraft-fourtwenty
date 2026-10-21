#!/usr/bin/env python3
"""Find flicker in a block model. Z-fighting: pairs of faces that face the same way, lie in the same
plane or nearly (closer than GAP pixels: at a distance the depth buffer can't separate them) and
overlap with real area (the game can't decide which to draw, so they flicker). And faces whose
UV has no size (they sample their sprite's edge in the atlas and flicker with its neighbours).

    python3 tools/check_model_overlaps.py <model.json> [...]

Rotated elements are checked against other rotated elements with the same rotation.
"""
import json
import sys

GAP = 0.1      # pixels; 0.1 px = 1/160 block holds up to ~80 blocks away
AXIS = {'east': (0, 1), 'west': (0, 0), 'up': (1, 1), 'down': (1, 0), 'south': (2, 1), 'north': (2, 0)}


def faces(e):
    f, t = e['from'], e['to']
    r = e.get('rotation')
    rot = json.dumps(r, sort_keys=True)
    for name in e['faces']:
        axis, hi = AXIS[name]
        plane = t[axis] if hi else f[axis]
        others = [i for i in range(3) if i != axis]
        rect = [(f[i], t[i]) for i in others]
        # A y-rotation leaves up/down faces horizontal: compare them with everything, using the
        # rotated footprint's bounds (a slight over-estimate, which is the safe side).
        if r and r['axis'] == 'y' and name in ('up', 'down'):
            import math
            a = math.radians(r['angle'])
            ox, oz = r['origin'][0], r['origin'][2]
            pts = [((x - ox) * math.cos(a) + (z - oz) * math.sin(a) + ox, -(x - ox) * math.sin(a) + (z - oz) * math.cos(a) + oz)
                   for x in (f[0], t[0]) for z in (f[2], t[2])]
            rect = [(min(p[0] for p in pts), max(p[0] for p in pts)), (min(p[1] for p in pts), max(p[1] for p in pts))]
            rot = 'null'
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
    # A face with real size whose UV has none samples one line on its sprite's edge in the atlas:
    # it picks up neighbouring sprites (transparent ones punch flickering holes). Same fault class.
    for i, e in enumerate(els):
        f, t = e['from'], e['to']
        for name, spec in e['faces'].items():
            if 'uv' not in spec:
                continue
            u0, v0, u1, v1 = spec['uv']
            axes = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}[name]
            size = [t[a] - f[a] for a in axes]
            if (abs(u1 - u0) < 1e-6 and size[0] > 0.01) or (abs(v1 - v0) < 1e-6 and size[1] > 0.01):
                found.append((i, i, name + ' (degenerate uv)', spec['uv'], 0))
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
