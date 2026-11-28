# Contract: Orders (client → server) and the Orders view

## `fourtwenty:tie_dye_order` (optional channel; sent only with `hasChannel`)

| Field | Type | Notes |
|---|---|---|
| `colony` | varint | colony id |
| `building` | block pos | the Shack |
| `action` | enum | `ADD` or `CANCEL` |
| `pattern` | varint | 0-7 (ADD) |
| `colourway` | string | colourway id (ADD) |
| `kind` | enum | `HEMP`, `RUG`, `COUCH`, `BED`, `SHIRT` (ADD); BED also names its wood |
| `count` | varint | 1-256 (ADD) |
| `order` | varint | order id (CANCEL) |

**Server checks, in order** (each refusal is one translatable chat line, nothing changes):

1. The colony exists in the player's dimension and grants them `MANAGE_HUTS` (`tiedye.fourtwenty.refused.permission`).
2. A Tie Dye Shack stands at `building` (`…refused.no_shack`).
3. ADD: the pattern's Textiles study is done (`…refused.pattern`); every colour of the colourway is researched (`…refused.colourway`); the kind is known and the count in range.
4. CANCEL: the order exists and its current batch has no supplies in it (`…refused.started`).

## The Orders view (MineColonies module view sync)

Sent only when it changes: the Shack's level; the unlocked patterns; for each colourway its id, colour codes and whether the colony can make it; the queue (id, pattern, colourway, kind, count, made, waiting supplies).

Colour names are never sent; the client names a colour only if its viewer caught it (research R6).
