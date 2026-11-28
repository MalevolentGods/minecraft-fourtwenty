# Tasks: The Tie Dye Shack

**Input**: `specs/018-tie-dye-shack/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)
**Tests**: required by the constitution (Principle VI): JUnit for `domain/tiedye/`, game tests for world behaviour, in-game checks per milestone (Principle VII).

Paths: `…` = `src/main/java/com/malevolentgods/fourtwenty`; `mc…` = `…/compat/minecolonies/tiedye`.

## Phase 1: Setup

- [X] T001 Generate the Textiles studies with their pattern names, ids (`tex_crumple` … `tex_mandala`) and `gives: {pattern: N}` in `tools/generate_college_studies.py`; regenerate data and lang
- [X] T002 [P] Colourways data `src/main/resources/data/fourtwenty/tie_dye/colourways.json` (16, from data-model.md) and lang names `tiedye.fourtwenty.colourway.<id>`, `tiedye.fourtwenty.pattern.<n>`

## Phase 2: Foundational (blocks the stories)

- [X] T003 [P] Pure rules in `…/domain/tiedye/`: `Colourway` (slot k → colour), `TieDyeRules` (orderable colourways for a known-colour set, batch size by level, supplies per batch), `OrderQueue` (add, cancel rule, head, progress), with JUnit in `src/test/java/…/domain/tiedye/TieDyeRulesTest.java`
- [X] T004 The `fourtwenty:tie_dye` item component (codec, stream codec) in `…/registry/ModDataComponents.java`; `…/tiedye/Colourways.java` loader (data reload, client copy built from the same file in the jar so items render offline)
- [X] T005 The R1 spike: `…/tiedye/TieDyeBlockEntity.java` (colourway, update tag, re-mesh on change) and a block colour handler that reads it; prove one block renders 4 tinted slots; record the result in research.md R1 (built; the visual proof is the M1 in-game check, T015)
- [X] T006 `tools/generate_tie_dye.py`: 4 slot masks per pattern for a block face (dithered edges), the 8 patterns; models with 4 cutout layers and tint indices 0-3

## Phase 3: US1 + US2, a colony's first tie-dye and only its colours (M1)

- [X] T007 [US1] `tie_dyed_hemp` block (pattern state, block entity, drops with the component, item model by pattern, item colour handler) in `…/tiedye/`
- [X] T008 [US1] `mc…/TieDyeRegistry.java`: hut block `tiedyeshack`, building, job `tie_dyer`, modules (work, orders, status); `CollegeFeatures` turns Textiles on
- [X] T009 [US1] `mc…/TieDyeSavedData.java` (orders per colony; shirts later) and `mc…/TieDyeOrders.java` (add, cancel, checks per contracts/orders.md)
- [X] T010 [US1] `mc…/TieDyeNetwork.java`: `fourtwenty:tie_dye_order` (ADD, CANCEL) with server checks
- [X] T011 [US1] `mc…/ShackBuilding.java` (scans vats = water cauldrons, racks = Curing Racks; required items; activity) and `mc…/TieDyerAI.java` (request supplies, dip at a vat, hang at a rack, deliver to the warehouse, fallback hut + pickup)
- [X] T012 [US1] Orders module + view + window (`mc…/client/OrdersWindow.java`, `gui/tiedye_orders.xml`): pattern buttons, colourway swatches, kind, count, queue with cancel; status module and tab
- [X] T013 [US2] Colour privacy in the window, item names and tooltips: uncaught colours as "?", colourway names only when all are caught (research R6)
- [X] T014 [US1] Colony game tests `src/colonytest/java/…/TieDyeGameTests.java`: order permission and checks; a batch fetched, dyed and delivered; save round trip; core game test: hemp keeps its look through place, mine, re-place
- [ ] T015 [US1] In game: quickstart M1. Report and commit (M1)

## Phase 4: US3, rugs, couches and beds (M2)

- [ ] T016 [P] [US3] Rug masks and models; `tie_dyed_rug` joining only the same pattern and colourway (reads the item in hand at placement)
- [ ] T017 [P] [US3] Couch masks and models; `tie_dyed_couch` (a `CouchBlock`, sitting works)
- [ ] T018 [P] [US3] Blanket masks and models; `tie_dyed_<wood>_bed` for the 10 woods (Fancy Bed with `pattern` in place of `blanket`; shapes and sleeping work)
- [ ] T019 [US3] Orders and the Tie Dyer handle all kinds; game tests for rug joining and each kind's look
- [ ] T020 [US3] In game: quickstart M2. Report and commit (M2)

## Phase 5: US3, shirts (M3)

- [ ] T021 [US3] `tie_dye_shirt` item; give, swap, take back on colonists (members only); shirts in `TieDyeSavedData`; dropped on death or leaving
- [ ] T022 [US3] `fourtwenty:shirt` payload (tracking, start-tracking) and `mc…/client/ShirtLayer.java` on the citizen renderer and the Lifted body; shirt masks from the generator
- [ ] T023 [US3] Colony game tests: give, swap, take, persistence
- [ ] T024 [US3] In game: quickstart M3. Report and commit (M3)

## Phase 6: US4, all patterns, levels and the hut (M4)

- [ ] T025 [US4] All 8 patterns finished in every mask set; a showcase row on the local test server
- [X] T026 [P] [US4] `tools/generate_tiedyeshack_hut_model.py` (diorama, `hut_model_check`), item model, tab icon; listed in `tools/tests/test_hut_models.py`
- [X] T027 [P] [US4] `C:\Users\Ignus\sensi-heights\tools\design_tie_dye_shack.py`: five levels (vats and racks per data-model.md, signature door from level 3, doors closed), checks pass, import into the jar
- [ ] T028 [US4] Colony game test: Tie Dyers and batch size by level
- [ ] T029 [US4] In game: quickstart M4. Report and commit (M4)

## Phase 7: US5 and the player's route (M5)

- [ ] T030 [US5] The Dye Vat (core, no MineColonies): block, menu (plain item, vials per colourway slot, pattern choice), recipe, model from a generator; game test
- [ ] T031 [US5] Handbook: `entries/colony/tie_dye_shack.md` and a core Dye Vat page; the College page names the patterns; PDF rebuilt; `check_handbook.py` passes
- [ ] T032 [US5] In game: quickstart M5, including Four Twenty without MineColonies. Report and commit (M5)

## Phase 8: Polish

- [ ] T033 CHANGELOG (with the College, one release); BUILDINGS-PLAN Phase 7 status
- [ ] T034 Full quickstart pass; open the PR (no release, no tag)

## Dependencies

- Setup → Foundational (T005 spike gates T007 onward) → M1 → M2 → M3; M4's levels (T026, T027) can run alongside M2/M3; M5 last.
- The College (017) must be on the branch (it is: 018 branches from 017).

## Parallel opportunities

- T002 with T001; T003 with T004; T016, T017 and T018; T026 and T027.

## Implementation strategy

MVP is M1: tie-dyed hemp ordered from a Shack, in the colony's colours only. Each milestone is tested in game and committed before the next.
