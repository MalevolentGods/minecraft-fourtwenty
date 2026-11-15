# Feature Specification: Lifted Body

**Feature Branch**: `015-lifted-body`
**Created**: 2026-11-15
**Status**: Draft
**Input**: "Own the Lifted Builder's body": while a Builder is Lifted, Four Twenty owns all of her movement and all of her looks; MineColonies keeps the rest of her (agreed with the user 2026-11-14, started 2026-11-15).

**Builds on:** spec 013 (Lifted Builders) and spec 014 (the Midnight Machine), released in 0.26.0. Everything here needs MineColonies, which stays optional for Four Twenty.

**Decided before writing** (user, 2026-11-14):
- **One real citizen, not a stand-in.** The Builder stays MineColonies' own citizen: her needs, requests, citizen window, home, meals, sleep, raids and work orders stay MineColonies'. A separate stand-in character that mirrors her is only considered if this approach hits a concrete wall.
- **While Lifted, her body is ours:** every trip she makes is flown, and everything about how she looks is drawn by Four Twenty.
- **While sober, nothing changes:** she walks the MineColonies way.

## Clarifications

### Session 2026-11-15

- Q: Does she fly on her breaks too (meals, bed, the hospital), since her high only pauses for them? → A: **Yes, every trip while she's high**, breaks included: she flies to the restaurant, home to bed or to the hospital, landing at the door and flying in.
- Q: What should her drawn body be like? → A: **A new, smoother stylised model**: more joints and a distinct "Lifted" silhouette, with new art for her skin (not MineColonies' blocky citizen shape).
- Q: One Lifted look for everyone, or one per Builder? → A: **One painted design, tinted per Builder** from her own MineColonies skin (hair, skin tone, clothing colours), so each stays recognisable ("for now": painted variants may come later).
- Q: How should she fly? → A: **A laid-back cruise**: reclined as if in a floating lounge chair, one arm behind her head, nodding to the music; upright when she hovers to build or dance.
- Q: Can each player turn the new look off for themselves? → A: **Yes**: the server switch, plus each player's own on/off toggle in the pause menu beside the Midnight Machine volume.
- Q (2026-11-15, after seeing every trip flown in game): Keep the new model? → A: **No.** The user loves her look as it is: she stays MineColonies' citizen with the boombox, glow and dance, polished: a still cruise pose in flight (no walking stride), and the joint held to her mouth while she smokes. The new stylised model, its per-Builder tint, its design pass and the look switches (the answers on the model, the tint and the toggle above) are dropped; a new model can come back as its own spec.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Every trip is flown while she's Lifted (Priority: P1)

While a Builder is Lifted she flies everywhere, not only around the build: home to her hut to unload, to fetch materials or to smoke; to the racks in her hut; out to the build again; and on her breaks, to the restaurant, home to bed or to the hospital. She never walks while high, so she's never stranded somewhere walking can't leave, such as a roof.

**Why this priority**: in testing on 14-15 Nov she was stranded on the Warehouse roof three times, because only build-site trips were flown and every other trip was left to MineColonies' walking, which started from wherever she'd landed. This is the main thing the feature is for.

**Independent Test**: Give a Lifted Builder a job on a tall building whose roof she works from. Fill her inventory so she has to go home to unload, and empty her hut so she has to fetch materials. Watch every trip until her high ends.

**Acceptance Scenarios**:

1. **Given** a Lifted Builder working on a roof, **When** her inventory fills, **Then** she flies straight home to her hut, unloads, and flies back, without landing on the roof or walking.
2. **Given** a Lifted Builder who needs materials from her hut, **When** she goes to fetch them, **Then** she flies there, picks them up, and flies back to the build.
3. **Given** a Lifted Builder anywhere in the colony, **When** any job step sends her somewhere, **Then** she reaches it within a reasonable time for the distance and is never left pacing in one place.
4. **Given** a Lifted Builder whose break comes (a meal, bedtime, feeling ill), **When** she goes to the restaurant, her home or the hospital, **Then** she flies there, in through the door, and flies back to work afterwards; her high stays paused throughout, as now.
5. **Given** a sober Builder, **When** she goes anywhere, **Then** she walks the MineColonies way exactly as before this feature.

---

### User Story 2 - She flies into and out of closed buildings (Priority: P1)

When a Lifted Builder's work or trip is inside a closed building (doors shut, roof on), she flies to a door, opens it, flies through and closes it behind her, and works inside; she leaves the same way. Her own hut counts: she flies in to her racks.

**Why this priority**: today flight can't reach blocks inside closed buildings, so she drops to the ground outside and walks in, which breaks "never walks while high" and stalls her on awkward stairs (the Warehouse centre stairs). It goes hand in hand with Story 1.

**Independent Test**: Give a Lifted Builder a repair on the inside of a closed building with its doors shut, and watch her go in, work on several rooms and floors, and come out.

**Acceptance Scenarios**:

1. **Given** a block to place inside a closed building, **When** a Lifted Builder goes to place it, **Then** she flies to the nearest usable door, opens it, flies in, and the door is closed again once she's through.
2. **Given** a Lifted Builder inside a building, **When** her next job step is outside, **Then** she leaves through a door the same way.
3. **Given** a building with stairs between floors, **When** she goes between floors inside, **Then** she flies up or down the stairwell rather than walking the stairs.
4. **Given** a building with no door she can use, **When** she must get inside, **Then** she places the block from outside if it's in reach; if it isn't, she works on the next reachable block and the job is never stuck for good.

---

### User Story 3 - Her look, polished (Priority: P2)

She keeps her look (MineColonies' citizen with the Midnight Machine boombox, the green glow, the smoke and the dance), polished in two places: in flight she holds a still **cruise pose** (legs together, a slight recline, leaning into her way) instead of the walking stride, and on a smoke break she **holds the joint to her mouth** between puffs.

**Why this priority**: flight mixed with MineColonies' walking stride looks twitchy (seen in game 14 Nov), and the smoke break reads better with the joint at her mouth. Stories 1 and 2 make her work right; this tidies how she looks doing it.

**Independent Test**: Watch a Lifted Builder fly a long trip, smoke a joint at her hut, and dance while building; compare her building speed and positions with the polish on and off.

**Acceptance Scenarios**:

1. **Given** a Lifted Builder flying, **When** she travels, **Then** her legs stay together without striding and she leans slightly into her way, turning smoothly; **When** she stops to hover and build or dance, **Then** the dance takes over as now.
2. **Given** a smoke break, **When** she smokes, **Then** she raises the joint to her mouth between puffs, and lowers it after.
3. **Given** the polish, **When** she builds, **Then** she places the same blocks from the same spots at the same speed as without it.
4. **Given** a sober Builder, **When** you look at her, **Then** she looks exactly as MineColonies draws her.

---

### Edge Cases

- **Her high ends mid-flight or mid-trip:** she finishes the flight home and lands there (as now), then walks only from her hut.
- **She's switched off ("Fly when Lifted"), or the server stops, mid-flight:** she lands gently where it's safe, as now; flying never hurts her.
- **A door is locked, blocked, or opens onto a drop:** she uses another door; if none works she follows Story 2's scenario 4.
- **Doors players care about:** she always closes what she opened; she never opens anything that isn't a door or gate (no trapdoors, chests or fences).
- **Other citizens and players in her path:** she flies around them; nobody is pushed off a ledge.
- **Chunks unloading under her:** she's set down on the ground below, as now.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: While a Builder is Lifted and allowed to fly, every movement MineColonies asks of her MUST be carried out by Four Twenty's flight, not MineColonies' walking: her job's trips (to the build, to her hut, to her racks, back again) and her breaks' (to the restaurant, home to bed, to the hospital).
- **FR-002**: A Lifted Builder MUST never be left walking while high, and MUST never stay more than 30 seconds without getting closer to where her job is sending her; past that she MUST be recalled to her hut, as MineColonies does with a stuck citizen.
- **FR-003**: A Lifted Builder MUST be able to enter and leave closed buildings through doors and gates: opening them on the way through and closing them behind her, whichever side she came from.
- **FR-004**: She MUST only open doors and fence gates, and MUST leave every door as she found it once she's through.
- **FR-005**: Inside a building she MUST move between floors by flying through stairwells and open space, not by walking stairs.
- **FR-006**: When a block can't be reached at all, she MUST place it from outside if it's within her reach, otherwise move on to other blocks, so no job is stuck for good.
- **FR-007**: When her high ends, she MUST finish the trip home by flight and land by her hut; only then does the MineColonies walking resume.
- **FR-008**: A sober Builder's movement MUST be exactly MineColonies' own.
- **FR-009**: In flight, a Lifted Builder MUST hold a still cruise pose (no walking stride: legs together, a slight recline, leaning into her way of travel), easing to the dance when she hovers.
- **FR-010**: On a smoke break she MUST raise the joint to her mouth between puffs.
- **FR-011**: Nothing visual MAY change where she is, which way she faces, her hitbox, or what she builds and how fast.
- **FR-013**: A sober Builder MUST look exactly as MineColonies draws her.
- **FR-014**: Flying MUST never kill or hurt her: every landing is gentle and every interruption sets her down safely.
- **FR-015**: Everything here MUST work only with MineColonies installed, and Four Twenty MUST still load and play without it.
- **FR-016**: The server MUST be able to switch the whole-body flight (FR-001 to FR-007) off, falling back to 0.26.0's movement.

### Key Entities

- **A trip:** where her job is sending her (the build, her hut, her racks), the route there, and whether it goes through a door.
- **A doorway:** a door or gate she's passing through, its side of entry, and whether she opened it (so she closes it).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Over a full Warehouse upgrade done Lifted, she is never stranded: zero rescues or recalls, and no step leaves her more than 30 seconds without progress.
- **SC-002**: While Lifted she walks for 0 seconds outside her hut (watched over a full job).
- **SC-003**: She completes an inside repair of a closed, multi-floor building without help, and every door she used is closed afterwards.
- **SC-004**: Building speed and the blocks placed are the same with the polish on and off (within 5% on the same job).
- **SC-005**: The user judges her flying and smoking look smooth and natural, with no walking stride in the air.
- **SC-006**: With a colony of 3 Lifted Builders working, the server's tick time stays within 10% of the same colony with them sober.
- **SC-007**: Four Twenty without MineColonies loads and passes its game tests as before.

## Assumptions

- **Her hut, not the Warehouse:** in MineColonies a Builder fetches materials from her own hut (couriers stock it), so "to the Warehouse" means her hut's racks unless MineColonies sends her elsewhere.
- **Doors:** the doors and fence gates MineColonies' own walking uses count; trapdoors, iron doors needing redstone, and modded locked doors don't.
- **Hidden route search cost:** flying routes are found within the existing flight search limits; long trips are flown in legs as now.
- **Players** must run the same Four Twenty version as the server (already true).

## Out of Scope

- A separate stand-in character (only if this approach hits a wall, and then in its own spec).
- A new Lifted model with its own art (dropped 15 Nov; could return as its own spec).
- Changing sober Builders, or any other colonist job.
- New moves or routines for the dance beyond what the new body makes possible (bent limbs, props); the routines from spec 014 carry over.
