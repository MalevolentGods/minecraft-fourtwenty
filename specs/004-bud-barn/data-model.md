# Data model: Bud Barn

## Bud Barn building (`BudBarnBuilding`)

| Field | Meaning | Saved |
|---|---|---|
| level | 1-5, from MineColonies | by MineColonies |
| trays | Trim Tray positions found in the hut's corners | no: rescanned |
| racks | Curing Rack positions found in the hut's corners | no: rescanned |
| made | cured buds taken down and seeds trimmed, lifetime counts for the status window | yes |

**Kept items** (`getRequiredItemsAndAmount`): rough buds up to 16 per tray, and trimmed buds up to free rack space plus the buffer. Everything else goes to the warehouse.

## Trim Daddy (`TrimDaddyJob`, `TrimDaddyAI`)

One worker. It carries rough, trimmed and cured buds and seeds in its own inventory.

### Work states and how the next one is chosen (`BudBarnRules.next`)

Inputs: cured buds on racks (any finished), trimmed buds held or in the hut, free rack spots, rough buds held or in the hut, trays present, racks present.

| Order | State | Taken when |
|---|---|---|
| 1 | `COLLECT` | a rack has a finished bud |
| 2 | `HANG` | trimmed buds are available and a rack has a free spot |
| 3 | `TRIM` | rough buds are available, a tray exists, and trimmed buds held are below free rack spots + buffer |
| 4 | `FETCH` | rough buds are in the hut's storage but not in the worker's inventory |
| 5 | `REQUEST` | no rough buds anywhere in the hut: ask the colony by tag |
| — | `IDLE` | nothing to do; the status says why |

Collecting comes first, so racks free up before more buds are trimmed. Trimming stops at free spots plus the buffer (FR-010).

### Status (`BudBarnStatus`, shown by the status module)

`NO_WORKER`, `NO_TRAYS`, `NO_RACKS`, `WAITING_FOR_ROUGH`, `TRIMMING`, `CURING` (with the number hanging and minutes to the next finish), `RACKS_FULL`.

## Items and blocks used (existing)

- **Rough, trimmed and cured buds, and seeds:** keep `fourtwenty:strain` and `fourtwenty:traits` through every step (`ModDataComponents.bredFrom`).
- **Trim Tray:** new static `trim(level, pos, rough)` → trimmed bud plus an optional seed.
- **Curing Rack:** `hang(bud)`, `take(true)` (finished buds only), `minutesLeft()`.

## Config (server, beside the Dispensary settings)

| Key | Default | Meaning |
|---|---|---|
| `budBarnTrimTicks1`..`5` | 40, 30, 20, 15, 10 | ticks per trim, by level |
| `budBarnTrimmedBuffer` | 8 | trimmed buds held beyond free rack spots |
| `budBarnRoughPerTray` | 16 | rough buds kept and requested per tray |
