# Implementation Plan: The Tie Dye Shack

**Branch**: `018-tie-dye-shack` | **Date**: 2026-11-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/018-tie-dye-shack/spec.md`

## Summary

A new MineColonies hut, the Tie Dye Shack (`fourtwenty:tiedyeshack`), worked by the Tie Dyer, built like the Weed College: building, worker AI, status and Orders modules, a hut-block diorama, five scripted levels.

- **Orders:** players order a pattern in a colourway on an item kind in the Shack's window. The Tie Dyer fetches plain items and dye supplies, dips at the vats, hangs at the racks, and delivers to the warehouse.
- **Patterns** come from the College's 8 Textiles studies; **colourways** (16 curated palettes) are orderable once the colony has researched all their colours.
- **The look as data:** one block per kind (hemp, rug, couch, a bed per wood). The pattern is in the block state; the colourway is in a block entity and item component. It is drawn as 4 tinted slot masks per pattern (research R1).
- **Shirts:** handed to colonists, drawn by a render layer.
- **A player route** (the Dye Vat, research R9), pending the user.

Ships with the Weed College (017) in one release.

## Technical Context

**Language/Version**: Java 21 (Minecraft 1.21.1, NeoForge 21.1.218); Python 3.12 for tools.

**Primary Dependencies**:
- MineColonies 1.1.1343 (optional, compile-only), with BlockUI.
- No new dependency.

**Storage**:
- Colourways: `data/fourtwenty/tie_dye/colourways.json`.
- Orders and shirts: `TieDyeSavedData`, keyed by colony.
- Looks: block state, block entity and item component.

**Testing**:
- JUnit (`domain/tiedye/`): colourway availability, slot mapping, order queue and cancel rules, batch sizes and supply costs.
- Colony game tests: order permission and checks; the Tie Dyer's cycle; delivery; persistence; shirt give, swap and take.
- Core game tests: the component survives place, mine and re-place; rugs join only the same look; Four Twenty without MineColonies.

**Target Platform**: client and dedicated server; local test server.

**Project Type**: Minecraft mod, Python tools, Sensi Heights blueprints.

**Performance Goals**:
- Tie-dyed blocks mesh like any tinted block: no block-entity renderers, nothing ticking.
- The Shack does no more per colony tick than the College.

**Constraints**:
- MineColonies optional. The tie-dye blocks, items, component and colourway data are core, and load without it (FR-020). The Shack, the Tie Dyer, orders and shirts on colonists live under `compat/minecolonies/tiedye`.
- Payloads only with `hasChannel`.
- Nothing calls the Infuser or `TerpeneDiscovery` (FR-007).

**Scale/Scope**:
- 1 hut (5 levels), 1 job, 8 patterns, 16 colourways.
- About 13 blocks (hemp, rug, couch, 10 beds), 1 shirt item, the Dye Vat if approved.
- 2 payloads; 1 Handbook page.

## Constitution Check

| Principle | Status | How |
|---|---|---|
| I. MineColonies Stays Optional | **Needs the user** | The look, blocks and items are core. But the spec makes the Shack the only way to tie-dye, which Principle I forbids ("colonists MUST NOT be the only way"). The plan proposes the **Dye Vat** (R9): a player dyes with terpene vials of their own caught colours, all 8 patterns open. Alternative: the user grants an exception. |
| II. Existing Machines Keep Their Game | PASS | The Terpinator and Infuser are untouched; the Dye Vat only consumes vials. |
| III. The Power Rule | PASS | The Shack draws no BI. A Dye Vat, if powered, would use `MachinePower`; proposed unpowered. |
| IV. Failure Is Safe | PASS | Not touched. |
| V. Approved Names | **Gate** | "Tie Dye Shack" and "Tie Dyer" are from the plan. Pattern and colourway names in data-model.md need the user's approval before shipping. |
| VI. Rules Are Pure And Tested | PASS | `domain/tiedye/` with JUnit; costs and batch sizes in config. |
| VII. Milestones In Game | PASS | Five milestones (quickstart.md). |
| Technical: art from generators | PASS | Patterns, shirt masks, diorama and tab icon from `tools/generate_*.py`. |

## Project Structure

### Documentation (this feature)

```text
specs/018-tie-dye-shack/
├── plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (orders.md, tie-dye-data.md)
└── tasks.md            # /speckit.tasks
```

### Source Code

```text
src/main/java/com/malevolentgods/fourtwenty/
├── domain/tiedye/                 # Colourways, TieDyeRules, OrderQueue (pure)
├── tiedye/                        # core: TieDye component, TieDyeBlockEntity, blocks, colour handlers, Colourways loader, (Dye Vat)
└── compat/minecolonies/tiedye/    # TieDyeRegistry, ShackBuilding, TieDyerJob/AI, OrdersModule, StatusModule, TieDyeNetwork,
                                   # TieDyeSavedData, Shirts (+ client: OrdersWindow, ShirtLayer)
src/main/resources/
├── data/fourtwenty/tie_dye/colourways.json
├── assets/fourtwenty/{blockstates,models,textures}/…tie_dye…   # generated
└── assets/fourtwenty/handbook/en_us/entries/colony/tie_dye_shack.md
tools/generate_tie_dye.py, tools/generate_tiedyeshack_hut_model.py, tools/generate_college_studies.py (Textiles)
C:\Users\Ignus\sensi-heights\tools\design_tie_dye_shack.py
```

## Milestones

1. **M1, the look and hemp**:
   - The R1 spike: a colour handler reading the block entity.
   - Pattern masks for a block face; `tie_dyed_hemp`; the component.
   - The Shack, the Tie Dyer, orders (window, payload, saved data); delivery.
   - Textiles studies renamed.
2. **M2, rugs, couches, beds**: their masks and models; rug joining by look.
3. **M3, shirts**: item, give/swap/take, saved data, payload, render layer (citizens and the Lifted body).
4. **M4, all patterns, levels and the hut**: the 8 patterns finished and approved; the levels; the diorama; a showcase row.
5. **M5, the player route and the book**: the Dye Vat (if approved), the Handbook pages, the no-MineColonies check, CHANGELOG.

## Complexity Tracking

| Item | Why | Simpler alternative rejected because |
|---|---|---|
| Pattern in the block state, colourway in a block entity | Rugs, couches and beds already have many states | All in the state means 32,768 states per rug; a block per combination is about 640 per kind |
| 10 bed blocks | A bed's frame is its wood's texture | One bed with the wood as data would need per-wood models anyway |
| The Dye Vat | Principle I | Leaving tie-dye colony-only breaks the constitution unless the user grants an exception |
