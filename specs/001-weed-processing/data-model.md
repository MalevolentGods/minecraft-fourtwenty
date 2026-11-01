# Data Model

## Items

| Item | ID | Strain | Notes |
|---|---|---|---|
| Rough Bud | `fourtwenty:rough_bud` | yes | Crop drop |
| Trimmed Bud | `fourtwenty:trimmed_bud` | yes | Trim output |
| Cured Bud | `fourtwenty:weed_bud` | yes | Existing item, display name only changes |
| Ash | `fourtwenty:ash` | no | Burnout drop |

Stage changes: `rough_bud` → trim → `trimmed_bud` (+ seed, by family) → cure → `weed_bud`. Strain never changes.

## Machines

| Field | Type | Notes |
|---|---|---|
| job | TRIM or CURE | Fixed per block |
| tier | 2, 3 or 4 | Fixed per block; sets BI tier |
| inputs | 4 stacks | Rough for TRIM, trimmed for CURE |
| outputs | 6 stacks | Products and seeds |
| progress | int | Kept on pause and through saves |
| buffer | long | BI held for use |

Curing Rack: 8 slots, each with a stack of one bud and `ticks` cured. Trim Tray: no stored data.

## Power

| Node | Tier | Stores | Role |
|---|---|---|---|
| Hookah (`weed_bong`) | Puff | 4k buffer | Source |
| Double Hose | Toke | 16k | Source |
| Big Bertha | Rip | 64k | Source |
| Stash Jar | Puff | 40k | Sink + source |
| Stash Box | Toke | 400k | Sink + source |
| Mother Stash | Rip | 4M | Sink + source |
| Hit Box | Set by placement | small | Transformer |
| Hemp / Rolled / Glass Wick | Puff / Toke / Rip | none | Carries |

## Rules

- `TrimRules.seedChance(family)`; defaults 0.50, 0.25, 0.15, 0.10, 0.05.
- `CureRules`: ticks and batch per tier.
- `BIRules.overloads(partTier, sourceTier)` is true when the source tier is higher.
- `MachinePowerRule.state(highNearby, buffer, cost)` gives RUN_FREE, RUN_PAID or PAUSE.
