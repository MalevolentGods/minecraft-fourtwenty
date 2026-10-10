# Remaining compatibility acceptance

These checks are open gates, not completed results. Use the frozen versions, final JAR hashes, original copied settings, disposable worlds, and commands in [DEVELOPMENT.md](DEVELOPMENT.md). Capture the baseline without FourTwenty before attributing a failure to it. Every result needs an observer, date, command/profile, checkpoint, exact expected/observed behavior, and evidence path. Any FourTwenty-caused crash, permission bypass, loss, duplication, or corruption blocks qualification.

## Environment and build gates

- Generate and verify the Linux dependency lock on a real Linux Java 21 worker. Run the build, unit tests, and native suites there. The available Docker CLI had no running daemon during this session.
- Accept the EULA explicitly before ordinary dedicated-server testing. Use the packaged release JARs; no source substitution. Record client-only server exclusions by exact mod ID, filename, hash, and reason. Do not remove an ordinary server mod merely to hide a failure.
- Complete standalone, minimal colony, and full-pack client/dedicated-server startup and core gameplay. Automated GameTest startup is separate evidence.
- Confirm selected resource packs render correctly. The client process reached startup, but the UI automation provider could not attach to its unbundled Java window; no visual observation follows from that log.
- Resolve or record each original-pack warning in the individual warning ledger. Recipe/data failures already present without FourTwenty remain affected-scenario limitations.

## Farming and protection

- In a real colony, assign a field to a farmer, deliver cannabis seeds through the normal request/courier system, and observe planting, natural or bone-meal maturation, native harvesting, collection, and replanting for at least three cycles without manually replenishing seeds. Record crop counts and seed/bud inventory before and after each cycle. The native planting/loot contract fixture does not establish navigation, field assignment, or courier operation.
- Exercise manual crop breaking at every growth stage, empty hand and hoe clicks, both hands, creative mode, repeated clicks, and disabled block drops. Native loot/replant/drop tests cover part of this matrix; confirm the player input paths.
- Repeat with Actually Harvest enabled/disabled, replanting enabled/disabled, fake-player and area-tool paths, configured XP, and the installed Vanilla Plus Tools. Check one harvest per block, guaranteed break seed retention, no diamonds, and no duplicate harvests.
- Confirm visitor, resident, and operator behavior follows the colony's settings for harvesting, breaking, and bong menus. Cross a colony boundary with area harvesting. The native regression now covers this conflict, but verify it in the real connected-player setup too.

## Bong automation and persistence

- Use two connected viewers, shift-click, full/partial/simulated transfers, every face, hoppers, and at least one actual pack transfer device (Sophisticated Storage, Storage Racks, Oritech, or Immersive Machinery as supported). Confirm filters, capacities, rejected inputs, and all remainders.
- Extract queued inputs while burning; remove and reinsert water; break before ignition, mid-burn, and after expiry. Count every item and distinguish the already-paid active charge from queued inventory. Recover partial invalid/oversized legacy stacks through automation and menus.
- Move empty, stocked, and burning bongs with actual Multi-Piston redstone activation in every direction, reverse repeatedly, block destinations, move with two menus open, and reconnect afterward. Old menus/handlers must not change destination contents. Native fixtures currently call the real movement transaction directly.
- Repeat movement followed by ordinary server restart. Save/reload a legacy lit bong and confirm only its outstanding bud is charged. Confirm no duplicate source entity/effects and no lost or refunded active charge.
- Unload/reload chunks with the full pack's **original** Better Chunk Loading/Limited Chunks settings and connected players. Native disk checkpoints prove serialization/restart in the fixture environment, not these original-setting behaviors. Check orderly shutdown, save scheduling, memory retention, and no offline catch-up.

## Player lifecycle

- Two real players independently consume cookies/brownies. Attempt duplicate use under latency and verify food, inventory, delay, and client resynchronization. Switch integrated-server worlds within one client process using the same account.
- Test logout/rejoin, final death/respawn, PlayerRevive downing/revival/final death, Corpse recovery, Wormhole travel, sleeping/Comforts, Better Days, and `/time` changes. Do not conflate fixture players or direct event invocation with network gameplay.
- Confirm stronger vanilla effects remain stronger, unrelated effects remain present, milk removes active effects, and bong/joint/pipe/edible effects cannot bypass PlayerRevive's downed movement or action restrictions. Native testing found no automatic revival from edible regeneration; broader player-state interactions still need observation.

## Colony and pack gameplay

- Confirm citizens never choose cookies/brownies as food while storage, requests, couriers, and allowed transport work. Native predicate and ordinary-container tests do not prove the complete logistics workflow.
- Run Kingdom's Gate recruitment, ordinary-book use, capacity, happiness, ritual scheduling, and return to work interactively. Its nine native GameTests already pass alongside FourTwenty.
- Scan a mixed build using Structurize, rotate/mirror it, place it, and let a builder construct it. Verify crop and bong state, inventory rules, and coexistence with Domum Ornamentum/StyleColonies. Test hut screens, BlockUI, JadeColonies, and TownTalk audio.
- Confirm food values and tracking with Farmer's Delight, Vanilla Cookbook, AppleSkin, and Spice of Life: Carrot. Check JEI discovery, Polymorph selection, Recipe Essentials and Client Crafting server-authoritative outputs. Existing recipe IDs and inputs remain unchanged; do not add unrelated processing integrations.
- Generate a fresh normal world with Tectonic/Lithostitched/Biome Size/Overgrowth and the structure/loot mods. Then generate new chunks in a named existing disposable world. Verify wild-weed biome modifiers, datapack reload, Global Packs, and an overriding loot/tag datapack. There is no retroactive generation feature.
- Inspect Jade/JourneyMap information, crop/bong models and GUI, particles, sound, Entity Culling, and resource reload. Record any rendering/audio failures with selected pack versions and resource packs.

## Performance and final release gate

Use Spark with matched baseline/FourTwenty worlds, seed, JVM arguments, settings, player count, loaded chunks, and duration. Measure idle, representative stocked/active bongs, and a practical stress case. Record median and p95 tick time, loaded chunks, heap/retained memory, GC behavior, and observations after unloading/reloading. Keep profiling data local unless its upload is separately authorized. Investigate sustained differences rather than treating a short native fixture runtime as a benchmark. FourTwenty production code adds no forced chunk tickets.

Repeat the final core smoke matrix from packaged JARs after any code or resource change. Verify the frozen reference hashes again, retain logs and reports, update each row of the inventory, and replace every open gate with evidence or a specific scoped limitation. The target remains 134 reference JARs plus the fresh sibling build and FourTwenty, excluding fixture infrastructure. Do not publish a release, replace installed mods, or migrate a player world as part of this work.
