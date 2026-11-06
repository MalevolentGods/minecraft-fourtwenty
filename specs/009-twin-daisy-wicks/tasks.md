# Tasks: Twin Wick and Daisy Wick

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

Paths: `S` = `src/main/java/com/malevolentgods/fourtwenty`, `T` = `src/test/java/com/malevolentgods/fourtwenty`, `G` = `src/gametest/java/com/malevolentgods/fourtwenty/gametest`, `R` = `src/main/resources`. Tests are asked for by the constitution (VI) and the spec's success criteria.

## Phase 1: Setup

- [ ] T001 Register the 8 cable blocks and items (`{hemp,rolled,glass,gold}_{twin,daisy}_wick`) in `S/registry/ModPower.java` as `CableBlock(kind, tier)`, add them to the creative tab, a `fourtwenty:bi_cables` block tag and `mineable/pickaxe` in `R/data/`

## Phase 2: Foundational

- [ ] T002 [P] `S/domain/DaisyLine.java`: order a Daisy line from cable neighbours and device contacts (research R3): components must be paths, devices beside or inline, inline devices at component ends, one path overall, end with the lower key first; fail with BRANCH, RING or EMPTY; ties by face order
- [ ] T003 [P] `T/domain/DaisyLineTest.java`: straight line, inline devices, beside devices, mixed, two devices beside one cable, a corner beside a block, a branch, a ring, a device touching three components, an inline device not at an end, empty
- [ ] T004 Conductor refactor in `S/power/Network.java`: key the wire graph by `Conductor(pos, Core)` (WIRE for single Wicks), add `core` to `Network.Wick`, keep burn, glow and readings per conductor; no behaviour change
- [ ] T005 Run the full game-test suite: every 006 and 007 circuit test passes unchanged before going on
- [ ] T006 `S/power/CableBlock.java`: kind, tier, COLOUR, six side links (WIRE to a cable of its kind and colour, or for a Twin a red or black single Wick; PLUG to any powered block), LIT, shapes, dyeing, glow and `BIGrid.changed` as in `WickBlock`; `WickBlock.link` shows WIRE towards a Twin Wick when the Wick is red or black

## Phase 3: User Story 1 - A row in parallel with one line (P1)

**Goal**: one Twin line powers a row in parallel, whatever the facings. **Independent test**: a Hookah and four machines alongside and inline read as on rails.

- [ ] T007 [US1] Twin Wick in `Network.walk` (`S/power/Network.java`): PLUS–PLUS and MINUS–MINUS joins between Twin blocks of one colour; contacts from any face of a powered block, + terminal to PLUS and − to MINUS; walk on from a device through every face for cables (`S/power/Terminal.java`: the terminal a cable on a given face meets)
- [ ] T008 [US1] Game tests in `G/CableGameTests.java`: four machines alongside a Twin line read within 1% of the same circuit on rails; inline the same; random facings never reversed; eight machines brown out evenly

## Phase 4: User Story 2 - A string in series with one line (P1)

**Goal**: one Daisy line makes a closed series string. **Independent test**: ten Lit Bits on a Toke Daisy line each read about 4.2 Kb.

- [ ] T009 [US2] Daisy Wick in `Network.walk`: collect Daisy blocks per colour and the devices touching them, order with `DaisyLine`, splice per research R4 (upstream − for sources, + otherwise; return core joined along the line; end caps); a failed line adds no connections and records its reason for each of its blocks
- [ ] T010 [US2] Game tests in `G/CableGameTests.java`: three equal loads share the voltage; same readings from either end and with the source first, last or in the middle; beside and inline alike; ten Lit Bits on a Double Hose each within 5% of 4.2 Kb and lit; two sources add; a broken end opens the string; a branch carries nothing

## Phase 5: User Story 3 - Mixing with rails (P2)

**Goal**: cables meet red and black rails. **Independent test**: a Twin line fed from rails, and a Daisy string between a red and a black Wick.

- [ ] T011 [US3] Single-Wick joins in `Network.walk` (research R5): red to a Twin's PLUS, black to its MINUS, others not; a Daisy line with a red Wick at one end and a black at the other runs between them; Hit Box contacts by face (front and back the high winding, left and right the low; top and bottom none) in `S/power/Terminal.java`
- [ ] T012 [US3] Game tests in `G/CableGameTests.java`: Twin from rails; a non-red Wick doesn't join; Daisy between red and black; a Hit Box feeding a Twin line from its low winding; Twin and Daisy beside each other don't join

## Phase 6: User Story 4 - See it and read it (P2)

**Goal**: the cables look and read like what they are. **Independent test**: the meter reads both and names a failed Daisy line.

- [ ] T013 [US4] Readings in `S/power/Circuits.java` and `S/power/InspoweedometerItem.java`: Twin (voltage between cores, current per core), Daisy (string current, voltage between cores), Daisy failures; lang keys from the contract
- [ ] T014 [P] [US4] `tools/generate_cables.py`: per tier, Twin and Daisy textures (normal and lit), core/arm/fork models, multipart blockstates by side and colour, item models; run it and `tools/check_block_assets.py`
- [ ] T015 [P] [US4] Fittings where a cable plugs into a block on any face: `S/client/PortModel.java` draws a block's port part for every face a cable plugs into
- [ ] T016 [US4] Game test in `G/CableGameTests.java`: burnout of a Hemp Twin Wick on a Double Hose, in smoke and ash, nothing else breaks; a Daisy failure is readable

## Phase 7: Polish

- [ ] T017 [P] Recipes (`R/data/fourtwenty/recipe/*_twin_wick.json`, `*_daisy_wick.json`), loot tables, lang names, tooltips
- [ ] T018 README and CHANGELOG
- [ ] T019 Full unit and game test run; build the jar
- [ ] T020 Flatworld: the quickstart, after checking who is online and that the client is closed before swapping its jar

## Dependencies

- T001 → T006. T002–T003 any time before T009. T004 → T005 → T007, T009, T011.
- US1 (T007–T008) and US2 (T009–T010) are independent after Phase 2. US3 needs T007 and T009. US4's readings need T007 and T009; its art (T014, T015) can be done any time after T006.

## Parallel examples

- T002 and T003 alongside T004.
- T014 and T015 alongside T009–T012.

## Implementation strategy

MVP: Phases 1–3 (Twin Wick), which already removes return lines from parallel builds. Then Daisy (Phase 4), rails (Phase 5), art and readings (Phase 6), polish (Phase 7). Commit after each phase.
