# Research: The Tie Dye Shack

Decisions for spec 018. Each has the decision, why, and what else was weighed. Facts about MineColonies and Four Twenty were read from the code on 2026-11-28.

## R1. How a tie-dye look is stored and drawn

- **Decision**: one new block per item kind, with the **pattern in the block state** (8 values) and the **colourway in a block entity** (and in the item's data component). A pattern is drawn as **four colour slots**: each pattern texture is four greyscale masks, one per slot, each a cutout quad layer with its own tint index (0 to 3). The block's colour handler reads the colourway from the block entity and tints slot *k* with the colourway's *k*th colour (2- and 3-colour colourways repeat colours across slots).
  - Items carry a `fourtwenty:tie_dye` data component (pattern, colourway). Item models pick the pattern by an item property; the item colour handler tints from the component.
  - The block entity sends its colourway to clients and asks for a re-render when it changes.
- **Rationale**:
  - **State counts** (counted from the blocks): a rug has 8 connection booleans (256 states), a couch 80 (facing, stair shape, two arms), a Fancy Bed 1,536 per wood (facing, part, occupied, 16 blankets, 6 shapes). Pattern × colourway in the state would be ×128: 32,768 states for the rug alone. The pattern alone (×8) keeps every block under about 2,100 states.
  - The masks are disjoint: each pixel belongs to exactly one slot, the others are transparent. With cutout rendering (no mipmaps) the discarded pixels write no depth, so layers in one plane don't z-fight.
  - Registered entries stay few (SC-005): see R2.
- **Alternatives**:
  - A block per combination: about 640 per kind; rejected by the spec.
  - Both in the block state: too many states for rugs, couches and beds.
  - A block-entity renderer drawing every block: too slow for walls of hemp.
  - Baked textures per combination (128 per face) with a custom model: works, but 4 slot masks per pattern are 16× less art and recolour for free.
- **To verify first (M1 spike)**: a colour handler that reads the block entity during chunk meshing, with the game's own renderer and with Embeddium if the pack carries it. Fallback: a custom baked model fed the colourway through NeoForge model data, colouring its quads itself.

## R2. The tie-dye blocks

- **Decision**: `tie_dyed_hemp` (a full block), `tie_dyed_rug` (a `RugBlock` that joins only neighbours with the same pattern and colourway), `tie_dyed_couch` (a `CouchBlock`), and `tie_dyed_<wood>_bed` for the 10 bed woods (a Fancy Bed whose blanket is tie-dyed: the `blanket` colour property is replaced by `pattern`). About 13 blocks, their items, and one shirt item.
  - The plain inputs are the existing natural pieces: Natural Hemp, the natural rug, the natural couch, and any Fancy Bed of that wood.
- **Rationale**: beds are one block per wood because the frame is the wood's texture. The rug's join check reads the neighbour's block entity, and the placement check reads the item in hand, so joins are right from the first placement.
- **Alternatives**: one bed block with the wood in the block entity: the frame texture can't be tinted, so it would need 10 models chosen per wood anyway.

## R3. The 8 patterns and 16 colourways

- **Decision**:
  - **Patterns**: one per Textiles study, in tier order, drawn by `tools/generate_tie_dye.py`: 4 slot masks per pattern for a block face, for a rug, for a couch's cushion faces, for a blanket, and for a shirt. Edges between slots are dithered so they read as dye bleeding, not as hard lines.
  - **Colourways**: 16 curated palettes in a data file, `data/fourtwenty/tie_dye/colourways.json`. Eight use only the 16 common colours (Chemistry tier 1), so a young colony has choices; eight use hemp colours. At least one uses only the neutrals (FR-008).
- **Rationale**: matches the 8 × 16 plan of 30 Oct. A data file lets a server pack add colourways.
- **Names**: proposed in data-model.md; the user approves them (Principle V).

## R4. Orders: the Shack's own window

- **Decision**: the Shack has an **Orders** tab: pick a pattern (unlocked ones), a colourway (ones the colony can make), an item kind, and a count. The queue below shows each order, with a cancel ×.
  - A payload, `fourtwenty:tie_dye_order` (ADD, CANCEL), sent only with `hasChannel`. The server checks `MANAGE_HUTS`, a Shack at the position, the pattern's study, the colourway's colours and the item kind.
  - Orders are saved per colony in `TieDyeSavedData` (the pattern of `CollegeSavedData`).
- **Rationale**: the user's answer (clarify Q2: A). It matches the College's board: same permission rule, same look.
- **Alternatives**: MineColonies crafter requests, which can't carry a pattern and colourway choice.

## R5. The Tie Dyer's work

- **Decision**: like the Weedologist. For the order at the head of the queue, the Tie Dyer:
  1. requests a batch's plain items and dye supplies through colony requests;
  2. **dips** at a dye vat (a water cauldron inside the hut);
  3. **hangs** at a drying rack (the hut's Curing Racks);
  4. delivers the finished items straight to the closest warehouse, or into the hut with a pickup request if it's full (as the College's seeds).
  - Batch size grows with level (4, 8, 8, 16, 16).
  - Dye supplies per batch: 1 Cured Bud for each colour of the colourway, and 1 Glass Bottle. All numbers are config.
- **Rationale**: physical and visible (FR-011), and it reuses the College's request and delivery code. The colour comes from what the colony knows; buds and bottles stand for the dyeing. Vials can't be the dye: the colony can know a colour no player has caught, so no one could make its vial (017 FR-021).

## R6. Colour privacy

- **Decision**: colourway swatches use the same rule as the College's board. A colour the viewer hasn't caught (`TerpeneKnowledge`) shows as a grey "?" swatch. A colourway's name is shown only if the viewer knows all its colours; otherwise it's "An Undiscovered Colourway". Dyed items' names and tooltips name the pattern and colourway the same way. Nothing the Shack does calls `TerpeneDiscovery` or the Infuser.
- **Rationale**: FR-007 and 017 FR-021.

## R7. Shirts

- **Decision**: `tie_dye_shirt`, an item with the tie-dye component.
  - Right-clicking a colonist with it (colony members only) puts it on them; the old shirt goes back to the player. Sneak-right-clicking a colonist with an empty hand takes it back.
  - Shirts are kept in `TieDyeSavedData` by colony and citizen id, and synced to the players tracking the colonist through an optional payload, `fourtwenty:shirt` (as `LiftedPayload`).
  - A render layer on the citizen renderer (as `BoomboxLayer`) draws the shirt's four tinted slot masks on the body and arms. A Lifted Builder's own body renderer draws it too.
- **Rationale**: the user's answer (clarify: A). MineColonies citizens have no free equipment slot for a cosmetic shirt, and their armour slots are used for real gear.

## R8. Textiles studies and the College

- **Decision**:
  - The 8 Textiles studies get their pattern names and costs: white wool and Cured Buds, growing by tier as now. They give `pattern: <id>` instead of a placeholder.
  - `CollegeFeatures.present("tie_dye")` already turns true once a block `fourtwenty:tiedyeshack` exists, so the hut takes that id.
  - The College's Handbook page names the patterns.
- **Rationale**: the hooks 017 left are used as they are.

## R9. The player's route (Principle I)

- **Decision (approved by the user, 2026-11-28)**: a **Dye Vat**, a plain Four Twenty block that needs no MineColonies.
  - The player puts in a plain item and **one terpene vial for each colour** of a colourway, then picks a pattern.
  - Since vials come from the Terpinator, and the Terpinator makes only colours the player has caught, this is the player's own-colours route.
  - All 8 patterns are open at the Dye Vat; the colony's patterns come from the College.
- **Rationale**: Principle I says every feature must be usable by a player alone and colonists must not be the only way. The 017 plan noted that tie-dye's player route was 018's to give.
- **Alternatives**:
  - Ask the user for an exception to Principle I (the Shack is the only route).
  - Player tie-dye at a crafting table, with a vial-per-slot recipe: no pattern choice without many recipes.

## R10. Levels and the hut block

- **Decision**:
  - Five levels by `C:\Users\Ignus\sensi-heights\tools\design_tie_dye_shack.py`: a dye shed growing to a bright yard of drying lines, with vats and racks per level. Signature door from level 3; doors saved closed; MineColonies hut name `tiedyeshack`.
  - The hut block is a diorama: a small shed with tie-dye hanging out front (`tools/generate_tiedyeshack_hut_model.py`, `hut_model_check`).
- **Rationale**: as the College (017 R11, R12).
