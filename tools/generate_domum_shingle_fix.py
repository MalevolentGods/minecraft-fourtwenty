#!/usr/bin/env python3
"""Stops Domum Ornamentum's roof tiles flickering.

    python3 tools/generate_domum_shingle_fix.py

Domum's shingle models have faces lying exactly on top of each other, worst on the corners. It barely shows when both are the same solid material, but
see-through materials such as glass flicker there. This copies those models from the pinned Domum jar with one face of each pair pulled in by a hair, so no
two faces share a plane, and writes them to assets/domum_ornamentum. Nothing else in them changes. They stay under Domum's GPL-3.0: see licenses/.
"""
import copy
import json
import math
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / '.compat/dependencies/domum-ornamentum-1.0.234-snapshot-main.jar'
OUT = ROOT / 'src/main/resources/assets/domum_ornamentum/models/block/shingle'
SOURCE = 'assets/domum_ornamentum/models/block/shingle/'
SPECS = [f'{kind}{shape}_spec' for kind in ('', 'flat_', 'flat_lower_', 'steep_', 'steep_lower_') for shape in ('straight', 'convex', 'concave')]
NUDGE = 0.06            # 1/16ths of a block
FLAT = 0.02             # faces this close count as sharing a plane
AXES = {'x': 0, 'y': 1, 'z': 2}
CORNERS = {'north': [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)], 'south': [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)],
           'west': [(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)], 'east': [(1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)],
           'down': [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)], 'up': [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)]}
OUTWARD = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0), 'down': (0, -1, 0), 'up': (0, 1, 0)}


def turn(point, rotation):
    if not rotation or not rotation.get('angle'):
        return list(point)
    angle = math.radians(rotation['angle'])
    axis = AXES[rotation['axis']]
    i, j = {0: (1, 2), 1: (2, 0), 2: (0, 1)}[axis]
    v = [point[k] - rotation['origin'][k] for k in range(3)]
    out = v[:]
    out[i] = v[i] * math.cos(angle) - v[j] * math.sin(angle)
    out[j] = v[i] * math.sin(angle) + v[j] * math.cos(angle)
    return [out[k] + rotation['origin'][k] for k in range(3)]


def quad(element, face):
    a, b = element['from'], element['to']
    return [turn([b[k] if c[k] else a[k] for k in range(3)], element.get('rotation')) for c in CORNERS[face]]


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def normal(element, face):
    n = turn(OUTWARD[face], {**element['rotation'], 'origin': [0, 0, 0]} if element.get('rotation') else None)
    return [x / math.sqrt(dot(n, n)) for x in n]


def overlap(p, q, n):
    u = [p[1][k] - p[0][k] for k in range(3)]
    length = math.sqrt(dot(u, u)) or 1
    u = [x / length for x in u]
    v = [n[1] * u[2] - n[2] * u[1], n[2] * u[0] - n[0] * u[2], n[0] * u[1] - n[1] * u[0]]
    flat = lambda points: [(dot(a, u), dot(a, v)) for a in points]
    a, b = flat(p), flat(q)
    for polygon in (a, b):
        for i in range(4):
            x, y = polygon[i]
            x2, y2 = polygon[(i + 1) % 4]
            side = (y2 - y, x - x2)
            size = math.hypot(*side) or 1
            side = (side[0] / size, side[1] / size)
            pa = [c[0] * side[0] + c[1] * side[1] for c in a]
            pb = [c[0] * side[0] + c[1] * side[1] for c in b]
            if min(max(pa), max(pb)) - max(min(pa), min(pb)) < 0.05:
                return False
    return True


def clashes(elements):
    faces = []
    for i, e in enumerate(elements):
        for face, info in e.get('faces', {}).items():
            q = quad(e, face)
            n = normal(e, face)
            faces.append((i, face, info.get('texture', ''), q, n, dot(n, q[0])))
    found = []
    for a in range(len(faces)):
        for b in range(a + 1, len(faces)):
            p, q = faces[a], faces[b]
            if p[0] != q[0] and dot(p[4], q[4]) > 0.9999 and abs(p[5] - q[5]) < FLAT and overlap(p[3], q[3], p[4]):
                found.append((p, q))
    return found


def pull_in(element, face):
    axis = {'west': 0, 'east': 0, 'down': 1, 'up': 1, 'north': 2, 'south': 2}[face]
    low, high = element['from'][axis], element['to'][axis]
    if high - low <= NUDGE * 2:
        return False
    if face in ('west', 'down', 'north'):
        element['from'][axis] = round(low + NUDGE, 5)
    else:
        element['to'][axis] = round(high - NUDGE, 5)
    return True


def fix(model):
    """Pulls the support in rather than the tile where there's a choice: the tile is what shows."""
    fixed = copy.deepcopy(model)
    elements = fixed['elements']
    for _ in range(400):
        found = clashes(elements)
        if not found:
            return fixed
        moved = False
        for p, q in found:
            pick = q if 'plank' in q[2] or 'plank' not in p[2] else p
            other = p if pick is q else q
            if pull_in(elements[pick[0]], pick[1]) or pull_in(elements[other[0]], other[1]):
                moved = True
                break
        if not moved:
            break
    return fixed


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    jar = zipfile.ZipFile(JAR)
    for spec in SPECS:
        model = json.loads(jar.read(f'{SOURCE}{spec}.json'))
        out = fix(model)
        (OUT / f'{spec}.json').write_text(json.dumps(out, indent=1) + '\n')
        print(spec, len(clashes(model['elements'])), '->', len(clashes(out['elements'])))


if __name__ == '__main__':
    main()
