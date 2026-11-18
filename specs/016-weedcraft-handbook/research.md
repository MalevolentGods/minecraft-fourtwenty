# Research: The Handbook

## R1. Where the book's words live

- **Decision**: Entry text in Markdown-like files under `assets/fourtwenty/handbook/<lang>/entries/<chapter>/<entry>.md`, chapter order and icons in `assets/fourtwenty/handbook/book.json`. A missing language falls back to `en_us` per entry. Short UI strings (buttons, notices, item names) stay in `lang/en_us.json`.
- **Rationale**: Long prose is unreadable as lang keys; a language folder per entry is how translators work on books. Assets mean resource packs can restyle or translate. One file format serves the game and the PDF.
- **Alternatives**: lang keys per paragraph (unwieldy, ~1000 keys); data (server) files (book would depend on the server's copy; text is a client concern).

## R2. Markup

- **Decision**: A small, strict markup (contract: `contracts/book-markup.md`): front matter (title, icon, terms, needs), headings, paragraphs with **bold**/*italic*/colour spans, lists, numbered steps, `{item:id}`, `{recipe:id}`, `{image:path}`, `{table}`, `{truth:AND}`, `[text](entry:id)` links, `{spoiler:terpene:code}` guarded text, `{page}` hard breaks. Parsed in `domain/handbook/Markup` (pure, JUnit) and in `tools/handbook_markup.py`; a shared test file of samples is run by both parsers so they agree.
- **Rationale**: Strict and small means two parsers can match exactly and errors are caught by the content check.

## R3. Pictures of items and recipes in game

- **Decision**: Items via the normal GUI item renderer (with tooltips on hover); recipes via the client's `RecipeManager` (synced from the server, so datapack changes show), drawn in our own frame (crafting grid, shapeless, smelting, and our machine recipes); an entry naming a recipe id that isn't loaded shows the item and a short "recipe changed on this server" note instead of failing at runtime (the build check catches it in our own data).
- **Rationale**: FR-004/edge cases: always the recipe the game actually uses.

## R4. Progress: deeds, storage, sync

- **Decision**: Requirements match **deeds**: `Deeds.fire(player, deed, subject, amount)` called from existing hooks (crafted, harvested, trimmed, cured, smoked, got high, placed, powered, discovered terpene, Infuser match, Terpinator produced, circuit outcome, colony deeds). Requirement definitions in `data/fourtwenty/handbook/{ranks,badges}/*.json` (contract `scouting-data.md`). Matching is pure (`domain/handbook/Requirement`). Progress in the player's `PlayerPersisted` NBT under `fourtwenty_handbook` (survives death, like `fourtwenty_terpenes`). On change, a `ProgressPayload` (diff) goes to that player only, guarded by `hasChannel`. A notice is shown client-side. Earned badges are also sent to players tracking the wearer for the sash (`SashPayload`).
- **Rationale**: Reuses the established per-player pattern; data files let us tune requirements without code; diffs keep packets small.
- **Old worlds**: on first login after install, a one-time **catch-up** ticks what's observable now: items in inventory (obtain deeds), discovered terpenes, current strains known (US6 scenario 7).

## R5. Curios without a compile dependency

- **Decision**: Put the sash in Curios' `body` slot with the data tag `data/curios/tags/item/body.json` (Curios reads it; vanilla ignores an unknown namespace's tag). Draw the sash ourselves with a player render layer that checks the chest slot, then Curios' body slot via reflection on `top.theillusivec4.curios.api.CuriosApi` (cached, failing quietly). No Curios jar on the compile classpath, so `validation/compatibility-lock.json` doesn't change.
- **Alternatives**: Curios `ICurioRenderer` API (needs a compile dependency and the lock); chest slot only (user wants an accessory).

## R6. Item icons for the PDF

- **Decision**: A dev-only client command `/fourtwenty handbook export` renders every item the book references to 64×64 PNGs (offscreen framebuffer) into `tools/art/handbook_icons/`, committed; the content check fails if a referenced item has no icon (re-export). Recipes in the PDF are drawn from the recipe JSON in `src/main/resources/data/*/recipe` and vanilla's from the client jar.
- **Rationale**: Blocks have 3D models; the game's own renderer is the only faithful source. CI can't run a client, so icons are committed artifacts like the other generated art.

## R7. PDF builder

- **Decision**: `tools/build_handbook_pdf.py` with **reportlab** (installed for tools and in CI's Python step): book-sized pages, embedded open-licence fonts (OFL) chosen to match the in-game type, the cover art full-bleed, DON'T PANIC alone on page 2, PDF outline (bookmarks) and internal links, index and glossary with page numbers, printable check-off pages for ranks and badges. Colony content marked "needs MineColonies". No terpene names (spoiler guard).
- **CurseForge**: project files must be mod files; a PDF is not accepted as a file for a mod project, so the description links to the PDF on the GitHub release (FR-033's fallback). To confirm when the PDF ships.

## R8. Scene illustrations

- **Decision**: Build each scene in the local test world, scan it (Structurize scan, as the pack's buildings are), keep the scan in `tools/art/handbook_scenes/`, and render it with the isometric renderer used for the pack (`sensi-heights/tools/render_blueprint.py`, ported into `tools/generate_handbook_scenes.py`) in the book's palette. PNGs go to `textures/gui/handbook/scenes/`.
- **Rationale**: "rendered from real builds in our pixel style"; regenerable (art-from-generators rule).

## R9. Detecting example circuits

- **Decision**: Each example circuit has an **outcome detector**: when a Lit Bit (or other output) changes state, the circuit deed checks its driving net for the example's signature (for example, "a Lit Bit lit only while two Toggle Joints are both on" for the AND lock, "a Lit Bit that toggles on its own at a steady rate" for the clock, "two Lit Bits showing the sum of two switch inputs" for the adder). Signatures are pure functions over a small snapshot of the net (component types, states, connections) in `domain/handbook/CircuitSignatures`, JUnit-tested with hand-made snapshots.
- **Rationale**: Layout-agnostic (assumption in the spec), cheap (runs only on output changes).
- **Risk**: snapshotting a net needs a read view of the electronics graph; if a signature can't be judged reliably, that requirement becomes honour (it still counts toward the ≥ 80% target only if most others are tracked).

## R10. Bookmark

- **Decision**: Last page per player kept on the client (`config/fourtwenty-handbook.json`, keyed by server/world); missing → title page.

## R11. Recipes for the new items

- **Decision (proposed, user review)**: Handbook = book + weed bud (shapeless). Stoner Badge Sash = 3 hemp fabric (any colour) + string in a diagonal (shaped). Names and recipes reviewed with the user before release.
