# Feature Specification: Polarity Circuits

**Feature Branch**: `007-polarity-circuits`
**Created**: 2026-11-05
**Status**: Draft
**Input**: User description: "Polarity circuits: make Boundless Inspiration a real circuit simulator. Every powered block has a + terminal face and a − terminal face, clearly marked on its model; its other faces don't connect. Wicks are single conductors, so a circuit needs a supply line and a return line, like wiring a breadboard. Series wiring chains blocks − to +; parallel wiring shares two rails; the solver handles any mix. There is no implicit ground: a block not in a closed loop gets no current. Old bases with no return lines stop working on update (strict at once). The Hit Box becomes a real transformer with two isolated windings. Voltages, Wick ratings, constant-power machines, current-limited Hookahs, battery Stashes, burnout and overload stay. The Inspoweedometer reads across a block's terminals, and a Wick's voltage against a chosen reference. Out of scope: switches, placeable resistors, logic, AC, capacitors."

## Background

Release 0.20.0 solves every Wick network as a DC circuit. But each block is a single connection, and current returns through an implicit ground, so a player can't build a loop, a series string or a parallel rail. This feature gives every powered block two terminals and takes the ground away, so Boundless Inspiration behaves like a real circuit on a breadboard.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Wire a closed loop (Priority: P1)

A player places a Hookah and a Snip Station and runs a Wick from the Hookah's + face to the station's + face. Nothing happens: the Inspoweedometer on the station says "no return path". They run a second Wick from the station's − face back to the Hookah's − face, closing the loop. The station starts trimming at full power.

**Why this priority**: the closed loop is the whole idea. Without it, nothing else in this feature can work.

**Independent Test**: build a Hookah and one machine with only a supply line and check that the machine gets no power. Add the return line and check that it runs at full speed.

**Acceptance Scenarios**:

1. **Given** a Hookah and a machine joined only + to +, **When** the Hookah could burn, **Then** no current flows, the machine waits for power, and the Inspoweedometer on it reads "no return path".
2. **Given** the same pair joined + to + and − to −, **When** the Hookah burns, **Then** the machine runs at full speed, and the meter reads the line voltage across its terminals and its current through it.
3. **Given** a closed loop, **When** the player breaks either line, **Then** the current stops at once and the machine pauses, keeping its progress.

---

### User Story 2 - Series and parallel (Priority: P1)

A player puts three Snip Stations in parallel between two rails: every one sees the full line voltage. They try three in series instead, chained − to +: the Hookah's voltage divides across them. Each station gets a third of the voltage, and because machines draw a fixed power, each pulls more current to make up for it. They see it on the Inspoweedometer, and they see each station's power hold as long as the Hookah can carry it.

**Why this priority**: series and parallel are what the user asked for by name, and the difference between them is the lesson.

**Independent Test**: build both layouts from one Hookah and compare the meter readings across and through each machine with a hand calculation.

**Acceptance Scenarios**:

1. **Given** three machines in parallel between the rails of a Puff Hookah, **When** they run, **Then** each reads about 4.2 Kb across it, and the line current is the sum of theirs.
2. **Given** three machines in series, **When** they run, **Then** the voltages across them add up to the Hookah's, and the same current flows through each (Kirchhoff's voltage and current laws).
3. **Given** any mix of series and parallel, **When** it runs, **Then** the meter readings agree with a hand solution of the same circuit.

---

### User Story 3 - The Hit Box as a real transformer (Priority: P2)

A player wires a Toke loop into the Hit Box's input terminals and a separate Puff loop out of its output terminals. Power crosses with the voltage stepped down 10:1. The two loops share no Wick, so they are isolated, as a real transformer's windings are.

**Why this priority**: stepping voltage for long runs is the reason to have tiers, and an honest transformer needs two isolated windings.

**Independent Test**: run a Toke source through a Hit Box to Puff machines on a separate loop, and check the power arrives with the 2% loss and nothing leaks between the loops.

**Acceptance Scenarios**:

1. **Given** a Toke loop on the input terminals and a Puff loop on the output, **When** a machine on the output loop needs power, **Then** it gets it, stepped down 10:1, less 2%.
2. **Given** the same, **When** the player measures between a Wick on the input loop and one on the output loop, **Then** no current can flow between them: they're isolated.

---

### User Story 4 - Find the fault (Priority: P2)

A player's machines stop after the update. They carry the Inspoweedometer along their old line and read "no return path" on each machine. The changelog tells them why. They add a return line and the machines run again. For finer work, they sneak-use one Wick to set it as the reference, then read other Wicks to see each one's voltage against it, the way a multimeter's black probe works.

**Why this priority**: the update breaks old bases on purpose (strict at once), so finding and fixing the break must be quick.

**Independent Test**: load a base built on 0.20.0, check that every machine reports "no return path", and fix one by adding a return line.

**Acceptance Scenarios**:

1. **Given** a base built before this feature, **When** it loads, **Then** nothing breaks or burns, its machines get no current, and the meter on each reads "no return path".
2. **Given** the player sneak-uses a Wick with the Inspoweedometer, **When** they then use it on another Wick, **Then** it reads the voltage between the two.

---

### Edge Cases

- **Short circuit:** a Wick joining a source's + straight to its − carries the source's limited current, at most 1.05 of its rated current. The source runs flat out into the Wick and nothing else gets power. A short across several Hookahs in parallel can exceed the Wick's rating and burn it.
- **Lines touching:** see FR-004. Supply and return lines that join become one line, which is a short.
- **Reversed polarity:** a machine wired − to the supply and + to the return is reversed. [NEEDS CLARIFICATION: does a reversed machine refuse to run (it sees negative voltage and stops, safely), or run anyway like a plain resistive load? Real electronics with a regulated supply usually refuse, or are damaged.]
- **Series with a full block:** in a series string, a machine whose buffer is full draws nothing. Since nothing else in the string can pass current without it, the whole string waits. That's how a series string behaves.
- **A Stash in series:** a Stash is a battery with a voltage, so two Stashes in series add their voltages. Their tier's overload still applies to each Stash's own terminals.
- **A loop through two sources:** sources in series add their voltages, and sources in parallel share the load.
- **Floating loop:** a loop with no source sits at 0 Kb.
- **A load with only one terminal connected:** it gets no current, and the meter says "no return path".

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every powered block (Hookahs, Stashes, the Hit Box, machines, Grow Ops, Construction benches, Re-Up Stations, the Terpinator, the Think Tank, the Rosin Rig and the Pheno blocks) MUST have exactly one + terminal and one − terminal. The Hit Box MUST have two of each, one pair per winding.
- **FR-002**: A block's terminals MUST be on fixed faces relative to the way it faces: [NEEDS CLARIFICATION: which faces? e.g. + on its left and − on its right as you look at its front; or + on top and − underneath; or + on the back and − on the front]. Its other faces MUST NOT connect.
- **FR-003**: Each terminal MUST be clearly marked on the block's model, + and −, so a player can wire it without a manual.
- **FR-004**: A Wick MUST be a single conductor that joins every Wick it touches. [NEEDS CLARIFICATION: how do supply and return lines run side by side? A: they mustn't touch: two touching Wicks are one conductor, so lines laid next to each other short (honest, but fiddly to build); B: Wicks come in colours, and only the same colour joins (like insulated wire); C: there are + Wicks and − Wicks that never join each other.]
- **FR-005**: Every circuit MUST be solved from its real loops, by Kirchhoff's current and voltage laws, with no implicit ground. A block whose terminals aren't both in a loop with a source MUST get no current.
- **FR-006**: Series and parallel connections, and any mix, MUST solve correctly: voltages divide across series parts, and currents divide across parallel ones.
- **FR-007**: Hookah voltages and current limits, constant-power machines, battery Stashes, Wick resistance and ratings, burnout and overload MUST behave as in 0.20.0, measured across terminals instead of against ground.
- **FR-008**: The Hit Box MUST be a transformer with two isolated windings. It steps voltage 10:1 between adjacent tiers in either direction, with a 2% loss, and no current flows between the windings.
- **FR-009**: Worlds from before this feature MUST load without any block breaking or any Wick burning because of the change. Blocks without a closed loop simply get no current.
- **FR-010**: The Inspoweedometer MUST read a block's voltage across its terminals, its current through them, its resistance and its power. On a block with no closed loop, it MUST say "no return path".
- **FR-011**: Sneak-using a Wick with the Inspoweedometer MUST set it as the reference. Using it on another Wick then MUST read the voltage between the two. Using it on a Wick with no reference set reads the current through that Wick and its ratings, as now.
- **FR-012**: The changelog MUST tell players plainly that old bases need return lines, and how to wire one.
- **FR-013**: Every tunable number MUST stay in the server config. Nothing may depend on MineColonies.

### Key Entities

- **Terminal:** one face of a powered block, + or −. A Hit Box has four.
- **Conductor:** a set of joined Wicks, and the terminals touching them. It is one node of the circuit, or a few, joined through the Wicks' resistance.
- **Loop:** a closed path from a source's + terminal, through loads, back to its − terminal.
- **Reference:** the Wick a player chose with the Inspoweedometer, which other readings are measured against.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A player can wire a working Hookah-and-machine loop from scratch in under a minute, using only the marks on the blocks.
- **SC-002**: Series and parallel layouts of up to ten machines read, on the Inspoweedometer, within 1% of a hand solution.
- **SC-003**: The standard base, rewired with a return line, runs every machine within 2% of 0.20.0's speed.
- **SC-004**: Loading a 0.20.0 world breaks no block and burns no Wick. Every machine without a return path reports why.
- **SC-005**: Every existing power behaviour has a game test, which passes once rebuilt with closed loops.
- **SC-006**: Solving stays fast enough that the server keeps 20 ticks a second on a base of 1,000 Wicks and 100 devices.

## Assumptions

- **Earth isn't a conductor:** the ground and other blocks never carry current. Only Wicks and terminals do.
- **Touching terminals:** two blocks whose terminals face each other directly are joined at those terminals, as if by a very short Wick, so a series string can be built block to block.
- **The flatworld demo:** it is rebuilt with return lines once this lands.
- **Hera:** bases there need rewiring after the update, as the user chose. The release note and the Inspoweedometer's "no return path" make that quick.
- **Out of scope:** switches, placeable resistors, logic, AC and capacitors are later steps.
