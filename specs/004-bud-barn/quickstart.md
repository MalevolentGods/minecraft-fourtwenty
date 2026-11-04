# Quickstart: checking the Bud Barn

## Automated

- `./gradlew test` covers `BudBarnRulesTest`: step order, the buffer, and that per-level speed rises.
- `./gradlew runGameTestServer -PcompatRunDir=.compat/runs/beds-dev` covers `TrimTrayBlock.trim`, which gives the same trimmed bud and seed odds as a player's click, keeping strain and traits. The Bud Barn classes load only with MineColonies, which the game-test run has.

## On the local test server (`C:\Users\Ignus\mc-local-server`)

1. Start a colony and build a level 1 Bud Barn from Sensi Heights (Agriculture → Horticulture). Hire a Trim Daddy.
2. Put 32 rough buds of two strains in the warehouse.
3. Over a Minecraft day, check that:
   - the Trim Daddy fetches rough buds, trims at the tray, hangs on the racks and takes cured buds down;
   - cured buds of both strains reach the warehouse, one per rough bud, strains unchanged;
   - Landrace trims bring about one seed in four to the warehouse.
4. Run a Dispensary with no cured buds and confirm the Budtender gets the Bud Barn's buds and serves. This is research R5: confirm bred strains fill its request.
5. Upgrade to level 5 and confirm more trays and racks, faster trimming, and roughly four times the output.
6. Hang a bud by hand on the hut's rack and confirm the Trim Daddy leaves it until it's cured.
7. Starve the hut of rough buds and confirm its status says it's waiting for rough buds.

## Without MineColonies

Remove MineColonies from a test instance. The mod loads, Trim Trays and Curing Racks work as before, and no Bud Barn item exists.
