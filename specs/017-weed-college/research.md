# Research: The Weed College

Phase 0 for [plan.md](plan.md). Each entry gives the decision, why, and what else was considered.

## R1. Where the College's research lives

- **Decision**: in Four Twenty's own tracker, which is a per-colony record in `CollegeSavedData` plus study definitions in data. MineColonies' research tree is never used for progress.
- **Rationale**: checked in MineColonies 1.1.1343 (`EntityAIWorkResearcher.study()`, javap, 2026-11-27). The University's Researcher takes a random entry of the colony's `getResearchInProgress()`, with no branch filter, and calls `research()` once per mana every 1200 ticks. Any College study placed in that list would be worked by University scholars. Branch types are only `default` and `unlockables` (hidden), and neither keeps scholars out. The spec's FR-005 then follows: only Weedologists progress College studies.
- **Alternatives**:
  - A custom branch in MineColonies' tree: rejected, because scholars would work it.
  - A hidden `unlockables` branch for progress: rejected for the same reason, since in-progress entries are listed regardless.

## R2. How a study progresses ("physical")

- **Decision**: progress is counted in **work units**, one per completed work cycle of a Weedologist, at about one unit a minute of working time, the same cadence as the University's 1200-tick study step. Units per study are `72 × 2^(tier-1)` (72, 144, 288, 576, 1152, 2304), the existing `Studies.units()`. That gives 1.2, 2.4, 4.8, 9.6, 19.2 and 38.4 hours, matching FR-010.
  - A cycle rotates through the hut's real workstations: plant the study's crop on a free Grow Op plot, harvest any ripe plot, take the harvest to the Think Tank, then write up at the desk.
  - Each visit that does work adds progress. A study's crop is real (real seeds on real Grow Ops), so Botany studies end with a real harvest whose seeds go to the warehouse (FR-022).
- **Rationale**: the timing in the spec is exact (SC-001), and the Weedologist must be seen working (FR-013). A real Think Tank run ties progress to Grow Op speeds and BI power, which can't hit the spec's timing and would make the College need a Hookah and fuel. The workstations are real blocks used for real actions: the plots grow real crops, and the Think Tank and desk are where he works. But the College's clock is its own rule, in `domain/college/StudyRules`.
- **Alternatives**:
  - Running real Think Tank studies: rejected (timing, power, the 3-slot input limit, and the Think Tank's own list).
  - Progress ticking while he stands in the hut: rejected, since the spec requires visible work.

## R3. Where a colony's research record is saved

- **Decision**: `CollegeSavedData`, a dimension `SavedData` keyed by colony id, holding a `ColonyResearch` per colony (study states, progress, queue, colours known by members, finished-study effects). It is written like `DispensarySavedData`, including its refusal to overwrite an unreadable save.
- **Rationale**: research is colony-wide and must survive the College being demolished or rebuilt (Edge Cases), so it can't live in the building's NBT. The Dispensary already uses this pattern safely.
- **Alternatives**:
  - The building's NBT: rejected, because it's lost with the building and not shared by several Colleges.
  - MineColonies' colony NBT through a capability: no supported hook in this version.

## R4. The board window

- **Decision**: two module tabs on the hut, as the Bud Barn has.
  - **Status** (read-only): worker, study in progress, progress, what it's waiting for.
  - **Board**: branch buttons; a tier grid (6 columns) of study buttons drawn by a custom BlockUI pane (`StudyTreePane`) that also draws the lines from parents; a details pane (supplies, length, effect, lock reason); and the queue with remove buttons.
  - Data goes down through `serializeToView`, re-sent only when it changes. Actions go up through a payload (R5).
- **Rationale**: it matches MineColonies' own windows and the Bud Barn's look, needs no new screen system, and BlockUI panes can draw lines.
- **Alternatives**: a separate vanilla `Screen` opened from a button was kept as the fallback if BlockUI's pane drawing fights us.

## R5. Board actions from the client

- **Decision**: one NeoForge payload, `fourtwenty:college_board`, carrying `{colony id, building pos, action: QUEUE | REMOVE, study id}`. It is sent only when the connection has the channel. The server checks that the player has MineColonies' `MANAGE_HUTS` permission in that colony, that the building is a College, and that the action is valid (via `StudyRules`). Starting is just queuing onto an empty queue.
- **Rationale**: no existing Four Twenty colony window sends actions yet, and a small payload is simpler than extending MineColonies' message classes. The permission check follows MineColonies' rule for changing a hut's settings.
- **Alternatives**: MineColonies' `AbstractBuildingServerMessage`: workable, but it ties us to internal classes beyond the API.

## R6. Turning finished studies into crafter recipes (Armory, Glassworks)

- **Decision**: when an Armory or Glassworks study finishes, apply a MineColonies **research effect** `fourtwenty:effects/<study>` (an unlock, strength 1) to the colony's `IResearchEffectManager`. Our crafter recipes in `data/minecolonies/crafterrecipes/<crafter>/*.json` name it as `"research-id"`.
  - The Blacksmith gets the Kush Suit pieces, the Dab Cell and the Blazer; the Glassblower gets the bongs.
  - Effects are re-applied from `CollegeSavedData` on colony load, so they never depend on MineColonies saving them.
- **Rationale**: MineColonies gates its own crafter recipes this way (for example `"research-id": "minecolonies:effects/platearmorunlock"`, from the jar's blacksmith recipes). Effects apply without touching the University's in-progress list, so R1 still holds. Players keep their crafting-table recipes, so Principle I holds.
- **To verify first in M3 (spike task)**: the exact way to apply an unlock effect from code (`IResearchEffectManager.applyEffect` with a `GlobalResearchEffect`), and whether effect ids need a data definition. If an effect can't be applied safely, the fallback is to mark a hidden `unlockables`-branch research **finished** with `ILocalResearchTree.finishResearch`: never in progress, so the University can't work it.

## R7. Colony power and machine modifiers (Engineering, Chemistry)

- **Decision**: one hook, `ColonyEffects.factor(effect, level, pos)`. It returns 1.0 unless `pos` is inside a colony whose record has the effect. It is called in exactly these shared places:
  - `MachinePowerRule` (BI draw for Grow Ops, and Terpinators per Chemistry);
  - `BIRules`'s burn length (Hookahs) and Stash capacity;
  - the shared processing-speed path used by trimmers, curers, the Rosin Rig and the Pheno Chamber.

  The Terpinator and the Infuser are excluded from every speed factor.
- **Rationale**: Principle III (one shared power rule) and Principle II (no Terpinator or Infuser gameplay change, power only). The colony lookup is cached per colony and refreshed when a study finishes, so machines don't search colonies every tick.
- **Alternatives**: per-machine checks, rejected under Principle III.

## R8. Botany: seeds and yields

- **Decision**:
  - **Seeds:** a strain study's supplies are the seeds it breeds from. Biome strains take Landrace seeds plus the climate's item (sand, a snow block, a lily pad or a jungle sapling, the same items a Cosmic Kush Pod copies a climate with). Hybrid studies take seeds of each hybrid's parents. The Weedologist plants and harvests those during the study's cycles. At write-up the study has bred its result: the hut receives a batch of seeds of the new strain or strains (count in config: default 4 for a single strain, 2 of each for a multi-strain study). The normal dump and pickup carries them to the warehouse.
  - **Yields:** a loot modifier on the weed crop's harvest adds buds when the harvesting entity is a citizen of a colony with Soil Science, Selective Breeding or Master Grower.
- **Rationale**: the user chose physical results (clarify Q3). Seeds are items, not knowledge, so they don't bend FR-021. MineColonies Farmers break crops as their citizen entity, so the loot context has them (to confirm in M3 with a colony game test; fallback is a `BlockDropsEvent` check).
- **Alternatives**: unlocking strains only for farmers (rejected by the user, Q3: B).

## R9. Caught colours make a study cheaper (FR-020)

- **Decision**: discoveries are kept in each player's persisted data, which can't be read for offline players. So the College keeps a **members' colours** set in the colony record. It's updated whenever the colony's owner or an officer is online (at login, and whenever they learn a colour). A colour study's supplies are reduced for each of its colours in that set: by default, a caught colour's share costs half. The reduction is fixed when the study starts.
- **Rationale**: it's cheap, always available, and needs no offline-player IO.

## R10. Unknown colours on the board

- **Decision**: the board shows a colour's name and swatch only to a viewer who has discovered it. Others see the study by its accord's place ("an unknown accord") and greyed swatches, even after the colony has researched it.
- **Rationale**: FR-021 says the College never teaches a player, and the Handbook already hides undiscovered terpenes (spec 016 FR-012). Showing names would teach.

## R11. The hut's workstations and the levels

- **Decision**: the building finds its stations by scanning its area every 200 ticks (the Bud Barn's way): Grow Op blocks (plots), one Think Tank, and a desk (a lectern, Sensi Heights' "desk" in the blueprints).
  - Level N's blueprint holds plots of Grow Op tier N (FR-017).
  - Worker count: 1 at levels 1 and 2, 2 from level 3 (FR-004); both work the same study, so progress adds up.
  - The tier cap: level N allows tier N; tier 6 needs level 5 and every tier-5 study of its branch (FR-016).
- **Rationale**: it follows a proven pattern, and a player-added plot or bench counts too.

## R12. Blueprints and the hut block

- **Decision**:
  - **Blueprints:** `sensi-heights/tools/design_college.py` writes all five levels with `hutkit.py`. A shed with a chalkboard (level 1) grows to a quad with a greenhouse (level 5). Earth tones, the signature door from level 3, doors saved closed. The pack checks run (`check_hut_levels.py`, `check_footprints.py`, `audit_tags.py`), and the import goes into the jar.
  - **Hut block:** `tools/generate_weedcollege_hut_model.py`, a little two-block diorama (a schoolhouse front with a greenhouse roof and a chalkboard), run through `hut_model_check.check()` and listed in `tools/tests/test_hut_models.py`.
- **Rationale**: the user chose scripted levels (clarify Q2), and every hut needs its own diorama (user rule, 2026-11-24).
