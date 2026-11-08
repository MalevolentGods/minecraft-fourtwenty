"""The keypad calculator on flatworld (Sensi Heights tier 5): a 4x4 keypad and a 3-digit display mounted on Rolling Trays in a lobby
wall, a Dankoder behind the keypad, and the logic in a sealed room behind, every wire routed by a router that refuses any cell where a
wire would touch something it mustn't, then an independent check that each net reaches exactly its own pins.

    python tools/build_calculator.py > cmds          the flatworld commands (the building and the machine)
    python tools/build_calculator.py java > Layout   the machine alone, for the game test (CalculatorLayout.java)

The machine (0 to 255, with an overflow lamp; codes from the Dankoder): digits 0-9; A +, B -, C clear, D =, E back (one step), F '.'
(nothing). Five Jar Nugs: X (the entry, shown), A (the first operand), P (X before the last digit, for back), and two staging registers
SD (X x 10 + digit) and SR (A + X, or A - X), which with P share one bus into X: each lets go of it unless its key was pressed, so the
others (+, -, C) load 0. The key-down pulse, delayed by Buffer Nugs, clocks the staging registers first (CLK1) and X after (CLK2).
x 10 is three doublings and one more add (8x + 2x); subtraction flips X with an XOR Blend Nug against a mask of the operation, and adds
1 on the Stacker's carry in.
"""
import sys
from collections import deque

sys.stdout.reconfigure(encoding="utf-8")

Y = -59                         # the logic's level; GND on the floor below it, VDD lines above
FLOOR = -61                     # the building's floor, level with the ground
ZP = -14                        # the panel wall: its trays face south into the lobby
DIRS = {'E': (1, 0, 0), 'W': (-1, 0, 0), 'S': (0, 0, 1), 'N': (0, 0, -1), 'U': (0, 1, 0), 'D': (0, -1, 0)}
OPP = {'E': 'W', 'W': 'E', 'S': 'N', 'N': 'S', 'U': 'D', 'D': 'U'}
BOUNDS = ((303, 340), (Y - 1, Y + 7), (-39, ZP - 1))
BUILDING = ((302, 341), (-40, -7))      # x, z: walls included
ORIGIN = (302, FLOOR, -40)              # the game test's layout is relative to this

RIBBONS = ['K', 'DEC', 'X', 'X2', 'X4', 'X8', 'X10', 'D10', 'S', 'A', 'R', 'R2', 'XM', 'MASK']
THREADS = ['CLK1', 'CLK2', 'n1', 'CMD', 'DIG', 'OPK', 'OP', 'CLKP', 'CLKA', 'NDOT', 'CLKX', 'RSD', 'RSR', 'RP', 'OVF',
           'B1', 'B2', 'B3', 'STB', 'PLUS', 'MINUS', 'EQ', 'BKS', 'DOT', 'CARRY', 'M8']
TYPE = {n: 'ribbon' for n in RIBBONS + ['MASK2']}
TYPE.update({n: 'thread' for n in THREADS})
TYPE['VDD'], TYPE['GND'] = 'red', 'black'

grid = {}                       # (x, y, z) -> dict(kind, net, ...)
reserved = {}                   # (x, y, z) -> net, or None to be kept empty
comps = []
panel = []                      # (pos, block state, nbt): the panel's parts, not routed


def step(p, d):
    dx, dy, dz = DIRS[d]
    return (p[0] + dx, p[1] + dy, p[2] + dz)


def comp(name, block, pos, pins, nbt=''):
    """pins: {direction: (net, 'single'|'bus')}; a None net keeps that face's cell empty."""
    c = {'kind': 'comp', 'name': name, 'block': block, 'pins': pins, 'nbt': nbt}
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
    """A chip facing east: front E, back W, left S, right N, VDD top, GND bottom (its riser up to the VDD lines is reserved)."""
    pins = dict(pins)
    pins['U'] = ('VDD', 'single')
    pins['D'] = ('GND', 'single')
    comp(name, block + '[facing=east]', pos, pins, nbt)
    for dy in (1, 2):
        reserved[(pos[0], pos[1] + dy, pos[2])] = 'VDD'


def cell(pos, kind, net, chan=None, channel=None):
    grid[pos] = {'kind': kind, 'net': net, 'chan': chan, 'channel': channel}


def clip(pos, bus, channel, chan):
    """A Roach Clip on {bus}, tapping {channel} out to the single net {chan}."""
    cell(pos, 'clip', bus, chan, channel)
    reserved.pop(pos, None)


# ---- the parts ---------------------------------------------------------------------------------------------------------------------
def C(k): return 312 + 4 * k


R0, R1, R2, R3, R4, R5 = -18, -22, -26, -30, -34, -38
# the key-down pulse, delayed: four Buffers to CLK1, four more to CLK2
for i in range(4):
    chip(f'BUF{i}', 'buffer_nug', (312 + i, Y, R0), {'W': ('STB' if i == 0 else 'direct', 'single'), 'E': ('CLK1' if i == 3 else 'direct', 'single')})
for i in range(4):
    chip(f'BUF{4 + i}', 'buffer_nug', (326 + i, Y, R0), {'W': ('CLK1' if i == 0 else 'direct', 'single'), 'E': ('CLK2' if i == 3 else 'direct', 'single')})
for x in (313, 314, 315, 327, 328, 329):
    reserved.pop((x, Y, R0), None)
clip((311, Y, R0), 'K', 8, 'STB')
# decoding: a command is code bit 3 and (bit 1 or bit 2); a digit isn't one
chip('OR1', 'or_nug', (C(0), Y, R1), {'S': ('B1', 'single'), 'N': ('B2', 'single'), 'E': ('n1', 'single')})
clip((C(0), Y, R1 + 1), 'K', 1, 'B1')
clip((C(0), Y, R1 - 1), 'K', 2, 'B2')
chip('AND1', 'and_nug', (C(1), Y, R1), {'S': ('B3', 'single'), 'N': ('n1', 'single'), 'E': ('CMD', 'single')})
clip((C(1), Y, R1 + 1), 'K', 3, 'B3')
chip('NOT1', 'not_nug', (C(2), Y, R1), {'W': ('CMD', 'single'), 'E': ('DIG', 'single')})
chip('NOT3', 'not_nug', (C(3), Y, R1), {'W': ('DIG', 'single'), 'E': ('RSD', 'single')})
chip('AND2', 'and_nug', (C(4), Y, R1), {'S': ('CLK1', 'single'), 'N': ('DIG', 'single'), 'E': ('CLKP', 'single')})
# the Dealer picks out the command: its select the code's low bits, its input bit 3; out A +, B -, D =, E back, F '.'
chip('DEAL', 'dealer_nug', (C(5), Y, R1), {'W': ('B3', 'single'), 'S': ('K', 'bus'), 'E': ('DEC', 'bus')})
clip((C(5) - 1, Y, R1), 'K', 3, 'B3')
chip('OR2', 'or_nug', (C(0), Y, R2), {'S': ('MINUS', 'single'), 'N': ('PLUS', 'single'), 'E': ('OPK', 'single')})
clip((C(0), Y, R2 + 1), 'DEC', 3, 'MINUS')
clip((C(0), Y, R2 - 1), 'DEC', 2, 'PLUS')
chip('LATCH', 'latch_nug', (C(1), Y, R2), {'S': ('MINUS', 'single'), 'N': ('PLUS', 'single'), 'E': ('OP', 'single')})
clip((C(1), Y, R2 + 1), 'DEC', 3, 'MINUS')
clip((C(1), Y, R2 - 1), 'DEC', 2, 'PLUS')
chip('AND3', 'and_nug', (C(2), Y, R2), {'S': ('CLK1', 'single'), 'N': ('OPK', 'single'), 'E': ('CLKA', 'single')})
chip('NOT2', 'not_nug', (C(3), Y, R2), {'W': ('DOT', 'single'), 'E': ('NDOT', 'single')})
clip((C(3) - 1, Y, R2), 'DEC', 7, 'DOT')
chip('AND4', 'and_nug', (C(4), Y, R2), {'S': ('CLK2', 'single'), 'N': ('NDOT', 'single'), 'E': ('CLKX', 'single')})
chip('NOT4', 'not_nug', (C(5), Y, R2), {'W': ('EQ', 'single'), 'E': ('RSR', 'single')})
clip((C(5) - 1, Y, R2), 'DEC', 5, 'EQ')
chip('NOT5', 'not_nug', (C(6), Y, R2), {'W': ('BKS', 'single'), 'E': ('RP', 'single')})
clip((C(6) - 1, Y, R2), 'DEC', 6, 'BKS')
# arithmetic: x 10 as 2x, 4x, 8x, 8x + 2x; then + the digit (the code bus); A + X, or A + ~X + 1
chip('S1', 'stacker_nug', (C(0), Y, R3), {'W': ('X', 'bus'), 'S': ('X', 'bus'), 'N': (None, 'single'), 'E': ('X2', 'bus')})
chip('S2', 'stacker_nug', (C(1), Y, R3), {'W': ('X2', 'bus'), 'S': ('X2', 'bus'), 'N': (None, 'single'), 'E': ('X4', 'bus')})
chip('S3', 'stacker_nug', (C(2), Y, R3), {'W': ('X4', 'bus'), 'S': ('X4', 'bus'), 'N': (None, 'single'), 'E': ('X8', 'bus')})
chip('S4', 'stacker_nug', (C(3), Y, R3), {'W': ('X8', 'bus'), 'S': ('X2', 'bus'), 'N': (None, 'single'), 'E': ('X10', 'bus')})
chip('S5', 'stacker_nug', (C(4), Y, R3), {'W': ('X10', 'bus'), 'S': ('K', 'bus'), 'N': (None, 'single'), 'E': ('D10', 'bus')})
chip('JSD', 'jar_nug', (C(5), Y, R3), {'W': ('D10', 'bus'), 'S': ('CLK1', 'single'), 'N': ('RSD', 'single'), 'E': ('S', 'bus')})
chip('JP', 'jar_nug', (C(6), Y, R3), {'W': ('X', 'bus'), 'S': ('CLKP', 'single'), 'N': ('RP', 'single'), 'E': ('S', 'bus')})
chip('BLEND', 'blend_nug', (C(0), Y, R4), {'W': ('X', 'bus'), 'S': ('MASK', 'bus'), 'E': ('XM', 'bus')}, '{value:2}')
chip('S6', 'stacker_nug', (C(1), Y, R4), {'W': ('A', 'bus'), 'S': ('XM', 'bus'), 'N': ('OP', 'single'), 'E': ('R', 'bus')})
chip('JSR', 'jar_nug', (C(2), Y, R4), {'W': ('R2', 'bus'), 'S': ('CLK1', 'single'), 'N': ('RSR', 'single'), 'E': ('S', 'bus')})
chip('JX', 'jar_nug', (C(3), Y, R4), {'W': ('S', 'bus'), 'S': ('CLKX', 'single'), 'N': (None, 'single'), 'E': ('X', 'bus')})
chip('JA', 'jar_nug', (C(4), Y, R4), {'W': ('X', 'bus'), 'S': ('CLKA', 'single'), 'N': (None, 'single'), 'E': ('A', 'bus')})
# the overflow lamp: X's ninth bit, past 255 (the display still reads it, to 511)
clip((C(3) + 2, Y, R4), 'X', 8, 'OVF')
# subtracting, the carry out means "no borrow", not a ninth bit: carry and minus flips it back to 0 before SR takes the result
chip('AND6', 'and_nug', (C(3), Y, R5), {'S': ('CARRY', 'single'), 'N': ('OP', 'single'), 'E': ('M8', 'single')})
clip((C(3), Y, R5 + 1), 'R', 8, 'CARRY')
chip('BLEND2', 'blend_nug', (C(2), Y, R5), {'W': ('R', 'bus'), 'S': ('MASK2', 'bus'), 'E': ('R2', 'bus')}, '{value:2}')
clip((C(2), Y, R5 + 1), 'MASK2', 8, 'M8')
# the mask: eight Roach Clips in a row, channels 0 to 7, each touched by the OP thread running beside them; the row leaves the Blend's B
# face southward first (straight west it would touch the Blend's A input, X)
cell((C(0), Y, R4 + 2), 'ribbon', 'MASK')
for i in range(8):
    clip((304 + i, Y, R4 + 2), 'MASK', i, 'OP')
    cell((304 + i, Y, R4 + 3), 'thread', 'OP')
    reserved[(304 + i, Y, R4 + 1)] = None
    reserved[(304 + i, Y + 1, R4 + 2)] = None
# the supply: a Bottomless Stash in the room's corner
comp('STASH', 'bottomless_stash_puff[facing=north]', (C(6), Y, R4),
     {'E': ('VDD', 'single'), 'D': ('GND', 'single'), 'W': (None, 'single'), 'N': (None, 'single'), 'S': (None, 'single'), 'U': (None, 'single')})

# ---- the panel: trays in the wall at ZP facing south; keys at y -59 to -56, x 320 to 323; the display row at -55 -------------------
KX, KY = 320, -59
LAYOUT = [['7', '8', '9', 'C'], ['4', '5', '6', '-'], ['1', '2', '3', '+'], ['<', '0', '.', '=']]
NUMBER = {**{str(d): d for d in range(10)}, '+': 10, '-': 11, 'C': 12, '=': 13, '<': 14, '.': 15}
LABEL = {'<': '←', '-': '−'}
# glows (dye ids): digits white, + and - lime, = green, C red, back orange, '.' gray
GLOW = {**{str(d): 0 for d in range(10)}, '+': 5, '-': 5, '=': 13, 'C': 14, '<': 1, '.': 7}
DISPLAY_Y = KY + 4
TRAYS = {}
for r, row in enumerate(LAYOUT):
    for c, key in enumerate(row):
        TRAYS[(KX + c, KY + 3 - r, ZP)] = ('key', key)
for c in range(3):
    TRAYS[(KX + 1 + c, DISPLAY_Y, ZP)] = ('digit', c)
TRAYS[(KX, DISPLAY_Y, ZP)] = ('lamp', 'OVF')
TRAYS[(KX + 4, DISPLAY_Y, ZP)] = ('lamp', 'OP')
FEED = {(KX + 3, DISPLAY_Y, ZP): ('X', 'bus'), (KX + 2, DISPLAY_Y, ZP): ('VDD', 'single'), (KX + 1, DISPLAY_Y, ZP): ('GND', 'single'),
        (KX, DISPLAY_Y, ZP): ('OVF', 'single'), (KX + 4, DISPLAY_Y, ZP): ('OP', 'single')}
# behind the panel nothing but its feeds and the Dankoder: a wire on a tray's back would be its signal or its power
for x in range(KX - 3, KX + 6):
    for y in range(Y - 1, Y + 8):
        reserved[(x, y, ZP - 1)] = None
for pos, (what, v) in TRAYS.items():
    comp('tray', f'rolling_tray[facing=south]', pos, {'N': FEED.get(pos, (None, 'single'))})
    if pos in FEED:
        reserved[step(pos, 'N')] = FEED[pos][0]
# the Dankoder behind the bottom-left key: its back on that tray, its front (north) the code bus
DK = (KX, KY, ZP - 1)
comp('DANK', 'dankoder[facing=north]', DK, {'N': ('K', 'bus'), 'U': (None, 'single'), 'D': (None, 'single'), 'E': (None, 'bus'), 'W': (None, 'bus')})
for pos, (what, v) in TRAYS.items():
    front = (pos[0], pos[1], pos[2] + 1)
    if what == 'key':
        n = NUMBER[v]
        label = LABEL.get(v, v)
        panel.append((front, 'puff_button[facing=south,mounted=true]', f'{{value:{n + 1},label:"{label}",glow:{GLOW[v]}}}'))
    elif what == 'digit':
        panel.append((front, 'numb_nug[facing=south,mounted=true]', ''))
    else:
        panel.append((front, 'red_lamp' if v == 'OVF' else 'orange_lamp', ''))

for p, net in list(reserved.items()):
    if net is not None and p not in grid:
        cell(p, TYPE[net], net)


# ---- joining rules -----------------------------------------------------------------------------------------------------------------
def joins(t, c_dir_from_n, n):
    """If a cell of type t (ribbon, clip, thread, red, black) sits beside n, on n's side c_dir_from_n, what joins: [(n's net, how)]."""
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
        return [(n['net'], 'wire')] if t == 'thread' else ([(n['net'], 'chan')] if t == 'clip' else [])
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
            n = {'kind': TYPE[r], 'net': r, 'chan': None}
        for jn, how in joins(t, OPP[d], n):
            want = chan if how == 'chan' else net
            if jn != want and jn != 'direct':
                return False
    return True


def free(p, net):
    (x0, x1), (y0, y1), (z0, z1) = BOUNDS
    if not (x0 <= p[0] <= x1 and y0 <= p[1] <= y1 and z0 <= p[2] <= z1):
        return False
    if p in grid:
        return False
    r = reserved.get(p, 'free')
    return r == 'free' or r == net


def route(net):
    t = TYPE[net]
    cells = [p for p, v in grid.items() if (v.get('net') == net and v['kind'] != 'comp') or (v['kind'] == 'clip' and v['chan'] == net)]
    cells = [p for p in cells if grid[p]['kind'] != 'clip' or grid[p]['net'] == net or t != 'ribbon']
    if len(cells) < 2:
        return
    tree = {cells[0]}
    todo = set(cells[1:])

    def touching(p, q):
        # a cell of this net reaches a clip's tap only from a wire, a clip's bus only from a ribbon
        return True

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
            raise Blocked(net, sorted(todo)[:3])
        p = prev[hit]
        while p is not None and p not in tree:
            cell(p, t, net)
            tree.add(p)
            p = prev[p]
        tree.add(hit)
        todo.discard(hit)
        grow()


# the buses first (they're wide and stiff), then the threads, then power; a net that can't get through goes first next time, from a
# clean slate
class Blocked(Exception):
    pass


ORDER = RIBBONS + [n for n in THREADS if n not in ('B1', 'B2', 'B3', 'STB', 'PLUS', 'MINUS', 'EQ', 'BKS', 'DOT', 'CARRY')] + ['VDD', 'GND']
clean = (dict(grid), dict(reserved))
for attempt in range(60):
    grid.clear(); grid.update(clean[0])
    reserved.clear(); reserved.update(clean[1])
    try:
        for net in ORDER:
            route(net)
        break
    except Blocked as e:
        net = e.args[0]
        ORDER.remove(net)
        ORDER.insert(0, net)
        sys.stderr.write(f'attempt {attempt}: {net} blocked, first next time' + chr(10))
else:
    raise SystemExit('no order routes')


# ---- the check: rebuild every net from the grid, as the game will, and compare -----------------------------------------------------
def check():
    parent = {}

    def find(a):
        while parent.setdefault(a, a) != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a

    def union(a, b): parent[find(a)] = find(b)

    for p, v in grid.items():
        if v['kind'] == 'comp':
            continue
        t = v['kind']
        for d in DIRS:
            q = step(p, d)
            n = grid.get(q)
            if n is None:
                continue
            for jn, how in joins(t, OPP[d], n):
                mine = (p, 'chan') if how == 'chan' else (p, 'self')
                if n['kind'] == 'comp':
                    union(mine, ('pin', q, OPP[d]))
                else:
                    theirs = (q, 'chan') if n['kind'] == 'clip' and t not in ('ribbon', 'clip') else (q, 'self')
                    union(mine, theirs)
    # a clip's tap is its bus's channel: taps of the same channel on one bus are one node
    for p, v in grid.items():
        if v['kind'] == 'clip':
            union((p, 'chan'), ('channel', find((p, 'self')), v['channel']))
    sets = {}
    for pos, c in comps:
        for d, (net, kind) in c['pins'].items():
            if net in (None, 'direct'):
                continue
            sets.setdefault(net, set()).add(find(('pin', pos, d)))
    # and every clip's tap belongs to its net: a tap left unwired is a split too
    for p, v in grid.items():
        if v['kind'] == 'clip':
            sets.setdefault(v['chan'], set()).add(find((p, 'chan')))
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


def state(v):
    k = v['kind']
    if k == 'comp':
        return 'fourtwenty:' + v['block'], v['nbt']
    if k == 'ribbon':
        return 'fourtwenty:eighth_ribbon', ''
    if k == 'clip':
        return f'fourtwenty:roach_clip[channel={v["channel"]}]', ''
    if k == 'thread':
        return 'fourtwenty:hemp_thread', ''
    return f'fourtwenty:hemp_wick[colour={"red" if k == "red" else "black"}]', ''


machine = [(p, *state(v)) for p, v in grid.items()] + [(p, 'fourtwenty:' + s, n) for p, s, n in panel]

# ---- the game test's layout: every block of the machine relative to ORIGIN ----------------------------------------------------------
if len(sys.argv) > 1 and sys.argv[1] == 'java':
    NL = chr(10)
    lines = []
    for p, s, nbt in sorted(machine, key=lambda m: (m[0][1], m[0][0], m[0][2])):
        body = f'{p[0] - ORIGIN[0]} {p[1] - ORIGIN[1]} {p[2] - ORIGIN[2]} {s}{nbt}'.replace(chr(92), chr(92) * 2).replace('"', chr(92) + '"')
        lines.append(f'            "{body}",')
    keys = []
    for pos, (what, v) in TRAYS.items():
        if what == 'key':
            f = (pos[0], pos[1], pos[2] + 1)
            keys.append(f'        KEYS.put("{v}", new int[]{{{f[0] - ORIGIN[0]}, {f[1] - ORIGIN[1]}, {f[2] - ORIGIN[2]}}});')
    jx = [p for p, c in comps if c['name'] == 'JX'][0]
    digit = [p for p, (w, v) in TRAYS.items() if w == 'digit'][-1]
    print('package com.malevolentgods.fourtwenty.gametest;' + NL)
    print('import java.util.HashMap;')
    print('import java.util.Map;' + NL)
    print('/** The keypad calculator, from its flatworld build\'s router (tools/build_calculator.py): every block relative to its corner. Generated. */')
    print('final class CalculatorLayout {')
    print('    private CalculatorLayout() {}' + NL)
    print(f'    /** Register X (the entry, as shown) and the display\'s fed digit. */')
    print(f'    static final int[] JX = {{{jx[0] - ORIGIN[0]}, {jx[1] - ORIGIN[1]}, {jx[2] - ORIGIN[2]}}};')
    ovf = (KX, DISPLAY_Y, ZP + 1)
    print(f'    static final int[] OVERFLOW = {{{ovf[0] - ORIGIN[0]}, {ovf[1] - ORIGIN[1]}, {ovf[2] - ORIGIN[2]}}};')
    print('    static final Map<String, int[]> KEYS = new HashMap<>();' + NL)
    print('    static {')
    print(NL.join(keys))
    print('    }' + NL)
    print('    static final String[] BLOCKS = {')
    print(NL.join(lines))
    print('    };')
    print('}')
    sys.exit(0)

# ---- out: the machine (the building comes from tools/build_calculator_house.py, around it) -----------------------------------------
out = []
for p, s, nbt in machine:
    out.append(f'setblock {p[0]} {p[1]} {p[2]} {s}{nbt}')
sys.stderr.write(f'{len(grid)} cells, {len(panel)} panel parts, nets ok\n')
print('\n'.join(out))
