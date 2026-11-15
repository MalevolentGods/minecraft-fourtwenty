# Tasks: Lifted Body

**Input**: Design documents from `/specs/015-lifted-body/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/config-and-ui.md, quickstart.md

**Tests**: included. The constitution (VI) requires pure rules with JUnit tests and world behaviour in game tests.

Paths: `J` = `src/main/java/com/malevolentgods/fourtwenty`, `MC` = `J/compat/minecolonies`, `T` = `src/test/java/com/malevolentgods/fourtwenty/domain`, `CT` = `src/colonytest/java/com/malevolentgods/fourtwenty/colonytest`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task).
- **[Story]**: US1 every trip flown, US2 doors and buildings, US3 the look.

---

## Phase 1: Setup

- [X] T001 Add the server switches `bodyFlight` and `liftedLook` (both default true) to the `[lifted]` section of `MC/builder/BuilderConfig.java`, with accessors, per contracts/config-and-ui.md.
- [X] T002 [P] Add the client setting `liftedLook` (default true) to `MC/client/MidnightMachineClientConfig.java` (config values only; no client classes, per the 0.26.0 dedicated-server fix).

---

## Phase 2: Foundational (blocks US1 and US2)

- [X] T003 **Prove the takeover first (research R1).** Read MineColonies' `MinecoloniesAdvancedPathNavigate.setPathJob`, `isDone`, `tick`, `walkTo` and `EntityNavigationUtils.walkToPos/walkToBuilding` (javap on `.compat/dependencies/minecolonies-*.jar`) and record in research.md R1: what callers do with the returned `PathResult`, which result status makes them wait, and how arrival is judged. Decide the returned result (status, path) before writing the mixin.
- [X] T004 [P] Write `T/ErrandTest.java`: an errand is reached within the arrival distance; progress means getting at least 2 blocks closer within 30 s; a new destination replaces the old and resets progress; no progress for 30 s says "recall".
- [X] T005 Create `J/domain/Errand.java` (pure): destination, purpose (job or break), `reached(distance)`, `progress(distance, now)` with the 30 s rule (research R4). Make T004 pass.

**Checkpoint**: the takeover approach is proven on paper and the errand rule is tested.

---

## Phase 3: User Story 1 - Every trip is flown while she's Lifted (Priority: P1) 🎯 MVP

**Goal**: every walk MineColonies asks of a Lifted Builder (job, hut, racks, restaurant, bed, hospital) is flown; she never walks while high and is never stranded.

**Independent Test**: quickstart M1 steps 1-3, 5-7: a Warehouse upgrade with full-inventory and fetch trips and a meal and bedtime, all flown, with no `stranded`, `recalled` or `rescue` lines.

### Tests

- [ ] T006 [P] [US1] Write `CT/NavigatorTakeoverGameTests.java`: a Lifted Builder's walk request becomes an errand and her navigator reports not done until she's within the arrival distance; a sober Builder's walk is MineColonies' own; with `bodyFlight` off, a Lifted Builder's non-site walks are MineColonies' own. Add the new mixin's handlers to `CT/BuilderHookGameTests.java`.

### Implementation

- [X] T007 [US1] Create `MC/mixin/NavigatorTakeoverMixin.java` on `MinecoloniesAdvancedPathNavigate`, per T003: `setPathJob` HEAD records the destination as her errand via `LiftedFlight.errand(...)` and returns an in-progress result without a walking path; `isDone()` false while the errand flies, true once reached; `tick()` and `moveTo(...)` cancelled while an errand flies. Only for a Lifted Builder allowed to fly with `bodyFlight` on; `require = 0`. Register it in `src/main/resources/fourtwenty.minecolonies.mixins.json`.
- [X] T008 [US1] Add errands to `MC/builder/LiftedFlight.java`: `errand(citizen, destination, purpose)` flies to the destination in legs (reusing the home-flight machinery: routes, stalls, climb-over, landing), lands there, and reports reached; a new destination retargets; no progress for 30 s (`Errand`) recalls her (to her hut, or to the errand's building for a break). Spec 013's build-site `approach` keeps its own path.
- [X] T009 [US1] Breaks (research R3) in `MC/builder/LiftedFlight.java`: end any flight when she rides a seat or sleeps (`isPassenger()`, sleeping); land exactly at a bed or seat destination so MineColonies seats her or puts her to bed itself.
- [X] T010 [US1] Retire spec 013's separate rescues now covered by errands, in `MC/builder/LiftedService.java` and `MC/builder/LiftedFlight.java`: the stranded check and `walkIn` apply only with `bodyFlight` off.
- [ ] T011 [US1] Log errands in `MC/builder/BuilderLog.java` (`errand citizen= purpose= to=`, `reached`, `recalled`) under `logBuilderPathing`, and show "Flying to dinner / bed / work" style detail on her high badge via `MC/builder/HighStatusPayload.java` and `MC/client/HighStatusClient.java` *(wording for the user to approve)*.
- [ ] T012 [US1] **In game (M1 without doors):** quickstart M1 steps 1-3, 5-7 on flatworld; record the results.

**Checkpoint**: every trip flies; she's never stranded.

---

## Phase 4: User Story 2 - She flies into and out of closed buildings (Priority: P1)

**Goal**: she enters and leaves closed buildings through doors and gates, opening and closing them, and moves between floors through stairwells.

**Independent Test**: quickstart M1 step 4: an inside repair of a closed, multi-floor building with doors shut; every door ends closed.

### Tests

- [X] T013 [P] [US2] Write `T/FlightPlannerDoorsTest.java`: a route through a closed wooden door or gate cell exists at a small extra cost and marks the doorway cells; iron doors and trapdoors are walls; a route avoids a door when an open way is as short.
- [ ] T014 [P] [US2] Write `CT/DoorwaysGameTests.java`: a Lifted Builder flying a route through a closed door opens it as she reaches it and closes it once 2 blocks past; a door someone else opened is left alone; a fence gate the same.

### Implementation

- [X] T015 [US2] Teach `J/domain/FlightPlanner.java` doorways: a `Free` that can say "doorway" (a passable door or gate cell, extra cost), route cells flagged as doorways (research R2). Make T013 pass.
- [X] T016 [US2] Create `MC/builder/Doorways.java`: which blocks count (wooden doors and fence gates only, FR-004), open as she comes within 1.5 blocks of a doorway on her route, close what she opened once she's 2 blocks past and it's empty, upright and at door height through the doorway. Make T014 pass.
- [X] T017 [US2] Use doorways in `MC/builder/LiftedFlight.java`: the planner's free test includes doorways; `fly()` calls `Doorways` for doorway cells ahead; the build-site `approach` uses them too, so blocks inside closed buildings are reached by flying in (no more walk-in with `bodyFlight` on).
- [ ] T018 [US2] Unreachable blocks (FR-006) in `MC/builder/LiftedFlight.java`: when no route reaches a block, place it from outside if it's in reach, otherwise let MineColonies move on to the next block; never stuck for good.
- [ ] T019 [US2] **In game (M1 step 4):** an inside repair of the Warehouse with its doors shut; record which doors were used and that all ended closed.

**Checkpoint**: Stories 1 and 2 together: SC-001, SC-002, SC-003.

---

## Phase 5: User Story 3 - Her look, polished (Priority: P2)

*(Descoped 15 Nov: she keeps her look. The new model, per-Builder tint, design pass and look switches, the old T020-T035, are dropped.)*

**Goal**: a still cruise pose in flight and the joint at her mouth on a smoke break.

**Independent Test**: quickstart M3.

- [ ] T041 [US3] Cruise pose in `MC/mixin/CitizenDanceMixin.java` and `MC/client/DanceAnimation.java`: while she's flying (moving and off the ground), cancel the walking stride (legs together, no limb swing), recline slightly and lean into her direction, easing over 0.3 s; the dance takes over when she hovers. Expose "flying" to the client from her motion (no new payload).
- [ ] T042 [US3] Joint at her mouth on a smoke break in `MC/mixin/CitizenDanceMixin.java`: while she holds a joint (main hand) and isn't flying, raise the right arm to her mouth for about a second every few seconds, between puffs.
- [X] T043 [US3] Drop the unused look switches: `liftedLook` in `MC/builder/BuilderConfig.java` and `MC/client/MidnightMachineClientConfig.java`.
- [ ] T044 [US3] **In game (M3):** a long flight, a smoke break, building with the polish on and off (SC-004, SC-005).

---

## Phase 7: Polish

- [ ] T036 Performance (SC-006): three Lifted Builders on flatworld, tick time against the same colony sober; record in quickstart.md.
- [ ] T037 [P] Without MineColonies, the standalone game tests pass (SC-007); colony game tests show nothing new against `main`.
- [ ] T038 [P] CHANGELOG `[Unreleased]` and `CURSEFORGE.md`'s changelog; README "What's new" at the release bump.
- [ ] T039 Update memory (`project_fourtwenty_lifted_builders.md`) and `claude-misc/four-twenty/PLAN.md`; mark every task [X].
- [ ] T040 PR to `main` and release, on the user's go.

---

## Dependencies

- Phase 1 → Phase 2 → US1 (Phase 3) → US2 (Phase 4): doors build on errands.
- US3 (Phase 5) is independent and small; any time.
- Polish last.

## Parallel

- T001 with T002; T004 with T003.
- T006 alongside T007-T008 (different files).
- T013 with T014.
- T041 and T042 touch the same mixin: one after the other.

## Strategy

- **MVP:** Phases 1-3 (every trip flown). It fixes the stranding on its own.
- **Then** Phase 4 (doors), proven in game.
- **Then** the look polish (Phase 5), proven in game; polish; release on the user's go.
