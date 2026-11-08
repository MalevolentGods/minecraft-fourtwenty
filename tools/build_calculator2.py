"""The Hall of Operations v2 calculator (docs/electronics/hall-of-operations-v2.md), laid out flat as a circuit board by tools/pcb.py:
every chip on one level, every signal a coloured trace beside it, crossings by Nah, Skip Me, Bro.

    python tools/build_calculator2.py X0 Z0 > cmds            the machine, its keypad's bottom-left key at (X0, -59, Z0)
    python tools/build_calculator2.py X0 Z0 java > Layout     the same, for the game test (relative to its corner)

Stage 2 of the plan: the v1 machine (keypad, + and -, signs, back, clear, 5-digit display) on the flat board, to prove the router.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.stdout.reconfigure(encoding='utf-8')
from pcb import Board, step  # noqa: E402

KX, KZ = int(sys.argv[1]), int(sys.argv[2])
Y = -59
ZP = KZ - 1                 # the panel wall: trays in it, facing south; keys in front of it
SP = 8                      # chip spacing on the board: a bus pin's neighbours are no-go for other Ribbons, so 7 leaves two clear lanes
COLS = 10
X0 = KX - 30                # the board's west edge
Z1 = ZP - 7                 # its first row, clear of the panel and the Dankoder
b = Board(Y, bounds=((X0 - 3, X0 + SP * COLS + 2), (Y - 1, Y + 7), (Z1 - SP * 12, ZP - 1)),
          feed=((KX - 4, KX + 8), (Y - 1, Y + 6), (ZP - 3, ZP - 1)))
one, bus = b.one, b.bus
NONE1, NONEB = (None, 'single'), (None, 'bus')

slots = iter((X0 + SP * (i % COLS), Y, Z1 - SP * (i // COLS)) for i in range(10 ** 4))


def place():
    return next(slots)


def pin(spec):
    return spec if isinstance(spec, tuple) else one(spec)


def chip(name, block, pins, nbt=''):
    """A chip facing east at the next slot (front E, back W, left S, right N). A pin ('tap', bus, ch, net) is a Roach Clip on that face."""
    pos = place()
    out, taps = {}, []
    for d, spec in pins.items():
        if isinstance(spec, tuple) and spec[0] == 'tap':
            _, bn, ch, n = spec
            taps.append((step(pos, d), bn, ch, n))
            out[d] = one(n)
        else:
            out[d] = pin(spec)
    b.chip(name, block, pos, out, nbt)
    for t in taps:
        b.clip(*t)
    return pos


def gate(name, kind, a, c, out): return chip(name, kind, {'S': a, 'N': c, 'E': out})


def inv(name, a, out, kind='not_nug'): return chip(name, kind, {'W': a, 'E': out})


def add16(name, aL, aH, bL, bH, outL, outH, subtract=False):
    nbt = '{value:1}' if subtract else ''
    chip(name + 'L', 'stacker_nug', {'W': bus(aL), 'S': bus(bL) if bL else NONEB, 'N': NONE1, 'E': bus(outL)}, nbt)
    chip(name + 'H', 'stacker_nug', {'W': bus(aH), 'S': bus(bH) if bH else NONEB, 'N': ('tap', outL, 8, name + 'c'), 'E': bus(outH)}, nbt)


def jar16(name, inL, inH, clk, rel, outL, outH):
    for s, i, o in (('L', inL, outL), ('H', inH, outH)):
        chip(name + s, 'jar_nug', {'W': bus(i), 'S': one(clk), 'N': one(rel) if rel else NONE1, 'E': bus(o)})


def const(pos, name, value):
    """A constant byte into the B face (S) of the chip at pos: Roach Clips in a column up from that face, each tapping VDD."""
    chans = [c for c in range(8) if value >> c & 1]
    x, y, z = step(pos, 'S')
    for i, c in enumerate(chans):
        b.clip((x, y + i, z), name, c, 'VDD')


# ---- the machine (v1's logic) ------------------------------------------------------------------------------------------------------
# the key-down pulse, delayed: six Buffers to CLK1, six more to CLK2, each chain in a row of its own
for chain, (src, dst) in enumerate((('STB', 'CLK1'), ('CLK1', 'CLK2'))):
    x0, z = X0 + SP * COLS + 6, Z1 - 3 * chain
    for i in range(6):
        w = ('tap', 'K', 8, 'STB') if (chain == 0 and i == 0) else one(src if i == 0 else 'direct')
        e = one(dst if i == 5 else 'direct')
        pos = (x0 + i, Y, z)
        b.chip(f'BUF{chain}{i}', 'buffer_nug', pos, {'W': w if not isinstance(w, tuple) or w[0] != 'tap' else one('STB'), 'E': e})
        if isinstance(w, tuple) and w[0] == 'tap':
            b.clip(step(pos, 'W'), 'K', 8, 'STB')
b.bounds = ((X0 - 3, X0 + SP * COLS + 14), b.bounds[1], b.bounds[2])

gate('OR1', 'or_nug', ('tap', 'K', 1, 'B1'), ('tap', 'K', 2, 'B2'), 'n1')
gate('AND1', 'and_nug', ('tap', 'K', 3, 'B3'), 'n1', 'CMD')
inv('NOT1', 'CMD', 'DIG')
inv('NOT3', 'DIG', 'RSD')
gate('AND2', 'and_nug', 'CLK1', 'DIG', 'CLKP')
chip('DEAL', 'dealer_nug', {'W': ('tap', 'K', 3, 'B3'), 'S': bus('K'), 'E': bus('DEC')})
gate('OR2', 'or_nug', ('tap', 'DEC', 3, 'MINUS'), ('tap', 'DEC', 2, 'PLUS'), 'OPK')
gate('LATCH', 'latch_nug', ('tap', 'DEC', 3, 'MINUS'), ('tap', 'DEC', 2, 'PLUS'), 'OP')
inv('NOTEFF', 'EFF', 'NEFF')
gate('AND3', 'and_nug', 'CLK1', 'OPK', 'CLKA0')
gate('ANDA', 'and_nug', 'CLKA0', 'NREF', 'CLKA')
inv('NOT2', ('tap', 'DEC', 7, 'PM'), 'NPM')
gate('AND4', 'and_nug', 'CLK2', 'NREF', 'CLKS')
gate('ANDX', 'and_nug', 'CLKS', 'NPM', 'CLKX')
inv('NOT5', ('tap', 'DEC', 6, 'BKS'), 'RP')
gate('EQOP', 'and_nug', ('tap', 'DEC', 5, 'EQ'), 'EFF', 'EQOP')
gate('RELA', 'nand_nug', ('tap', 'DEC', 5, 'EQ'), 'NEFF', 'RSA')
inv('NOTNEG', 'NEG', 'NNEG')
gate('REL1', 'nand_nug', 'EQOP', 'NNEG', 'RS1')
gate('SELNEG', 'and_nug', 'EQOP', 'NEG', 'SELNEG')
inv('NOTS2', 'SELNEG', 'RS2')
gate('SXFF', 'flip_flop_nug', 'SXD', 'CLKS', 'SX')
gate('R1', 'and_nug', 'DIG', 'BIG4', 'r1')
gate('R2', 'and_nug', 'OPK', 'BIG5', 'r2')
gate('REF', 'or_nug', 'r1', 'r2', 'REF')
inv('NREF', 'REF', 'NREF')
for big, hi, lo in (('BIG4', 0x03, 0xE8), ('BIG5', 0x27, 0x10)):
    sh, sl = 'SH' + big, 'SL' + big
    p = chip('SCH' + big, 'scale_nug', {'W': bus('XH'), 'S': bus(f'C{hi:02X}'), 'E': bus(sh)})
    const(p, f'C{hi:02X}', hi)
    p = chip('SCL' + big, 'scale_nug', {'W': bus('XL'), 'S': bus(f'C{lo:02X}'), 'E': bus(sl)})
    const(p, f'C{lo:02X}', lo)
    inv('GE' + big, ('tap', sl, 0, 'LT' + big), 'GE' + big)
    gate('T' + big, 'and_nug', ('tap', sh, 1, 'EQ' + big), 'GE' + big, 'T' + big)
    gate(big, 'or_nug', ('tap', sh, 2, 'GT' + big), 'T' + big, big)
gate('XSA', 'xor_nug', 'SA', 'SX', 'SAX')
gate('XEFF', 'xor_nug', 'SAX', 'OP', 'EFF')
gate('EN', 'and_nug', 'EFF', 'NEG', 'EN')
gate('RSG', 'xor_nug', 'SA', 'EN', 'RSIGN')
gate('KEEP', 'or_nug', 'DIG', ('tap', 'DEC', 6, 'BKS'), 'KEEP')
gate('K1', 'and_nug', 'KEEP', 'SX', 'K1')
inv('NSX', 'SX', 'NSX')
gate('F1', 'and_nug', ('tap', 'DEC', 7, 'PM'), 'NSX', 'F1')
gate('Q1', 'and_nug', ('tap', 'DEC', 5, 'EQ'), 'RSIGN', 'Q1')
gate('O1', 'or_nug', 'K1', 'F1', 'O1')
gate('SXD', 'or_nug', 'O1', 'Q1', 'SXD')
gate('SAFF', 'flip_flop_nug', 'SX', 'CLKA', 'SA')
add16('S1', 'XL', 'XH', 'XL', 'XH', 'X2L', 'X2H')
add16('S2', 'X2L', 'X2H', 'X2L', 'X2H', 'X4L', 'X4H')
add16('S3', 'X4L', 'X4H', 'X4L', 'X4H', 'X8L', 'X8H')
add16('S4', 'X8L', 'X8H', 'X2L', 'X2H', 'X10L', 'X10H')
add16('S5', 'X10L', 'X10H', 'K', None, 'D10L', 'D10H')
jar16('JSD', 'D10L', 'D10H', 'CLK1', 'RSD', 'SL', 'SH')
jar16('JP', 'XL', 'XH', 'CLKP', 'RP', 'SL', 'SH')
jar16('JX', 'SL', 'SH', 'CLKX', None, 'XL', 'XH')
jar16('JA', 'XL', 'XH', 'CLKA', None, 'AL', 'AH')
add16('ADD', 'AL', 'AH', 'XL', 'XH', 'RAL', 'RAH')
add16('SUB1', 'AL', 'AH', 'XL', 'XH', 'R1L', 'R1H', subtract=True)
add16('SUB2', 'XL', 'XH', 'AL', 'AH', 'R2L', 'R2H', subtract=True)
jar16('JSA', 'RAL', 'RAH', 'CLK1', 'RSA', 'SL', 'SH')
jar16('JS1', 'R1L', 'R1H', 'CLK1', 'RS1', 'SL', 'SH')
p = place()
b.chip('JS2L', 'jar_nug', p, {'W': bus('R2L'), 'S': one('CLK1'), 'N': one('RS2'), 'E': bus('SL')})
p = place()
b.chip('JS2H', 'jar_nug', p, {'W': one('NEGW'), 'S': one('CLK1'), 'N': one('RS2'), 'E': bus('SH')})
# (JS2H's input is R2H: placed as a bus pin below, with the NEG tap beside it)
b.grid[p]['pins']['W'] = bus('R2H')
b.reserved.pop(step(p, 'W'), None)
b.reserved[step(p, 'W')] = 'R2H'
p1 = [q for q, c in b.comps if c['name'] == 'JS1H'][0]
b.clip(step(p1, 'W'), 'R1H', 8, 'NEG')
p = chip('SIGN', 'blend_nug', {'W': bus('XH'), 'S': bus('MASKS'), 'E': bus('DH')}, '{value:2}')
b.clip(step(p, 'S'), 'MASKS', 8, 'SX')
p = place()
b.comp('STASH', 'bottomless_stash_puff[facing=north]', p, {'E': one('VDD'), 'D': one('GND'), 'W': NONE1, 'N': NONE1, 'S': NONE1, 'U': NONE1})

# ---- the panel: trays in the wall, facing south; keys at y -59 to -56; the display row at -55 ----------------------------------------
LAYOUT = [['7', '8', '9', 'C'], ['4', '5', '6', '-'], ['1', '2', '3', '+'], ['<', '0', '±', '=']]
NUMBER = {**{str(d): d for d in range(10)}, '+': 10, '-': 11, 'C': 12, '=': 13, '<': 14, '±': 15}
LABEL = {'<': '←', '-': '−'}
GLOW = {**{str(d): 0 for d in range(10)}, '+': 5, '-': 5, '=': 13, 'C': 14, '<': 1, '±': 5}
DY = Y + 4
panel = []
trays = {}
for r, row in enumerate(LAYOUT):
    for c, k in enumerate(row):
        trays[(KX + c, Y + 3 - r, ZP)] = ('key', k)
for c in range(5):
    trays[(KX - 1 + c, DY, ZP)] = ('digit', c)
feed = {(KX + 3, DY, ZP): bus('XL'), (KX + 1, DY, ZP): bus('DH'), (KX + 2, DY, ZP): one('VDD'), (KX, DY, ZP): one('GND')}
for x in range(KX - 4, KX + 8):
    for y in range(Y - 1, Y + 7):
        b.reserved[(x, y, ZP - 1)] = None
for pos in trays:
    b.comp('tray', 'rolling_tray[facing=south]', pos, {'N': feed.get(pos, NONE1)})
    if pos in feed:
        b.reserved[step(pos, 'N')] = feed[pos][0]
b.comp('DANK', 'dankoder[facing=north]', (KX, Y, ZP - 1), {'N': bus('K'), 'U': NONE1, 'D': NONE1, 'E': NONEB, 'W': NONEB, 'S': NONEB})
for pos, (what, v) in trays.items():
    f = (pos[0], pos[1], pos[2] + 1)
    if what == 'key':
        panel.append((f, 'fourtwenty:puff_button[facing=south,mounted=true]', f'{{value:{NUMBER[v] + 1},label:"{LABEL.get(v, v)}",glow:{GLOW[v]}}}'))
    else:
        panel.append((f, 'fourtwenty:numb_nug[facing=south,mounted=true]', ''))

attempt = b.route(attempts=int(os.environ.get("ATTEMPTS", "80")))
b.check()
blocks = list(b.blocks()) + panel
counts = {}
for _, s, _ in blocks:
    k = s.split('[')[0]
    counts[k] = counts.get(k, 0) + 1
sys.stderr.write(f'routed on attempt {attempt}: {len(blocks)} blocks; ' + ', '.join(f'{v} {k.split(":")[1]}' for k, v in sorted(counts.items(), key=lambda kv: -kv[1])[:6]) + chr(10))
xs = [p[0] for p, _, _ in blocks]
zs = [p[2] for p, _, _ in blocks]
sys.stderr.write(f'extent x {min(xs)}..{max(xs)}, z {min(zs)}..{max(zs)}' + chr(10))
print(chr(10).join(f'setblock {p[0]} {p[1]} {p[2]} {s}{n}' for p, s, n in blocks))
