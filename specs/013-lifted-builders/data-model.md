# Data Model: Lifted Builders and Higher Ground

## BuilderWatch (memory only, per Builder)

| Field | Meaning |
|---|---|
| colonyId, citizenId | who |
| lastProgress | last progress key seen (iterator position + stage), or none |
| lastProgressTime | game time the progress last changed |
| placements | ring of placement times over the slow window |
| step | rescue step reached for this stall: NONE, FRESH_SPOT, NEAR_BLOCK, OUTSIDE, RECALL |

**Transitions:**
- **Reset to NONE, with times restarted:** any state other than `WORKING` + (`BUILDING_STEP` or `MINE_BLOCK`), or the build moving on.
- **Escalation:** NONE → FRESH_SPOT when slow; then NEAR_BLOCK → OUTSIDE → RECALL, one step per stuck window without progress.

**Pure rule:** `BuilderWatchRules.verdict(samples, now, expectedRate, cfg)` returns `OK | SLOW | STUCK`.

## Lifted (saved, `LiftedSavedData`, per level)

| Field | Meaning |
|---|---|
| colonyId, citizenId | key |
| strain | strain of the joint smoked (sets glow tint, time, speed) |
| ticksLeft | buff time left; counts down only while she is building (simulation ticks) |

**Rules:**
- **Created:** when she smokes for Lifted.
- **Removed:** when `ticksLeft` reaches 0. Whether she lights another joint is decided at that moment.
- **Counting down:** only while she's in a building state, so meals don't burn buff time (spec edge case).
- **Restart:** the record survives; she loads grounded (R7).

**Pure rule:** `LiftedRules.ticks(strain, potency)` and `LiftedRules.speed(strain)`.

## Fly when Lifted (MineColonies building setting)

- **Key:** `fourtwenty:flywhenlifted`, a `BoolSetting`, default `true`.
- **Where:** on every Builder's hut, added at building creation (R5).
- **Saved:** with the building by MineColonies.
- **Old saves:** without the key, they read the default.

## Joint request (MineColonies request, transient)

- **What:** a `Stack` of `fourtwenty:joint`, min 1, count ≤ min(3, spare).
- **When it's open:** only while `SpareJoints.spare(stock, owedToday) > 0`; otherwise it's cancelled.
- **Pure rule:** `SpareJoints.spare(int stock, int owed)` → max(0, stock − owed).

## Flight (memory only, per Builder)

| Field | Meaning |
|---|---|
| waypoints | path from `FlightPlanner` |
| target | hover point or landing spot |
| mode | HOVER, TRAVEL, LANDING |

**Pure rules:**
- `FlightPlanner.plan(start, goal, passable, maxCells)`: a path or none.
- `HoverCover.choose(candidates, nextBlocks, reach)`: the best point.

## Config (`fourtwenty-builders-server.toml`)

See [contracts/config.md](contracts/config.md).
