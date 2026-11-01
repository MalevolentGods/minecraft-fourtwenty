# Tasks: Extracts, Breeding and the Hybrid Ladder

**Tests**: Required by the constitution. `J` = `src/main/java/com/malevolentgods/fourtwenty`.

## Phase 1: Strains and traits (US1, US3)

- [X] T001 [P] [US1] Traits, averaging and drift in J/domain/Traits.java with tests
- [X] T002 [US3] Add grades, parents and 22 strains to J/domain/Strain.java; seed chances in J/domain/TrimRules.java with tests
- [X] T003 [US1] Traits data component in J/registry/ModDataComponents.java, carried through trimming, curing, joints and pipes
- [X] T004 [US1] Crop block entity in J/block/WeedCropBlock.java and loot in J/registry/ModStrainData.java
- [X] T005 [US1] Traits scale effects in J/util/StrainEffects.java; yield in crop drops
- [X] T006 [US3] Plot requirements and growth rates in J/block/WeedCropBlock.java and J/item/StrainItems.java
- [X] T007 [US1] Tooltips and Jade in J/item/StrainItems.java and J/compat/jade/FourTwentyJadePlugin.java
- [X] T008 [US1] New strain effects in J/util/StrainEffects.java
- [X] T009 [US1] Item art in tools/generate_strain_items.py
- [X] T010 Game tests in src/gametest/java/com/malevolentgods/fourtwenty/gametest/StrainGameTests.java

## Phase 2: Breeding (US2)

- [ ] T011 [P] [US2] Cross table in J/domain/Crosses.java with tests, including that every strain is reachable
- [ ] T012 [US2] Pheno Chamber, Lab and Cathedral blocks and entities in J/processing/
- [ ] T013 [US2] Screen, ghosts, popups and ? help in J/client/
- [ ] T014 [P] [US2] Models, recipes, ports and loot in tools/generate_processing.py and tools/generate_ports.py
- [ ] T015 [US2] Hybrids inspire parents' colours in J/infuser/Infuser.java
- [ ] T016 [US2] Game tests

## Phase 3: Biome strains (US4)

- [ ] T020 [P] [US4] Climates in J/domain/Climate.java with tests
- [ ] T021 [US4] Climate-touched seeds from harvests, and settling in the Chamber
- [ ] T022 [US4] Cosmic Kush Pod copies a climate, in J/power/GrowOpBlock.java and J/power/GrowOpBlockEntity.java
- [ ] T023 [US4] Game tests

## Phase 4: Golden God (US5)

- [ ] T030 [US5] Flight, effects and landing in J/util/GoldenGod.java
- [ ] T031 [US5] Shine and halo render layer in J/client/, payload to tracking players
- [ ] T032 [US5] Game tests of flight ending, death and logout

## Phase 5: Rosin Rig (US6)

- [ ] T040 [P] [US6] Extract steps and burn lengths in J/domain/Extracts.java with tests
- [ ] T041 [US6] Items, art and strain and trait carry
- [ ] T042 [US6] Rig block, entity, screen and help, with models and ports
- [ ] T043 [US6] Hookah and pipe take extracts
- [ ] T044 [US6] Game tests

## Phase 6: Construction Joint (US7)

- [ ] T050 [US7] Block, menu, recipes list and art
- [ ] T051 [US7] Game test

## Phase 7: Finish

- [ ] T060 README, CHANGELOG and help text
- [ ] T061 Update PLAN.md and HANDOFF.md in IgnusVM/claude-misc
