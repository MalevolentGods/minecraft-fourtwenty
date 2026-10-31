# Implementation Plan: Weed Processing and Boundless Inspiration

**Branch**: `001-weed-processing` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/001-weed-processing/spec.md`

## Summary

Split harvests into rough, trimmed and cured stages with seeds coming from trimming; build four tiers of trimming and curing machines; build Boundless Inspiration up to Rip tier; move the Terpinator and Infuser onto one shared power rule. `weed_bud` stays the finished item, so every existing use keeps working.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: NeoForge 21.1.218, Minecraft 1.21.1; MineColonies optional
**Storage**: Block entity NBT; per-player persisted data as today
**Testing**: JUnit (`src/test`), game tests (`src/gametest`), in game on the local test server
**Target Platform**: Dedicated server + client, same jar on both
**Project Type**: Minecraft mod
**Performance Goals**: A grid of 200 Wicks adds no visible tick lag; routes rebuild only when the grid changes
**Constraints**: No MineColonies dependency; art from `tools/generate_*.py`
**Scale/Scope**: 2 items, 18 blocks, 1 shared machine class, 1 grid, about 10 phases of tasks across 6 stages

## Constitution Check

| Principle | Status |
|---|---|
| I. MineColonies optional | Pass. No new colony code beyond the status text. |
| II. Existing machines keep their game | Pass. `weed_bud` stays the input; the Terpinator and Infuser only gain the power gate. |
| III. The power rule | Pass. `MachinePower` is the only implementation. |
| IV. Failure is safe | Pass. `BIRules` decides burnout; burnout replaces the block with ash, with no explosion. |
| V. Approved names | Pass. Plan names, plus names approved 2026-10-31. |
| VI. Rules pure and tested | Pass. `TrimRules`, `CureRules`, `BIRules`, `MachinePower` rule are pure; config supplies numbers. |
| VII. Milestones in game | Pass. Each stage ends with an in-game check and a commit. |

Re-checked after design: no violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-weed-processing/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/machine-io.md
├── contracts/bi-grid.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/java/com/malevolentgods/fourtwenty/
├── domain/        TrimRules, CureRules, BIRules, MachinePowerRule
├── power/         BINode (capability), BIGrid, MachinePower, HookahBlockEntity, StashBlockEntity,
│                  WickBlock, HitBoxBlock + entity, BurnOut
├── processing/    ProcessingConfig, TrimTrayBlock, CuringRackBlock + entity, MachineBlock,
│                  MachineBlockEntity, MachineMenu
├── client/        CuringRackRenderer, MachineScreen
├── util/StrainEffects.java     isHigh, markHigh
├── terpene/TerpinatorBlockEntity.java     power gate
├── infuser/Infuser.java        power gate on catch and infuse
└── registry/      new items, blocks, entities, menus, capability
src/main/resources/data/fourtwenty/loot_table/blocks/weed_crop.json
src/main/resources/data/fourtwenty/recipe/
src/test/java/.../domain/       TrimRulesTest, CureRulesTest, BIRulesTest, MachinePowerRuleTest
src/gametest/java/.../gametest/ ProcessingGameTests, PowerGameTests
tools/generate_processing.py, tools/generate_power.py
```

**Structure Decision**: New `power/` and `processing/` packages beside the existing feature packages (`terpene/`, `infuser/`). Pure rules stay in `domain/`.

## Stages

1. Pipeline + T1
2. BI core
3. Grid to Rip
4. Terpinator and Infuser on the shared rule
5. Powered tiers + automation
6. Docs and test build

## Complexity Tracking

No constitution violations.
