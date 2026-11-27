# Contract: What other code reads from the College

`compat/minecolonies/college/CollegeResearch`. These are server-side only, they return safe defaults when MineColonies is absent, and the callers never import MineColonies types.

| Call | Returns | Used by |
|---|---|---|
| `knowsColour(Level, BlockPos, String terpeneCode)` | whether the colony at `pos` has researched that colour | the Tie Dye Shack (spec 018): it may dye only with colours its colony knows |
| `knownColours(Level, BlockPos)` | the colony's known terpene codes | spec 018's colour picker |
| `done(Level, BlockPos, ResourceLocation study)` | whether that colony finished the study | gating checks; game tests |
| `factor(Level, BlockPos, Effect effect)` | the colony's multiplier for `effect` at `pos` (1.0 outside a colony, or with nothing studied) | the shared power and processing rules (research R7) |

## Effects

The `ColonyEffects.Effect` enum's defaults are in config.

| Effect | Applied in | Default after studies |
|---|---|---|
| `TERPINATOR_BI` | `MachinePowerRule` (Terpinator only) | 0.5 Lean Extraction, 0.25 Full Spectrum, 0 Master Blend |
| `GROW_OP_BI` | `MachinePowerRule` (Grow Ops) | 0.5 |
| `HOOKAH_BURN` | `BIRules` burn length | 1.25 Long Burn, 2.0 Boundless Grid |
| `STASH_CAPACITY` | Stash capacity | 1.5 |
| `TRIM_CURE_SPEED` | trimmer and curer work time (÷) | 1.25 Bench Habits, 1.5 Assembly Line |
| `PRESS_SPEED` | Rosin Rig and Pheno Chamber work time (÷) | 1.25 |
| `FARM_BONUS_BUD` | the weed crop loot modifier, for colony citizens only | chance 0.25, then 0.5, then 1.0 |

**Rules:**
- A later study in the same effect line replaces an earlier one; they don't stack.
- No effect may touch the Terpinator's or the Infuser's speed, recipes or outputs (Principle II).

## MineColonies research effects (unlock studies, research R6)

When an Armory or Glassworks study finishes, the colony gets the MineColonies research effect `fourtwenty:effects/<study path>` at strength 1. It is re-applied from `CollegeSavedData` whenever the colony loads.

The crafter recipes that use it:

| Crafter | Recipes (`data/minecolonies/crafterrecipes/<crafter>/`) | `research-id` |
|---|---|---|
| blacksmith | dab_cell, hotbox_helm, moon_boots, couchlock_greaves, puff_plate, blazer | `fourtwenty:effects/arm_<piece>` |
| glassblower | glass_bong, and the six specialty bongs | `fourtwenty:effects/gla_<bong>` |

Players' own crafting-table recipes are untouched (Principle I).

## Feature flags (`needs`)

`CollegeFeatures.present(id)` checks whether the feature's registry entry exists:

- `tie_dye`: the Tie Dye Shack's building entry.
- `weedscout_uniform`: the uniform item.

A study with a missing feature is LOCKED, with the reason `Waiting for <feature name>`.
