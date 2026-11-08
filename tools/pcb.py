"""A router for FourTwenty circuits laid out like a printed circuit board: chips on one level, every signal a trace on that same level,
VDD a level above and GND the floor below; signals in Hemp Thread of different colours (wires join only their own colour, so neighbours
may run side by side), and crossings made with Nah, Skip Me, Bro hops. Then a check that rebuilds every net as the game does.

    board = Board(level=-59)
    board.chip('AND1', 'and_nug', (x, y, z), {'S': board.one('a'), 'N': board.one('b'), 'E': board.one('out')})
    board.clip(pos, 'BUS', 3, 'b3')          # a Roach Clip on BUS, its channel 3 out to the net b3
    board.route()                            # raises SystemExit if it can't
    board.check()
    for pos, state, nbt in board.blocks(): ...

Nets: a Ribbon net ('ribbon'), a signal ('thread', coloured when routed), VDD ('red') and GND ('black').
"""
import heapq
import random
import sys
from collections import deque

DIRS = {'E': (1, 0, 0), 'W': (-1, 0, 0), 'S': (0, 0, 1), 'N': (0, 0, -1), 'U': (0, 1, 0), 'D': (0, -1, 0)}
OPP = {'E': 'W', 'W': 'E', 'S': 'N', 'N': 'S', 'U': 'D', 'D': 'U'}
AXIS = {'E': 'x', 'W': 'x', 'U': 'y', 'D': 'y', 'S': 'z', 'N': 'z'}
# the signal colours: every dye but red and black (those are + and - where a wire meets a BI block), and undyed
COLOURS = ['none', 'white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown',
           'green']
# a Ribbon's colours: all of them (red and black mean nothing on a Ribbon)
RIBBON_COLOURS = COLOURS + ['red', 'black']
HOP_COST = 6
TURN_COST = 3


def step(p, d):
    dx, dy, dz = DIRS[d]
    return (p[0] + dx, p[1] + dy, p[2] + dz)


class Blocked(Exception):
    pass


class Board:
    def __init__(self, level, bounds, feed=None):
        """level: the chips' y. bounds: ((x0, x1), (y0, y1), (z0, z1)) the routing may use. feed: a box ((x0, x1), (y0, y1), (z0, z1))
        where traces may also climb (a panel's feeds), or None."""
        self.Y = level
        self.bounds = bounds
        self.feed = feed
        self.grid = {}          # pos -> cell
        self.reserved = {}      # pos -> net (a stub to come), or None (kept empty)
        self.type = {'VDD': 'red', 'GND': 'black'}
        self.colour = {}        # signal net -> its thread colour
        self.comps = []

    # ---- describing the circuit -------------------------------------------------------------------------------------------------------
    def net(self, name, kind):
        self.type.setdefault(name, kind)
        return name

    def one(self, n): return (self.net(n, 'thread'), 'single') if n is not None else (None, 'single')

    def bus(self, n): return (self.net(n, 'ribbon'), 'bus') if n is not None else (None, 'bus')

    def comp(self, name, block, pos, pins, nbt=''):
        c = {'kind': 'comp', 'name': name, 'block': block, 'pins': pins, 'nbt': nbt}
        self.grid[pos] = c
        self.comps.append((pos, c))
        for d, (n, kind) in pins.items():
            cell = step(pos, d)
            if n is None:
                self.reserved.setdefault(cell, None)
            elif cell not in self.grid and n != 'direct':
                self.reserved[cell] = n
        return c

    def chip(self, name, block, pos, pins, nbt='', facing='east'):
        """A chip: pins by world side; VDD on top, GND below (their cells are its risers)."""
        pins = dict(pins)
        pins['U'] = ('VDD', 'single')
        pins['D'] = ('GND', 'single')
        return self.comp(name, f'{block}[facing={facing}]', pos, pins, nbt)

    def clip(self, pos, bus, channel, chan):
        self.net(bus, 'ribbon')
        self.net(chan, 'thread' if chan not in ('VDD', 'GND') else self.type[chan])
        self.grid[pos] = {'kind': 'clip', 'net': bus, 'chan': chan, 'channel': channel}
        self.reserved.pop(pos, None)

    # ---- what a cell shows a neighbour ------------------------------------------------------------------------------------------------
    def cell_of(self, p):
        """The cell at p, or a stub to come as if laid, or None."""
        c = self.grid.get(p)
        if c is not None:
            return c
        n = self.reserved.get(p)
        if n is None:
            return None
        t = self.type[n]
        return {'kind': 'wire', 'type': t, 'net': n, 'colour': self.colour.get(n, '?') if t in ('thread', 'ribbon') else None}

    @staticmethod
    def key(t, colour):
        """What a wire joins: Wicks and thread of one colour (red and black Wicks are VDD and GND)."""
        return {'red': 'red', 'black': 'black'}.get(t, colour)

    def faces(self, c, d):
        """What cell c presents on its side d: [(net, mode, key)]. Modes: wire (a key: its colour), bus, pin, pinbus, clipchan."""
        k = c['kind']
        if k == 'comp':
            pin = c['pins'].get(d)
            if not pin or pin[0] is None:
                return []
            return [(pin[0], 'pin' if pin[1] == 'single' else 'pinbus', None)]
        if k == 'wire':
            if c['type'] == 'ribbon':
                return [(c['net'], 'ribbon', c['colour'])]
            return [(c['net'], 'wire', self.key(c['type'], c['colour']))]
        if k == 'clip':
            return [(c['net'], 'bus', None), (c['chan'], 'clipchan', None)]
        if k == 'hop':
            for w in c['wires']:
                if w['axis'] == AXIS[d]:
                    return self.faces({'kind': 'wire', 'type': w['type'], 'net': w['net'], 'colour': w['colour']}, d)
            return []
        return []

    @staticmethod
    def meet(a, b):
        """Whether two faces touching join."""
        ma, mb = a[1], b[1]
        pair = {ma, mb}
        if ma == 'ribbon' and mb == 'ribbon':
            # Ribbons join their own colour
            return a[2] == b[2] or '?' in (a[2], b[2])
        if 'ribbon' in pair:
            # and any Roach Clip or bus face
            return bool(pair & {'bus', 'pinbus'})
        if ma == 'wire' and mb == 'wire':
            return a[2] == b[2] and a[2] != '?' or (a[2] == '?' or b[2] == '?')
        if 'wire' in pair:
            return bool(pair & {'pin', 'clipchan'})
        if pair <= {'pin', 'clipchan'}:
            return 'pin' in pair
        if pair <= {'bus', 'pinbus'}:
            return True
        return False

    def ok(self, p, cell):
        """Laying cell at p joins nothing but its own net."""
        for d in DIRS:
            nb = self.cell_of(step(p, d))
            if nb is None:
                continue
            for fa in self.faces(cell, d):
                for fb in self.faces(nb, OPP[d]):
                    if self.meet(fa, fb) and fa[0] != fb[0] and 'direct' not in (fa[0], fb[0]):
                        return False
        return True

    def inside(self, p, net):
        (x0, x1), (y0, y1), (z0, z1) = self.bounds
        if not (x0 <= p[0] <= x1 and y0 <= p[1] <= y1 and z0 <= p[2] <= z1):
            return False
        t = self.type[net]
        if self.feed:
            (fx0, fx1), (fy0, fy1), (fz0, fz1) = self.feed
            if fx0 <= p[0] <= fx1 and fy0 <= p[1] <= fy1 and fz0 <= p[2] <= fz1:
                return True
        # the board: signals on the chips' level, VDD a level above, GND below
        return p[1] == {'red': self.Y + 1, 'black': self.Y - 1}.get(t, self.Y)

    def free(self, p, net):
        if not self.inside(p, net) or p in self.grid:
            return False
        r = self.reserved.get(p, 'free')
        return r == 'free' or r == net

    def wire(self, net):
        t = self.type[net]
        return {'kind': 'wire', 'type': t, 'net': net, 'colour': self.colour.get(net) if t in ('thread', 'ribbon') else None}

    # ---- hops ---------------------------------------------------------------------------------------------------------------------
    def straight(self, q):
        """The axis a wire at q joins along, if it joins along one axis only (and so can be hopped across), else None."""
        c = self.grid.get(q)
        if c is None or c['kind'] != 'wire':
            return None
        axes = set()
        for d in DIRS:
            nb = self.cell_of(step(q, d))
            if nb is None:
                continue
            if any(self.meet(fa, fb) for fa in self.faces(c, d) for fb in self.faces(nb, OPP[d])):
                axes.add(AXIS[d])
        return next(iter(axes)) if len(axes) == 1 else None

    # ---- routing -------------------------------------------------------------------------------------------------------------------
    def cells(self, net):
        t = self.type[net]
        out = []
        for p, c in self.grid.items():
            if c['kind'] == 'wire' and c['net'] == net:
                out.append(p)
            elif c['kind'] == 'clip' and (c['chan'] == net or (t == 'ribbon' and c['net'] == net)):
                out.append(p)
            elif c['kind'] == 'hop' and any(w['net'] == net for w in c['wires']):
                out.append(p)
        return out

    def route_net(self, net):
        t = self.type[net]
        # its stubs, laid now in its colour
        for p, n in list(self.reserved.items()):
            if n == net and p not in self.grid:
                if not self.ok(p, self.wire(net)):
                    raise Blocked(net, [p])
                self.grid[p] = self.wire(net)
                del self.reserved[p]
        cells = self.cells(net)
        if len(cells) < 2:
            return
        tap = lambda p: t != 'ribbon' and self.grid.get(p, {}).get('kind') == 'clip'
        if all(tap(p) for p in cells) and len({(self.grid[p]['net'], self.grid[p]['channel']) for p in cells}) == 1:
            return
        joined = lambda p, q: not (tap(p) and tap(q)) and self.grid[p]['kind'] != 'hop' and self.grid[q]['kind'] != 'hop'
        tree = {cells[0]}
        todo = set(cells[1:])

        def grow():
            changed = True
            while changed:
                changed = False
                for p in list(todo):
                    if any(step(p, d) in tree and joined(p, step(p, d)) for d in DIRS):
                        tree.add(p)
                        todo.discard(p)
                        changed = True
        grow()
        me = self.wire(net)
        while todo:
            # states are (cell, the way it was entered): a turn costs extra, so traces run long and straight, and straight runs are what
            # later traces can hop across
            dist = {(p, None): 0 for p in tree}
            prev = {(p, None): None for p in tree}
            heap = [(0, p, None) for p in tree if self.grid[p]['kind'] != 'hop']
            heapq.heapify(heap)
            hit = None
            while heap:
                dp, p, came = heapq.heappop(heap)
                if dp > dist.get((p, came), 1 << 30):
                    continue
                if p in todo:
                    hit = (p, came)
                    break
                for d in DIRS:
                    q = step(p, d)
                    moves = []
                    turn = 0 if came is None or came == d else TURN_COST
                    if q in todo and not (tap(p) and tap(q)):
                        moves.append((q, None, dp + 1 + turn))
                    elif self.free(q, net) and self.ok(q, me):
                        moves.append((q, None, dp + 1 + turn))
                    else:
                        # hops: across straight runs of other nets (one, or several side by side), out the far side
                        vias, at = [], q
                        while at in self.grid and self.grid[at]['kind'] == 'wire' and self.grid[at]['net'] != net and self.inside(at, net):
                            a = self.straight(at)
                            if a is None or a == AXIS[d]:
                                break
                            vias.append(at)
                            at = step(at, d)
                        if vias and (at in todo or (self.free(at, net) and self.ok(at, me))):
                            moves.append((at, tuple(vias), dp + HOP_COST * len(vias) + turn))
                    for q2, via, nd in moves:
                        if nd < dist.get((q2, d), 1 << 30):
                            dist[(q2, d)] = nd
                            prev[(q2, d)] = ((p, came), via)
                            heapq.heappush(heap, (nd, q2, d))
            if hit is None:
                raise Blocked(net, sorted(todo)[:3])
            state = hit
            while prev[state] is not None:
                (pstate, via) = prev[state]
                q, p = state[0], pstate[0]
                if q not in self.grid:
                    self.grid[q] = dict(me)
                if via is not None:
                    d = next(d for d in DIRS if step(p, d) == via[0])
                    axes = [self.straight(v) for v in via]
                    for v, a in zip(via, axes):
                        other = self.grid[v]
                        self.grid[v] = {'kind': 'hop', 'wires': [
                            {'type': other['type'], 'colour': other['colour'], 'net': other['net'], 'axis': a},
                            {'type': t, 'colour': me['colour'], 'net': net, 'axis': AXIS[d]}]}
                tree.add(q)
                state = pstate
            todo.discard(hit[0])
            grow()

    def colour_nets(self, rng):
        """Each signal a colour: nets whose stubs lie close get different ones, greedily, the busiest first."""
        stubs = {}
        for p, n in self.reserved.items():
            if n is not None and self.type.get(n) in ('thread', 'ribbon'):
                stubs.setdefault(n, []).append(p)
        for p, c in self.grid.items():
            if c['kind'] == 'clip' and self.type.get(c['chan']) == 'thread':
                stubs.setdefault(c['chan'], []).append(p)
            if c['kind'] == 'clip' and self.type.get(c['net']) == 'ribbon':
                stubs.setdefault(c['net'], []).append(p)
        names = sorted(stubs, key=lambda n: -len(stubs[n]))
        for n in names:
            near = set()
            for m in names:
                if m == n or m not in self.colour or self.type[m] != self.type[n]:
                    continue
                if any(abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2]) <= 4 for a in stubs[n] for b in stubs[m]):
                    near.add(self.colour[m])
            palette = RIBBON_COLOURS if self.type[n] == 'ribbon' else COLOURS
            free = [c for c in palette if c not in near]
            self.colour[n] = rng.choice(free[:3]) if free else rng.choice(palette)

    def route(self, order=None, attempts=60):
        """Routes every net: Ribbons first, then signals, then power; a net that can't get through goes first next time, the rest shuffled a
        little, from a clean slate."""
        clean = (dict(self.grid), dict(self.reserved))
        nets = [n for n in self.type if n != 'direct']
        order = order or ([n for n in nets if self.type[n] == 'ribbon'] + [n for n in nets if self.type[n] == 'thread'] + ['VDD', 'GND'])
        for attempt in range(attempts):
            rng = random.Random(attempt)
            self.grid, self.reserved = dict(clean[0]), dict(clean[1])
            self.colour = {}
            self.colour_nets(rng)
            try:
                for n in order:
                    self.route_net(n)
                return attempt
            except Blocked as e:
                n = e.args[0]
                order.remove(n)
                for i in range(len(order) - 1):
                    if rng.random() < 0.15:
                        j = min(len(order) - 1, i + rng.randint(1, 4))
                        order[i], order[j] = order[j], order[i]
                order.insert(0, n)
                sys.stderr.write(f'attempt {attempt}: {n} blocked at {e.args[1]}, first next time' + chr(10))
        raise SystemExit('no order routes')

    # ---- the check -----------------------------------------------------------------------------------------------------------------
    def check(self):
        """Rebuilds every net from the blocks, as the game joins them, and fails if any net is split or two are joined."""
        parent = {}

        def find(a):
            while parent.setdefault(a, a) != a:
                parent[a] = parent[parent[a]]
                a = parent[a]
            return a

        def union(a, b): parent[find(a)] = find(b)

        def node(p, c, d, face):
            """The node a face of cell c at p stands for."""
            k = c['kind']
            if k == 'comp':
                return ('pin', p, d)
            if k == 'clip':
                return (p, 'chan') if face[1] == 'clipchan' else (p, 'bus')
            if k == 'hop':
                return (p, 'hop', AXIS[d])
            return (p, 'self')

        for p, c in self.grid.items():
            for d in DIRS:
                q = step(p, d)
                nb = self.grid.get(q)
                if nb is None:
                    continue
                for fa in self.faces(c, d):
                    for fb in self.faces(nb, OPP[d]):
                        if self.meet(fa, fb):
                            union(node(p, c, d, fa), node(q, nb, OPP[d], fb))
        # a hop's wire is one conductor end to end; a clip's tap is its bus's channel
        for p, c in self.grid.items():
            if c['kind'] == 'hop':
                for w in c['wires']:
                    a = [d for d in DIRS if AXIS[d] == w['axis']]
                    union((p, 'hop', w['axis']), (p, 'hop', w['axis']))
            if c['kind'] == 'clip':
                union((p, 'chan'), ('channel', find((p, 'bus')), c['channel']))
        sets = {}
        for pos, c in self.comps:
            for d, (n, kind) in c['pins'].items():
                if n in (None, 'direct'):
                    continue
                sets.setdefault(n, set()).add(find(('pin', pos, d)))
        for p, c in self.grid.items():
            if c['kind'] == 'clip':
                sets.setdefault(c['chan'], set()).add(find((p, 'chan')))
        bad = {n: len(s) for n, s in sets.items() if len(s) != 1}
        roots = {}
        for n, s in sets.items():
            for r in s:
                roots.setdefault(r, set()).add(n)
        merged = [ns for ns in roots.values() if len(ns) > 1]
        if bad or merged:
            raise SystemExit(f'check failed: split {bad}, merged {merged}')

    # ---- out -----------------------------------------------------------------------------------------------------------------------
    @staticmethod
    def wire_state(t, colour):
        if t == 'ribbon':
            return f'fourtwenty:eighth_ribbon[colour={colour}]'
        if t == 'thread':
            return f'fourtwenty:hemp_thread[colour={colour}]'
        return f'fourtwenty:hemp_wick[colour={t}]'

    def blocks(self):
        """(pos, block state, nbt) for every block of the board."""
        names = {'ribbon': 'fourtwenty:eighth_ribbon', 'thread': 'fourtwenty:hemp_thread', 'red': 'fourtwenty:hemp_wick', 'black': 'fourtwenty:hemp_wick'}
        for p, c in self.grid.items():
            k = c['kind']
            if k == 'comp':
                yield p, 'fourtwenty:' + c['block'], c['nbt']
            elif k == 'wire':
                yield p, self.wire_state(c['type'], c['colour']), ''
            elif k == 'clip':
                yield p, f'fourtwenty:roach_clip[channel={c["channel"]}]', ''
            elif k == 'hop':
                parts = []
                for i, w in enumerate(c['wires']):
                    colour = w['colour'] if w['type'] in ('thread', 'ribbon') else w['type']
                    parts.append(f'wire{i}:{{block:"{names[w["type"]]}",colour:"{colour}",axis:"{w["axis"]}"}}')
                yield p, 'fourtwenty:skip_me_bro', '{' + ','.join(parts) + '}'
