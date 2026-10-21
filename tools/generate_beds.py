#!/usr/bin/env python3
"""Assets and recipes for FourTwenty's beds, one per vanilla wood (blanket colour is a block state).

FourTwenty's own design: tall stripped-log head posts with a top rail and a plank headboard, a
lower footboard, a plank base, a white mattress, a pillow tilted against the headboard, and a
blanket a touch wider than the mattress so it drapes over the sides, with a rolled hem. The whole
bed is one model drawn from the foot half (reaching into the head block, so there's no seam); the
head half draws nothing of its own. When someone sleeps in it the blanket is turned back. Only
vanilla textures (the wood's planks and stripped log, wool). Writes the template, per wood/colour
models, blockstates, item models, loot tables (the head half drops the bed), recipes, the bed and
axe tags, and English names. Run again after edits.
"""
import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'crimson', 'warped']
COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan',
          'purple', 'blue', 'brown', 'green', 'red', 'black']
ROTATION = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + '\n', encoding='utf-8')


def uv(face, f, t):
    """Vanilla's automatic UV for a face, taken from the element's position within its own block,
    so elements reaching into the next block (z < 0) still sample inside the texture."""
    (x0, y0, z0), (x1, y1, z1) = f, t
    if z1 - z0 > 16:
        z0, z1 = 0, 16                  # longer than a block (base, mattress): stretch one texture
    elif not (0 <= z0 and z1 <= 16):
        z0, z1 = z0 % 16, z0 % 16 + (z1 - z0)
        if z1 > 16:
            z0, z1 = 16 - (z1 - z0), 16  # straddles a block edge: keep it inside the texture
    return {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
            'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
            'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}[face]


def box(f, t, texture, rotation=None, shift=0):
    f2, t2 = [f[0], f[1], f[2] + shift], [t[0], t[1], t[2] + shift]
    e = {'from': f2, 'to': t2, 'faces': {face: {'texture': texture, 'uv': uv(face, f, t)}
                                       for face in ('north', 'south', 'east', 'west', 'up', 'down')}}
    if rotation:
        angle, origin = rotation
        e['rotation'] = {'angle': angle, 'axis': 'x', 'origin': [origin[0], origin[1], origin[2] + shift]}
    return e


def elements(occupied, shift=0):
    """The whole bed, foot block at z 0..16 and the head block at z -16..0 (head end north)."""
    b = lambda f, t, tex, rot=None: box(f, t, tex, rot, shift)
    parts = [
        # Head end: two tall posts, a top rail, a plank headboard panel.
        b([0, 0, -16], [2, 14, -14], '#log'), b([14, 0, -16], [16, 14, -14], '#log'),
        b([0, 14, -16], [16, 16, -14], '#log'),
        b([2, 4, -15.5], [14, 14, -14.5], '#planks'),
        # Foot end: lower posts, a rail, a plank footboard.
        b([0, 0, 14], [2, 7, 16], '#log'), b([14, 0, 14], [16, 7, 16], '#log'),
        b([0, 7, 14], [16, 8.5, 16], '#log'),
        b([2, 3, 14.5], [14, 7, 15.5], '#planks'),
        # The base and the mattress.
        b([1, 2, -14], [15, 4, 14], '#planks'),
        b([1, 4, -14], [15, 7, 14], '#sheet'),
        # A pillow leaning back against the headboard.
        b([3, 7, -13.5], [13, 9.5, -10], '#sheet', (-22.5, [8, 7, -12])),
    ]
    if occupied:
        # Turned back: the blanket covers the foot end, its rolled hem halfway down.
        parts += [b([0.5, 4.5, 2], [15.5, 7.5, 14], '#blanket'), b([0.5, 6.5, 0.5], [15.5, 8.5, 2.5], '#blanket')]
    else:
        parts += [b([0.5, 4.5, -7], [15.5, 7.5, 14], '#blanket'), b([0.5, 6.5, -8.5], [15.5, 8, -6.5], '#blanket')]
    return parts


def log_texture(wood):
    return f'minecraft:block/stripped_{wood}_stem' if wood in ('crimson', 'warped') else f'minecraft:block/stripped_{wood}_log'


def title(wood): return ' '.join(part.capitalize() for part in wood.split('_'))


def generate():
    for occupied in (False, True):
        write(ASSETS / f'models/block/bed{"_occupied" if occupied else ""}.json',
              {'parent': 'minecraft:block/block', 'textures': {'particle': '#planks'}, 'elements': elements(occupied)})
    write(ASSETS / 'models/block/bed_head.json', {'textures': {'particle': '#planks'}, 'elements': []})
    write(ASSETS / 'models/item/bed_template.json', {
        'parent': 'minecraft:block/block', 'textures': {'particle': '#planks'}, 'elements': elements(False, shift=8),
        'display': {'gui': {'rotation': [30, 160, 0], 'translation': [0, 1, 0], 'scale': [0.42, 0.42, 0.42]},
                    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.2, 0.2, 0.2]},
                    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
                    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.25, 0.25, 0.25]},
                    'firstperson_righthand': {'rotation': [0, 135, 0], 'translation': [0, 2, 0], 'scale': [0.3, 0.3, 0.3]},
                    'firstperson_lefthand': {'rotation': [0, 135, 0], 'translation': [0, 2, 0], 'scale': [0.3, 0.3, 0.3]}}})
    names, tagged = {}, []
    for wood in WOODS:
        name = f'{wood}_bed'
        textures = lambda color: {'planks': f'minecraft:block/{wood}_planks', 'log': log_texture(wood),
                                  'sheet': 'minecraft:block/white_wool', 'blanket': f'minecraft:block/{color}_wool'}
        write(ASSETS / f'models/block/{name}_head.json', {'parent': 'fourtwenty:block/bed_head',
                                                          'textures': {'planks': f'minecraft:block/{wood}_planks'}})
        for color in COLORS:
            for occupied in (False, True):
                write(ASSETS / f'models/block/{name}_{color}{"_occupied" if occupied else ""}.json', {
                    'parent': f'fourtwenty:block/bed{"_occupied" if occupied else ""}', 'textures': textures(color)})
        variants = {}
        for facing, rot in ROTATION.items():
            for color in COLORS:
                for occupied in ('false', 'true'):
                    for part in ('foot', 'head'):
                        model = f'fourtwenty:block/{name}_head' if part == 'head' else \
                            f'fourtwenty:block/{name}_{color}{"_occupied" if occupied == "true" else ""}'
                        variants[f'blanket={color},facing={facing},occupied={occupied},part={part}'] = \
                            {'model': model, 'y': rot} if rot else {'model': model}
        write(ASSETS / f'blockstates/{name}.json', {'variants': variants})
        write(ASSETS / f'models/item/{name}.json', {'parent': 'fourtwenty:item/bed_template', 'textures': textures('lime')})
        write(DATA / f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{
            'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fourtwenty:{name}', 'conditions': [{
                'condition': 'minecraft:block_state_property', 'block': f'fourtwenty:{name}', 'properties': {'part': 'head'}}]}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        # 3 white wool over planks either side of a weed bud -> 1 bed.
        write(DATA / f'recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'group': 'fourtwenty_bed',
            'pattern': ['WWW', 'PBP'],
            'key': {'W': {'item': 'minecraft:white_wool'}, 'P': {'item': f'minecraft:{wood}_planks'}, 'B': {'item': 'fourtwenty:weed_bud'}},
            'result': {'id': f'fourtwenty:{name}', 'count': 1}})
        names[name] = f'{title(wood)} Bed'
        tagged.append(f'fourtwenty:{name}')
    write(RES / 'data/minecraft/tags/block/beds.json', {'replace': False, 'values': tagged})
    write(RES / 'data/minecraft/tags/item/beds.json', {'replace': False, 'values': tagged})
    axe = RES / 'data/minecraft/tags/block/mineable/axe.json'
    current = json.loads(axe.read_text(encoding='utf-8')) if axe.exists() else {'replace': False, 'values': []}
    current['values'] = sorted(set(current['values']) | set(tagged))
    write(axe, current)
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    for name, label in names.items():
        data[f'block.fourtwenty.{name}'] = label
        data[f'item.fourtwenty.{name}'] = label
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{len(names)} beds generated')


if __name__ == '__main__':
    generate()
