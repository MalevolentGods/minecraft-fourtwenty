# Quickstart: testing Lifted Builders on flatworld

All runs happen on the flatworld test server (root@116.203.59.22, container `minecolonies`). Hera is never touched, and the jar goes there only when the user asks.

**Setup:**
- **The colony:** a test colony with a Builder's hut at level 5, a Dispensary, a Warehouse, and a weed field's worth of joints.
- **Speed:** the Builder at top stats; use `/mc citizens modify` (or skill commands) to max her Dexterity and Adaptability, for comparable runs.
- **The tests to compare:** paste Warehouse 2 and Tavern 1 (Sensi Heights) as real colony buildings, ready for upgrade.

## M1: confirm the crawl

1. Set `logBuilderPathing = true` and `fixStepAside = false`, and restart.
2. Queue Warehouse 2→3. Watch the Builder, and tail the log for `[FourTwenty/Builders]`.
3. **Expected if R1 is right:** while she crawls, `stepaside` and `path ... job=walkTo` pairs alternate for each block, and `placed ... sinceLast` is 40-80 ticks.
4. Record what was seen in research.md (R1 confirmed, or the real cause).

## M2: the fix and the watchdog

1. **The fix:** `fixStepAside = true`. Repeat the Warehouse 2→3 and Tavern upgrades, then compare placements per minute with M1 (SC-001: within 25% of her expected rate).
2. **Trapped:** seal a building Builder in with glass (a 1×2 box). She's rescued within `stuckSeconds` + 10 s, and the log shows `rescue NEAR_BLOCK` (SC-002).
3. **Busy elsewhere:** at mealtime and at night, check the log shows no rescue of an eating or sleeping Builder. Take her materials away, so she waits on `NEEDS_ITEM` for more than `stuckSeconds`, and check no rescue happens either (SC-003).
4. **Pockets:** the pockets under the top centre steps of Warehouse 1-4 are solid in the imported pack, and the stairs look the same.

## M3: Lifted

1. **Asking for joints:** with the hut switch on and joints in the Warehouse above what citizens are owed, the Builder asks for joints, gets them, and on starting a job logs `lifted`.
2. **Flying:** she flies with smoke and a faint green shimmer, visible to a second client.
3. **Citizens first:** take the colony's joints down to what citizens are owed. Her request cancels and she builds walking.
4. **Landing**, each case logging `landed reason=...` with no damage taken (SC-005):
   - let the buff run out with no joints left;
   - turn the switch off mid-air;
   - finish the job;
   - make it mealtime;
   - restart the server while she's mid-air.

## M4: Higher Ground

1. **Upgrading:** Lifted, run Warehouse 2→3. She clears changing blocks from the top down first (the racks that stay keep their contents), then builds bottom-up from hover points.
2. **Speed:** time it against M2's walking run (SC-004: at least twice as fast, no stall).
3. **Storage contents:** put items in a chest that an upgrade removes. They arrive in her hut and then the Warehouse; nothing drops on site.

## M5: balance

- **The model:** `python tools/progression_model.py` shows the Lifted Builders column, and one field still leaves a spare with two busy Builders (SC-006).
- **Without MineColonies:** run the client and server with MineColonies removed; Four Twenty loads and plays as before (SC-007).
