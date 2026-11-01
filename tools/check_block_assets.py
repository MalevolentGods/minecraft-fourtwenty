#!/usr/bin/env python3
"""Check that named blocks are complete: blockstate, models, textures, item model, name and loot. A wrong path here only shows in game as
a purple block, so run this after changing any art generator. A path with '..' in it counts as wrong: resource packs refuse it.

    python3 tools/check_block_assets.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
DATA = ROOT / 'src/main/resources/data'
BLOCKS = ('trim_tray', 'curing_rack', 'hemp_wick', 'rolled_wick', 'glass_wick', 'gold_wick', 'double_hose', 'big_bertha', 'grand_hookah', 'the_vault', 'closet_grow', 'grow_tent', 'hydro_bed', 'aero_pod',
          'cosmic_kush_pod', 'think_tank', 'stash_jar', 'stash_box',
          'mother_stash', 'hit_box', 'snip_station', 'bud_barber', 'trim_reaper', 'dry_box', 'cure_closet', 'the_humidor', 'pheno_chamber', 'pheno_lab', 'pheno_cathedral', 'transport_tube', 'rosin_rig', 'construction_joint', 'construction_blunt', 'construction_spliff', 'construction_cannon')


def model_problems(ref, seen):
    if ref in seen or not ref.startswith('fourtwenty:'):
        return []
    seen.add(ref)
    path = ASSETS / f'models/{ref.split(":")[1]}.json'
    if not path.exists():
        return [f'no model {ref}']
    model = json.loads(path.read_text())
    out = []
    if 'parent' in model:
        out += model_problems(model['parent'], seen)
    defined = model.get('textures', {})
    for key, value in defined.items():
        if value.startswith('#'):
            if value[1:] not in defined:
                out.append(f'{ref}: {key} points at undefined {value}')
        elif '..' in value or not (ASSETS / f'textures/{value.split(":")[1]}.png').exists():
            out.append(f'{ref}: no texture {value}')
    for element in model.get('elements', []):
        for face in element['faces'].values():
            if face['texture'][1:] not in defined:
                out.append(f'{ref}: a face uses undefined {face["texture"]}')
    return out


def main():
    lang = json.loads((ASSETS / 'lang/en_us.json').read_text(encoding='utf-8'))
    problems = []
    for name in BLOCKS:
        state = ASSETS / f'blockstates/{name}.json'
        if not state.exists():
            problems.append(f'{name}: no blockstate')
            continue
        data = json.loads(state.read_text())
        applied = list(data.get('variants', {}).values()) + [part['apply'] for part in data.get('multipart', [])]
        seen = set()
        for entry in applied:
            problems += model_problems(entry['model'], seen)
        item = json.loads((ASSETS / f'models/item/{name}.json').read_text())
        problems += model_problems(item['parent'], seen)
        if f'block.fourtwenty.{name}' not in lang:
            problems.append(f'{name}: no name')
        if not (DATA / f'fourtwenty/loot_table/blocks/{name}.json').exists():
            problems.append(f'{name}: no loot table')
    ports = ('construction_joint', 'construction_blunt', 'construction_spliff', 'construction_cannon', 'rosin_rig', 'pheno_chamber', 'pheno_lab', 'pheno_cathedral', 'weed_bong', 'double_hose', 'big_bertha', 'grand_hookah', 'stash_jar', 'stash_box', 'mother_stash', 'the_vault', 'snip_station', 'bud_barber', 'trim_reaper',
             'dry_box', 'cure_closet', 'the_humidor', 'grow_tent', 'hydro_bed', 'aero_pod', 'cosmic_kush_pod', 'think_tank', 'terpinator', 'terpinator_lab',
             'inspiration_infuser', 'inspiration_infuser_lab', 'hit_box_toke', 'hit_box_rip', 'hit_box_blaze')
    for name in ports:
        for side in ('north', 'east', 'south', 'west', 'up', 'down'):
            for lit in ('', '_lit'):
                problems += model_problems(f'fourtwenty:block/port/{name}_{side}{lit}', set())
    for name, sides in (('weed_bong', ('east',)), ('double_hose', ('east', 'west')), ('big_bertha', ('north', 'east', 'south', 'west')), ('grand_hookah', ('north', 'east', 'south', 'west'))):
        for side in sides:
            problems += model_problems(f'fourtwenty:block/port/{name}_hose_{side}', set())
        problems += model_problems(f'fourtwenty:block/{name}_item', set())
    print('\n'.join(problems) if problems else f'{len(BLOCKS)} blocks and {len(ports)} sets of fittings complete')
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
