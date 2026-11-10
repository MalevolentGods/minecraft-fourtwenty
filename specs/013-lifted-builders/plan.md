# Implementation Plan: Lifted Builders and Higher Ground

**Branch**: `013-lifted-builders` | **Date**: 2026-11-09 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/013-lifted-builders/spec.md`

## Summary

**Part 1: fix walking Builders.**
- **Diagnostics:** switchable logging of the Builder's route searches, step-asides and standing-spot changes, to confirm the suspected loop on flatworld.
- **The fix:** a narrow mixin. When she steps aside from her own column, her old standing spot is forgotten, so she picks a new one out of the way.
- **The watchdog:** a server-side tick that reads MineColonies' own AI states. It only judges a Builder whose citizen state is `WORKING` and whose builder AI state is `BUILDING_STEP` or `MINE_BLOCK`. It measures progress by the build's own progress position, and escalates slow → stuck → outside → recall.
- **Warehouse pockets:** filled in the Sensi Heights generator, and the pack is re-imported.

**Part 2: Lifted and Higher Ground.**
- **Hut switch:** a "fly when Lifted" setting injected into the Builder's existing settings tab.
- **Joints:** asked for through MineColonies' normal requests, opened only for the colony's spare.
- **Lifted:**
  - a Builder-only state saved per citizen;
  - flight by our own short-range planner (no gravity, steered motion, gentle landing);
  - a green see-through glow layer on MineColonies' citizen renderer;
  - a smoke trail.
- **Higher Ground**, as mixins into MineColonies' structure AI:
  - reach 8-10 blocks;
  - greedy hover points;
  - a clear-first stage on upgrades;
  - a shorter placing delay.
- **Storage contents** that the clear removes go to her hut, so the hut's normal pickup takes them to the Warehouse.
- **Balance:** the model gains Lifted demand.

## Technical Context

**Language/Version**: Java 21 (Minecraft 1.21.1, NeoForge 21.1.218); Python 3 for tools.
**Primary Dependencies**:
- MineColonies 1.1.1343 and Structurize 1.0.831, compile-only. They are optional, under `compat/minecolonies`, loaded via `ColonyIntegration`.
- Mixins on MineColonies classes go in the existing `fourtwenty.minecolonies.mixins.json`.

**Storage**:
- **Lifted state:** a `SavedData` per level, keyed by colony and citizen.
- **The hut switch:** a MineColonies `BoolSetting` saved with the building.
- **Watchdog state:** in memory only.

**Testing**:
- **Unit tests:** JUnit for the pure rules in `domain/` (watch verdicts, Lifted time and speed, flight planning, hover cover, spare joints).
- **Game tests** in `src/gametest` for what runs without a colony (flight planner against real blocks, landing).
- **In game:** every milestone is tested on the flatworld test server (Principle VII).

**Target Platform**: NeoForge dedicated server and client (same jar).
**Project Type**: Minecraft mod (MineColonies add-on part).
**Performance Goals**:
- The watchdog checks each Builder once a second.
- Flight planning is bounded to a 24-block radius and at most 4,000 cells per search.
- With 10 Builders, the extra server cost per tick is under 0.5 ms.

**Constraints**:
- **MineColonies stays optional:** no MineColonies class may load without it (Principle I).
- **Pinned versions:** the mixins target method names in MineColonies 1.1.1343. Each mixin is `require = 0` with a logged warning, so a MineColonies update can't crash the game, only switch the part off.

**Scale/Scope**:
- Colonies of up to about 100 citizens and a handful of Builders.
- Builders only; other workers are out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How |
|---|---|---|
| I. MineColonies Stays Optional | ✅ | All code is under `compat/minecolonies` (new `builder/` package) and the MineColonies mixin config. Without MineColonies nothing loads, and SC-007 checks it. |
| II. Existing Machines Keep Their Game | ✅ | No machine is touched. |
| III. The Power Rule | ✅ | Nothing is powered. |
| IV. Failure Is Safe | ✅ | Flying never kills: any end of Lifted lands her gently (FR-014). The principle's flight clause is about the Puff Plate wearer; this extends the same rule to citizens. Nothing explodes. |
| V. Approved Names | ✅ | "Lifted" and "Higher Ground" were approved 2026-11-09. The hut switch's label "Fly when Lifted" uses the approved name. |
| VI. Rules Are Pure And Tested | ✅ | `domain/` pure classes with JUnit tests: `BuilderWatchRules`, `LiftedRules`, `FlightPlanner`, `HoverCover`, `SpareJoints`. Every number is in the config (contracts/config.md). |
| VII. Milestones Prove Themselves In Game | ✅ | Five milestones, each tested on flatworld and committed before the next (see Milestones). |
| Constraints: same jar, payloads guarded | ✅ | One new payload (`fourtwenty:lifted`), sent only to connections with the channel, as `GoldenGod.Shine` is. No registry changes. |

No violations.

## Project Structure

### Documentation (this feature)

```text
specs/013-lifted-builders/
├── plan.md              # This file
├── research.md          # Phase 0: decisions from MineColonies/Structurize source
├── data-model.md        # Phase 1: state, settings, saved data
├── quickstart.md        # Phase 1: in-game test runs on flatworld
├── contracts/
│   └── config.md        # Phase 1: the new server config and the hut setting
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/java/com/malevolentgods/fourtwenty/
├── domain/
│   ├── BuilderWatchRules.java      # verdict from progress samples: OK / SLOW / STUCK, escalation steps
│   ├── LiftedRules.java            # Lifted ticks and placing speed by family + potency
│   ├── SpareJoints.java            # joints a Builder may ask for: stock - owed today
│   ├── FlightPlanner.java          # bounded 3D A* over a passability predicate (air cells, 2 high)
│   └── HoverCover.java             # greedy hover point covering the next blocks within reach
├── compat/minecolonies/
│   ├── builder/
│   │   ├── BuilderConfig.java      # fourtwenty-builders-server.toml
│   │   ├── BuilderDiagnostics.java # FR-001 logging (off by default)
│   │   ├── BuilderWatchdog.java    # server tick, reads AI states, applies rescues, logs them
│   │   ├── BuilderAccess.java      # one place that reaches into MineColonies (AI state, progress, workFrom)
│   │   ├── Lifted.java             # LiftedSavedData + queries (isLifted, strain, ticks left)
│   │   ├── LiftedService.java      # requests joints, lights up, counts the dose, ends Lifted
│   │   ├── LiftedFlight.java       # per-tick flight, smoke, gentle landing
│   │   ├── LiftedSetting.java      # injects "fly when Lifted" into the Builder's settings
│   │   └── LiftedPayload.java      # fourtwenty:lifted (entity id, on/off, strain colour)
│   ├── client/
│   │   └── LiftedClient.java       # green see-through glow layer on RenderBipedCitizen, smoke particles
│   └── mixin/
│       ├── BuilderWalkMixin.java        # EntityAIStructureBuilder.walkToConstructionSite: forget workFrom after a step-aside; Higher Ground reach/fly
│       ├── BuilderStepAsideMixin.java   # BuildingStructureHandler.prePlacementLogic: mark the step-aside
│       ├── HigherGroundStageMixin.java  # AbstractEntityAIStructure.loadStructure: CLEAR first on upgrades when Lifted
│       ├── HigherGroundDelayMixin.java  # AbstractEntityAIStructure.structureStep: shorter delay when Lifted
│       ├── HigherGroundStorageMixin.java# contents of storage the CLEAR removes → her hut
│       └── BuilderSettingsMixin.java    # BuildingEntry.produceBuilding: add the hut setting to Builders
src/test/java/com/malevolentgods/fourtwenty/domain/
│   ├── BuilderWatchRulesTest.java, LiftedRulesTest.java, SpareJointsTest.java,
│   └── FlightPlannerTest.java, HoverCoverTest.java
src/gametest/java/.../LiftedFlightGameTests.java   # planner on real blocks, landing safety
src/main/resources/fourtwenty.minecolonies.mixins.json   # the six mixins added
src/main/resources/assets/fourtwenty/lang/en_us.json     # setting label, log/status strings
tools/progression_model.py                               # Lifted demand in the colony table
tools/import_sensi_heights.py                            # re-import the pack (pockets filled)

C:\Users\Ignus\sensi-heights (separate repo)
├── tools/fill_warehouse_pockets.py  # one-off: solid fill under the top centre steps, Warehouse 1-4
└── craftsmanship/storage/warehouse1-4.blueprint, pack.json (v100)
```

**Structure Decision**:
- **Placement:** new code lives in one `compat/minecolonies/builder/` package, beside the Dispensary code it borrows from (`JointService` dosing, `DispensarySavedData` demand).
- **Pure rules** go in `domain/`, so they test without Minecraft.
- **Mixins** are kept thin. They call `BuilderAccess` or `LiftedService`, so MineColonies' internals are touched in one place.

## Milestones

Each is tested in game on flatworld and committed before the next starts (Principle VII).

1. **M1, diagnostics:** `BuilderConfig`, `BuilderDiagnostics`, and the two walk mixins in logging-only mode. Run the Warehouse 2→3 and Tavern upgrades on flatworld and read the log. **Gate:** the loop is confirmed, or the real cause is found and the fix in M2 changes to match.
2. **M2, walking fix and watchdog:** the `workFrom` fix, `BuilderWatchRules` and `BuilderWatchdog`, and the Warehouse pockets (sensi-heights pack v100, imported). Test SC-001, SC-002 and SC-003 on flatworld.
3. **M3, Lifted:** the setting, `SpareJoints`, joint requests, lighting up, `LiftedSavedData`, `FlightPlanner`, `LiftedFlight`, the landing cases, the shimmer and the smoke. Test US3 and SC-005.
4. **M4, Higher Ground:** reach and hover points (`HoverCover`), clear-first on upgrades, storage contents to the hut, the speed boost. Test US4 and SC-004.
5. **M5, balance and docs:** the model's Lifted demand (SC-006), config defaults, README, CHANGELOG, and the in-game walkthrough. A release only on the user's go.

## Complexity Tracking

No constitution violations. Two risks are worth recording:

| Risk | Mitigation |
|---|---|
| Mixins into MineColonies' private AI methods break on a MineColonies update. | Every injection is `require = 0` and logged when it misses. `BuilderAccess` keeps all reflection and accessors in one class. The jar's compatibility lock pins 1.1.1343. |
| Flight lets a citizen end up somewhere unreachable (sealed in, high up). | A landing targets the nearest open standing spot outside the building's footprint, or straight down if she is already outside it, never a spot inside the walls (research R7). The watchdog also covers Lifted Builders, and recall to the hut is the last resort. |
