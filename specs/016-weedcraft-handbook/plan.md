# Implementation Plan: The Handbook

**Branch**: `016-weedcraft-handbook` | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/016-weedcraft-handbook/spec.md`

## Summary

An in-game book, *A Handbook of Weedcraft, Scouting, and Weed-craft*, built into Four Twenty: its own screen (cover art, DON'T PANIC inside cover, two-page spreads, contents, links, back, bookmark, index, glossary), entries written in a small markup that both the game and a Python PDF builder read, with item pictures and real recipes drawn from the game's data and scene illustrations rendered from real builds. Ranks and Stoner Badges in the Boy Scout Handbook's format are defined as server data; "deeds" fired from the game's existing hooks tick requirements, kept per player and synced to the book, with notices and a reset command. A wearable Stoner Badge Sash draws the wearer's earned patches (Curios slot via a data tag when Curios is present, else the chest slot). A PDF edition is built from the same content for the repo, GitHub releases, README and CurseForge. Six milestones, each tested in game.

## Technical Context

**Language/Version**: Java 21 (Minecraft 1.21.1, NeoForge 21.1.218); Python 3.12 for tools
**Primary Dependencies**: NeoForge only at runtime. Optional: MineColonies (already optional), Curios (no compile dependency: data tag + reflection, research R5). Tools: Pillow (present), reportlab (new, tools-only; research R7)
**Storage**: Book content in the jar's assets (resource-pack data, translatable by language); rank/badge definitions in data (datapack-overridable); player progress in the player's persisted NBT (survives death), like terpene discoveries; bookmark in the client's config folder
**Testing**: JUnit for the pure rules (`domain/`): markup parsing, requirement matching, rank order, progress merge; game tests in `src/gametest` for deeds ticking requirements, persistence and the reset command; colony tests for colony deeds; a content check (Gradle task + Python) that fails the build on dead references, missing icons, missing real-term names and uncovered items; in-game checks per milestone
**Target Platform**: Minecraft client + dedicated server (the user's local test server, flatworld, Hera)
**Project Type**: Minecraft mod (single project) + Python tools
**Performance Goals**: Book opens and turns pages with no visible delay (SC-007): pages laid out once per open and cached; deeds tick within a second (SC-010)
**Constraints**: No new runtime dependency (FR-001); payloads only to connections with the channel; MineColonies and Curios optional; names in data files (approved names only, Principle V)
**Scale/Scope**: ~8 chapters, ~70 entries, ~35 glossary terms, ~55 electronic parts in the translation table, 6 ranks, 14 Stoner Badges (~150 requirements), ~20 scene illustrations, 1 item + 1 wearable, 1 PDF of ~120 pages

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How |
|---|---|---|
| I. MineColonies Stays Optional | PASS | Colony chapter, Stoner Badges and requirements carry `needs: minecolonies`; hidden when absent; no rank needs it (FR-028); colony deed hooks live under `compat/minecolonies/` |
| II. Existing Machines Keep Their Game | PASS | Terpinator and Infuser only gain a deed call after their existing logic; no recipe, output or gameplay change |
| III. The Power Rule | PASS | Not touched; "a machine runs on power" is read from `MachinePower`'s existing result |
| IV. Failure Is Safe | PASS | Not touched |
| V. Approved Names | PASS with a gate | Title, motto, slogan and "Stoner Badges" approved. Rank, badge, Oath, Law and "Stoner Badge Sash" names are proposals: they live only in data/lang files and the user approves them before the release (task gate) |
| VI. Rules Are Pure And Tested | PASS | Markup parser, requirement matcher, rank ladder and progress merge in `domain/handbook/` with JUnit; deed ticks, persistence and reset in game tests; tunables (chat announcement, notice duration) in config |
| VII. Milestones Prove Themselves In Game | PASS | Six milestones (below), each tested in game and committed before the next |
| Technical: payloads | PASS | Progress sync and sash payloads guarded with `hasChannel` |
| Technical: art from generators | PASS | Book GUI textures, patches, sash texture, icons and scenes come from `tools/generate_handbook_*.py`; the cover is the user's art (`tools/art/handbook_cover.png`) |

Post-design re-check: unchanged, all PASS.

## Milestones

1. **The book** (US1, US5 partly): Handbook item + recipe; screen with cover, DON'T PANIC page, contents, spreads, page turning, links, back, bookmark; markup renderer with text, items (tooltips), recipes, images, tables, steps; the content check; chapters Getting Started and Growing Weed. *In game*: open, navigate, follow the chapters to a first joint.
2. **All chapters** (US2, US4): the remaining chapters; scene illustrations; glossary and index; MineColonies hiding; terpene spoiler guard. *In game*: every entry opens, every link works, hidden without MineColonies.
3. **Inspoweednetics** (US3): theory, binary, logic, parts, six example circuits (each built and checked in game), the translation table from the real-term data. *In game*: build all six from the book.
4. **Ranks and Stoner Badges** (US6): definitions (requirement lists drafted for the user's review first), deeds, progress store, sync, notices, sash page, honour signing, reset command. *In game*: a fresh character ticks Tenderbud through playing.
5. **Stoner Badge Sash** (FR-035 to FR-040): item, render layer with patches, Curios slot, chest-slot fallback, sync to others. *In game*: two players see each other's patches.
6. **PDF edition and release** (FR-030 to FR-034, FR-018): PDF builder, icon export, CI build and release asset, README/CurseForge link; one Handbook in the Supply Ship and Supply Camp racks (sensi-heights pack). *Checked*: PDF matches the book (SC-012).

## Project Structure

### Documentation (this feature)

```text
specs/016-weedcraft-handbook/
├── plan.md              # This file
├── research.md          # Phase 0 decisions
├── data-model.md        # Phase 1 entities
├── quickstart.md        # Phase 1 in-game walkthroughs per milestone
├── contracts/
│   ├── book-markup.md   # Entry file format (read by Java and Python)
│   ├── scouting-data.md # Rank/badge/requirement JSON and the deed list
│   └── payloads-and-commands.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
src/main/java/com/malevolentgods/fourtwenty/
├── domain/handbook/          # pure: Markup (parse to elements), Layout (paginate), Requirement matching,
│                             #   RankLadder, Progress (merge, counts), Contents/Index building
├── handbook/                 # common: HandbookItem, BookData (load entries/chapters from assets),
│                             #   ScoutingData (ranks/badges from data), Deeds (fire), ProgressStore (player NBT),
│                             #   ProgressPayload, SignPayload, HandbookCommands (reset), SashItem
├── client/handbook/          # HandbookScreen, PageRenderer (elements), RecipeView, Bookmark, Notices,
│                             #   SashLayer (render layer), ProgressClient
└── compat/minecolonies/handbook/  # colony deeds (Dispensary, Budtender, Lifted Builders)

src/main/resources/assets/fourtwenty/handbook/
├── book.json                 # chapters in order, icons, needs
└── en_us/entries/<chapter>/<entry>.md   # markup (translatable by language folder)
src/main/resources/assets/fourtwenty/textures/gui/handbook/   # cover, paper, frames, scenes/, patches/, emblems/
src/main/resources/data/fourtwenty/handbook/
├── ranks/<rank>.json
└── badges/<badge>.json
src/main/resources/data/curios/tags/item/body.json            # sash in Curios' body slot (loads only with Curios)

tools/
├── handbook_markup.py        # Python reader of the same markup (shared with the PDF)
├── check_handbook.py         # dead links, icons, items covered, real terms present, counts
├── build_handbook_pdf.py     # the PDF
├── generate_handbook_art.py  # paper, frames, emblems, patches, sash texture
├── generate_handbook_scenes.py   # scene PNGs from scanned builds (tools/art/handbook_scenes/*.blueprint)
└── art/handbook_cover.png, art/handbook_icons/*.png (exported from the game, research R6)

src/test/java/.../domain/handbook/   # JUnit
src/gametest/java/.../gametest/HandbookGameTests.java
src/colonytest/java/.../colonytest/HandbookColonyGameTests.java
docs/handbook/A-Handbook-of-Weedcraft.pdf
```

**Structure Decision**: single mod project; book content as assets so languages and resource packs can change it; scouting rules as data so servers can adjust them; pure logic in `domain/handbook/` per Principle VI.

## Complexity Tracking

| Choice | Why needed | Simpler alternative rejected because |
|---|---|---|
| Own book screen instead of Patchouli | User's call (FR-001: no book-mod dependency, our look) | Patchouli adds a required mod and its generic look |
| A shared markup read by Java and Python | One source for the game and the PDF (FR-030, SC-012) | Separate PDF text would drift from the book |
| Deed hooks in many systems | Tracked requirements (FR-022, SC-009 ≥ 80%) | Honour-only requirements wouldn't be "in-game tracking" |
| reportlab (tools-only) | Proper PDF with links, outline and embedded fonts | Pillow can only make image-only PDFs (no links, big files) |
