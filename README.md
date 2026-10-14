# FourTwenty 🌿

Grow wild cannabis into a farm, craft joints and edibles, and build a bong for shared effects around your base. FourTwenty uses vanilla farming, crafting, inventory, and potion mechanics, and works without MineColonies or any other optional mod.

This compatibility candidate targets **Minecraft 1.21.1, NeoForge 21.1.218, and Java 21**. Its declared NeoForge range is `[21.1.218,21.2)`; qualification currently targets **21.1.218 specifically**. The [compatibility report](docs/COMPATIBILITY.md) distinguishes automated results from the remaining interactive and multiplayer checks. Full modpack qualification is not yet complete.

## Grow and harvest

Find wild weed in Plains, Savanna, Grove, and Meadow biomes, collect its seeds, and plant them on farmland. Cannabis grows through eight stages and responds to normal crop conditions and bone meal.

Breaking an immature crop returns one seed. A mature crop drops one seed and 1–3 buds, with a 5% chance of 1–4 extra seeds. Right-clicking a mature crop resets it to age zero and retains one seed for replanting; the remaining harvest drops normally. Right-clicking an immature crop does nothing.

MineColonies farmers recognize the crop and its seeds through their native farming support. Actually Harvest uses its configured harvesting, replanting, tool-area, and XP behavior. With both installed, FourTwenty checks colony permissions for every cannabis crop in an area harvest. Datapacks can override the loot and additive tags.

## Craft and consume

Craft joints, pipes, cookies, brownies, and bongs with the existing recipes. Cookies take 200 server ticks to start their effects; brownies take 300. At 20 ticks per second, that is 10 and 15 seconds. Another edible cannot be consumed while a dose is pending.

Pending effects survive dimension travel, but cancel on logout, actual death, or server shutdown. Sleep and changes to the daylight clock do not advance the delay. Effects already applied follow vanilla effect merging and milk removal. MineColonies citizens exclude the cookies and brownies from food selection; normal inventory storage and transport remain available.

## Use a bong

Right-click the bong to open its inventory:

| Slot | Ingredient | Capacity |
| --- | --- | ---: |
| Buds | Weed Bud | 4 |
| Water | Water Bucket | 1 |
| Fuel | Blaze Powder | 64 |

A bong automatically consumes **one bud and one blaze powder when a burn starts**. The water bucket remains in its slot. Each charge burns for **1,200 server ticks** (60 seconds at 20 TPS) and applies the existing player effects every 20 ticks within four blocks. Queued ingredients remain extractable. Removing the water extinguishes the paid charge without a refund.

Hoppers and standard NeoForge item-handler automation can access all three slots from every face. Multi-Piston can move stocked and burning bongs while preserving their inventory and paid charge. Vanilla piston behavior is unchanged. Breaking the bong drops the block and its unconsumed inventory once; the active charge has already been spent.

## Install and upgrade

Install the same FourTwenty build on the server and every client. This work retains the version **0.3.2**, so compare the JAR's SHA-256 as well as its filename when replacing an older build with that version. MineColonies and the other pack mods are optional, with exact tested versions recorded in the [frozen inventory](validation/compatibility-lock.json).

Back up a world before upgrading. Registry IDs and inventory slot numbers are preserved. Existing lit bongs receive an additive `ChargePaid` marker: their remaining bud is consumed once on the first server tick, their already-paid blaze powder is not charged again, and elapsed burn time is retained. Invalid legacy contents remain recoverable through extraction. An invalid active legacy burn stops. Unloaded bongs pause instead of catching up offline.

For downgrade recovery, restore the backup together with its matching mod builds. Older versions do not understand the paid-charge state and should not open an upgraded save. This implementation does not modify an installed modpack or migrate any player world automatically.

## Build and test

With Java 21 and Python 3.11 or newer:

```sh
make build
make test-python
make help
```

The JAR is `build/libs/fourtwenty-0.3.2.jar`. See [development and isolated runs](docs/DEVELOPMENT.md) for the pinned sibling build, GameTests, preparation, dependency verification, launch commands, and evidence capture. Report issues with exact mod versions, JAR hashes, reproduction steps, and relevant logs.
