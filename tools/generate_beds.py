#!/usr/bin/env python3
"""Assets and recipes for FourTwenty's beds, one per vanilla wood (blanket colour is a block state).

FourTwenty's own design: tall stripped-log head posts with a top rail and a plank headboard, a
lower footboard, a plank base, a white mattress, a pillow tilted against the headboard, and a
blanket a touch wider than the mattress so it drapes over the sides, with a rolled hem. The whole
bed is one model drawn from the foot half (reaching into the head block, so there's no seam); the
head half draws nothing of its own. When someone sleeps in it the blanket is turned back. Beds
side by side join into a double or a king (BedShape); each column gets its own models. Its own
64x64 textures (tools/generate_bed_textures.py): grained frame wood per wood, panelled boards, linen,
a pillow and quilted blankets in 16 colours. Writes the template, per wood/colour
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
    # Panels and the pillow show their whole texture, so the bevelled frame and the seam sit on the
    # piece's edges; everything else samples the texture by position, like vanilla.
    whole = texture in ('#panel', '#pillow', '#runner')
    e = {'from': f2, 'to': t2, 'faces': {face: {'texture': texture, 'uv': [0, 0, 16, 16] if whole else uv(face, f, t)}
                                       for face in ('north', 'south', 'east', 'west', 'up', 'down')}}
    if texture == '#runner':
        # The runner's stripes run along its length (across the bed); on its sides (the flaps that
        # fold down) turn the texture a quarter so they carry on down over the fold.
        for face in ('east', 'west'):
            e['faces'][face]['rotation'] = 90
    if rotation:
        angle, origin = rotation
        e['rotation'] = {'angle': angle, 'axis': 'x', 'origin': [origin[0], origin[1], origin[2] + shift]}
    return e


SHAPES = ['single', 'left', 'right', 'king_left', 'king_middle', 'king_right']


def elements(occupied, shape='single', shift=0):
    """One bed column, foot block at z 0..16 and the head block at z -16..0 (head end north, so the
    left of someone at the foot looking at the head is x 0). Joined beds (BedShape) leave out the
    posts, sides and drapes where the bed carries on into the next column, so mattress, blanket,
    band and runner run straight across; the headboard gets a crest over the middle and a slim
    mullion at each seam. A king column is a block longer: its foot end is in the extension block
    (z 16..32)."""
    b = lambda f, t, tex, rot=None: box(f, t, tex, rot, shift)
    lo = shape in ('right', 'king_middle', 'king_right')     # the bed carries on to the left
    ro = shape in ('left', 'king_left', 'king_middle')       # ... and to the right
    king = shape.startswith('king')
    end = 16 if king else 0                                  # how much further the foot end is
    x0, x1 = (0 if lo else 1), (16 if ro else 15)            # mattress, blanket top, drape
    parts = [
        # Head end: posts on the outer sides, a top rail, a panelled headboard.
        b([0, 14, -16], [16, 16, -14], '#rail'),
        b([0 if lo else 2, 4, -15.5], [16 if ro else 14, 14, -14.5], '#panel'),
        # Foot end: lower posts on the outer sides, a rail, a panelled footboard.
        b([0, 7, 14 + end], [16, 8.5, 16 + end], '#rail'),
        b([0 if lo else 2, 3, 14.5 + end], [16 if ro else 14, 7, 15.5 + end], '#panel'),
    ]
    if not lo:
        parts += [b([0, 0, -16], [2, 14, -14], '#post'), b([0, 0, 14 + end], [2, 7, 16 + end], '#post')]
    if not ro:
        parts += [b([14, 0, -16], [16, 14, -14], '#post'), b([14, 0, 14 + end], [16, 7, 16 + end], '#post')]
    # A slim mullion where two headboard panels meet.
    if ro:
        parts.append(b([15.25, 3.8, -15.75], [16, 14.2, -14.25], '#rail'))     # past the panel's top (into the rail) and bottom
    if lo:
        parts.append(b([0, 3.8, -15.75], [0.75, 14.2, -14.25], '#rail'))
    # The crest over the middle of a joined headboard: one step (and a cap) across a double's seam,
    # a wider, taller stepped crest over the middle of a king.
    crest = {'left': [(6, 16, 16, 17.5), (10, 16, 17.5, 18)],
             'right': [(0, 10, 16, 17.5), (0, 6, 17.5, 18)],
             'king_left': [(10, 16, 16, 17.5)],
             'king_middle': [(0, 16, 16, 17.5), (2.5, 13.5, 17.5, 19), (5, 11, 19, 19.5)],
             'king_right': [(0, 6, 16, 17.5)]}.get(shape, [])
    parts += [b([cx0, cy0, -16], [cx1, cy1, -14], '#rail') for cx0, cx1, cy0, cy1 in crest]
    # The base and the mattress (split where a king's runs into its extension block).
    for za, zb in ([(-14, 16), (16, 30)] if king else [(-14, 14)]):
        parts += [b([x0, 2, za], [x1, 4, zb], '#rail'), b([x0, 4, za], [x1, 7, zb], '#sheet')]
    # The pillow: a core with a smaller puff on top and bulges at the ends and front, so its edges
    # read as rounded; every piece leans back together against the headboard. One per column.
    lean = (-22.5, [8, 7, -12])
    parts += [b([3, 7.25, -13.5], [13, 9.25, -10.5], '#pillow', lean),
              b([3.5, 9.25, -13.25], [12.5, 9.75, -10.75], '#pillow', lean),
              b([2.5, 7.5, -13.25], [3, 9, -10.75], '#pillow', lean), b([13, 7.5, -13.25], [13.5, 9, -10.75], '#pillow', lean),
              b([3.5, 7.5, -10.5], [12.5, 9, -10], '#pillow', lean)]
    # The blanket: its top sits half a pixel above the draped sides, which end in a small rolled
    # hem, so the edges look rounded; it drapes down at the foot too. The sheet is folded back over
    # its top edge as a soft band (Handcrafted's idea, kept subtle): a lower roll and a narrower cap.
    # Turned back towards the foot while someone sleeps.
    z0, (b0, b1) = (2.5, (0.5, 3)) if occupied else (-7, (-9, -6.5))
    foot = 13.5 + end
    for za, zb in ([(z0, 16), (16, foot)] if king else [(z0, foot)]):
        # the top starts where the folded band ends, so the two never lie on each other
        parts.append(b([x0, 7, max(za, b1)], [x1, 7.75, zb], '#blanket'))
        # Side drapes and their hems (the hems stop short of the drapes' ends, so no faces share a plane).
        if not lo:
            parts += [b([0.5, 4.75, za], [1, 7.25, zb], '#blanket'), b([0.25, 4.5, za + 0.2], [0.75, 5.25, zb - 0.2], '#blanket')]
        if not ro:
            parts += [b([15, 4.75, za], [15.5, 7.25, zb], '#blanket'), b([15.25, 4.5, za + 0.2], [15.75, 5.25, zb - 0.2], '#blanket')]
    # The foot drape and hem reach the side drapes' outer edge and hang just past the mattress end,
    # so neither shares a plane with the mattress.
    dx0, dx1 = (0 if lo else 0.5), (16 if ro else 15.5)
    hx0, hx1 = (0 if lo else 0.25), (16 if ro else 15.75)
    parts += [b([dx0, 4.75, foot], [dx1, 7.25, foot + 0.75], '#blanket'),            # the foot drape
              b([hx0, 4.5, foot + 0.35], [hx1, 5.25, foot + 1.0], '#blanket'),
              # the folded band, sitting just above the blanket so no two faces share a plane
              b([0 if lo else 0.25, 7, b0], [16 if ro else 15.75, 8, b1], '#sheet'),
              b([0 if lo else 0.75, 8, b0 + 0.25], [16 if ro else 15.25, 8.5, b1 - 0.25], '#sheet'),
              # a hotel runner across the foot, a deeper shade with fine light edge stripes
              b([0, 7.75, 9 + end], [16, 8, 12 + end], '#runner')]
    # The runner folds over the sides: each flap hangs straight down from the top's end (one piece,
    # no gap), outside the side drape and above the hem, so no faces meet in a plane.
    if not lo:
        parts.append(b([0, 5.4, 9 + end], [0.25, 7.75, 12 + end], '#runner'))
    if not ro:
        parts.append(b([15.75, 5.4, 9 + end], [16, 7.75, 12 + end], '#runner'))
    return parts


def log_texture(wood):
    return f'minecraft:block/stripped_{wood}_stem' if wood in ('crimson', 'warped') else f'minecraft:block/stripped_{wood}_log'


def model_name(base, shape, occupied):
    """bed, bed_occupied, bed_left, bed_left_occupied, ... (a single keeps its original names)."""
    return base + ('' if shape == 'single' else f'_{shape}') + ('_occupied' if occupied else '')


def title(wood): return ' '.join(part.capitalize() for part in wood.split('_'))


def generate():
    for shape in SHAPES:
        for occupied in (False, True):
            write(ASSETS / f'models/block/{model_name("bed", shape, occupied)}.json',
                  {'parent': 'minecraft:block/block', 'textures': {'particle': '#post'}, 'elements': elements(occupied, shape)})
    # A king's extra length: invisible (the column's own model draws over it).
    write(ASSETS / 'models/block/bed_extension.json', {'textures': {'particle': 'minecraft:block/oak_planks'}, 'elements': []})
    write(ASSETS / 'blockstates/bed_extension.json', {'variants': {'': {'model': 'fourtwenty:block/bed_extension'}}})
    write(ASSETS / 'models/block/bed_head.json', {'textures': {'particle': '#post'}, 'elements': []})
    write(ASSETS / 'models/item/bed_template.json', {
        'parent': 'minecraft:block/block', 'textures': {'particle': '#post'}, 'elements': elements(False, shift=8),
        'display': {'gui': {'rotation': [30, 160, 0], 'translation': [0, 1, 0], 'scale': [0.42, 0.42, 0.42]},
                    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.2, 0.2, 0.2]},
                    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
                    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.25, 0.25, 0.25]},
                    'firstperson_righthand': {'rotation': [0, 135, 0], 'translation': [0, 2, 0], 'scale': [0.3, 0.3, 0.3]},
                    'firstperson_lefthand': {'rotation': [0, 135, 0], 'translation': [0, 2, 0], 'scale': [0.3, 0.3, 0.3]}}})
    names, tagged = {}, []
    for wood in WOODS:
        name = f'{wood}_bed'
        textures = lambda color: {'post': f'fourtwenty:block/bed/{wood}_post', 'rail': f'fourtwenty:block/bed/{wood}_rail',
                                  'panel': f'fourtwenty:block/bed/{wood}_panel', 'sheet': 'fourtwenty:block/bed/sheet',
                                  'pillow': 'fourtwenty:block/bed/pillow', 'blanket': f'fourtwenty:block/bed/blanket_{color}',
                                  'runner': f'fourtwenty:block/bed/runner_{color}'}
        write(ASSETS / f'models/block/{name}_head.json', {'parent': 'fourtwenty:block/bed_head',
                                                          'textures': {'post': f'fourtwenty:block/bed/{wood}_post'}})
        for color in COLORS:
            for shape in SHAPES:
                for occupied in (False, True):
                    write(ASSETS / f'models/block/{model_name(f"{name}_{color}", shape, occupied)}.json', {
                        'parent': f'fourtwenty:block/{model_name("bed", shape, occupied)}', 'textures': textures(color)})
        # The head half draws nothing whatever its state; the foot half draws the whole column.
        variants = {'part=head': {'model': f'fourtwenty:block/{name}_head'}}
        for facing, rot in ROTATION.items():
            for color in COLORS:
                for shape in SHAPES:
                    for occupied in ('false', 'true'):
                        model = f'fourtwenty:block/{model_name(f"{name}_{color}", shape, occupied == "true")}'
                        variants[f'blanket={color},facing={facing},occupied={occupied},part=foot,shape={shape}'] = \
                            {'model': model, 'y': rot} if rot else {'model': model}
        write(ASSETS / f'blockstates/{name}.json', {'variants': variants})
        # The item shows its blanket colour: an item model per colour, picked by the "fourtwenty:blanket"
        # property (dye id / 16, registered in ModClientEvents); no component means lime.
        for color in COLORS:
            write(ASSETS / f'models/item/{name}_{color}.json', {'parent': 'fourtwenty:item/bed_template', 'textures': textures(color)})
        write(ASSETS / f'models/item/{name}.json', {'parent': 'fourtwenty:item/bed_template', 'textures': textures('lime'),
            'overrides': [{'predicate': {'fourtwenty:blanket': i / 16}, 'model': f'fourtwenty:item/{name}_{color}'}
                          for i, color in enumerate(COLORS)]})
        # Breaking the bed keeps its blanket colour on the item.
        write(DATA / f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{
            'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fourtwenty:{name}', 'conditions': [{
                'condition': 'minecraft:block_state_property', 'block': f'fourtwenty:{name}', 'properties': {'part': 'head'}}],
                'functions': [{'function': 'minecraft:copy_state', 'block': f'fourtwenty:{name}', 'properties': ['blanket']}]}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        # Any bed of this wood + a dye -> the same bed with that blanket.
        for color in COLORS:
            write(DATA / f'recipe/{name}_{color}_blanket.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                'group': 'fourtwenty_bed_dye', 'ingredients': [{'item': f'fourtwenty:{name}'}, {'item': f'minecraft:{color}_dye'}],
                'result': {'id': f'fourtwenty:{name}', 'count': 1, 'components': {'minecraft:block_state': {'blanket': color}}}})
        # 3 weed buds (the hemp bedding) over 3 planks -> 1 hemp bed.
        write(DATA / f'recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'group': 'fourtwenty_bed',
            'pattern': ['BBB', 'PPP'],
            'key': {'P': {'item': f'minecraft:{wood}_planks'}, 'B': {'item': 'fourtwenty:weed_bud'}},
            'result': {'id': f'fourtwenty:{name}', 'count': 1}})
        names[name] = f'{title(wood)} Hemp Bed'
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
    data['item.fourtwenty.bed.blanket'] = 'Blanket: %s'
    data['block.fourtwenty.bed_extension'] = 'Hemp Bed'
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{len(names)} beds generated')


if __name__ == '__main__':
    generate()
