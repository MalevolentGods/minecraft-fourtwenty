# Tasks: Bud Barn

**Input**: `specs/004-bud-barn/` (spec.md, plan.md, research.md, data-model.md, quickstart.md)
**Tests**: required by the constitution (pure rules have JUnit tests, world behaviour has game tests).

`MC` = `src/main/java/com/malevolentgods/fourtwenty/compat/minecolonies`, `SH` = the `sensi-heights` repo.

## Phase 1: Setup

- [x] T001 Add item tag `fourtwenty:rough_buds` (containing `fourtwenty:rough_bud`) in `src/main/resources/data/fourtwenty/tags/item/rough_buds.json`
- [x] T002 [P] Add Bud Barn config values (`budBarnTrimTicks1`..`5`, `budBarnTrimmedBuffer`, `budBarnRoughPerTray`) to `MC/DispensaryConfig.java`

## Phase 2: Foundational (blocks every story)

- [x] T003 Move the trim logic into `TrimTrayBlock.trim(ServerLevel, BlockPos, ItemStack)` returning the trimmed bud and an optional seed; `useItemOn` calls it, in `src/main/java/com/malevolentgods/fourtwenty/processing/TrimTrayBlock.java`
- [x] T004 [P] Game test: `trim` keeps strain and traits, consumes nothing itself, and gives seeds at the configured rate, in `src/gametest/java/com/malevolentgods/fourtwenty/gametest/ProcessingGameTests.java`
- [x] T005 [P] Pure rules `BudBarnRules` (step order COLLECT > HANG > TRIM > FETCH > REQUEST > IDLE, trimmed-bud cap, trim ticks per level, status for each idle reason) in `MC/domain/BudBarnRules.java`
- [x] T006 [P] JUnit tests for `BudBarnRules` in `src/test/java/com/malevolentgods/fourtwenty/compat/minecolonies/domain/BudBarnRulesTest.java`

## Phase 3: User Story 1, a colony turns its harvest into smokeable weed (P1)

**Goal:** a Bud Barn and its Trim Daddy turn warehouse rough buds into cured buds in colony storage, no player involved.
**Independent test:** quickstart steps 1-4.

- [x] T007 [US1] `BudBarnBlock` hut block (hut name `budbarn`, pack and path `agriculture/horticulture/budbarn1`) in `MC/building/BudBarnBlock.java`
- [x] T008 [US1] `BudBarnBuilding`: schematic name, max level 5, tray and rack scan, kept items (rough buds, trimmed buds) in `MC/building/BudBarnBuilding.java`
- [x] T009 [US1] `TrimDaddyJob` in `MC/building/TrimDaddyJob.java`
- [x] T010 [US1] `TrimDaddyAI`: walk-and-use loop for COLLECT, HANG, TRIM, FETCH, REQUEST (tag request), dump to the hut, using `BudBarnRules`, in `MC/building/TrimDaddyAI.java`
- [x] T011 [US1] Register block, item, block entity, job, building and modules in `MC/BudBarnRegistry.java`, called from `MC/ColonyIntegration.java`
- [x] T012 [US1] Lang (hut, item, job, tooltip) in `src/main/resources/assets/fourtwenty/lang/en_us.json`; hut block model, blockstate and item model in `src/main/resources/assets/fourtwenty/`
- [ ] T013 [US1] Check research R5 on the local server (bred-strain cured buds fill a Budtender's request); if they don't, request cured buds by tag `fourtwenty:cured_buds` in `MC/service/BongTending.java` and `MC/building/JointCraftingModule.java`

## Phase 4: User Story 2, trimming gives the colony seeds (P2)

**Goal:** seeds from the Trim Daddy's trimming reach storage at player odds.

- [x] T014 [US2] `TrimDaddyAI` keeps seeds from `trim` with the buds and dumps them; `BudBarnBuilding` doesn't keep seeds, so couriers take them to the warehouse (`MC/building/TrimDaddyAI.java`, `MC/building/BudBarnBuilding.java`)

## Phase 5: User Story 3, the Bud Barn grows with the colony (P2)

**Goal:** five Sensi Heights levels, each with more trays and racks, and a faster worker.

- [x] T015 [US3] Blueprint generator for levels 1-5 (trays 1,1,2,2,3, racks 2,3,4,6,8; earth tones; one footprint) in `SH/tools/design_budbarn.py`, writing `SH/sensiheights/agriculture/horticulture/budbarn1-5.blueprint`
- [ ] T016 [US3] Pass `SH/tools/check_hut_levels.py` and `SH/tools/audit_tags.py`; add a BUD BARN column to `SH/tools/build_showcase.py`; update `SH/BUILDINGS.md`
- [x] T017 [US3] Import the pack into the mod with `tools/import_sensi_heights.py`; trim ticks per level read from config in `TrimDaddyAI`

## Phase 6: User Story 4, the player sees what it's doing (P3)

- [x] T018 [US4] `BudBarnStatusModule` (state, hanging count, next finish, made counts) in `MC/building/BudBarnStatusModule.java`
- [x] T019 [US4] Client status view and window in `MC/client/BudBarnStatusView.java` and `MC/client/BudBarnStatusWindow.java`, with strings in `en_us.json`

## Phase 7: Polish

- [x] T020 Full game test and JUnit runs; mod loads without MineColonies (the standalone probe)
- [x] T021 [P] README and CHANGELOG `[Unreleased]` entries
- [ ] T022 [P] Update `four-twenty/PLAN.md` and `HANDOFF.md` in `IgnusVM/claude-misc` (Bud Barn built)
- [ ] T023 Colony walkthrough on the local server (quickstart.md), then the flatworld showcase

## Dependencies

- Phase 2 before every story. T003 before T010 and T014; T005 before T010.
- US1 (T007-T013) before US2 and US4, which extend its classes. US3's blueprints (T015-T016) can be built alongside US1, but T017 and quickstart step 1 need both.

## Parallel opportunities

- T001 and T002; T004, T005 and T006; T015 and T016 (in `sensi-heights`) alongside T007-T012; T021 and T022.

## MVP

Phases 1-3 with a temporary level 1 blueprint: a working colony supply loop. Then US2, US3 and US4.
