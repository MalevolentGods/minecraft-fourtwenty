# Research: Weed Progression Balance

## Where the numbers live today

- **Harvest:** `loot_table/blocks/weed_crop.json` drops 1 seed (`copy_strain`) and 1 to 3 rough buds (uniform), only when mature (age 7). `ModStrainData.CopyStrain` adds the seed's yield trait to the bud count, at least 1. Pot Pots harvest through the same table.
- **Trimming:** `TrimRules.Chances.DEFAULT` sets seed chances per family: Landrace 0.25, Neutrals 0.25, Warm 0.15, Green 0.10, Cool 0.05, Biome 0.20, Cross 0.04, Double Cross 0.02, Ultra 0.01, Golden 0.005. The config keys are `seedChance*` in `fourtwenty-processing-server.toml`.
- **Growth:** `GrowRules.CROP_SPEED` 0.5 (`cropSpeed` in config) means weed grows at half the speed of other crops. Grades slow further by `GRADE_SPEEDS` (1, 1, 0.7, 0.5, 0.35, 0.25) and need plot tiers by `GRADE_PLOTS` (0, 0, 1, 3, 4, 5).
- **Grow Ops:** speed ×1.5, 2, 3, 5, 8 for tiers 1 to 5. Tiers 4 and 5 give big harvests (×1.5 non-seed drops, rounded up) and replant (`GrowOps.onDrops`). There is no seed bonus today.
- **Highs:** `StrainEffects` gives each strain its own effects. The screen tint lasts 2 to 6 minutes by grade. Traits add `potencyBonus` (levels) and `lengthFactor`.
- **Colony:**
  - **Demand:** each eligible adult citizen wants one joint per in-game day (`DemandRules.defaults()`: 24,000 ticks).
  - **Joints:** a joint is one cured bud and two paper (`weed_joint.json`).
  - **Bud Barn:** its Trim Daddy trims and cures rough buds the colony's farms bring in (`BudBarnRules.ROUGH_PER_TRAY` 16).
- **Start:** the CurseForge pack's datapack `malevolentcolonies:starter_supply_camp` gives each player the supply camp deployer once. Hera may not run that datapack.

## Decisions

### D1. Family stats in one pure rule

- **Decision:** a new `domain/StrainStats` holds each family's yield range (min, max), potency (1 to 5) and seed chance, plus the plot tiers' seed chances. It is built from config.
- **Rationale:** the constitution puts rules in `domain/` with JUnit tests and numbers in config. One table, read in one place, is what the model mirrors.
- **Alternatives considered:**
  - Per-strain numbers: 45 strains is too many knobs, and the spec decides by family.
  - Datapack loot tables per family: loot functions can't read config.

### D2. Yield set by the loot function, not the table

- **Decision:** `weed_crop.json`'s rough bud count becomes 1. `CopyStrain` sets the count to a roll in the family's range, plus the yield trait, never below 1.
- **Rationale:** the family isn't known to the static table. The function already handles traits, and Pot Pots and the right-click harvest both go through it.
- **Alternatives considered:** a loot condition per family, which would mean 10 pools to keep in step with config.

### D3. Plot seeds in one helper for every harvest path

- **Decision:** one `GrowOps.extraSeed(level, pos, strain, rng)` adds a seed with the plot tier's chance. It is called from the drops event and from `WeedCropBlock`'s right-click harvest. It is called once per harvest; replanting at tiers 4 and 5 still takes one seed back.
- **Rationale:** both harvest paths must agree, or players would choose one over the other.

### D4. Potency scales each strain's own high

- **Decision:** a family's potency adds effect levels and lengthens each strain's own effects. Landrace's potency 1 is today's high, unchanged.
  - **Levels:** +0, +0, +1, +1, +2 for potency 1 to 5.
  - **Duration:** ×1.0, ×1.1, ×1.2, ×1.3, ×1.4.
  - **Stacking:** seed traits stack on top as now.
- **Rationale:** the spec allows at most +2 levels and at most twice as long at Golden God. Scaling each strain's own effect keeps its character (Third-Eye Tonic short, Purple Haze long).
- **Alternatives considered:** one shared high per potency, which would erase the strain effects players already know.

### D5. Starting seeds from the mod, not the datapack

- **Decision:** `compat/minecolonies/StarterSeeds` gives 4 Landrace seeds once per player, on their first login with MineColonies present. A persistent player flag `fourtwenty.starterSeeds` records it.
- **Rationale:** the deployer datapack is only in the CurseForge pack and may not be on every server (Hera). The mod is on every server. Giving them at first login puts them in hand with the deployer.
- **Alternatives considered:**
  - Editing the datapack: misses servers without it.
  - Detecting supply-camp placement: fragile, since MineColonies builds it from a blueprint.
- **Note:** players who joined before this release also get their 4 seeds once, on their next login. That is harmless.

### D6. Seed chances by family (defaults)

Following the spec's table:

| Family | Seed chance |
|---|---|
| Landrace | 0.12 |
| Neutrals | 0.15 |
| Warm | 0.15 |
| Green | 0.10 |
| Cool | 0.10 |
| Biome | 0.10 |
| Cross | 0.04 |
| Double cross | 0.02 |
| Ultra | 0.01 |
| Golden | 0.005 |

- **Note:** Cool rises from 0.05 to 0.10 and Biome falls from 0.20 to 0.10, so the ladder reads in order.

### D7. The model

- **Decision:** `tools/progression_model.py`, a deterministic expected-value simulation.
  - **Inputs:** the shipped config numbers (read from `contracts/config.md`'s table, or the toml); crop time (minutes per crop at each speed, from the random-tick rate); a player's choices (replant everything, smoke a joint an hour, upgrade plots when a stage allows).
  - **Output:** the hour each milestone is reached.
  - **Colony check:** compares a colony's daily joint demand (one per adult citizen at the citizen counts typical of each hut level) with what that stage's farm grows.
- **Rationale:** the spec asks for the hours to be checked before play. Expected values are enough to set numbers, and play confirms them.
- **Alternatives considered:** a Monte Carlo run, which the model can add later if the averages hide too much.

### D8. What stays

Growth speeds, plot requirements, breeding and recipes stay as they are (spec Assumptions). If the model shows a milestone missing its target by more than 25%, the first knobs to turn are, in order: seed chance, plot seed chance, then yield.

### D9. Tuned on the model (2026-11-09)

**What the model added:**
- **Bone meal:** works on grade 1 and under, assumed at 30 an hour, which is a few skeletons' bones or a composter.
- **Seeds compounding:** every seed is replanted the next crop.

**The plan's numbers:** Golden God at 64 hours, with the first bred strain (6.7 h), the first Grow Op (12.9 h) and crosses (36.3 h) late.

**Tuning:** following D8's order (seed chance first), Landrace 12% → 14% and Cross 4% → 8%. Every milestone now lands within 25%:

| Joint | Bred | Grow Op | Biome | Cross | Double | Ultra | Golden God |
|---|---|---|---|---|---|---|---|
| 1.1 | 5.9 | 12.1 | 16.4 | 30.5 | 41.6 | 50.4 | 58.2 |

**Caveat:** the gates (Terpinator, power, Pheno Chamber, plots) are guesses; play checks them.

**The colony (US4): no rule changed, flagged for the user.**
- **Fields:** a Farmer's field can only grow strains that need no Grow Op (grade 1 and under).
- **Supply:** one 116-plot weed field (Field 1) grows 55 to 73 joints a day.
- **Demand:** citizens ask for one joint a day each, so 6 to 40 citizens leave 80% to 800% spare.
- **Why it isn't a seed or yield problem:** a field is the smallest unit, and the colony side is set by demand.
- **Where the surplus goes:** the Dispensary sells to players and runs bong sessions.
- **The levers, for the user to choose:** citizen demand (`demandIntervalTicks`) or how much a smoke uses.
