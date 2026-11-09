# Implementation Plan: Weed Progression Balance

**Branch**: `012-progression-balance` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/012-progression-balance/spec.md`

## Summary

Make the weed ladder a roughly 60-hour climb.

- **Landrace:** gives 1 bud per harvest and a 12% trimming seed chance.
- **Strain families:** each gets its own yield range, potency and seed chance, set by a pure rule in `domain/` and read from config.
- **Grow Op plots:** each tier adds an extra-seed chance at harvest.
- **Starting seeds:** a new player gets 4 Landrace seeds once when MineColonies is present.
- **Tooltips:** show each strain's numbers.
- **Progression model:** `tools/progression_model.py` plays a typical run on the shipped numbers and reports each milestone's time. It is also used to check the colony's farm against its Dispensary.

## Technical Context

**Language/Version**: Java 21 (mod); Python 3.13 (the model tool)
**Primary Dependencies**: NeoForge 21.1.218, Minecraft 1.21.1; MineColonies 1.1.1343 optional (`compat/minecolonies`)
**Storage**: `fourtwenty-processing-server.toml` (config); a persistent player flag for the starting seeds
**Testing**:
- JUnit for `domain/` rules;
- game tests in `src/gametest` for harvests, trimming, plots and the starting seeds;
- `./gradlew runGameTestServer` with the known `malformedAndFutureSavesFailClosed` failure;
- the model tool's self-check.

**Target Platform**: NeoForge dedicated server and client
**Project Type**: Minecraft mod
**Performance Goals**: no change. A harvest adds one roll per extra seed.
**Constraints**:
- Saved configs keep their old values, so the release notes list every changed default.
- Old crops, seeds and buds keep working.

**Scale/Scope**:
- 10 strain families;
- 5 plot tiers;
- 13 config values for families and plots;
- 1 starter value.

## Constitution Check

| Principle | Check | Status |
|---|---|---|
| I. MineColonies stays optional | Starting seeds live in `compat/minecolonies` and run only with MineColonies. Every other change works alone; without MineColonies, wild weed is the start as now. | Pass |
| II. Existing machines keep their game | Terpinator and Infuser recipes and outputs untouched; only what strains yield and seed changes. | Pass |
| III. The power rule | No machine power changes. | Pass |
| IV. Failure is safe | Not touched. | Pass |
| V. Approved names | No new player-facing names; tooltip lines use existing words (yield, potency, seed chance). | Pass |
| VI. Rules are pure and tested | New `domain/StrainStats` (yields, potency, plot seeds) with JUnit tests; every number in config. | Pass |
| VII. Milestones prove themselves in game | Harvests, trimming and plots tested in game on flatworld before merge; the run's pace checked by the model, then in play. | Pass |

Re-checked after design: no changes. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/012-progression-balance/
├── plan.md              # this file
├── research.md          # decisions and where each number lives
├── data-model.md        # strain family stats, plot tiers, potency, the starter grant
├── quickstart.md        # how to check it in game and with the model
├── contracts/
│   └── config.md        # the config keys, defaults, old values (feeds the release notes)
└── checklists/requirements.md
```

### Source Code (repository root)

```text
src/main/java/com/malevolentgods/fourtwenty/
├── domain/StrainStats.java            # NEW: family base yield range, potency, plot seed chances; pure
├── domain/TrimRules.java              # Chances.DEFAULT: Landrace 0.25 → 0.12
├── processing/ProcessingConfig.java   # NEW keys: yields, potency, plot seed chances
├── registry/ModStrainData.java        # CopyStrain: a rough bud's count from the family roll + traits, at least 1
├── power/GrowOps.java                 # extra seed by plot tier on every harvest (shared helper)
├── block/WeedCropBlock.java           # right-click harvest goes through the same helper
├── util/StrainEffects.java            # potency: effect level and duration scaled by family potency
├── item/StrainItems.java              # tooltip: yield, potency, growth, seed chance
└── compat/minecolonies/StarterSeeds.java   # NEW: 4 Landrace seeds once per player
src/main/resources/data/fourtwenty/loot_table/blocks/weed_crop.json   # rough bud count 1 (the family roll sets it)
src/test/java/.../domain/StrainStatsTest.java                         # NEW
src/gametest/java/.../ProgressionGameTests.java                       # NEW
tools/progression_model.py                                             # NEW: the run model and colony check
```

**Structure Decision**: the single mod project, as now. The new rule class goes in `domain/`, the colony grant in `compat/minecolonies`, and the model in `tools/` beside the other generators.

## Complexity Tracking

None.
