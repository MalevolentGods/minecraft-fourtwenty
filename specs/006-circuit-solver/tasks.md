# Tasks: Circuit Solver

**Input**: `specs/006-circuit-solver/` (spec, plan, research, data-model, contracts/circuit.md, quickstart)
**Tests**: required by the constitution: pure rules have JUnit tests, and world behaviour has game tests.

`S` = `src/main/java/com/malevolentgods/fourtwenty`, `T` = `src/test/java/com/malevolentgods/fourtwenty`, `G` = `src/gametest/java/com/malevolentgods/fourtwenty/gametest`.

Only the three sources (Hookah, Stash, Hit Box) push today. Every load is a `BIBuffer` that only receives, so roles can mostly be derived from `canReceive`/`canExtract`, with a default demand of the tier's machine draw.

## Phase 1: Setup

- [x] T001 `S/domain/CircuitRules.java`: tier volts, base amps and ohms, pu helpers, every default from research R2–R7, `Numbers` record; `S/processing/ProcessingConfig.java` `circuit` group building it (contract keys)
- [x] T002 [P] `S/domain/Units.java`: SI formatting to three significant figures; `T/domain/UnitsTest.java`

## Phase 2: Foundational (the solver)

- [x] T003 `S/domain/Circuit.java`: nodes, conductors, sources (emf, ohms, maxOut, maxIn), constant-power loads; MNA with gmin; dense Gaussian elimination with partial pivoting; Newton for loads with damping and fixed-point fallback; CV/CC clamp iteration; fair-share brownout bisection; `Solution` (volts, amps, watts, losses, k, converged); conjugate gradients above 400 nodes
- [x] T004 `T/domain/CircuitTest.java`, hand-calculated cases:
  - a divider;
  - two droop sources sharing;
  - a source at its current limit;
  - a battery charging and discharging;
  - two batteries settling;
  - a constant-power load;
  - brownout with eight loads on one source;
  - a ring splitting current;
  - a 100-block line losing 3%;
  - an island with no source;
  - KCL at every node.
- [x] T005 [P] `T/domain/CircuitRulesTest.java`:
  - Puff 100 Ig and 0.042 Φ;
  - a Wick's rating and resistance per tier;
  - stepped up a tier, a run loses half;
  - the overload limit.

## Phase 3: User Story 1, my base keeps working (P1)

- [x] T006 [US1] `S/power/BINode.java`: `role()` (derived default), `demandWatts()` (default: the tier's machine draw while there's room, with hysteresis), `sourceReady()`; `S/power/BIBuffer.java` takes an optional demand
- [x] T007 [US1] `S/power/CircuitBuilder.java`: flood fill from BIGrid's walk; union-find for devices that touch; collapse Wick runs, keeping cumulative resistance per Wick for readings
- [x] T008 [US1] `S/power/Network.java` and `S/power/Circuits.java`:
  - **Per level:** a cache keyed by the BIGrid version, rewalked every 100 ticks.
  - **Each game tick, once:** a signature check, re-solving only when it changed.
  - **Energy:** move it with fractional carry, never creating any.
  - **Readings:** `reading(level, pos, face)`.
- [x] T009 [US1] Devices onto the circuit:
  - **Hookah:** `S/blockentity/WeedBongBlockEntity.java`, a source that lights when `Circuits.wanted`.
  - **Stash:** `S/power/StashBlockEntity.java`, a battery.
  - **Hit Box:** `S/power/HitBoxBlockEntity.java`, an input load and an output source with 2% loss.
  - **Other loads:** Construction and Re-Up intakes declare their charge rate.
- [x] T010 [US1] Remove `S/power/BIPush.java`, `BIGrid.route`/`carried`, and `BIRules.wire`/`wireBurns`/`flows` (keep `drain` and `stored`); update callers and old tests
- [x] T011 [US1] Every existing `G/PowerGameTests.java`, `GrowOpGameTests`, `PhenoGameTests`, `ExtractGameTests` and `GearGameTests` power test is green. New in `G/CircuitGameTests.java`:
  - four machines on a Hookah at full speed;
  - a Stash charging, then discharging;
  - a high player means no draw;
  - an old save loads.

## Phase 4: User Story 2, long lines lose a little (P1)

- [x] T012 [US2] Game tests in `G/CircuitGameTests.java`:
  - a 100-block Puff line loses 2–5%;
  - the same line stepped up to Toke with Hit Boxes loses at most half on its Wicks;
  - a 10-block line has no effect on speed;
  - eight machines on one Hookah slow evenly.

## Phase 5: User Story 4, failures follow the physics (P2)

- [x] T013 [US4] `S/power/Network.java` burn timers. A Wick above its current or insulation rating for `burnTicks` calls `BurnOut.burn`, and the server logs insulation burns with the position. A block above its overload limit calls `overload`.
- [x] T014 [US4] Game tests:
  - burnout by current (two Hookahs, eight machines, one Hemp Wick);
  - burnout by insulation (a Double Hose on a Hemp Wick);
  - a brief spike doesn't burn;
  - a Puff block on a Toke source overloads and recovers;
  - nothing explodes.

## Phase 6: User Story 3, see the electricity (P2)

- [x] T015 [US3] `S/item/InspoweedometerItem.java`, registered in `S/registry/ModItems.java` and the creative tab. It shows a reading on use (action bar), and more on sneak-use (chat). Lang strings, a recipe from the mod's own materials, `tools/generate_inspoweedometer.py` art.
- [ ] T016 [US3] (done for the Stash, Hit Box, plot, machine, Pheno, Rig and Think Tank screens; the Hookah, Terpinator, Construction and Re-Up screens still show charge only) Volts and amps `ContainerData` slots on the BI screens (Stash, Hit Box, plot, Hookah, machines, Pheno, Rig, Think Tank, Terpinator, Construction, Re-Up), shown next to the charge bar in `S/client/`
- [x] T017 [US3] Game test: the meter reading on a known circuit matches the hand calculation to three significant figures

## Phase 7: Polish

- [x] T018 README, CHANGELOG (including the mixed-Wick warning), the help screen text for Wicks, Stashes and the Hit Box in lang; a constitution PATCH note for principle IV
- [x] T019 Performance check: a 1,000-Wick, 100-device network re-solves in under 2 ms (a game test that times it)
- [ ] T020 Full test run; in-game walkthrough on the local server ([quickstart.md](quickstart.md))

## Dependencies

- **Setup and solver first:** T001–T005 come before everything else.
- **Phase 3 depends on Phase 2.** Within it, T006 → T007 → T008 → T009 → T010 → T011.
- **Phases 4 to 6 come after Phase 3.** Phase 6 needs only T008's readings.
- **Polish comes last.**

**MVP:** Phases 1–3, the circuit running a base exactly as before.
