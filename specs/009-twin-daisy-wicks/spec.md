# Feature Specification: Twin Wick and Daisy Wick

**Feature Branch**: `009-twin-daisy-wicks`
**Created**: 2026-11-05
**Status**: Draft
**Input**: User description: "Twin Wick and Daisy Wick: two-core cables for Inspoweednetics that wire blocks automatically. Approved names (user, 2026-11-05): Twin Wick (parallel) and Daisy Wick (series), each in every Wick tier (Hemp, Rolled, Glass, Gold) with the same voltage and current ratings as the single Wick of that tier, crafted from two single Wicks of that tier; two separate blocks, not one with a mode. Each carries a supply (+) core and a return (−) core in one block. Twin Wick: both cores continuous; every powered block it touches is connected across the cores, + to the block's + terminal and − to its −, whichever way the block faces, so it can't be wired backwards; it works laid alongside a row of blocks or inline through them, putting everything in parallel. Daisy Wick: the supply core runs into each block's + and out of its − to the next block, in order along the cable, and at the far end joins the return core automatically, closing the loop; works inline and alongside; a Daisy Wick may not branch, and a branched one carries nothing and the Inspoweedometer says why. Both take Wick colours and interoperate with single Wicks. The circuit solver itself is unchanged: the cables only generate the + and − connections. Custom textures: Twin Wick twisted red-and-black, Daisy Wick red-and-black with a band at each block it passes, with fork-shaped fittings where they plug into blocks. Overloads burn out in smoke and ash like other Wicks. Nothing depends on MineColonies."

## Background

Since polarity circuits (spec 007), every powered block has a + terminal on its left and a − terminal on its right, and Wicks are single conductors, so every circuit needs a supply line and a return line. It is real and it works, but neat builds are hard: two rails per row, blocks turned the right way, buses that must not cross. The user loves the full electronics and wants it kept, with two cables that do the routine wiring for them. A two-core cable is what electricians use for the same reason.

The circuit stays exactly as real as it is. The cables only decide which terminals join which; the same solver then solves the result, with the same losses, ratings, brownouts and burnouts.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A row in parallel with one line (Priority: P1)

A player puts a Hookah and four Snip Stations in a row and lays one line of Hemp Twin Wick along them, or between them, without caring which way any block faces. Every machine gets its full 105 W, exactly as on a pair of rails.

**Why this priority**: it is the everyday case, and it removes return lines from everyday builds.

**Independent Test**: build the row both ways (cable alongside, cable inline through the blocks) with the blocks facing at random, and compare every reading with the same circuit on red and black rails.

**Acceptance Scenarios**:

1. **Given** a Hookah and four machines with a Twin Wick running alongside all five, **When** they run, **Then** each machine reads about 4.2 Kb and 105 W, as on rails.
2. **Given** the same row, inline (Hookah, cable, machine, cable, machine...), **When** they run, **Then** the readings are the same.
3. **Given** any block turned any way against a Twin Wick, **When** it runs, **Then** it is never reversed: + always meets the + core.
4. **Given** eight machines on one Hookah's Twin Wick, **When** they run, **Then** they brown out evenly, as eight machines on rails do.

---

### User Story 2 - A string in series with one line (Priority: P1)

A player runs a Rolled Daisy Wick (Toke, 42 Kb) through ten Lit Bits and a Double Hose. Each lamp gets about a tenth of the voltage, 4.2 Kb, and lights, like a real string of LEDs on a higher voltage. The player never lays a return line: the Daisy Wick closes the loop itself.

**Why this priority**: series is the half of the request that single Wicks make hardest, and LED strings are the showcase.

**Independent Test**: a Toke source and ten Lit Bits on one Daisy line; each lamp reads about 4.2 Kb and lights. Remove one lamp and the whole string goes dark, as a real series string does.

**Acceptance Scenarios**:

1. **Given** a Daisy line through a source and three equal loads, **When** it runs, **Then** each load gets a third of the source's voltage and the same current flows through all of them.
2. **Given** a Daisy line, **When** the player lays it from either end, or puts the source first, last or in the middle, **Then** the circuit and the readings are the same.
3. **Given** a Daisy line running alongside a row of blocks, and the same blocks with the line inline through them, **When** they run, **Then** both give the same readings.
4. **Given** a lit string, **When** one block in it is broken, **Then** the line runs on through the gap if the cable is still continuous; if the gap breaks the line, the whole string stops, and the Inspoweedometer says the loop is open.
5. **Given** two sources on one Daisy line, **When** it runs, **Then** their voltages add, like batteries in series.

---

### User Story 3 - Mixing with rails (Priority: P2)

A player with an existing base on red and black rails extends it with Twin Wick, and feeds a Daisy string from the rails.

**Why this priority**: bases already built on rails must not be stranded, and the cables are more useful if they meet them.

**Independent Test**: a Twin Wick touching a red rail and a black rail powers its machines from the rails' Hookah; a Daisy line whose ends touch a red Wick and a black Wick runs as a series string between them.

**Acceptance Scenarios**:

1. **Given** a Twin Wick touching a red single Wick, **When** the circuit is solved, **Then** the red Wick joins its + core; a black single Wick joins its − core. Wicks of other colours don't join it.
2. **Given** a Daisy line with one end touching a red single Wick and the other a black single Wick, **When** it runs, **Then** the string runs between them and does not close on itself.
3. **Given** single Wicks touching a Daisy line anywhere but its ends, **When** the circuit is solved, **Then** they don't join it.

---

### User Story 4 - See it and read it (Priority: P2)

Each cable looks like what it is. The Twin Wick is a twisted red-and-black pair; the Daisy Wick is red and black with a band wherever it passes a block. Each has a fork-shaped fitting where it plugs into a block. The Inspoweedometer reads both.

**Why this priority**: the point of the feature is builds that look good and are easy to follow.

**Independent Test**: lay each cable in straight runs, corners, T-junctions (Twin only), inline and alongside, and check the models join up with no gaps or flicker; read each with the Inspoweedometer.

**Acceptance Scenarios**:

1. **Given** a Twin Wick, **When** the player reads it, **Then** the Inspoweedometer gives the voltage between its cores and the current in each core.
2. **Given** a Daisy Wick, **When** the player reads it, **Then** it gives the string's current and the voltage between its cores at that point.
3. **Given** a Daisy line that branches, **When** the player reads any block of it, **Then** it says "a Daisy line can't branch" and the line carries nothing.

### Edge Cases

- **A Daisy line with no source** carries nothing, and the Inspoweedometer says there is no source in the loop.
- **A Daisy line that branches**: a cable block with more than two Daisy neighbours, or a block touched by the line in more than two places. The whole line carries nothing; nothing burns.
- **Two blocks beside the same Daisy cable block** (one on each side): both are spliced in at that point, in a fixed order, so the string is still one series string.
- **The Hit Box**: a cable touching its front or back connects across its high winding; touching its left or right, across its low winding. Its top and bottom don't connect. Both windings stay isolated.
- **Batteries (Stashes)** on a Twin Wick charge and discharge as on rails. On a Daisy line a Stash is spliced like a source, its voltage adding to the string's; charging one through a Daisy line is not supported.
- **A Twin Wick and a Daisy Wick touching** don't join; neither do cables of different colours.
- **A short**: a Twin Wick touching nothing but a source carries no current; one bridged to itself by a single Wick from its + core to its − core shorts the source as a single Wick does.
- **Overload**: each core of a cable has the single Wick's current and voltage rating for its tier. A core over its rating burns the cable block in smoke and ash. A Daisy line broken by a burn opens the string.
- **A cable of a lower tier on a higher source** (Hemp Twin on a Double Hose) burns as a Hemp Wick would.
- **Blocks with no terminals** (anything not powered) are ignored by both cables.
- **Old bases**: nothing on rails changes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The mod MUST add the Twin Wick and the Daisy Wick in each Wick tier: Hemp (Puff), Rolled (Toke), Glass (Rip) and Gold (Blaze), eight blocks, each with the tier's names: "Hemp Twin Wick", "Hemp Daisy Wick" and so on.
- **FR-002**: Each cable MUST carry two cores, a supply (+) core and a return (−) core. Each core MUST have the resistance, current rating and voltage rating of one single Wick block of the same tier.
- **FR-003**: Cables MUST take the 17 Wick colours by dye, as single Wicks do. A cable MUST join only touching cables of the same kind (Twin or Daisy) and the same colour.
- **FR-004**: A Twin Wick's two cores MUST be continuous through every Twin Wick block it joins, and through any powered block that two or more of its blocks touch (inline wiring).
- **FR-005**: Every powered block touching a Twin Wick MUST be connected with its + terminal to the + core and its − terminal to the − core, whatever face the cable touches and whichever way the block faces.
- **FR-006**: A Daisy Wick MUST form a line: an ordered chain of cable blocks and the powered blocks they touch. Each powered block MUST be spliced into the supply core at its place in the line: a load with its + upstream and its − downstream, a source (Hookah, battery) with its − upstream and its + downstream, so that current flows one way through the whole line. The return core MUST join the line's two ends, closing the loop, unless FR-008 applies.
- **FR-007**: A Daisy line MUST give the same circuit whichever end it is read from and wherever its sources sit along it. A Daisy line that branches MUST carry nothing.
- **FR-008**: A single red Wick touching a Twin Wick MUST join its + core, and a single black Wick its − core; single Wicks of other colours MUST NOT join cables. A Daisy line whose one end touches a single red Wick and whose other end touches a single black Wick MUST run between them, from red to black, without closing on itself. Single Wicks touching a Daisy line elsewhere MUST NOT join it.
- **FR-009**: The Hit Box MUST connect to a cable through the winding of the face the cable touches (front and back: the high winding; left and right: the low winding); its top and bottom MUST NOT connect.
- **FR-010**: The cables MUST only decide connections. The circuit MUST be solved, with its losses, limits, brownouts, overloads and burnouts, exactly as for the same connections made with single Wicks.
- **FR-011**: A cable core over its rating MUST burn out its block in smoke and ash, never explode, as single Wicks do.
- **FR-012**: The Inspoweedometer MUST read a Twin Wick (voltage between its cores, current in each) and a Daisy Wick (the string's current, the voltage between its cores there), and MUST name why a Daisy line carries nothing: it branches, it has no source, or it is open.
- **FR-013**: Each cable MUST have its own art: the Twin Wick a twisted red-and-black pair, the Daisy Wick red and black with a band wherever it passes a block, both with fork-shaped fittings where they plug into a block, joining up cleanly in straight runs, corners and (Twin) junctions, glowing while live as single Wicks do.
- **FR-014**: Each cable MUST be crafted from two single Wicks of its tier: two side by side for a Twin Wick, two with an oxeye daisy for a Daisy Wick.
- **FR-015**: Nothing in this feature MAY depend on MineColonies, and existing builds on single Wicks MUST behave exactly as before.

### Key Entities

- **Twin Wick** (per tier, per colour): a two-core cable. Joins Twin Wicks of its colour, red and black single Wicks (by core), and every powered block it touches.
- **Daisy Wick** (per tier, per colour): a two-core cable forming a line; splices the blocks it touches into a series string and closes the loop with its return core.
- **Daisy line**: the ordered chain of Daisy Wick blocks and the powered blocks along them; valid when it has no branch.
- **Core**: one conductor of a cable, with a single Wick's ratings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A Hookah and four machines in a row can be powered with one line of Twin Wick, with no single Wick, in any facing, and every machine reads within 1% of the same circuit built on rails.
- **SC-002**: Ten Lit Bits on a Toke Daisy line each read within 5% of 4.2 Kb and light, with no return line laid by the player.
- **SC-003**: Every Daisy circuit reads the same (within 1%) laid from either end, and with its source first, last or in the middle.
- **SC-004**: A Daisy line that branches never carries current and is named as branched by the Inspoweedometer in 100% of cases tried.
- **SC-005**: A player can wire a parallel row with half the cable blocks the same row needs on rails (one line instead of two plus buses).
- **SC-006**: Every existing circuit game test from specs 006 and 007 still passes unchanged.

## Assumptions

- Recipes follow FR-014; the oxeye daisy marks the Daisy Wick. They can change without changing anything else.
- A powered block touched by cables on several faces joins each of them (a block can sit on a Twin line and a single-Wick rail at once, which puts those in parallel).
- A Daisy line's order is the order of its cable blocks; where several blocks sit beside one cable block, they are spliced in the fixed order north, east, south, west, up, down.
- Charging a Stash through a Daisy line is out of scope; Stashes charge on Twin Wicks or rails.
- Out of scope: switches, cables with more than two cores, automatic Hit Box routing, and converting old rails into cables.
