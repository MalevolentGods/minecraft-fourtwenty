# Data Model: The Weed College

Phase 1 for [plan.md](plan.md). Pure rules live in `domain/college`; saved state in `CollegeSavedData` (research R3).

## Entities

### Study (definition, from data)
| Field | Meaning |
|---|---|
| `id` | e.g. `fourtwenty:bot_dust_devil` |
| `branch` | `botany`, `chemistry`, `glassworks`, `textiles`, `engineering`, `armory` |
| `tier` | 1 to 6 |
| `parents` | study ids, all of which must be done |
| `supplies` | item ids and counts, at most 6 lines; colour studies list per-colour shares (R9) |
| `kind` | `colours`, `strains`, `effect`, `unlock` |
| `gives` | colours (terpene codes), strains (seed batch), an effect id and strength, or a research-effect id |
| `needs` | optional feature id that must exist (`tie_dye`, `weedscout_uniform`); absent = available now |
| `units` | derived: `72 × 2^(tier-1)` |

**Validation (JUnit, and the generator):**
- Ids are unique.
- Parents exist, have a lower or equal tier, and form no cycles.
- Every study can be reached from tier 1.
- A tier-6 study's branch has at least one tier-5 study.
- Supplies are real item ids.
- Colours are real codes.

### ColonyResearch (per colony, saved)
| Field | Meaning |
|---|---|
| `colony` | colony id |
| `states` | study id → `LOCKED`, `AVAILABLE`, `QUEUED`, `IN_PROGRESS` or `DONE` (LOCKED and AVAILABLE are derived on read, not stored) |
| `progress` | study id → units done (kept through pauses, demolition, a worker leaving) |
| `queue` | ordered study ids; the head is the study in progress |
| `supplied` | the head study's supplies already delivered to the hut (so nothing is requested twice or lost) |
| `cost` | the head study's supplies, fixed when it started (after the caught-colour discount) |
| `membersColours` | terpene codes known by the colony's owner or officers (R9) |
| `done` | ordered list of finished studies, with the game day each was finished |

### Weed College (building)
- Level 1 to 5; its scanned stations: Grow Op plots, one Think Tank, one desk.
- Counters for its status window: units worked, studies finished, bad batches.
- Workers: 1 at levels 1 and 2, 2 from level 3.

### Weedologist (job)
- Skills: Intelligence (primary) and Focus (secondary), like the University's researcher. Higher Intelligence lowers the bad-batch chance, within the configured floor.

## State and lifecycle

```
LOCKED ──(parents done, level ≥ tier, feature present)──▶ AVAILABLE ──(queued)──▶ QUEUED
QUEUED ──(reaches head of queue)──▶ IN_PROGRESS
IN_PROGRESS: WAITING (supplies missing: shows which) ⇄ WORKING (a Weedologist in a cycle)
IN_PROGRESS ──(units done = units)──▶ written up ──▶ DONE (announced; effects applied)
QUEUED/AVAILABLE ──(removed before start)──▶ AVAILABLE
IN_PROGRESS ──(College gone or below its tier)──▶ paused (progress kept, reason shown)
```

- **A cycle adds one unit.** A bad batch (chance in config, default 0.1, lowered by Intelligence) adds nothing that cycle: the unit is worked again.
- **Lock reasons, in this order:** needs a later feature (`Waiting for the Tie Dye Shack`); needs a parent (named); needs a College of level N (tier 6: level 5 and every tier-5 study of its branch).

## Rules (domain/college)

- `StudyRules.units(tier)`: 72, 144, 288, 576, 1152, 2304 (from `Studies.units`).
- `StudyRules.cap(level)`: level N allows tiers up to N; tier 6 needs level 5 and the branch's tier-5 studies done.
- `StudyRules.cost(study, membersColours)`: supplies with each already-caught colour's share halved (factor in config).
- `StudyRules.badBatch(roll, intelligence)`: `roll < max(floor, base − intelligence × step)`.
- `ColonyEffects.factor(effect)`: product of the finished studies' factors for that effect (1.0 if none).
- `StudyBoard.available(record, level, features)`: the AVAILABLE set and a lock reason for every other study.

## The board: 63 proposed studies (names for the user's approval)

Tier N needs a level-N College, except tier 6 (level 5, plus all of the branch's tier-5 studies). Supplies are the defaults, tuned in play; all of them live in config and data.

### Chemistry (23): colours, and the colony's Terpinators (power only)
| Tier | Study | Parents | Gives |
|---|---|---|---|
| 1 | **The Neutrals** | – | colony knows white, light gray, gray, black, brown |
| 1 | **The Warms** | – | red, orange, yellow |
| 1 | **The Greens and Blues** | – | green, lime, cyan, light blue, blue |
| 1 | **The Purples** | – | purple, magenta, pink |
| 2 | 8 accord rows: `myr_*` and `lim_*` (shown by accord name to viewers who know it) | the tier-1 group of the accord's family (via `HueFamilies`; myr accords → The Neutrals) | that accord's 4 hemp colours |
| 3 | 8 accord rows: `pin_*` and `lav_*` | as above | that accord's 4 hemp colours |
| 4 | **Lean Extraction** | the four tier-1 groups | the colony's Terpinators draw half their BI |
| 5 | **Full Spectrum** | all 16 accord rows | the colony's Terpinators draw a quarter of their BI |
| 6 | **Master Blend** *(proposed; replaces the "ultimate terpene", see plan)* | Full Spectrum | the colony's Terpinators need no BI |

**Supplies:**
- A tier-1 group takes, per colour, the Think Tank's 8 Cured Buds, 4 Weed Seeds and 1 Glass Bottle, with a caught colour's share halved.
- An accord row takes 16 Cured Buds, 4 terpene vials of its group's common colours, and 4 Glass Bottles.
- The effect studies take 32 Cured Buds, 8 Glass Bottles and 2 Rosin.

### Botany (11): strains to the warehouse, farm yields
| Tier | Study | Parents | Gives |
|---|---|---|---|
| 1 | **Soil Science** | – | colony weed farms: 25% chance of one extra bud a harvest |
| 2 | **Dust Devil** | Soil Science | 4 Dust Devil seeds (desert) |
| 2 | **Snowblind** | Soil Science | 4 Snowblind seeds (snow) |
| 2 | **Bog Butter** | Soil Science | 4 Bog Butter seeds (swamp) |
| 2 | **Canopy Creeper** | Soil Science | 4 Canopy Creeper seeds (jungle) |
| 3 | **Selective Breeding** | Soil Science | farms: 50% chance of one extra bud (replaces Soil Science's) |
| 3 | **The Crosses** | Soil Science | 2 seeds of each of the 8 Crosses |
| 4 | **The Double Crosses** | The Crosses, and the four biome studies | 2 seeds of each of the 6 Double Crosses |
| 5 | **The Ultras** | The Double Crosses | 2 seeds of each of the 3 Ultras |
| 5 | **Master Grower** | Selective Breeding | farms: one extra bud every harvest |
| 6 | **Golden God** | The Ultras, Master Grower | 2 Golden God seeds |

**Supplies:**
- Biome studies take 8 Landrace seeds, the climate's item ×4 (sand, snow block, lily pad, jungle sapling) and 8 Cured Buds.
- Hybrid studies take 2 seeds of each parent.
- Yield studies take Bone Meal ×32 and Cured Buds ×16, ×32 or ×64.

### Engineering (7): colony power and machines (never the Terpinator's or Infuser's speed)
| Tier | Study | Parents | Gives (inside the colony) |
|---|---|---|---|
| 1 | **Bench Habits** | – | trimmers and curers work 25% faster |
| 2 | **Long Burn** | Bench Habits | Hookahs burn 25% longer per bud and charcoal |
| 3 | **Deep Stash** | Long Burn | Stashes hold 50% more |
| 3 | **Grow Light Tuning** | Long Burn | Grow Ops draw half their BI |
| 4 | **Pressure Work** | Bench Habits | Rosin Rigs and Pheno Chambers work 25% faster |
| 5 | **Assembly Line** | Pressure Work | trimmers and curers 50% faster (replaces Bench Habits) |
| 6 | **Boundless Grid** | Assembly Line | Hookahs burn twice as long (replaces Long Burn) |

**Supplies:** Hemp Wicks, Glass Wicks, Resinators, Cured Buds and Charcoal, rising by tier.

### Glassworks (7): bongs at the Glassblower (crafter recipes, R6)
| Tier | Study | Parents | Unlocks for the Glassblower |
|---|---|---|---|
| 1 | **Glassblowing** | – | Glass Bong |
| 2 | **Jackrabbit Bong** | Glassblowing | Jackrabbit Bong |
| 2 | **Owl Bong** | Glassblowing | Owl Bong |
| 3 | **Deep Sea Bong** | Owl Bong | Deep Sea Bong |
| 4 | **Forge Bong** | Deep Sea Bong | Forge Bong |
| 5 | **Sage Bong** | Forge Bong | Sage Bong |
| 6 | **Phantom Bong** | Sage Bong | Phantom Bong |

**Supplies:** Glass, plus the bong's own two ingredients ×4.

### Textiles (8): tie-dye patterns (needs the Tie Dye Shack, spec 018)
- Eight pattern studies at tiers 1, 2, 2, 3, 3, 4, 5, 6.
- Their names, patterns and supplies are defined by spec 018.
- Until then, they show as `Waiting for the Tie Dye Shack`.

### Armory (7): gear at the Blacksmith (crafter recipes, R6)
| Tier | Study | Parents | Unlocks for the Blacksmith |
|---|---|---|---|
| 1 | **Dab Cell** | – | Dab Cell |
| 1 | **Weedscout Uniform** | – | the uniform (needs `weedscout_uniform`, its own later feature) |
| 2 | **Hotbox Helm** | Dab Cell | Hotbox Helm |
| 3 | **Moon Boots** | Hotbox Helm | Moon Boots |
| 4 | **Couchlock Greaves** | Moon Boots | Couchlock Greaves |
| 5 | **Puff Plate** | Couchlock Greaves | Puff Plate |
| 6 | **The Blazer** | Puff Plate | Blazer |

**Supplies:** the piece's own crafting ingredients ×2, plus Cured Buds.

**Totals:** 23 + 11 + 7 + 7 + 8 + 7 = **63**. At the University's pace the whole board is 33,264 units, about 550 hours of a single Weedologist's working time, or about 280 with two from level 3. This is the colony's long game, as intended.
