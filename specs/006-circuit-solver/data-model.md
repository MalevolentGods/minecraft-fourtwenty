# Data Model: Circuit Solver

Nothing new is saved. Every circuit is rebuilt from the world and solved again, so saves keep only what they hold today: Ws in each buffer, under the `ws` keys from 006-willards.

## Pure model (`domain/`)

### CircuitRules
The tunable numbers, as defaults (overridden from `ProcessingConfig`):
- `volts(Tier)`: 4.2, 42, 420, 4,200 Kb.
- `hookahWatts(Tier)`: 420, 840, 1,680, 3,360 W (from `BIRules.Tier.rate()`).
- `baseAmps(Tier) = W / V`, `baseOhms(Tier) = V² / W`.
- **Sources:** `sourceDroop` 0.005 pu, `sourceLimit` 1.05 pu.
- **Stashes:** `stashEmfLow` 0.96, `stashEmfHigh` 0.99, `stashResistance` 0.01 pu, `stashLimit` 1.0 pu.
- **Loads:** `loadMinVolts` 0.5 pu, `loadHysteresis` 0.10.
- **Wicks:** `wickOhmsPerBlock` 1.5e-4 pu, `wickAmpRating` 2.0 pu, `wickVoltRating` 1.5 pu, `burnTicks` 20.
- **Overload and the Hit Box:** `overloadMargin` 1.5, `hitBoxLoss` 0.02.
- `ratedDraw(cost per tick) = cost × 20` W.

### Circuit (the solver)
Input, built fresh for each solve:

| Element | Fields | Notes |
|---|---|---|
| Node | index | ground is implicit |
| Conductor | a, b, ohms | a collapsed Wick run, or one Wick |
| Source | node, emf, ohms, maxOut, maxIn | Hookah and Hit Box output: maxIn = 0. Stash: both. |
| Load | node, watts, minVolts | constant power, which scales with brownout |

Output, `Solution`:
- `volts[node]`.
- `amps[conductor]`, signed a→b.
- `amps[source]`, positive out; `watts[source]`.
- `watts[load]`, after brownout; `k`, the brownout factor (1 when healthy).
- `losses` W; `converged`, a boolean.

Rules: Kirchhoff's current law holds at every node to 1e-9 relative. Every conductor obeys `I = ΔV / R`. Source clamps are honoured.

### Units
Formats a value and a base unit (W, Ws, Ig, Kb, Φ) with SI prefixes (n, µ, m, k, M, G) to three significant figures.

## World model (`power/`)

### BINode (extended)
What each BI block exposes to the circuit, per face:
- `tier()`, `stored()`, `capacity()`, `receive`, `extract`: as now.
- `role()`: `SOURCE`, `BATTERY`, `LOAD` or `NONE` (for the face that doesn't connect).
- `demandWatts()`: what a load asks for now (0 = not asking).
- `sourceReady()`: whether a source has Ws to give.
- `fill()`: as now; it drives a Stash's EMF.

| Block | Role | Demand / limit |
|---|---|---|
| Hookah, Double Hose, Big Bertha, Grand Hookah | SOURCE | limit: its tier current |
| Stash Jar, Box, Mother Stash, The Vault | BATTERY | ±its tier current |
| Hit Box, input face | LOAD | its input tier's rate, while its bank has room and the output has demand |
| Hit Box, output face | SOURCE | its output tier's current |
| Trim and cure machines, Pheno Chambers, Rosin Rig, Think Tank, Terpinator, Grow Op plots | LOAD | rated draw while the buffer has room (with hysteresis) |
| Construction benches, Re-Up Stations | LOAD | their charge rate while the intake has room |

### Network
- `members`: positions and faces of the devices on it.
- `wicks`: the Wick positions on it, and the runs they collapse into.
- `version`: the BIGrid version it was walked at.
- `signature`: a hash of each member's discrete state.
- `solution`: the last `Solution`.
- `nominalVolts`: the highest source tier present, or 0.
- `burnTimers`: Wick position → ticks over its rating.
- `carry`: fractional Ws per member.

State: `DIRTY` → solve → `SOLVED`. A changed signature, a changed version, a 100-tick rewalk, or a source running dry makes it `DIRTY` again.

### Circuits (per level)
- `Map<BlockPos, Network>`: a cache from any member or Wick to its network. It is rebuilt on a version change.
- `tick(level, pos)`: runs that position's network once per game tick.
- `reading(level, pos, face)`: a `Reading` for the meter and screens.

### Reading
`volts`, `amps`, `ohms`, `watts`, and for a Wick its `ampRating`, `voltRating` and the fraction of its rating in use.

## Items

- **Inspoweedometer** (`fourtwenty:inspoweedometer`): stacks to 1. It has no data component. Using it on a block prints a reading.
