#!/usr/bin/env python3
"""The weed progression model (specs/012-progression-balance): how long a typical player takes to climb the strain ladder.

    python tools/progression_model.py                    the run on the shipped defaults: crop times, stocking, milestones, the colony
    python tools/progression_model.py --today            the same on the numbers before 012 (the self-check against what the user saw)
    python tools/progression_model.py --config <toml>    a server's saved fourtwenty-processing-server.toml in place of the defaults

Expected values, not dice: enough to set numbers, and play confirms them.

**A crop.** A weed crop grows on a random tick:
- **Odds per tick:** it passes `cropSpeed` (0.5), then its grade's speed (hybrids), then vanilla's 1 in (25 / growth + 1), with growth about 8 on a
  well-watered field.
- **Random ticks:** each block gets randomTickSpeed (3) in 4,096 per game tick.
- **Plots:** a Grow Op plot of speed s adds (s - 1) times as many.
- **Length:** 7 stages to mature.

**Stocking a strain.** Each crop, every plant gives its seed back, its buds (the family's yield), each bud's trimming seed chance, and a Grow Op's extra
seed chance. Seeds are reinvested, and trimming leaves the bud, so the stock multiplies by 1 + yield x seedChance + plotSeedChance
a crop. A strain is "had" once 16 plants of it are growing.

**Gates** are the parts the model can't play: building the Terpinator and Infuser, the first power, the Pheno Chamber, each plot tier.
They're given as hours of play beside the farm, and are the assumptions to check in game.
"""
import argparse
import math
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / 'specs/012-progression-balance/contracts/config.md'

FAMILIES = ['Landrace', 'Neutrals', 'Warm', 'Green', 'Cool', 'Biome', 'Cross', 'DoubleCross', 'Ultra', 'Golden']
GRADE = {'Landrace': 0, 'Neutrals': 1, 'Warm': 1, 'Green': 1, 'Cool': 1, 'Biome': 1, 'Cross': 2, 'DoubleCross': 3, 'Ultra': 4, 'Golden': 5}
GRADE_SPEED = [1.0, 1.0, 0.7, 0.5, 0.35, 0.25]
GRADE_PLOT = [0, 0, 1, 3, 4, 5]
PLOT_SPEED = [1.0, 1.5, 2.0, 3.0, 5.0, 8.0]          # no plot, then tiers 1-5
CROP_SPEED = 0.5
RANDOM_TICK_SPEED, SECTION = 3, 4096
GROWTH = 8.0                                        # vanilla getGrowthSpeed on a good, wet field
STAGES = 7
STOCK = 16                                          # plants of a strain before the player moves on with it

TODAY = {   # before 012: every family 1-3 buds (avg 2), the old trimming chances, no plot seeds, potency from traits only
    'yield': {f: (1, 3) for f in FAMILIES},
    'seed': {'Landrace': .25, 'Neutrals': .25, 'Warm': .15, 'Green': .10, 'Cool': .05, 'Biome': .20, 'Cross': .04,
             'DoubleCross': .02, 'Ultra': .01, 'Golden': .005},
    'plotSeed': [0, 0, 0, 0, 0, 0],
}

# The hours of play a typical player spends on the things the model doesn't simulate, after the stage before (assumptions)
GATES = {
    'bred': 2.0,      # Terpinator and Infuser built and fed, once Landrace is stocked
    'growop': 3.0,    # the first Hookah, Wicks and a tier-1 plot, once a bred strain is stocked
    'biome': 2.0,     # travel to a climate biome and grow Landrace there
    'cross': 3.0,     # the Pheno Chamber and two parents
    'double': 4.0,    # tier-3 plots and the power for them
    'ultra': 4.0,     # tier-4 plots
    'golden': 4.0,    # tier-5 plots
}
TARGETS = {'first joint': 1, 'first bred strain': 5, 'first Grow Op': 10, 'biome strains': 17.5, 'crosses': 27.5,
           'double crosses': 40, 'ultras': 50, 'Golden God': 60}

# The colony: one joint (one cured bud) per adult citizen a day (20 minutes), at a typical citizen count by stage
COLONY = {'first bred strain': ('Neutrals', 6), 'first Grow Op': ('Neutrals', 10), 'biome strains': ('Biome', 14),
          'crosses': ('Cross', 20), 'double crosses': ('DoubleCross', 26), 'ultras': ('Ultra', 32), 'Golden God': ('Golden', 40)}
FIELD = 72               # a MineColonies field's plots (scarecrow radius 5 by 5... minus paths): what a Farmer tends per field
FIELDS_PER_CITIZEN = 0.1  # fields a colony runs per citizen


def defaults():
    """The shipped defaults, from the config contract's tables."""
    text = CONTRACT.read_text(encoding='utf-8')
    numbers = dict(TODAY, seed=dict(TODAY['seed']))
    for key, new in re.findall(r'\| `seedChance(\w+)` \| [\d.]+ \| ([\d.]+) \|', text):
        numbers['seed'][key] = float(new)
    numbers['yield'] = {}
    for fam, lo, hi in re.findall(r'\| `yield(\w+)` \| "(\d)-(\d)" \|', text):
        numbers['yield'][fam] = (int(lo), int(hi))
    plots = re.search(r'`plotSeedChance1` … `plotSeedChance5` \| ([\d., ]+) \|', text).group(1)
    numbers['plotSeed'] = [0] + [float(v) for v in plots.split(',')]
    return numbers


def from_toml(path, numbers):
    text = Path(path).read_text(encoding='utf-8')
    for fam in FAMILIES:
        m = re.search(rf'^seedChance{fam}\s*=\s*([\d.]+)', text, re.M)
        if m:
            numbers['seed'][fam] = float(m.group(1))
        m = re.search(rf'^yield{fam}\s*=\s*"(\d)-(\d)"', text, re.M)
        if m:
            numbers['yield'][fam] = (int(m.group(1)), int(m.group(2)))
    for t in range(1, 6):
        m = re.search(rf'^plotSeedChance{t}\s*=\s*([\d.]+)', text, re.M)
        if m:
            numbers['plotSeed'][t] = float(m.group(1))
    return numbers


def crop_minutes(family, plot):
    """Minutes for one crop of the family to mature on a plot tier (0 = none)."""
    per_tick = RANDOM_TICK_SPEED / SECTION * PLOT_SPEED[plot]
    grows = CROP_SPEED * GRADE_SPEED[GRADE[family]] / (math.floor(25 / GROWTH) + 1)
    return STAGES / (per_tick * grows) / 1200


def growth(numbers, family, plot):
    lo, hi = numbers['yield'][family]
    return 1 + (lo + hi) / 2 * numbers['seed'][family] + numbers['plotSeed'][plot]


def stocking(numbers, family, plot, start=1):
    """Hours from `start` seeds to STOCK plants of the family on a plot tier."""
    m = growth(numbers, family, plot)
    crops = math.ceil(math.log(STOCK / start) / math.log(m)) if start < STOCK else 0
    return crops * crop_minutes(family, plot) / 60, crops


def run(numbers):
    """The milestones' hours for a typical player."""
    out = {}
    t = crop_minutes('Landrace', 0) / 60 + 5 / 60          # the first crop, then hand trimming and five minutes on a Curing Rack
    out['first joint'] = t
    landrace, _ = stocking(numbers, 'Landrace', 0, start=4)
    t = max(t, landrace) + GATES['bred']
    out['first bred strain'] = t
    bred, _ = stocking(numbers, 'Neutrals', 0)
    t += bred + GATES['growop']
    out['first Grow Op'] = t
    biome, _ = stocking(numbers, 'Biome', 1)
    t += GATES['biome'] + biome
    out['biome strains'] = t
    for name, fam, gate, plot in (('crosses', 'Cross', 'cross', 1), ('double crosses', 'DoubleCross', 'double', 3),
                                  ('ultras', 'Ultra', 'ultra', 4), ('Golden God', 'Golden', 'golden', 5)):
        hours, _ = stocking(numbers, fam, plot)
        t += GATES[gate] + hours
        out[name] = t
    return out


def colony(numbers):
    """Each stage's daily joints asked for against what its fields grow, and the spare."""
    rows = []
    for stage, (fam, citizens) in COLONY.items():
        lo, hi = numbers['yield'][fam]
        fields = max(1, round(citizens * FIELDS_PER_CITIZEN))
        plot = GRADE_PLOT[GRADE[fam]]
        per_day = fields * FIELD * (lo + hi) / 2 * 20 / crop_minutes(fam, plot)
        rows.append((stage, fam, citizens, fields, citizens, per_day, per_day / citizens - 1))
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--today', action='store_true')
    ap.add_argument('--config')
    a = ap.parse_args()
    numbers = TODAY if a.today else defaults()
    if a.config:
        numbers = from_toml(a.config, numbers)

    print('Minutes per crop (no plot, then plot tiers 1-5):')
    for fam in FAMILIES:
        print(f'  {fam:12}', '  '.join(f'{crop_minutes(fam, p):6.1f}' for p in range(6)))
    print('\nStock growth per crop (no plot / plot tier 1 / tier 5):')
    for fam in FAMILIES:
        print(f'  {fam:12} x{growth(numbers, fam, 0):.3f}  x{growth(numbers, fam, 1):.3f}  x{growth(numbers, fam, 5):.3f}')

    if a.today:
        lo, hi = numbers['yield']['Landrace']
        per_harvest = 1 + (lo + hi) / 2 * numbers['seed']['Landrace']
        print(f'\nSelf-check: 17 buds from {17 / ((lo + hi) / 2):.1f} harvests give {17 / ((lo + hi) / 2):.1f} seeds back and '
              f'{17 * numbers["seed"]["Landrace"]:.1f} from trimming = {17 / ((lo + hi) / 2) + 17 * numbers["seed"]["Landrace"]:.1f} '
              f'(the user saw 12); the stock grows x{per_harvest:.2f} a crop.')
        return 0

    out = run(numbers)
    print('\nMilestones (hours):')
    ok = True
    for name, target in TARGETS.items():
        h = out[name]
        within = abs(h - target) <= 0.25 * target if name != 'Golden God' else 50 <= h <= 75
        ok &= within
        print(f'  {name:18} {h:6.1f}   target {target:5.1f}   {"ok" if within else "MISS"}')
    print('\nThe colony (joints a day: asked, grown, spare):')
    for stage, fam, citizens, fields, asked, grown, spare in colony(numbers):
        print(f'  {stage:18} {fam:12} {citizens:3} citizens, {fields} field(s): {asked:4.0f} asked, {grown:6.0f} grown, spare {spare:+.0%}')
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
