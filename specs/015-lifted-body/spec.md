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

### User Story 3 - Her looks are ours while she's Lifted (Priority: P2)

While Lifted, everything about how she looks is drawn by Four Twenty: a new, smoother **stylised model** with more joints and its own "Lifted" silhouette, the Midnight Machine boombox, the green glow, the smoke and the dance. Her moves can bend at elbows, knees and spine, she can hold props (a joint in hand while smoking), and her flying has its own poses (no walking legs in mid-air). Her real place, facing and hitbox never move for anything visual.

**Why this priority**: today the dance is layered onto MineColonies' model, which has stiff limbs, and flight mixes with walking animations, which looks twitchy (seen in game 14 Nov). It makes her look right; Stories 1 and 2 make her work right.

**Independent Test**: Watch a Lifted Builder through a smoke break, a flight to work, building, a dance flourish and a flight home, side by side with the same Builder sober; and check that her building speed and positions are the same with the new look on or off.

**Acceptance Scenarios**:

1. **Given** a Builder lighting up, **When** her high starts, **Then** she changes to the Lifted model (with the boombox and glow), still showing her name; when it ends she changes back to MineColonies' look.
2. **Given** two Lifted Builders with different MineColonies skins, **When** you look at them, **Then** they share the Lifted design but each wears her own hair colour, skin tone and clothing colours, taken from her own skin.
3. **Given** a Lifted Builder in flight, **When** she travels, **Then** she cruises laid back (reclined, one arm behind her head, nodding to the music; no walking stride), turning smoothly toward where she's going; **When** she stops to hover and build or dance, **Then** she eases upright.
4. **Given** the dance, **When** a move calls for it, **Then** her arms and legs bend at the elbow and knee.
5. **Given** a smoke break, **When** she smokes, **Then** she holds the joint to her mouth between puffs.
6. **Given** the new look, **When** she builds, **Then** she places the same blocks from the same spots at the same speed as with it off.
7. **Given** a sober Builder, **When** you look at her, **Then** she looks exactly as MineColonies draws her.

---

### Edge Cases

- **Her high ends mid-flight or mid-trip:** she finishes the flight home and lands there (as now), then walks only from her hut.
- **She's switched off ("Fly when Lifted"), or the server stops, mid-flight:** she lands gently where it's safe, as now; flying never hurts her.
- **A door is locked, blocked, or opens onto a drop:** she uses another door; if none works she follows Story 2's scenario 4.
- **Doors players care about:** she always closes what she opened; she never opens anything that isn't a door or gate (no trapdoors, chests or fences).
- **Other citizens and players in her path:** she flies around them; nobody is pushed off a ledge.
- **Chunks unloading under her:** she's set down on the ground below, as now.
- **A player has turned the look off:** they see her in 0.26.0's look (MineColonies' citizen with the boombox, glow and dance); everyone else still sees the Lifted model.
- **Another mod draws her too** (a shader, or a mod that changes citizen models): our look applies only while she's Lifted, and if it can't be drawn she falls back to MineColonies' look rather than vanishing.
- **Players who don't have the mod's visuals** (server and client must match anyway): not supported.

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
- **FR-009**: While Lifted, her whole appearance MUST be drawn by Four Twenty with its own stylised model and art: her body, the boombox, the glow, the smoke and the dance, with poses for flying (a reclined, laid-back cruise), hovering upright while building, smoking and dancing, easing between them.
- **FR-009a**: The Lifted design MUST be tinted per Builder from her own MineColonies skin: her hair colour, skin tone and main clothing colours, read from the skin itself, so any skin (including other mods' or players' custom citizen skins) works.
- **FR-010**: The Lifted model MUST have more joints than MineColonies' citizen (at least elbows, knees, a bending spine and a turning neck) and hold props (at least a joint).
- **FR-011**: Nothing visual MAY change where she is, which way she faces, her hitbox, or what she builds and how fast.
- **FR-012**: If her Lifted look can't be drawn for any reason, she MUST fall back to MineColonies' own look, never disappear.
- **FR-013**: A sober Builder MUST look exactly as MineColonies draws her.
- **FR-014**: Flying MUST never kill or hurt her: every landing is gentle and every interruption sets her down safely.
- **FR-015**: Everything here MUST work only with MineColonies installed, and Four Twenty MUST still load and play without it.
- **FR-016**: The server MUST be able to switch the whole-body flight (FR-001 to FR-007) and the drawn look (FR-009 to FR-012) off separately, falling back to 0.26.0's behaviour.
- **FR-017**: Each player MUST be able to turn the drawn look off for themselves with a toggle in the pause menu, beside the Midnight Machine volume; with it off they see Lifted Builders in 0.26.0's look. It's that player's own setting and never affects anyone else or anything she does.

### Key Entities

- **A trip:** where her job is sending her (the build, her hut, her racks), the route there, and whether it goes through a door.
- **A doorway:** a door or gate she's passing through, its side of entry, and whether she opened it (so she closes it).
- **Her look:** the pose she's in (flying, hovering, building, smoking, dancing) and its props.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Over a full Warehouse upgrade done Lifted, she is never stranded: zero rescues or recalls, and no step leaves her more than 30 seconds without progress.
- **SC-002**: While Lifted she walks for 0 seconds outside her hut (watched over a full job).
- **SC-003**: She completes an inside repair of a closed, multi-floor building without help, and every door she used is closed afterwards.
- **SC-004**: Building speed and the blocks placed are the same with the drawn look on and off (within 5% on the same job).
- **SC-005**: The user judges her flying, smoking and dancing look smooth and natural, with no twitching between walking and flying poses.
- **SC-006**: With a colony of 3 Lifted Builders working, the server's tick time stays within 10% of the same colony with them sober.
- **SC-007**: Four Twenty without MineColonies loads and passes its game tests as before.

## Assumptions

- **Her skin:** the Lifted model has one painted design (concept first, as with the boombox, likely from one generated texture sheet), tinted per Builder from her MineColonies skin; that skin returns when she's sober. Painted variants are a later option.
- **Her hut, not the Warehouse:** in MineColonies a Builder fetches materials from her own hut (couriers stock it), so "to the Warehouse" means her hut's racks unless MineColonies sends her elsewhere.
- **Doors:** the doors and fence gates MineColonies' own walking uses count; trapdoors, iron doors needing redstone, and modded locked doors don't.
- **Hidden route search cost:** flying routes are found within the existing flight search limits; long trips are flown in legs as now.
- **Visual fallback:** if another mod replaces citizen rendering, our look may not show; she then looks as that mod draws her.
- **Players** must run the same Four Twenty version as the server (already true).

## Out of Scope

- A separate stand-in character (only if this approach hits a wall, and then in its own spec).
- Changing sober Builders, or any other colonist job.
- New moves or routines for the dance beyond what the new body makes possible (bent limbs, props); the routines from spec 014 carry over.
