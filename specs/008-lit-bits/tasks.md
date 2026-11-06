# Tasks: Lit Bits and the Weedweeno

`S` = `src/main/java/com/malevolentgods/fourtwenty`, `T` = `src/test/java/com/malevolentgods/fourtwenty`, `G` = `src/gametest/java/com/malevolentgods/fourtwenty/gametest`.

## Phase 1: Setup

- [ ] T001 Bundle Cobalt 0.9.9: the squiddev Maven repository and jarJar in `build.gradle`, with the dependency locks and `gradle/verification-metadata.xml` updated. Check that the built jar carries it and that a dev run loads it.

## Phase 2: Foundational (pure)

- [ ] T002 [P] `S/domain/LedRules.java`: the LED current law, brightness, light level and HSV conversion; `T/domain/LedRulesTest.java`
- [ ] T003 [P] `S/domain/Circuit.java`: an LED element (current-controlled, with a knee, reversed takes nothing); cases in `T/domain/CircuitTest.java`
- [ ] T004 [P] `S/domain/LitBitIdRules.java` and `S/domain/Programs.java` (limits, names), with tests
- [ ] T005 `S/lights/LuaShow.java`:
  - **Sandbox:** Cobalt with the standard globals only.
  - **The `bits` library:** `set`, `hsv`, `off`, `all`, `ids`, `count`, `frame`, `sleep`, `time`, `tick`, `rgb`, and `print` to the console.
  - **Running:** a coroutine per run, yielding at `frame()` and `sleep()`.
  - **Limits:** a watchdog interrupts at the tick budget; string guards; size sampling.
  - **Tests:** `T/lights/LuaShowTest.java` covers no `io`, `os`, `debug` or `dofile`; an infinite loop stopped; an error and its line; `frame` pacing; `sleep`; the string guard.

## Phase 3: User Story 2, LEDs (P1)

- [ ] T006 [US2] `S/lights/LitBitBlock.java` and `LitBitBlockEntity.java` (ID, colour, level), the LED role in `S/power/BINode.java`, and the LED element in `S/power/Network.java`; registered in `S/lights/ModLights.java`, with capabilities and terminals
- [ ] T007 [US2] `S/client/LitBitRenderer.java`, the `LampColours` payload in `S/lights/LightsNetwork.java`, and a light level that changes at most every 10 ticks
- [ ] T008 [US2] Game tests in `G/LitBitGameTests.java`:
  - current against colour (full white is three times full red);
  - black takes only its idle current;
  - dim on a sagging line;
  - reversed stays dark;
  - a Toke line overloads it.

## Phase 4: User Story 4, IDs (P2, needed by US1)

- [ ] T009 [US4] `S/lights/LitBitIds.java` (SavedData plus the live index); placing at a remembered position gets its ID back, and by hand gets the lowest free ID nearby
- [ ] T010 [US4] `S/lights/StickyFingerItem.java` (`next_id` component, places from the inventory and counts up); `S/client/StickyFingerScreen.java` and `LitBitScreen.java`, with the `SetFingerId` and `SetLitBitId` payloads
- [ ] T011 [US4] Game tests:
  - the glove counts 100, 101, 102;
  - a re-placed lamp gets its ID back;
  - placing by hand takes the lowest free ID;
  - a renamed ID is answered.

## Phase 5: User Stories 1 and 3, shows (P1)

- [ ] T012 [US1] `S/lights/WeedweenoBlock.java`, `WeedweenoBlockEntity.java` (programs, running, status, console, its power node, resuming after a restart, programs kept on the item) and `WeedweenoMenu.java`
- [ ] T013 [US1] Presets in `data/fourtwenty/weedweeno/presets/` (rainbow, chase, pulse, strobe); new Weedweenos start with them; a Restore Presets action
- [ ] T014 [US3] `S/client/WeedweenoScreen.java`:
  - **Editor:** the program list, the code with line numbers, the error line marked, the console.
  - **Buttons:** New, Duplicate, Rename, Delete, Run, Stop, Export, Import.
  - **Payloads:** `WeedweenoEdit` and `WeedweenoState`.
- [ ] T015 [US1] Game tests:
  - a program sets lamps by ID within range;
  - lamps beyond range are left alone;
  - stop turns them off;
  - a runaway program is stopped and the server goes on;
  - restarting resumes the show.

## Phase 6: User Story 5, Bit Chits (P2)

- [ ] T016 [US5] `S/lights/BitChitItem.java` (`program` component, tooltip); export and import in the Weedweeno; `BitChitCopyRecipe.java`
- [ ] T017 [US5] Game test: export, copy and import round trip

## Phase 7: Polish

- [ ] T018 [P] `tools/generate_lit_bits.py`: art for the lamp shell, glove, Weedweeno and Bit Chit; recipes; lang; help text
- [ ] T019 Config values (range, budget, global cap, lamps per tick, LED numbers, operators only); README and CHANGELOG
- [ ] T020 Timing: 1,024 lamps at 20 frames a second; full test run
- [ ] T021 Flatworld: build a demo strip, then the quickstart in game

## Dependencies

- **Order:** T001 → T005, and T002/T003 → T006. IDs (Phase 4) come before shows (Phase 5). Polish comes last.
- **MVP:** Phases 1–5. A wall of lamps runs a preset show.
