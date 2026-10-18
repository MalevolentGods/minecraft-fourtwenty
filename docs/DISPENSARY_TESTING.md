# In-game test playbook

Manual checks for the colony features, using the op-only `/fourtwenty` testing commands (permission level 2, from chat or the server console). Each scenario lists its setup, the steps, and what you should see. Run them on a sandbox world.

Unit tests (`./gradlew test`) cover the pure rules. The native colony GameTests (`src/colonytest`) need the frozen reference pack described in [DISPENSARY_PHASE6](DISPENSARY_PHASE6.md).

## Testing commands

`<citizens>` is an entity selector, such as `@e[type=minecolonies:citizen,distance=..10]` or `@e[type=minecolonies:citizen,name="Lucia Y. Walsh"]`. From the console, prefix with `execute positioned X Y Z run` to use `distance`.

| Command | Effect |
| --- | --- |
| `/fourtwenty status <citizens>` | Prints the dispensary clock, then one line per colonist: first exposure (or N/A), type, step (and base step), any tolerance break, home level, demand status and when due, carried doses |
| `/fourtwenty expose <citizens>` | First exposure with a random roll (as a bong session would) |
| `/fourtwenty expose <citizens> <type> <step>` | First exposure with a fixed type (`smoke`, `edible`, `both`, `none`) and step (0–6) |
| `/fourtwenty forget <citizens>` | Back to "never tried it" (first exposure N/A) |
| `/fourtwenty due <citizens>` | A craving colonist is due now (their grace deadline is kept) |
| `/fourtwenty break <citizens> start` / `end` | Start or end a tolerance break |
| `/fourtwenty drift <citizens> <rolls>` | Apply drift rolls with the configured odds and print the steps |
| `/fourtwenty clock <ticks>` | Advance the dispensary clock: due times, grace, health and breaks move; the world's day/night does not |
| `/fourtwenty stock <joint\|cookie\|pipe\|brownie> <count>` | Put menu items into the nearest Dispensary's storage |

Steps: 0 no interest, 1 every 4 days, 2 every 2 days, 3 daily, 4 twice a day, 5 three times a day, 6 four times a day, 7 tolerance break.

Customers only visit during the day, when not hungry, sick or asleep. Day/night must be running (`/gamerule doDaylightCycle true`), or set `/time set day`.

## Exposure and affinity

**First exposure from a bong**

1. `/fourtwenty forget @e[type=minecolonies:citizen]`, then `/fourtwenty status @e[type=minecolonies:citizen]`. Everyone shows "first exposure N/A" and "no craving".
2. Light a bong (water, fuel, bud) and bring a colonist within 6 blocks.
3. `status` that colonist: they now have a type and step, "exposed N ticks ago", and are not yet due.
4. Their happiness tab shows the cannabis row with "(type · frequency)" in the tooltip; before exposure it reads "never tried it".

**Secondhand smoke:** stand within 4 blocks of an unexposed colonist and smoke a joint or pipe yourself. `status` shows them exposed.

**Cravings start one interval after exposure:** `expose <c> smoke 3`; `status` says "due in 24000 ticks". `/fourtwenty clock 24000` → "due now" (grace).

**Drift odds:** `expose <c> both 3`, then `drift <c> 200`. Roughly 1 roll in 10 changes the step, about three up for every one down. Reaching 7 starts a tolerance break, after which rolls stop changing anything.

**Tolerance break**

1. `break <c> start`. `status` shows step 7 and "tolerance break 48000 ticks left", with no craving. The happiness tab shows "Cannabis: tolerance break" as a penalty row.
2. `/fourtwenty clock 48000` (or `break <c> end`). The step returns to the base step and cravings resume.

**Uninterested colonists:** `expose <c> none 3`, then `due <c>`. They report no craving and never visit.

**Persistence:** expose some colonists, then restart the server. `status` shows the same exposure times, types and steps.

## Service at the shop

**Restaurant-style serving**

1. Setup: a staffed Dispensary, a colonist with a home, `expose <c> smoke 3`, `/fourtwenty stock joint 8`, `due <c>`.
2. The colonist walks into the shop and stops inside, and the Budtender walks over to them.
3. They get a joint, sit or stand, smoke, and `status` shows "satisfied, due in …".

**Self-service:** with no Budtender hired, the customer serves themselves after about 20 seconds.

**No constant courier pickup:** watch the hut's requests while the Budtender crafts. No pickup request reappears after each craft; one only appears when the hut is full.

**Shop choice:** at a level 1 (joints only) Dispensary, `expose <c> edible 3` then `due <c>`. The colonist does not walk there; they stay unmet until a level 2+ shop has edibles.

## Menu and take-away (by home level)

Use `expose <c> <type> 3`, set their home level, `stock` the items, then `due <c>`.

| Home | Type | Stock | Expect them to leave with / consume |
| --- | --- | --- | --- |
| 1–2 | smoke | joints | Smokes a joint at the shop |
| 1–2 | edible | cookies | Eats a cookie at the shop (eating sounds, no smoke) |
| 3–4 | smoke | pipes | Takes a pipe, uses one dose there (3 uses left), and later doses come from the pipe anywhere |
| 3–4 | edible | brownies | Takes 4 brownies, eats one there |
| 3–4 | smoke | joints only | Smokes a joint at the shop, then complains "nothing to take home" |
| 5 | smoke | joints + pipes | Smokes a joint at the shop and keeps a full pipe |
| any | both | everything | Takes the best item in stock: brownie, then pipe, then cookie, then joint |

**Carried doses:** after a take-away, `due <c>` again. They use the carried item where they are, without walking to the shop. A pipe breaks on its fourth use.

**Edibles aren't food:** a colonist carrying cookies still goes to the restaurant when hungry, and the cook never serves cookies or brownies.

## Production and the status tab

1. Upgrade (or set) the Dispensary to each level and open its Dispensary service tab.
2. Level 2 adds cookies (a cookie + bud), level 3 pipes (oak, birch and spruce planks, charcoal, bud), and level 4 brownies (4 cocoa, 4 wheat, bud).
3. The tab lists each menu item as "stock / target" and flags missing ingredients ("Production needs Cookie.").
4. With ingredients delivered, the Budtender crafts each item up to its target.

## Player sales

| Player | How | Expect |
| --- | --- | --- |
| Colony member (hut permission) | Inside the shop, right-click the Budtender holding an emerald, then click an item | Free, "Free for colony members" |
| Non-member | Same | Pays joint 1, cookie 1, brownie 2 or pipe 3 emeralds, which land in the hut's storage |

Also check that:
- the right-click is refused with "Come into the shop to buy" from outside the shop;
- items show "Sold out" with no stock;
- a normal right-click (no emerald) still opens the citizen window.

## Bongs and the Budtender

- **Fuels:** flint and steel burns 30 s per lighting and loses 1 durability; a lava bucket burns 45 s and leaves an empty bucket; blaze powder burns 60 s.
- **Front bong only:** the Budtender tends the bong nearest the hut block and leaves the others alone.
- **Water and fuel:**
  - he fills water (free) and fuel even with no customers;
  - he adds buds only while colonists are in the shop, so the bong lights then.
- **Requests:** with no fuel or buds in stock, he requests "bong fuel" (any item in the `fourtwenty:bong_fuel` tag) and Weed Buds within about 10 seconds.
- **Bong session:** colonists within 6 blocks of a burning bong get the "Bong session" happiness row for 5 minutes, and the day's first session rolls drift.
- **Facing:** a placed bong's top texture reads upright from where you stood.

## Other branches

- **Builder costs** (`feat/froglight-placement-handler`): a blueprint with froglights lists sea lanterns in the builder's resources. CB microblock covers cost their source block at 1 per 8 covers, so the Dispensary level 5 roof trim needs 6 lime lamps.
- **Farmland** (`fix/weed-crop-maintains-farmland`): weed crops on dry farmland keep it as farmland. Test with `/gamerule randomTickSpeed 4096` briefly, then set it back to 3.
