#!/usr/bin/env python3
"""Everything data-driven for the terpene system and hemp blocks, from tools/terpenes.json.

Writes block/item models, blockstates (blocks, stairs, slabs), loot tables, recipes (4 weed buds
-> 2 Natural Hemp; a final terpene + 8 hemp -> 8 of its colour; stairs, slabs, stonecutting),
tags, English names and flavour lines, and the Java colour table
(domain/TerpenePalette.java). Ids come from terpene codes (myr_pin_lim), never names, so a rename
never touches a world. Textures: tools/generate_terpene_textures.py. Run again after edits.
"""
import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/fourtwenty'
DATA_DIR = RES / 'data/fourtwenty'
JAVA = ROOT / 'src/main/java/com/malevolentgods/fourtwenty/domain/TerpenePalette.java'
DATA = json.loads((ROOT / 'tools/terpenes.json').read_text(encoding='utf-8'))


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def hemp_blocks():
    """(block id, texture, display name) for Natural Hemp and the 64 colours."""
    out = [('natural_hemp', 'fourtwenty:block/hemp/natural', DATA['natural']['name'])]
    out += [(f'hemp_{c["code"]}', f'fourtwenty:block/hemp/{c["code"]}', f'{c["name"]} Hemp') for c in DATA['colors']]
    return out


def stairs_blockstate(model):
    """Vanilla's stairs blockstate, for one set of models (model, model_inner, model_outer)."""
    variants = {}
    yaw = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    for facing, y0 in yaw.items():
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                m = model + ('_inner' if shape.startswith('inner') else '_outer' if shape.startswith('outer') else '')
                y = y0
                if half == 'bottom' and shape.endswith('left'):
                    y -= 90
                if half == 'top' and shape.endswith('right'):
                    y += 90
                v = {'model': m}
                if half == 'top':
                    v['x'] = 180
                if y % 360:
                    v['y'] = y % 360
                if half == 'top' or y % 360:
                    v['uvlock'] = True
                variants[f'facing={facing},half={half},shape={shape}'] = v
    return {'variants': variants}


def block_assets(block, texture):
    m = f'fourtwenty:block/{block}'
    write(ASSETS / f'models/block/{block}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture}})
    write(ASSETS / f'blockstates/{block}.json', {'variants': {'': {'model': m}}})
    sides = {'bottom': texture, 'top': texture, 'side': texture}
    for suffix, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
        write(ASSETS / f'models/block/{block}_stairs{suffix}.json', {'parent': f'minecraft:block/{parent}', 'textures': sides})
    write(ASSETS / f'blockstates/{block}_stairs.json', stairs_blockstate(f'{m}_stairs'))
    write(ASSETS / f'models/block/{block}_slab.json', {'parent': 'minecraft:block/slab', 'textures': sides})
    write(ASSETS / f'models/block/{block}_slab_top.json', {'parent': 'minecraft:block/slab_top', 'textures': sides})
    write(ASSETS / f'blockstates/{block}_slab.json', {'variants': {
        'type=bottom': {'model': f'{m}_slab'}, 'type=top': {'model': f'{m}_slab_top'}, 'type=double': {'model': m}}})
    for item, model in ((block, m), (f'{block}_stairs', f'{m}_stairs'), (f'{block}_slab', f'{m}_slab')):
        write(ASSETS / f'models/item/{item}.json', {'parent': model})


def block_data(block):
    for item in (block, f'{block}_stairs'):
        write(DATA_DIR / f'loot_table/blocks/{item}.json', {'type': 'minecraft:block', 'pools': [{
            'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fourtwenty:{item}'}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    slab = f'{block}_slab'
    write(DATA_DIR / f'loot_table/blocks/{slab}.json', {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fourtwenty:{slab}', 'functions': [
            {'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [{
                'condition': 'minecraft:block_state_property', 'block': f'fourtwenty:{slab}', 'properties': {'type': 'double'}}]},
            {'function': 'minecraft:explosion_decay'}]}]}]})
    src = {'item': f'fourtwenty:{block}'}
    write(DATA_DIR / f'recipe/{block}_stairs.json', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'group': 'fourtwenty_hemp_stairs',
        'pattern': ['X  ', 'XX ', 'XXX'], 'key': {'X': src}, 'result': {'id': f'fourtwenty:{block}_stairs', 'count': 4}})
    write(DATA_DIR / f'recipe/{block}_slab.json', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'group': 'fourtwenty_hemp_slab',
        'pattern': ['XXX'], 'key': {'X': src}, 'result': {'id': f'fourtwenty:{block}_slab', 'count': 6}})
    write(DATA_DIR / f'recipe/{block}_stairs_from_stonecutting.json', {'type': 'minecraft:stonecutting', 'ingredient': src,
        'result': {'id': f'fourtwenty:{block}_stairs', 'count': 1}})
    write(DATA_DIR / f'recipe/{block}_slab_from_stonecutting.json', {'type': 'minecraft:stonecutting', 'ingredient': src,
        'result': {'id': f'fourtwenty:{block}_slab', 'count': 2}})


def palette_java():
    def ints(hexes):
        return ', '.join(f'0x{h[1:].upper()}' for h in hexes)
    finals = [c['hex'] for c in DATA['colors']]
    by_code = {c['code']: c['hex'] for c in DATA['colors']}
    accords = [by_code[a['code'] + '_pin'] for a in DATA['accords']]
    bases = [b['hex'] for b in DATA['bases']]
    rows = lambda xs: '\n            '.join(ints(xs[i:i + 8]) + ',' for i in range(0, len(xs), 8))
    JAVA.write_text(f'''package com.malevolentgods.fourtwenty.domain;

/**
 * Terpene and hemp colours (RGB), in {{@link Terpenes}} order. Generated by
 * tools/generate_terpenes.py from tools/terpenes.json; don't edit by hand.
 */
public final class TerpenePalette {{
    private TerpenePalette() {{}}

    /** Natural Hemp. */
    public static final int NATURAL = 0x{DATA['natural']['hex'][1:].upper()};

    private static final int[] BASES = {{{ints(bases)}}};

    /** Each accord's colour: its row's bright tone. */
    private static final int[] ACCORDS = {{
            {rows(accords)}
    }};

    private static final int[] FINALS = {{
            {rows(finals)}
    }};

    /** A terpene's colour (a final terpene's is its hemp colour). */
    public static int color(Terpenes.Code code) {{
        return switch (code.tier()) {{
            case 1 -> BASES[code.index()];
            case 2 -> ACCORDS[code.index()];
            default -> FINALS[code.index()];
        }};
    }}
}}
''', encoding='utf-8')


def generate():
    names = collections.OrderedDict()
    hemp = hemp_blocks()
    for block, texture, name in hemp:
        block_assets(block, texture)
        block_data(block)
        names[f'block.fourtwenty.{block}'] = name
        names[f'block.fourtwenty.{block}_stairs'] = f'{name} Stairs'
        names[f'block.fourtwenty.{block}_slab'] = f'{name} Slab'
    write(RES / 'data/fourtwenty/tags/item/hemp_blocks.json', {'replace': False, 'values': [f'fourtwenty:{b}' for b, _, _ in hemp]})
    for kind in ('stairs', 'slabs'):
        values = [f'fourtwenty:{b}_{kind.rstrip("s")}' for b, _, _ in hemp]
        write(RES / f'data/minecraft/tags/block/{kind}.json', {'replace': False, 'values': values})
        write(RES / f'data/minecraft/tags/item/{kind}.json', {'replace': False, 'values': values})
    # 4 weed buds -> 2 Natural Hemp.
    write(DATA_DIR / 'recipe/natural_hemp.json', {'type': 'minecraft:crafting_shaped', 'category': 'building',
        'pattern': ['BB', 'BB'], 'key': {'B': {'item': 'fourtwenty:weed_bud'}}, 'result': {'id': 'fourtwenty:natural_hemp', 'count': 2}})
    # A final terpene + 8 hemp blocks of any colour -> 8 of its colour.
    for c in DATA['colors']:
        write(DATA_DIR / f'recipe/hemp_{c["code"]}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building',
            'group': 'fourtwenty_hemp_dye', 'pattern': ['HHH', 'HTH', 'HHH'],
            'key': {'H': {'tag': 'fourtwenty:hemp_blocks'}, 'T': {'item': f'fourtwenty:terpene_{c["code"]}'}},
            'result': {'id': f'fourtwenty:hemp_{c["code"]}', 'count': 8}})
    # Terpene items: models, names, flavour lines; the guide's words.
    write(ASSETS / 'models/item/sticky_situation.json', {'parent': 'minecraft:item/generated',
                                                         'textures': {'layer0': 'fourtwenty:item/terpene/resin'}})
    names['item.fourtwenty.sticky_situation'] = DATA['resin']['name']
    names['terpene.fourtwenty.resin.line'] = DATA['resin']['line']
    entries = [(b['code'], b['name'], b.get('line')) for b in DATA['bases']] + \
              [(a['code'], a['name'], None) for a in DATA['accords']] + \
              [(c['code'], c['name'], c['line']) for c in DATA['colors']]
    for code, name, line in entries:
        write(ASSETS / f'models/item/terpene_{code}.json', {'parent': 'minecraft:item/generated',
                                                           'textures': {'layer0': f'fourtwenty:item/terpene/{code}'}})
        names[f'item.fourtwenty.terpene_{code}'] = name
        if line:
            names[f'terpene.fourtwenty.{code}.line'] = line
    for b in DATA['bases']:
        for key in ('method', 'family', 'hue', 'tone', 'abbr'):
            names[f'terpene.fourtwenty.{b["code"]}.{key}'] = b[key]
    for f in DATA['families']:
        names[f'terpene.fourtwenty.family.{f["code"]}'] = f['name']
        names[f'terpene.fourtwenty.family.{f["code"]}.blurb'] = f['blurb']
    for a in DATA['accords']:
        names[f'terpene.fourtwenty.{a["code"]}.hue'] = a['hue']
    lang = ASSETS / 'lang/en_us.json'
    data = json.loads(lang.read_text(encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
    data.update(names)
    lang.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    palette_java()
    print(f'{len(hemp)} hemp blocks (+ stairs, slabs), {len(entries)} terpenes')


if __name__ == '__main__':
    generate()
