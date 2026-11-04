# Feature Specification: Kush Suit, Blazer and Charging

**Feature Branch**: `005-kush-suit`
**Created**: 2026-11-04
**Status**: Draft
**Input**: User description: "Kush Suit, Blazer and charging: milestone 4 of IgnusVM/claude-misc four-twenty/HANDOFF.md. Wearable gear powered by Boundless Inspiration (BI) that works only while the wearer is high AND has stored BI; sober or empty, it does nothing. Hotbox Helm (100k BI), Puff Plate (400k) with the Puff Pack jetpack, Couchlock Greaves (200k), Moon Boots (100k), and the Blazer arm laser (1M) with four modes. Each piece has a small terpene inventory taking only its own hue family; each colour unlocks one ability or mode, data-driven. Charging at the Re-Up Station (four tiers) and with the portable Dab Cell. Nothing depends on MineColonies."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Suit up and get powers while high (Priority: P1)

A player crafts the Kush Suit pieces from the mod's own materials, charges them, smokes, and puts them on. While they are high and the pieces hold BI, each piece does its thing:

- **Hotbox Helm:** night vision, and a haze HUD showing their BI, their terps and how high they are.
- **Couchlock Greaves:** no knockback, and a ground stomp.
- **Moon Boots:** a high jump and no fall damage.
- **Puff Plate:** the heaviest armour.

When the high wears off, or a piece runs out of BI, that piece's powers stop.

**Why this priority**: it's the heart of the milestone, and the reason to have a BI network and to keep smoking.

**Independent Test**: charge all four armour pieces, get high, wear them, and check each power works; then sober up, and separately drain a piece, and check its powers stop.

**Acceptance Scenarios**:

1. **Given** a charged Hotbox Helm and a high player, **When** they wear it in the dark, **Then** they see as in daylight, and the HUD shows BI, terps and how high they are.
2. **Given** charged Moon Boots and a high player, **When** they jump, **Then** they jump much higher than normal, and they take no damage when they land.
3. **Given** the same player sober, **When** they jump and fall, **Then** the boots do nothing: a normal jump, and normal fall damage.
4. **Given** charged Couchlock Greaves and a high player, **When** a mob hits them, **Then** they aren't knocked back; **When** they crouch-land or use the stomp, **Then** nearby mobs are hurt and pushed away.
5. **Given** any piece at zero BI, **When** worn by a high player, **Then** none of its powers work.

---

### User Story 2 - Fly with the Puff Pack, and never die of it (Priority: P1)

The Puff Plate carries a jetpack. A high player with BI in the Plate can fly. If they sober up or the Plate runs dry mid-flight, they are lowered gently to the ground and never take fall damage from it.

**Why this priority**: flight is the headline power, and the constitution forbids flight ever killing the wearer.

**Independent Test**: fly high, then let the high end; separately, fly until the Plate is empty; in both cases the player drifts down and lands unharmed.

**Acceptance Scenarios**:

1. **Given** a charged Puff Plate and a high player, **When** they hold jump in the air, **Then** they fly, using BI as they go.
2. **Given** a flying player 100 blocks up, **When** their high ends, **Then** they float down slowly and land with no damage.
3. **Given** a flying player, **When** the Plate's BI runs out, **Then** the same safe descent happens.

---

### User Story 3 - The Blazer (Priority: P2)

The Blazer is worn on the arm. A high player with BI in it can fire it in the mode they've chosen:

- **Blaze Beam:** a ranged weapon.
- **Bore Toke:** mines a 3×3 area.
- **Timber Torch:** fells a whole tree.
- **Paint Puff:** places lines or walls of a block from the inventory.

Each mode costs BI per use, and the modes beyond Blaze Beam are unlocked by terpenes (see User Story 4).

**Why this priority**: a strong tool and weapon, but the suit works without it.

**Independent Test**: with a charged Blazer and the right terpenes, fire each mode while high, and check its effect and its BI cost; then check that sober, nothing fires.

**Acceptance Scenarios**:

1. **Given** Blaze Beam selected, **When** a high player fires at a mob, **Then** it takes damage at range and the Blazer loses BI.
2. **Given** Bore Toke unlocked and selected, **When** fired at stone, **Then** a 3×3 area is mined and drops as normal.
3. **Given** Timber Torch unlocked, **When** fired at a tree trunk, **Then** the whole tree comes down and drops its logs.
4. **Given** Paint Puff unlocked with blocks in the inventory, **When** used, **Then** a line or wall of those blocks is placed, using them up.
5. **Given** a protected area (spawn protection or a claim), **When** any mode would change blocks there, **Then** nothing in it changes.

---

### User Story 4 - Terpenes unlock abilities (Priority: P2)

Each piece has a small terpene inventory that takes only its own hue family:

| Piece | Hue family |
|---|---|
| Blazer | reds and oranges |
| Hotbox Helm | yellows |
| Puff Plate | greens |
| Moon Boots | blues |
| Couchlock Greaves | purples |

Putting a terpene of the right colour in unlocks the ability that colour stands for, such as zoom on the Helm, hover on the Plate, water walking on the Boots, or Bore Toke on the Blazer. Wrong-family terpenes are refused. Which colour unlocks what is a list that can grow.

**Why this priority**: gives the colour-catching and the Terpinator a purpose, and makes gear grow with the player.

**Independent Test**: insert a correct-family terpene and see its ability appear; try a wrong-family one and see it refused; remove the terpene and see the ability go.

**Acceptance Scenarios**:

1. **Given** the Moon Boots and a blue terpene that maps to water walking, **When** it's placed in the boots, **Then** a high wearer with BI walks on water.
2. **Given** a red terpene, **When** a player tries to put it in the Moon Boots, **Then** it isn't accepted.
3. **Given** a terpene that is in the right family but not yet mapped to an ability, **When** inserted, **Then** it is accepted but does nothing, and the screen says it has no ability yet.

---

### User Story 5 - Charging (Priority: P1)

Gear needs BI. The Re-Up Station, in four tiers (Puff, Toke, Rip, Blaze), charges gear hung on it, or worn by a player standing on its pad, from the BI network. A better tier charges faster. The Dab Cell is a handheld battery that charges the gear the player wears or carries, and is itself charged at a Re-Up Station.

**Why this priority**: without charging, no gear works.

**Independent Test**: hang an empty piece on a Puff Re-Up Station fed by a Hookah, and watch it fill; stand on the pad wearing gear and watch it fill; carry a charged Dab Cell and watch worn gear top up.

**Acceptance Scenarios**:

1. **Given** a Re-Up Station connected to a powered network, **When** a piece is hung on it, **Then** the piece's BI rises steadily until full, drawing from the network.
2. **Given** a player wearing gear on the station's pad, **When** they stand there, **Then** every worn piece charges.
3. **Given** a charged Dab Cell in the inventory, **When** worn gear is below full, **Then** the Dab Cell moves BI into it until either is empty or full.
4. **Given** a Re-Up Station fed from a higher tier than its own, **When** power arrives, **Then** it overloads and stops, safely, without breaking (the network's usual rule).

### Edge Cases

- **Sober with BI:** nothing works, and BI isn't spent.
- **High with no BI:** nothing works; the HUD (if the Helm has BI) or the item tooltip says the piece is empty.
- **Mid-flight power loss** in any form (sober, empty Plate, Plate taken off, dimension change): always a safe, slow descent with no fall damage.
- **Moon Boots run out mid-jump:** normal fall damage applies (no landing safety for the boots, as planned).
- **Armour protection when sober or empty:** protection stays; it's armour. Only the powers switch off (decided 2026-11-04).
- **Blazer in protected land:** no blocks are changed; Blaze Beam may still hurt mobs.
- **Death:** gear keeps its BI and terpenes like any item.
- **Multiplayer:** the HUD and the beam's visuals show only to the right players; a server without the mod on a client refuses it as for any FourTwenty item.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: One release MUST include all five pieces, the Re-Up Station in four tiers and the Dab Cell (decided 2026-11-04).
- **FR-002**: Every power on every piece (not its armour protection, which always applies) MUST work only while the wearer is high **and** the piece holds BI; otherwise it does nothing and spends nothing.
- **FR-003**: Each piece MUST store BI up to its capacity: Helm 100,000; Plate 400,000; Greaves 200,000; Boots 100,000; Blazer 1,000,000. Its tooltip and the Helm HUD MUST show its charge.
- **FR-004**: Each power MUST spend BI, per second while it's running or per use, at rates set in config.
- **FR-005**: The Hotbox Helm MUST give night vision and show a HUD with the wearer's BI per piece, terpenes and how high they are (strain and time left).
- **FR-006**: The Puff Plate MUST be the most protective piece, and MUST let a high wearer with BI fly by holding jump.
- **FR-007**: Flight MUST NEVER kill the wearer. Whenever flight ends for any reason other than the player landing, the wearer MUST be lowered slowly and take no fall damage from that descent.
- **FR-008**: The Couchlock Greaves MUST stop knockback and give a ground stomp that hurts and pushes nearby mobs (not players unless PvP is on).
- **FR-009**: The Moon Boots MUST give a high jump and stop fall damage; when sober or empty they MUST do nothing, with no landing safety.
- **FR-010**: The Blazer MUST have four modes, Blaze Beam, Bore Toke (3×3), Timber Torch (whole tree) and Paint Puff (lines or walls from inventory blocks), switched by the player. Blaze Beam MUST work with no terpene; the other three MUST each need their unlocking terpene.
- **FR-011**: Every block change the gear makes MUST respect spawn protection, claims and other mods' break and place checks, and drop blocks as normal mining would.
- **FR-012**: Each piece MUST have a small terpene inventory that accepts only its hue family. A list, not code, MUST say which colour unlocks which ability, so more can be added without new code.
- **FR-013**: The starting list MUST map at least two abilities per armour piece and the three Blazer modes.
- **FR-014**: The Re-Up Station MUST come in four tiers that follow the network's tier and overload rules, charge hung gear and gear worn on its pad, and charge faster at higher tiers.
- **FR-015**: The Dab Cell MUST store BI, charge at a Re-Up Station, and move BI into the gear its holder wears or carries.
- **FR-016**: All pieces, stations and the Dab Cell MUST be crafted from the mod's own progression (hemp, extracts, Stash and Wick parts) plus common materials, not from late-game vanilla items.
- **FR-017**: Nothing in this feature may require MineColonies.

### Key Entities

- **Gear piece**: one of the five; holds BI (with a capacity), a small terpene inventory, and (for the Blazer) a selected mode.
- **Ability**: a power a piece gives, either built in or unlocked by one colour; has a BI cost.
- **Ability list**: which colour unlocks which ability on which piece; data, extendable.
- **Re-Up Station**: a charger block of a tier, with a hanging slot and a pad.
- **Dab Cell**: a handheld BI store that charges gear.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In 100 tests of flight ending mid-air (by sobering, by running dry, by removing the Plate), the wearer takes no damage from the descent every time.
- **SC-002**: With the wearer sober, no piece spends any BI or gives any power, measured over 10 minutes worn.
- **SC-003**: A full Puff Plate gives at least 2 minutes of continuous flight, and a full Moon Boots charge lasts at least 30 minutes of normal walking and jumping.
- **SC-004**: A Blaze Re-Up Station on a network that can feed it fills empty Moon Boots in under 2 minutes, and each tier charges twice as fast as the one below (so a Puff station takes about 12 minutes: gear is mid-to-late game, as the planned capacities imply).
- **SC-005**: Every Blazer mode changes nothing inside a protected area, in every test.
- **SC-006**: A player can go from no gear to a working suit using only the mod's own machines and common materials, with no Nether or End trip.

## Clarifications

### Session 2026-11-04

- Q: One release or two? → A: everything in one release.
- Q: Does armour protection stay when sober or empty? → A: yes; only the powers switch off.

## Assumptions

- "High" uses the mod's existing high, which now survives restarts.
- Hue families are read from the colour of the terpene: the 16 hue rows of the 64 terpene colours, plus the matching Minecraft colours.
- Gear goes in the normal armour slots; the Blazer is held in a hand like a tool.
- Abilities beyond the base ones are a starting set the user can extend.
- Visuals (models, textures, the HUD and the beam) are generated in the repo's usual way and tuned in playtesting.
