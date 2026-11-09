"""The keypad calculator on flatworld (Sensi Heights tier 5): a 4x4 keypad and a 5-digit display mounted on Rolling Trays in a lobby
wall, a Dankoder behind the keypad, and the logic in a sealed room behind, every wire routed by a router that refuses any cell where a
wire would touch something it mustn't, then an independent check that each net reaches exactly its own pins.

    python tools/build_calculator.py > cmds          the flatworld commands for the machine (tools/build_calculator_house.py: its building)
    python tools/build_calculator.py java > Layout   the machine alone, for the game test (CalculatorLayout.java)
    python tools/build_calculator.py both LAYOUT CMDS   both from one routing (it takes minutes)

The machine works in 16 bits, every register a pair of Jar Nugs and every adder a pair of Stacker Nugs, the low one's carry into the
high one's. Entries up to 9999 (a fifth digit is refused), sums up to 19998, differences shown as their size with a "negative" lamp.
Keys (Dankoder codes): digits 0-9; A +, B -, C clear, D =, E back (one step), F '.' (nothing).

Registers: X (the entry, shown), A (the first operand), P (X before the last digit, for back), and staging registers SD (X x 10 + the
digit), SA (A + X), S1 (A - X) and S2 (X - A), which with P share one bus into X: each lets go of it unless its key was pressed (and,
for = after -, unless it's the right one of A - X and X - A: S2 when A - X borrows), so +, - and C load 0. The key-down pulse, delayed
by Buffer Nugs, clocks the staging registers first (CLK1) and X after (CLK2). x 10 is three doublings and one more add (8x + 2x).
Two pairs of Scale Nugs watch X: past 999 a digit is refused, past 9999 + and - are; a refused key flashes the red lamp.
"""
import sys
from collections import deque

sys.stdout.reconfigure(encoding="utf-8")

Y = -59                         # the logic's level; GND on the floor below it, VDD lines above
FLOOR = -61                     # the building's floor, level with the ground
ZP = -14                        # the panel wall: its trays face south into the lobby
DIRS = {'E': (1, 0, 0), 'W': (-1, 0, 0), 'S': (0, 0, 1), 'N': (0, 0, -1), 'U': (0, 1, 0), 'D': (0, -1, 0)}
OPP = {'E': 'W', 'W': 'E', 'S': 'N', 'N': 'S', 'U': 'D', 'D': 'U'}


def COL(k): return 306 + 5 * k


def ROW(r): return -18 - 6 * r


# the arithmetic: its own, roomier grid behind the gates (16-bit buses crowd)
def AP(k, r): return (306 + 6 * k, Y, ROW(6) - 6 * (r + 1))


BACK = AP(0, 4)[2] - 3
BOUNDS = ((300, 352), (Y - 1, Y + 11), (BACK, ZP - 1))
ORIGIN = (299, FLOOR, BACK - 2)        # the game test's layout is relative to this (the back wall's corner)

TYPE = {}                       # net -> 'ribbon', 'thread', 'lime' (a lime Hemp Thread: it joins only lime), 'red', 'black'
TYPE['VDD'], TYPE['GND'] = 'red', 'black'
WIRES = ('thread', 'lime', 'red', 'black')

grid = {}                       # (x, y, z) -> dict(kind, net, ...)
reserved = {}                   # (x, y, z) -> net, or None to be kept empty
comps = []
panel = []                      # (pos, block state, nbt): the panel's parts, not routed


def net(name, kind='thread'):
    TYPE.setdefault(name, kind)
    return name


def step(p, d):
    dx, dy, dz = DIRS[d]
    return (p[0] + dx, p[1] + dy, p[2] + dz)


def comp(name, block, pos, pins, nbt=''):
    """pins: {direction: (net, 'single'|'bus')}; a None net keeps that face's cell empty."""
    c = {'kind': 'comp', 'name': name, 'block': block, 'pins': pins, 'nbt': nbt}
    grid[pos] = c
    comps.append((pos, c))
    for d, (n, kind) in pins.items():
        cell = step(pos, d)
        if n is None:
            reserved[cell] = None
        elif cell not in grid:
            reserved.setdefault(cell, n)
    return c


def cell(pos, kind, n, chan=None, channel=None):
    grid[pos] = {'kind': kind, 'net': n, 'chan': chan, 'channel': channel}


def clip(pos, bus, channel, chan):
    """A Roach Clip on {bus}, tapping {channel} out to the single net {chan}."""
    net(bus, 'ribbon')
    net(chan)
    cell(pos, 'clip', bus, chan, channel)
    reserved.pop(pos, None)


def chip(name, block, pos, pins, nbt=''):
    """A chip facing east: front E, back W, left S, right N, VDD top, GND bottom (its riser up to the VDD lines is reserved). A pin
    given as ('tap', bus, channel, name) is a Roach Clip on that bus, on that face, its channel the pin's net."""
    out = {}
    taps = []
    for d, spec in pins.items():
        if isinstance(spec, tuple) and spec and spec[0] == 'tap':
            _, bus, ch, n = spec
            taps.append((step(pos, d), bus, ch, n))
            out[d] = (net(n), 'single')
        else:
            out[d] = spec
            if spec[0] is not None:
                net(spec[0], 'ribbon' if spec[1] == 'bus' else 'thread')
    out['U'] = ('VDD', 'single')
    out['D'] = ('GND', 'single')
    comp(name, block + '[facing=east]', pos, out, nbt)
    for t in taps:
        clip(*t)
    for dy in (1, 2):
        reserved[(pos[0], pos[1] + dy, pos[2])] = 'VDD'


def bus(n): return (n, 'bus')


def one(n): return (n, 'single')


NONE_BUS, NONE_ONE = (None, 'bus'), (None, 'single')


def gate(name, kind, k, r, a, b, out):
    """A two-input Nug at column k, row r: a on its left (S), b on its right (N), out in front (E)."""
    chip(name, kind, (COL(k), Y, ROW(r)), {'S': a if isinstance(a, tuple) else one(a), 'N': b if isinstance(b, tuple) else one(b), 'E': one(out)})


def inv(name, k, r, a, out, kind='not_nug'):
    chip(name, kind, (COL(k), Y, ROW(r)), {'W': a if isinstance(a, tuple) else one(a), 'E': one(out)})


def add16(name, r, pair, aL, aH, bL, bH, outL, outH, subtract=False):
    """A 16-bit adder (or subtractor), pair {pair} of arithmetic row {r}: two Stacker Nugs, the low one's carry (or borrow) wire into
    the high one's carry in."""
    nbt = '{value:1}' if subtract else ''
    chip(name + 'L', 'stacker_nug', AP(2 * pair, r), {'W': bus(aL), 'S': bus(bL) if bL else NONE_BUS, 'N': NONE_ONE, 'E': bus(outL)}, nbt)
    chip(name + 'H', 'stacker_nug', AP(2 * pair + 1, r), {'W': bus(aH), 'S': bus(bH) if bH else NONE_BUS, 'N': ('tap', outL, 8, name + 'c'),
                                                           'E': bus(outH)}, nbt)


def jar16(name, r, pair, inL, inH, clk, rel, outL, outH):
    """A 16-bit register, pair {pair} of arithmetic row {r}: two Jar Nugs on one clock and one release."""
    for suffix, k, i, o in (('L', 2 * pair, inL, outL), ('H', 2 * pair + 1, inH, outH)):
        chip(name + suffix, 'jar_nug', AP(k, r), {'W': bus(i), 'S': one(clk), 'N': one(rel) if rel else NONE_ONE, 'E': bus(o)})


def const(name, k, r, value):
    """A constant byte into the B face (S) of the chip at column k, row r: a column of Roach Clips up from that face, one per set
    channel, each tapping VDD (the chip's own VDD riser touches the column)."""
    chans = [c for c in range(8) if value >> c & 1]
    x, z = COL(k), ROW(r) + 1
    for i, c in enumerate(chans):
        clip((x, Y + i, z), name, c, 'VDD')
    reserved[(x, Y + len(chans), z)] = None


# ---- the parts ---------------------------------------------------------------------------------------------------------------------
# the key-down pulse, delayed: six Buffers to CLK1, six more to CLK2
for i in range(6):
    chip(f'BUF{i}', 'buffer_nug', (306 + i, Y, ROW(0)), {'W': ('tap', 'K', 8, 'STB') if i == 0 else one('direct'), 'E': one('CLK1' if i == 5 else 'direct')})
    chip(f'BUF{6 + i}', 'buffer_nug', (330 + i, Y, ROW(0)), {'W': one('CLK1' if i == 0 else 'direct'), 'E': one('CLK2' if i == 5 else 'direct')})
for x in list(range(307, 312)) + list(range(331, 336)):
    reserved.pop((x, Y, ROW(0)), None)
# decoding: a command is code bit 3 and (bit 1 or bit 2); a digit isn't one. The Dealer picks out which command.
gate('OR1', 'or_nug', 0, 1, ('tap', 'K', 1, 'B1'), ('tap', 'K', 2, 'B2'), 'n1')
gate('AND1', 'and_nug', 1, 1, ('tap', 'K', 3, 'B3'), 'n1', 'CMD')
inv('NOT1', 2, 1, 'CMD', 'DIG')
inv('NOT3', 3, 1, 'DIG', 'RSD')
gate('AND2', 'and_nug', 4, 1, 'CLK1', 'DIG', 'CLKP')
chip('DEAL', 'dealer_nug', (COL(5), Y, ROW(1)), {'W': ('tap', 'K', 3, 'B3'), 'S': bus('K'), 'E': bus('DEC')})
gate('OR2', 'or_nug', 6, 1, ('tap', 'DEC', 3, 'MINUS'), ('tap', 'DEC', 2, 'PLUS'), 'OPK')
net('OP', 'lime')
gate('LATCH', 'latch_nug', 7, 1, ('tap', 'DEC', 3, 'MINUS'), ('tap', 'DEC', 2, 'PLUS'), 'OP')
inv('NOTEFF', 8, 1, 'EFF', 'NEFF')
# clocks, gated by a refusal
gate('AND3', 'and_nug', 0, 2, 'CLK1', 'OPK', 'CLKA0')
gate('ANDA', 'and_nug', 1, 2, 'CLKA0', 'NREF', 'CLKA')
inv('NOT2', 2, 2, ('tap', 'DEC', 7, 'PM'), 'NPM')
# CLKS: any key not refused (the sign's clock); CLKX: that, but not +/- (which flips only the sign)
gate('AND4', 'and_nug', 3, 2, 'CLK2', 'NREF', 'CLKS')
gate('ANDX', 'and_nug', 4, 2, 'CLKS', 'NPM', 'CLKX')
inv('NOT5', 5, 2, ('tap', 'DEC', 6, 'BKS'), 'RP')
# which result = takes, on the sizes: their sum when the signs and the operation make it one (EFF 0), else the larger less the smaller
# (A - X, or X - A if A - X borrows: NEG)
gate('EQOP', 'and_nug', 6, 2, ('tap', 'DEC', 5, 'EQ'), 'EFF', 'EQOP')
gate('RELA', 'nand_nug', 7, 2, ('tap', 'DEC', 5, 'EQ'), 'NEFF', 'RSA')
inv('NOTNEG', 8, 2, 'NEG', 'NNEG')
gate('REL1', 'nand_nug', 0, 3, 'EQOP', 'NNEG', 'RS1')
gate('SELNEG', 'and_nug', 1, 3, 'EQOP', 'NEG', 'SELNEG')
inv('NOTS2', 2, 3, 'SELNEG', 'RS2')
# X's sign (SX), a flip-flop beside X: kept by a digit or back, flipped by +/-, the result's on =, else (C, +, -) cleared
gate('SXFF', 'flip_flop_nug', 3, 3, 'SXD', 'CLKS', 'SX')
# refusals: a digit past 999, + or - past 9999; a refused key flashes the red lamp
gate('R1', 'and_nug', 4, 3, 'DIG', 'BIG4', 'r1')
gate('R2', 'and_nug', 5, 3, 'OPK', 'BIG5', 'r2')
gate('REF', 'or_nug', 6, 3, 'r1', 'r2', 'REF')
inv('NREF', 7, 3, 'REF', 'NREF')
# X >= 1000 (0x03E8) and X >= 10000 (0x2710): high byte greater, or equal and the low byte not less
for i, (big, hi, lo, k0) in enumerate((('BIG4', 0x03, 0xE8, 0), ('BIG5', 0x27, 0x10, 5))):
    sh, sl = f'SH{i}', f'SL{i}'
    chip('SCH' + big, 'scale_nug', (COL(k0), Y, ROW(4)), {'W': bus('XH'), 'S': bus(f'C{hi:02X}'), 'E': bus(sh)})
    const(f'C{hi:02X}', k0, 4, hi)
    chip('SCL' + big, 'scale_nug', (COL(k0 + 1), Y, ROW(4)), {'W': bus('XL'), 'S': bus(f'C{lo:02X}'), 'E': bus(sl)})
    const(f'C{lo:02X}', k0 + 1, 4, lo)
    inv('GE' + big, k0 + 2, 4, ('tap', sl, 0, 'LT' + big), 'GE' + big)
    gate('T' + big, 'and_nug', k0 + 3, 4, ('tap', sh, 1, 'EQ' + big), 'GE' + big, 'T' + big)
    kk, rr = (k0 + 4, 4) if k0 + 4 <= 8 else (0, 5)
    gate(big, 'or_nug', kk, rr, ('tap', sh, 2, 'GT' + big), 'T' + big, big)
# the signs: EFF (subtract the sizes) = A's sign xor X's xor the operation; the result's sign = A's, flipped if X - A was taken
gate('XSA', 'xor_nug', 1, 5, 'SA', 'SX', 'SAX')
gate('XEFF', 'xor_nug', 2, 5, 'SAX', 'OP', 'EFF')
gate('EN', 'and_nug', 3, 5, 'EFF', 'NEG', 'EN')
gate('RSG', 'xor_nug', 4, 5, 'SA', 'EN', 'RSIGN')
gate('KEEP', 'or_nug', 5, 5, 'DIG', ('tap', 'DEC', 6, 'BKS'), 'KEEP')
gate('K1', 'and_nug', 6, 5, 'KEEP', 'SX', 'K1')
inv('NSX', 7, 5, 'SX', 'NSX')
gate('F1', 'and_nug', 0, 6, ('tap', 'DEC', 7, 'PM'), 'NSX', 'F1')
gate('Q1', 'and_nug', 1, 6, ('tap', 'DEC', 5, 'EQ'), 'RSIGN', 'Q1')
gate('O1', 'or_nug', 2, 6, 'K1', 'F1', 'O1')
gate('SXD', 'or_nug', 3, 6, 'O1', 'Q1', 'SXD')
# A's sign: X's, taken when A takes X
gate('SAFF', 'flip_flop_nug', 4, 6, 'SX', 'CLKA', 'SA')
# arithmetic: x 10 as 2x, 4x, 8x, 8x + 2x; then + the digit (the code bus, into the low byte)
add16('S1', 0, 0, 'XL', 'XH', 'XL', 'XH', 'X2L', 'X2H')
add16('S2', 0, 1, 'X2L', 'X2H', 'X2L', 'X2H', 'X4L', 'X4H')
add16('S3', 0, 2, 'X4L', 'X4H', 'X4L', 'X4H', 'X8L', 'X8H')
add16('S4', 1, 0, 'X8L', 'X8H', 'X2L', 'X2H', 'X10L', 'X10H')
add16('S5', 1, 1, 'X10L', 'X10H', 'K', None, 'D10L', 'D10H')
jar16('JSD', 1, 2, 'D10L', 'D10H', 'CLK1', 'RSD', 'SL', 'SH')
jar16('JP', 2, 0, 'XL', 'XH', 'CLKP', 'RP', 'SL', 'SH')
jar16('JX', 2, 1, 'SL', 'SH', 'CLKX', None, 'XL', 'XH')
jar16('JA', 2, 2, 'XL', 'XH', 'CLKA', None, 'AL', 'AH')
add16('ADD', 3, 0, 'AL', 'AH', 'XL', 'XH', 'RAL', 'RAH')
add16('SUB1', 3, 1, 'AL', 'AH', 'XL', 'XH', 'R1L', 'R1H', subtract=True)
add16('SUB2', 3, 2, 'XL', 'XH', 'AL', 'AH', 'R2L', 'R2H', subtract=True)
jar16('JSA', 4, 0, 'RAL', 'RAH', 'CLK1', 'RSA', 'SL', 'SH')
jar16('JS1', 4, 1, 'R1L', 'R1H', 'CLK1', 'RS1', 'SL', 'SH')
jar16('JS2', 4, 2, 'R2L', 'R2H', 'CLK1', 'RS2', 'SL', 'SH')
# A - X borrows: its borrow wire, tapped where it goes into S1's high Jar
clip(step(AP(3, 4), 'W'), 'R1H', 8, 'NEG')
# the display's high byte, its carry wire set while the result is negative: the Numb Nugs draw a minus in front
SIGN_AT = (316, Y, ROW(0))      # near the display (deeper in, the room's too crowded to route its buses)
chip('SIGN', 'blend_nug', SIGN_AT, {'W': bus('XH'), 'S': bus('MASKS'), 'E': bus('DH')}, '{value:2}')
clip(step(SIGN_AT, 'S'), 'MASKS', 8, 'SX')
# the supply: a Bottomless Stash in the room's far corner
comp('STASH', 'bottomless_stash_puff[facing=north]', (COL(8), Y, ROW(5)),
     {'E': one('VDD'), 'D': one('GND'), 'W': NONE_ONE, 'N': NONE_ONE, 'S': NONE_ONE, 'U': NONE_ONE})

# ---- the panel: trays in the wall at ZP facing south; keys at y -59 to -56, x 320 to 323; the display row at -55 -------------------
KX, KY = 320, -59
LAYOUT = [['7', '8', '9', 'C'], ['4', '5', '6', '-'], ['1', '2', '3', '+'], ['<', '0', '±', '=']]
NUMBER = {**{str(d): d for d in range(10)}, '+': 10, '-': 11, 'C': 12, '=': 13, '<': 14, '±': 15}
LABEL = {'<': '←', '-': '−'}
# glows (dye ids): digits white, + and - lime, = green, C red, back orange, '.' gray
GLOW = {**{str(d): 0 for d in range(10)}, '+': 5, '-': 5, '=': 13, 'C': 14, '<': 1, '±': 5}
DISPLAY_Y = KY + 4
TRAYS = {}
for r, row in enumerate(LAYOUT):
    for c, key in enumerate(row):
        TRAYS[(KX + c, KY + 3 - r, ZP)] = ('key', key)
# five digits, x 319 to 323: the low byte fed at the right, the high byte two to its left; lamps: overflow left, negative and minus right
for c in range(5):
    TRAYS[(KX - 1 + c, DISPLAY_Y, ZP)] = ('digit', c)
FEED = {(KX + 3, DISPLAY_Y, ZP): bus('XL'), (KX + 1, DISPLAY_Y, ZP): bus('DH'), (KX + 2, DISPLAY_Y, ZP): one('VDD'), (KX, DISPLAY_Y, ZP): one('GND')}
# lime lamp trim either side of the display, flat on the wall: decoration
# behind the panel nothing but its feeds and the Dankoder: a wire on a tray's back would be its signal or its power
for x in range(KX - 5, KX + 9):
    for y in range(Y - 1, Y + 8):
        reserved[(x, y, ZP - 1)] = None
for pos, (what, v) in TRAYS.items():
    comp('tray', 'rolling_tray[facing=south]', pos, {'N': FEED.get(pos, NONE_ONE)})
    if pos in FEED:
        reserved[step(pos, 'N')] = FEED[pos][0]
# the Dankoder behind the bottom-left key: its back on that tray, its front (north) the code bus
DK = (KX, KY, ZP - 1)
comp('DANK', 'dankoder[facing=north]', DK, {'N': bus('K'), 'U': NONE_ONE, 'D': NONE_ONE, 'E': NONE_BUS, 'W': NONE_BUS})
for pos, (what, v) in TRAYS.items():
    front = (pos[0], pos[1], pos[2] + 1)
    if what == 'key':
        panel.append((front, 'puff_button[facing=south,mounted=true]', f'{{value:{NUMBER[v] + 1},label:"{LABEL.get(v, v)}",glow:{GLOW[v]}}}'))
    elif what == 'digit':
        panel.append((front, 'numb_nug[facing=south,mounted=true]', ''))
for x in (KX - 2, KX + 4, KX + 5):
    panel.append(((x, DISPLAY_Y, ZP + 1), 'lamp_trim_pin_lim_pin[facing=north]', ''))

for p, n in list(reserved.items()):
    if n is not None and p not in grid:
        cell(p, TYPE[n], n)


# ---- joining rules -----------------------------------------------------------------------------------------------------------------
def joins(t, c_dir_from_n, n):
    """If a cell of type t (ribbon, clip, thread, lime, red, black) sits beside n, on n's side c_dir_from_n, what joins: [(n's net, how)]."""
    k = n['kind']
    if k == 'comp':
        pin = n['pins'].get(c_dir_from_n)
        if not pin or pin[0] is None:
            return []
        n_, kind = pin
        if kind == 'bus' and t in ('ribbon', 'clip'):
            return [(n_, 'bus')]
        if kind == 'single' and (t in WIRES or t == 'clip'):
            return [(n_, 'chan' if t == 'clip' else 'wire')]
        return []
    if k == 'ribbon':
        return [(n['net'], 'bus')] if t in ('ribbon', 'clip') else []
    if k == 'clip':
        if t in ('ribbon', 'clip'):
            return [(n['net'], 'bus')]
        return [(n['chan'], 'wire')]
    if k in WIRES:
        if t == k:
            return [(n['net'], 'wire')]
        return [(n['net'], 'chan')] if t == 'clip' else []
    return []


def ok(p, n_, t, chan=None):
    for d in DIRS:
        q = step(p, d)
        n = grid.get(q)
        if n is None:
            r = reserved.get(q, 'free')
            if r == 'free' or r is None:
                continue
            n = {'kind': TYPE[r], 'net': r, 'chan': None}
        for jn, how in joins(t, OPP[d], n):
            want = chan if how == 'chan' else n_
            if jn != want and jn != 'direct':
                return False
    return True


def free(p, n_):
    (x0, x1), (y0, y1), (z0, z1) = BOUNDS
    if not (x0 <= p[0] <= x1 and y0 <= p[1] <= y1 and z0 <= p[2] <= z1):
        return False
    if p in grid:
        return False
    r = reserved.get(p, 'free')
    return r == 'free' or r == n_


class Blocked(Exception):
    pass


def route(n_):
    t = TYPE[n_]
    cells = [p for p, v in grid.items() if (v.get('net') == n_ and v['kind'] != 'comp' and (v['kind'] != 'clip' or t == 'ribbon'))
             or (v['kind'] == 'clip' and v['chan'] == n_)]
    if len(cells) < 2:
        return
    # taps only (clips each beside its own pin), all of one bus and channel: already one node
    if all(grid[p]['kind'] == 'clip' and grid[p]['chan'] == n_ for p in cells) and len({(grid[p]['net'], grid[p]['channel']) for p in cells}) == 1:
        return
    tree = {cells[0]}
    todo = set(cells[1:])

    def tap(p):
        # a clip whose channel this (single) net is: a wire beside it reaches that channel, another clip beside it doesn't
        return t != 'ribbon' and grid.get(p, {}).get('kind') == 'clip'

    def grow():
        changed = True
        while changed:
            changed = False
            for p in list(todo):
                if any(step(p, d) in tree and not (tap(p) and tap(step(p, d))) for d in DIRS):
                    tree.add(p)
                    todo.discard(p)
                    changed = True
    grow()
    while todo:
        # cheapest path, not shortest: a cell on the chips' own level or the ground plane costs more, so wires climb into the room
        # above and leave the chips' faces free for the wires that must reach them
        import heapq
        prev = {p: None for p in tree}
        dist = {p: 0 for p in tree}
        heap = [(0, p) for p in tree]
        heapq.heapify(heap)
        hit = None
        while heap:
            dp, p = heapq.heappop(heap)
            if dp > dist.get(p, 1 << 30):
                continue
            if p in todo and p not in tree:
                hit = p
                break
            if p in todo:
                continue
            for d in DIRS:
                q = step(p, d)
                if q in todo and q not in prev:
                    if tap(p) and tap(q):
                        continue
                    nd = dp + 1
                elif free(q, n_) and ok(q, n_, t):
                    nd = dp + (4 if q[1] <= Y else 1)
                else:
                    continue
                if nd < dist.get(q, 1 << 30):
                    dist[q] = nd
                    prev[q] = p
                    heapq.heappush(heap, (nd, q))
        if not hit:
            raise Blocked(n_, sorted(todo)[:3])
        p = prev[hit]
        while p is not None and p not in tree:
            cell(p, t, n_)
            tree.add(p)
            p = prev[p]
        tree.add(hit)
        todo.discard(hit)
        grow()


# the buses first (they're wide and stiff), then the threads, then power; a net that can't get through goes first next time, from a
# clean slate
ORDER = [n for n, t in TYPE.items() if t == 'ribbon'] + [n for n, t in TYPE.items() if t in ('thread', 'lime')] + ['VDD', 'GND']
ORDER = [n for n in ORDER if n != 'direct']
clean = (dict(grid), dict(reserved))
for attempt in range(300):
    grid.clear(); grid.update(clean[0])
    reserved.clear(); reserved.update(clean[1])
    try:
        for n in ORDER:
            route(n)
        break
    except Blocked as e:
        n = e.args[0]
        ORDER.remove(n)
        # the rest a little shuffled too (seeded, so a build is repeatable): moving only the stuck net first can go round in circles
        import random
        rng = random.Random(attempt)
        for i in range(len(ORDER) - 1):
            if rng.random() < 0.15:
                j = min(len(ORDER) - 1, i + rng.randint(1, 4))
                ORDER[i], ORDER[j] = ORDER[j], ORDER[i]
        ORDER.insert(0, n)
        sys.stderr.write(f'attempt {attempt}: {n} blocked, first next time' + chr(10))
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
        for d, (n, kind) in c['pins'].items():
            if n in (None, 'direct'):
                continue
            sets.setdefault(n, set()).add(find(('pin', pos, d)))
    # and every clip's tap belongs to its net: a tap left unwired is a split too
    for p, v in grid.items():
        if v['kind'] == 'clip':
            sets.setdefault(v['chan'], set()).add(find((p, 'chan')))
    bad = {n: len(s) for n, s in sets.items() if len(s) != 1}
    roots = {}
    for n, s in sets.items():
        for r in s:
            roots.setdefault(r, set()).add(n)
    merged = [ns for ns in roots.values() if len(ns) > 1]
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
    if k == 'lime':
        return 'fourtwenty:hemp_thread[colour=lime]', ''
    return f'fourtwenty:hemp_wick[colour={"red" if k == "red" else "black"}]', ''


machine = [(p, *state(v)) for p, v in grid.items()] + [(p, 'fourtwenty:' + s, n) for p, s, n in panel]


def rel(p): return f'{{{p[0] - ORIGIN[0]}, {p[1] - ORIGIN[1]}, {p[2] - ORIGIN[2]}}}'


# ---- the game test's layout: every block of the machine relative to ORIGIN ----------------------------------------------------------
BOTH = len(sys.argv) > 3 and sys.argv[1] == 'both'     # both LAYOUT.java CMDS: one routing, both files
if BOTH:
    sys.stdout = open(sys.argv[2], 'w', encoding='utf-8')
if len(sys.argv) > 1 and sys.argv[1] in ('java', 'both'):
    NL = chr(10)
    lines = []
    for p, s, nbt in sorted(machine, key=lambda m: (m[0][1], m[0][0], m[0][2])):
        body = f'{p[0] - ORIGIN[0]} {p[1] - ORIGIN[1]} {p[2] - ORIGIN[2]} {s}{nbt}'.replace(chr(92), chr(92) * 2).replace('"', chr(92) + '"')
        lines.append(f'            "{body}",')
    keys = []
    for pos, (what, v) in TRAYS.items():
        if what == 'key':
            keys.append(f'        KEYS.put("{v}", new int[]{rel((pos[0], pos[1], pos[2] + 1))});')
    named = {c['name']: p for p, c in comps}
    print('package com.malevolentgods.fourtwenty.gametest;' + NL)
    print('import java.util.HashMap;')
    print('import java.util.Map;' + NL)
    print('/** The keypad calculator, from its flatworld build\'s router (tools/build_calculator.py): every block relative to its corner. Generated. */')
    print('final class CalculatorLayout {')
    print('    private CalculatorLayout() {}' + NL)
    print('    /** Register X\'s two Jar Nugs (the entry, as shown), low and high byte; the lamps. */')
    print(f'    static final int[] XL = {rel(named["JXL"])}, XH = {rel(named["JXH"])};')
    print(f'    static final int[] SIGN = {rel(named["SIGN"])};')
    print('    static final Map<String, int[]> KEYS = new HashMap<>();' + NL)
    print('    static {')
    print(NL.join(keys))
    print('    }' + NL)
    print('    static final String[] BLOCKS = {')
    print(NL.join(lines))
    print('    };')
    print('}')
    if not BOTH:
        sys.exit(0)
    sys.stdout.close()
    sys.stdout = open(sys.argv[3], 'w', encoding='utf-8')

# ---- out: the machine (the building comes from tools/build_calculator_house.py, around it) -----------------------------------------
out = [f'setblock {p[0]} {p[1]} {p[2]} {s}{nbt}' for p, s, nbt in machine]
sys.stderr.write(f'{len(grid)} cells, {len(panel)} panel parts, {sum(1 for _, c in comps if c["name"] != "tray")} parts, nets ok' + chr(10))
print(chr(10).join(out))
