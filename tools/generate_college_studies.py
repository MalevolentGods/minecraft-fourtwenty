#!/usr/bin/env python3
"""The Weed College's board (specs/017-weed-college data-model.md, contracts/study-data.md): one table, 63 studies.

    python tools/generate_college_studies.py

Writes:
- src/main/resources/data/fourtwenty/college/studies/<id>.json (one per study)
- the study and branch lang keys, merged into lang/en_us.json (the file's order kept, ours added at the end)
- the crafter recipes the Armory and Glassworks studies unlock (data/minecolonies/crafterrecipes/<crafter>/fourtwenty_*.json),
- the MineColonies research effects those recipes name (data/fourtwenty/researches/effects/<study>.json): MineColonies only honours a
  recipe's research-id when its research tree knows the effect, so each unlock study's effect is defined there (research R6),
  gated by the MineColonies research effect each study applies (research R6)

Validates the tree as it writes and exits non-zero on: an unknown parent, a parent of a higher tier, a cycle, an unknown item,
or a tier-6 study whose branch has no tier-5 study. Names approved by the user on 2026-11-27.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / 'src/main/resources'
OUT = RES / 'data/fourtwenty/college/studies'
CRAFTERS = RES / 'data/minecolonies/crafterrecipes'
EFFECTS = RES / 'data/fourtwenty/researches/effects'
RESEARCHES = RES / 'data/fourtwenty/researches/college'
BRANCH = RES / 'data/fourtwenty/researches/college.json'
LANG = RES / 'assets/fourtwenty/lang/en_us.json'
NS = 'fourtwenty:'

BRANCHES = {'botany': 'Botany', 'chemistry': 'Chemistry', 'glassworks': 'Glassworks', 'textiles': 'Textiles',
            'engineering': 'Engineering', 'armory': 'Armory'}

# ---- colours (codes as TerpeneDiscovery keys them: mc_<dye> for the 16 common colours, <family>_<hue>_<tone> for hemp)
BASES = ['myr', 'lim', 'pin', 'lav']
TONES = ['lim', 'pin', 'lav', 'myr']                      # Terpenes.TONE_ORDER
GROUPS = {   # tier-1 groups of the common colours, by HueFamilies' family (neutrals: no family)
    'chem_neutrals': ('The Neutrals', ['white', 'light_gray', 'gray', 'black', 'brown']),
    'chem_warms': ('The Warms', ['red', 'orange', 'yellow']),
    'chem_greens_blues': ('The Greens and Blues', ['green', 'lime', 'cyan', 'light_blue', 'blue']),
    'chem_purples': ('The Purples', ['purple', 'magenta', 'pink']),
}
# HueFamilies.HEMP: accord -> family; unmapped accords are neutral
ACCORD_FAMILY = {'lim_pin': 'REDS', 'lim_lav': 'REDS', 'lim_lim': 'YELLOWS', 'pin_lim': 'GREENS', 'pin_pin': 'GREENS', 'pin_myr': 'GREENS',
                 'lav_pin': 'BLUES', 'lav_lav': 'BLUES', 'pin_lav': 'BLUES', 'lav_myr': 'PURPLES', 'lav_lim': 'PURPLES'}
FAMILY_GROUP = {'REDS': 'chem_warms', 'YELLOWS': 'chem_warms', 'GREENS': 'chem_greens_blues', 'BLUES': 'chem_greens_blues',
                'PURPLES': 'chem_purples', None: 'chem_neutrals'}

# ---- strains (Strain ids) and the hybrid ladder (domain/Crosses.java)
CROSSES = ['tropical_thunder', 'razzle_dazzle', 'alarm_clock_assassin', 'couch_fort', 'moss_boss', 'deep_thoughts', 'lucid_lagoon', 'rocket_lime']
DOUBLE_CROSSES = ['hurricane_haze', 'sledgehammer_serenade', 'caffeine_comet', 'old_growth', 'oracle_of_snacks', 'polar_vortex']
ULTRAS = ['cosmic_couch', 'storm_shepherd', 'the_elder_bud']
BIOMES = [('bot_dust_devil', 'Dust Devil', 'dust_devil', 'minecraft:sand'),
          ('bot_snowblind', 'Snowblind', 'snowblind', 'minecraft:snow_block'),
          ('bot_bog_butter', 'Bog Butter', 'bog_butter', 'minecraft:lily_pad'),
          ('bot_canopy_creeper', 'Canopy Creeper', 'canopy_creeper', 'minecraft:jungle_sapling')]

BUD, SEEDS, BOTTLE = NS + 'weed_bud', NS + 'weed_seeds', 'minecraft:glass_bottle'


def s(item, count, strain=None, per_colour=False):
    d = {'item': item, 'count': count}
    if strain:
        d['strain'] = strain
    if per_colour:
        d['per_colour'] = True
    return d


STUDIES = []   # dicts: id, name, branch, tier, parents, kind, supplies, gives, icon, needs, desc


def add(sid, name, branch, tier, parents, kind, supplies, gives, icon, desc, needs=None):
    STUDIES.append(dict(id=NS + sid, name=name, branch=branch, tier=tier, parents=[NS + p for p in parents], kind=kind,
                        supplies=supplies, gives=gives, icon=icon, needs=needs, desc=desc))


# ---- Chemistry (23)
COLOUR_SUPPLIES = [s(BUD, 8, per_colour=True), s(SEEDS, 4, per_colour=True), s(BOTTLE, 1, per_colour=True)]   # the Think Tank's, per colour
for sid, (name, dyes) in GROUPS.items():
    add(sid, name, 'chemistry', 1, [], 'colours', COLOUR_SUPPLIES, {'colours': ['mc_' + d for d in dyes]}, 'minecraft:' + dyes[0] + '_dye',
        'Your dyers learn ' + ', '.join(d.replace('_', ' ') for d in dyes[:-1]) + ' and ' + dyes[-1].replace('_', ' ') + '.')
for first in BASES:
    for hue in BASES:
        accord = f'{first}_{hue}'
        tier = 2 if first in ('myr', 'lim') else 3
        add('chem_accord_' + accord, 'Accord Row', 'chemistry', tier, [FAMILY_GROUP[ACCORD_FAMILY.get(accord)]], 'colours',
            [s(BUD, 16), s(NS + 'resin', 4), s(BOTTLE, 4)], {'colours': [f'{accord}_{t}' for t in TONES], 'accord': accord},
            NS + 'terpene_' + accord, "Your dyers learn the accord's four hemp colours.")
add('chem_lean_extraction', 'Lean Extraction', 'chemistry', 4, list(GROUPS), 'effect', [s(BUD, 32), s(BOTTLE, 8), s(NS + 'rosin', 2)],
    {'effect': 'terpinator_bi', 'strength': 0.5}, NS + 'terpinator', "The colony's Terpinators draw half their BI.")
add('chem_full_spectrum', 'Full Spectrum', 'chemistry', 5, ['chem_accord_' + a + '_' + b for a in BASES for b in BASES], 'effect',
    [s(BUD, 48), s(BOTTLE, 12), s(NS + 'rosin', 4)], {'effect': 'terpinator_bi', 'strength': 0.25}, NS + 'terpinator',
    "The colony's Terpinators draw a quarter of their BI.")
add('chem_master_blend', 'Master Blend', 'chemistry', 6, ['chem_full_spectrum'], 'effect', [s(BUD, 64), s(BOTTLE, 16), s(NS + 'oil', 4)],
    {'effect': 'terpinator_bi', 'strength': 0.0}, NS + 'terpinator', "The colony's Terpinators need no BI at all.")

# ---- Botany (11)
add('bot_soil_science', 'Soil Science', 'botany', 1, [], 'effect', [s('minecraft:bone_meal', 32), s(BUD, 16)],
    {'effect': 'farm_bonus_bud', 'strength': 0.25}, 'minecraft:bone_meal', "The colony's weed farms: a 1 in 4 chance of an extra bud a harvest.")
for sid, name, strain, climate in BIOMES:
    add(sid, name, 'botany', 2, ['bot_soil_science'], 'strains', [s(SEEDS, 8, 'landrace'), s(climate, 4), s(BUD, 8)],
        {'strains': [strain], 'seeds_each': 4}, SEEDS, f'Four {name} seeds for the warehouse.')
add('bot_selective_breeding', 'Selective Breeding', 'botany', 3, ['bot_soil_science'], 'effect', [s('minecraft:bone_meal', 32), s(BUD, 32)],
    {'effect': 'farm_bonus_bud', 'strength': 0.5}, 'minecraft:bone_meal', "The colony's weed farms: an even chance of an extra bud.")
add('bot_crosses', 'The Crosses', 'botany', 3, ['bot_soil_science'], 'strains', [s(SEEDS, 16), s(BUD, 32), s('minecraft:bone_meal', 8)],
    {'strains': CROSSES, 'seeds_each': 2}, SEEDS, 'Two seeds of each of the eight Crosses.')
add('bot_double_crosses', 'The Double Crosses', 'botany', 4, ['bot_crosses'] + [b[0] for b in BIOMES], 'strains',
    [s(SEEDS, 12), s(BUD, 48), s('minecraft:bone_meal', 16)], {'strains': DOUBLE_CROSSES, 'seeds_each': 2}, SEEDS,
    'Two seeds of each of the six Double Crosses.')
add('bot_ultras', 'The Ultras', 'botany', 5, ['bot_double_crosses'], 'strains', [s(SEEDS, 6), s(BUD, 64), s('minecraft:bone_meal', 32)],
    {'strains': ULTRAS, 'seeds_each': 2}, SEEDS, 'Two seeds of each of the three Ultras.')
add('bot_master_grower', 'Master Grower', 'botany', 5, ['bot_selective_breeding'], 'effect', [s('minecraft:bone_meal', 64), s(BUD, 64)],
    {'effect': 'farm_bonus_bud', 'strength': 1.0}, 'minecraft:bone_meal', "The colony's weed farms: an extra bud every harvest.")
add('bot_golden_god', 'Golden God', 'botany', 6, ['bot_ultras', 'bot_master_grower'], 'strains',
    [s(SEEDS, 1, 'cosmic_couch'), s(SEEDS, 1, 'storm_shepherd'), s(SEEDS, 1, 'the_elder_bud'), s(BUD, 64), s('minecraft:gold_block', 2)],
    {'strains': ['golden_god'], 'seeds_each': 2}, SEEDS, 'Two Golden God seeds.')

# ---- Engineering (7)
def eng(sid, name, tier, parents, effect, strength, supplies, icon, desc):
    add(sid, name, 'engineering', tier, parents, 'effect', supplies, {'effect': effect, 'strength': strength}, icon, desc)


eng('eng_bench_habits', 'Bench Habits', 1, [], 'trim_cure_speed', 1.25, [s(NS + 'hemp_wick', 8), s(BUD, 16)], NS + 'snip_station',
    "The colony's trimmers and curers work a quarter faster.")
eng('eng_long_burn', 'Long Burn', 2, ['eng_bench_habits'], 'hookah_burn', 1.25, [s('minecraft:charcoal', 32), s(BUD, 16), s(NS + 'hemp_wick', 8)],
    NS + 'weed_bong', "The colony's Hookahs burn a quarter longer on each bud and charcoal.")
eng('eng_deep_stash', 'Deep Stash', 3, ['eng_long_burn'], 'stash_capacity', 1.5, [s(NS + 'resinator', 8), s(NS + 'stash_jar', 2), s(BUD, 32)],
    NS + 'stash_jar', "The colony's Stashes hold half as much again.")
eng('eng_grow_light_tuning', 'Grow Light Tuning', 3, ['eng_long_burn'], 'grow_op_bi', 0.5, [s(NS + 'resinator', 8), s(NS + 'glass_wick', 8), s(BUD, 32)],
    NS + 'grow_tent', "The colony's Grow Ops draw half their BI.")
eng('eng_pressure_work', 'Pressure Work', 4, ['eng_bench_habits'], 'press_speed', 1.25, [s(NS + 'glass_wick', 16), s(NS + 'rosin', 4), s(BUD, 48)],
    NS + 'rosin_rig', "The colony's Rosin Rigs and Pheno Chambers work a quarter faster.")
eng('eng_assembly_line', 'Assembly Line', 5, ['eng_pressure_work'], 'trim_cure_speed', 1.5, [s(NS + 'glass_wick', 32), s(NS + 'rosin', 8), s(BUD, 64)],
    NS + 'bud_barber', "The colony's trimmers and curers work half again as fast.")
eng('eng_boundless_grid', 'Boundless Grid', 6, ['eng_assembly_line'], 'hookah_burn', 2.0, [s(NS + 'glass_wick', 64), s(NS + 'oil', 8), s(BUD, 64)],
    NS + 'big_bertha', "The colony's Hookahs burn twice as long.")

# ---- Glassworks (7): bongs at the Glassblower
BONGS = [('gla_glassblowing', 'Glassblowing', 1, [], 'glass_bong', [s('minecraft:glass', 16), s('minecraft:water_bucket', 1)]),
         ('gla_jackrabbit_bong', 'Jackrabbit Bong', 2, ['gla_glassblowing'], 'jackrabbit_bong', [s('minecraft:rabbit_foot', 2), s('minecraft:sugar', 4)]),
         ('gla_owl_bong', 'Owl Bong', 2, ['gla_glassblowing'], 'owl_bong', [s('minecraft:golden_carrot', 4), s('minecraft:spyglass', 1)]),
         ('gla_deep_sea_bong', 'Deep Sea Bong', 3, ['gla_owl_bong'], 'deep_sea_bong', [s('minecraft:prismarine_crystals', 4), s('minecraft:nautilus_shell', 1)]),
         ('gla_forge_bong', 'Forge Bong', 4, ['gla_deep_sea_bong'], 'forge_bong', [s('minecraft:blaze_rod', 2), s('minecraft:magma_cream', 4)]),
         ('gla_sage_bong', 'Sage Bong', 5, ['gla_forge_bong'], 'sage_bong', [s('minecraft:amethyst_shard', 8), s('minecraft:book', 4)]),
         ('gla_phantom_bong', 'Phantom Bong', 6, ['gla_sage_bong'], 'phantom_bong', [s('minecraft:phantom_membrane', 4), s('minecraft:feather', 8)])]
for sid, name, tier, parents, bong, extra in BONGS:
    add(sid, name, 'glassworks', tier, parents, 'unlock', [s('minecraft:glass', 8 * tier)] + extra,
        {'research_effect': NS + 'effects/' + sid, 'unlocks': NS + bong, 'crafter': 'glassblower'}, NS + bong,
        f'Your Glassblower learns to make the {name.replace("Glassblowing", "Glass Bong")}.')

# ---- Textiles (8): the Tie Dye Shack's patterns (spec 018 data-model.md; names approved 2026-11-28), in pattern order
PATTERNS = [('crumple', 'Crumple', 1), ('stripes', 'Stripes', 2), ('bullseye', 'Bullseye', 2), ('spiral', 'Spiral', 3),
            ('sunburst', 'Sunburst', 3), ('shibori', 'Shibori', 4), ('heart', 'Heart', 5), ('mandala', 'Mandala', 6)]
for i, (key, name, tier) in enumerate(PATTERNS):
    parents = [] if i == 0 else ['tex_crumple'] if tier < 6 else ['tex_heart']
    add(f'tex_{key}', name, 'textiles', tier, parents, 'effect', [s('minecraft:white_wool', 8 * tier), s(BUD, 8 * tier)],
        {'effect': None, 'strength': 0, 'pattern': i}, 'minecraft:white_wool', f'Your Tie Dye Shack learns the {name} pattern.', needs='tie_dye')

# ---- Armory (7): gear at the Blacksmith
GEAR = [('arm_dab_cell', 'Dab Cell', 1, [], 'dab_cell', {'fourtwenty:hemp_wick': 1, 'fourtwenty:resin': 3, 'fourtwenty:stash_jar': 1}, None),
        ('arm_weedscout_uniform', 'Weedscout Uniform', 1, [], 'weedscout_uniform', {}, 'weedscout_uniform'),
        ('arm_hotbox_helm', 'Hotbox Helm', 2, ['arm_dab_cell'], 'hotbox_helm', {'fourtwenty:natural_hemp': 4, 'fourtwenty:stash_jar': 1}, None),
        ('arm_moon_boots', 'Moon Boots', 3, ['arm_hotbox_helm'], 'moon_boots', {'fourtwenty:natural_hemp': 2, 'fourtwenty:stash_jar': 1, 'fourtwenty:kief': 2}, None),
        ('arm_couchlock_greaves', 'Couchlock Greaves', 4, ['arm_moon_boots'], 'couchlock_greaves',
         {'fourtwenty:natural_hemp': 4, 'fourtwenty:stash_jar': 1, 'fourtwenty:hash': 2}, None),
        ('arm_puff_plate', 'Puff Plate', 5, ['arm_couchlock_greaves'], 'puff_plate', {'fourtwenty:natural_hemp': 4, 'fourtwenty:rosin': 3, 'fourtwenty:stash_box': 1}, None),
        ('arm_blazer', 'The Blazer', 6, ['arm_puff_plate'], 'blazer', {'fourtwenty:oil': 1, 'fourtwenty:glass_wick': 3, 'fourtwenty:stash_box': 1}, None)]
for sid, name, tier, parents, piece, recipe, needs in GEAR:
    supplies = [s(item, count * 2) for item, count in recipe.items()] + [s(BUD, 16 * tier)]
    if not recipe:
        supplies = [s('minecraft:green_wool', 8), s(BUD, 16)]
    add(sid, name, 'armory', tier, parents, 'unlock', supplies,
        {'research_effect': NS + 'effects/' + sid, 'unlocks': NS + piece, 'crafter': 'blacksmith', 'recipe': recipe}, NS + piece if recipe else 'minecraft:green_wool',
        f'Your Blacksmith learns to make the {name.replace("The ", "")}.', needs=needs)


# ---- validation
def known_items():
    items = set()
    for folder in ('models/item',):
        for f in (RES / 'assets/fourtwenty' / folder).glob('*.json'):
            items.add(NS + f.stem)
    return items


def validate():
    errors = []
    ids = [st['id'] for st in STUDIES]
    if len(ids) != len(set(ids)):
        errors.append('duplicate ids')
    by_id = {st['id']: st for st in STUDIES}
    ours = known_items()
    for st in STUDIES:
        for p in st['parents']:
            if p not in by_id:
                errors.append(f"{st['id']}: unknown parent {p}")
            elif by_id[p]['tier'] > st['tier']:
                errors.append(f"{st['id']}: parent {p} has a higher tier")
        for sup in st['supplies']:
            if sup['item'].startswith(NS) and sup['item'] not in ours:
                errors.append(f"{st['id']}: unknown item {sup['item']}")
        if st['icon'].startswith(NS) and st['icon'] not in ours:
            errors.append(f"{st['id']}: unknown icon {st['icon']}")
        if st['tier'] == 6 and not any(o['branch'] == st['branch'] and o['tier'] == 5 for o in STUDIES):
            errors.append(f"{st['id']}: tier 6 with no tier-5 study in its branch")
    seen, stack = {}, set()

    def visit(i):
        if seen.get(i) == 2:
            return
        if i in stack:
            errors.append(f'{i}: part of a cycle')
            return
        stack.add(i)
        for p in by_id[i]['parents']:
            if p in by_id:
                visit(p)
        stack.discard(i)
        seen[i] = 2
    for i in by_id:
        visit(i)
    return errors


def write():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob('*.json'):
        old.unlink()
    lang = {f'college.fourtwenty.branch.{k}': v for k, v in BRANCHES.items()}
    for st in STUDIES:
        path = st['id'].split(':')[1]
        data = {k: st[k] for k in ('branch', 'tier', 'parents', 'kind', 'supplies', 'gives', 'icon', 'needs')}
        (OUT / f'{path}.json').write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
        lang[f'college.fourtwenty.study.{path}'] = st['name']
        lang[f'college.fourtwenty.study.{path}.desc'] = st['desc']
    # the research effects the unlock studies apply (research R6), defined for every unlock study, the waiting ones too
    EFFECTS.mkdir(parents=True, exist_ok=True)
    RESEARCHES.mkdir(parents=True, exist_ok=True)
    for old in list(EFFECTS.glob('*.json')) + list(RESEARCHES.glob('*.json')):
        old.unlink()
    (BRANCH).write_text(json.dumps({'base-time': 0.0, 'branch-name': 'college.fourtwenty.research_branch', 'branch-type': 'unlockables',
                                    'hidden': True, 'sortOrder': 300}, indent=2) + '\n', encoding='utf-8')
    lang['college.fourtwenty.research_branch'] = 'The Weed College'
    for st in STUDIES:
        effect = st['gives'].get('research_effect')
        if not effect:
            continue
        path = effect.split(':')[1].split('/')[-1]
        definition = {'effect': True, 'name': f'college.fourtwenty.effect.{path}'}
        (EFFECTS / f'{path}.json').write_text(json.dumps(definition, indent=2) + '\n', encoding='utf-8')
        lang[f'college.fourtwenty.effect.{path}'] = f"Weed College: {st['name']}"
        # MineColonies knows an effect id only if some research grants it, so each has a research in a hidden branch that nothing
        # starts (no autostart, no University window). The College applies the effect itself; the research also names the recipe's lock.
        research = {'branch': NS + 'college', 'name': f'college.fourtwenty.effect.{path}', 'effects': [{'id': effect, 'level': 1}],
                    'hidden': True, 'icon': st['icon'], 'researchLevel': 1}
        (RESEARCHES / f'{path}.json').write_text(json.dumps(research, indent=2) + '\n', encoding='utf-8')
    # crafter recipes the unlock studies gate (research R6); the weedscout uniform waits for its item
    for crafter in ('blacksmith', 'glassblower'):
        for old in (CRAFTERS / crafter).glob('fourtwenty_*.json') if (CRAFTERS / crafter).exists() else []:
            old.unlink()
    for st in STUDIES:
        g = st['gives']
        if st['kind'] != 'unlock' or st['needs']:
            continue
        if g['crafter'] == 'blacksmith':
            inputs = [{'id': item, 'count': count} if count > 1 else {'id': item} for item, count in g['recipe'].items()]
            level = 3
        else:
            sid = st['id'].split(':')[1]
            extra = next(b for b in BONGS if b[0] == sid)
            if sid == 'gla_glassblowing':
                inputs = [{'id': 'minecraft:glass', 'count': 4}, {'id': 'minecraft:water_bucket'}]
            else:
                inputs = [{'id': NS + 'glass_bong'}] + [{'id': e['item']} for e in extra[5]]
            level = 1
        recipe = {'type': 'recipe', 'crafter': g['crafter'] + '_crafting', 'inputs': inputs, 'intermediate': 'minecraft:air',
                  'min-building-level': level, 'research-id': g['research_effect'], 'result': {'id': g['unlocks']}, 'show-tooltip': True}
        folder = CRAFTERS / g['crafter']
        folder.mkdir(parents=True, exist_ok=True)
        (folder / f"fourtwenty_{g['unlocks'].split(':')[1]}.json").write_text(json.dumps(recipe, indent=2) + '\n', encoding='utf-8')
    cur = json.loads(LANG.read_text(encoding='utf-8'))
    cur.update(lang)
    LANG.write_text('{\n' + ',\n'.join(f'  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}' for k, v in cur.items()) + '\n}\n',
                    encoding='utf-8')


if __name__ == '__main__':
    problems = validate()
    for p in problems:
        print('ERROR:', p)
    if problems:
        sys.exit(1)
    write()
    counts = {}
    for st in STUDIES:
        counts[st['branch']] = counts.get(st['branch'], 0) + 1
    print(f'{len(STUDIES)} studies:', ', '.join(f'{BRANCHES[b]} {n}' for b, n in counts.items()))
