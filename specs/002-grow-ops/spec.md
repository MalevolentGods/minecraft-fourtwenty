# Feature Specification: Grow Ops, the Think Tank and the Blaze Tier

**Feature Branch**: `002-grow-ops`
**Created**: 2026-10-31
**Status**: Draft
**Input**: Milestone 2 of the Four Twenty plan: five tiers of indoor plot, a research block that uses them, and the Blaze power tier the top plot needs.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Grow indoors, faster (Priority: P1)

A player places a Closet Grow, plants a crop on it and sees it grow faster than on farmland. As they progress they upgrade the same plot, with the crop still on it, through to a Cosmic Kush Pod.

**Why this priority**: Plots are useful alone, and the Think Tank depends on them.

**Independent Test**: Grow the same crop on farmland and on each plot tier and compare the time to mature.

**Acceptance Scenarios**:

1. **Given** a Closet Grow with light, **When** a crop is planted on it, **Then** it grows about 1.5 times as fast as on watered farmland.
2. **Given** a Grow Tent in the dark, **When** a crop is on it, **Then** it still grows, by the plot's own lamp.
3. **Given** a Hydro Bed, **When** a crop is planted, **Then** it needs no soil or water nearby.
4. **Given** any crop, not only weed, **When** it's planted on a plot, **Then** the plot works the same.
5. **Given** a plot holding a crop, **When** the next tier's plot is used on it, **Then** it upgrades in place and the crop stays.
6. **Given** a powered plot with no BI and no high player nearby, **When** the crop grows, **Then** it grows at normal speed with no bonus, and the crop is never harmed.

---

### User Story 2 - Bigger harvests at the top end (Priority: P2)

A player harvests from an Aero Pod or Cosmic Kush Pod and gets more, and the crop replants itself.

**Why this priority**: It's the reward for the expensive tiers.

**Independent Test**: Harvest mature crops on an Aero Pod and on farmland and compare.

**Acceptance Scenarios**:

1. **Given** a mature crop on an Aero Pod or better, **When** it's harvested, **Then** the crop's non-seed drops are half as many again, rounded up.
2. **Given** the same crop, **When** it's broken, **Then** it replants from one of its own seeds, and the seed is never lost.

---

### User Story 3 - Research with no colony (Priority: P1)

A player with no MineColonies picks a study at a Think Tank, loads its inputs and keeps a crop growing on a nearby plot. In time the study finishes and they collect the result.

**Why this priority**: It's the milestone's definition of done.

**Independent Test**: Finish one tier-1 study with only a Think Tank and a Closet Grow.

**Acceptance Scenarios**:

1. **Given** a Think Tank, **When** a player picks a study, **Then** it shows the inputs, time and reward.
2. **Given** the inputs are loaded and a plot within 8 blocks has a crop on it, **When** the study runs, **Then** progress builds.
3. **Given** no plot in range, or no crop on one, **When** a study is queued, **Then** it waits and says why.
4. **Given** better or more plots, **When** the study runs, **Then** it runs faster.
5. **Given** a study runs, **When** a bad batch comes up, **Then** it loses time but still finishes.
6. **Given** a finished study, **When** a player collects it, **Then** they get its reward.

---

### User Story 4 - The Blaze tier (Priority: P2)

A player builds a Grand Hookah, The Vault and Gold Wick to run a Cosmic Kush Pod.

**Why this priority**: The top plot can't run without it.

**Independent Test**: Run a Cosmic Kush Pod from a Grand Hookah through Gold Wick.

**Acceptance Scenarios**:

1. **Given** a lit Grand Hookah, **When** a burn finishes, **Then** it has made 33,600 BI in the same one-minute burn.
2. **Given** The Vault, **When** power is spare, **Then** it stores up to 40M BI and outputs at the Grand Hookah's rate.
3. **Given** Gold Wick, **When** two Grand Hookahs feed it, **Then** it holds, and a third burns it out.

### Edge Cases

- A plot is broken with a crop on it: the crop pops off, as on farmland.
- The Think Tank is broken mid-study: inputs already used are lost, and loaded inputs drop.
- A server restart during a study: progress is kept.
- A linked plot loses its crop mid-study: the study waits and keeps its progress.
- The reward is already owned: the study isn't offered.

## Requirements *(mandatory)*

### Functional Requirements

**Grow Ops**

- **FR-001**: There MUST be five plots, each its own custom block: Closet Grow, Grow Tent, Hydro Bed, Aero Pod, Cosmic Kush Pod.
- **FR-002**: Crops on a plot MUST grow about ×1.5, ×2, ×3, ×5 and ×8 as fast as on watered farmland, with those numbers in config.
- **FR-003**: Plots MUST accept any crop that grows on farmland.
- **FR-004**: The Closet Grow MUST use no power and need outside light. The Grow Tent and above MUST light their own crop.
- **FR-005**: The Grow Tent, Hydro Bed, Aero Pod and Cosmic Kush Pod MUST run at Puff, Toke, Rip and Blaze tier under the shared power rule. With neither power nor a high player they MUST give no bonus and do no harm.
- **FR-006**: A plot MUST upgrade in place by using the next tier's plot on it, keeping the crop.
- **FR-007**: The Aero Pod and Cosmic Kush Pod MUST give half as many again of a crop's non-seed drops, and MUST replant the crop from its seeds.
- **FR-008**: Plots MUST share the tech block style, screens where they have one, and tooltips.

**Think Tank**

- **FR-009**: The Think Tank MUST work without MineColonies.
- **FR-010**: It MUST let a player pick a study, load its inputs, and see progress and what it's waiting for.
- **FR-011**: A study MUST need at least one plot with a crop within 8 blocks. Progress MUST scale with the plots' speed, more plots adding up to a configured limit.
- **FR-012**: Study length MUST be 72 units at tier 1, doubling each tier to tier 6, at one unit per minute at ×1, in config.
- **FR-013**: Studies MUST always succeed. A bad batch MUST only add time.
- **FR-014**: This release MUST offer one tier-1 study for each of the 16 Minecraft colours, costing 8 Cured Buds, 4 Landrace seeds and a glass bottle, and rewarding the colour as caught for the player who collects it.
- **FR-015**: Studies MUST be defined so more can be added without new code.
- **FR-016**: The Think Tank MUST follow the shared power rule at Puff tier, and MUST have a ? tutorial, ghost icons and popups.

**Blaze tier**

- **FR-017**: A Blaze tier MUST exist at 33,600 BI per burn, twice Rip, following every existing power rule.
- **FR-018**: There MUST be a Grand Hookah (the Hookah upgraded from Big Bertha), The Vault (40M BI) and Gold Wick. The Hit Box MUST also step Blaze to Rip.

### Key Entities

- **Plot**: a tier, a speed, and the crop on top of it.
- **Study**: a name, a tier, inputs, a length and a reward.
- **Think Tank**: the current study, its loaded inputs, its progress and its linked plots.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A player finishes a tier-1 study with only a Think Tank and a Closet Grow, with no MineColonies, in under an hour of play.
- **SC-002**: Crops on each plot tier reach maturity within 20% of the stated speed-up.
- **SC-003**: No study ever fails. A study with bad batches still finishes.
- **SC-004**: An upgrade never removes the crop on a plot.
- **SC-005**: Every plot tier works with weed and with wheat.

## Assumptions

- Plots sit under the crop, like farmland, rather than holding it in a slot.
- 72 units at one unit a minute is 1.2 hours at ×1, so a Closet Grow's ×1.5 gives 48 minutes.
- Bad batches come up about 1 unit in 10 and cost that unit.
- A reward is collected by whoever clicks Collect. Colony-wide research waits for the Weed College.
- Cross-pollination is not in this release: there are no hybrids for it to make until the Pheno Chamber.
- With no biome strains yet, the Cosmic Kush Pod's biome copying has nothing to do; it arrives with them.
- This branch builds on 0.15.0, which isn't merged yet.
