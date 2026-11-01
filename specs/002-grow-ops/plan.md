# Implementation Plan: Grow Ops, the Think Tank and the Blaze Tier

**Branch**: `002-grow-ops` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

## Summary

Add the Blaze tier, five plots that sit under a crop like farmland and speed it up, and a Think Tank that spends plot time on studies.

## Technical Context

**Language/Version**: Java 21, NeoForge 21.1.218, Minecraft 1.21.1; MineColonies optional
**Storage**: Block entity NBT; research rewards go in the player's existing Infuser state
**Testing**: JUnit, game tests, in game on the local test server
**Constraints**: Builds on 0.15.0 (PR #31), which isn't merged

## Constitution Check

| Principle | Status |
|---|---|
| I. MineColonies optional | Pass. Nothing here touches it. |
| II. Existing machines keep their game | Pass. Researching a colour only adds a route to `caught`; the Terpinator and Infuser don't change. |
| III. The power rule | Pass. Plots and the Think Tank use `MachinePower`. |
| IV. Failure is safe | Pass. A plot with no power gives no bonus and never hurts the crop. |
| V. Approved names | Pass. All names are in the plan. |
| VI. Rules pure and tested | Pass. `GrowRules`, `Studies`. |
| VII. Milestones in game | The user waived per-stage in-game checks; they stay listed as not seen. |

## Design

- **Plots**: `GrowOpBlock` extends `BIBlock`, and says it sustains crops and is fertile. Its block entity gives the crop above extra random ticks: a chance per tick of (speed - 1) × randomTickSpeed / 4096, so speed matches however fast the world's random ticks are. The block itself gives off light from tier 2.
- **Harvest**: `BlockDropsEvent` for breaking; `WeedCropBlock.useWithoutItem` for right-click.
- **Think Tank**: a machine with three input slots. Inputs are taken when a study starts. Units gain plot speed / 1200 per tick; each unit may be a bad batch and is worked again.
- **Studies**: one table in `domain/Studies`, one line per study. FR-015's "without new code" means a line of data, not a datapack, because a menu needs the list on the client.
- **Blaze**: a fourth `BIRules.Tier`, so every rule follows by itself.

## Structure

```text
domain/      GrowRules, Studies
power/       GrowOpBlock, GrowOpBlockEntity, GrowOps (drops)
processing/  ThinkTankBlock, ThinkTankBlockEntity, ThinkTankMenu
client/      ThinkTankScreen
tools/       generate_power.py, generate_hookah_model.py, generate_tech_ui.py
```

## Complexity Tracking

No constitution violations.
