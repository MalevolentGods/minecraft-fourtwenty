# Contract: Board actions and board view

## Client → server: `fourtwenty:college_board`

Sent only if the client's connection has the channel (`hasChannel`).

| Field | Type | Notes |
|---|---|---|
| `colony` | varint | colony id |
| `building` | block pos | the College's hut position |
| `action` | enum | `QUEUE` or `REMOVE` |
| `study` | resource location | a study id |

**Server checks, in order** (a failed check does nothing and sends the player a short, translatable chat line):

1. The colony exists, and the player has MineColonies' `MANAGE_HUTS` permission in it.
2. A Weed College stands at `building` in that colony.
3. `QUEUE`: the study is AVAILABLE (`StudyBoard.available`), not done, and not already queued. It is appended; if the queue was empty, it becomes the study in progress.
4. `REMOVE`: the study is queued and not the head, or it is the head with 0 units done and no supplies delivered. Removing a head that has started is refused ("already under way"). Supplies already delivered stay in the hut for the next study, or go back to the warehouse at the next dump.

On success, the colony record is marked dirty and every College in the colony re-sends its board view.

## Server → client: the board view (MineColonies module view sync)

`CollegeBoardModule.serializeToView` writes the following. It is re-sent only when it changes (as with `BudBarnStatusModule.refresh`).

| Field | Notes |
|---|---|
| `level` | College level |
| `queue` | study ids, head first |
| `head` | `{study, units done, units, waiting: list of missing supplies (item, count), working: bool}` |
| per study | `state` (`LOCKED`, `AVAILABLE`, `QUEUED`, `IN_PROGRESS`, `DONE`), `lock` (reason key and argument), `cost` (supplies after the caught-colour discount) |

Definitions (names, parents, tiers, effects) are not sent: the client reads the same study data from the synced data pack (the loader also runs on the client, as the Handbook's scouting data does). If a client's data doesn't match the server's, it shows `?` for unknown ids rather than failing.

**Colour privacy (research R10)**: the client hides a colour's name and swatch unless the viewing player has discovered it. The server sends codes, never names.

## Status view

`CollegeStatusModule.Status`: level, worker names, state (`NO_WORKER`, `IDLE` (empty queue), `WAITING`, `WORKING`, `PAUSED` (with reason)), head study, units done and units, minutes left at the current pace, studies finished, bad batches.
