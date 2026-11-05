# Feature Specification: Circuit Solver

**Feature Branch**: `006-circuit-solver`
**Created**: 2026-11-04
**Status**: Draft
**Input**: User description: "Circuit solver: real electronics for Boundless Inspiration (BI), the first step of the electronics milestone in IgnusVM/claude-misc four-twenty/HANDOFF.md, under the school of Inspoweednetics. Units: power Willards (W), current Ignus (Ig), voltage Kerberos (Kb), resistance Phu/Phus (Φ), stored energy Willard-seconds (Ws). Each power tier is a voltage: Puff 4.2 Kb, Toke 42 Kb, Rip 420 Kb, Blaze 4,200 Kb. Each Wick network is solved as a real DC circuit by Kirchhoff's laws and I=V/R. Hookahs and discharging batteries are sources with internal resistance; machines are resistive loads; Stashes charge and discharge by voltage; Wicks have small real resistance; the Hit Box steps between tiers. Overload and burnout stay. Everything plays the same as now on short lines. A meter item reads V, I, R and P. Gear and the Pave Pen are untouched. Nothing depends on MineColonies. Out of scope: switches, placeable resistors, logic gates, AC, chips, the gold wire."

## Background

Boundless Inspiration is the mod's electricity, and its technology belongs to the school of **Inspoweednetics**. Branch `006-willards` already measures it in real units: power in **Willards (W)** and stored energy in **Willard-seconds (Ws)**. A Hookah puts out 420 W.

This feature makes the power network obey real DC circuit laws, using the approved units:

| Quantity | Unit | Symbol | Plural |
|---|---|---|---|
| Power | Willard | W | Willards |
| Current | Ignus | Ig | Ignus |
| Voltage | Kerberos | Kb | Kerberos |
| Resistance | Phu | Φ | Phus |
| Energy | Willard-second | Ws | Willard-seconds |

Each tier becomes a voltage class:

| Tier | Voltage | A Hookah of the tier | Its current | A Wick's rating |
|---|---|---|---|---|
| Puff | 4.2 Kb | 420 W | 100 Ig | 200 Ig |
| Toke | 42 Kb | 840 W | 20 Ig | 40 Ig |
| Rip | 420 Kb | 1,680 W | 4 Ig | 8 Ig |
| Blaze | 4,200 Kb | 3,360 W | 0.8 Ig | 1.6 Ig |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - My base keeps working, now with real electricity (Priority: P1)

A player with an existing base updates the mod. Their Hookahs, Wicks, Stashes, machines, Grow Ops, benches and Re-Up Stations keep running as they did: the same speeds, the same burn lengths, the same Stash sizes. Under the hood, each network is now a real circuit: the Hookah drives a voltage, each machine draws current by I = V/R, and the currents add up by Kirchhoff's laws.

**Why this priority**: the circuit model is only worth having if nothing the player built breaks. Everything else stands on it.

**Independent Test**: build the standard short setups (one Hookah and four machines; a Stash between a Hookah and machines; a machine with a high player near it) and check that each runs exactly as fast as it does today.

**Acceptance Scenarios**:

1. **Given** one Puff Hookah and four Puff machines on a short Wick line, **When** the Hookah burns, **Then** all four machines run at full speed for the minute, as they do today.
2. **Given** a Stash Jar on a line with a lit Hookah and no load, **When** time passes, **Then** the Stash charges; **When** the Hookah goes out and machines need power, **Then** the Stash discharges into them.
3. **Given** a machine with a high player within 21 blocks, **When** it runs, **Then** it runs free and draws nothing from the line.
4. **Given** a world saved before this feature, **When** it loads, **Then** every Stash, Hookah and machine has the charge it had, and nothing has overloaded or burned out from the change.

---

### User Story 2 - Long lines lose a little, and stepping up pays (Priority: P1)

A player runs a Puff Wick line 100 blocks to a far field of Grow Ops. Because each Wick has a little real resistance, the far end sees a slightly lower voltage, the machines there get a little less power, and the line warms up the world by the difference (as I²R loss). The player puts a Hit Box at each end to step up to Toke for the long run. At ten times the voltage, the same power travels at a tenth of the current, so the loss drops sharply.

**Why this priority**: it's the one new thing players feel, and the reason a real circuit model is worth the trouble: it rewards building like an engineer.

**Independent Test**: power the same load through a 100-block line at Puff, then through the same line stepped up to Toke by Hit Boxes, and compare the power that arrives.

**Acceptance Scenarios**:

1. **Given** a load at the end of a 100-block Puff line carrying its rated current, **When** the line is solved, **Then** between 2% and 5% of the power is lost on the way.
2. **Given** the same load and line with a Hit Box stepping up to Toke at the start and down to Puff at the end, **When** the line is solved, **Then** at most half as much power is lost on the Wicks as at Puff, plus whatever the Hit Boxes lose.
3. **Given** a line of 10 blocks or fewer, **When** it carries any current up to its rating, **Then** machines on it run at full speed (the loss is too small to matter).

---

### User Story 3 - See the electricity (Priority: P2)

A player wants to know what their network is doing. They point the Inspoweedometer at any BI block or Wick and read the voltage (Kb), current (Ig), resistance (Φ) and power (W) there. Machine, Stash, Hookah and plot screens show the voltage and current at the block next to their charge bars.

**Why this priority**: real electronics are only fun if you can measure them. It's also the main way to find a sagging line or a Wick close to burning out.

**Independent Test**: build a known circuit (one Hookah, a 10-block line, two machines), read it with the Inspoweedometer, and check the numbers against hand calculation.

**Acceptance Scenarios**:

1. **Given** a running Puff machine, **When** the player reads it with the meter, **Then** they see the voltage across it, the current through it, its resistance and the power it draws, and V = I × R and P = V × I hold to the shown precision.
2. **Given** a Wick carrying current, **When** the player reads it, **Then** they see the current through it, the voltage at it and how close it is to its rating.
3. **Given** an unpowered line, **When** the player reads it, **Then** it shows 0 Ig and the line's resting voltage (0 Kb with no source).

---

### User Story 4 - Overloads and burnouts follow the physics (Priority: P2)

A player connects a Toke source to a Puff machine. The machine sees ten times its rated voltage, overloads, smokes and stops, but never breaks. Separately, the player hangs eight Puff machines and two Hookahs on a single Hemp Wick. The current through that Wick goes past its 200 Ig rating, and it burns out in smoke and ash.

**Why this priority**: these are the existing failure rules, now expressed in Kerberos and Ignus. They must keep working, but they're not new.

**Independent Test**: overload a block with a higher tier, then overcurrent a Wick, and check that the block stops while the Wick burns.

**Acceptance Scenarios**:

1. **Given** a block of one tier, **When** it sees more Kerberos than its tier's voltage with margin, **Then** it overloads for the existing time, smokes and stops, and recovers once the higher voltage is gone.
2. **Given** a Wick, **When** the current through it stays above its rating for one second, **Then** it burns out in smoke and leaves ash; nothing explodes.
3. **Given** a Wick, **When** a brief spike passes, such as a Hookah lighting, **Then** it doesn't burn out unless the current stays over the rating for the full second.

---

### Edge Cases

- **Too many machines on one Hookah:** the voltage sags, and every machine on the line slows down evenly, instead of some running and others starving.
- **No source:** a network with only loads sits at 0 Kb, and its machines show they're waiting for power.
- **A source with nothing to feed:** a lit Hookah on a line with no load and no Stash with room draws nothing. A Hookah only lights when something on its Wicks needs power, as now.
- **Two sources of the same tier:** they share the load by the circuit; neither feeds the other backwards beyond what the circuit says.
- **Stashes of different fullness on one line:** the fuller one charges the emptier one until they settle, as now, without passing the same power back and forth.
- **A loop of Wicks:** a ring or mesh of Wicks is solved properly: current divides between the paths.
- **A huge network:** the number of blocks in one network is capped, as now; past the cap the network still works but splits.
- **Breaking or placing a block mid-run:** the network is re-solved, with no lost charge and no spurious burnout.
- **One-off costs** (an Infuser catching a colour, a Buzz Bot recharging): these take stored Ws from Stashes on the network, as now.
- **Chunk borders:** a network reaching into an unloaded chunk treats it as cut off at that edge, as now.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every Wick network MUST be solved as a DC circuit that satisfies Kirchhoff's current and voltage laws, with I = V/R at every resistive part.
- **FR-002**: Each tier MUST have a rated voltage: Puff 4.2 Kb, Toke 42 Kb, Rip 420 Kb, Blaze 4,200 Kb.
- **FR-003**: A Hookah (Single, Double Hose, Big Bertha, Grand Hookah) MUST act as a voltage source with a small internal resistance. Feeding the loads it is sized for (four machines of its tier), it MUST deliver its rated power, 420 W at Puff, within 2%.
- **FR-004**: Machines, Grow Op plots, Construction benches, the Think Tank, the Rosin Rig, Pheno Chambers, Re-Up Stations, the Terpinator and the Inspiration Infuser MUST act as resistive loads sized from today's draw (R = V² / P at their rated voltage).
- **FR-005**: A load MUST run at full speed when it receives at least 98% of its rated power. Below that, it MUST run in proportion to the power it gets. Above its rated power it MUST NOT run faster.
- **FR-006**: A Stash MUST act as a battery: a voltage that rises with its charge, and an internal resistance. It charges when the line is above its voltage and discharges when below, at no more than today's rate for its tier.
- **FR-007**: Each Wick block MUST have a resistance, set per tier, such that a 100-block line carrying its rated current loses 2% to 5% of the power it carries.
- **FR-008**: The Hit Box MUST step voltage between adjacent tiers by a factor of 10 in either direction, conserving power apart from a small loss of no more than 2%.
- **FR-009**: A block MUST overload (smoke, stop, keep its contents and progress, never break) when it sees a voltage above its tier's rated voltage by more than a set margin. It recovers when the voltage falls back.
- **FR-010**: A Wick MUST burn out (smoke and ash, nothing explodes) when the current through it stays above its rating for one second. The rating is twice its tier's Hookah current: Puff 200 Ig, Toke 40 Ig, Rip 8 Ig, Blaze 1.6 Ig.
- **FR-011**: A load with a high player within 21 blocks MUST run free and draw no current.
- **FR-012**: Players MUST be able to read the voltage, current, resistance and power at any BI block or Wick with a hand-held meter item, the **Inspoweedometer** (name approved 2026-11-04).
- **FR-013**: The screens of BI blocks MUST show the voltage and current at the block alongside their charge.
- **FR-014**: Every displayed value MUST use the approved units and symbols: W, Ws, Ig, Kb, Φ. Each value MUST use a sensible prefix or precision so it stays readable: 0.8 Ig, 4.2 Kb, 0.168 Φ.
- **FR-015**: Saved worlds MUST load with every stored charge kept. Changing over to the circuit model MUST NOT trigger an overload or burnout.
- **FR-016**: The circuit MUST be re-solved whenever the network changes: a block placed or broken, a Hookah lit or out, a load starting or stopping, or a Stash filling or emptying past what its voltage reflects.
- **FR-017**: Every number that sets the circuit (voltages, internal and Wick resistances, overload margin, burnout time) MUST be tunable in the server config.
- **FR-018**: Gear, the Dab Cell and the Pave Pen MUST keep storing and spending Ws exactly as now. Nothing in this feature may depend on MineColonies.

### Key Entities

- **Network:** a connected set of Wicks and the BI blocks plugged into them, at one tier. It has a solved state: the voltage at every node and the current through every part.
- **Source:** a Hookah, or a Stash while it is discharging. Has a voltage and an internal resistance.
- **Load:** a machine, plot, bench or station. Has a resistance derived from its rated power and voltage; while running free, it is open.
- **Battery:** a Stash. Has a stored energy in Ws, a voltage that follows it, and an internal resistance.
- **Conductor:** a Wick block. Has a resistance and a current rating.
- **Transformer:** a Hit Box. Joins two networks of adjacent tiers at a 10:1 voltage ratio.
- **Reading:** what the meter or a screen shows at a point: Kb, Ig, Φ and W.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In every standard short setup (lines of 10 blocks or fewer, at most four machines per Hookah), every machine, plot, bench and station finishes its work within 2% of the time it takes today.
- **SC-002**: A 100-block line at its rated current delivers 95% to 98% of the power it carries. The same run stepped up one tier by Hit Boxes loses at most half as much on its Wicks.
- **SC-003**: Meter readings agree with a hand calculation of the same circuit to three significant figures.
- **SC-004**: A network of 1,000 Wicks and 100 devices adds no visible server lag: the server keeps 20 ticks a second.
- **SC-005**: Every existing power game test passes unchanged, apart from tests that name the old flow rules directly. Those tests get circuit equivalents.
- **SC-006**: Loading a world saved by the previous version produces no overload, burnout or lost charge.

## Assumptions

- **Even slowdown:** over-subscribing a Hookah, with more than four machines of its tier, slows all of them evenly, as real voltage sag would. Today some machines may run while others wait; the circuit's even sharing replaces that.
- **Hookah lighting:** Hookahs keep their current rule of lighting only when something on their Wicks needs power. "Needs power" now means the line voltage would sag below its rated value without them, or a Stash on the line has room.
- **Tiers stay apart:** a network is one tier. The Hit Box is the only way between tiers, as now, and a direct join of two tiers is what causes an overload.
- **One-off costs:** the Infuser's per-catch cost and the Buzz Bot's recharge keep taking stored Ws directly from Stashes on the network, rather than running as loads.
- **Re-solving is not per tick:** the circuit is re-solved when something changes. In between, the last solution is used to move energy each tick, so cost stays low on stable networks.
- **No magnetism, frequency or capacitance yet:** this is DC only. AC, capacitors and inductors belong to later steps of the electronics milestone.
- **Out of scope:** switches, placeable resistors, logic gates, AC, chips and the gold wire that replaces all Wicks are later steps.
