# Tasks: Kush Suit, Blazer and Charging

**Input**: `specs/005-kush-suit/` (spec, plan, research, data-model, quickstart)
**Tests**: required by the constitution (pure rules have JUnit tests, world behaviour has game tests).

`S` = `src/main/java/com/malevolentgods/fourtwenty`.

## Phase 1: Setup

- [ ] T001 Gear config values (capacities, costs, charge rates, stomp, beam) in `S/gear/GearConfig.java`, registered as a server config in `S/FourTwenty.java`
- [ ] T002 [P] `data/fourtwenty/gear/abilities.json` with the starting ability set (research R4)

## Phase 2: Foundational

- [ ] T003 [P] `S/domain/HueFamilies.java` (colour code to family, research R3) and `src/test/.../domain/HueFamiliesTest.java`
- [ ] T004 [P] `S/domain/GearRules.java` (spend, charge steps, stomp maths, flight-end decisions) and `src/test/.../domain/GearRulesTest.java`
- [ ] T005 `GearState` data component in `S/registry/ModDataComponents.java`
- [ ] T006 `S/gear/Gear.java`: `powered`, `spend`, `charge`, the ability lookup from `abilities.json`; `S/gear/Abilities.java` loader with a JUnit test that every colour fits its piece

## Phase 3: User Story 1, suit powers while high (P1)

- [ ] T007 [US1] Armour material and the four armour items (`S/gear/KushArmorItem.java`), registered in `S/registry/ModItems.java`, with tooltips showing charge and terpenes
- [ ] T008 [US1] Helm: night vision, ore and mob sense, and a zoom key; HUD layer in `S/client/HazeHud.java`
- [ ] T009 [US1] Greaves: knockback cancel, crouch-landing stomp, resistance and slow-fall abilities, in `S/gear/GearPowers.java`
- [ ] T010 [US1] Boots: jump boost, fall cancel, higher jump, water walking, speed, in `S/gear/GearPowers.java`
- [ ] T011 [US1] Game tests: sober means no powers and no spending; each armour power works while high and charged (`src/gametest/.../GearGameTests.java`)

## Phase 4: User Story 2, flight that can't kill you (P1)

- [ ] T012 [US2] `S/gear/FlightLease.java`, generalised from `GoldenGod`'s lent flight; Golden God moved onto it
- [ ] T013 [US2] Puff Plate flight with BI spend, hover and fast-flight abilities, toughness, in `S/gear/GearPowers.java`
- [ ] T014 [US2] Game tests: every end path (sober, empty, removed, death, logout) ends in Slow Falling with no fall damage

## Phase 5: User Story 5, charging (P1)

- [ ] T015 [US5] `S/gear/ReUpStationBlock.java` and `ReUpStationBlockEntity.java`, four tiers, hanging slot and pad, in `S/registry/ModPower.java`
- [ ] T016 [US5] `S/gear/DabCellItem.java`, which charges at a station and tops up carried and worn gear
- [ ] T017 [US5] Game tests: hanging and pad charging at the tier rate, Dab Cell transfer, overload on too high a tier

## Phase 6: User Story 4, terpenes unlock abilities (P2)

- [ ] T018 [US4] Terpene slots screen and menu (sneak-use the piece) in `S/gear/GearMenu.java` and `S/client/GearScreen.java`; wrong family refused
- [ ] T019 [US4] Game test: an ability appears with its colour and goes when it's removed; wrong family refused

## Phase 7: User Story 3, the Blazer (P2)

- [ ] T020 [US3] `S/gear/BlazerItem.java`: modes, cycling, Blaze Beam
- [ ] T021 [US3] Bore Toke, Timber Torch and Paint Puff through the game's break and place checks, in `S/gear/BlazerModes.java`
- [ ] T022 [US3] Game tests: each mode's effect and cost; nothing changes in protected spots

## Phase 8: Art and polish

- [ ] T023 [P] Art generator `tools/generate_kush_suit.py`: armour layers, icons, station models, Dab Cell
- [ ] T024 Recipes from the mod's own progression (FR-016); lang strings
- [ ] T025 README and CHANGELOG; full test run; in-game walkthrough (quickstart.md)

## Dependencies

Phase 2 before all stories. US1 before US2, which needs the Plate item. Charging (US5) can come any time after T007. US4 and US3 come after US1.
