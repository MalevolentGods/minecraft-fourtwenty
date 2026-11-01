# Changelog

All notable changes to the FourTwenty mod will be documented in this file.

## [0.17.0] - Moving On Up

### Added
- **Residence Fancy:** a second Residence for Sensi Heights, five levels on the same hut.

### Fixed
- Roads stay visible under and on top of glass, leaves and other see-through blocks.

## [0.16.1] - Old Huts Welcome

### Fixed
- Dispensaries built from the old FourTwenty pack no longer fail to load.

## [0.16.0] - The Construction Crew

### Added
- **Construction Blunt, Spliff and Cannon:** bench upgrades that make 3, 4 and 6 times as many blocks.
- JEI fills a Construction bench like a crafting table.

### Changed
- Construction benches need power, or a high player near, and every coloured block counts.
- The Dispensary lives in Sensi Heights. The old FourTwenty pack is gone, and huts built from it carry on from Sensi Heights.

### Fixed
- The Construction Joint no longer hides the block under it.

## [0.15.0] - Boundless Inspiration

### Added
- **Weed processing:** mature crops give Rough Buds. Trimming makes Trimmed Buds and is where seeds come from, at a chance by strain family (Landrace 50%, Neutrals 25%, Warm 15%, Green 10%, Cool 5%). Curing makes Cured Buds, the existing Weed Bud under a new name. All chances, times and costs are in `fourtwenty-processing-server.toml`.
- **Trim Tray and Curing Rack:** tier 1, by hand, with no power. The tray stands at waist height.
- **Powered machines:** Snip Station, Bud Barber and Trim Reaper; Dry Box, Cure Closet and The Humidor. Hoppers and pipes feed the top and sides and empty the bottom.
- **Boundless Inspiration, up to Rip:** the Hookah now makes BI while it burns, and upgrades to the Double Hose and Big Bertha; Stash Jar, Stash Box and Mother Stash; Hemp, Rolled and Glass Wicks; the Hit Box. Too much power burns a Wick out into ash; a block on too high a tier overloads and stops, and never breaks.

- **Grow Ops:** Closet Grow, Grow Tent, Hydro Bed, Aero Pod and Cosmic Kush Pod, 1.5 to 8 times as fast, upgraded in place. The top two give bigger harvests and replant.
- **Think Tank:** research with no colony, using Grow Ops as test plots. Sixteen tier 1 studies teach the Minecraft colours.
- **Blaze tier:** the Grand Hookah, The Vault and Gold Wick.
- **Rosin Rig:** refines Cured Buds into Kief, Hash, Resin, Rosin and Oil, which burn longer in a Hookah and hit harder in a pipe.
- **Construction Joint:** a bench that makes twice as many hemp blocks, lamps, rugs and roads.
- **Transport Tubes:** one tube, no tiers. Any number of machines share a network and items run both ways. A network into an unloaded chunk stops its machines.
- **Pollination and the Buzz Bot**, biome strains, and the Golden God.
- A flowering crop grows buds in its strain's colour, and Jade names the strain.
- **Guard Tower 1-5** in the Sensi Heights pack.
- **Hybrids:** Pheno Chambers, 22 new strains, and seed traits.

### Changed
- Hookahs hold 64 buds and only start themselves when something on their Wicks needs power. The Light button starts a session any time.
- Smoking shows one badge of what was smoked instead of a row of effect icons. Hover over it in your inventory for the list.
- Hookahs burn charcoal instead of flint and steel, lava or blaze powder, for one minute a burn.
- A Budtender loads one Hookah session per 10 minutes, one bud at a time.
- The Terpinator and Inspiration Infuser now need BI, or a high player within 21 blocks. Their recipes and outputs are unchanged.
- Rough buds can't feed a Dispensary: colonies need cured buds supplied by a player until the Bud Barn.

### Fixed
- Domum roof tiles no longer flicker when made from glass.

### Removed
- The 5% bonus seed roll on mature crops.

## [0.14.1] - The Warehouse

### Added
- **Sensi Heights pack v84-v85:** Warehouse 1-5 (21x13x35) and Warehouse Courier 1-5. Racks grow 10, 18, 30, 42, 102 and no upgrade replaces one. Each Warehouse holds 2, 3, 4, 6, 8 Courier's Huts, unbuilt until a builder is sent. The crafting table and architect's cutter stay in the same spots on levels 3-5.

### Changed
- The Sensi Heights importer accepts a nested hut's link to its level 0 blueprint.

## [0.14.0] - The Hotbox

### Added
- **Hotbox:** a creative-only block that keeps the 3x3 chunks around it running for an hour after the last player leaves them. It starts by itself when a player enters the area, lets go at once if nobody is online, and holds nothing across a restart. Light level 15, with a green area outline while holding the Build Tool or a Hotbox, and a green area on JourneyMap.

### Fixed
- Infuser and terpene sync packets are sent only to connections that have the channel.

## [0.13.2] - The Courtyard

### Changed
- **Sensi Heights pack v82-v83:** every Town Hall level grows to a 31x18x15 footprint (hut at (6,2,7)); level 5 gains its courtyard (two couch circles in Yellow Brick Road and grass, 16 outdoor seats); levels 1-4 get the space as cleared ground; Town Hall 4 reworked (paved road floors and stairs, Forest for the Trees concrete, Cloud Watching Blue glass, panels, Key Lime lamps).

## [0.13.1] - The Town Hall

### Changed
- **Sensi Heights pack v76-v81:** Town Hall levels 2-5 (the user's builds from generated drafts that grow level 1's apse and garden into a rotunda and atrium); every level shares a 25x18x15 footprint with the hut at (6,2,7); the temporary `wip` folder is gone.

## [0.13.0] - Strains and Inspiration

### Added
- **Strains:** a `fourtwenty:strain` component on seeds, buds and joints (Landrace = none) and a crop STRAIN state kept through growth and harvest; 16 bred strains in four families with per-strain effects (quiet, plus auras for Mossy Philosopher and Third-Eye Tonic), coloured smoke and screen tint; hue-shifted bud/joint icons; tooltips name the family. The pipe loads up to 16 buds of one strain like ammo.
- **Inspiration Infuser** (Steel and Lab): a Kubelka-Munk colour-matching game over 80 colours (16 Minecraft + 64 hemp) with server-checked catches, per-player persistent pool, inspiration map, twins, an Infuse tab for breeding strain seeds, and Drops x1/x4/x8.
- **16 Minecraft-colour terpenes** (`terpene_mc_<dye>`, named once caught) with hemp, concrete, concrete powder and Minecraft-themed rugs.
- **Colour drift** in place of nausea; every weed inspires the Minecraft colours; a hotbar hint when a strain has nothing left to show.
- **Hookah model:** stem, charcoal tray, clay bowl with glowing coals and smoke, and a hose, on the painted cabinet, inside the block.
- **Joint in first person:** raised to your lips while you smoke.

### Changed
- **Terpinator:** production only (pick a caught colour, feed it weed: 2 buds a vial), new Steel and Lab models; old machines keep their buds and drop their contents. The codex guide screen is gone.
- **Lamps:** the 16 dye lamps, inverted lamps and trims use the Minecraft-colour terpenes instead of vanilla dye.
- **Hookah:** the weed bong is named the Hookah (id unchanged).
- **Sensi Heights pack v72-v75:** Dispensary 5 in FourTwenty whites; Dispensary 3-5 and Tavern 3 re-textured panels; a `wip` folder with draft Town Halls 2-5.

### Fixed
- Strain icons have solid edges (no see-through sides in the hand); your own smoke no longer fills the top of your view.

## [0.12.0] - Pot Pots and Lit Roads

### Added
- **Pot Pots** in the 64 terpene colours: 3 of a terpene concrete in a V round a block of dirt make one. A weed seed planted in it makes it a Potted Pot Pot of that colour; the plant grows through the crop's eight stages in the light (bone meal works) and, mature, is harvested with a right-click like the field crop (its loot, one seed kept back), then starts again. Each colour's pot is its own design: body x rim x pattern = 64 (`tools/generate_pot_pots.py`).
- **Builders and Potted Pot Pots** (PottedPotPotPlacementHandler): a builder pays the plain Pot Pot of the same colour (the seed is free) and accepts any plant age as built.
- **Roads give off light by tier:** Cobble Path 6, Paved Road 10, Yellow Brick Road 15, with no visible glow.
- **Rugs redesigned:** a themed design per colour (florals, leaves, ocean, night sky, plaid and more), drawn as smooth anti-aliased shapes at 4x resolution (128x128 a quarter) with a finer weave (`tools/generate_rugs.py`, `tools/rug_designs.py`).

### Fixed
- **Furniture on roads:** our blocks that aren't full blocks are drawn 1/16 lower when they stand on a road's full top (SunkModel), so they sit on its surface instead of hovering a pixel above it.
- **Road lighting:** road tops keep smooth lighting even though roads give off light (vanilla draws light-emitting blocks flat), so lit roads blend instead of reading as a grid.
- **Potted plants** aren't shaded dark (no ambient occlusion on the pots, as with vanilla's flower pot).

### Changed
- **Sensi Heights pack v65-v71:**
  - Town Hall level 1 (the user's build).
  - Pot Pots replace every vanilla potted plant.
  - Dispensary levels 1-5 are redecorated so racks only accumulate across upgrades.
  - Tavern road tiers: Paved Road at level 2, Yellow Brick Road at level 3.
  - The user's Restaurant 4-5, Tavern 3 and Dispensary 4-5 edits.

## [0.11.0] - Roads Round Every Building

### Added
- **Hemp and terpene concrete are Domum Ornamentum materials.** Natural Hemp and the 64 hemp colours join Domum's default material tag, and the 64 terpene concretes join its concrete tag (which lists vanilla's by name). Hemp glass already came in through `c:glass_blocks`. If Domum isn't installed, the tags go unused.

### Fixed
- **Furniture on roads:** a rug or a couch on a road counted as covering it, so the road dropped its top and you could see through the gap under it. Only a block solid over the whole quarter, top to bottom, covers a road now.
- **Stacked roads seen from below:** where a road block stands on another road block, it now always draws the pixel down to the block below. Before, a road beside the upper block could hide that strip, and the gap showed through from below.
- **Hemp glass in Domum Ornamentum:** pieces made of hemp glass showed blank item icons. Domum takes a material's faces by render layer, and the glass models named their own render type, so they gave Domum no faces. The glass and panes are now set translucent in code, the way vanilla's stained glass is.

### Changed
- **Sensi Heights pack v63:** every building from level 3 up has a road border two blocks thick on the ground: Cobble Path at level 3, Paved Road at level 4 and Yellow Brick Road at level 5. Dirt paths became road too. The border goes round decorative ground, such as the Workshop's cherry ring and rainbow lamps and stone paving. Buildings grew where the border didn't fit, every level alike.
- **Sensi Heights pack v64:** the user's Dispensary level 5 rework, with a yellow brick roof deck, hemp floors, and couches and rugs through the lounge and on the roof. Its seats are tagged.

## [0.10.0] - Hemp Roads

### Added
- **Roads** in three tiers: Cobble Path, Paved Road, Yellow Brick Road (HempPathBlock), with **road slabs and stairs** (HempPathSlabBlock, HempPathStairBlock). Roads stand 15/16 high like a dirt path, with light occlusion shaped to it, so a lamp beside a road shows a strip of light. Walkers (players, mobs, colonists) are 5, 10 and 15% faster on all three blocks of a tier: the block speed factor is solved for that steady-state gain, since vanilla's factor compounds with ground friction. Slabs are sized to the road (bottom 0-7/16, top 7-15/16, double 15/16), and stairs have 7/16 treads and 15/16 backs, so a ramp runs road to road with no lip.
- **Roads read as one bordered surface wherever their faces meet** (RoadShape, drawn by the dynamic client model RoadModel, so the blocks carry no join properties). The rule is: faces side by side are one space, and a border runs wherever the next face rises, drops, is covered or is another block. Each quarter of each block is handled separately.
  - **Tops:** these join any road, slab or stair of the tier at the same height, whichever way it faces. Borders mitre where they meet, and inner-corner notches carry them round where two joined edges meet a gap. Yellow brick road rosettes sit where a border turns a block corner.
  - **Stairs:** these are drawn as separate steps. Flights, corner stairs, rings and stairs set into a road all join up.
  - **Undersides:** these join by their own heights.
  - **Step faces:** a face from a lower step of the road up to a higher one, inside a block or across two, is a bordered strip. Strips run on into each other and round inside and outside corners.
  - **Side walls:** these are the cut through the road, bordered round their outline, with top edging at their own height. Corner bands run up a wall's end where it turns a corner, meets another block or rises above the wall beside it.
  - **Stacks:** these read as one wall. A road block standing on a road block draws its faces down the pixel between them, while other blocks underneath leave that gap open. Under any other solid block, a road fills the pixel itself.
- **Road textures:** 64x64 tiles for every arrangement of borders and notches (tops, undersides, step faces and walls) come from `tools/generate_paths.py`.
- **Tests:** `PathGameTests` checks the speed on a walking mob, the light strip, slab and stair heights, joins, covered roads, step faces, undersides and wall bands. It also places every pair of road shapes side by side both ways and checks that every join is two-sided and no border stops short at a joined edge. `render_block_model.py --fit 1` frames a whole scene.

### Changed
- Sensi Heights pack v62: placeholders used the way MineColonies' Colonial style uses them: every building's area is cleared above ground from level 1 (so colonists can walk anywhere in it), with solid fill in and under the ground layer (the level 1 Builder's Hut and Residence keep their terrain-keeping quick builds).
- Sensi Heights pack v61: the Tavern's and Restaurant's red wool chairs are Heart of the Matter couches (joined into sofas, on the floor of their design), and floor fixes (parquet and log grain, cherry trim facing).

## [0.9.0] - Hemp Couches and the Restaurant

### Added
- **Couches** in the 64 terpene colours and Natural Hemp (CouchBlock): a hemp cushion and back with softened edges, arm rests and dark oak legs. They join each other like stairs (vanilla's stair shape rule: straight, inner and outer corners), with arms only on a straight couch's open ends; colours join too. No sturdy faces, so fences, walls and panes don't join them. 3 hemp stairs over 2 sticks make 3. Players sit with an empty-handed right-click (an invisible, unsaved seat entity; one per couch; sneak, or breaking the couch, stands them up). `/sit` (any player) sits down where you stand: on a couch's cushion, or on the floor, a step or a slab. MineColonies: a couch a player sits on counts as taken for colonists, and a colonist sitting on a couch faces out of it (`CouchSittingMixin`); MineColonies already seats them at the cushion height. Models from `tools/generate_couches.py`; game tests in `CouchGameTests` (48 in all).

### Changed
- Sensi Heights pack v60: the Restaurant (cook), all 5 levels: a timber dining hall with a sunken seating pit that grows into a polished restaurant with a covered patio, a big sunken dining pit, a roof terrace and an upper dining room, topped at level 5 with cherry trim and a lamp-lit skylight.
- Sensi Heights pack v56: every seat is tagged (wall benches and chairs in the Residence and Tavern 1), and Builder's Hut level 4's front door is saved closed.
- Sensi Heights pack v54: Workshop levels 3-5 have a Terpinator in place of the middle pillar (and no longer need Farmer's Delight), terpene-coloured lamps in Workshop 4-5 and Residence 4-5, a reworked Residence 5 (terpene concrete, FourTwenty hemp beds including a king, rugs and a side table), Residence 1 and Tavern 1 clear a path to their front doors, and Tavern 1's loft is opened up.
- Sensi Heights Tavern level 3: terpene rugs in place of carpets (through the lounge, the bar and every upstairs room), diorite floors and roof, panel ceilings with lamp trim, a deepslate hearth, and rearranged guest rooms (still 36 indoor and 8 outdoor seats).
- `tools/import_sensi_heights.py` leaves out blueprints listed in `EXCLUDE` (pack decorations that use third-party characters).

## [0.8.0] - Hemp Beds, the Terpinator and the Hemp Palette

### Changed
- Lamp recipe: 3 glass, a weed bud and the dye (shapeless) make 4 lamps, instead of 6 glass round a torch.
- Lamp Trim: 1 lamp (plain or inverted) makes 4, instead of 1 inverted lamp making 8.

### Added
- **Hemp blocks and the Terpinator.** 64 coloured hemp blocks plus Natural Hemp, each with stairs and slabs (FourTwenty's own seamless 64x64 woven texture). The Terpinator presses weed buds into resin, refines resin into four base terpenes (a method dial: heat, cold, pressure, time) and blends them: two bases make one of 16 accords, an accord and a third base one of 64 final terpenes, each dyeing 8 hemp blocks its colour. 84 named terpenes with flavour lines, discovered per player (names hidden until you make one or are given one), with a live preview, a guided first session, a guide map and discovery toasts. Data: `tools/terpenes.json`; generators: `tools/generate_terpenes.py`, `tools/generate_terpene_textures.py`, `tools/generate_terpinator.py`.
- **Rugs join into larger rugs**, each colour with its own pattern: a rug knows its eight same-colour neighbours (RugBlock), and each quarter of its model is an outer corner, an edge, an inner corner or interior, so the border runs only round the outside of the whole rug and the pattern flows across the inside (`tools/generate_rugs.py`).
- **Concrete powder, stained glass and glass panes in the 64 terpene colours.** Concrete is now one flat colour; the specked texture moved to new concrete powder (vanilla ConcretePowderBlock: sets in water; 3 sand, 3 gravel, 2 weed buds and the terpene make 4; MineColonies' Concrete Mixer makes powder and concrete). The 8-concrete re-dye recipe is gone, as in vanilla. Glass and panes recoloured from Classic Faithful 64x (Faithful License, credited). Palette lamps use the dye-colour lamps' own texture, recoloured.
- **Concrete, rugs and lamps in the 64 terpene colours.** Concrete (8 + a final terpene), rugs (2 hemp make 3; inset a pixel with rounded corners so the floor shows round them; Natural Hemp rugs too) and lamps, inverted lamps and trims (3 glass, a bud and the terpene make 4; halos in their own colour). FourTwenty's own 64x64 textures from `tools/generate_palette_blocks.py`. Lamps now carry their halo colour, so any colour works.
- Terpinator model: the solid machine renders as cutout and only the glass and terpene as translucent (a NeoForge composite), fixing flicker and see-through sides; a simpler valve wheel.
- Terpinator: Start/Stop for each stage (progress kept), the refiner starts only with an empty output and locks its dial while running, a TrueType font (Atkinson Hyperlegible, OFL), and a detailed model with 256x256 textures (walnut cabinet, numbered brass ports, a method dial in the four base colours, a press crank and hopper, a glass still, a coil condenser, a resin tank and a pressure gauge; the terpene and a status lamp glow while it works).
- MineColonies builders place a king bed's extension for free (it has no item).
- Sensi Heights Tavern levels 2-3 use FourTwenty hemp beds (a king in level 3) and side tables instead of Handcrafted ones.
- **Hemp beds** in the same ten woods, with FourTwenty's own 64x64 textures (grained frame wood, panelled boards, linen, a pillow, quilted blankets): tall head posts and a panelled headboard, a footboard, a mattress, a gently tilted pillow, the sheet folded back over a draped hotel duvet and a matching bed runner across the foot (16 colours: every colour is its own item in the creative tab, a bed + a dye crafts that colour, right-click a placed bed with a dye to recolour it, and breaking it keeps the colour; crafted beds are lime), turned back while someone sleeps. Real beds (BedBlock, `#minecraft:beds`), so colonies count them. 3 weed buds over 3 planks make 1. Two side by side join into a double, three into a king one block longer (a placement ghost shows the king, red when there's no room, in which case the bed stays a single); each column is still its own bed for players and colonists, and breaking any part breaks the whole bed.
- **Side tables** in ten woods (every vanilla wood but bamboo): a nightstand with a drawer front, a brass knob, slim stripped-log legs and a low shelf. Decoration; 3 slabs, 4 planks and a weed bud make 2. FourTwenty's own model on vanilla textures.
- Sensi Heights: the Tavern (levels 1-3). Level 3 uses Handcrafted beds and tables.

## [0.7.0] - The Budtender's Guide and the Builder's Hut

### Added
- **The Budtender's Guide:** a citizen-window tab (bud icon) on every Budtender with 17 topics covering the whole mod, written in his voice. The text lives in the lang file (`fourtwenty.guide.<topic>.title` / `.text`).
- **Sensi Heights Builder's Hut, levels 1–5** (`fundamentals/builder1`–`5`): an open shelter that keeps the terrain, a timber cabin, a stone-and-timber house with a storeroom, a polished house with a roof terrace and the second story begun, and the full two-story house with a front yard. Racks: 1, 3, 8, 18, 24.
- **Builder cost:** the Domum Ornamentum architect's cutter costs one cobblestone.

### Changed
- Lamp and lamp trim halos skip ladders: no halo side is drawn against a ladder (as against another lit lamp); the glow still spills onto other neighbouring blocks.
- Bundled Dispensary blueprints trimmed from 16 to 11 blocks tall (the top layers were keep-terrain placeholders only).
- Sensi Heights Residence: keep-terrain under the front steps and bare eaves; huts record their own pack and path.
- `tools/import_dispensary_blueprints.py --lamps-only` also rewrites lamp tile entity ids, and the Sensi Heights validator rejects foreign tile entities.

### Fixed
- Workshop 4–5 shipped 59 lamps with ProjectRed tile entity ids.
- README and guide: the Dispensary's seats (6/6/6/9/9, plus 17 rooftop seats at level 5) and space (11×24, 11 tall).

## [0.6.0] - Dispensary service, affinity, bongs and lamps

### Added
- **Restaurant-style Dispensary service:**
  - The Budtender walks to customers anywhere in the shop.
  - Player sales: right-click the Budtender with an emerald for a server-driven menu. It is free with hut permission; otherwise joint 1, cookie 1, brownie 2 and pipe 3 emeralds.
- **Dispensary menu by level:** joints (1), weed cookies (2), pipes (3) and weed brownies (4).
  - Production is balanced: one small batch at a time for the least-stocked item.
  - A raw bud reserve is kept for bong sessions and the hookah.
  - The status tab lists each menu item and any missing ingredient, and its icon is the bud art.
- **Take-away by home level:**
  - Levels 1–2 consume at the shop, 3–4 take a pipe or 4 edibles home, and 5 do both.
  - Carried joints, edibles and pipes are used anywhere when due. A pipe holds 4 doses.
  - Missing take-aways fall back with a complaint.
- **Colonist affinity:**
  - The first exposure (hookah area, secondhand smoke, a dose) rolls a preference and a frequency step.
  - Drift: 10% per dose and per daily hookah session, 75% of it upward.
  - A two-day tolerance break follows reaching the top step.
  - The happiness tab shows the profile.
- **Glass bong:** a placeable block; right-click with a bud for the pipe's effects.
- **Six specialty bongs** (glass bong upgrades) with one-day buffs; the Phantom bong grants flight.
  - Level 5 smokers take their shop session at the job-matching specialty bong: +5 to its two skills for a day, +10 on a primary-skill match.
- **Hookah fuels:** flint and steel (30 s), lava bucket (45 s) and blaze powder (60 s).
  - The "Hookah session" happiness bonus applies within 6 blocks.
  - The Budtender tends the front hookah: free water, fuel, buds while customers are around, and requests for fuel and buds.
- **Lamps:** Lamp, Inverted Lamp and Lamp Trim in 16 colours, with ProjectRed's illumar textures and halo.
  - Halos merge between touching lamps.
  - Lit lamp items glow in inventories.
- **The Sensi Heights style pack** is bundled (`blueprints/fourtwenty/sensiheights`): supply camp, Workshop 1–5, Residence 1–5 and Dispensary 1–5.
  - `tools/import_sensi_heights.py` imports and validates it from its own repository.
- **Bundled Sensi Heights Dispensary** (levels 1–5) and a 3D two-block counter hut block.
  - `tools/import_dispensary_blueprints.py` re-imports and validates the levels, and converts ProjectRed lamps and CB covers to FourTwenty lamps.
- **Builder costs:** froglights cost a sea lantern, and CB Multipart microblocks cost their source block at 1 per 8 covers.
- **Op-only `/fourtwenty` testing commands:** status, expose, forget, due, clear, break, drift, clock and stock. Also the in-game test playbook.

### Changed
- The Weed Bong is displayed as the **Hookah**. Ids and saves are unchanged.
- **Dispensary demand:** any completed home level qualifies after a first exposure. The save format is now v3; v1 and v2 load, with every colonist unexposed.
- The Budtender no longer requests a courier pickup after every dump.
- FourTwenty potion effects hide their swirl particles.

### Fixed
- Weed crops maintain farmland.
- The hookah block faces its placer.
- The Budtender's bong tending no longer depends on the AI tick landing on a multiple of 20.
- Workers keep carried doses when emptying their inventory.
- Budtenders get no take-away from their own shop.

### Credits
- Lamp textures and halo rendering are adapted from [ProjectRed](https://github.com/MrTJP/ProjectRed) (MIT License, see `licenses/ProjectRed-LICENSE.txt`).

## [0.3.2] - Compatibility candidate

- Target Minecraft 1.21.1 and NeoForge 21.1.218 with consistent packaged version metadata.
- Repair crop and wild-weed loot, remove duplicate seed paths, and provide sustainable farmer replanting.
- Add validated bong automation, paid-charge persistence, legacy recovery, Multi-Piston movement, and complete blockstate models.
- Scope delayed edible effects to the running server and player lifecycle; exclude edibles from citizen food selection.
- Protect FourTwenty crops from Actually Harvest bypassing MineColonies permissions, including area harvests.
- Reject stale post-event crop interactions and preserve seeds when Actually Harvest disables replanting.
- Add isolated pack tooling, dependency inventory, JUnit and native integration fixtures. Full qualification remains subject to the compatibility report's open gates.

### Earlier interface work in 0.3.2

### Added
- **Custom Bong Menu Art**: Beautiful cannabis-themed GUI texture
  - Custom background with cannabis leaf decorations
  - Integrated title design eliminating need for overlay text
  - Professional visual presentation matching mod theme

### Fixed
- **Weed Slot Input Limitation**: Resolved bug allowing unlimited weed bud input
  - Properly enforced 4-bud maximum limit in weed input slot
  - Prevented excess items from being consumed while maintaining count
  - Enhanced container validation for reliable slot restrictions
- **GUI Title Overlap**: Removed redundant title text since it's now part of the artwork

### Technical
- **Slot Validation**: Improved `canPlaceItem` method for stricter input control
- **Container Management**: Enhanced stack size validation per slot type
- **Screen Rendering**: Cleaned up label rendering to prevent text overlap

## [0.3.1] - GUI System Overhaul

### Changed
- **Weed Bong Interface**: Complete redesign from block state to GUI-based system
  - **Brewing Stand-like GUI**: Right-click bong opens inventory interface with 3 slots
  - **Slot System**: Dedicated slots for weed buds, water bottles, and blaze powder
  - **Automatic Processing**: Auto-lights when all required items are present
  - **Simplified Interaction**: No more manual lighting - just add items and get effects
  - **Blaze Powder Fuel**: Replaces flint & steel for more intuitive brewing stand parallel
  - **Area Effects**: Maintained 4-block radius group session benefits
  - **Visual Feedback**: Clean interface without manual interaction buttons

### Technical
- **Menu System**: Added WeedBongMenu with Container interface implementation
- **Client GUI**: WeedBongScreen for rendering and interaction
- **Registry Updates**: ModMenuTypes for custom menu registration
- **Inventory Management**: Full item persistence and automatic consumption

## [0.3.0] - Stationary Equipment

### Added
- **Weed Bong Block**: Stationary smoking apparatus with advanced mechanics
  - Multi-state block with visual indicators for water, weed level, and lit status
  - Interactive system using water buckets, weed buds, and flint & steel
  - Area-of-effect benefits for nearby players (4-block radius)
  - Enhanced effects for group sessions (stronger and longer-lasting)
  - 5-second cooldown system to prevent spam usage
  - 15-second burn duration per lighting
  - Automatic weed consumption (1 bud per burn cycle)
  - Campfire smoke particle effects when active
  - Sound effects for lighting, bubbling, and ambient fire
  - Crafting recipe using glass blocks and water bucket
  - State persistence (remembers water/weed levels when reloaded)

### Changed
- **Group Gameplay**: Introduced social mechanics encouraging communal use
- **Effect Scaling**: Bong provides stronger effects than individual consumption methods

## [0.2.0] - Edibles & Food Integration

### Added
- **Weed Cookies**: Basic tier edible with delayed effects
  - Shapeless crafting recipe (cookie + weed bud)
  - 10-second delayed onset for realistic edible experience
  - 3-minute effect duration (longer than smoking)
  - Provides food nutrition (2 hunger + 2.4 saturation)
  - Moderate effect profile with standard weed benefits
  - Visual and audio feedback during consumption

- **Weed Brownies**: Premium tier edible with enhanced effects
  - Shaped crafting recipe requiring cocoa, wheat, egg, sugar, and weed bud
  - 15-second delayed onset
  - 5-minute effect duration with premium effects
  - Enhanced nutrition (4 hunger + 4.8 saturation)
  - Includes Jump Boost II and Speed I alongside standard effects
  - More complex crafting requirements for balance

- **Delayed Effect System**: Advanced timing mechanism for edibles
  - Server tick-based DelayedEffectManager for precise timing
  - UUID-based player tracking for multiplayer compatibility
  - Overdose prevention (cannot consume multiple edibles while effects pending)
  - Thread-safe operation using ConcurrentHashMap
  - Two-tier system supporting different delay and duration profiles

### Changed
- **Crop Drop System**: Improved sustainability and balance
  - Weed seeds now have 5% drop chance instead of guaranteed drops
  - Immature crops still drop 1 seed for replanting sustainability
  - Implemented direct popResource() drops for reliable functionality
  - Enhanced crop breaking mechanics for better farming experience

### Fixed
- **Loot Table Issues**: Resolved inconsistent loot table loading
  - Switched to direct drop mechanism for guaranteed functionality
  - Documented loot table vs direct drop approaches in development guide
  - Improved crop harvest reliability

## [0.1.0]

### Added
- **Weed Pipe**: New reusable item that provides the same effects as the joint
  - Functions like a tool with 4 uses before breaking
  - Same animation and effects as the joint (regeneration, night vision, luck, hunger, occasional confusion)
  - 5-second cooldown between uses to prevent spam
  - Can be repaired using weed buds in an anvil
  - Premium crafting recipe requiring 3 different wood types, charcoal, and weed bud
  - Does not stack (like other tools)
  - Added to the "Four Twenty" creative tab
- **Smoke Particle Effects**: Both joint and pipe now produce realistic smoke effects
  - Initial smoke when starting to use the item
  - Final exhale effect when finishing
  - Server-synchronized for multiplayer visibility

### Changed
- Nothing yet

### Fixed
- Nothing yet

## [0.0.1] - Initial Release

### Added
- Basic weed cultivation system with seeds and crop blocks
- Weed joints as consumable items with multiple effects
- Wild weed generation in world
- Custom "Four Twenty" creative tab
## v0.5.0 (2026-10-15)

### Feat

- improve player and villager/colonist smoke effect

## v0.4.1 (2026-10-15)

### Fix

- update missing translation for dispensary name and desc

## v0.4.0 (2026-10-15)

### Feat

- add dispensary building and budtender profession phases 5 and 6
- add dispensary building and budtender profession phase 4
- add dispensary building and budtender profession phase 3
- add dispensary building and budtender profession phase 2
- add dispensary building and budtender profession phase 1

### Refactor

- update crop loot and bong automation

## v0.3.2 (2025-07-21)

### Fix

- add missing weed bong gui files

## v0.3.1 (2025-07-20)

### Feat

- add proper bong art
- add stationary bong block with area effect
- add weed edibles with unique effect system
- add reusable weed pipe
- add the ability for weed to spawn naturally in certain biomes

### Fix

- update the bong recipe to match new model
- add missing weed brownie art
- use direct drop mechanism for weed crop loot as workaround
- add language file for weed pipe
- fix crafting recipes and make pipe more difficult
- adjust display values for weed joint to look more natural in first and third person views
- update crop model to use a cross pattern instead of a block

### Refactor

- modify bong to use a custom GUI for interaction
