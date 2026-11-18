# Feature Specification: The Handbook

**Feature Branch**: `016-weedcraft-handbook`
**Created**: 2026-11-17
**Status**: Draft
**Input**: User description: "An in-game guidebook for Four Twenty, titled exactly *A Handbook of Weedcraft, Scouting, and Weed-craft*: our own book (no book-mod dependency), a very nice book screen in the mod's look, contents, index, cross-links and a glossary; crafted, and one copy in the racks of the Sensi Heights Supply Ship and Supply Camp; live item/recipe pictures plus illustrated scenes; chapters from getting started through growing, processing, colours, the Terpinator, power and MineColonies, and a beginner's electronics guide (Inspoweednetics) with theory, binary, Boolean logic, example circuits and a back-of-the-book table of real-world electronics terms; kept correct as the mod changes."

## Context

Four Twenty has grown to dozens of systems (strains and breeding, trimming and curing, terpene colours, the Terpinator, Boundless Inspiration power, electronics, MineColonies huts and Lifted Builders) and nothing in the game teaches them. Players learn from tooltips, the README or by asking. The user, playing a fresh balance-testing run, wants one beautiful in-game book that takes a new player from their four starter seeds to building circuits. Title (user-approved, 2026-11-17, kept exactly): **A Handbook of Weedcraft, Scouting, and Weed-craft**, a riff on Ernest Thompson Seton's *Handbook of Woodcraft, Scouting, and Life-craft*.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A new player learns to grow and smoke their first weed from the book (Priority: P1)

A player who has never seen the mod gets the Handbook (crafted, or from a Supply Camp or Supply Ship rack), opens it, reads Getting Started and Growing Weed, and follows them: plants the starter seeds, grows, harvests, trims, cures and smokes their first bud, without leaving the game.

**Why this priority**: This is the gap the user named: lots of content, no way to learn it. The book screen, navigation and the first chapters are the core everything else hangs on.

**Independent Test**: Give a player who doesn't know the mod the Handbook and four seeds; they reach a smoked joint using only the book.

**Acceptance Scenarios**:

1. **Given** a player holding the Handbook, **When** they use it, **Then** the book opens on its title page (or where they last left off), showing the title, a contents page and page turning.
2. **Given** the contents page, **When** the player clicks a chapter or entry, **Then** the book jumps to it; a back control returns them to where they were.
3. **Given** an entry that mentions an item, block or recipe, **When** it's shown, **Then** the item's picture and, where it has one, its real crafting recipe (from the game's own data) are drawn on the page, and hovering shows the item's normal tooltip.
4. **Given** the Getting Started and Growing Weed chapters, **When** a new player follows them step by step, **Then** every step matches how the game actually behaves (planting, light, harvest, seeds from trimming, curing, smoking).

---

### User Story 2 - Every system in the mod has a chapter (Priority: P1)

A player who has their first weed going wants the next thing: processing machines, oil and rosin, the Vape Pen, researching colours with the Inspiration Infuser, the Terpinator, Boundless Inspiration power, and, when MineColonies is installed, the Dispensary, Budtender, Bud Barn, Weed Lab, Lifted Builders and the Midnight Machine. Each has an entry that says what it is, how to get it and how to use it, with pictures.

**Why this priority**: Coverage is the point of the book; a book that stops after growing leaves the hard systems as undiscoverable as today.

**Independent Test**: For each system listed, a player can find its entry from the contents or index and use the system by following it.

**Acceptance Scenarios**:

1. **Given** the chapters Getting Started, Growing Weed, Trimming and Processing, Researching Colours, The Terpinator, Boundless Inspiration and (with MineColonies) Colony Life, **When** a player opens each, **Then** each holds entries for every system in it, as listed in FR-010.
2. **Given** MineColonies is not installed, **When** the player browses the book, **Then** the MineColonies chapter and any entry that needs MineColonies do not appear, and no link leads to a missing page.
3. **Given** an entry about a machine or setup (a grow room, a powered trimming line, a wiring layout), **When** it's shown, **Then** it includes an illustrated scene of a real build, in the mod's pixel style.
4. **Given** terpenes the player has not discovered yet, **When** the Researching Colours chapter shows terpenes, **Then** it does not reveal their names or colours (they stay hidden as they are elsewhere in the game) and explains how to discover them.

---

### User Story 3 - A player learns real electronics through Inspoweednetics (Priority: P2)

A player curious about the electronics parts reads the Inspoweednetics section: basic electronic theory (voltage, current, resistance, power, and the mod's units), binary and base 2, Boolean logic and truth tables, and the mod's parts. They build the example circuits in the game step by step, and use the back-of-the-book table to learn each part's real-world name.

**Why this priority**: It's the most ambitious and educational part, and the electronics are the hardest system to learn, but players can grow and process without it.

**Independent Test**: A player with no electronics background builds each example circuit from its steps and pictures, and it works as the book says.

**Acceptance Scenarios**:

1. **Given** the Inspoweednetics section, **When** a player reads it in order, **Then** it covers electronic theory, binary/base 2, Boolean logic with truth tables, the mod's parts, and example circuits, each building on the last.
2. **Given** an example circuit, **When** a player follows its numbered steps and pictures in the game, **Then** the finished circuit behaves exactly as the book describes (each example is checked in the game before release).
3. **Given** the back-of-the-book translation table, **When** a player looks up any electronic part, **Then** it lists the in-game name beside its real-world term (the same real names the parts' tooltips show), and the in-game units beside the real units.
4. **Given** a new electronic part is added to the mod, **When** the book is built, **Then** the translation table includes it, or the build reports it missing.

---

### User Story 4 - Find anything fast (Priority: P2)

A player who wants one fact (what a Hit Box does, which family a strain is, what "Kb" means) finds it from the index, the glossary or a cross-link, without paging through chapters.

**Why this priority**: A reference book is used far more for looking things up than for reading straight through.

**Independent Test**: Asked ten random questions about the mod, a player finds each answer's page from the index, glossary or a link in under 30 seconds.

**Acceptance Scenarios**:

1. **Given** the index at the back, **When** the player clicks a term, **Then** the book opens at the entry that explains it.
2. **Given** an entry that mentions another system, **When** the player clicks the highlighted mention, **Then** the book jumps to that system's entry.
3. **Given** the glossary, **When** the player reads a term (Lifted, Puff, cured bud, terpene, …), **Then** it gives a one-line meaning and a link to the full entry.
4. **Given** the book is open, **When** the player closes and reopens it later, **Then** it opens where they left off.

---

### User Story 5 - Getting the book (Priority: P3)

A player crafts the Handbook from simple early-game ingredients, can craft more to replace a lost copy or give one to a friend, and finds one already waiting in the racks of a Sensi Heights Supply Ship or Supply Camp when their colony starts.

**Why this priority**: Needed for anyone to have the book, but small once the book exists.

**Independent Test**: A player crafts the Handbook in survival; a fresh Supply Camp and Supply Ship each have one in their racks.

**Acceptance Scenarios**:

1. **Given** the recipe ingredients, **When** a player crafts it, **Then** they get the Handbook, named exactly *A Handbook of Weedcraft, Scouting, and Weed-craft*.
2. **Given** a Sensi Heights Supply Ship or Supply Camp just placed, **When** the player opens its racks, **Then** one Handbook is there.

### User Story 6 - Ranks and merit badges, marked off in the book (Priority: P2)

Like the Boy Scout Handbook, the book gives the player things to do: six ranks earned in order, each a page of numbered requirements, and merit badges, one per system, each with its own requirement list. When the player does something the game can see (harvests a Cross strain, cures a bud, powers a machine from a Hookah, discovers a terpene, builds a working circuit), the requirement is ticked off in their book on its own; things the game can't see are signed on the player's honour, as a Scoutmaster would sign the real thing. Finishing a rank or badge is celebrated and shown in the book.

**Why this priority**: It turns the book from a manual into a guided playthrough with goals, the way the Scout handbook works, and it's the user's ask. It needs the book (US1-US2) to exist first.

**Independent Test**: A fresh player plays normally; each requirement they meet is ticked in the book within a moment; completing every requirement of a rank or badge marks it earned and announces it.

**Acceptance Scenarios**:

1. **Given** the Ranks section, **When** the player opens it, **Then** it shows the six ranks in order, each with numbered requirements ("1.", "2a.", "2b.") in the Scout handbook's style, ticked or open for that player, and which rank they hold.
2. **Given** a requirement the game can detect, **When** the player does it, **Then** it's ticked in their book (and a small notice says so), with nothing to click.
3. **Given** a requirement the game can't detect, **When** the player clicks its box, **Then** it asks them to sign on their honour and, once signed, stays ticked.
4. **Given** every requirement of a rank is ticked and the rank before it is earned, **When** the last one is ticked, **Then** the rank is earned, announced, and its emblem appears in the book; ranks are earned only in order (badges in any order).
5. **Given** a merit badge's requirements are all ticked, **When** the last one is ticked, **Then** the badge is earned and its patch appears on the player's badge sash page in the book.
6. **Given** a multiplayer server, **When** players read their books, **Then** each sees only their own progress; it survives logging out, dying and server restarts.
7. **Given** progress made before the Handbook existed (an old world), **When** the player first opens it, **Then** nothing is ticked retroactively except what the game can still see (for example items in their inventory, terpenes already discovered); the rest is earned going forward or signed on honour.
8. **Given** MineColonies is absent, **When** the player views ranks and badges, **Then** colony badges and any colony requirement are hidden, and no rank needs MineColonies to be earned.

### Edge Cases

- MineColonies not installed: its chapter and entries are absent; links to them are not shown; the Supply Ship/Camp copy is irrelevant (those are MineColonies pieces).
- An item, block or recipe the book refers to is removed or renamed in a later version: the book build fails with the missing reference listed, rather than shipping a blank picture or a dead link.
- A recipe changes: the book shows the new recipe automatically (drawn from the game's data).
- A recipe is changed by a modpack or datapack: the book shows the recipe the game actually uses.
- Undiscovered terpenes: hidden in the book as everywhere else (FR-012).
- Very long entries and small screens / large GUI scale: text and pictures reflow onto more pages; nothing is cut off.
- A language with longer words: text reflows; translations fall back to English per line where missing.
- Opening the book on a server: it reads entirely on the player's side; no server change is needed beyond having the mod.
- A player opens a link to an entry hidden by a missing mod (e.g. from the glossary): the link isn't offered.

## Requirements *(mandatory)*

### Functional Requirements

**The book**

- **FR-001**: The mod MUST add the Handbook, an item named exactly **A Handbook of Weedcraft, Scouting, and Weed-craft**, that opens the book when used. Four Twenty MUST NOT need any other mod for it.
- **FR-002**: The book MUST have its own look in the mod's style (cover, paper, Four Twenty palette and type), with two-page spreads, page turning, page numbers, and a bookmark that reopens the book where the player left it.
- **FR-002a**: The cover and title page MUST use the user's cover art (`tools/art/handbook_cover.png`, 2026-11-17: the scout on the cliff over a psychedelic Minecraft vista) and credit its author as printed on it, **Simon Stalwart**.
- **FR-003**: The book MUST open on a title page and include a contents page listing chapters and their entries, each clickable.
- **FR-004**: Entries MUST support: formatted text (headings, bold, colour, lists), item and block pictures with their normal tooltips on hover, crafting and machine recipes drawn from the game's own recipe data, illustrated scene images, truth tables and simple tables, numbered steps, and clickable links to other entries.
- **FR-005**: A back control MUST return the player to the page they came from after following a link or a contents/index entry.
- **FR-006**: The back of the book MUST hold an index (terms → pages), a glossary (term, one-line meaning, link), and the Inspoweednetics translation table (FR-015).
- **FR-007**: All book text MUST live in data files keyed for translation (English first), separate from the code, so it can be edited and translated without changing code.
- **FR-008**: Entries that need MineColonies MUST be hidden when MineColonies is absent, along with every link and index line that leads to them.
- **FR-009**: The book's build MUST fail when an entry refers to an item, block, recipe, image or entry that doesn't exist, listing each missing reference.

**Content** (written in the mod's voice for new players, every step matching how the game behaves)

- **FR-010**: The book MUST contain these chapters and, at least, these entries:
  - **Getting Started**: what Four Twenty is; the four starter seeds; your first grow, harvest, trim, cure and smoke; being high.
  - **Growing Weed**: planting and light; harvesting (the plant comes up; seeds come from trimming); strains, families and grades; Grow Ops and plot tiers; pollination and breeding; Pheno Chambers; Pot Pots; bone meal.
  - **Trimming and Processing**: rough, trimmed and cured bud; the Trim Tray and Curing Rack; the powered trimmers and curers by tier; oil and rosin; smoking: joints, pipes, bongs, the Hookah and Vape Pen; edibles.
  - **Researching Colours**: terpenes and discovery; the Inspiration Infuser and its colour-match game; colour drift and families.
  - **The Terpinator**: what it does; using it; production; the Steel and Lab Terpinators.
  - **Boundless Inspiration**: tiers Puff, Toke, Rip and Blaze; Hookahs as generators; wicks and cables (red/black, Twin and Daisy, one colour per line); stashes; the Hit Box; overloads; machines running free near a high player.
  - **Colony Life** (MineColonies only): the Dispensary and Budtender; the Bud Barn; the Weed Lab's levels; Lifted Builders and the Midnight Machine; the Sensi Heights style pack and its buildings.
  - **Inspoweednetics** (FR-013 to FR-016).
- **FR-011**: Entries about machines, setups and builds MUST include at least one illustrated scene rendered from a real in-game build in the mod's pixel style; item and recipe pictures MUST come from the game itself.
- **FR-012**: The book MUST NOT reveal names or colours of terpenes the reading player hasn't discovered, the same as the rest of the game; it shows them once discovered.

**Inspoweednetics**

- **FR-013**: The section MUST teach, in order: basic electronic theory (voltage, current, resistance, power, series and parallel, in the mod's units with real units beside them); binary and base 2 (counting, bits and bytes, converting); Boolean logic (NOT, AND, OR, NAND, NOR, XOR, XNOR with truth tables); and the mod's parts grouped by what they do (power, passives, switches, gates, memory, timing, buses and data parts, the Weedweeno and Bit Chit).
- **FR-014**: The section MUST include at least six example circuits to build in the game, from simple to advanced, each with a parts list, numbered steps, pictures and what it should do; at least: a switched light; an inverter; a two-switch AND lock; a one-bit memory (latch); a blinking clock; and a small adder showing binary addition on lights.
- **FR-015**: The back of the book MUST contain a translation table from every electronic part's in-game name to its real-world electronics term (the same real names the parts' tooltips show), plus the mod's units beside the real units.
- **FR-016**: When a part is added to the mod and its real name is recorded, the table MUST include it; a part without a recorded real name MUST fail the book's build.

**Getting the book**

- **FR-017**: The Handbook MUST be craftable in survival from early-game ingredients (proposed: a book and a weed bud), repeatably.
- **FR-018**: One Handbook MUST sit in the racks of the Sensi Heights Supply Ship and the Supply Camp.

**Ranks, merit badges and the Scout front matter** (the Boy Scout Handbook's format, with light weed jargon and humour, never heavy)

- **FR-019**: The book MUST open its scouting part with front matter in the Scout handbook's style: an Oath, a Law (a short list of virtues), a Motto and a Slogan, each a gentle weed riff on the original. Proposed (for the user's approval): **Motto** "Be Prepared (Bring Snacks)"; **Slogan** "Pass It Left Daily"; **Oath** "On my honour I will do my best to grow my best, share my stash, keep my fire lit and my colony fed"; **Law** "A Weedcrafter is Chill, Generous, Patient, Curious, Tidy, Helpful, Respectful of Elders, Kind to Creepers (from a distance), Thrifty with Seeds, Prepared, Cheerful and Lifted".
- **FR-020**: The book MUST hold six **ranks**, earned strictly in order, that follow the strain ladder (a rank per grade). Proposed names, riffing on Tenderfoot to Eagle (for the user's approval): **Tenderbud** (your first Landrace grow), **Second Toke** (a family strain), **First Hit** (a Cross), **Starseed** (a Double Cross), **High Life** (an Ultra), **Golden Eagle** (Golden God). Each rank page lists numbered requirements (with lettered sub-parts) that walk the player through the systems at that stage, not only the strain.
- **FR-021**: The book MUST hold **merit badges**, earnable in any order, at least one per chapter's system. Proposed set (riffing on real badges; names for the user's approval): **Ganja Gardening** (growing), **Plant Breeding** (pollination, Pheno Chambers), **Bud Processing** (trimming and curing), **Kitchen Chemistry** (oil, rosin, the Vape Pen), **Munchies** (edibles), **Safe Sesh** (joints, pipes, bongs, the Hookah), **Colour Theory** (terpenes and the Inspiration Infuser), **Terpination** (the Terpinator), **Inspiration** (Boundless Inspiration power), **Inspoweednetics** (electronics, including building the book's example circuits), **Bits & Bytes** (binary, logic and the Weedweeno), **Hempcraft** (hemp blocks and colours), and, with MineColonies, **Community Sesh** (the Dispensary and Budtender) and **Lifted Building** (Lifted Builders and the Midnight Machine).
- **FR-022**: Each rank and badge requirement MUST be one of: **tracked** (the game ticks it when the player does it: obtains, crafts, harvests, trims, cures, smokes, gets high on a strain, places or powers a block, discovers a terpene, wins an Infuser match, earns another badge, builds a working example circuit) or **honour** (the player ticks it and confirms "on my honour"). Most requirements MUST be tracked; honour is for what the game can't see (explain something to a friend, read a chapter).
- **FR-023**: Tracked requirements MUST be ticked within a second of the deed, with a small on-screen notice naming the requirement, without opening the book.
- **FR-024**: Each requirement on a rank or badge page MUST show its box (open, ticked, or signed on honour), and link to the book entry that teaches it.
- **FR-025**: Earning a rank or badge MUST be announced to the player (a notice and a sound), shown in the book (the rank emblem on the Ranks page; the badge patch on a **Badge Sash** page that shows every badge, earned in colour and unearned greyed), and MAY be announced in chat to others on a server (a server setting, on by default).
- **FR-026**: Progress MUST be kept per player, survive death, logout and restarts, and be shown only to that player.
- **FR-027**: An operator MUST be able to reset a player's Handbook progress (all of it or one rank or badge), for balance testing and do-overs.
- **FR-028**: Requirements that need MineColonies MUST be hidden when it's absent; no rank may depend on MineColonies.
- **FR-029**: The tone throughout (front matter, requirement wording, notices) MUST be the Scout handbook's earnest, helpful voice with light weed jargon and humour sprinkled in, never crude or heavy; in the mod's established voice.

### Key Entities

- **Handbook**: the item; opens the book; remembers the reading player's last page.
- **Chapter**: a titled group of entries with an icon, in a fixed order; may need a mod (MineColonies).
- **Entry**: one topic: title, icon, pages, the terms it explains (for the index and glossary), and what it needs (a mod, a discovery).
- **Page element**: text, heading, item picture, recipe, scene image, table, truth table, numbered step, link.
- **Glossary term**: term, one-line meaning, the entry it links to.
- **Part translation**: a part's in-game name, its real-world term, and the units table.
- **Example circuit**: name, goal, parts list, numbered steps with pictures, expected behaviour, and whether it's been checked in the game.
- **Rank**: name, emblem, order (1-6), the strain grade it follows, its requirements.
- **Merit badge**: name, patch, the system it covers, whether it needs MineColonies, its requirements.
- **Requirement**: number (1, 2a, …), wording, tracked or honour, what the game watches for (and how many), the entry that teaches it.
- **Player progress**: per player: which requirements are ticked (and whether on honour), counts toward counted requirements, ranks and badges earned and when.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A player new to the mod gets from four seeds to their first smoked joint using only the book, in one sitting, without outside help.
- **SC-002**: Every item and block Four Twenty adds that a player can obtain appears on at least one page of the book and in the index (100% coverage, checked when the book is built).
- **SC-003**: Every electronic part appears in the translation table (100%, checked when the book is built).
- **SC-004**: All six or more example circuits work in the game exactly as described, checked before release.
- **SC-005**: Asked ten questions about the mod, a player finds each answer's page in under 30 seconds using the contents, index, glossary or links.
- **SC-006**: Zero dead links, missing pictures or missing recipes in a released book (the build refuses to ship them).
- **SC-007**: The book opens and turns pages with no visible delay on the user's machine.
- **SC-008**: The user reads the finished book and calls it "very nice" (the bar the user set).
- **SC-009**: At least 80% of all rank and badge requirements are tracked automatically (not honour).
- **SC-010**: In a full playthrough, every tracked requirement ticks when its deed is done (zero missed ticks across the user's balance-testing run), within a second.
- **SC-011**: A player following only the ranks reaches Golden Eagle having used every chapter's system at least once.

## Assumptions

- Recipe for the Handbook: a book plus a weed bud (shapeless); the user can change it at review. Ingredients are early-game so a new player can make it.
- The book is English first; other languages can be added later through the same data files.
- MineColonies content is hidden (not greyed out) when MineColonies is absent: cleaner for non-colony players.
- The bookmark is kept per player on their own machine; losing it (new machine) just reopens the title page.
- A search box is not required; contents, index, glossary and links cover look-ups. It may be added later.
- Scene illustrations are made by rendering real builds (as the pack's building renders are) and stored with the mod; they're redrawn when a build changes.
- Example circuits use only parts a survival player can craft (no creative-only bench rig: the user's call that the Bench Rig isn't in survival).
- Weed College and Tie Dye Shack (not built yet) are not covered; the Colony Life chapter grows when they ship.
- The README and CurseForge text may later point to the book, but updating them isn't part of this feature.
- Ranks and badges give recognition (emblems, the sash, notices), not items or stat boosts, so they don't move the balance the user is testing; real reward items can be added later if the user wants.
- The names of ranks, badges and the Oath/Law/Motto/Slogan in FR-019 to FR-021 are proposals; every player-facing name needs the user's approval before it ships.
- The rank and badge requirement lists themselves are drafted in planning (with the content) and reviewed by the user.
- "Building a working example circuit" is tracked by watching for the circuit's outcome (for example a Lit Bit lit through an AND of two switches), not by matching an exact layout.
- Seeds come from trimming and harvests take the plant up (1.0.2); the book describes the game as it is when the book ships, and later balance changes update the affected entries.
