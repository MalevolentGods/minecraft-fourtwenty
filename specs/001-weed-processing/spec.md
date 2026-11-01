# Feature Specification: Weed Processing and Boundless Inspiration

**Feature Branch**: `001-weed-processing`
**Created**: 2026-10-31
**Status**: Draft
**Input**: Rough weed is trimmed and cured through four tiers of machines, trimming gives seeds, and powered tiers run on an IC-style power system called Boundless Inspiration.

## Clarifications

### Session 2026-10-31

- Q: How fast does a machine run free for a high player compared with on BI? → A: The same speed, as a config value.
- Q: Are the machine names and "Cured Bud" approved? → A: Yes, all of them, exactly as written.
- Q: What does one burn give, and how do blocks fail? → A: One coal and one bud run four same-tier machines for a minute, at 4,200, 8,400 and 16,800. Batteries match Hookahs, Wicks carry two Hookahs, only Wicks break, and overloaded blocks stop with a green, smoky screen.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - From crop to joint by hand (Priority: P1)

A player with no power and no colony harvests rough weed, trims it at a Trim Tray and hangs it on a Curing Rack. Later they take down cured buds and roll a joint. Trimming also gives them seeds, so a farm can finally grow.

**Why this priority**: This is the new core loop. It replaces plant-to-smokeable for everyone, and nothing else matters without it.

**Independent Test**: In a new world with no machines beyond T1, go from one seed to a smoked joint, and end with more seeds than the start.

**Acceptance Scenarios**:

1. **Given** a mature crop, **When** a player harvests it, **Then** they get 1-3 rough buds and the crop's replant seed, and never a smokeable bud.
2. **Given** rough buds and a Trim Tray, **When** the player right-clicks, **Then** one bud becomes a trimmed bud, and sometimes a seed appears.
3. **Given** trimmed buds on a Curing Rack, **When** the cure time passes, **Then** they become cured buds, with no power, and the player can see them hanging while they cure.
4. **Given** a cured bud of any strain, **When** used in any existing recipe or smoked, **Then** it behaves exactly as a Weed Bud did before this release.
5. **Given** a rough or trimmed bud, **When** a player tries to smoke it or use it in a recipe that takes cured buds, **Then** it is refused.

---

### User Story 2 - Seeds get rarer with better strains (Priority: P1)

A player trims many buds and sees seeds come often from Landrace and rarely from the best strains, so high-end seeds stay valuable.

**Why this priority**: It's the reason for the change: seeds become a real output of processing.

**Independent Test**: Trim a large batch of each family and compare seed counts with the configured chances.

**Acceptance Scenarios**:

1. **Given** default settings, **When** many buds are trimmed, **Then** seeds appear at about 50% for Landrace, 25% Neutrals, 15% Warm, 10% Green and 5% Cool.
2. **Given** a strain's bud, **When** it gives a seed, **Then** the seed is that strain.
3. **Given** a server owner changes a chance, **When** trimming continues, **Then** the new chance applies.

---

### User Story 3 - Power a machine while sober (Priority: P2)

A player lights a Hookah, runs Wicks to a machine and feeds it weed. The machine runs while they're sober. If they get high nearby it runs for free. With neither it waits, without losing its place.

**Why this priority**: Everything automated depends on this, and it's the plan's foundation for all later tech.

**Independent Test**: With a Single Hose Hookah, Hemp Wick and Stash Jar, run a Terpinator while sober, then test the high and unpowered cases.

**Acceptance Scenarios**:

1. **Given** a fuelled Hookah wired to a machine, **When** nobody high is near, **Then** the machine runs on BI.
2. **Given** a high player within 21 blocks, **When** there is no power at all, **Then** the machine runs free.
3. **Given** no high player and no BI, **When** a machine has work, **Then** it pauses and keeps its progress, and resumes from there.
4. **Given** too much power for a Wick, **When** it arrives, **Then** the Wick burns out in smoke and leaves ash. **Given** a block fed from a higher tier, **When** the power arrives, **Then** it overloads and stops without breaking.
5. **Given** a Stash block, **When** power is spare, **Then** it stores it and gives it back when needed.
6. **Given** a Hit Box, **When** wired between tiers, **Then** power can step between tiers without burnout.

---

### User Story 4 - Automate the line (Priority: P2)

A player wires up T2-T4 machines, feeds rough buds in by hopper or pipe, and collects cured buds and seeds at the far end with nobody present.

**Why this priority**: This is the payoff for the machine tiers and what makes a large farm workable.

**Independent Test**: Hopper-fed Snip Station into a Dry Box into a chest, all on power, with the player away and sober.

**Acceptance Scenarios**:

1. **Given** a T2+ machine, **When** a hopper or pipe feeds its top or side, **Then** only valid items enter.
2. **Given** finished output, **When** a hopper or pipe pulls from the bottom, **Then** outputs and seeds come out, and inputs never do.
3. **Given** two strains in one machine, **When** it runs, **Then** neither strain is lost or changed.
4. **Given** each higher tier, **When** it runs at the same load, **Then** it's faster than the tier below.

---

### User Story 5 - Colonies keep running on supplied buds (Priority: P3)

A colony's farmers bring in rough weed. Until the Bud Barn exists, players supply cured buds, and a Dispensary with none says why instead of failing silently.

**Why this priority**: Colonies must not break, but the full colony answer is a later milestone.

**Independent Test**: Run a Dispensary with and without cured buds supplied, with MineColonies installed, and again confirm the mod plays fully without it.

**Acceptance Scenarios**:

1. **Given** cured buds in the colony's supply, **When** the Budtender works, **Then** the menu runs as it does today.
2. **Given** no cured buds, **When** the Budtender needs them, **Then** the hut reports what it's missing.
3. **Given** MineColonies isn't installed, **When** the game runs, **Then** all of this feature works.

---

### Edge Cases

- An upgrade from 0.14.x: existing Weed Buds become cured buds, and crops already planted keep growing and give rough buds when harvested.
- Breaking a machine holding items: its contents drop.
- A machine is mid-job when the server restarts: progress is kept. A player's high is not kept, so a machine with no BI pauses until someone is high again.
- A machine's output is full: it stops without losing anything.
- A high player leaves range mid-job: the machine switches to BI, or pauses.
- Mixed strains on one rack: each keeps its own strain.
- A Wick network is split or a Wick burns out: the parts cut off stop receiving power, and nothing else breaks.
- Pot Pots: harvests follow the same rule as crops.
- Wild weed still gives seeds directly, as it does today.

## Requirements *(mandatory)*

### Functional Requirements

**Weed pipeline**

- **FR-001**: Mature weed crops and Pot Pots MUST give 1-3 rough buds and one replant seed, and no cured buds. The 5% bonus seed chance MUST be removed.
- **FR-002**: Immature crops and wild weed MUST behave as they do today.
- **FR-003**: Trimming MUST turn one rough bud into one trimmed bud. Curing MUST turn one trimmed bud into one cured bud.
- **FR-004**: A bud's strain MUST be kept through every step, and any seed from trimming MUST be that strain.
- **FR-005**: Each trimmed bud MUST roll for a seed at its family's chance: Landrace 50%, Neutrals 25%, Warm 15%, Green 10%, Cool 5%.
- **FR-006**: Cured buds MUST be the existing Weed Bud item, displayed as "Cured Bud". All its existing uses MUST keep working and existing stacks MUST stay valid.
- **FR-007**: Rough and trimmed buds MUST NOT be smokeable and MUST NOT work in recipes that take cured buds.

**Machines**

- **FR-008**: There MUST be four tiers of trimming and four of curing, each its own block with its own custom look: Trim Tray, Snip Station, Bud Barber, Trim Reaper; Curing Rack, Dry Box, Cure Closet, The Humidor.
- **FR-009**: T1 machines MUST need no power and MUST be fed by hand. The Curing Rack MUST show the buds on it.
- **FR-010**: T2, T3 and T4 machines MUST run at Puff, Toke and Rip tier, each faster than the last, and MUST take items in from the top and sides and give items out of the bottom, for hoppers and pipe mods.
- **FR-011**: All machine speeds, batch sizes and power use MUST be configurable, starting at: trim 40/20/10 ticks per bud at 16/64/256 BI/t; cure 5 minutes for 8 buds at T1, then 2.5 minutes for 16, 1 minute for 32 and 20 seconds for 64 at 16/64/256 BI/t.
- **FR-012**: Every machine MUST be craftable in survival, using nothing from MineColonies.

**Boundless Inspiration**

- **FR-013**: The power system MUST have three tiers in this release, shown to players as "Inspo": Puff, Toke and Rip. One coal and one cured bud MUST give 4,200, 8,400 and 16,800 Inspo at those tiers, burning for one minute on all of them, and MUST run four machines of that tier for that minute.
- **FR-014**: The generator MUST be the existing Hookah, making Puff power while it burns, with the Double Hose (Toke) and Big Bertha (Rip) as upgrades of that same block: same session, same inputs, same look with more hoses. Coal is the only fuel.
- **FR-015**: Storage MUST be the Stash Jar (40k), Stash Box (400k) and Mother Stash (4M), each outputting at the rate of a Hookah of its tier.
- **FR-016**: Wiring MUST be the Hemp, Rolled and Glass Wicks, which glow while carrying power and carry twice the output of a Hookah of their tier. The Hit Box MUST step power between tiers.
- **FR-017**: A Wick carrying more than its limit MUST burn out in smoke and leave ash. A block fed from a tier above its own MUST NOT break: it overloads and stops, smoking, with its screen green and smoky, until the higher tier is gone. Nothing explodes.
- **FR-018**: Every powered machine MUST follow one rule: free while a high player is within 21 blocks, otherwise draw BI, otherwise pause and keep its progress.
- **FR-019**: The Terpinator and Inspiration Infuser MUST move onto that rule, with their recipes, outputs and gameplay unchanged.

**Compatibility**

- **FR-020**: Everything MUST work without MineColonies.
- **FR-021**: With MineColonies, the Budtender's menu MUST keep working from supplied cured buds, and a Dispensary without them MUST report what's missing.
- **FR-022**: Clients without the channel MUST NOT be sent power or machine updates.
- **FR-023**: A Budtender MUST load at most one Hookah session per 10 minutes.
- **FR-024**: Every tech block's screen MUST share one custom style with an animated Inspo gauge, and tech blocks MUST have animated textures and their own sound effects.

### Key Entities

- **Rough Bud / Trimmed Bud**: the two new stages of a harvest. Each has a strain.
- **Cured Bud**: the existing Weed Bud under its new name.
- **Machine**: a tier, a job (trim or cure), inputs, outputs and progress.
- **Power Tier**: Puff, Toke or Rip, with a maximum rate.
- **Network**: generators, storage, machines and the Wicks joining them.
- **Seed Chance**: a strain family's chance per trimmed bud.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new player can go from one seed to a smoked joint using only T1 machines in one in-game session.
- **SC-002**: Landrace farming nets about 1 extra seed per harvest at default settings, up from about 1 per 8 harvests.
- **SC-003**: Over 1,000 trims per family, seed rates land within 3 percentage points of the configured chance.
- **SC-004**: Every strain comes out of a full run through all tiers with no bud lost and no strain changed.
- **SC-005**: A hopper-fed line runs for a full in-game day with the player away and sober, ending with no item lost.
- **SC-006**: 100% of existing bud recipes and uses work unchanged on a world upgraded from 0.14.x.
- **SC-007**: No overload in testing ever causes an explosion.
- **SC-008**: The game loads and plays through all of the above with MineColonies absent.

## Assumptions

- A release ships everything in one version, 0.15.0, built up in tested phases.
- Trimming tiers give speed only; the seed chance doesn't change by tier.
- T1 machines don't accept hoppers.
- Higher tiers accept lower-tier power at a slower rate, if they have enough input.
- A Hookah burns cured buds with its usual lighter fuel, as it always has; rough and trimmed buds don't burn. Extracts arrive with the Rosin Rig.
- Blaze tier, Grow Ops, Think Tank, Rosin Rig, Pheno Chamber, Kush Suit, Blazer and the Bud Barn are out of scope.
- The existing block shown as "Hookah" keeps its name and job, and also generates. Changed on 2026-10-31 at the user's request; the plan's separate Single Hose Hookah is gone.
