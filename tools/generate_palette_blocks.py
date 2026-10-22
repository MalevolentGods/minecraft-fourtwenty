#!/usr/bin/env python3
"""Concrete, rugs and lamps in the 64 terpene colours, from tools/terpenes.json.

Concrete (concrete_<code>): one flat colour, like vanilla concrete. Concrete powder
(concrete_powder_<code>): vanilla's ConcretePowderBlock, so it falls and sets into its concrete in
water; fine aggregate specks. Vanilla's powder recipe with 1 sand and 1 gravel swapped for weed buds
(3 sand, 3 gravel, 2 buds, the final terpene) makes 4. MineColonies' Concrete Mixer gets the same two
recipes (data/minecolonies/crafterrecipes/concretemixer/fourtwenty_*): it recognises powder by its
ConcretePowderBlock and sets it in its pond.

Glass and panes (glass_<code>, glass_pane_<code>): stained glass, recoloured from the white stained
glass and pane edge in tools/source_textures/faithful (Classic Faithful 64x, Faithful License;
see its README). A terpene in the middle of 8 glass (or 8 panes) dyes 8; 6 glass make 16 panes.

Rugs: see tools/generate_rugs.py (they join into larger rugs). Formerly here: rugs (rug_<code>, and rug_natural): one pixel thick and inset a pixel from every edge, so the
block underneath always shows round them, with rounded corners (stepped bands of quarter-pixel
geometry). Woven texture: a darker border, a light pinstripe, corner diamonds and a central
medallion. 2 hemp blocks of the colour make 3; a terpene in the middle of 8 rugs recolours them.

Lamps (lamp_<code>, inverted_lamp_<code>, lamp_trim_<code>): like the 16 dye-colour lamps (a Lamp
lights while powered, an Inverted Lamp unless powered, a Trim is an always-lit plate), with their
very texture recoloured (16x16) and a halo in their own colour (LampGlow.forRgb). 3 glass, a weed
bud and the final terpene make 4.

Concrete and rug textures are 64x64 and seeded. Writes textures, models, blockstates, item models, loot,
recipes, tags and English names. Run again after edits.
"""
import collections
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA = RES / 'data/fourtwenty'
TERP = json.loads((ROOT / 'tools/terpenes.json').read_text(encoding='utf-8'))
S = 64
VANILLA_COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan',
                  'purple', 'blue', 'brown', 'green', 'red', 'black']


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, f):
    return tuple(clamp(v * f) for v in c[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def lum(c):
    return (0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]) / 255


def vary(c, f, dark_lift=0.3):
    """Vary a colour by f (a share), plus a little absolute light so blacks still show texture."""
    return tuple(clamp(v * (1 + f) + 255 * f * (dark_lift - 0.25 * lum(c))) for v in c)


def loot(name):
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fourtwenty:{name}'}],
                                                  'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}


# ---- concrete --------------------------------------------------------------------------------

def flat_texture(c):
    """Concrete: one flat colour."""
    return Image.new('RGB', (16, 16), c)


def glass_texture(c, template):
    """A stained glass texture: the template's transparency (face, frame, glint) in colour c."""
    t = Image.open(ROOT / 'tools/source_textures/faithful' / template).convert('RGBA')
    tp = t.load()
    img = Image.new('RGBA', t.size)
    px = img.load()
    for y in range(t.size[1]):
        for x in range(t.size[0]):
            r, g, b, a = tp[x, y]
            px[x, y] = (clamp(c[0] * r / 255), clamp(c[1] * g / 255), clamp(c[2] * b / 255), a)
    return img


def concrete_texture(c, seed):
    rng = random.Random(seed)
    img = Image.new('RGB', (S, S))
    px = img.load()
    for y in range(S):
        for x in range(S):
            f = rng.uniform(-0.018, 0.018)
            r = rng.random()
            if r < 0.025:
                f += rng.uniform(0.04, 0.07)          # pale aggregate
            elif r < 0.05:
                f -= rng.uniform(0.04, 0.07)          # dark aggregate
            px[x, y] = vary(c, f)
    for _ in range(24):                               # a few pinholes
        x, y = rng.randrange(S), rng.randrange(S)
        px[x, y] = vary(c, -0.12)
    return img


# ---- rugs ------------------------------------------------------------------------------------

RUG_INSET = 1.0         # units from the block edge
RUG_RADIUS = 2.0        # corner radius, units
RUG_STEPS = [1.0, 1.25, 1.5, 1.75, 2.25, 3.0]     # band edges (z from the block edge) for the rounded ends


def rug_elements():
    """The rug: a slab one pixel thick, its rounded ends built from bands that only touch."""
    def inset_at(d):
        """x inset at depth d (units from the block edge) on a corner of radius RUG_RADIUS."""
        r = RUG_RADIUS
        t = r - (d - RUG_INSET)
        return RUG_INSET + (r - math.sqrt(max(0.0, r * r - t * t)) if t > 0 else 0)
    els = []
    def box(x0, z0, x1, z1, faces):
        f, t = [x0, 0, z0], [x1, 1, z1]
        uv = {'north': [16 - x1, 15, 16 - x0, 16], 'south': [x0, 15, x1, 16], 'east': [16 - z1, 15, 16 - z0, 16],
              'west': [z0, 15, z1, 16], 'up': [x0, z0, x1, z1]}
        els.append({'from': [round(v, 4) for v in f], 'to': [round(v, 4) for v in t],
                    'faces': {fc: {'texture': '#rug', 'uv': [round(v, 4) for v in uv[fc]]} for fc in faces}})
    bands = list(zip(RUG_STEPS, RUG_STEPS[1:]))
    for a, b in bands:
        xi = inset_at((a + b) / 2)
        # North end (outermost band first; each band's face towards the middle is covered by the next, wider one).
        box(xi, a, 16 - xi, b, ('north', 'east', 'west', 'up'))
        box(xi, 16 - b, 16 - xi, 16 - a, ('south', 'east', 'west', 'up'))
    box(RUG_INSET, RUG_STEPS[-1], 16 - RUG_INSET, 16 - RUG_STEPS[-1], ('east', 'west', 'up'))
    return els


def rug_texture(c, seed):
    """64x64: the rug from pixel 4 to 60 (1 to 15 units); outside it, the edge colour the sides sample."""
    rng = random.Random(seed)
    light = mix(c, (246, 240, 226), 0.55) if lum(c) < 0.75 else shade(c, 0.72)
    border = shade(c, 0.74) if lum(c) > 0.18 else mix(c, (120, 120, 120), 0.25)
    motif = mix(c, (246, 240, 226), 0.35) if lum(c) < 0.6 else shade(c, 0.82)
    img = Image.new('RGB', (S, S))
    px = img.load()
    rows = [rng.uniform(-1, 1) for _ in range(S)]
    for y in range(S):
        for x in range(S):
            edge = min(x, y, S - 1 - x, S - 1 - y) - 4         # pixels in from the rug's edge
            weave = (0.03 if (x // 1 + y // 1) % 2 == 0 else -0.03) + 0.012 * rows[y] + rng.uniform(-0.012, 0.012)
            if edge < 0:
                col = shade(border, 0.82)                       # what the rug's sides show
            elif edge < 6:
                col = border
            elif edge == 6:
                col = light                                     # the pinstripe
            elif edge == 7:
                col = border
            else:
                col = c
                # Corner diamonds and the central medallion (a diamond in a diamond).
                cx, cy = abs(x - 31.5), abs(y - 31.5)
                d = cx + cy
                if d <= 15:
                    col = motif if d > 12 or d <= 6 else c
                    if d <= 3:
                        col = border
                for kx, ky in ((15, 15), (48, 15), (15, 48), (48, 48)):
                    if abs(x - kx) + abs(y - ky) <= 3:
                        col = motif if abs(x - kx) + abs(y - ky) > 1 else border
            px[x, y] = vary(col, weave)
    return img


# ---- lamps -----------------------------------------------------------------------------------

def lamp_texture(c, lit, seed=None):
    """The dye-colour lamps' own texture (white_lamp_on/off, ProjectRed's illumar lamp, MIT), recoloured:
    the one-pixel grey frame kept as it is, the inside's light-to-dark shading multiplied onto the
    palette colour (greyed and dimmed for off, as the dye colours are). 16x16, like them."""
    template = Image.open(ASSETS / f'textures/block/white_lamp_{"on" if lit else "off"}.png').convert('RGBA')
    tp = template.load()
    inner = [(x, y) for y in range(1, 15) for x in range(1, 15)]
    mean = sum(sum(tp[p][:3]) / 3 for p in inner) / len(inner)
    if lit:
        target = c
    else:
        l = lum(c) * 255 * 0.85
        target = shade(mix(c, (l, l, l), 0.45), 0.88)
    img = Image.new('RGBA', template.size)
    px = img.load()
    for y in range(template.size[1]):
        for x in range(template.size[0]):
            r, g, b, a = tp[x, y]
            if x in (0, 15) or y in (0, 15):
                px[x, y] = (r, g, b, a)                       # the frame, unchanged
            else:
                f = ((r + g + b) / 3) / mean                  # the shading, relative to the middle
                px[x, y] = tuple(clamp(v * f) for v in target) + (a,)
    return img


TRIM_ROTATION = {'down': {}, 'up': {'x': 180}, 'north': {'x': 270}, 'south': {'x': 270, 'y': 180},
                 'west': {'x': 270, 'y': 270}, 'east': {'x': 270, 'y': 90}}


def builtin_item(particle):
    """An item model drawn by LampItemRenderer, held and shown like a block (as the dye-colour lamps)."""
    return {'parent': 'minecraft:builtin/entity', 'textures': {'particle': particle}, 'gui_light': 'side', 'display': {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'head': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}}


# ---- everything --------------------------------------------------------------------------------

def generate():
    names = collections.OrderedDict()
    tex = ASSETS / 'textures/block'
    for sub in ('concrete', 'rug', 'lamp'):
        (tex / sub).mkdir(parents=True, exist_ok=True)
    colors = [(c['code'], c['name'], c['hex']) for c in TERP['colors']]
    # Concrete (flat) and concrete powder (specked; sets into the concrete in water).
    for old in list(DATA.glob('recipe/concrete_*.json')):
        old.unlink()                                     # the old 8 concrete + terpene recipes
    old_tag = DATA / 'tags/item/concrete.json'
    if old_tag.exists():
        old_tag.unlink()
    mixer = RES / 'data/minecolonies/crafterrecipes/concretemixer'
    for sub in ('concrete_powder', 'glass'):
        (tex / sub).mkdir(parents=True, exist_ok=True)
    for i, (code, name, hexc) in enumerate(colors):
        c = rgb(hexc)
        flat_texture(c).save(tex / f'concrete/{code}.png')
        concrete_texture(c, 5000 + i).save(tex / f'concrete_powder/{code}.png')
        for block, texture in ((f'concrete_{code}', f'concrete/{code}'), (f'concrete_powder_{code}', f'concrete_powder/{code}')):
            write(ASSETS / f'models/block/{block}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'fourtwenty:block/{texture}'}})
            write(ASSETS / f'blockstates/{block}.json', {'variants': {'': {'model': f'fourtwenty:block/{block}'}}})
            write(ASSETS / f'models/item/{block}.json', {'parent': f'fourtwenty:block/{block}'})
            write(DATA / f'loot_table/blocks/{block}.json', loot(block))
        powder = f'concrete_powder_{code}'
        ingredients = [{'item': 'minecraft:sand'}] * 3 + [{'item': 'minecraft:gravel'}] * 3 + \
                      [{'item': 'fourtwenty:weed_bud'}] * 2 + [{'item': f'fourtwenty:terpene_{code}'}]
        write(DATA / f'recipe/{powder}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'building',
            'group': 'fourtwenty_concrete_powder', 'ingredients': ingredients, 'result': {'id': f'fourtwenty:{powder}', 'count': 4}})
        write(mixer / f'fourtwenty_{powder}.json', {'type': 'recipe', 'crafter': 'concretemixer_custom', 'inputs': [
            {'count': 3, 'id': 'minecraft:sand'}, {'count': 3, 'id': 'minecraft:gravel'}, {'count': 2, 'id': 'fourtwenty:weed_bud'},
            {'id': f'fourtwenty:terpene_{code}'}], 'intermediate': 'minecraft:air', 'result': {'count': 4, 'id': f'fourtwenty:{powder}'}})
        write(mixer / f'fourtwenty_concrete_{code}.json', {'type': 'recipe', 'crafter': 'concretemixer_custom',
            'inputs': [{'id': f'fourtwenty:{powder}'}], 'intermediate': 'minecraft:air', 'result': {'id': f'fourtwenty:concrete_{code}'}})
        names[f'concrete_{code}'] = f'{name} Concrete'
        names[powder] = f'{name} Concrete Powder'
    pick = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'
    current = json.loads(pick.read_text(encoding='utf-8')) if pick.exists() else {'replace': False, 'values': []}
    current['values'] = sorted(set(current['values']) | {f'fourtwenty:concrete_{code}' for code, _, _ in colors})
    write(pick, current)
    shovel = RES / 'data/minecraft/tags/block/mineable/shovel.json'
    current = json.loads(shovel.read_text(encoding='utf-8')) if shovel.exists() else {'replace': False, 'values': []}
    current['values'] = sorted(set(current['values']) | {f'fourtwenty:concrete_powder_{code}' for code, _, _ in colors})
    write(shovel, current)
    # Glass and glass panes (translucent).
    pane_parts = {'post': 'template_glass_pane_post', 'side': 'template_glass_pane_side', 'side_alt': 'template_glass_pane_side_alt',
                  'noside': 'template_glass_pane_noside', 'noside_alt': 'template_glass_pane_noside_alt'}
    for code, name, hexc in colors:
        c = rgb(hexc)
        glass_texture(c, 'white_stained_glass.png').save(tex / f'glass/{code}.png')
        glass_texture(c, 'white_stained_glass_pane_top.png').save(tex / f'glass/{code}_pane_top.png')
        glass, pane = f'glass_{code}', f'glass_pane_{code}'
        textures = {'pane': f'fourtwenty:block/glass/{code}', 'edge': f'fourtwenty:block/glass/{code}_pane_top'}
        write(ASSETS / f'models/block/{glass}.json', {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                                                     'textures': {'all': f'fourtwenty:block/glass/{code}'}})
        write(ASSETS / f'blockstates/{glass}.json', {'variants': {'': {'model': f'fourtwenty:block/{glass}'}}})
        write(ASSETS / f'models/item/{glass}.json', {'parent': f'fourtwenty:block/{glass}'})
        for part, parent in pane_parts.items():
            write(ASSETS / f'models/block/{pane}_{part}.json', {'parent': f'minecraft:block/{parent}', 'render_type': 'minecraft:translucent',
                                                               'textures': textures})

        def m(part, **r):
            return dict({'model': f'fourtwenty:block/{pane}_{part}'}, **r)
        write(ASSETS / f'blockstates/{pane}.json', {'multipart': [
            {'apply': m('post')},
            {'when': {'north': 'true'}, 'apply': m('side')}, {'when': {'east': 'true'}, 'apply': m('side', y=90)},
            {'when': {'south': 'true'}, 'apply': m('side_alt')}, {'when': {'west': 'true'}, 'apply': m('side_alt', y=90)},
            {'when': {'north': 'false'}, 'apply': m('noside')}, {'when': {'east': 'false'}, 'apply': m('noside_alt')},
            {'when': {'south': 'false'}, 'apply': m('noside_alt', y=90)}, {'when': {'west': 'false'}, 'apply': m('noside', y=270)}]})
        write(ASSETS / f'models/item/{pane}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'fourtwenty:block/glass/{code}'}})
        # Glass drops only with silk touch, like vanilla glass.
        for block in (glass, pane):
            write(DATA / f'loot_table/blocks/{block}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
                {'type': 'minecraft:item', 'name': f'fourtwenty:{block}'}], 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {
                    'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]}]})
        write(DATA / f'recipe/{glass}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'group': 'fourtwenty_glass',
            'pattern': ['GGG', 'GTG', 'GGG'], 'key': {'G': {'item': 'minecraft:glass'}, 'T': {'item': f'fourtwenty:terpene_{code}'}},
            'result': {'id': f'fourtwenty:{glass}', 'count': 8}})
        write(DATA / f'recipe/{pane}.json', {'type': 'minecraft:crafting_shaped', 'category': 'decorations', 'group': 'fourtwenty_glass_pane',
            'pattern': ['GGG', 'GGG'], 'key': {'G': {'item': f'fourtwenty:{glass}'}}, 'result': {'id': f'fourtwenty:{pane}', 'count': 16}})
        write(DATA / f'recipe/{pane}_dyeing.json', {'type': 'minecraft:crafting_shaped', 'category': 'decorations',
            'group': 'fourtwenty_glass_pane_dye', 'pattern': ['PPP', 'PTP', 'PPP'],
            'key': {'P': {'tag': 'c:glass_panes'}, 'T': {'item': f'fourtwenty:terpene_{code}'}},
            'result': {'id': f'fourtwenty:{pane}', 'count': 8}})
        names[glass] = f'{name} Glass'
        names[pane] = f'{name} Glass Pane'
    for kind, ids in (('glass_blocks', [f'fourtwenty:glass_{c}' for c, _, _ in colors]), ('glass_panes', [f'fourtwenty:glass_pane_{c}' for c, _, _ in colors])):
        write(RES / f'data/c/tags/item/{kind}.json', {'replace': False, 'values': ids})
        write(RES / f'data/c/tags/block/{kind}.json', {'replace': False, 'values': ids})
    # Lamps.
    for i, (code, name, hexc) in enumerate(colors):
        c = rgb(hexc)
        for lit in (True, False):
            state = 'on' if lit else 'off'
            lamp_texture(c, lit).save(tex / f'lamp/{code}_{state}.png')
            write(ASSETS / f'models/block/lamp_{code}_{state}.json',
                  {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'fourtwenty:block/lamp/{code}_{state}'}})
        for block, item_state in ((f'lamp_{code}', 'off'), (f'inverted_lamp_{code}', 'on')):
            write(ASSETS / f'blockstates/{block}.json', {'variants': {
                'lit=true': {'model': f'fourtwenty:block/lamp_{code}_on'}, 'lit=false': {'model': f'fourtwenty:block/lamp_{code}_off'}}})
            write(ASSETS / f'models/item/{block}.json', builtin_item(f'fourtwenty:block/lamp/{code}_on') if item_state == 'on'
                  else {'parent': f'fourtwenty:block/lamp_{code}_off'})
            write(DATA / f'loot_table/blocks/{block}.json', loot(block))
        trim = f'lamp_trim_{code}'
        write(ASSETS / f'models/block/{trim}.json', {'parent': 'fourtwenty:block/lamp_trim', 'textures': {'lamp': f'fourtwenty:block/lamp/{code}_on'}})
        write(ASSETS / f'blockstates/{trim}.json', {'variants': {f'facing={d}': {'model': f'fourtwenty:block/{trim}', **r} for d, r in TRIM_ROTATION.items()}})
        write(ASSETS / f'models/item/{trim}.json', builtin_item(f'fourtwenty:block/lamp/{code}_on'))
        write(DATA / f'loot_table/blocks/{trim}.json', loot(trim))
        write(DATA / f'recipe/lamp_{code}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone', 'group': 'fourtwenty_palette_lamp',
            'ingredients': [{'item': 'minecraft:glass'}] * 3 + [{'item': 'fourtwenty:weed_bud'}, {'item': f'fourtwenty:terpene_{code}'}],
            'result': {'id': f'fourtwenty:lamp_{code}', 'count': 4}})
        write(DATA / f'recipe/inverted_lamp_{code}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
            'ingredients': [{'item': f'fourtwenty:lamp_{code}'}, {'item': 'fourtwenty:weed_joint'}],
            'result': {'id': f'fourtwenty:inverted_lamp_{code}', 'count': 1}})
        write(DATA / f'recipe/{trim}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'decorations',
            'ingredients': [[{'item': f'fourtwenty:lamp_{code}'}, {'item': f'fourtwenty:inverted_lamp_{code}'}]],
            'result': {'id': f'fourtwenty:{trim}', 'count': 4}})
        names[f'lamp_{code}'] = f'{name} Lamp'
        names[f'inverted_lamp_{code}'] = f'{name} Inverted Lamp'
        names[trim] = f'{name} Lamp Trim'
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    for block, label in names.items():
        data[f'block.fourtwenty.{block}'] = label
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{len(colors)} concrete + powder, {len(colors)} glass + panes, {len(colors) * 3} lamps (rugs: tools/generate_rugs.py)')


if __name__ == '__main__':
    generate()
