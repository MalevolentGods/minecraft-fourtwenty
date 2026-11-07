# Tasks: Polarity Circuits

`S` = `src/main/java/com/malevolentgods/fourtwenty`, `T` = `src/test/java/com/malevolentgods/fourtwenty`, `G` = `src/gametest/java/com/malevolentgods/fourtwenty/gametest`.

## Phase 1: Foundational (pure)

- [ ] T001 `S/domain/Circuit.java`: sources, batteries and loads between a + node and a − node; reversed loads take nothing and report `REVERSED`; per-load status (`OK`, `NO_LOOP`, `REVERSED`, `BROWNOUT`); breakpoint stepping on the voltage across each source
- [ ] T002 `T/domain/CircuitTest.java`: existing cases rewritten as loops, plus:
  - parallel, where three loads share a voltage;
  - series, where three loads share a current and their voltages add up to the source's;
  - a mix of the two;
  - a reversed load;
  - an open loop (`NO_LOOP`);
  - two sources in series;
  - a short.
- [ ] T003 [P] `S/domain/Terminals.java` (front → + and − faces; the Hit Box's high winding) and `T/domain/TerminalsTest.java`

## Phase 2: User Story 1, a closed loop (P1)

- [ ] T004 [US1] `S/power/BINode.java` `Terminal(node, sign)`; capabilities per side in `S/registry/ModPower.java` and `S/gear/ModGear.java`, null for faces that aren't terminals; `front(state)` for each block (the Hookahs face the other way)
- [ ] T005 [US1] `S/power/Network.java`: terminal keys, + and − nodes per member, terminals touching directly join, a status per member
- [ ] T006 [US1] `S/power/Circuits.java` readings: across terminals, with status
- [ ] T007 [US1] `G/PowerGameTests.java`, `CircuitGameTests.java`, `GearGameTests.java` and the others rebuilt with return lines (a `loop()` helper); `G/PolarityGameTests.java`: no return path → no current, a closed loop runs, and breaking a line stops it

## Phase 3: User Story 2, series and parallel (P1)

- [ ] T008 [US2] `S/power/WickBlock.java`: a `colour` property, Wicks join the same colour only, `PLUG` to terminals, dye on use, the item's colour component, the dye recipe, and the block-colour tint in `S/client/`
- [ ] T009 [US2] Game tests: parallel and series readings against a hand solution; red and black lines side by side; a same-colour short

## Phase 4: User Story 3, the Hit Box (P2)

- [ ] T010 [US3] `S/power/HitBoxBlockEntity.java`: two windings (low: left and right; high: front and back), isolated, coupled through its banks with a 2% loss
- [ ] T011 [US3] Game test: a Toke loop in, a separate Puff loop out, the station runs, and the loops are isolated

## Phase 5: User Story 4, finding faults (P2)

- [ ] T012 [US4] `S/power/InspoweedometerItem.java`: a `meter_reference` component; sneak-use a Wick to set it, sneak-use the air to clear it; readings across terminals, "no return path", "wired backwards", Wick against the reference
- [ ] T013 [US4] Screen meter line with status; machines say "wired backwards" or "no return path" on their screens
- [ ] T014 [US4] Game tests: a 0.20.0-style line loads with nothing burned and every machine `NO_LOOP`; a reversed machine refuses to run; the reference reading

## Phase 6: Polish

- [ ] T015 `tools/generate_terminals.py`: + and − plates on every powered block's model (four on the Hit Box); a game test per block type that its marked + face is the + terminal
- [ ] T016 Lang, help screens (how to wire a loop), README, CHANGELOG (old bases need return lines)
- [ ] T017 Full test run; timing on 1,000 Wicks and 100 devices
- [ ] T018 Rebuild the flatworld demo with return lines; in-game quickstart

## Dependencies

Phase 1 first. Phase 2 needs T001–T004. Phases 3–5 need Phase 2. Polish comes last.
