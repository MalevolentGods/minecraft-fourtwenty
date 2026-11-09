# Feature Specification: Weed Progression Balance

**Feature Branch**: `012-progression-balance`
**Created**: 2026-11-08
**Status**: Draft
**Input**: User description: "Weed progression balance pass for Four Twenty, paced for about 60 hours from the first seeds to Golden God, alongside a MineColonies colony." (full brief and decisions below)

## Context

**Seeds are too easy.** A mature plant drops its own seed back plus 1 to 3 rough buds, and trimming each bud gives a further 25% chance of a seed. Each Landrace plant returns about 1.5 seeds and 2 buds, so a field grows by half every crop. The user trimmed 17 buds and ended with 12 seeds.

**Every strain plays the same.** Strains differ only in:

- the effect they give when smoked;
- how rare their trimming seed is;
- how slowly the higher grades grow, and which plot they need.

Every strain yields the same 1 to 3 buds at the same strength.

**Decisions (user, 2026-11-08):**

- **Pace:** the whole ladder takes about 60 hours of play.
- **Start:** 4 Landrace seeds, given with the supply camp.
- **Landrace:** 1 bud per harvest.
- **Seeds:** plants keep dropping their own seed.
- **Strains:** each strain family gets its own yield, potency, growth speed and seed chance, all rising up the ladder.
- **Potency:** makes a high both a little stronger and a little longer.
- **Grow Op plots:** each tier adds an extra-seed chance at harvest.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A slow, honest start (Priority: P1)

A new player in a MineColonies world places their supply camp and finds 4 Landrace seeds with it. They plant them, harvest one rough bud per plant (plus each plant's seed back), trim and cure it, and smoke their first joint within about an hour. Growing a full field from those 4 seeds takes a few hours of early play, not one harvest.

**Why this priority**: The opening sets the pace for everything after it, and it is where the current numbers are most wrong.

**Independent Test**: Start a fresh player. Confirm they hold 4 Landrace seeds after placing the supply camp. Harvest a mature Landrace plant 100 times in a test, and confirm every harvest gives exactly 1 rough bud and 1 seed. Trim 1,000 Landrace buds, and confirm about 12% give a seed.

**Acceptance Scenarios**:

1. **Given** a new player places their supply camp, **When** they look in their inventory, **Then** they hold 4 Landrace seeds, given once per player.
2. **Given** a mature Landrace plant with plain traits, **When** it is harvested, **Then** it drops 1 rough bud and 1 Landrace seed.
3. **Given** 4 Landrace seeds and only hand tools, **When** the player replants everything, **Then** the modelled field reaches 16 plants after about 12 crops, not 3 to 4.

---

### User Story 2 - Every strain step is worth it (Priority: P1)

A player who breeds or finds a better strain sees the difference at once:

- more buds per harvest;
- a stronger and longer high;
- rarer seeds, so a good strain stays precious;
- slower growth and better plots needed for the top grades.

The jump from one family to the next is visible in the tooltip and felt in play.

**Why this priority**: Without differences between strains, the ladder has no reason to be climbed.

**Independent Test**: For each strain family:

- Harvest a plain-trait plant many times and check the average buds per harvest against the family's table row.
- Smoke one and check the high's level and duration.
- Trim many buds and check the seed rate.

**Acceptance Scenarios**:

1. **Given** plain-trait plants of two adjacent families, **When** each is harvested many times, **Then** the higher family's average yield is at least equal to and usually above the lower's, matching the table in FR-004.
2. **Given** a strain's bud, joint or pipe load, **When** it is smoked, **Then** its high is stronger and longer by its family's potency, and a seed's potency trait still adds on top.
3. **Given** a strain item, **When** the player reads its tooltip, **Then** it shows the strain's yield, potency and growth and its trimming seed chance.

---

### User Story 3 - Grow Ops are a seed stepping stone (Priority: P2)

A player who builds Grow Op plots gets two things from each tier: faster growth, as now, and an extra-seed chance at harvest. Upgrading plots is the way to grow a stock of a rare strain's seeds.

**Why this priority**: With slower seed growth, players need a planned way to multiply rare strains; plots already exist and are powered, so they are the natural lever.

**Independent Test**: In a test, harvest the same strain on each plot tier many times, and confirm the extra seeds per harvest match each tier's chance.

**Acceptance Scenarios**:

1. **Given** a mature plant on a plot of tier N, **When** it is harvested, **Then** besides its own seed it has tier N's chance of one more seed of its strain (about 10%, 20%, 30%, 40%, 50% for tiers 1 to 5).
2. **Given** the top two plot tiers, **When** a plant is harvested, **Then** their existing bigger harvests and replanting still apply, on the new family yields.

---

### User Story 4 - The colony's appetite matches its farms (Priority: P2)

A colony's weed demand and its weed farms stay in step at each stage of the run:

- **Demand:** the Dispensary's Budtender serves citizens, and serving a level 3 Dispensary takes a reachable farm.
- **Supply:** the Bud Barn grows weed, and a farm of the strain a player has at that point in the run can keep that Dispensary supplied.

**Why this priority**: The mod lives inside MineColonies; a balance that starves the Dispensary, or drowns it, breaks the colony side of the game.

**Independent Test**: Use the progression model, and a game test where it can be done. Compare the buds a Dispensary uses per citizen per in-game day with what a Bud Barn of each level produces at that strain stage.

**Acceptance Scenarios**:

1. **Given** a colony at the hut levels reachable at a milestone, **When** its Bud Barn grows the strains reachable then, **Then** its output covers its Dispensary's use with no more than about 50% to spare.

---

### User Story 5 - The run takes about 60 hours (Priority: P3)

A player climbing the ladder hits the milestones near their targets:

| Milestone | Target |
|---|---|
| First joint | about 1 hour |
| First bred strain | about 5 hours |
| First Grow Op | about 10 hours |
| Biome strains | 15 to 20 hours |
| Crosses | 25 to 30 hours |
| Double crosses | about 40 hours |
| Ultras | about 50 hours |
| Golden God | about 60 hours |

A model of seed and bud growth per strain, using the real numbers, shows the run meets these targets before anyone plays it.

**Why this priority**: The hours are the goal the numbers serve. The model is how the numbers are chosen and checked, but players only see the pace.

**Independent Test**: Run the model with the shipped numbers and confirm each milestone lands within 25% of its target.

**Acceptance Scenarios**:

1. **Given** the shipped numbers, **When** the model plays a typical run, **Then** every milestone lands within 25% of its target, and Golden God between 50 and 75 hours.

### Edge Cases

- **Old seeds and buds:** plants already growing in old worlds keep their strain and traits, and harvest on the new numbers.
- **Old configs:** saved config files keep their old values, so the release notes list which settings to change to get the new balance.
- **Wild weed:** found in the world, it stays a Landrace source, at no more than 1 bud per plant.
- **Player-placed supply camps:** a player who places a second supply camp, or joins a colony someone else placed, still gets their 4 seeds once only.
- **Without MineColonies:** with no supply camp, the starting seeds come from wild weed as now. Everything else in this spec works the same.
- **Traits pushing yield below zero:** a seed with a poor yield trait on Landrace (base 1) gives at least 1 bud, never none.
- **Breeding output:** Infuser and Pheno Chamber results keep their current recipes. Only what the resulting strains yield and seed changes.
- **Yield ranges:** for families with a range (for example 1 to 2), each harvest rolls within it.

## Requirements *(mandatory)*

### Functional Requirements

**Starting seeds**

- **FR-001**: A player MUST receive 4 Landrace seeds once, with their first MineColonies supply camp, when MineColonies is present.

**Harvest and strain stats**

- **FR-002**: A mature plant MUST drop its own seed back on harvest, as now.
- **FR-003**: Each strain family MUST have a base yield (rough buds per harvest), a potency, a growth speed and a trimming seed chance.
- **FR-004**: The defaults MUST follow this table, where "normal" is today's growth speed and potency 1 is today's high:

| Family | Buds per harvest | Potency | Growth | Trimming seed chance | Plot needed |
|---|---|---|---|---|---|
| Landrace | 1 | 1 | normal | 12% | any |
| Neutrals | 1–2 | 2 | normal | 15% | any |
| Warm | 1–2 | 2 | normal | 15% | any |
| Green | 2 | 2 | normal | 10% | any |
| Cool | 2 | 3 | normal | 10% | any |
| Biome | 2 | 3 | slower (as now) | 10% | as now |
| Cross | 2–3 | 3 | slower (as now) | 4% | as now |
| Double cross | 3 | 4 | slow (as now) | 2% | as now |
| Ultra | 3–4 | 4 | slow (as now) | 1% | as now |
| Golden God | 4–5 | 5 | slowest (as now) | 0.5% | top plot (as now) |

- **FR-005**: A seed's traits (potency, yield, length, rated 1 to 5) MUST still add to or take from its family's base. A harvest MUST never give fewer than 1 rough bud.

**Potency**

- **FR-006**: Potency MUST make a high both stronger (effect level) and longer (duration), each step a modest amount, so that Golden God's high is clearly stronger and longer than Landrace's, without breaking the game. Its effect level is at most 2 higher than Landrace's, and it lasts at most twice as long.

**Grow Op plots**

- **FR-007**: Each Grow Op plot tier MUST add a chance of one extra seed of the plant's strain at harvest. Defaults are 10%, 20%, 30%, 40% and 50% for tiers 1 to 5.
- **FR-008**: Grow Op plots MUST keep their current speed bonuses, and the top two tiers MUST keep their bigger harvests and replanting.

**Tooltips**

- **FR-009**: Seeds and buds MUST show their strain's yield, potency and growth, and their trimming seed chance, in the tooltip.

**Configuration**

- **FR-010**: Every number in FR-001, FR-004, FR-006 and FR-007 MUST be tunable in config.

**Colony**

- **FR-011**: The Dispensary's consumption and the Bud Barn's output MUST be checked against the model at each milestone. Each MUST be changed if needed, so that a colony's farm at that stage covers its Dispensary with no more than about 50% to spare.

**The model and the release**

- **FR-012**: A progression model MUST be kept with the project. It plays a typical run on the shipped numbers and reports when each milestone is reached, so future changes can be checked the same way.
- **FR-013**: The release notes MUST list every config setting whose default changed, with its old and new value, for servers to update by hand.

### Key Entities

- **Strain family**: a rung of the ladder (Landrace, Neutrals, Warm, Green, Cool, Biome, Cross, Double cross, Ultra, Golden). It sets the base yield, potency, growth and trimming seed chance of every strain in it.
- **Seed traits**: potency, yield and length, each rated 1 to 5 (2 is plain), carried by a seed and added on top of its family's base.
- **Grow Op plot tier**: tiers 1 to 5. Each sets a speed bonus, an extra-seed chance at harvest, and, at the top two tiers, bigger harvests and replanting.
- **Milestone**: a point in the run with a target time: first joint, first bred strain, first Grow Op, biome, cross, double cross, ultra, Golden God.
- **Progression model**: a simulation of a typical player's run on the shipped numbers, giving each milestone's time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new player smokes their first joint within about 1 hour of placing their supply camp.
- **SC-002**: 4 starting Landrace seeds grow to about 16 plants in roughly 12 crops on hand tools alone, compared with 3 to 4 crops today.
- **SC-003**: The model run reaches Golden God in 50 to 75 hours, and every other milestone within 25% of its target.
- **SC-004**: Over many harvests, each family's average buds per harvest and trimming seed rate match the table within 10%.
- **SC-005**: A player can tell two adjacent strain families apart by their tooltip alone (yield, potency and seed chance shown).
- **SC-006**: At each milestone, a colony's Bud Barn output covers its Dispensary's use with between 0% and 50% to spare.

## Assumptions

- "Normal" growth is today's Landrace growth (weed grows at half the speed of other crops). The higher grades keep their current slower speeds and plot requirements unless the model shows a milestone missing its target.
- Effects stay as they are per strain; potency only scales how strong and long they are.
- **Typical player:** plays about 2 hours per session and builds a colony alongside, so MineColonies hut levels arrive at their usual pace.
- **Supply-camp seeds** go through the same starter datapack that already gives the supply camp deployer; that datapack also ships with the client pack.
- **Existing items:** buds and seeds already in worlds keep working; only their behaviour from now on changes.
- **Recipes:** breeding, crafting, the Terpinator and the Infuser keep their recipes; this spec changes numbers, not recipes.
- **Bud Barn and Dispensary:** these are the colony's grow and consumption huts. Their changes are limited to rates, not new mechanics.
