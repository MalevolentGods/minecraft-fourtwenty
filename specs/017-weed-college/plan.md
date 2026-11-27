# Implementation Plan: The Weed College

**Branch**: `017-weed-college` | **Date**: 2026-11-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/017-weed-college/spec.md`

## Summary

A new MineColonies hut, the Weed College (`fourtwenty:weedcollege`), worked by the Weedologist. It is built the way the Bud Barn is: a building class, a worker AI, a status module, and module windows. It also gets its own hut-block diorama.

**Its research is entirely its own:**
- The College keeps its own board of 63 proposed studies in six branches, defined as data and generated from one table.
- Each colony's progress and queue are saved in a colony-keyed saved-data file.
- The Weedologist works his way through physical work cycles inside the hut: fetching supplies through colony requests, planting and harvesting real crops on the hut's Grow Ops, a turn at the Think Tank, then writing up at a desk.
- Each cycle is one unit of progress, timed like the University's (72 units at tier 1, doubling each tier).
- MineColonies' University tree is never used for progress (research R1).

**What finished studies give:**
- **Colours:** colony-known colours, read by the Tie Dye Shack (spec 018).
- **Botany:** seed batches delivered to the warehouse, and better yields on the colony's weed farms.
- **Engineering and Chemistry:** colony power and machine modifiers, applied inside the shared power rules.
- **Armory and Glassworks:** each finished study turns on a MineColonies research *effect* in the colony, which unlocks our crafter recipes. The Blacksmith gets Kush Suit pieces and the Blazer; the Glassblower gets bongs.

**The building:** five levels, scripted in the Sensi Heights repo, with plots of Grow Op tier N at level N.

**Delivery:** five milestones, each tested in game. The feature ships in the same release as the Tie Dye Shack.

## Technical Context

**Language/Version**: Java 21 (Minecraft 1.21.1, NeoForge 21.1.218); Python 3.12 for tools.

**Primary Dependencies**:
- MineColonies 1.1.1343 (already a compile-only, optional dependency pinned in `validation/compatibility-lock.json`), with Structurize and BlockUI through it.
- No new dependency.

**Storage**:
- **Study definitions:** data files `data/fourtwenty/college/studies/*.json`, datapack-overridable, written by `tools/generate_college_studies.py` from one table.
- **Per-colony research record:** `CollegeSavedData`, a dimension `SavedData` keyed by colony id, the pattern of `DispensarySavedData`.
- **The College building's own counters:** the building's NBT.

**Testing**:
- JUnit for the pure rules in `domain/college/`: availability, tier caps, cost with caught colours, unit counting, bad batches, the queue, effect lookup, and tree validation (every study reachable, no cycles).
- Colony game tests (the existing `fourtwenty_colonytests` batch) for: the worker cycle, persistence across save and load, University separation, delivery of Botany seeds, and crafter-recipe gating.
- The Sensi Heights blueprint checks for all five levels; `tools/tests/test_hut_models.py` for the diorama; in-game checks for each milestone.

**Target Platform**: Minecraft client and dedicated server (local test server, flatworld, Hera on the user's go).

**Project Type**: Minecraft mod (single project), plus Python tools and the Sensi Heights blueprint repo.

**Performance Goals**:
- The College does nothing heavier than the Bud Barn per colony tick.
- The board view syncs only when it changes, as the Bud Barn's status module does.
- No per-tick scans; plots and benches are rescanned every 200 ticks at most.

**Constraints**:
- MineColonies stays optional: everything lives under `compat/minecolonies/` except the pure rules and data, which load harmlessly without it.
- Payloads go only to connections that have the channel.
- Terpinator and Infuser gameplay is unchanged (Principle II; see the Constitution Check).
- Names are approved before shipping.

**Scale/Scope**:
- 1 hut (5 levels, 5 blueprints, 1 diorama), 1 job, 63 studies in 6 branches, 2 board windows (status, board), 2 client→server actions.
- About 12 colony effects, about 12 crafter recipes, 1 Handbook page.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How |
|---|---|---|
| I. MineColonies Stays Optional | PASS | Everything that touches MineColonies is under `compat/minecolonies/college`. Every College result has a player route: colours (Infuser, Think Tank), biome strains (climates), hybrids (Pheno Chamber, Buzz Bot), bongs and Kush Suit pieces (crafting table; their recipes exist). Engineering, Chemistry and yield studies are colony-only bonuses on top of what players already have, not the only way to do anything. Tie-dye needs a player route too, which is spec 018's to give. |
| II. Existing Machines Keep Their Game | PASS after a design change, plus **one item needing the user** | The spec's "faster colony Terpinators" (FR-023) would change Terpinator gameplay, so the plan makes it **power only**: colony Terpinators draw less BI, which Principle II allows. Engineering's speed effects exclude the Terpinator and the Infuser. **The "ultimate terpene" at the top of Chemistry conflicts with Principle II twice**: it would add a Terpinator output, or make a second source of terpenes. The plan proposes **Master Blend** as the tier-6 capstone instead (colony Terpinators run with no BI), pending the user (see Complexity Tracking). |
| III. The Power Rule | PASS | Colony modifiers (BI draw, Hookah burn length, Stash size) are factors applied inside the shared `MachinePowerRule` and `BIRules` paths, keyed by "inside a colony that finished study X". They are never per machine. The College's own work draws no BI (research R2). |
| IV. Failure Is Safe | PASS | Not touched. |
| V. Approved Names | PASS with a gate | "Weed College", "Weedologist" and the six branch names are approved (brainstorm and buildings plan). The 63 study names were approved by the user on 2026-11-27. Hemp-row studies take the existing accord names. |
| VI. Rules Are Pure And Tested | PASS | `domain/college/` (StudyBoard, StudyRules, CollegeRules, ColonyEffects) is pure and JUnit-tested. Timing, bad-batch chance, supply scale and effect strengths are config values. The world behaviour has colony game tests. |
| VII. Milestones Prove Themselves In Game | PASS | Five milestones below, each tested in game on the local server and committed before the next. |
| Technical: payloads | PASS | The board's two actions use a NeoForge payload sent only when `hasChannel`. Board data goes down through MineColonies' module view sync. |
| Technical: art from generators | PASS | The diorama comes from `tools/generate_weedcollege_hut_model.py` (with `hut_model_check`). The board icons reuse item icons; any GUI texture comes from a generator. |
| Technical: compile-only APIs | PASS | MineColonies' research effect API (`IResearchEffectManager`, research effect data) is already within the pinned jar. |

**Post-design re-check:** unchanged. All PASS; the capstone was resolved by the user (Master Blend).

## Milestones

Each is tested in game on the local server and committed before the next (Principle VII).

1. **M1: the hut and one study, end to end (US1).**
   - Registry: hut block, building, job, worker module, status module.
   - `CollegeSavedData`; `domain/college` with the 4 tier-1 colour studies.
   - The Weedologist's cycle: request, plant, harvest, Think Tank, desk.
   - Status window; a placeholder level-1 blueprint (scripted) so it can be built.
   - **In game:** build it, hire, start a colour study by command, watch it finish, and confirm the University never lists it.
2. **M2: the board (US2).**
   - Board module window: branches, tier grid, parent links, locks with reasons, details pane, queue.
   - The client→server payload; permissions.
   - **In game:** queue and remove studies, see locks and reasons.
3. **M3: the whole tree and its effects (US3).**
   - All 63 studies from `tools/generate_college_studies.py`.
   - Botany seed delivery and farm yields; Engineering and Chemistry modifiers in the shared rules.
   - Armory and Glassworks research effects and crafter recipes; Textiles locked (needs spec 018).
   - Caught-colour discount; completion announcements.
   - **In game:** one study per branch.
4. **M4: the building (US4).**
   - `design_college.py` in Sensi Heights for all five levels; plots of Grow Op tier N; pack checks.
   - Diorama generator and model check.
   - **In game:** build all five levels, check the tier cap per level, and the diorama in hand and placed.
5. **M5: the page and the finish (US5).**
   - Handbook Colony Life page (voice rules), index, glossary, PDF rebuild; quickstart passes.
   - The user reviews the study names. Release waits for spec 018 (FR-026b) and the user's go.

## Project Structure

### Documentation (this feature)

```text
specs/017-weed-college/
├── plan.md              # This file
├── research.md          # Phase 0: decisions R1-R12
├── data-model.md        # Phase 1: entities, states, rules, the 63-study board
├── quickstart.md        # Phase 1: in-game validation per milestone
├── contracts/
│   ├── board-actions.md       # client→server actions, view sync, permissions
│   ├── colony-research-api.md # what spec 018, the gear and the power rules read
│   └── study-data.md          # the study JSON format and the generator table
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/java/com/malevolentgods/fourtwenty/
├── domain/college/                    # pure rules, no Minecraft or MineColonies types
│   ├── Study.java                     # id, branch, tier, parents, supplies, effect, needs
│   ├── StudyBoard.java                # the tree: lookup, validation, availability
│   ├── StudyRules.java                # units per tier, bad batches, caught-colour cost, tier caps
│   ├── ColonyResearch.java            # one colony's record: states, progress, queue (pure)
│   └── ColonyEffects.java             # effect lookup: multipliers and unlocks from done studies
├── compat/minecolonies/college/
│   ├── CollegeRegistry.java           # hut block, item, BE, job, building entry, modules
│   ├── CollegeBlock.java
│   ├── CollegeBuilding.java           # scans plots, Think Tank, desk; level → tier cap
│   ├── WeedologistJob.java
│   ├── WeedologistAI.java             # request → plant → tend/harvest → Think Tank → write up
│   ├── CollegeStatusModule.java       # status tab data (read-only)
│   ├── CollegeBoardModule.java        # board tab data (view sync)
│   ├── CollegeSavedData.java          # per-colony ColonyResearch, saved per dimension
│   ├── CollegeResearch.java           # the API others call (contracts/colony-research-api.md)
│   ├── CollegeEffects.java            # applies MineColonies research effects for unlock studies
│   ├── CollegeNetwork.java            # board actions payload (hasChannel)
│   └── CollegeStudies.java            # loads data/fourtwenty/college/studies (reload listener)
├── compat/minecolonies/client/
│   ├── CollegeStatusView.java / CollegeStatusWindow.java
│   └── CollegeBoardView.java / CollegeBoardWindow.java / StudyTreePane.java
├── power/ (existing)                  # shared rules gain a colony-modifier hook (Principle III)
└── processing/ (existing)             # trimmer/curer/Rosin Rig/Pheno speed hook

src/main/resources/
├── data/fourtwenty/college/studies/*.json          # 63 studies (generated)
├── data/fourtwenty/researches/effects/*.json       # MineColonies research effects for unlocks (if needed, R6)
├── data/minecolonies/crafterrecipes/blacksmith/*.json, glassblower/*.json   # gated by research-id
├── assets/fourtwenty/gui/college_status.xml, college_board.xml
├── assets/fourtwenty/models/block/weedcollege.json  # generated diorama
├── assets/fourtwenty/handbook/en_us/entries/colony/weed_college.md
└── blueprints/fourtwenty/sensiheights/...weedcollege1-5.blueprint (from the pack import)

src/test/java/.../domain/college/*Test.java
src/gametest/java/.../college/*                     # colony game tests
tools/generate_college_studies.py, tools/generate_weedcollege_hut_model.py
(sensi-heights repo) tools/design_college.py
```

**Structure Decision**: single mod project, following the Bud Barn's layout. Pure rules live in `domain/college`, and everything that touches MineColonies in `compat/minecolonies/college`. Blueprints come from the Sensi Heights repo through the existing pack import.

## Complexity Tracking

| Item | Why it matters | Resolution |
|---|---|---|
| Chemistry tier-6 "ultimate terpene" (spec FR-023) | Principle II: the Terpinator's outputs may not change, and it must stay the only source of terpenes. | **Resolved (user, 2026-11-27):** **Master Blend** instead: the colony's Terpinators need no BI (a power change, allowed). |
| "Faster colony Terpinators" (spec FR-023) | Principle II forbids gameplay changes to the Terpinator. | Resolved in the plan as BI draw only (Lean Extraction: half; Full Spectrum: a quarter). The spec's FR-023 is updated to match. |
