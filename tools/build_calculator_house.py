"""The calculator's building on flatworld, Sensi Heights tier 5 ("modern": white, black, light gray and cream concrete, generous glass, lime
accents and lamp trim, a flat roof terrace), in FourTwenty blocks only. A lobby along the front (south), the calculator's panel in its
back wall; behind it the sealed logic room tools/build_calculator.py fills. Prints the commands; run it before the machine's.

    python tools/build_calculator_house.py > house.cmds
"""
X0, X1 = 299, 353               # walls included
Z0, Z1 = -89, -7                # back wall, front wall
FLOOR, ROOF = -61, -51          # floor level with the ground; the roof deck
ZP = -14                        # the panel wall between the lobby and the logic room
PANEL = (316, 327, -60, -52)    # x0, x1, y0, y1 of the dark surround round the panel (the trays themselves are the machine's)

WHITE = 'fourtwenty:concrete_myr_pin_lim'           # Rolling Paper White
BLACK = 'fourtwenty:concrete_myr_pin_myr'           # Dave's Not Here
GRAY = 'fourtwenty:concrete_mc_light_gray'          # Gravel Gaze
CREAM = 'fourtwenty:concrete_myr_lim_pin'           # Buttercream Brain
LIME = 'fourtwenty:concrete_pin_lim_pin'            # Grandma's Glaucoma Green
GLASS = 'fourtwenty:glass_myr_lav_lim'              # First Exhale: nearly clear
LIME_GLASS = 'fourtwenty:glass_pin_lim_pin'
TRIM = 'fourtwenty:lamp_trim_pin_lim_pin'           # lime lamp trim (not a circuit lamp)
LIGHT = 'fourtwenty:inverted_lamp_myr_pin_lim'      # white, lit unless powered: the lobby's ceiling lights

out = []


def fill(x0, y0, z0, x1, y1, z1, block):
    out.append(f'fill {x0} {y0} {z0} {x1} {y1} {z1} {block}')


def put(x, y, z, block):
    out.append(f'setblock {x} {y} {z} {block}')


out.append(f'forceload add {X0 - 2} {Z0 - 2} {X1 + 2} {Z1 + 2}')
# clear the space, then the floor: black under the logic, light gray in the lobby with a lime path from the door to the panel
# (in two: a fill takes at most 32768 blocks)
ZM = (Z0 + Z1) // 2
fill(X0, FLOOR + 1, Z0, X1, ROOF + 3, ZM, 'minecraft:air')
fill(X0, FLOOR + 1, ZM + 1, X1, ROOF + 3, Z1, 'minecraft:air')
fill(X0, FLOOR, Z0, X1, FLOOR, ZP, BLACK)
fill(X0, FLOOR, ZP + 1, X1, FLOOR, Z1, GRAY)
fill(320, FLOOR, ZP + 1, 323, FLOOR, Z1, LIME)
fill(321, FLOOR, ZP + 1, 322, FLOOR, Z1, CREAM)
# the shell: white walls on a black plinth, a light gray band round it at the lintels
for x0, z0, x1, z1 in ((X0, Z0, X1, Z0), (X0, Z0, X0, Z1), (X1, Z0, X1, Z1), (X0, Z1, X1, Z1)):
    fill(x0, FLOOR + 1, z0, x1, ROOF - 1, z1, WHITE)
    fill(x0, FLOOR + 1, z0, x1, FLOOR + 1, z1, BLACK)
    fill(x0, ROOF - 3, z0, x1, ROOF - 3, z1, GRAY)
# the front: tall glass bays between white piers every five, black mullions at the band; a doorway in the middle under a cream lintel
for x in range(X0 + 1, X1):
    if (x - X0) % 5 == 0:
        continue
    fill(x, FLOOR + 2, Z1, x, ROOF - 4, Z1, GLASS)
    put(x, ROOF - 2, Z1, LIME_GLASS)
fill(320, FLOOR + 1, Z1, 323, FLOOR + 4, Z1, 'minecraft:air')
fill(319, FLOOR + 5, Z1, 324, FLOOR + 5, Z1, CREAM)
for x in range(320, 324):
    put(x, FLOOR + 6, Z1, f'{TRIM}[facing=south]')
# the sides: a ribbon of glass along the lobby only (the logic room stays sealed)
for x in (X0, X1):
    fill(x, FLOOR + 3, ZP + 2, x, ROOF - 4, Z1 - 2, GLASS)
# the panel wall: white, the panel's surround black with a cream frame and a lime lamp trim line above the display
fill(X0 + 1, FLOOR + 1, ZP, X1 - 1, ROOF - 1, ZP, WHITE)
x0, x1, y0, y1 = PANEL
fill(x0, y0, ZP, x1, y1, ZP, BLACK)
fill(x0 - 1, y0, ZP, x0 - 1, y1 + 1, ZP, CREAM)
fill(x1 + 1, y0, ZP, x1 + 1, y1 + 1, ZP, CREAM)
fill(x0 - 1, y1 + 1, ZP, x1 + 1, y1 + 1, ZP, CREAM)
for x in range(319, 325):
    put(x, -53, ZP + 1, f'{TRIM}[facing=south]')
# a low black counter under the keypad, a step out from the wall
fill(318, FLOOR + 1, ZP + 1, 325, FLOOR + 1, ZP + 1, BLACK)
# open to the sky (the user's call, 2026-11-08): the walls' top course in white, a cream parapet on it with lime glass along the front
# and lime trim on the corners, nothing across the top
for x0, z0, x1, z1 in ((X0, Z0, X1, Z0), (X0, Z0, X0, Z1), (X1, Z0, X1, Z1), (X0, Z1, X1, Z1)):
    fill(x0, ROOF, z0, x1, ROOF, z1, WHITE)
    fill(x0, ROOF + 1, z0, x1, ROOF + 1, z1, CREAM)
fill(X0 + 1, ROOF + 2, Z1, X1 - 1, ROOF + 2, Z1, LIME_GLASS)
for x, z in ((X0, Z0), (X1, Z0), (X0, Z1), (X1, Z1)):
    put(x, ROOF + 2, z, f'{TRIM}[facing=up]')
print('\n'.join(out))
