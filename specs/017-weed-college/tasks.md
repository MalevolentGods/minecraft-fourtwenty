---
description: "Tasks for spec 017, the Weed College"
---

# Tasks: The Weed College

**Input**: `specs/017-weed-college/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Tests**: required by the constitution (Principle VI): JUnit for `domain/college`, colony game tests for world behaviour, plus in-game checks per milestone (Principle VII).

**Paths**: Java under `src/main/java/com/malevolentgods/fourtwenty/` (abbreviated `…/`); tests under `src/test/java/com/malevolentgods/fourtwenty/` and `src/gametest/java/com/malevolentgods/fourtwenty/`; the Sensi Heights repo at `C:\Users\Ignus\sensi-heights`.

## Phase 1: Setup

- [X] T001 Create packages `…/domain/college/` and `…/compat/minecolonies/college/`, and add `CollegeConfig` (unit ticks 1200, bad-batch base 0.1 / floor 0.03 / step per Intelligence, caught-colour factor 0.5, seeds per strain 4 and 2 for multi-strain, every effect strength from contracts/colony-research-api.md) in `…/compat/minecolonies/college/CollegeConfig.java`, registered like `DispensaryConfig`
- [X] T002 [P] Add the College's lang keys (hut, job, status lines, board labels, lock reasons, announcements, branch names) to `src/main/resources/assets/fourtwenty/lang/en_us.json`

## Phase 2: Foundational (blocks every story)

- [X] T003 [P] `Study`, `Supply`, `Gives` and `Kind` records in `…/domain/college/Study.java`, as in data-model.md
- [X] T004 [P] `StudyRules`: units by tier (from `Studies.units`), the tier cap by level, tier 6's extra rule, cost with the caught-colour discount, and the bad-batch roll with Intelligence, in `…/domain/college/StudyRules.java`, with `src/test/java/…/domain/college/StudyRulesTest.java`
- [X] T005 `StudyBoard`: lookup, validation (unique ids, parents, no cycles, everything reachable, tier-6 rule), and availability with lock reasons in their order, in `…/domain/college/StudyBoard.java`, with `StudyBoardTest.java`
- [X] T006 `ColonyResearch`: queue (add, remove-before-start, head), progress, supplies delivered, the fixed cost, members' colours, done list, and NBT-agnostic copy/merge, in `…/domain/college/ColonyResearch.java`, with `ColonyResearchTest.java`
- [X] T007 [P] `ColonyEffects`: an effect enum and `factor(effect, doneStudies)`, where a later study replaces an earlier one in the same line, in `…/domain/college/ColonyEffects.java`, with `ColonyEffectsTest.java`
- [X] T008 `tools/generate_college_studies.py`: the 63-study table from data-model.md. It writes `src/main/resources/data/fourtwenty/college/studies/*.json`, the study lang keys, and the Blacksmith and Glassblower crafter recipes (`data/minecolonies/crafterrecipes/…`, `research-id` per contracts), and validates the tree; with `tools/tests/test_college_studies.py`
- [X] T009 `CollegeStudies` reload listener (server and client) that loads the study JSON into a `StudyBoard`, disabling only invalid studies, in `…/compat/minecolonies/college/CollegeStudies.java`
- [X] T010 `CollegeSavedData`: a per-colony `ColonyResearch`, refusing to overwrite an unreadable save (as `DispensarySavedData` does), in `…/compat/minecolonies/college/CollegeSavedData.java`
- [X] T011 `CollegeRegistry` and `CollegeBlock`: hut block, item, block entity, `fourtwenty:weedologist` job, building entry with the worker module (1 worker at levels 1 and 2, 2 from 3, Intelligence and Focus), status and board modules, and the stats module. Hooked into `…/compat/minecolonies/ColonyIntegration.java` so it loads only with MineColonies. Files in `…/compat/minecolonies/college/CollegeRegistry.java` and `CollegeBlock.java`
- [X] T012 `CollegeResearch` (the API in contracts/colony-research-api.md) and `CollegeFeatures` (`tie_dye`, `weedscout_uniform`) in `…/compat/minecolonies/college/`

**Checkpoint**: unit tests pass; the hut registers with MineColonies and is absent without it.

## Phase 3: User Story 1 (P1), a colony's first study 🎯 MVP (M1)

**Goal**: build, hire, queue a colour study; the Weedologist works it physically to completion; the University never sees it.
**Independent test**: quickstart M1.

- [X] T013 [US1] `CollegeBuilding`: scans every 200 ticks for Grow Op plots, one Think Tank and a desk (lectern); level → tier cap; counters; NBT; keeps the head study's supplies in the hut. In `…/compat/minecolonies/college/CollegeBuilding.java`
- [X] T014 [US1] `WeedologistJob` and `WeedologistAI`. The cycle is: request missing supplies (colony requests), fetch, plant on a free plot, harvest ripe plots, Think Tank, desk write-up. Each cycle adds one unit (with the bad-batch roll). It shows WAITING or PAUSED reasons, and two workers share the head study. Files `…/compat/minecolonies/college/WeedologistJob.java` and `WeedologistAI.java`
- [X] T015 [US1] Finishing a study: mark it DONE, announce it to the colony's members, call the effect hooks (stubbed until US3), and advance the queue. In `…/compat/minecolonies/college/CollegeBuilding.java` and `CollegeResearch.java`
- [X] T016 [P] [US1] Status tab: `CollegeStatusModule` in `…/compat/minecolonies/college/`, `CollegeStatusView` and `CollegeStatusWindow` in `…/compat/minecolonies/client/`, and `src/main/resources/assets/fourtwenty/gui/college_status.xml`
- [X] T017 [P] [US1] Command `/fourtwenty college queue|remove|status|finish` (officers; `finish` op-only for tests) in `…/compat/minecolonies/college/CollegeCommands.java`
- [X] T018 [P] [US1] Level-1 blueprint from `C:\Users\Ignus\sensi-heights\tools\design_college.py` (a shed with a chalkboard, 2 Closet Grows, Think Tank, desk), with the pack checks and an import into `src/main/resources/blueprints/fourtwenty/sensiheights/`
- [X] T019 [US1] Colony game tests in `src/gametest/java/…/college/CollegeStudyGameTest.java`: a study completes with fast unit ticks; University researchers never list or progress it (SC-002); progress and supplies survive worker removal and save/load (SC-005)
- [X] T020 [US1] In game: quickstart M1 on the local server. Report what was seen and commit (M1).

## Phase 4: User Story 2 (P1), choosing what to study (M2)

**Goal**: the board, with tree, locks, reasons, details and queue.
**Independent test**: quickstart M2.

- [X] T021 [US2] `CollegeNetwork`: the `fourtwenty:college_board` payload (QUEUE/REMOVE), sent only with `hasChannel`; the server checks `MANAGE_HUTS`, the building and the rules (contracts/board-actions.md). In `…/compat/minecolonies/college/CollegeNetwork.java`
- [X] T022 [US2] `CollegeBoardModule.serializeToView`: level, queue, head (units, missing supplies, working), and per-study state, lock and cost, sent only when changed. In `…/compat/minecolonies/college/CollegeBoardModule.java`
- [X] T023 [US2] `CollegeBoardView` and `CollegeBoardWindow` with `college_board.xml`: branch buttons, tier grid, details pane (supplies, length, effect, lock reason, "for the Tie Dye Shack" on colour studies per FR-026b), queue with remove. In `…/compat/minecolonies/client/` and `src/main/resources/assets/fourtwenty/gui/`
- [X] T024 [US2] `StudyTreePane`: study buttons in tier columns with lines from parents, in `…/compat/minecolonies/client/StudyTreePane.java`
- [X] T025 [US2] Colour privacy (R10): names and swatches only for viewers who discovered them, in `CollegeBoardWindow` and `StudyTreePane`
- [X] T026 [US2] Game test: queue/remove rules and the permission check, in `src/colonytest/java/…/colonytest/CollegeGameTests.java` (`theBoardChecksWhoAndWhat`: it needs a real colony, so it sits with the other College colony tests)
- [X] T027 [US2] In game: quickstart M2. Report and commit (M2).

## Phase 5: User Story 3 (P2), the whole tree and its effects (M3)

**Goal**: all 63 studies, and their effects colony-wide.
**Independent test**: quickstart M3.

- [X] T028 [US3] Spike (R6): apply a MineColonies unlock research effect from code, and check that a crafter recipe gated by it unlocks; if it doesn't work, use the fallback of a finished hidden research. Record the result in research.md
- [X] T029 [US3] `CollegeEffects`: unlock effects for Armory and Glassworks on finish and on colony load, in `…/compat/minecolonies/college/CollegeEffects.java`
- [X] T030 [P] [US3] Botany seed batches delivered at write-up (strain component, plain traits) to the hut for pickup, in `…/compat/minecolonies/college/CollegeBuilding.java` (done as FR-022 and the clarification say: straight into the warehouse closest to a College; into a College with a pickup request if it is full; dropped at the College only as a last resort)
- [X] T031 [P] [US3] Farm yield: a loot modifier on the weed crop that adds buds when a colony citizen with Soil Science, Selective Breeding or Master Grower harvests, in `…/compat/minecolonies/college/FarmYieldModifier.java` and `src/main/resources/data/fourtwenty/loot_modifiers/` (the modifier lives in core, `registry/FarmYieldModifier.java`, with its chance hook set by the MineColonies side, so its data loads without MineColonies)
- [X] T032 [US3] Colony factors in the shared rules (R7): `MachinePowerRule` (Terpinator BI, Grow Op BI), `BIRules` burn length, Stash capacity, and trimmer/curer/Rosin Rig/Pheno speed, through one `ColonyEffects` lookup cached per colony. Never Terpinator or Infuser speed. In the existing `…/domain/MachinePowerRule.java`, `…/domain/BIRules.java` and the processing speed paths, with tests
- [X] T033 [US3] Members' colours (R9): update on login and on learning a colour, for the owner and officers; the caught-colour discount fixed at start. In `…/compat/minecolonies/college/CollegeResearch.java` and a hook in `…/terpene/TerpeneDiscovery.java` (caught colours come from the Infuser, `Infuser.caughtListener`, not TerpeneDiscovery: FR-020 is about catching at the Infuser)
- [X] T034 [US3] Feature locks: Textiles studies (needs `tie_dye`) and the Weedscout Uniform (needs `weedscout_uniform`) show `Waiting for …`
- [X] T035 [US3] Game tests, one per branch effect: seeds arrive; yield bonus; a trimmer is faster inside the colony; Terpinator BI is halved with its speed unchanged; a Blacksmith recipe is unlocked. In `src/gametest/java/…/college/CollegeEffectsGameTest.java`
- [X] T036 [US3] In game: quickstart M3. Report and commit (M3).

## Phase 6: User Story 4 (P2), a College that grows (M4)

**Goal**: five levels, plots per level, the diorama.
**Independent test**: quickstart M4.

- [X] T037 [US4] `design_college.py`, all five levels: a shed with a chalkboard, growing to a quad with a greenhouse. Level N plots are Grow Op tier N; the signature door from level 3; doors saved closed; earth tones. `check_hut_levels.py`, `check_footprints.py` and `audit_tags.py` pass; import into the jar. In `C:\Users\Ignus\sensi-heights\tools\design_college.py`
- [X] T038 [P] [US4] The diorama: `tools/generate_weedcollege_hut_model.py` (using `box()` from the Dispensary generator, with `hut_model_check.check()`), the item model parent, and the hut listed in `tools/tests/test_hut_models.py` HUTS
- [X] T039 [US4] Game test: worker count by level, and the tier cap including tier 6's rule. In `src/gametest/java/…/college/CollegeLevelsGameTest.java`
- [X] T040 [US4] In game: quickstart M4, plus a showcase row on the local test server. Report and commit (M4).

## Phase 7: User Story 5 (P3), reading about it (M5)

- [X] T041 [US5] Handbook page `src/main/resources/assets/fourtwenty/handbook/en_us/entries/colony/weed_college.md` (voice rules, `needs: minecolonies`); `book.json`, glossary, index terms; `python tools/build_handbook_pdf.py`; `python tools/check_handbook.py` passes (no glossary entry: the glossary has no colony entries and sits outside the colony chapter; the page's terms feed the index)
- [X] T042 [US5] In game: quickstart M5, including no-MineColonies startup in the compat runs (SC-007). Report and commit (M5).

## Phase 8: Polish

- [X] T043 [P] CHANGELOG and README notes (the release waits for spec 018, FR-026b); update `claude-misc/four-twenty/BUILDINGS-PLAN.md` Phase 6 status (CHANGELOG and BUILDINGS-PLAN done; the README gets its "What's new" section with the release number, at release)
- [X] T044 Full quickstart pass, then open the PR (no release, no tag)

## Dependencies

- **Setup → Foundational → US1** (the MVP). US2 needs US1's building and record. US3 needs US1 (and US2's board to show it, though commands work without it).
- **US4** needs only Foundational and US1's building; it can run alongside US2 and US3.
- **US5** goes last (it describes the finished thing).
- The study table (T008) feeds the board (US2) and the effects (US3). Its first cut must carry all 63 studies, even if their effects are stubbed until US3.

## Parallel opportunities

- Phase 2: T003, T004 and T007 in parallel (separate files); T008 alongside the Java tasks.
- US1: T016, T017 and T018 in parallel once T013 is in.
- US3: T030 and T031 in parallel; T037 and T038 (US4) can run alongside US3.

## Implementation strategy

- **MVP first**: Phases 1–3 make a working College with one colour study, tested in game (M1).
- Then the board (M2), the tree and its effects (M3), the levels and diorama (M4), and the page (M5). Each milestone is committed after its in-game check, per Principle VII.
- **No release**: the College ships together with the Tie Dye Shack (spec 018).
