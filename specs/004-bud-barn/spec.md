# Feature Specification: Bud Barn

**Feature Branch**: `004-bud-barn`
**Created**: 2026-11-03
**Status**: Draft
**Input**: User description: "Bud Barn, milestone 5 item from IgnusVM/claude-misc four-twenty/HANDOFF.md: a MineColonies hut (optional integration only) that trims and cures the colony's rough weed by hand with no power, using real Trim Trays and Curing Racks that are part of its blueprint. Today colony farmers bring in rough buds, which the Dispensary can't use, so a Dispensary runs dry unless a player supplies cured buds; the Bud Barn closes that loop. Its worker takes rough buds from the colony, trims them at the hut's Trim Trays (one bud per action, keeping the strain, with the same seed chances as a player trimming), hangs trimmed buds on the hut's Curing Racks, collects cured buds when done, and makes them available to the colony so the Dispensary and Budtender get supplied without a player. Seeds from trimming also go to the colony. Five hut levels: more Trays and Racks and a faster worker each level. Sensi Heights gets levels 1-5 of its blueprint, in the agriculture earth-tone style. The worker's job name needs user approval. Out of scope: powered machines in the hut, the Weed College, the Weedologist, Hookah stocking."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A colony turns its own harvest into smokeable weed (Priority: P1)

A player runs a colony with a weed farm and a Dispensary. The farmer harvests rough buds into the warehouse. The player builds a Bud Barn and hires a worker. Without the player touching the weed, the worker fetches rough buds, trims them at the hut's Trim Trays, hangs the trimmed buds on its Curing Racks, takes the cured buds down when they're done, and sends them on to the colony. The Dispensary's Budtender gets cured buds through the colony's normal deliveries and keeps rolling joints.

**Why this priority**: it is the whole point: today a colony's Dispensary runs dry unless a player keeps feeding it cured buds, because farmers only bring in rough ones.

**Independent Test**: in a colony with rough buds in the warehouse, a level 1 Bud Barn with a worker and no player involvement turns them into cured buds that reach the warehouse, and a Dispensary that was out of buds starts serving again.

**Acceptance Scenarios**:

1. **Given** a level 1 Bud Barn with a worker and 16 rough buds of one strain in the warehouse, **When** a Minecraft day passes, **Then** cured buds of that strain have reached the colony's storage, one for each rough bud taken.
2. **Given** a Dispensary out of cured buds and a working Bud Barn, **When** the Bud Barn delivers cured buds, **Then** the Budtender receives them through the colony's usual requests and serves again, without a player.
3. **Given** rough buds of two strains, **When** they are trimmed and cured, **Then** every cured bud keeps its own strain and traits, and no strain is turned into another.

---

### User Story 2 - Trimming gives the colony seeds (Priority: P2)

Trimming is where seeds come from. When the worker trims at a Trim Tray, it gets seeds at the same chances a player would, by the strain's family, and the seeds go to the colony's storage, so the farmer can keep replanting and the colony can spread its better strains.

**Why this priority**: without it, a colony that relies on the Bud Barn would slowly lose its seed supply, and bred strains couldn't spread through the colony.

**Independent Test**: have the worker trim a large batch of one strain and compare the seeds that reach storage with the strain family's seed chance.

**Acceptance Scenarios**:

1. **Given** 400 Landrace rough buds trimmed by the worker, **When** trimming is done, **Then** about 25% of them (within a few points) produced a Landrace seed, which reached the colony's storage.
2. **Given** a bred strain, **When** a seed comes from trimming it, **Then** the seed is that strain with the bud's traits.

---

### User Story 3 - The Bud Barn grows with the colony (Priority: P2)

The player builds the Bud Barn from the Sensi Heights style and upgrades it through five levels. Each level has more Trim Trays and Curing Racks, and its worker works faster, so a bigger colony with more Dispensaries can be kept supplied. The building looks like the rest of the town's farm buildings: earth tones, growing from a timber shed to a modern barn.

**Why this priority**: a single tray and two racks can't keep up with a large colony; growth is what makes the hut worth upgrading, but the loop already works at level 1.

**Independent Test**: build levels 1 through 5 from the Sensi Heights style with a builder, and measure cured buds per Minecraft day at each level with plenty of rough buds supplied.

**Acceptance Scenarios**:

1. **Given** the Sensi Heights style, **When** a player places a Bud Barn, **Then** levels 1 to 5 are available, each builds cleanly and keeps the same footprint, and every level has its Trim Trays and Curing Racks in place.
2. **Given** plenty of rough buds, **When** the hut is upgraded a level, **Then** it turns out more cured buds per Minecraft day than the level before.

---

### User Story 4 - The player can see what the Bud Barn is doing (Priority: P3)

The player opens the hut and sees what it's doing: waiting for rough buds, trimming, curing (and how many are hanging), or full, along with what it has made. When it can't work, it says why in plain words.

**Why this priority**: helps players fix a stalled supply chain, but the loop works without it.

**Independent Test**: starve the hut of rough buds, then fill its racks, and check that its status says so each time.

**Acceptance Scenarios**:

1. **Given** no rough buds anywhere in the colony, **When** the player opens the hut, **Then** it says it is waiting for rough buds.
2. **Given** every Curing Rack full and none finished, **When** the player opens the hut, **Then** it says the buds are curing and when the next ones are due.

### Edge Cases

- **No rough buds in the colony:** the worker asks for them through the colony's normal requests and waits; it never takes cured buds back off the shelves to re-trim them.
- **Racks full:** the worker stops trimming once it has more trimmed buds than free rack space, and goes back to it as racks free up.
- **A player uses the hut's trays or racks by hand:** a player can still use them as normal; the worker works around buds a player hung, and takes down only what the hut hung or what has finished curing.
- **A tray or rack is broken or missing:** the hut keeps working with what is left; with no trays or no racks it says what's missing.
- **Mixed strains:** each bud keeps its strain from rough to cured; buds of different strains can hang on the same rack.
- **The hut is upgraded or the server restarts mid-cure:** buds hanging on racks keep their curing progress, as racks do for players.
- **MineColonies is not installed:** nothing about the Bud Barn exists or loads; Trim Trays and Curing Racks work for players as before.
- **The worker is missing (no citizen hired):** nothing happens and the hut says it needs a worker.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Bud Barn MUST exist only when MineColonies is installed; without it, the mod MUST load and play exactly as before.
- **FR-002**: The Bud Barn MUST be a MineColonies hut with one worker, whose job is named **Trim Daddy** (approved by the user, 2026-11-03).
- **FR-003**: The worker MUST obtain rough buds through the colony's normal request and delivery system, and MUST NOT take cured or trimmed buds as input.
- **FR-004**: The worker MUST trim at the hut's own Trim Trays, one rough bud per trim, producing one trimmed bud of the same strain and traits.
- **FR-005**: Each trim MUST roll for a seed with the same chance a player trimming that strain gets, using the same configured values; a seed MUST carry the bud's strain and traits.
- **FR-006**: The worker MUST hang trimmed buds on the hut's Curing Racks and MUST take down cured buds when the rack has finished them; curing time MUST be the rack's normal time.
- **FR-007**: Cured buds and seeds MUST go into the hut's storage, where the colony's couriers and requests can take them, so a Dispensary's request for cured buds can be filled from them.
- **FR-008**: The hut MUST work with no BI power at any level.
- **FR-009**: Each of the five levels MUST have more combined Trim Trays and Curing Racks than the level below, and the worker MUST work faster at each level.
- **FR-010**: The worker MUST keep only as many trimmed buds as there is free rack space for, plus a small buffer, so trimmed buds don't pile up.
- **FR-011**: The hut MUST show its state: waiting for rough buds, trimming, curing (with how many are hanging and the next finish), racks full, or missing trays, racks or a worker.
- **FR-012**: Buds a player hung on the hut's racks MUST NOT be removed by the worker before they finish curing.
- **FR-013**: The Sensi Heights style MUST include Bud Barn levels 1 to 5, sharing one footprint, in the agriculture earth tones (no neon lime, no quartz), passing the pack's own blueprint checks.
- **FR-014**: The numbers that tune the hut (trays and racks per level, worker speed per level, the trimmed-bud buffer) MUST be config values.

### Key Entities

- **Bud Barn**: the colony building; has a level (1-5), its Trim Trays and Curing Racks (from its blueprint), its storage, and a status.
- **Trim Daddy**: the Bud Barn's worker, the citizen employed there; carries rough and trimmed buds, works at trays and racks, delivers cured buds and seeds.
- **Rough, trimmed and cured buds; seeds**: existing items, each carrying a strain and traits that never change along the way.
- **Trim Tray and Curing Rack**: existing blocks, used by the worker the same way a player uses them.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A colony with a weed farm, a level 1 Bud Barn and a Dispensary keeps the Dispensary serving through 5 Minecraft days with no player handling weed.
- **SC-002**: Every rough bud the worker takes becomes exactly one cured bud of the same strain and traits: none lost, none duplicated, none changed.
- **SC-003**: Over 400 trims of one strain, the share that gave a seed is within 5 percentage points of that strain's configured chance.
- **SC-004**: With plenty of rough buds, cured buds per Minecraft day rise at every level from 1 to 5, with level 5 making at least four times level 1.
- **SC-005**: All five levels build with a colony builder without errors, and each has its trays and racks working.
- **SC-006**: With MineColonies removed, the mod loads and every existing game test passes.

## Clarifications

### Session 2026-11-03

- Q: What is the worker's job called? → A: Trim Daddy.

## Assumptions

- The worker reuses the colony's existing request system for rough buds, the way other huts request their materials; nothing new is needed in the warehouse.
- Curing time stays the Curing Rack's normal time (5 minutes per bud); the levels go faster by having more racks and a quicker worker, not by curing faster.
- The worker trims and cures only weed; other crops are not touched.
- Rough buds already in a player's or another hut's possession are not taken.
- The Sensi Heights blueprints are generated in the same way as the animal huts (one plan grown through five levels) and reviewed by the user in the flatworld showcase.
- Seeds go to storage rather than straight to a farmer; the farmer gets them through the colony's normal requests.
