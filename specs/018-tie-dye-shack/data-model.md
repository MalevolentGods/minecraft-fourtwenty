# Data Model: The Tie Dye Shack

## Pattern

A tie-dye design: 4 colour slots, drawn by `tools/generate_tie_dye.py`. Stored as an index 0 to 7 (block state `pattern`, item component).

| # | Pattern (proposed) | Unlocked by (Textiles study, tier) | Look |
|---|---|---|---|
| 0 | Crumple | tex_crumple (1) | blotches from a scrunched cloth: the classic first try |
| 1 | Stripes | tex_stripes (2) | accordion-folded bands |
| 2 | Bullseye | tex_bullseye (2) | rings from a centre pinch |
| 3 | Spiral | tex_spiral (3) | the twisted swirl everyone pictures |
| 4 | Sunburst | tex_sunburst (3) | rays from several pinches |
| 5 | Shibori | tex_shibori (4) | folded diamonds and grids |
| 6 | Heart | tex_heart (5) | a heart folded into the middle |
| 7 | Mandala | tex_mandala (6) | many-ringed, many-pointed symmetry |

The 8 study ids are renamed from `tex_pattern_1..8`; parents stay as now (every study needs Crumple, and Mandala needs Heart).

## Colourway

A named palette of 2 to 4 colour codes from the 80, in `data/fourtwenty/tie_dye/colourways.json`: `{id, colours: [code…]}`; names in lang. Slot *k* takes `colours[k % n]`. A colony can order it once every colour is researched; a viewer sees its name once they've caught every colour.

**Common colours only** (reachable with Chemistry tier 1):

| id | Colourway (proposed) | Colours |
|---|---|---|
| ashtray | Ashtray | black, gray, white |
| campfire | Campfire | red, orange, yellow |
| lagoon | Lagoon | blue, cyan, light blue |
| grape_soda | Grape Soda | purple, magenta, pink |
| roots | Roots | red, yellow, green |
| sunset_strip | Sunset Strip | orange, magenta, purple |
| earth_mother | Earth Mother | brown, green, lime |
| cotton_candy | Cotton Candy | pink, light blue, white |

**Hemp colours** (need accord studies):

| id | Colourway (proposed) | Colours |
|---|---|---|
| terrapin | Terrapin | Seafoam Sesh, Hippie Hot Tub, Big Blue Thought |
| begonia | Begonia | Scarlet Begonias, Sugar Magnolia, Peach Fuzz Daydream |
| grandpas_couch | Grandpa's Couch | Mustard on the Couch, Dad's Recliner, Army Surplus Jacket |
| deep_space | Deep Space | Dark Star Indigo, 3 A.M. Deep Thoughts, Space Cadet Sky, Cloud Watching Blue |
| forest_floor | Forest Floor | Kind Bud Green, Forest for the Trees, Everything Returns to Soil |
| sherbet | Sherbet | Key Lime Contact High, Peach Fuzz Daydream, Lavender Laughing Fit |
| last_light | Last Light | Last Light at 4:20, Red-Eye Flight, Ego Death Violet, The Void Stares Back |
| smoke_and_mirrors | Smoke and Mirrors | Touch of Grey, Soundcheck Fog, Rolling Paper White |

## Tie-dye look (`fourtwenty:tie_dye` component)

`{pattern: 0..7, colourway: id}`. On every tie-dyed item. Placed blocks keep the pattern in their state and the colourway in their block entity, and give both back when mined.

## Tie-dyed blocks and items

| Id | Kind | Made from (plain) |
|---|---|---|
| `tie_dyed_hemp` | full block | Natural Hemp |
| `tie_dyed_rug` | rug, joins same look | natural rug |
| `tie_dyed_couch` | couch | natural couch |
| `tie_dyed_<wood>_bed` × 10 | Fancy Bed, tie-dyed blanket | that wood's Fancy Bed |
| `tie_dye_shirt` | item, worn by colonists | white wool ×3 (proposed) |

## Order (in `TieDyeSavedData`, per colony)

| Field | Notes |
|---|---|
| `id` | unique in the colony |
| `pattern`, `colourway`, `kind` | what to make |
| `count`, `made` | ordered, finished so far |
| `by` | the player's UUID |
| batch state | the current batch's supplies fixed and delivered (as the College's head study) |

The queue is worked head first. An order can be cancelled until its current batch has supplies in it; finished items already delivered stay delivered.

## Shirt (in `TieDyeSavedData`)

`colony id + citizen id → shirt ItemStack`. Removed when the citizen dies or leaves the colony (the shirt drops at the citizen).

## Tie Dye Shack building

| Level | Tie Dyers | Batch | Vats / racks (proposed) |
|---|---|---|---|
| 1 | 1 | 4 | 1 / 1 |
| 2 | 1 | 8 | 2 / 2 |
| 3 | 2 | 8 | 2 / 3 |
| 4 | 2 | 16 | 3 / 4 |
| 5 | 2 | 16 | 4 / 5 |

Activity: `NO_WORKER`, `IDLE`, `WAITING` (with missing supplies), `WORKING`, `PAUSED` (with reason: no vat, no rack).
