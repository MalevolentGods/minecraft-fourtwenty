"""The adding machine on flatworld: parts placed by hand, every wire routed by a router that refuses any cell where a wire would
touch something it mustn't, then an independent check that each net reaches exactly its own pins.

Usage: adder.py X0 Z0 > cmds      (X0, Z0: register A's Jar Nug; the desk is at Z0 + 11, the displays at Z0 - 8, high up)
"""
import sys
from collections import deque

X0, Z0 = int(sys.argv[1]), int(sys.argv[2])
Y = -59
YD = Y + 6                      # the displays' level
DIRS = {'E': (1, 0, 0), 'W': (-1, 0, 0), 'S': (0, 0, 1), 'N': (0, 0, -1), 'U': (0, 1, 0), 'D': (0, -1, 0)}
OPP = {'E': 'W', 'W': 'E', 'S': 'N', 'N': 'S', 'U': 'D', 'D': 'U'}
TYPE = {}                       # net -> 'ribbon', 'thread', 'red', 'black'
for n in ('qA', 'nA', 'qB', 'nB', 'qC'):
    TYPE[n] = 'ribbon'
for n in ('clkA', 'clkB', 'clkC', 'RESET', 'MASK', 'DRESET'):
    TYPE[n] = 'thread'
TYPE['VDD'], TYPE['GND'] = 'red', 'black'
BOUNDS = ((X0 - 10, X0 + 18), (Y - 1, YD + 1), (Z0 - 10, Z0 + 11))

grid = {}                       # (x, y, z) -> dict(kind, net, ...)
reserved = {}                   # (x, y, z) -> net, or None to be kept empty
comps = []


def add(p, v, x, z):
    return (p[0] + x, p[1], p[2] + z) if len(p) == 3 else None


def at(x, y, z): return (x, y, z)


def step(p, d):
    dx, dy, dz = DIRS[d]
    return (p[0] + dx, p[1] + dy, p[2] + dz)


def comp(name, block, pos, facing, pins, nbt=''):
    """pins: {direction: (net, 'single'|'bus')}; a None net keeps that face's cell empty."""
    c = {'kind': 'comp', 'name': name, 'block': block, 'facing': facing, 'pins': pins, 'nbt': nbt}
    grid[pos] = c
    comps.append((pos, c))
    for d, (net, kind) in pins.items():
        cell = step(pos, d)
        if net is None:
            reserved[cell] = None
        elif cell not in grid:
            reserved.setdefault(cell, net)
    return c


def chip(name, block, pos, pins, nbt=''):
    """A chip facing east: front E, back W, left S, right N, VDD top, GND bottom (its riser up to the VDD plane is reserved)."""
    pins = dict(pins)
    pins['U'] = ('VDD', 'single')
    pins['D'] = ('GND', 'single')
    comp(name, block + '[facing=east]', pos, 'east', pins, nbt)
    for dy in (1, 2):
        reserved[(pos[0], pos[1] + dy, pos[2])] = 'VDD'


def cell(pos, kind, net, chan=None):
    grid[pos] = {'kind': kind, 'net': net, 'chan': chan}


# ---- the parts ---------------------------------------------------------------------------------------------------------------------
def register(ox, q, n, clk):
    """A Jar Nug and a Stacker Nug adding the carry in (1 normally, 0 during reset) to it, in a loop; a Roach Clip grounds the carry
    wire of the loop (on the GND plane under it), so 255 wraps to 0."""
    J, S = (ox, Y, Z0), (ox + 4, Y, Z0)
    chip('J' + q, 'jar_nug', J, {'W': (n, 'bus'), 'E': (q, 'bus'), 'S': (clk, 'single'), 'N': ('RESET', 'single')})
    chip('S' + q, 'stacker_nug', S, {'W': (q, 'bus'), 'E': (n, 'bus'), 'S': (None, 'bus'), 'N': ('MASK', 'single')})
    for dx in (1, 2, 3):
        cell((ox + dx, Y, Z0), 'ribbon', q)
    loop = [(5, 0), (5, -1), (5, -2), (4, -2), (3, -2), (2, -2), (1, -2), (0, -2), (-1, -2), (-1, -1), (-1, 0)]
    for dx, dz in loop:
        p = (ox + dx, Y, Z0 + dz)
        if (dx, dz) == (2, -2):
            cell(p, 'clip', n, chan='GND')           # channel 8 to GND, through the cell beneath
            reserved[(p[0], Y + 1, p[2])] = None
            reserved[(p[0], Y - 1, p[2])] = 'GND'
        else:
            cell(p, 'ribbon', n)


register(X0, 'qA', 'nA', 'clkA')
register(X0 + 10, 'qB', 'nB', 'clkB')
S2, JC = (X0 + 6, Y, Z0 + 4), (X0 + 7, Y, Z0 + 4)
chip('S2', 'stacker_nug', S2, {'W': ('qA', 'bus'), 'S': ('qB', 'bus'), 'N': (None, 'single'), 'E': (None, 'bus')})
grid[S2]['pins']['E'] = ('direct', 'bus')
chip('JC', 'jar_nug', JC, {'W': ('direct', 'bus'), 'E': ('qC', 'bus'), 'S': ('clkC', 'single'), 'N': (None, 'single')})
reserved.pop(step(S2, 'E'), None)
# the reset logic, west: NOT for the mask, four Buffers for the late clock
chip('NOT', 'not_nug', (X0 - 3, Y, Z0 + 5), {'W': ('RESET', 'single'), 'E': ('MASK', 'single')})
for i in range(4):
    pins = {'W': ('RESET' if i == 0 else 'direct', 'single'), 'E': ('DRESET' if i == 3 else 'direct', 'single')}
    chip(f'BUF{i}', 'buffer_nug', (X0 - 7 + i, Y, Z0 + 8), pins)
for i in range(3):
    reserved.pop((X0 - 6 + i, Y, Z0 + 8), None)
# One-Hitters from the late clock into each register's clock: back (anode) on DRESET, front (cathode) on the clock
comp('dA', 'one_hitter[facing=east]', (X0 - 1, Y, Z0 + 1), 'east', {'E': ('clkA', 'single'), 'W': ('DRESET', 'single')})
comp('dB', 'one_hitter[facing=east]', (X0 + 9, Y, Z0 + 1), 'east', {'E': ('clkB', 'single'), 'W': ('DRESET', 'single')})
comp('dC', 'one_hitter[facing=north]', (X0 + 7, Y, Z0 + 6), 'north', {'N': ('clkC', 'single'), 'S': ('DRESET', 'single')})
# the desk: Puff Buttons side by side, each from VDD (its back, west) to its line (its front, east)
BUTTONS = [('A', 'clkA', X0 - 1), ('B', 'clkB', X0 + 3), ('=', 'clkC', X0 + 7), ('RESET', 'RESET', X0 + 11)]
for label, net, bx in BUTTONS:
    comp('btn' + label, 'puff_button[facing=east]', (bx, Y, Z0 + 11), 'east', {'W': ('VDD', 'single'), 'E': (net, 'single')})
# the supply: a Bottomless Stash at the west end of the desk
comp('STASH', 'bottomless_stash_puff[facing=north]', (X0 - 4, Y, Z0 + 10), 'north',
     {'E': ('VDD', 'single'), 'D': ('GND', 'single'), 'W': (None, 'single'), 'N': (None, 'single'), 'S': (None, 'single'), 'U': (None, 'single')})
# the displays: Numb Nugs facing south, high on the back wall; only the source digit (rightmost as you face them) is wired
DISPLAYS = [('qA', [X0 - 2, X0 - 1, X0]), ('qC', [X0 + 4, X0 + 5, X0 + 6, X0 + 7]), ('qB', [X0 + 11, X0 + 12, X0 + 13])]
ZW = Z0 - 8
for net, xs in DISPLAYS:
    for x in xs:
        src = x == xs[-1]
        pins = {'N': (net, 'bus'), 'U': ('VDD', 'single'), 'D': ('GND', 'single')} if src else {'N': (None, 'bus'), 'U': (None, 'single'), 'D': (None, 'single')}
        comp('disp' + net + str(x), 'numb_nug[facing=south]', (x, YD, ZW), 'south', pins)
        if src:
            # stubs behind it for its power and ground, kept before any Ribbon is routed (or one climbing behind it walls them in)
            reserved[(x, YD + 1, ZW - 1)] = 'VDD'
            reserved[(x, YD - 1, ZW - 1)] = 'GND'
for p, net in list(reserved.items()):
    if net is not None and p not in grid:
        cell(p, {'ribbon': 'ribbon', 'thread': 'thread', 'red': 'red', 'black': 'black'}[TYPE[net]], net)


# ---- joining rules -----------------------------------------------------------------------------------------------------------------
def joins(t, c_dir_from_n, n):
    """If a cell of type t (ribbon, clip, thread, red, black) sits beside n, on n's side c_dir_from_n, what joins: a list of (n's net, how)."""
    k = n['kind']
    if k == 'comp':
        pin = n['pins'].get(c_dir_from_n)
        if not pin or pin[0] is None:
            return []
        net, kind = pin
        if kind == 'bus' and t in ('ribbon', 'clip'):
            return [(net, 'bus')]
        if kind == 'single' and t in ('thread', 'red', 'black', 'clip'):
            return [(net, 'chan' if t == 'clip' else 'wire')]
        return []
    if k == 'ribbon':
        return [(n['net'], 'bus')] if t in ('ribbon', 'clip') else []
    if k == 'clip':
        if t in ('ribbon', 'clip'):
            return [(n['net'], 'bus')]
        return [(n['chan'], 'wire')]
    if k == 'thread':
        return [(n['net'], 'wire')] if t in ('thread',) else ([(n['net'], 'chan')] if t == 'clip' else [])
    if k in ('red', 'black'):
        if t == k:
            return [(n['net'], 'wire')]
        return [(n['net'], 'chan')] if t == 'clip' else []
    return []


def ok(p, net, t, chan=None):
    for d in DIRS:
        q = step(p, d)
        n = grid.get(q)
        if n is None:
            r = reserved.get(q, 'free')
            if r == 'free' or r is None:
                continue
            n = {'kind': {'ribbon': 'ribbon', 'thread': 'thread', 'red': 'red', 'black': 'black'}[TYPE[r]], 'net': r, 'chan': None}
        for jn, how in joins(t, OPP[d], n):
            want = chan if how == 'chan' else net
            if jn != want and jn != 'direct':
                return False
    return True


def free(p, net):
    (x0, x1), (y0, y1), (z0, z1) = BOUNDS
    if not (x0 <= p[0] <= x1 and y0 <= p[1] <= y1 and z0 <= p[2] <= z1):
        return False
    r = reserved.get(p, 'free')
    # the view from the desk to the displays stays clear: wires climb to them behind their wall
    if r == 'free' and (p[1] >= Y + 3 and p[2] >= ZW or p[1] >= Y + 2 and p[2] >= Z0 + 6):
        return False
    if p in grid:
        return False
    r = reserved.get(p, 'free')
    return r == 'free' or r == net


def route(net):
    t = {'ribbon': 'ribbon', 'thread': 'thread', 'red': 'red', 'black': 'black'}[TYPE[net]]
    cells = [p for p, v in grid.items() if v.get('net') == net and v['kind'] != 'comp']
    if not cells:
        return
    tree = {cells[0]}
    todo = set(cells[1:])
    # parts of the net already touching (cells side by side) are one piece
    def grow():
        changed = True
        while changed:
            changed = False
            for p in list(todo):
                if any(step(p, d) in tree for d in DIRS):
                    tree.add(p)
                    todo.discard(p)
                    changed = True
    grow()
    while todo:
        prev = {p: None for p in tree}
        queue = deque(tree)
        hit = None
        while queue:
            p = queue.popleft()
            for d in DIRS:
                q = step(p, d)
                if q in prev:
                    continue
                if q in todo:
                    prev[q] = p
                    hit = q
                    break
                if free(q, net) and ok(q, net, t):
                    prev[q] = p
                    queue.append(q)
            if hit:
                break
        if not hit:
            raise SystemExit(f'cannot route {net}: {sorted(todo)[:3]}')
        p = prev[hit]
        while p is not None and p not in tree:
            cell(p, t, net)
            tree.add(p)
            p = prev[p]
        tree.add(hit)
        todo.discard(hit)
        grow()


ZW = Z0 - 8
for net in ('qA', 'qB', 'qC', 'clkA', 'clkB', 'clkC', 'RESET', 'MASK', 'DRESET'):
    route(net)

# ---- the planes: VDD a level above the routing, GND on the ground; then everything else that's VDD or GND joined to them -----------
# VDD and GND as thin lines: a sheet overhead would hide the displays, and a sheet anywhere makes one enormous network to solve
route('VDD')
route('GND')


# ---- the check: rebuild every net from the grid, as the game will, and compare -----------------------------------------------------
def check():
    parent = {}
    def find(a):
        while parent.setdefault(a, a) != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a
    def union(a, b): parent[find(a)] = find(b)
    nodes = {}
    for p, v in grid.items():
        if v['kind'] == 'comp':
            continue
        nodes[p] = v
        if v['kind'] == 'clip':
            union(('clip', p), ('chanref', p))
    for p, v in nodes.items():
        t = v['kind']
        for d in DIRS:
            q = step(p, d)
            n = grid.get(q)
            if n is None:
                continue
            if n['kind'] == 'comp':
                for jn, how in joins(t, OPP[d], n):
                    key = ('pin', q, OPP[d])
                    union((p, 'chan' if how == 'chan' else 'self'), key)
                continue
            for jn, how in joins(t, OPP[d], n):
                # a clip's tapped channel is its own node: a wire beside it joins that, not the rest of its bus
                mine = (p, 'chan') if how == 'chan' else (p, 'self')
                theirs = (q, 'chan') if n['kind'] == 'clip' and t not in ('ribbon', 'clip') else (q, 'self')
                union(mine, theirs)
    # each pin should be in exactly its own net's set
    sets = {}
    for pos, c in comps:
        for d, (net, kind) in c['pins'].items():
            if net in (None, 'direct'):
                continue
            key = ('pin', pos, d)
            sets.setdefault(net, set()).add(find(key))
    bad = {net: len(s) for net, s in sets.items() if len(s) != 1}
    roots = {}
    for net, s in sets.items():
        for r in s:
            roots.setdefault(r, set()).add(net)
    merged = [nets for nets in roots.values() if len(nets) > 1]
    return bad, merged


bad, merged = check()
if bad or merged:
    raise SystemExit(f'check failed: split {bad}, merged {merged}')


# ---- the layout, for the game test: every block relative to (X0, Y - 1, Z0) ----------------------------------------------------------
if len(sys.argv) > 3 and sys.argv[3] == 'java':
    lines = []
    for p, v in sorted(grid.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
        k = v['kind']
        state = {'comp': lambda: 'fourtwenty:' + v['block'], 'ribbon': lambda: 'fourtwenty:eighth_ribbon', 'clip': lambda: 'fourtwenty:roach_clip[channel=8]',
                 'thread': lambda: 'fourtwenty:hemp_thread', 'red': lambda: 'fourtwenty:hemp_wick[colour=red]', 'black': lambda: 'fourtwenty:hemp_wick[colour=black]'}[k]()
        lines.append(f'            "{p[0] - X0} {p[1] - (Y - 1)} {p[2] - Z0} {state}",')
    NL = chr(10)
    print('package com.malevolentgods.fourtwenty.gametest;' + NL)
    print("/** The adding machine's layout, from the flatworld build's router (every block relative to register A's Jar Nug, a level down). Generated. */")
    print('final class AdderLayout {')
    print('    private AdderLayout() {}' + NL)
    print('    static final String[] BLOCKS = {')
    print(NL.join(lines))
    print('    };')
    print('}')
    sys.exit(0)


# ---- out ---------------------------------------------------------------------------------------------------------------------------
out = [f'fill {X0 - 10} {Y - 1} {Z0 - 10} {X0 + 18} {YD + 1} {Z0 + 11} minecraft:air']
for p, v in grid.items():
    x, y, z = p
    k = v['kind']
    if k == 'comp':
        out.append(f'setblock {x} {y} {z} fourtwenty:{v["block"]}{v["nbt"]}')
    elif k == 'ribbon':
        out.append(f'setblock {x} {y} {z} fourtwenty:eighth_ribbon')
    elif k == 'clip':
        out.append(f'setblock {x} {y} {z} fourtwenty:roach_clip[channel=8]')
    elif k == 'thread':
        out.append(f'setblock {x} {y} {z} fourtwenty:hemp_thread')
    else:
        out.append(f'setblock {x} {y} {z} fourtwenty:hemp_wick[colour={"red" if k == "red" else "black"}]')
for label, net, bx in BUTTONS:
    text = {'A': ['A + 1', '', '', ''], 'B': ['B + 1', '', '', ''], '=': ['=', 'A + B', '', ''], 'RESET': ['RESET', 'clears all', '', '']}[label]
    msgs = ','.join(f"'\"{t}\"'" for t in text)
    out.append(f"setblock {bx} {Y - 1} {Z0 + 12} minecraft:oak_sign[rotation=0]{{front_text:{{messages:[{msgs}]}}}}")
sys.stderr.write(f'{len(grid)} cells, nets ok\n')
print('\n'.join(out))
