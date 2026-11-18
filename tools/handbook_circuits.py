#!/usr/bin/env python3
"""The Handbook's six example circuits as block lists, the same layouts CircuitExampleGameTests builds and checks
(specs/016-weedcraft-handbook FR-014). Used to build them in a test world for their pictures:

    python3 tools/handbook_circuits.py <x0> <y0> <z0> > circuits.cmds     # rcon commands, one circuit every 16 blocks along x

Coordinates are the tests' (x, y, z): y 2 is the board's row; world y is y0 + y. Switches are set to show each circuit at work.
"""
import sys

R, B, T = 'fourtwenty:hemp_wick[colour=red]', 'fourtwenty:hemp_wick[colour=black]', 'fourtwenty:hemp_thread'


def part(kind, facing):
    return f'fourtwenty:{kind}[facing={facing}]'


def board(x1):
    out = []
    for x in range(0, x1 + 1):
        out += [((x, 2, 0), R), ((x, 2, 2), B)]
    out.append(((1, 2, 1), 'fourtwenty:weed_bong[facing=east,has_water=true,lit=true,weed_level=4]'))
    return out


def power_off_board(x, z):
    return [((x, 3, zz), R) for zz in range(0, z + 1)] + [((x, 1, zz), B) for zz in range(2, z + 1)]


def switch_from(x, z, facing, line_x):
    step = {'west': (-1, 0), 'east': (1, 0)}[facing]
    bx, bz = x - step[0], z - step[1]
    out = [((x, 2, z), part('toggle_joint', facing)), ((bx, 2, bz), R)]
    out += [((xx, 3, bz), R) for xx in range(min(bx, line_x), max(bx, line_x) + 1)]
    return out


def light():
    return board(4) + [((5, 2, 0), part('toggle_joint', 'west')), ((6, 2, 0), R), ((6, 2, 1), R),
                       ((6, 2, 2), 'fourtwenty:lit_bit'), ((5, 2, 2), B)], [(5, 2, 0)]


def inverter():
    return board(6) + [((5, 2, 1), part('toggle_joint', 'east')), ((4, 2, 1), R), ((6, 2, 1), part('not_nug', 'east')),
                       ((6, 3, 1), R), ((6, 3, 0), R), ((6, 1, 1), B), ((6, 1, 2), B)], []


def lock():
    return board(7) + [((5, 2, 4), part('and_nug', 'north'))] + power_off_board(5, 4) + \
        switch_from(6, 4, 'west', 5) + switch_from(4, 4, 'east', 5), [(6, 2, 4), (4, 2, 4)]


def memory():
    return board(7) + [((5, 2, 4), part('latch_nug', 'north'))] + power_off_board(5, 4) + \
        switch_from(6, 4, 'west', 5) + switch_from(4, 4, 'east', 5), []


def clock():
    return board(6) + [((5, 2, 1), part('ticker', 'west')), ((5, 3, 1), R), ((5, 3, 0), R), ((5, 1, 1), B), ((5, 1, 2), B)], []


def half_adder():
    out = board(7) + [((5, 2, 4), part('xor_nug', 'north')), ((5, 2, 6), part('and_nug', 'north'))] + power_off_board(5, 6)
    for z in range(4, 7):
        out += [((6, 2, z), T), ((4, 2, z), T)]
    out += [((7, 2, 5), part('toggle_joint', 'west')), ((8, 2, 5), R), ((8, 3, 5), R), ((7, 3, 5), R), ((6, 3, 5), R),
            ((3, 2, 5), part('toggle_joint', 'east')), ((2, 2, 5), R), ((2, 3, 5), R), ((3, 3, 5), R), ((4, 3, 5), R)]
    return out, [(7, 2, 5), (3, 2, 5)]


CIRCUITS = {'circuit_light': light, 'circuit_inverter': inverter, 'circuit_lock': lock, 'circuit_memory': memory,
            'circuit_clock': clock, 'circuit_adder': half_adder}


def main():
    x0, y0, z0 = map(int, sys.argv[1:4])
    scenes = []
    for i, (name, make) in enumerate(CIRCUITS.items()):
        blocks, closed = make()
        ox = x0 + i * 16
        print(f'fill {ox - 2} {y0} {z0 - 3} {ox + 12} {y0 + 6} {z0 + 9} minecraft:air')
        print(f'fill {ox - 2} {y0 - 1} {z0 - 3} {ox + 12} {y0 - 1} {z0 + 9} minecraft:smooth_stone')
        # parts first, then the wires that join them (Wicks and Hemp Thread link to what's there when placed)
        for (x, y, z), state in sorted(blocks, key=lambda b: ('wick' in b[1] or 'thread' in b[1])):
            print(f'setblock {ox + x} {y0 + y} {z0 + z} {state}')
        for (x, y, z) in closed:
            print(f'data merge block {ox + x} {y0 + y} {z0 + z} {{closed:1b}}')
        for (x, y, z), state in blocks:
            if 'weed_bong' in state:
                w = (ox + x, y0 + y, z0 + z)
                print(f'item replace block {w[0]} {w[1]} {w[2]} container.0 with fourtwenty:weed_bud 4')
                print(f'item replace block {w[0]} {w[1]} {w[2]} container.1 with minecraft:water_bucket')
                print(f'item replace block {w[0]} {w[1]} {w[2]} container.2 with minecraft:charcoal 16')
        scenes.append({'name': name, 'from': [ox - 1, y0, z0 - 2], 'to': [ox + 10, y0 + 4, z0 + 8]})
    import json
    sys.stderr.write(json.dumps(scenes) + '\n')


if __name__ == '__main__':
    main()
