# Feature Specification: The Tie Dye Shack

**Feature Branch**: `018-tie-dye-shack`
**Created**: 2026-11-28
**Status**: Draft
**Input**: User description: "The Tie Dye Shack (MineColonies only; builds on spec 017 Weed College and ships in the same release, FR-026b). A new MineColonies hut, the Tie Dye Shack, worked by a colony worker, the Tie Dyer. It is the first colony worker to use the colours the Weed College researches: it dyes only with colours the colony has researched (the 16 Minecraft colours and the 64 hemp colours, studied in the College's Chemistry branch), never teaches a player a colour (017 FR-021), and players still catch their own colours at the Inspiration Infuser. It dyes in tie-dye patterns: about 8 patterns (spiral, bullseye, crumple, stripes and so on), each pattern unlocked by one of the College's 8 Textiles studies (today placeholders named Pattern 1 to 8 that show "Waiting for the Tie Dye Shack"; this spec names them, sets their costs and makes each unlock its pattern). User decision on record (3 Nov): "Tie Dye goes all the way": the full range of patterns in the colony's colours on hemp blocks, rugs, couches, beds and colonist shirts; hemp blocks and rugs come first. Tie-dye art is drawn by our generators (the rug generator style), in Four Twenty's palette. The big design question to settle in planning: how a pattern and its colours are stored (separate blocks would be about 640 per block type; likely a block entity or a tinted pattern overlay). Colonist shirts need a render layer on citizens with the shirt tinted per pattern and colour. The hut gets five Sensi Heights levels (blueprints from the sensi-heights repo designer, signature door from level 3, doors saved closed), a hut block with its own diorama model, a status tab, and a Handbook page in the colony chapter (voice rules apply). Nothing depends on anything outside Four Twenty and MineColonies; Four Twenty still loads and plays without MineColonies. No release until the user's go."

## Context

Spec 017 gave colonies the Weed College, which researches colours for the colony's own workers (017 FR-021): a colony can learn a colour nobody has caught, but that knowledge never passes to a player. Until now no colony worker uses colours, so a finished colour study changes nothing visible. The Tie Dye Shack is that worker. It turns the colony's researched colours into tie-dyed hemp blocks, rugs, couches, beds and shirts, and it's why 017 and 018 ship together.

Four facts shape it:

- **Colours are the colony's, never the player's.** The Shack dyes only with colours the colony has researched. Nothing it makes, shows or says names or teaches a colour a player hasn't caught.
- **Patterns come from the College.** Each of the College's 8 Textiles studies unlocks one pattern; a colony without the study can't dye that pattern.
- **The range is large.** About 8 patterns in up to 80 colours, on five kinds of thing. A separate block for every combination isn't workable, so the dyed things must carry their pattern and colours as data.
- **Hemp blocks and rugs come first**, then couches, beds and colonist shirts (user, 2026-11-03).

## Clarifications

### Session 2026-11-28

- Q: How many colours in one tie-dye? → A: Curated **colourways**: about 16 ready-made palettes of 2 to 4 colours each, offered for any pattern; a colourway is offered only once the colony has researched every colour in it.
- Q: How do players get tie-dye? → A: They order in the Shack's own window (pattern, colourway, item kind, count), and the finished items are delivered to the warehouse.
- Q: How much ships in this release? → A: All five kinds: hemp blocks, rugs, couches, beds and colonist shirts.
- Q: How does a colonist end up wearing a tie-dyed shirt? → A: A player hands it to them (right-click with the shirt); giving another swaps it, and a player can take it back.
- Q: Can a pattern spread across several blocks? → A: Yes, auto-fit squares: a square of 2×2 to 5×5 blocks of the same look, flat in one wall, floor or ceiling, with nothing matching touching it there, shows one big pattern across it; any other shape shows the 1×1 pattern. Patterns are drawn at 32 pixels per block (user, 2026-11-28).
- Q: Tie-dye can't be colony-only (constitution Principle I); what's the player's route? → A: A **Dye Vat**: the player puts in a plain item and one terpene vial for each colour of a colourway and picks any of the 8 patterns. Pattern and colourway names in data-model.md approved as written (user, 2026-11-28).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A colony's first tie-dye (Priority: P1)

A colony with a Weed College has researched The Neutrals and finished the first Textiles study (Spiral). The player builds a Tie Dye Shack and hires a Tie Dyer. They order tie-dyed hemp in the Shack's window: the Spiral pattern in a colourway made only of neutral colours. The Tie Dyer asks the warehouse for plain hemp blocks and the dye supplies, works at the Shack's dye vats and drying racks, and the finished tie-dyed hemp blocks go to the warehouse. Placed in the world, each block shows the spiral in its colours.

**Why this priority**: it proves the whole chain: College colours and patterns, a worker who dyes, the colony's normal requests, and a dyed block that keeps its look when placed, picked up and placed again.

**Independent Test**: in a colony with a level 1 Shack, a Tie Dyer, a stocked warehouse, The Neutrals and the Spiral study done, order Spiral hemp in a neutrals colourway; watch the Tie Dyer work; confirm the blocks arrive and look right when placed, mined and re-placed.

**Acceptance Scenarios**:

1. **Given** a colony that has researched The Neutrals and Spiral, **When** the player opens the Shack's dye window, **Then** it offers Spiral (unlocked), shows the other patterns as waiting for their studies, and offers the colourways made only of neutral colours, showing the others as needing research.
2. **Given** an order for 8 Spiral hemp blocks in a neutrals colourway, **When** the Tie Dyer works it, **Then** they request plain hemp and the dye supplies, work at the vats and racks, and 8 tie-dyed blocks reach the warehouse.
3. **Given** a placed tie-dyed block, **When** it is mined and placed again, **Then** it keeps its pattern and colours.

---

### User Story 2 - Only the colony's colours (Priority: P1)

A player who has never caught red belongs to a colony that has researched The Warms. The Shack dyes in red for the colony, and the player can own and place red tie-dye, but nothing teaches them red: the dye window shows a colour the viewer hasn't caught as a swatch with no name, and the player's Infuser and Terpinator are unchanged.

**Why this priority**: it's the rule 017 promised (FR-021) and the reason colony research and player catching can live side by side.

**Independent Test**: as a player who hasn't caught red, order and receive red tie-dye from a colony that has researched it; confirm no colour is learned, the dye window shows red unnamed, and the item's name and description don't name it.

**Acceptance Scenarios**:

1. **Given** a colourway with a colour the colony hasn't researched, **When** the player looks for it in the dye window, **Then** it can't be ordered, and says which of its colours are missing without naming any the viewer hasn't caught.
2. **Given** a colony-researched colour the viewer hasn't caught, **When** they see it in the dye window or on a dyed item, **Then** its swatch shows but its name doesn't.
3. **Given** a player holding colony-made tie-dye in a colour they haven't caught, **When** they carry it, place it or use it, **Then** their caught and discovered colours are unchanged.

---

### User Story 3 - The full range (Priority: P2)

As the colony finishes more Textiles studies, the Shack offers more patterns, and the player orders tie-dyed rugs (which join with matching neighbours like today's rugs), couches and beds. Colonists can be given tie-dyed shirts, which they wear over their usual look.

**Why this priority**: "Tie Dye goes all the way" (user, 2026-11-03), but it builds on Stories 1 and 2.

**Independent Test**: with all 8 Textiles studies done, order one of each item kind in a different pattern; place them; put a shirt on a colonist and see it worn.

**Acceptance Scenarios**:

1. **Given** all 8 Textiles studies done, **When** the dye window is opened, **Then** all 8 patterns are offered.
2. **Given** tie-dyed rugs of the same pattern and colours side by side, **When** placed, **Then** they join into one larger rug as plain rugs do.
3. **Given** a tie-dyed couch or bed, **When** placed and used, **Then** it works like its plain version and shows the pattern.
4. **Given** a tie-dyed shirt, **When** a player right-clicks a colonist with it, **Then** the colonist wears it in its pattern and colourway, and giving another shirt swaps it, returning the first.

---

### User Story 4 - A Shack that grows (Priority: P2)

The Shack has five levels in the Sensi Heights style. Higher levels add vats and racks, a second Tie Dyer and bigger batches. Its hut block is a small diorama of a dye shack.

**Independent Test**: build each level; check worker slots, work stations and the hut block.

**Acceptance Scenarios**:

1. **Given** each level, **When** built, **Then** it has the vats and racks for that level and the doors are closed in the blueprint; levels 3 to 5 carry the signature door.
2. **Given** a level 3 or higher Shack, **When** opened, **Then** it can hire a second Tie Dyer.

---

### User Story 5 - Reading about it (Priority: P3)

The Handbook's colony chapter gains a Tie Dye Shack page: how to order, what each pattern looks like, which Textiles study unlocks it, and the colour rule.

**Independent Test**: open the page in game; check it in the printed edition; run the Handbook checks.

### Edge Cases

- A colony loses access to a pattern or colour (it can't, since studies are permanent, but a data-pack change could remove one): orders for it are refused with a reason, and already-made items keep their look.
- A dyed item ends up outside any colony, or in a world without MineColonies (picked up and carried, or a world that drops MineColonies): it keeps its look and works as its plain version; Four Twenty still loads.
- The Tie Dyer is out of plain blocks or dye supplies: the order waits, and the status tab says what's missing.
- The order is for a colour the colony researched after the order window was opened: the window updates.
- A Shack is demolished with orders queued: the orders and anything delivered are kept or returned to the warehouse, not lost.
- Two players order at once: orders queue; only owners and officers can place or cancel them (as the College's board).

## Requirements *(mandatory)*

### Functional Requirements

**The hut and its worker**

- **FR-001**: Colonies MUST be able to build a Tie Dye Shack hut, worked by a Tie Dyer, with five levels; levels 1-2 hire one Tie Dyer, levels 3-5 hire two.
- **FR-002**: The hut block MUST have its own diorama model, built like the other Four Twenty huts' (no two faces in one plane).
- **FR-003**: The Shack MUST have a status tab showing its level, workers, the order in progress, what it's waiting for, and how many items it has made.

**Colours and patterns**

- **FR-004**: The Shack MUST dye only with colours the colony has researched at the Weed College (the 16 Minecraft colours and the 64 hemp colours).
- **FR-005**: Each of the 8 patterns MUST be unlocked by exactly one of the College's Textiles studies; the studies MUST be renamed for their patterns, given real costs, and stop showing "Waiting for the Tie Dye Shack" once the Shack exists.
- **FR-006**: The 8 patterns MUST be drawn by Four Twenty's own generators in its palette, legible at block size, and distinct from each other at a glance.
- **FR-007**: The Shack MUST never teach a player a colour: its windows show a colour the viewer hasn't caught as an unnamed swatch, dyed items don't name such a colour, and carrying, placing or using a dyed item changes no player's caught or discovered colours.
- **FR-008**: A tie-dye MUST be a pattern in a **colourway**: one of about 16 curated palettes of 2 to 4 colours from the 80. A colourway MUST be orderable only once the colony has researched every colour in it, and at least one colourway MUST use only the tier-1 neutral colours, so a colony's first tie-dye needs only The Neutrals. Colourway names and colours are proposed in planning and approved by the user.

**Orders and work**

- **FR-009**: Players MUST order tie-dye in the Shack's own window: pick a pattern, a colourway, an item kind and a count. The finished items MUST be delivered to the warehouse. Orders queue and are worked in order.
- **FR-010**: Only the colony's owner and officers MUST be able to place or cancel orders.
- **FR-011**: The Tie Dyer MUST work visibly: fetching plain items and dye supplies through the colony's normal requests, working at the Shack's vats and drying racks, and delivering finished items to the warehouse.
- **FR-012**: An order MUST say what it's waiting for, keep its progress through a save and reload, and never lose supplies or items if the Shack is demolished.

**The dyed things**

- **FR-013**: Tie-dyed hemp blocks, rugs, couches and beds MUST exist in every pattern and colourway, carry their look as data (not a separate registered block per combination), and keep it when mined, moved and placed again.
- **FR-013c**: A flat square of 2×2 up to 5×5 tie-dyed hemp blocks or rugs of one look (in a wall, floor or ceiling, with nothing of that look touching it in that plane) MUST show one pattern spread across the whole square; any other arrangement shows each block's 1×1 pattern. Patterns are drawn at 32 pixels per block.
- **FR-014**: Tie-dyed rugs MUST join with matching neighbours (same pattern and colours) the way plain rugs do.
- **FR-015**: Tie-dyed couches and beds MUST work exactly like their plain versions and show their pattern.
- **FR-016**: A player MUST be able to give a colonist a tie-dyed shirt by right-clicking them with it; the colonist wears it, drawn over their usual look in its pattern and colourway, through saves and reloads. Giving another shirt swaps it (the old one comes back to the player), and a player can take a shirt back. Only the colony's members can dress its colonists.
- **FR-017**: All five kinds (hemp blocks, rugs, couches, beds and colonist shirts) MUST ship in this release, built in that order.

**Building, docs and safety**

- **FR-018**: Five Sensi Heights levels MUST be made by the sensi-heights designer with the usual checks (doors saved closed, signature door from level 3, MineColonies hut names), imported into the jar.
- **FR-019**: The Handbook's colony chapter MUST gain a Tie Dye Shack page in the Handbook's voice, and the College page's Textiles line MUST name the patterns.
- **FR-019b**: A player MUST be able to tie-dye alone at a **Dye Vat** (needs no MineColonies): a plain item of any kind plus one terpene vial for each colour of a colourway, in any of the 8 patterns. It only consumes vials; the Terpinator and the Infuser are unchanged.
- **FR-020**: Four Twenty MUST still load and play without MineColonies; a dyed item in a world without MineColonies MUST still load and keep its look.
- **FR-021**: The Shack MUST ship in the same release as the Weed College (017 FR-026b), and no release happens without the user's go.

### Key Entities

- **Pattern**: one of 8 tie-dye designs (for example Spiral, Bullseye, Crumple, Stripes); unlocked by one Textiles study; drawn by a generator.
- **Colourway**: a curated palette of 2 to 4 of the 80 colours, with a name; orderable when the colony has researched all its colours.
- **Tie-dye look**: a pattern and a colourway; carried by every dyed item and placed block.
- **Order**: what the player asked for (pattern, colourway, item kind, count), its progress, what it's waiting for, and who placed it.
- **Tie Dyer**: the colony worker who fills orders.
- **Dyed thing**: a hemp block, rug, couch, bed or shirt carrying a tie-dye look.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a colony with the right studies, a player can order tie-dyed hemp and have it in the warehouse within one in-game day of supplies being available.
- **SC-002**: 100% of colours the Shack offers are ones the colony has researched, and no player gains a caught or discovered colour from anything the Shack makes or shows.
- **SC-003**: Every pattern and colourway combination offered can be made, placed, mined and re-placed with its look unchanged, across a save and reload.
- **SC-004**: The 8 patterns are told apart at a glance by a first-time viewer at normal play distance.
- **SC-005**: With all patterns, colourways and item kinds available, the game's loaded block and item count grows by no more than a few dozen entries (no per-combination registration).
- **SC-006**: Four Twenty starts and plays without MineColonies, with no errors from the Shack's data.

## Assumptions

- Spec 017 (the Weed College) is merged or released together with this one; its Textiles studies and colour research are the inputs.
- Plain hemp blocks, rugs, couches and beds already exist in the 80 colours and natural; the Shack dyes plain (natural or white) ones.
- Dye supplies are things the colony already makes (buds, vials of terpene, water and so on); exact costs are set in planning.
- Colonist shirts are cosmetic: no stat, happiness or job effect.
- The order window follows the College board's look and permission rule.
- Pattern names are proposed in planning and approved by the user, as the College's study names were.
