# Implementation Plan: Bud Barn

**Branch**: `004-bud-barn` | **Spec**: [spec.md](spec.md) | **Research**: [research.md](research.md) | **Data model**: [data-model.md](data-model.md) | **Quickstart**: [quickstart.md](quickstart.md)

## Technical Context

Java 21, NeoForge 21.1.218, Minecraft 1.21.1, MineColonies 1.1.1343 as an optional compile dependency. All Bud Barn code lives under `compat/minecolonies`, registered from `ColonyIntegration` only when MineColonies is loaded. JUnit in `src/test`, game tests in `src/gametest`. Blueprints from a new `sensi-heights` generator, `tools/design_budbarn.py`, imported with `tools/import_sensi_heights.py`.

## Constitution Check

- **Nothing needs MineColonies:** yes. The hut, job, AI, status module and config sit behind `ColonyIntegration`. The one change outside it, a shared trim helper on the Trim Tray, is plain mod code that players already use.
- **Terpinator and Infuser untouched:** yes.
- **Machines on the shared power rule:** not applicable; the hut is unpowered (FR-008), like the Trim Tray and Curing Rack it uses.
- **Approved names:** Bud Barn (in the plan) and Trim Daddy (approved 2026-11-03).
- **Pure rules in `domain/`, numbers in config:** yes. `compat/minecolonies/domain/BudBarnRules` decides the worker's next step and the per-level numbers, with JUnit tests; the numbers are in `DispensaryConfig`'s server config.
- **Payloads only to connections with the channel:** the status view uses MineColonies' own building-module sync, so no new payload.
- **Client and server on the same jar:** a new hut block and job are new registry entries, so yes, as for any release.

Gates pass; nothing needs justifying.

## Key decisions

(Reasons and alternatives in [research.md](research.md).)

- **A worker AI of its own, not a crafter.** The Budtender is a crafter, but curing takes minutes on a rack, which doesn't fit a recipe. `TrimDaddyAI` extends MineColonies' basic worker AI with a small state machine: collect cured buds, hang trimmed buds, trim rough buds, fetch rough buds, request more. A pure `BudBarnRules.next(...)` picks the step, so the order is tested without a colony.
- **Trays and racks are found in the hut,** by scanning the building's corners as `BongTending` does for Hookahs, rescanned every 10 seconds.
- **Trimming goes through the tray.** `TrimTrayBlock.trim(level, pos, rough)` takes the trim logic out of the click handler and returns the trimmed bud and any seed. The player's click and the Trim Daddy both call it, so seed chances, strain and traits can't drift apart (FR-004, FR-005).
- **Curing goes through the rack** with its existing `hang` and `take(cured)`. `take(true)` only ever takes finished buds, which keeps a player's uncured buds safe (FR-012).
- **Rough buds by tag request.** A new item tag `fourtwenty:rough_buds` lets the hut ask the colony for rough buds of any strain (FR-003). The hut keeps only rough buds in its storage; cured buds and seeds are left for couriers to take to the warehouse, where a Dispensary's requests find them (FR-007).
- **Levels:** trays and racks per level come from the blueprint, never assumed. Worker speed is a config value per level, a trim taking 40 ticks at level 1 down to 10 at level 5. Level 5 has four times level 1's rack space (SC-004).
- **Status** is a building module like `DispensaryStatusModule`: state, buds hanging, next finish, and cured buds and seeds made (FR-011).
- **Blueprints** use one plan grown through five levels, like the animal huts: a curing barn with racks along the walls and trim benches by the door, in agriculture earth tones with no quartz and no lime. They're saved in `agriculture/horticulture/budbarn1-5`.

## Build order

1. **Shared trimming.** `TrimTrayBlock.trim` with a game test proving a player and the helper give the same results; the `rough_buds` tag.
2. **Rules.** `BudBarnRules` (next step, trimmed-bud buffer, per-level speed) with JUnit tests; config values.
3. **Hut and job.** `BudBarnBlock`, `BudBarnBuilding` (worker and status modules, kept items), `TrimDaddyJob`, registry entries, lang, a hut block model and item.
4. **The AI.** `TrimDaddyAI`: scan, collect, hang, trim, fetch, request, dump. Then a colony test on the local server: rough buds in, cured buds and seeds out.
5. **Status window** and its strings.
6. **Blueprints.** `design_budbarn.py`, `check_hut_levels.py`, added to the showcase as a new column, then imported into the mod.
7. **Docs:** README, CHANGELOG, `BUILDINGS.md`, and the plan repo.

## Risks

- **Dispensary requests and strains:** the Budtender asks for a plain cured bud. If MineColonies' request matching compares data components, bred-strain cured buds may not fill it. Checked in step 4; the fix, if needed, is a tag request on the Dispensary side as well (research R5).
- **Racks the hut didn't place:** a player's rack inside the hut counts as the hut's. That's accepted, and the worker only takes finished buds from it.
