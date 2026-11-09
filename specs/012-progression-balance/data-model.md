# Data Model: Weed Progression Balance

## StrainStats (domain, pure)

Built from config once per use, the way `TrimRules.Chances` is.

| Field | Type | Meaning |
|---|---|---|
| `yieldMin[family]`, `yieldMax[family]` | int, 1 to 8 | Rough buds per harvest before traits, rolled uniformly in the range |
| `potency[family]` | int, 1 to 5 | The family's potency |
| `plotSeedChance[tier]` | double, 0 to 1, tiers 1 to 5 | Chance of one extra seed at harvest on that plot tier |

Rules (all pure, JUnit-tested):

- `rollYield(family, traits, roll)`: `yieldMin + floor(roll × (yieldMax − yieldMin + 1))`, plus `traits.yieldBonus()`, at least 1.
- `potencyLevels(p)`: 0, 0, 1, 1, 2 for p = 1 to 5.
- `potencyLength(p)`: 1.0, 1.1, 1.2, 1.3, 1.4 for p = 1 to 5.
- `extraSeed(tier, roll)`: `roll < plotSeedChance[tier]` for a tier from 1 to 5; false for no plot.

Validation:

- `yieldMin ≤ yieldMax`. A bad config is clamped: max is raised to min.
- Potency is clamped to 1 to 5.
- Chances are clamped to 0 to 1.

### Defaults

| Family | Yield | Potency | Seed chance (TrimRules) |
|---|---|---|---|
| Landrace | 1–1 | 1 | 0.12 |
| Neutrals | 1–2 | 2 | 0.15 |
| Warm | 1–2 | 2 | 0.15 |
| Green | 2–2 | 2 | 0.10 |
| Cool | 2–2 | 3 | 0.10 |
| Biome | 2–2 | 3 | 0.10 |
| Cross | 2–3 | 3 | 0.04 |
| Double cross | 3–3 | 4 | 0.02 |
| Ultra | 3–4 | 4 | 0.01 |
| Golden | 4–5 | 5 | 0.005 |

Plot seed chance, tiers 1 to 5: 0.10, 0.20, 0.30, 0.40, 0.50.

## A harvest (state flow)

1. **Mature crop:** age 7, with its strain and its block entity's traits.
2. **Drops from the loot table:** 1 seed (strain and traits copied), plus 1 rough bud whose count `CopyStrain` sets to `rollYield`.
3. **On a Grow Op of tier t:**
   - `extraSeed(t)` may add one more seed of the strain and traits.
   - At tiers 4 and 5, non-seed drops are ×1.5 (rounded up), and the plant replants from one seed.
4. **Right-click harvest:** the same as above, then replant from one seed, as now.

## Potency on a smoke

A smoked strain's effects get:

- **Level:** its own level, plus `potencyLevels(familyPotency)`, plus the trait's `potencyBonus`.
- **Duration:** its own duration × `potencyLength(familyPotency)` × the trait's `lengthFactor`.

## StarterSeeds (compat/minecolonies)

| Field | Where | Meaning |
|---|---|---|
| `fourtwenty.starterSeeds` | persistent player data (boolean) | The grant has been given |
| `starterSeeds` | config int, default 4, 0 turns it off | How many Landrace seeds |

On player login with MineColonies loaded:

- **Flag not set:** give the seeds (dropped at the player's feet if the inventory is full), then set the flag.
- **Flag set:** do nothing.

## Tooltip (seeds, rough, trimmed and cured buds, joints)

- **Existing lines:** strain name and traits, as now.
- **New line:** "Yield 1–2 · Potency 2 · Growth normal". Growth reads from the grade: normal, slower, slow, slowest.
- **Existing seed-chance line on rough buds:** now reads the new chances.
