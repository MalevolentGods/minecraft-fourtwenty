# Tasks: Weed Progression Balance

**Input**: Design documents from `specs/012-progression-balance/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/config.md, quickstart.md

**Tests**: included. The constitution (VI) requires JUnit tests for `domain/` rules and game tests for behaviour in the world.

**Paths**:
- `J` = `src/main/java/com/malevolentgods/fourtwenty`
- `T` = `src/test/java/com/malevolentgods/fourtwenty`
- `GT` = `src/gametest/java/com/malevolentgods/fourtwenty/gametest`

## Phase 1: Setup

- [X] T001 Write the progression model skeleton in `tools/progression_model.py`:
  - read the default numbers from `specs/012-progression-balance/contracts/config.md`, or a server toml given with `--config`;
  - work out one crop's minutes at each growth speed from the random-tick rate (3 per chunk section per tick, 4,096 blocks, weed at `cropSpeed` 0.5, grade speeds 1, 1, 0.7, 0.5, 0.35, 0.25, plot speeds 1.5, 2, 3, 5, 8);
  - print a table of minutes per crop per family and plot tier.
- [X] T002 [P] Add the model's self-check to `tools/progression_model.py`: with today's numbers (Landrace 1–3 buds, 25% seed), it reports the seed stock growing about 1.5× per crop, matching the user's 17 buds giving 12 seeds.

## Phase 2: Foundational (blocks every story)

- [X] T003 Create `J/domain/StrainStats.java`, pure, as in data-model.md:
  - a record of yield min and max, potency per `Strain.Family`, and plot seed chances for tiers 1 to 5, with `DEFAULT`;
  - `rollYield(family, traits, roll)`, `potencyLevels(p)`, `potencyLength(p)`, `extraSeed(tier, roll)`;
  - clamping: max ≥ min, potency 1 to 5, chances 0 to 1.
- [X] T004 [P] Create `T/domain/StrainStatsTest.java`:
  - every default from the data-model.md table;
  - `rollYield` at roll 0 and just under 1 for each family;
  - the yield trait adding and subtracting, never below 1;
  - potency steps;
  - plot chances by tier, and no plot giving false;
  - clamping of a bad config.
- [X] T005 Change `TrimRules.Chances.DEFAULT` in `J/domain/TrimRules.java` to Landrace 0.12, Neutrals 0.15, Cool 0.10 and Biome 0.10. Update `T/domain/TrimRulesTest.java` if it pins the old values.
- [X] T006 Add the new config keys to `J/processing/ProcessingConfig.java`, per contracts/config.md:
  - `yield<Family>` (as "min-max"), `potency<Family>`, `plotSeedChance1`–`5`;
  - a `ProcessingConfig.strainStats()` accessor that builds `StrainStats` with clamping.
- [X] T007 Add `starterSeeds` (int, 0 to 64, default 4) to `J/compat/minecolonies/DispensaryConfig.java`, with its accessor.

**Checkpoint**: `./gradlew test` passes; nothing in the world has changed yet.

## Phase 3: User Story 1 — A slow, honest start (P1) 🎯 MVP

**Goal**: 4 starting Landrace seeds, 1 bud per Landrace harvest, 12% trimming seeds.
**Independent test**:
- a new player gets 4 Landrace seeds once;
- a mature Landrace crop drops 1 rough bud and 1 seed every time;
- about 12% of trimmed Landrace buds give a seed.

- [X] T008 [US1] In `src/main/resources/data/fourtwenty/loot_table/blocks/weed_crop.json`, set the rough bud pool's count to a constant 1 (the family roll replaces the uniform 1 to 3).
- [X] T009 [US1] In `J/registry/ModStrainData.java`, change `CopyStrain.run` so a rough bud's count is `ProcessingConfig.strainStats().rollYield(strain.family(), traits, roll)`:
  - the roll comes from `context.getRandom()`;
  - seeds and other drops are unchanged.
- [X] T010 [US1] Create `J/compat/minecolonies/StarterSeeds.java`:
  - on `PlayerEvent.PlayerLoggedInEvent`, if persistent data `fourtwenty.starterSeeds` is unset and `starterSeeds` > 0, give that many Landrace `weed_seeds` (strain set with `ModDataComponents.withStrain`);
  - drop them at the player if the inventory is full;
  - then set the flag.
- [X] T011 [US1] Register `StarterSeeds` from `J/compat/minecolonies/ColonyIntegration.java`'s `register` (MineColonies only).
- [X] T012 [P] [US1] Create `GT/ProgressionGameTests.java`:
  - a mature Landrace crop broken 50 times always drops exactly 1 rough bud and 1 Landrace seed;
  - 1,000 Landrace rough buds trimmed through `TrimRules` give between 9% and 15% seeds.
- [X] T013 [P] [US1] Add a game test in `GT/ProgressionGameTests.java` for the starter grant. A mock player's first login gives 4 Landrace seeds and the second gives none; this runs only when MineColonies is present (follow the existing colony test gating).

**Checkpoint**: US1 works on its own; check in game on the local test server (quickstart steps 1–2).

## Phase 4: User Story 2 — Every strain step is worth it (P1)

**Goal**: family yields, potency and tooltips.
**Independent test**: plain-trait harvests of each family average their table yield; a high-potency strain's high is stronger and longer; tooltips show the stats.

- [X] T014 [US2] In `J/util/StrainEffects.java`, scale every effect `give(...)` applies by the smoked strain's family potency:
  - level plus `StrainStats.potencyLevels(p)`;
  - duration times `StrainStats.potencyLength(p)`;
  - both on top of the trait's `potencyBonus` and `lengthFactor`;
  - Landrace (potency 1) unchanged.
- [X] T015 [US2] In `J/item/StrainItems.java`, add a line to seed, rough, trimmed and cured bud and joint tooltips: "Yield {min}–{max} · Potency {p} · Growth {normal|slower|slow|slowest}" (growth from the strain's grade). Add the lang keys to `src/main/resources/assets/fourtwenty/lang/en_us.json`.
- [X] T016 [P] [US2] Add game tests to `GT/ProgressionGameTests.java`:
  - a Golden God crop drops 4 or 5 rough buds every time;
  - a Cross crop averages between 2.2 and 2.8 over 200 harvests;
  - a yield-trait-1 Landrace still drops 1.
- [X] T017 [P] [US2] Add unit tests to `T/domain/StrainStatsTest.java` for the potency maths applied to a sample effect (level and ticks before and after).

**Checkpoint**: US1 and US2 both work; tooltip and smoke checked in game (quickstart steps 3 and 5).

## Phase 5: User Story 3 — Grow Ops are a seed stepping stone (P2)

**Goal**: an extra seed by plot tier on every harvest.
**Independent test**: harvests on a tier-5 plot give an extra seed about half the time; tiers 4–5 keep big harvests and replanting.

- [X] T018 [US3] Add `GrowOps.extraSeed(ServerLevel, BlockPos, List<ItemStack> drops)` in `J/power/GrowOps.java`. If the crop stands on a `GrowOpBlock` of tier t and `StrainStats.extraSeed(t, roll)` passes, it adds one seed with the crop's strain and traits.
- [X] T019 [US3] Call `extraSeed` from `GrowOps.onDrops` (break harvests, before the tier-4/5 replant takes its seed) and from `J/block/WeedCropBlock.java`'s right-click harvest. Pot Pots stay unchanged; they aren't Grow Ops.
- [X] T020 [P] [US3] Add game tests to `GT/ProgressionGameTests.java`:
  - Landrace on a tier-5 plot over 200 harvests gives between 40% and 60% extra seeds;
  - on a tier-1 plot, between 4% and 16%;
  - tier-5 harvests still replant and give ×1.5 buds.

**Checkpoint**: plots give seeds in game (quickstart step 4).

## Phase 6: User Story 4 — The colony's appetite matches its farms (P2)

**Goal**: Bud Barn output and Dispensary use agree at each milestone.
**Independent test**: the model's colony table shows 0–50% spare at every stage.

- [ ] T021 [US4] Add the colony check to `tools/progression_model.py`:
  - **Demand:** one joint (one cured bud) per adult citizen per in-game day (`DemandRules.defaults()`, 24,000 ticks), at typical citizen counts per stage.
  - **Supply:** what a farm the size a Farmer's field supports produces of that stage's strain.
  - **Output:** demand, supply and spare per milestone.
- [ ] T022 [US4] If any stage falls outside 0–50% spare, tune the numbers and record each change in `specs/012-progression-balance/research.md` under a new D9. Only if numbers alone can't fix it, change `BudBarnRules.ROUGH_PER_TRAY` in `J/compat/minecolonies/domain/BudBarnRules.java` or the demand interval in `DemandRules`.

## Phase 7: User Story 5 — The run takes about 60 hours (P3)

**Goal**: the model reports every milestone within 25% of its target.
**Independent test**: `python tools/progression_model.py` passes its milestone check.

- [ ] T023 [US5] Finish the run simulation in `tools/progression_model.py`, a typical player:
  - replants everything;
  - smokes a joint an hour;
  - breeds when the Infuser is reachable, builds the first Grow Op when the power is;
  - climbs each family when its seeds can be stocked, about 16 plants of it.
- [ ] T024 [US5] Add the milestone check to `tools/progression_model.py`: each milestone's hour against its target (1, 5, 10, 17.5, 27.5, 40, 50, 60), within 25%, Golden God within 50–75; exit 1 on a miss.
- [ ] T025 [US5] Run the model. If it misses, tune the defaults in `StrainStats.DEFAULT`, `TrimRules.Chances.DEFAULT` and `contracts/config.md` in this order: seed chance, plot seed chance, yield (research D8). Re-run until it passes, and re-run `./gradlew test`.

## Phase 8: Polish & Cross-Cutting

- [ ] T026 [P] Update `CHANGELOG.md` Unreleased:
  - the balance (Landrace 1 bud, family stats, plot seeds, starting seeds, tooltips);
  - "servers: edit these settings", copying the changed-defaults table from `contracts/config.md`.
- [ ] T027 [P] Update `README.md`'s growing section with the per-family table and the plot seed chances.
- [ ] T028 Edit the saved toml on the local test server (`C:\Users\Ignus\mc-local-server\config`) and the game-test run dir (`.compat/runs/beds-dev/config`) to the new defaults. Leave Hera and flatworld for release day (contracts/config.md lists them).
- [ ] T029 Run the full checks:
  - `./gradlew test`;
  - the game tests (only the known `malformedAndFutureSavesFailClosed` failure);
  - the model;
  - the quickstart in game on the local test server.

Report what was and wasn't seen in game (constitution VII).

## Dependencies

- **Setup (T001–T002)** first. The model only needs config.md, so it can run beside Phase 2.
- **Foundational (T003–T007)** blocks every story.
- **US1 (T008–T013)**: needs T003, T005, T006 and T007.
- **US2 (T014–T017)**: needs T003 and T006. It is independent of US1, apart from sharing `CopyStrain` (T009 first).
- **US3 (T018–T020)**: needs T003 and T006. It is independent of US1 and US2.
- **US4 (T021–T022)** and **US5 (T023–T025)**: need the model (T001) and the final numbers; run after US1–US3 so the model mirrors the code.
- **Polish (T026–T029)**: last.

## Parallel examples

- **During Phase 2:** T004 (tests) beside T003, then T005, T006 and T007 in different files.
- **US1:** T012 and T013 (game tests) while T009–T011 are written.
- **After Phase 2:** US2's T014–T015 and US3's T018–T019 touch different files and can go side by side.

## Implementation strategy

1. **MVP = US1:** Landrace at 1 bud, 12% seeds and 4 starting seeds fix the user's complaint on their own. Ship it to the local test server and play it.
2. **Then US2 and US3:** the ladder and plots that give a reason to climb.
3. **Then US4 and US5:** the model sets the final numbers and checks the colony; any tuning is config-only.
4. **Commits:** one per task or small group, conventional messages, every phase checked in game before the next (constitution VII).
