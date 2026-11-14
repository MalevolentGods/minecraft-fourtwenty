# Tasks: Lifted Builders and Higher Ground

**Input**: Design documents from `specs/013-lifted-builders/` (plan.md, spec.md, research.md, data-model.md, contracts/config.md, quickstart.md)

**Prerequisites**: the constitution requires JUnit tests for every `domain/` rule (Principle VI) and in-game verification of every milestone (Principle VII), so test tasks are included.

**Paths**:
- `J` = `src/main/java/com/malevolentgods/fourtwenty`
- `MC` = `J/compat/minecolonies`
- `T` = `src/test/java/com/malevolentgods/fourtwenty/domain`
- `G` = `src/gametest/java/com/malevolentgods/fourtwenty/gametest`

**Mixin rules:** every new MineColonies mixin goes in `src/main/resources/fourtwenty.minecolonies.mixins.json`, and each injection sets `require = 0` (the file's `defaultRequire` is 1). A missed injection logs one WARN line through `BuilderAccess.missing(name)`.

**Builds:**
- **Build:** `./gradlew build --dependency-verification lenient` (JDK 21).
- **Game tests:** `./gradlew runGameTestServer -PcompatRunDir=.compat/runs/beds-dev --no-daemon --dependency-verification lenient`.
- **In-game runs:** follow quickstart.md on flatworld. Never swap jars while a Minecraft client is running.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [X] T001 Create the builder config `MC/builder/BuilderConfig.java`:
  - **Contents:** a `ModConfigSpec` with sections `[diagnostics]`, `[watchdog]` and `[lifted]`, every key, default and range exactly as in contracts/config.md.
  - **Accessors:** typed ones (`logBuilderPathing()`, `watchdogEnabled()`, `slowTicks()`, `slowFraction()`, `stuckTicks()`, `fixStepAside()`, `liftedEnabled()`, `liftedMinutes(String family)`, `potencyBonus()`, `speedLowest()`, `speedHighest()`, `reach()`, `flightRadius()`, `stash()`, `clearFirst()`, `rescueRadius()`, `landingSpeed()`, `requestIntervalTicks()`, `hoverLookahead()`, `hoverGrid()`).
- [X] T002 Register `BuilderConfig.SPEC` as a SERVER config named `fourtwenty-builders-server.toml` in `MC/ColonyIntegration.java#register`, next to the Dispensary config.
- [X] T003 [P] Add the en_us strings to `src/main/resources/assets/fourtwenty/lang/en_us.json`:
  - `com.minecolonies.coremod.setting.fourtwenty:flywhenlifted` = "Fly when Lifted:";
  - its `.tooltip`: "When on, this Builder smokes spare colony joints to become Lifted: she flies and builds the Higher Ground way.";
  - `fourtwenty.lifted.status` = "Lifted";
  - `fourtwenty.lifted.waiting` = "Waiting for spare joints".

## Phase 2: Foundational (blocks every story)

- [X] T004 Create `MC/builder/BuilderAccess.java`, the only class that reaches into MineColonies internals.
  - **Builder lookup:** `isBuilder(ICitizenData)`, `builderAI(EntityCitizen)` (from `getCitizenJobHandler().getColonyJob()` → `getWorkerAI()`, as `EntityAIStructureBuilder`).
  - **States:** `citizenState(EntityCitizen)` (`getCitizenAI().getState()`), `workerState(EntityCitizen)`.
  - **Progress:** `progressKey(EntityCitizen)` (the builder building's `getProgress()` position + stage as a long/record, or null).
  - **Building:** `workOrderLocation`, `buildingCorners(EntityCitizen)` (the work order's structure footprint).
  - **Standing spot:** `clearWorkFrom(EntityCitizen)` and `setWorkFrom(EntityCitizen, BlockPos)`, via a mixin accessor on `EntityAIStructureBuilder.workFrom`, `prevBlockPosition` and `gotoPath`.
  - **Expected rate:** `expectedTicksPerBlock(EntityCitizen)`, with the R3 formula using `getPlaceSpeedLevel()` and the `BLOCK_PLACE_SPEED` research effect.
  - **Recall:** `recall(EntityCitizen)`, MineColonies' own recall (`EntityCitizen.callForHelp`/`TeleportHelper.homeTeleport`, whichever 1.1.1343 has; check against `$TEMP/mcsrc`).
  - **Missed hooks:** `missing(String)` warns once per name.
- [X] T005 Create `MC/mixin/BuilderAIAccessor.java`, an `@Accessor`/`@Invoker` mixin on `com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder` for `workFrom`, `prevBlockPosition` and `gotoPath`. Register it in `fourtwenty.minecolonies.mixins.json` `mixins`.
- [X] T006 [P] Create `MC/builder/BuilderLog.java`, which writes the INFO line formats of contracts/config.md "Log lines" with the logger name `FourTwenty/Builders`.

**Checkpoint**: `./gradlew build` passes; the game starts with and without MineColonies (`.compat/runs/beds-dev`, and a run without the compat mods).

---

## Phase 3: User Story 1, Builders keep their pace (P1) 🎯 MVP

**Goal**: confirm the crawl, then fix it (FR-001, FR-002).

**Independent test**: quickstart.md M1, then M2 step 1.

- [X] T007 [US1] Create `MC/builder/BuilderDiagnostics.java`. When `logBuilderPathing()` is on, it logs (contracts "Diagnostics lines"):
  - **path:** every route search (job type, target, from);
  - **stepaside:** every step-aside;
  - **workfrom:** every standing-spot change, with its reason;
  - **placed:** every placement, with ticks since the last one.

  It keeps a per-citizen last-placement time in a `WeakHashMap<EntityCitizen, Long>`.
- [X] T008 [US1] Create `MC/mixin/BuilderStepAsideMixin.java` on `com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler#prePlacementLogic`.
  - **Where:** inject before the `EntityNavigationUtils.walkAwayFrom` call (`@At(value="INVOKE", target=...walkAwayFrom)`, `require = 0`).
  - **What:** record the step-aside on the worker: a `@Unique` flag set through a small `StepAside` interface implemented by `EntityCitizen` via `MC/mixin/CitizenStepAsideMixin.java`, or a `WeakHashMap` in `BuilderDiagnostics`.
  - **Log:** `stepaside`.
- [X] T009 [US1] Create `MC/mixin/BuilderWalkMixin.java` on `EntityAIStructureBuilder#walkToConstructionSite(BlockPos)`.
  - **At HEAD:** if a step-aside is pending and `fixStepAside()` is on, clear `workFrom`, `prevBlockPosition` and any `gotoPath` through `BuilderAIAccessor`, log `workfrom ... why=stepaside`, and clear the flag.
  - **Diagnostics:** wrap `setPathJob` with `@WrapOperation` to log `path`, and log `workfrom` changes by comparing before/after at RETURN.
  - **Off switch:** with `fixStepAside()` off, it only logs. This is the M1 baseline.
- [X] T010 [US1] Log `placed` from `MC/mixin/BuilderPlacementMixin.java` (US4's T039 extends it with the Lifted speed): inject into `com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure#structureStep` before the `setDelay` call, and log the placement for Builders.
- [X] T011 [US1] Register `BuilderStepAsideMixin`, `BuilderWalkMixin` and `BuilderPlacementMixin` in `src/main/resources/fourtwenty.minecolonies.mixins.json`.
- [X] T012 [US1] **In game (M1):** build, deploy to flatworld, and set `logBuilderPathing=true`, `fixStepAside=false`. Run the Warehouse 2→3 upgrade (and a Tavern upgrade) per quickstart M1.
  - **Record:** the observed log signature and the verdict in `specs/013-lifted-builders/research.md` R1 ("Confirmed in game:" + date).
  - **If the cause differs:** stop and revise T009 before going on.
- [X] T013 [US1] (Superseded 2026-11-13: the crawl couldn't be reproduced on demand, see research R1; the watchdog is the safety net, and SC-001 is checked in M2/M4 runs.) **In game (M2 step 1):** `fixStepAside=true`. Repeat the same upgrades, compare placements per minute against M1 (SC-001), and note the numbers in research.md R1.

**Checkpoint**: the crawl is gone in both upgrades. Commit.

---

## Phase 4: User Story 2, the watchdog (P1)

**Goal**: rescue stuck or slow building Builders and never touch busy ones (FR-003 to FR-008).

**Independent test**: quickstart.md M2 steps 2-4.

- [X] T014 [P] [US2] Write `T/BuilderWatchRulesTest.java`. Cover:
  - OK while progress changes;
  - SLOW when placements over the window fall under `slowFraction` × expected;
  - STUCK after `stuckTicks` with no progress;
  - escalation NONE→FRESH_SPOT→NEAR_BLOCK→OUTSIDE→RECALL, one step per stuck window;
  - reset to NONE on progress;
  - reset when not building.
- [X] T015 [US2] Create `J/domain/BuilderWatchRules.java`, a pure class.
  - **`record Sample(long time, boolean building, Long progressKey, boolean placed)`;**
  - **`enum Verdict {OK, SLOW, STUCK}`** and **`enum Step {NONE, FRESH_SPOT, NEAR_BLOCK, OUTSIDE, RECALL}`;**
  - **`State`:** last progress, last progress time, a ring of placement times, the step;
  - **`next(State, Sample, double expectedPerTick, Config)`** returns the new state and the rescue to apply now (or none).

  Make T014 pass.
- [X] T016 [US2] Create `MC/builder/BuilderWatchdog.java`, registered from `ColonyIntegration` on `ServerTickEvent.Post`. Every 20 ticks, when `watchdogEnabled()`:
  - **Which Builders:** each loaded `EntityCitizen` whose data `BuilderAccess.isBuilder`.
  - **Building:** means `citizenState == CitizenAIState.WORKING` and `workerState` is `AIWorkerState.BUILDING_STEP` or `MINE_BLOCK`.
  - **Rules:** build a `Sample` and run `BuilderWatchRules.next`.
  - **Rescues:**
    - FRESH_SPOT → `BuilderAccess.clearWorkFrom`;
    - NEAR_BLOCK → teleport to the nearest standing spot (solid below, two air above, open sky preferred) within `rescueRadius()` blocks of the current block (`getPosToWorkAt`);
    - OUTSIDE → the nearest standing spot just outside `buildingCorners`;
    - RECALL → `BuilderAccess.recall`.
  - **Log:** each rescue with `BuilderLog` (`rescue <step> ...`).
  - **Memory:** watch state in a `Map<UUID, State>`, cleared for citizens no longer present.
- [X] T017 [P] [US2] Create `J/domain/StandingSpot.java`, a pure search for the nearest standing spot. It takes a `Predicate<BlockPos> solid` and `Predicate<BlockPos> free`, a centre, a radius and an optional exclusion box (footprint). Test it in `T/StandingSpotTest.java`.
- [X] T018 [US2] Fill the Warehouse pockets. Create `C:\Users\Ignus\sensi-heights\tools\fill_warehouse_pockets.py`. For `craftsmanship/storage/warehouse1-4.blueprint`, it sets every air cell under a centre-stair step at (9..11, 1, 12) and (9..11, 1, 20) to the blueprint's solid block used at (9,1,12).
  - **Scope:** only cells that are air with a stair directly above.
  - **Check:** print the before/after and verify with `tools/check_hut_levels.py craftsmanship/storage/warehouse` (no new problems).
  - **Record:** bump `pack.json` to 101 (v100 is the University, shipped in 0.25.0), add a BUILDINGS.md Warehouse note, and commit in sensi-heights.
- [X] T019 [US2] Import the pack into the mod: `python tools/import_sensi_heights.py`, then check `git diff --stat src/main/resources/blueprints/fourtwenty/sensiheights` shows pack v101 and the 4 Warehouse files. (v98-v100 already shipped in 0.24.0 and 0.25.0.)
- [X] T019a [P] [US2] Write `G/BuilderRescueGameTests.java`: build a small maze of real blocks and check `StandingSpot` picks a spot with solid below and two air above, within `rescueRadius()`, and outside a given footprint for the OUTSIDE step.
- [ ] T020 [US2] **In game (M2 steps 2-4):**
  - trap test (SC-002);
  - a full day with mealtimes, night and `NEEDS_ITEM` waits, with no rescues (SC-003);
  - a raid on the colony (`/mc raid` on flatworld), with no rescue of a fleeing Builder (SC-003);
  - the pocket check.

  Record the results in quickstart.md under M2.

**Checkpoint**: P1 complete, and a walking Builder is fixed and protected. Commit. This is shippable on its own if the user wants it early.

---

## Phase 5: User Story 3, Lifted (P2)

**Goal**: a Builder with spare joints lights up, flies, shimmers and smokes, and always lands safely (FR-009 to FR-014, FR-017, FR-018).

**Independent test**: quickstart.md M3.

- [X] T021 [P] [US3] Write `T/LiftedRulesTest.java`:
  - Landrace 5 min = 6000 ticks;
  - each family's minutes from the config map;
  - potency 1 adds 0 and potency 5 adds `potencyBonus`;
  - speed from `speedLowest` at grade 0 to `speedHighest` at Golden God, linear by grade;
  - the delay factor is 1/speed.
- [X] T022 [P] [US3] Write `T/SpareJointsTest.java`: spare = max(0, stock − owed); request size = min(stash, spare); zero spare means no request (and an open one cancels).
- [X] T023 [P] [US3] Write `T/FlightPlannerTest.java`, against a fake grid predicate:
  - a straight path in open air;
  - around a wall;
  - over a roof;
  - none when the goal is sealed;
  - stops at `maxCells`;
  - every cell has two free cells for her height.
- [X] T024 [US3] Create `J/domain/LiftedRules.java` (pure; `ticks(Strain, int potency, Config)`, `speed(Strain, Config)`, `delayFactor`) and `J/domain/SpareJoints.java`. Make T021 and T022 pass. Family and grade come from `Strain`/`StrainStats`.
- [X] T025 [US3] Create `J/domain/FlightPlanner.java`, a bounded 3D A* (26-neighbour, cost √ distance) over `Predicate<BlockPos> fits`, where she fits if two cells are free. It returns a waypoint list or empty. Make T023 pass.
- [X] T026 [US3] Create `MC/builder/Lifted.java`, a `LiftedSavedData` (per level, `computeIfAbsent` on the overworld data storage like `DispensarySavedData`).
  - **Records:** `(colonyId, citizenId) → (Strain, ticksLeft)`, with save/load.
  - **API:** `get`, `start(…)`, `tick(…)` (decrements only when told she's building), `end(…)`.
- [X] T027 [US3] Create `MC/builder/LiftedSetting.java`, holding `SettingKey<BoolSetting> FLY_WHEN_LIFTED = new SettingKey<>(BoolSetting.class, ResourceLocation("fourtwenty","flywhenlifted"))` and `boolean on(IBuilding)`. Create `MC/mixin/BuilderSettingsMixin.java`:
  - **Hook:** inject at RETURN of `com.minecolonies.api.colony.buildings.registry.BuildingEntry#produceBuilding`. When the entry's registry name is `minecolonies:builder`, get its `ISettingsModule` and add `FLY_WHEN_LIFTED` with `new BoolSetting(true)`.
  - **Register** the mixin.
  - **Check in game:** the switch shows in the Settings tab, and in an old colony it reads on.
- [X] T028 [US3] Create `MC/builder/LiftedService.java`.
  - **Asking for joints:** every `requestIntervalTicks()` per Builder with the switch on and `liftedEnabled()`, compute stock (joints in the colony's Dispensaries' and Warehouses' item handlers) and owed (count of citizens with `JointService.due`).
    - **Spare:** open a `Stack(fourtwenty:joint, min 1, count)` request via `citizenData.createRequestAsync` when the spare is positive and she has fewer than `stash()` joints.
    - **No spare:** cancel an open joint request; she shows `fourtwenty.lifted.waiting` as her status.
  - **Lighting up** (`tryLightUp(EntityCitizen)`):
    - take one joint from her inventory, or else from her hut's storage. Joints a player drops in the Builder's hut skip the citizens-first rule (FR-010);
    - `Lifted.start` with `LiftedRules.ticks` of its strain;
    - count it as a citizen dose with `JointService.consume`/the same `DispensarySavedData` call a Dispensary visit makes (FR-018);
    - smoke particles and sound;
    - log `lifted`.
  - **When it's called:** when the watchdog tick sees her enter building, and when her Lifted ends while she's still building.
  - **Ending:** `end(EntityCitizen, reason)` → landing (T029) + log `landed`.
- [X] T029 [US3] Create `MC/builder/LiftedFlight.java`, with its own `ServerTickEvent.Post` handler that runs every tick for Lifted Builders only. The watchdog keeps its 20-tick cadence.
  - **Flying (building, Lifted, the hut switch on and `liftedEnabled()`):** `setNoGravity(true)`, stop the navigator, follow `FlightPlanner` waypoints to the current target at walking speed, set `fallDistance = 0`.
  - **Otherwise, LANDING:** descend at ≤ `landingSpeed()` blocks/tick to the target from `StandingSpot` (outside the footprint, or straight down when already outside), then restore gravity.
  - **Chunk unload:** an `EntityLeaveLevelEvent` listener lands her instantly at the first standing spot below.
  - **Load:** an `EntityJoinLevelEvent` listener moves a Lifted Builder whose saved position is in the air to the first standing spot below.
- [X] T030 [US3] Create `MC/builder/LiftedPayload.java` (`fourtwenty:lifted`, entity VarInt + on bool). Register it in the payload registrar used by `GoldenGod` (play to client).
  - **Sending:** to players tracking her, only where the connection `hasChannel`, on start, end, `StartTracking` and login.
- [X] T031 [P] [US3] Create `MC/client/LiftedClient.java`.
  - **Glow:** in `EntityRenderersEvent.AddLayers`, get the renderer for MineColonies' citizen entity type and add a `LiftedGlowLayer`. It's a copy of `GoldenGodClient.Shine`'s shells only: colours `{0x40AAFF60, 0x28AAFF60, 0x149CFF50}`, alpha × (0.8 + 0.2·sin(time/10)), and no body recolour.
  - **Smoke:** a client tick spawns 1 campfire smoke particle every 6 ticks at her feet for each Lifted citizen in view.
  - **Lifted set:** kept from `LiftedPayload`, cleared on level change.
- [X] T032 [P] [US3] Create `G/LiftedFlightGameTests.java`:
  - the planner finds a path over a built wall and none into a sealed box;
  - the `StandingSpot` landing target is outside a given footprint;
  - an entity flagged landing from 10 blocks takes no damage (use an armour stand or zombie with the same landing code path factored into `LiftedFlight.land(Mob, BlockPos)`);
  - `LiftedSavedData` round trip: save, reload, and the strain and ticks left survive, keyed by colony and citizen (as `DemandPersistenceGameTests` does for the Dispensary).
- [ ] T033 [US3] **In game (M3):** quickstart M3 steps 1-4, all five landing cases (SC-005), and a second client seeing the shimmer. Record the results in quickstart.md.

**Checkpoint**: Builders fly on spare joints and always land safely. Commit.

---

## Phase 6: User Story 4, Higher Ground (P2)

**Goal**: Lifted Builders build faster with reach, hover points, and clear-first upgrades (FR-015, FR-016).

**Independent test**: quickstart.md M4.

- [ ] T034 [P] [US4] Write `T/HoverCoverTest.java`:
  - picks the candidate covering most of the next N blocks within reach;
  - ties go to the higher point;
  - never a candidate whose own cell or the cell above holds one of the next blocks (she would be built in);
  - an empty candidate list gives none.
- [ ] T035 [US4] Create `J/domain/HoverCover.java`: `choose(List<BlockPos> candidates, List<BlockPos> nextBlocks, int reach)`. Reject candidates whose cell or the cell above is among `nextBlocks`. Make T034 pass.
- [ ] T036 [US4] Extend `MC/mixin/BuilderWalkMixin.java`. When the Builder is Lifted and flying is on, cancel at HEAD and return our own result:
  - **Choosing a spot:** if she has no hover point or the current block is beyond `reach()`, choose a new hover point with `HoverCover`. The candidates are free cells within `reach()` of the current block, scanned on a `hoverGrid()`-block grid around and above it. The next blocks are the following up to `hoverLookahead()` positions from the structure iterator (read them through `BuilderAccess.peekNext(EntityCitizen, hoverLookahead())`, iterating a copy of the placer iterator).
  - **Never built in:** before each placement, if the block goes in her own cell or the cell above, choose a new hover point first.
  - **Getting there:** fly there with `LiftedFlight`.
  - **Return value:** `true` once she is within `reach()` of the current block.
- [ ] T037 [US4] Create `MC/mixin/HigherGroundStageMixin.java` on `AbstractEntityAIStructure#loadStructure`.
  - **When:** the building's level > 0, the worker is a Builder, she's Lifted and `clearFirst()` is on.
  - **What:** prepend `BuildingProgressStage.CLEAR` to the upgrade stage array passed to `BuildingStructureHandler` (`@ModifyArg` on its constructor or the stages array).
  - **Register** the mixin.
- [ ] T038 [US4] Create `MC/mixin/HigherGroundStorageMixin.java`.
  - **Where:** before Structurize/MineColonies breaks a block during CLEAR (target `AbstractEntityAIStructure` BLOCK_REMOVAL / `mineBlock` path; find the exact call in `$TEMP/mcsrc/AbstractEntityAIStructure.java`).
  - **What:** if the block entity has an item handler capability with items, move them into the Builder's hut (`building.getItemHandlerCap()`); whatever doesn't fit drops at the hut position. The block then breaks empty.
  - **Register** the mixin.
  - **Testable core:** factor the transfer into `MC/builder/StorageTransfer.move(IItemHandler from, IItemHandler to, Level, BlockPos dropAt)`.
- [ ] T038a [P] [US4] Write `G/StorageTransferGameTests.java`: a chest with items is emptied into a second container; the overflow drops at the given position and nothing drops at the source.
- [ ] T039 [US4] Extend `MC/mixin/BuilderPlacementMixin.java`: when the worker is Lifted, multiply the `setDelay` argument by `LiftedRules.delayFactor(strain)` (`@ModifyArg`).
- [ ] T040 [US4] **In game (M4):** quickstart M4:
  - the Lifted Warehouse 2→3 clears top-down, then builds;
  - time it against T013's walking run (SC-004);
  - the chest-contents test;
  - a Lifted Warehouse upgrade keeps its keep-terrain courier zones, the nested Courier's Huts and the hut block untouched (FR-016).

  Record the results.

**Checkpoint**: Higher Ground works. Commit.

---

## Phase 7: User Story 5, the cost (P3)

**Goal**: Lifted costs weed, balanced (FR-019, FR-020).

**Independent test**: quickstart.md M5 model run.

- [ ] T041 [US5] Extend `tools/progression_model.py`.
  - **Model:** add a `LIFTED` table mirroring the config defaults, and `builder_joints_per_day(family) = 18 / lifted_minutes(family)` (18 building minutes of a 20-minute day, so 3.6 Landrace joints).
  - **Colony table:** add two busy Builders' joints to "asked" and print a "Lifted Builders" column. Keep `--today` unchanged.
- [ ] T042 [US5] Run `python tools/progression_model.py`, and check every colony stage still shows a positive spare with two Lifted Builders (SC-006). If not, raise the Lifted minutes in `BuilderConfig` defaults and contracts/config.md together, and re-run.

---

## Phase 8: Polish & Cross-Cutting

- [ ] T043 [P] README.md "What's new": Lifted Builders, Higher Ground and the Builder fixes, for players, with the hut switch and the config file named.
- [ ] T044 [P] CHANGELOG.md: an Unreleased entry for spec 013, also noting Sensi Heights pack v101 (the Warehouse stair pockets).
- [ ] T045 Without MineColonies, run the client and server with no compat mods, and check Four Twenty loads and plays (SC-007). `./gradlew build` and the game tests pass (the known-flaky `thecalculatorcalculates` only with no client running).
- [ ] T045a On flatworld with 10 Builders (5 Lifted), measure the server tick cost of `BuilderWatchdog`, `LiftedFlight` and `LiftedService` with a timing log over 5 minutes. It must stay under 0.5 ms per tick on average (plan Performance Goals).
- [ ] T046 Update memory (`project_fourtwenty_*`) with what shipped and what was seen in game. Mark every task [X] in this file.
- [ ] T047 PR from `013-lifted-builders` to `main`. Merge, release and server updates wait for the user's go.

---

## Dependencies & Execution Order

- **Phase 1 → Phase 2:** T004 and T005 block every story.
- **US1 (Phase 3):** blocks US2 (the watchdog's FRESH_SPOT reuses the step-aside fix and its accessor).
- **US3 (Phase 5):** needs Phase 2 only. It can start after the US1 M1 gate (T012), but its in-game tests are cleaner after US2.
- **US4 (Phase 6):** needs US3 (it only applies while Lifted).
- **US5 (Phase 7):** needs the US3 config numbers only. It can run any time after T024.
- **Polish:** last.

```text
Setup → Foundational → US1 (gate T012) → US2 → US3 → US4 → US5 → Polish
                                      └→ US5 (model) any time after T024
```

## Parallel Opportunities

- **Phase 1:** T003 alongside T001-T002.
- **Phase 2:** T006 alongside T004-T005.
- **US2:** T014, T017 and T019a together; then T018-T019 (sensi-heights) alongside T015-T016.
- **US3:** T021, T022 and T023 (tests) together; T031 (client) alongside T028-T029; T032 alongside T030.
- **US4:** T034 alongside T036-T038 setup reading; T038a after T038.
- **Polish:** T043 and T044 together.

## Implementation Strategy

- **MVP:** US1 + US2. That fixes walking Builders for every colony and can ship alone.
- **Then:** US3 (the fun), US4 (what makes it worth the weed), then US5 tunes it.
- **Milestones:** each is a commit, and nothing goes to Hera without the user's go.
