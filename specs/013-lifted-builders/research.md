# Research: Lifted Builders and Higher Ground

**Sources:**
- **MineColonies:** tag `v1.21.1-1.1.1343-snapshot` (commit 60a463ba), the version on every server. Read: `AbstractEntityAIStructure`, `EntityAIStructureBuilder`, `AbstractEntityAIStructureWithWorkOrder`, `BuildingStructureHandler`, `EntityNavigationUtils`, `AbstractEntityAIBasic`, `CitizenAI`, `CitizenAIState`, `AIWorkerState`, `BuildingEntry`, `ModBuildingsInitializer`, `RenderBipedCitizen`.
- **Structurize:** `PlacementHandlers` at tag 1.0.839. The handler match rules haven't changed since 1.0.831; check this against the jar at M4.

## R1. Why Builders crawl

**Finding:**
- **The standing spot:** `EntityAIStructureBuilder.walkToConstructionSite` keeps a standing spot, `workFrom`. It only resets it in three cases:
  1. she stands on top of the current block's column (`workFrom.x/z == block.x/z && workFrom.y >= block.y`);
  2. the navigator reports stuck;
  3. the current block is more than 5 blocks away (2D).
- **The step-aside:** `BuildingStructureHandler.prePlacementLogic` calls `EntityNavigationUtils.walkAwayFrom(worker, pos, 1, 1.0)` when the block being placed is in her own column within one block of height.
- **What it costs:** that replaces her navigator's job. `walkToPos(workFrom)` is then no longer "on the right task", so the next block calls `nav.walkTo(workFrom)` and starts a fresh async path job unless she is within 1.5 blocks.
- **Why upgrades:** upgrades rebuild in place, so the blocks under and beside her come up again and again, and every block pays a path job (a second or more on a busy server). A player shove of more than 5 blocks hits reset case 3, and she picks a new spot.

**Decision:** treat this as the leading hypothesis, not a fact.
- **M1 logs** every `walkTo` job start, every walk-away, every `workFrom` change, and the time between placements, per Builder.
- **Expected signature:** alternating walk-away / walkTo pairs with a path job per block while placing slows to one every 2-4 seconds.

**Fix (M2):** when the step-aside fires, also clear `workFrom` and `prevBlockPosition` for that Builder. The next block then asks `PathJobMoveCloseToXNearY` for a new spot, which picks one near the block but not in its column, instead of returning to the old one.
- **Hooks:** the step-aside is marked in `prePlacementLogic`, and the clear happens at the head of `walkToConstructionSite`.

**Alternatives considered:**
- **Skip the walk-away entirely:** rejected. She would place blocks inside herself.
- **Raise the 1.5 "reached" distance:** rejected. It hides the symptom, and she would place from too far.

## R2. Telling "building" from "busy with something else"

**Finding:**
- **Top level:** citizens run a top-level state machine, `CitizenAI`, with states `IDLE, FLEE, EATING, SICK, SLEEP, MOURN, WORK, WORKING, INACTIVE`. Raids put her in `FLEE`. Food and the restaurant go through `EATING`.
- **Builder's own AI:** her job's AI (`EntityAIStructureBuilder`) runs `AIWorkerState`. `BUILDING_STEP` covers both walking to the site and placing. `MINE_BLOCK` breaks a block in the way. `NEEDS_ITEM`, `GATHERING_REQUIRED_MATERIALS`, `PICK_UP`, `INVENTORY_FULL`, `DUMPING`, `LOAD_STRUCTURE`, `START_WORKING`, `IDLE` and `DECIDE` are all not-building.

**Decision:** the watchdog judges a Builder only when the citizen state is `WORKING`, her job is the Builder's, and the job AI state is `BUILDING_STEP` or `MINE_BLOCK`. Any other state resets her watch, so time spent eating never counts.

**Alternative:** watching movement only. Rejected, because a Builder walking to lunch looks the same as one pacing.

## R3. Measuring progress

**Decision:** progress means the build's progress position (`building.getProgress()`: the iterator position and stage) changed, or a block was placed or broken (the builder's placement stat). The watch keeps the time of the last change and a rolling count of placements over the last 20 seconds.
- **Expected rate:** comes from the same formula MineColonies uses for its delay, `BUILD_BLOCK_DELAY (15) * PROGRESS_MULTIPLIER / (placeSpeedLevel/2 + PROGRESS_MULTIPLIER) * (1 - research)`.
- **Slow:** fewer than a third of the expected placements over the slow window (20 s).
- **Stuck:** no progress for the stuck time (45 s).

## R4. Rescues

**Decision.** Escalation, one step per check while the problem lasts:
1. **SLOW:** clear `workFrom` (and any running path job), so she picks a fresh spot. This is what the player's shove does today.
2. **STUCK:** teleport to a free standing spot (solid below, two air above) within 4 blocks of her next block, the one nearest open sky first.
3. **Still stuck after another stuck window:** teleport to the nearest standing spot just outside the building's corners.
4. **Still stuck:** recall to the hut, with MineColonies' own recall.

**Logging:** every step is logged at INFO with the colony, citizen, building, position and step. On/off switch and times are in the config.

## R5. The "fly when Lifted" switch

**Finding:**
- **Where Builder settings live:** the Builder's settings are one `SettingsModule` from the `BUILDER_SETTINGS` producer, created per building in `BuildingEntry.produceBuilding`.
- **Adding a setting:** `SettingsModule.with(key, setting)` adds a setting.
- **Showing it:** the client view gets its settings from the server's serialised module, so any setting added server-side shows up in the Settings tab, labelled by `com.minecolonies.coremod.setting.<key>`.

**Decision:**
- **The hook:** `BuilderSettingsMixin` injects at the return of `BuildingEntry.produceBuilding`. When the entry is the Builder's, it adds `fourtwenty:flywhenlifted` as a `BoolSetting(true)` to its settings module.
- **Old saves:** they load the module from NBT without that key, so it stays at the default, on (clarification Q4).
- **Label:** in our lang file.

**Alternative considered:** a separate module and tab. Rejected: it is more UI code, and two settings modules on one building is untested ground.

## R6. Getting joints, citizens first

**Finding:** the Dispensary already resolves joint requests. `JointCraftingModule` is a crafting module, so MineColonies' crafting resolver lets the Budtender roll joints for a colony `Stack` request. A Warehouse with joints resolves through couriers, and any request can be filled by the player through the hut.

**Decision:**
- **Opening a request:** when the switch is on and she has no joint, the Builder opens an ordinary `Stack` request for joints, as MineColonies does for her materials. It opens only while `SpareJoints` says the colony has spare.
- **What spare means:** joints in the Dispensaries and Warehouses, minus one per citizen still due today (`JointService.due`). The request asks for at most that spare, capped at a small stash (3).
- **When there's no spare:** an open request is cancelled.
- **The player:** can always hand her joints through the hut's request.

**Alternative considered:** a custom resolver that knows about demand. Rejected: the request system API is large, and gating the request achieves the same.

## R7. Flight

**Decision:**
- **Movement:** our own movement, not MineColonies navigation. While she flies we set no-gravity, cancel her navigator, and steer her each tick toward the next waypoint at about walking speed.
- **Waypoints:** come from `FlightPlanner`, a bounded 3D A* over cells where she fits (two air cells high) within 24 blocks.
  - **Destinations:** hover points from `HoverCover` (R8), or the build site from her hut.
  - **If there's no route:** she lands and walks, and the watchdog covers her.
- **Landing:** she descends at up to 0.15 blocks a tick, with fall distance reset every tick. She lands at the nearest open standing spot outside the building's footprint, or straight down if she's already outside it.
- **When she lands:** the buff ends, the switch goes off, the job ends, she leaves the `WORKING` or building states, or her chunk is about to unload.
- **After a restart:** a Lifted Builder loads on the ground. If her saved position is in the air, she is moved down to the first standing spot below.

**Alternative considered:** a MineColonies flying navigator. Rejected, because the path jobs it builds assume walking.

## R8. Higher Ground

**Finding:** the upgrade stage list is fixed in `AbstractEntityAIStructure.loadStructure`. A building above level 0 skips `CLEAR`.

**Decision:**
- **Clearing first:** `HigherGroundStageMixin`, when she is Lifted at load time, puts `CLEAR` back at the front of an upgrade's stages.
  - **What it removes:** `CLEAR` already removes only non-matching blocks, from the top down.
  - **What it leaves:** keep-terrain placeholders match anything during a builder's build (`SubstitutionPlacementHandler`: `fancyPlacement() || equals`), and solid fill matches any good floor block. The hut is in `DONT_TOUCH`.
  - **Not undone:** a stage list chosen while Lifted stays even if she lands mid-job. Clearing on foot is what new builds do anyway.
- **Reach:** in `walkToConstructionSite`, when she is Lifted, the 5-block check becomes the configured reach (default 9). Her standing spot becomes the hover point `HoverCover` picked: the free cell above or beside the work that covers the most of the next N blocks of the iterator within reach.
- **Speed:** `HigherGroundDelayMixin` scales the delay set at the end of `structureStep` by `LiftedRules.delayFactor(strain)`. That's 1/1.25 for Landrace, down to 1/1.5 for Golden God.
- **Storage contents:** `HigherGroundStorageMixin` acts before `CLEAR` breaks a block with an item handler. It moves the contents into her hut's storage, and the hut's normal pickup sends spare items to the Warehouse. Whatever doesn't fit drops at the hut. Nothing drops where the block was.

**Alternative considered:** a new AI class for Lifted Builders. Rejected, because it duplicates MineColonies' builder and breaks with every update.

## R9. Lifted time and the balance

**Decision:**
- **Times (`LiftedRules.ticks`):** Landrace 5 minutes, then by family up to Golden God 10 minutes:
  - Neutrals / Warm 5.5;
  - Green / Cool / Biome 6;
  - Cross 7;
  - Double 8;
  - Ultra 9;
  - Golden 10.
- **Potency** (spec 012) adds up to 10%.
- **Speed boost** (`LiftedRules.speed`): runs linearly from Landrace 1.25 to Golden God 1.5 by grade.
- **The model:** gains a "Lifted Builders" column, counting each busy Builder as building 12 of a day's 20 minutes, so about 2.4 Landrace joints a day; colonies on better strains need fewer. SC-006 is checked against it.

**Alternative considered:** charging by blocks placed. Rejected as harder to explain, and it rewards slow builders.

## R10. The shimmer

**Finding:** `RenderBipedCitizen` is a `MobRenderer`, so layers can be added through `EntityRenderersEvent.AddLayers`. `GoldenGodClient` already draws a body glow from three outward shells of the body model.

**Decision:**
- **Look:** `LiftedClient` adds a layer that draws only the shells, in green (base 0x40AAFF60, outer shells fainter), with a slow pulse. There's no solid recolour of the body.
- **Turning it on:** the server sends `fourtwenty:lifted` (entity id, on/off) to players tracking her, only on connections with the channel. A login or start of tracking sends the current set.
- **Smoke:** puffs come from the client every few ticks while she is shown as Lifted.
