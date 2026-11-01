# Feature Specification: Extracts, Breeding and the Hybrid Ladder

**Feature Branch**: `003-extracts-and-breeding`
**Created**: 2026-11-01
**Status**: Draft
**Input**: Milestone 3 of `four-twenty/HANDOFF.md`, changed by the user on 2026-11-01: hybrids are a whole new tier of bud, harder and slower to grow with advanced properties, tied into the tiered progression of the mod, with hybrids of hybrids and an ultra tier at the top that makes you a shining golden god when smoked.

## Clarifications

### Session 2026-11-01

- Q: What is a hybrid? → A: A new tier of bud: named strains of their own, harder and slower to grow, with hybrids of hybrids above them and one strain at the top that makes the smoker a shining golden god.
- Q: How are traits inherited? → A: Each trait, rated 1 to 5, takes the parents' average, then may move up or down by one. Every existing seed is a 2.
- Q: How are biome strains made? → A: Landrace grown in the biome, or in a Cosmic Kush Pod copying it, sometimes gives a climate-touched seed, and the Pheno Chamber settles that into the biome strain.

## The ladder

Each grade needs a better plot and a better Pheno Chamber, grows slower and gives seeds more rarely. All numbers are config.

| Grade | Strains | Bred in | Grows on | Growth | Seed chance |
|---|---|---|---|---|---|
| 0 Landrace | Landrace | | anywhere | ×1 | 50% |
| 1 Bred | The 16 existing strains | Infuser | anywhere | ×1 | 25% to 5% |
| 1 Biome | The 4 biome landraces | Pheno Chamber | anywhere | ×1 | 20% |
| 2 Cross | 8 strains | Pheno Chamber (Toke) | any Grow Op | ×0.7 | 4% |
| 3 Double Cross | 6 strains | Pheno Lab (Rip) | Hydro Bed or better | ×0.5 | 2% |
| 4 Ultra | 3 strains | Pheno Cathedral (Blaze) | Aero Pod or better | ×0.35 | 1% |
| 5 Golden | Golden God | Pheno Cathedral (Blaze) | Cosmic Kush Pod | ×0.25 | 0.5% |

### Biome landraces

| Strain | Climate | Effect |
|---|---|---|
| Dust Devil | Desert, badlands | Fire Resistance, Haste |
| Snowblind | Snowy biomes | Speed, Resistance |
| Bog Butter | Swamps | Water Breathing, Regeneration |
| Canopy Creeper | Jungles | Jump Boost, Slow Falling |

### Grade 2: Crosses

| Strain | Parents | Effect |
|---|---|---|
| Tropical Thunder | Tangie Tango × Mermaid Hotbox | Water Breathing, Fire Resistance, Dolphin's Grace |
| Razzle Dazzle | Cherry Pie Accord × Cotton Candy Kush | Strength II, Absorption II |
| Alarm Clock Assassin | Wake & Bake × Sunshine Daydream | Haste II, Speed II |
| Couch Fort | Couchlock Hum × Brownie Batch | Resistance II, Saturation |
| Moss Boss | Mossy Philosopher × Sticky Icky Accord | The growing aura, Regeneration II |
| Deep Thoughts | Third-Eye Tonic × Spacebar Static | The seeing aura, Luck II |
| Lucid Lagoon | Blue Dream × Cloud Nine Drift | Slow Falling, Night Vision, no phantoms |
| Rocket Lime | Lime in the Coconut × Purple Haze | Jump Boost III, Night Vision |

### Grade 3: Double Crosses

| Strain | Parents | Effect |
|---|---|---|
| Hurricane Haze | Tropical Thunder × Bog Butter | Water Breathing, Conduit Power, Speed II |
| Sledgehammer Serenade | Razzle Dazzle × Couch Fort | Strength III, Resistance III |
| Caffeine Comet | Alarm Clock Assassin × Rocket Lime | Speed III, Haste III, Jump Boost III |
| Old Growth | Moss Boss × Canopy Creeper | A wider growing aura, Regeneration II |
| Oracle of Snacks | Deep Thoughts × Dust Devil | The seeing aura at 32 blocks, Luck III, Saturation |
| Polar Vortex | Lucid Lagoon × Snowblind | Slow Falling, Resistance II, slows mobs near you |

### Grade 4: Ultra

| Strain | Parents | Effect |
|---|---|---|
| Cosmic Couch | Sledgehammer Serenade × Polar Vortex | Resistance IV, Slow Falling, Regeneration III |
| Storm Shepherd | Hurricane Haze × Caffeine Comet | Speed IV, Haste IV, lightning strikes what you hit |
| The Elder Bud | Old Growth × Oracle of Snacks | Both auras at full width, Luck IV, Hero of the Village |

### Grade 5: Golden

| Strain | Parents | Effect |
|---|---|---|
| Golden God | Any two different Ultra strains | See User Story 5 |

## User Scenarios & Testing

### User Story 1 - Traits on every seed (Priority: P1)

A player reads a seed's Potency, Yield and Length, and better numbers really do more: stronger effects, more buds, longer highs.

**Independent Test**: Plant a seed with Yield 5 and Length 5, harvest, process and smoke it, and compare it with a seed rated 2.

**Acceptance Scenarios**:

1. **Given** any seed made before this feature, **When** it is planted and used, **Then** it is rated 2 in everything and behaves exactly as it did.
2. **Given** a seed with traits, **When** it goes crop → rough → trimmed → cured → joint, **Then** the traits stay with it at every step, including a seed trimmed from the bud.
3. **Given** a crop with traits, **When** a player looks at it with Jade, **Then** the strain and traits show.

### User Story 2 - The Pheno Chamber (Priority: P1)

A player loads two seeds in a powered Pheno Chamber and gets a seed back: the named hybrid if those parents have one, otherwise one of the parents, either way with mixed traits.

**Independent Test**: Cross Wake & Bake with Sunshine Daydream and get Alarm Clock Assassin; cross a strain with itself and get better or worse traits.

**Acceptance Scenarios**:

1. **Given** two parents of a listed cross, **When** the Chamber finishes, **Then** the output is that hybrid with each trait the parents' average, moved by at most one, clamped to 1-5.
2. **Given** a cross of a grade above the Chamber's, **When** the seeds are loaded, **Then** it refuses, and its screen says which Chamber it needs.
3. **Given** no power, **When** a cross is running, **Then** it pauses with progress kept.
4. **Given** the order of the two seeds is swapped, **When** crossed, **Then** the result is the same.

### User Story 3 - Harder crops (Priority: P1)

Higher grades only grow on good plots, grow slower, and rarely give seeds.

**Acceptance Scenarios**:

1. **Given** a grade 3 seed, **When** planted anywhere but a Hydro Bed or better, **Then** it won't plant, and the player is told what it needs.
2. **Given** a grade 2 crop, **When** compared with grade 1 under the same conditions, **Then** it grows about 0.7 as fast.

### User Story 4 - Biome strains (Priority: P2)

A player grows Landrace in a jungle, sometimes gets a climate-touched seed, and settles it in the Pheno Chamber into Canopy Creeper.

**Acceptance Scenarios**:

1. **Given** a mature Landrace crop in a climate biome, **When** harvested, **Then** there is a 10% chance per harvest of a climate-touched seed for that climate.
2. **Given** a Cosmic Kush Pod set to a climate, **When** Landrace grows on it, **Then** it counts as being in that climate, at half the chance.
3. **Given** one climate-touched seed, **When** run alone in a Pheno Chamber, **Then** it comes out as that biome strain.
4. **Given** a Cosmic Kush Pod, **When** right-clicked with a block of that climate (sand, snow block, lily pad, jungle sapling), **Then** it copies that climate.

### User Story 5 - The Golden God (Priority: P2)

Smoking Golden God turns the player into a shining golden god for its length.

**Acceptance Scenarios**:

1. **Given** a player smokes Golden God, **Then** everyone nearby sees them shining gold with a halo of gold sparks, and they have Resistance IV, Strength III, Regeneration III, Speed III, Haste III, Fire Resistance and creative-style flight.
2. **Given** the flight ends mid-air, **Then** they float down unharmed (flying can never kill you).
3. **Given** the player logs out or dies, **Then** the shine stops for everyone.

### User Story 6 - The Rosin Rig (Priority: P2)

A player refines cured bud into stronger fuel on Toke power. No terpenes.

| Step | In | Out | Hookah burn from one charcoal |
|---|---|---|---|
| Cured Bud | | | 1 minute |
| Kief | 1 Cured Bud | 1 Kief | 2 minutes |
| Hash | 2 Kief | 1 Hash | 5 minutes |
| Resin | 1 Hash | 1 Resin | 7.5 minutes |
| Rosin | 1 Resin | 1 Rosin | 15 minutes |
| Oil | 1 Resin + Glass Bottle | 1 Oil | 15 minutes |

**Acceptance Scenarios**:

1. **Given** a Rig with a Cured Bud, **When** it finishes, **Then** a Kief of that strain and traits comes out, and hoppers can feed and empty it.
2. **Given** a Hookah loaded with Hash and lit with one charcoal, **When** it burns, **Then** it makes BI for 5 minutes at its usual rate, and burns biome strains 1.5 times as long.
3. **Given** a pipe, **When** loaded with an extract, **Then** it smokes at +1 Potency and +1 Length per step past Kief, up to +2.

### User Story 7 - Construction Joint (Priority: P3)

A player uses one bench to craft the mod's building blocks in bulk from hemp and materials, under the shared power rule.

**Acceptance Scenarios**:

1. **Given** the Construction Joint, **When** opened, **Then** it lists hemp blocks, lamps and roads, and crafts the chosen one from the inputs, in twice the batch of the crafting grid.

### Edge Cases

- A hybrid seed placed by a colony Farmer or a replanting mod that loses the traits comes up rated 2, and never crashes.
- Crops planted before this feature have no stored traits, and read as 2.
- A hybrid crop whose plot is removed or downgraded stops growing and stays alive.
- Hybrids work for the Terpinator only where any weed does (the Minecraft colours), so it stays unchanged.
- Golden God flight must not persist after the effect in any path, and must not switch off a creative or spectator player's own flight.

## Requirements

- **FR-001**: Strains MUST carry a grade, and parents where they have them.
- **FR-002**: Seeds, buds, joints, pipe loads and extracts MUST carry traits, with rating 2 as the default.
- **FR-003**: Traits at 2 MUST leave every effect, drop and burn as it is today.
- **FR-004**: The cross result MUST NOT depend on the order of the parents.
- **FR-005**: Higher grades MUST need the plots and Chambers in the ladder.
- **FR-006**: Hybrids MUST inspire their parents' colours in the Infuser.
- **FR-007**: The Terpinator MUST stay the only source of terpenes; the Rig makes extracts only.
- **FR-008**: Pheno Chambers and the Rosin Rig MUST follow the shared power rule and accept hoppers.
- **FR-009**: Each new strain MUST have item art, crop colour and smoke colour by the existing pattern.
- **FR-010**: Nothing here MAY need MineColonies.

### Key Entities

- **Strain**: gains grade and parents.
- **Traits**: Potency, Yield, Length, each 1-5.
- **Climate**: Desert, Snow, Swamp, Jungle.

## Success Criteria

- **SC-001**: A player can go from Landrace seed to a grade 2 hybrid using only the Infuser, a Grow Op and a Pheno Chamber.
- **SC-002**: A rating 2 seed behaves identically to before in all automated tests.
- **SC-003**: Reaching Golden God needs every Grow Op tier and all three Chambers.
- **SC-004**: Every strain in the ladder can be reached from the Landrace seed.

## Assumptions

- Research gating of hybrids waits for the research tree in Milestone 6.
- Cross-pollination between neighbouring plots stays for later: the Chamber is the only way to cross for now.
- Strain names here are proposals and change only in `domain/Strain` and the generators.
