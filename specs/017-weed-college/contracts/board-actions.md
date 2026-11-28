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

1. The colony exists in the player's dimension, and the player has MineColonies' `MANAGE_HUTS` permission in it (`college.fourtwenty.refused.permission`).
2. A Weed College stands at `building` in that colony (`college.fourtwenty.refused.no_college`).
3. `QUEUE`: the study is AVAILABLE (`StudyBoard.available`), not done, and not already queued. It is appended; if the queue was empty, it becomes the study in progress.
4. `REMOVE`: the study is queued and not the head, or it is the head with 0 units done and no supplies delivered, even if its cost has been fixed (so a study queued by mistake can be taken back). Its fixed cost is cleared and the Colleges' open supply requests are cancelled. Removing a head that work or supplies have gone into is refused ("already under way").

On success, the colony record is marked dirty and every College in the colony re-sends its board view.

## Server → client: the board view (MineColonies module view sync)

`CollegeBoardModule.serializeToView` writes the following. It is re-sent only when it changes (as with `BudBarnStatusModule.refresh`).

| Field | Notes |
|---|---|
| `level` | College level |
| `queue` | study ids, head first |
| `unitTicks` | the server's ticks per unit, so the client can show a study's length in hours |
| `head` | units done on the head; `headStarted` (its cost is fixed) and `headUnderWay` (work or supplies have gone into it, so it can't come off) |
| per study | its definition (id, branch, tier, parents, kind, icon, colour codes, needed feature), `state` (`LOCKED`, `AVAILABLE`, `QUEUED`, `IN_PROGRESS`, `DONE`), `lock` (reason and argument), `cost` (supplies after the caught-colour discount; the fixed cost for a started head; empty once done) |

Definitions travel with the view (about 10 KB): the server's data pack is what its players see, and there is no client-side copy to fall out of step. Names and descriptions come from the client's lang file by study id; an id with no entry shows `?`. The missing supplies and whether the Weedologist is working are on the Status tab.

**Colour privacy (research R10)**: the client hides a colour's name and swatch unless the viewing player has discovered it. The server sends codes, never names.

## Status view

`CollegeStatusModule.Status`: level, worker names, state (`NO_WORKER`, `IDLE` (empty queue), `WAITING`, `WORKING`, `PAUSED` (with reason)), head study, units done and units, minutes left at the current pace, studies finished, bad batches.
