# Quickstart: validating the Weed College in game

**Setup:**
- The local test server (`C:\Users\Ignus\mc-local-server`), with MineColonies and the Sensi Heights pack, and a colony with a warehouse and couriers.
- Swap the jar into both mods folders while `javaw.exe` isn't running.
- Short runs use the config's `collegeUnitTicks` (default 1200, about a minute per unit). Set it to 20 to watch a whole study in minutes; set it back afterwards.

## M1: the hut and one study

1. Craft or give the Weed College hut block, place it, and build level 1 with the Build Tool. Its scripted blueprint has 2 Closet Grows, a Think Tank and a desk.
2. Hire a Weedologist.
3. Queue **The Neutrals**. During M1, before the board exists, use `/fourtwenty college queue fourtwenty:chem_neutrals`.
4. Stock the warehouse with Cured Buds, Weed Seeds and Glass Bottles.
5. Watch the Weedologist. He requests and carries the supplies in, then plants and later harvests at a plot, works at the Think Tank, then writes at the desk. The status tab shows each step and the units rising.
6. Check that it finishes: an announcement, and the study shows as done.
7. Build a University with a researcher. During a College study, open the University window: the College study is not listed, and its units rise only while the Weedologist works.
8. Save and quit mid-study. Reload, fire the Weedologist and hire another. Progress is kept, nothing is requested twice, and no supplies are lost.

## M2: the board

1. Open the Board tab. You should see six branches, a tier grid with lines from parents, and the tier-1 studies available. Higher tiers show "Needs a College of level N" and child studies name their parent.
2. Queue three studies. They run in order. Remove the third, and it is back to available.
3. Try removing the head after it has started: refused, with the reason given.
4. Have a non-officer player try to queue: refused.

## M3: the tree and its effects

1. **Colours:** with a player who has caught red (owner or officer, online once), queue The Warms. Its red share of supplies is halved.
2. **Botany:** finish Dust Devil. 4 Dust Devil seeds arrive in the warehouse. Finish Soil Science, then let a colony Farmer harvest weed for a while: some harvests give one extra bud.
3. **Engineering:** finish Bench Habits. A Snip Station inside the colony works 25% faster than the same one outside it. A Terpinator's speed is unchanged.
4. **Chemistry:** finish Lean Extraction. The Inspoweedometer shows a colony Terpinator drawing half the BI of one outside the colony.
5. **Armory and Glassworks:** finish Dab Cell and Glassblowing. The colony's Blacksmith can now make a Dab Cell, and the Glassblower a Glass Bong; a colony without them can't. Players' crafting-table recipes are unchanged.
6. **Textiles:** shows "Waiting for the Tie Dye Shack".
7. **Colour privacy:** a player who hasn't discovered an accord sees it unnamed on the board, even after the colony has researched it.

## M4: the building

1. Build levels 1 to 5. Each level's plots are of Grow Op tier N, and the board's tier cap follows the level.
2. Tier 6 stays locked at level 5 until every tier-5 study of its branch is done.
3. Hold the hut block: its own diorama shows, and it looks right placed and in the build tool.
4. In the Sensi Heights repo, `check_hut_levels.py`, `check_footprints.py` and `audit_tags.py` pass for all five levels.

## M5: the page

1. With MineColonies, the Handbook's Colony Life has a Weed College page in the book's voice, with index and glossary entries, and the PDF is rebuilt.
2. Without MineColonies, the page and its links are hidden, the Think Tank works as before, and Four Twenty loads normally.
