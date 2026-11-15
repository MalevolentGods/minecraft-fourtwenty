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

- [ ] T013 [P] [US2] Write `T/FlightPlannerDoorsTest.java`: a route through a closed wooden door or gate cell exists at a small extra cost and marks the doorway cells; iron doors and trapdoors are walls; a route avoids a door when an open way is as short.
- [ ] T014 [P] [US2] Write `CT/DoorwaysGameTests.java`: a Lifted Builder flying a route through a closed door opens it as she reaches it and closes it once 2 blocks past; a door someone else opened is left alone; a fence gate the same.

### Implementation

- [ ] T015 [US2] Teach `J/domain/FlightPlanner.java` doorways: a `Free` that can say "doorway" (a passable door or gate cell, extra cost), route cells flagged as doorways (research R2). Make T013 pass.
- [ ] T016 [US2] Create `MC/builder/Doorways.java`: which blocks count (wooden doors and fence gates only, FR-004), open as she comes within 1.5 blocks of a doorway on her route, close what she opened once she's 2 blocks past and it's empty, upright and at door height through the doorway. Make T014 pass.
- [ ] T017 [US2] Use doorways in `MC/builder/LiftedFlight.java`: the planner's free test includes doorways; `fly()` calls `Doorways` for doorway cells ahead; the build-site `approach` uses them too, so blocks inside closed buildings are reached by flying in (no more walk-in with `bodyFlight` on).
- [ ] T018 [US2] Unreachable blocks (FR-006) in `MC/builder/LiftedFlight.java`: when no route reaches a block, place it from outside if it's in reach, otherwise let MineColonies move on to the next block; never stuck for good.
- [ ] T019 [US2] **In game (M1 step 4):** an inside repair of the Warehouse with its doors shut; record which doors were used and that all ended closed.

**Checkpoint**: Stories 1 and 2 together: SC-001, SC-002, SC-003.

---

## Phase 5: The design pass (user, blocks US3's art)

- [ ] T020 [US3] Create `tools/render_lifted_concepts.py` (Blender, headless, as `tools/render_midnight_machine_concepts.py`): three stylised silhouettes (for example lanky and loose, chunky and cute, sleek and tall) with the joints from FR-010, front, back and side, in the cruise and hover poses, wearing the boombox. Render to `%TEMP%` and show the user.
- [ ] T021 [US3] **User picks a silhouette.** Record the choice and its proportions (part sizes) in research.md R8.
- [ ] T022 [US3] Write the texture-sheet prompt for the picked model (each part's faces as panels on magenta, flat colour zones for hair, skin and two clothing colours, per contracts/config-and-ui.md), and give it to the user for one generation.
- [ ] T023 [US3] Create `tools/import_lifted_sheet.py`: cut the sheet's panels, build the base texture and the four tint masks at sizes in multiples of 16, into `src/main/resources/assets/fourtwenty/textures/entity/lifted/`. Save the sheet as `tools/art/lifted_sheet.png`.

---

## Phase 6: User Story 3 - Her looks are ours while she's Lifted (Priority: P2)

**Goal**: the Lifted model, tinted per Builder, with cruise, hover, smoking and dancing poses, the boombox, glow and props; MineColonies still draws her name and status; a player toggle; a safe fallback.

**Independent Test**: quickstart M3.

### Tests

- [ ] T024 [P] [US3] Write `T/SkinPaletteTest.java`: hair, skin and two clothing colours come out as each region's dominant colour of a 64x64 skin; transparent and near-black pixels are ignored; a blank skin gives safe defaults.
- [ ] T025 [P] [US3] Write `T/LiftedPoseTest.java`: each named pose's joints stay within limits; blending eases over 0.3-0.5 s with no jump; cruise nods on the beat; the dance bends elbows and knees.

### Implementation

- [ ] T026 [P] [US3] Create `J/domain/SkinPalette.java` (pure): regions of the standard citizen skin layout → hair, skin, cloth 1, cloth 2. Make T024 pass.
- [ ] T027 [P] [US3] Create `J/domain/LiftedPose.java` (pure): named poses (cruise, hover, smoke, dance) as joint angles for the model's joints, blended by weights easing over time; the dance from `DanceRules`/`DanceMoves`, extended with elbow, knee and spine bends. Make T025 pass.
- [ ] T028 [US3] Create `MC/client/LiftedModel.java`: the picked model as a `ModelPart` tree (torso upper/lower, neck, head, upper/lower arms, hands, upper/lower legs, feet) with a `LayerDefinition`, registered in `J/client/ModClientEvents.java` (`EntityRenderersEvent.RegisterLayerDefinitions`).
- [ ] T029 [US3] Create `MC/client/LiftedSkins.java`: per distinct MineColonies skin, read it, `SkinPalette` it, compose base × tinted masks into a `DynamicTexture`, cache by skin, release unused ones.
- [ ] T030 [US3] Create `MC/client/LiftedRenderer.java` on `RenderLivingEvent.Post` for Lifted Builders with the look on (server switch from the Lifted payload, player toggle): draw the model in her tinted texture with `LiftedPose` (cruise when flying, hover when placing, smoke on a smoke break, dance when playing), the joint as a prop in her hand while smoking, and the green glow; any exception or missing texture falls back for that Builder to 0.26.0's look (FR-012).
- [ ] T031 [US3] Create `MC/mixin/CitizenModelHideMixin.java` (client): while `LiftedRenderer` draws her, hide MineColonies' body parts and held-item layer so only our model shows; her name and status bubble still draw. Register it in the client list of `fourtwenty.minecolonies.mixins.json`.
- [ ] T032 [US3] Move the boombox, VU bars and smoke onto the new body in `MC/client/BoomboxLayer.java` and `MC/client/LiftedClient.java` (attach to the model's upper torso), keeping them on MineColonies' model for the fallback.
- [ ] T033 [US3] The player toggle **"Lifted look"** *(name pending the user's approval)* in `MC/client/MidnightMachineSettings.java`, beside the volume and mute, shown only when the server has the look on; lang keys in `src/main/resources/assets/fourtwenty/lang/en_us.json`.
- [ ] T034 [US3] Send the server's `liftedLook` switch to clients with the Lifted payload (`MC/builder/LiftedPayload.java`), so no client draws the look when the server has it off.
- [ ] T035 [US3] **In game (M3):** quickstart M3 steps 1-5; the user judges the look (SC-005); build timing with the look on and off (SC-004).

**Checkpoint**: Story 3 done.

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
- The design pass (Phase 5) can run alongside Phases 3-4 (it needs the user); US3 (Phase 6) needs Phase 5's art for T029-T030, but T024-T028 can start any time.
- Polish last.

## Parallel

- T001 with T002; T004 with T003.
- T006 alongside T007-T008 (different files).
- T013 with T014.
- T024, T025, T026, T027 together; T020-T023 alongside Phases 3-4.

## Strategy

- **MVP:** Phases 1-3 (every trip flown). It fixes the stranding on its own.
- **Then** Phase 4 (doors), proven in game.
- **Meanwhile** the design pass with the user.
- **Then** the look (Phase 6), proven in game; polish; release on the user's go.
