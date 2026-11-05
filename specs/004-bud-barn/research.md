# Research: Bud Barn

## R1. Worker AI shape

- **Decision:** `TrimDaddyJob` extends MineColonies' plain job (`AbstractJob`), with `TrimDaddyAI` on its basic worker AI and a state machine of five work states. A pure `BudBarnRules.next(state)` picks the next step.
- **Rationale:** curing is time on a block, not a recipe. The crafter base (`AbstractEntityAICrafting`, used by the Budtender) assumes inputs turn into outputs in one action and would fight the wait. The basic AI still brings eating, sleeping, sickness, dumping and requests.
- **Alternatives:** a crafting module with a "rough → trimmed" recipe plus a custom curing step. That would split the work across two systems, and the cure step would still need custom AI.

## R2. Finding the hut's trays and racks

- **Decision:** scan the building's corners for `TrimTrayBlock` and `CuringRackBlockEntity`, nearest to the hut block first, rescanning every 200 ticks or after an upgrade.
- **Rationale:** `BongTending.scan` already does this for the Dispensary's Hookah. Blueprints don't need tags, and a tray a player adds later is picked up.
- **Alternatives:** blueprint position tags (`work` and similar). These are more precise, but every level's blueprint would have to tag every tray and rack, and they'd break if a player moves one.

## R3. One trimming path for players and the worker

- **Decision:** move the trim logic into `TrimTrayBlock.trim(ServerLevel, BlockPos, ItemStack roughParent)`, returning the trimmed bud and an optional seed, and play the snip sound there. `useItemOn` calls it.
- **Rationale:** FR-005 requires identical seed chances. One code path guarantees it, and a game test pins the player's results.
- **Alternatives:** have the AI copy `TrimRules.seeded` calls itself. Two copies would drift.

## R4. Getting rough buds without caring about strain

- **Decision:** a new item tag, `fourtwenty:rough_buds`, holding `rough_bud`, and requests through `checkIfRequestForTagExistOrCreateAsync`, as the Budtender does for `bong_fuel`.
- **Rationale:** a tag request matches any rough bud whatever its strain component, so a colony of mixed strains can supply it.
- **Alternatives:** requesting `new ItemStack(ROUGH_BUD)`, which may only match unbred buds (see R5).

## R5. Cured buds reaching the Dispensary (open, checked during build)

- **Question:** the Dispensary asks for `new ItemStack(WEED_BUD)`. Does MineColonies fill that from bred-strain buds, which carry a data component?
- **Plan:** in build step 4, put bred cured buds in the warehouse and watch a Budtender's request on the local server. If they don't match, switch the Dispensary's bud requests to a tag request too (`fourtwenty:cured_buds`), the same pattern as R4.

## R6. Output path

- **Decision:** the worker dumps cured buds and seeds into the hut's racks. `getRequiredItemsAndAmount` keeps only rough buds (one batch's worth) and trimmed buds, so couriers carry cured buds and seeds to the warehouse.
- **Rationale:** that's how MineColonies moves any hut's products. The warehouse then fills Dispensary and Farmer requests with no special wiring.

## R7. Per-level numbers

- **Decision:** trays and racks come from the blueprint, as built. Config holds trim ticks per level (40, 30, 20, 15, 10), the trimmed-bud buffer (8), and the rough-bud request size (16 per tray).
- **Planned blueprint counts:** trays 1, 1, 2, 2, 3 and racks 2, 3, 4, 6, 8 for levels 1-5. Rack space rises from 16 to 64 buds, four times as much at level 5 (SC-004).
