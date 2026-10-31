# Tasks: Weed Processing and Boundless Inspiration

**Input**: Design documents from `specs/001-weed-processing/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md
**Tests**: Required by the constitution (Principle VI): JUnit for domain rules, game tests for world behaviour.

Paths are relative to the repo root. `J` = `src/main/java/com/malevolentgods/fourtwenty`.

## Phase 1: Setup

- [ ] T001 Add server config for trim, cure, seed and power numbers with `overrideTuning` in J/processing/ProcessingConfig.java and register it in J/FourTwenty.java

## Phase 2: Foundational

- [ ] T002 [P] Add seed chance by family in J/domain/TrimRules.java with tests in src/test/java/com/malevolentgods/fourtwenty/domain/TrimRulesTest.java
- [ ] T003 [P] Add per-tier cure time and batch in J/domain/CureRules.java with tests in src/test/java/com/malevolentgods/fourtwenty/domain/CureRulesTest.java
- [ ] T004 Register `rough_bud` and `trimmed_bud` as strain items in J/registry/ModItems.java and J/registry/ModCreativeTabs.java
- [ ] T005 [P] Extend tools/generate_strain_items.py for rough and trimmed bud art, models and client tint in J/client/StrainTint.java
- [ ] T006 Add names to src/main/resources/assets/fourtwenty/lang/en_us.json, showing `weed_bud` as "Cured Bud", and update player text that says "Weed Bud"

**Checkpoint**: The new items exist and carry strains.

## Phase 3: User Story 1 - From crop to joint by hand (P1) MVP

**Goal**: Seed to smoked joint with T1 machines only.
**Independent Test**: quickstart.md Stage 1.

- [ ] T007 [US1] Change mature crop loot to 1-3 rough buds + one seed in src/main/resources/data/fourtwenty/loot_table/blocks/weed_crop.json
- [ ] T008 [US1] Update crop and Pot Pot expectations in src/gametest/java/com/malevolentgods/fourtwenty/gametest/CropGameTests.java and PotPotGameTests.java
- [ ] T009 [US1] Add Trim Tray block (right-click snips one, no block entity) in J/processing/TrimTrayBlock.java
- [ ] T010 [US1] Add Curing Rack block and entity with 8 timed slots in J/processing/CuringRackBlock.java and J/processing/CuringRackBlockEntity.java
- [ ] T011 [P] [US1] Draw hanging buds in J/client/CuringRackRenderer.java
- [ ] T012 [P] [US1] Generate Tray and Rack models in tools/generate_processing.py
- [ ] T013 [US1] Add survival recipes in src/main/resources/data/fourtwenty/recipe/trim_tray.json and curing_rack.json
- [ ] T014 [US1] Game tests for snip, strain, rack timing and drops in src/gametest/java/com/malevolentgods/fourtwenty/gametest/ProcessingGameTests.java
- [ ] T015 [US1] Test quickstart Stage 1 in game, then commit

## Phase 4: User Story 2 - Seeds get rarer (P1)

**Goal**: Family seed rates hold in play.
**Independent Test**: 1,000 trims per family within 3 points.

- [ ] T016 [US2] Rate game test over 1,000 rolls per family in src/gametest/java/com/malevolentgods/fourtwenty/gametest/ProcessingGameTests.java
- [ ] T017 [US2] Show a strain's seed chance on the Trim Tray tooltip in J/processing/TrimTrayBlock.java

## Phase 5: User Story 3 - Power a machine while sober (P2)

**Goal**: BI grid to Rip; Terpinator and Infuser on the shared rule.
**Independent Test**: quickstart.md Stages 2-4.

- [ ] T018 [US3] Add `markHigh` and `isHigh` in J/util/StrainEffects.java and call `markHigh` from J/util/SmokeEffects.java, J/blockentity/WeedBongBlockEntity.java and J/util/DelayedEffectManager.java
- [ ] T019 [P] [US3] Add tiers and overload in J/domain/BIRules.java with tests in src/test/java/com/malevolentgods/fourtwenty/domain/BIRulesTest.java
- [ ] T020 [P] [US3] Add run state in J/domain/MachinePowerRule.java with tests in src/test/java/com/malevolentgods/fourtwenty/domain/MachinePowerRuleTest.java
- [ ] T021 [US3] Add BINode and capability in J/power/BINode.java
- [ ] T022 [US3] Add routing, version and push in J/power/BIGrid.java
- [ ] T023 [US3] Add shared rule and `spendOnce` in J/power/MachinePower.java
- [ ] T024 [US3] Add Wick block, glow and burnout in J/power/WickBlock.java and J/power/BurnOut.java, with `ash` in J/registry/ModItems.java
- [ ] T025 [US3] Add Hookah generator entity for three tiers in J/power/HookahBlock.java and J/power/HookahBlockEntity.java
- [ ] T026 [US3] Add Stash blocks for three tiers in J/power/StashBlock.java and J/power/StashBlockEntity.java
- [ ] T027 [US3] Add Hit Box in J/power/HitBoxBlock.java and J/power/HitBoxBlockEntity.java
- [ ] T028 [P] [US3] Generate power block models in tools/generate_power.py
- [ ] T029 [US3] Add power recipes in src/main/resources/data/fourtwenty/recipe/
- [ ] T030 [US3] Gate Terpinator progress with `MachinePower` in J/terpene/TerpinatorBlockEntity.java, buffer and capability included
- [ ] T031 [US3] Gate catch and infuse with `spendOnce` at the opened Infuser in J/infuser/Infuser.java and J/infuser/Infusion.java
- [ ] T032 [US3] Game tests for run, pause, burnout and step-down in src/gametest/java/com/malevolentgods/fourtwenty/gametest/PowerGameTests.java
- [ ] T033 [US3] Test quickstart Stages 2-4 in game, then commit

## Phase 6: User Story 4 - Automate the line (P2)

**Goal**: Six powered machines with item input and output.
**Independent Test**: quickstart.md Stage 5.

- [ ] T034 [US4] Add shared block, entity and `WorldlyContainer` in J/processing/MachineBlock.java and J/processing/MachineBlockEntity.java
- [ ] T035 [US4] Add menu and screen in J/processing/MachineMenu.java and J/client/MachineScreen.java
- [ ] T036 [US4] Register six machines and item capability in J/registry/ModBlocks.java, J/registry/ModBlockEntities.java and J/FourTwenty.java
- [ ] T037 [P] [US4] Extend tools/generate_processing.py with six machine models
- [ ] T038 [US4] Add upgrade recipes in src/main/resources/data/fourtwenty/recipe/
- [ ] T039 [US4] Game tests for hopper in and out, strains, tier speed in src/gametest/java/com/malevolentgods/fourtwenty/gametest/ProcessingGameTests.java
- [ ] T040 [US4] Test quickstart Stage 5 in game, then commit

## Phase 7: User Story 5 - Colonies keep running (P3)

**Goal**: Supplied buds work; missing buds are reported.
**Independent Test**: A Dispensary with and without cured buds.

- [ ] T041 [US5] Change "weed buds" status text to cured buds in src/main/resources/assets/fourtwenty/lang/en_us.json
- [ ] T042 [US5] Test a Dispensary both ways and the game without MineColonies, then commit

## Phase 8: Polish

- [ ] T043 [P] Update README.md, the in-game guide text and CHANGELOG.md
- [ ] T044 Run the full quickstart regression on a copy of a 0.14.1 world
- [ ] T045 Open the PR; release 0.15.0 only on the user's go

## Dependencies

Setup → Foundational → US1 → US2. US3 follows Foundational. US4 needs US3. US5 needs US1. Polish is last.

## Parallel Examples

- T002, T003 and T005 touch different files.
- T019 and T020 are pure rules.
- T028 and T037 are art generators.

## Implementation Strategy

Stage 1 is T001-T017. Each stage ends with an in-game test and commit. Everything ships together as 0.15.0.
