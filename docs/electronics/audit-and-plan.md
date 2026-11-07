# Inspoweednetics: circuit audit and a component electronics plan

Status: proposal, 2026-11-06. Nothing here is built yet. Units are ours: Willards (W, power), Ignus (Ig, current), Kerberos (Kb, voltage), Phus (Φ, resistance), Garcias (Ga, capacitance), Marleys (Ma, inductance), seconds.

The aim is a DC simulator that is as trustworthy as SPICE's operating-point and transient analyses for the circuits players can build, without simulating electrons: lumped components, Kirchhoff's laws, real device equations. Then real parts on top of it: resistors, capacitors, inductors, diodes, and logic gates that compute Boolean logic correctly when wired.

---

## Part 1: audit of what we have (0.21.0)

### How it works today

`domain/Circuit` is a modified-nodal-analysis (MNA) solver. Every part is a conductance between two nodes and the unknowns are node voltages: **G·v = i**.

- **Wicks** are resistors (one block = a fixed Φ per tier; a contact is half a block).
- **Hookahs** are an EMF behind an internal resistance, with a current limit both ways (constant voltage until the limit, then constant current), like a bench supply.
- **Stashes** are the same with an EMF that rises with fill: a battery.
- **Machines** are constant-power loads at or above their rated voltage, and a fixed resistance (V²/P at rated) below it.
- **Lit Bits** are a piecewise-linear current-controlled LED.
- Each separate loop gets one reference node (a gauge, not a ground), plus a tiny GMIN leak on every node.
- Nonlinear parts are handled by **fixed-point relaxation**: each pass, a load becomes the resistance it had at the last voltage (G = P/V²) and an LED the secant I/V. Step size is limited, source current limits use breakpoint stepping, and if it won't settle the loads are ramped up from zero (continuation). The share it reaches is the brownout factor.
- Before solving, a linear **probe** marks loads with no loop through them, or wired backwards, and leaves them out.
- The **Hit Box** is two separate parts with a buffer between them: a load on one winding and a source on the other, exchanging energy once a tick.
- The network is solved when what its members want changes; between solves, energy moves at the solved power for 1/20 s per tick.

### What is right

| Area | Verdict |
|---|---|
| Kirchhoff's current and voltage laws | Exact. MNA enforces KCL at every node; KVL follows. |
| Ohm's law, series and parallel resistance, voltage dividers | Exact. |
| Line loss (I²R in Wicks), stepping up voltage to cut loss | Exact. |
| Source with internal resistance and current limit | Correct model of a regulated supply. |
| Battery as EMF(state of charge) + internal resistance | A standard, reasonable battery model. |
| Floating loops (no ground) | Correct: one reference per connected component is exactly the gauge freedom; GMIN is what SPICE does too. |
| Linear solve | Sparse direct elimination with minimum-degree ordering: the right method, exact to floating-point precision. |
| Energy bookkeeping | Source power includes its internal loss; loads receive what the solve says; energy is conserved per tick. |

For circuits of resistors, sources and batteries, today's solver already gives the same answers SPICE would.

### Where it falls short of SPICE

1. **Nonlinear method.** Fixed-point relaxation (secant conductances) converges for our monotone, gentle devices, but slowly, and it cannot handle exponential devices (a diode's current changes by e every 26 mV). SPICE uses **Newton–Raphson with companion models**: each nonlinear part is replaced by its tangent, a conductance G = dI/dV in parallel with a current source I − G·V, plus **voltage limiting** (pnjlim) so the exponential can't overflow. Without this, real diodes and transistors are out of reach.
2. **Convergence test.** We stop when the voltage update is small. SPICE also requires every node's current balance (the KCL residual) to be small, with relative and absolute tolerances (reltol 1e-3, vntol 1 µKb, abstol 1 pIg). A small step can hide a large residual.
3. **No time.** Each solve is a DC operating point. There is no state between ticks except buffers, so capacitors and inductors, RC delays, filters, oscillators, inrush and ringing can't exist. SPICE's transient analysis turns each energy-storing part into a companion model at every time step (backward Euler or trapezoidal integration) with step-size control from local truncation error.
4. **The probe.** Excluding "no loop" and "reversed" loads before solving is a heuristic SPICE doesn't need: a part with no loop simply carries zero current, and a polarised part's own I–V curve decides what happens when it's reversed. It works for machines, but real components must follow their equations instead.
5. **The Hit Box is not simulated as one circuit.** Two half-parts with a buffer between them add a tick of lag and can disagree with each other. Also, physically, a transformer does not pass DC: what the Hit Box does is a **DC–DC converter**. It can stay a "transformer" in the fiction but should be modelled as an ideal two-port in the matrix (input and output tied by turns ratio n: V₂ = V₁/n, I₁ = I₂/n), plus a loss, solved simultaneously with both sides.
6. **Machines are behavioural**, not components: constant power above rating, resistance below. That's fine and should stay. They're appliances, not circuit parts. But they should be stamped through the same Newton machinery as everything else, not a separate relaxation.
7. **Solve frequency.** Solving only on change is right for DC. Once anything has state (C, L, logic), those networks must be stepped every tick.

### Verdict

The linear core is sound and SPICE-equivalent. To reach SPICE quality for real components we need four engine changes: Newton–Raphson with companion models and limiting, residual-based convergence, transient analysis with companion models for C and L, and the Hit Box as a simultaneous two-port. Everything else builds on those.

---

## Part 2: the theory we'd implement

### 2.1 Modified nodal analysis

Unknowns: every node voltage, plus the current through every ideal voltage source (and every inductor in DC). Each component **stamps** its contribution into the matrix **A** and right-hand side **z**, and we solve **A·x = z**.

| Part | Stamp |
|---|---|
| Resistor R between a, b | G = 1/R added at (a,a), (b,b); −G at (a,b), (b,a) |
| Current source I from a to b | z[a] −= I, z[b] += I |
| Voltage source V, a(+) to b(−) | an extra row/column for its current: A[a][k] = A[k][a] = 1, A[b][k] = A[k][b] = −1, z[k] = V |
| Ideal transformer / converter (ratio n) | two extra equations: V₁ − n·V₂ = 0 and I₁ + I₂/n = 0 |
| Controlled sources (for gate outputs, op-amps) | standard VCVS / VCCS stamps |

The matrix stops being symmetric once voltage sources are in it, so the solver moves from symmetric elimination to sparse LU with partial pivoting (still minimum-degree ordered).

### 2.2 Newton–Raphson for nonlinear parts

At iteration k each nonlinear part, evaluated at the last voltage V_k, is replaced by its linear companion:

- conductance G_eq = dI/dV at V_k
- current source I_eq = I(V_k) − G_eq·V_k

Solve, update, repeat until both the step and the KCL residual are within tolerance. Safeguards, as in SPICE:

- **pnjlim**: limit how far a junction voltage may move in one step (logarithmically above the critical voltage).
- **GMIN stepping**: if it fails, start with a large leak to ground on every node and shrink it.
- **Source stepping**: if that fails, ramp all sources up from zero. We already have this as load continuation.

### 2.3 Transient analysis

Each game tick (50 ms) advances the circuit by Δt = 0.05 s, in substeps when a network's fastest time constant is shorter than that.

- **Capacitor** C (backward Euler): G_eq = C/h, I_eq = (C/h)·V(t−h). Trapezoidal, which is more accurate and doesn't damp ringing: G_eq = 2C/h, I_eq = (2C/h)·V(t−h) + I(t−h).
- **Inductor** L: the dual. G_eq = h/L, I_eq = I(t−h) (BE), or h/(2L) with trapezoidal history.
- **Start** each analysis with a DC operating point, in which capacitors are open and inductors are shorts.
- **Step control**: estimate local truncation error from the change in each reactive part's derivative and halve or double h, between a floor (1 µs) and the tick. A network with no capacitors or inductors takes one step.
- **Trapezoidal ringing**: use backward Euler for the first step after a switch event (a gate edge, a button), then trapezoidal, as SPICE does.

### 2.4 Diodes

The Shockley equation with series resistance:

 I = Iₛ·(e^(V/(n·V_T)) − 1), with V_T = 0.026 Kb,

plus a reverse breakdown knee (Zener), and Rₛ as a separate resistor stamp. Parameters per part (Iₛ, n, Rₛ, BV) give a silicon diode (0.65 Kb forward), a Zener and an LED (forward voltage by colour, from 1.8 to 3.2 Kb). Reversal needs no special case: the equation blocks reverse current by itself. Lit Bits would become real LEDs on this model.

### 2.5 Logic gates: how they really work, and the two ways to build them

A real logic gate is a **mixed-signal** device. Its inputs are high-impedance and read a voltage against thresholds (V_IL, V_IH). Its output is a transistor pair that pulls the pin toward its supply (V_OH) or ground (V_OL) through an output resistance. It needs power pins (VDD and GND), draws current from them, and takes a propagation delay to respond.

There are two ways to simulate that, and we'd offer both:

**A. Behavioural gate blocks (the 74-series way).** Each gate block is an analog component with a digital core, the way SPICE models digital ICs (XSPICE A/D bridges):
- **Inputs**: a large resistance to the gate's ground (megaphus), read each tick: below V_IL is 0, above V_IH is 1, between is "undefined" (treated as the last value, like hysteresis in a Schmitt input).
- **Core**: the Boolean function, evaluated from the inputs as solved at the end of the previous tick.
- **Output**: a voltage source at V_OH or V_OL behind an output resistance R_out, in the next solve.
- **Supply**: draws a quiescent current plus a little energy at each output change from its VDD/GND pins. With no supply, the output goes high-impedance (floats), as a real chip does.
- **Propagation delay**: one tick per gate by default. This is what makes logic **deterministic and stable**: every gate reads its inputs from the last settled state and drives its outputs in the next. Combinational logic of depth d settles in d ticks. Feedback is well defined: an SR latch holds its state, a ring of three inverters oscillates at a known frequency, and a D flip-flop samples on its clock edge. Loads and fan-out are real: too many inputs or a heavy load on one output pulls its voltage toward the threshold, and the logic fails there, as it would in real life.

**B. Transistor-level gates.** MOSFETs (a square-law model with threshold V_t and transconductance K), from which players can build CMOS gates themselves: an inverter from one P and one N transistor. These are real Newton–Raphson devices, accurate and educational, but heavier to solve, so most builds would use the gate blocks.

**Boolean correctness.** The behavioural core computes exact Boolean functions. The analog layer only decides whether the voltage on a pin is a valid 0 or 1. So a circuit built from gates within their fan-out and supply limits produces exactly its truth table, every time, after its settling time. Game tests would prove this with every truth table: half and full adders, a 4-bit ripple adder, SR and D latches, a counter.

---

### 2.6 Keeping the matrix symmetric

SPICE needs a general sparse LU solver because an ideal voltage source adds an unsymmetric row. We can keep our symmetric, pivot-free solver (minimum-degree elimination) by stamping every part as a positive conductance plus a current:

- **Voltage sources** as their Norton equivalent: the EMF behind a small internal resistance. Ours already are; a real source has one.
- **The Hit Box** as a transformer referred through its winding resistance R: I₁ = (V₁ − n·V₂)/R and I₂ = −n·I₁, which stamps the symmetric block [[1/R, −n/R], [−n/R, n²/R]]. R is its loss.
- **Capacitors, inductors, diodes, gate outputs** as companion models: each is a positive conductance and a current source.
- **Machines**, whose constant power has a negative slope, keep the secant (V²/P) relaxation, which is always positive.

The matrix then stays symmetric positive definite (after the one reference per loop), and the existing solver keeps working at every step.

**Precision.** The price of Norton sources is that a node's diagonal entry sums its stiff and its soft conductances in one double, which is good to 1e-16 of the largest. The smallest conductance at that node is then off by about 1e-16 × (largest / smallest), and no amount of refinement fixes it, because the matrix itself is off. So the stiffest any part may be is 1 µΦ: ideal sources, closed switches and an inductor's DC short all stop there. Against a kilophu resistor that bounds the error at 1e-7, and against a megaphu gate input at 1e-4 of the input's tiny current, both well inside SPICE's reltol of 1e-3. Within that bound, two more measures make the solve as exact as the matrix allows:

- **Hard references**: each loop's reference node is fixed at zero and removed from the system, not tied down by a conductance.
- **Iterative refinement in double-double**: the current left over at each node is computed with exact products (fma) and compensated sums, then solved for again with the same factors, three rounds. This recovers what elimination loses to stiffness.
- **Loops by conduction**: an open switch, and a capacitor in DC, don't join a loop, so a part of the circuit cut off by one gets its own reference instead of a singular matrix.

### 2.7 Built so far (branch 011-electronics)

`domain/electronics`: `Stamp` (the solver), `Part`, `Parts` (resistor, Norton source, current source, switch, capacitor, inductor, Shockley diode with Zener breakdown and pnjlim, transformer two-port), `Analysis` and `Netlist` (DC operating point by Newton–Raphson with SPICE's convergence test, GMIN stepping and source stepping; time steps by the trapezoidal rule, backward Euler after each switch). `NetlistTest` holds it to exact answers: divider, Wheatstone bridge, floating loops, RC charge at τ, 2τ and 5τ, RL rise, RLC ringing period, the Shockley forward drop, reverse blocking, a Zener across three loads, and a 10:1 transformer conserving power. **Logic** (`Logic`, `Netlist.run`): the Nugs (NOT, Buffer, AND, OR, NAND, NOR, XOR, XNOR), the Tri-State Nug, the Latch Nug, the Flip-Flop Nug and the Ticker, as mixed-signal parts. Inputs are 1 MΦ to the part's own ground pin, read at 30% and 70% of its own supply (between keeps the last reading); outputs are a switched 100 Φ pull-up or pull-down; under 1.5 Kb of supply the output floats; each draws a quiescent current. `run` is the event-driven scheduler of 4.1 option 3: solve, let every part queue an output change after its delay (inertial: a change its inputs take back first never happens), integrate the analog circuit to the next event, apply it, re-solve 1 µs on, repeat, within an event budget. Every part's delay is off by under 0.1%, fixed by build order, so symmetric circuits can't lock into a mode no real one holds. `LogicTest` proves: every gate's truth table, a half adder, all 512 sums of a 4-bit ripple adder of 20 Nugs, the latch and the flip-flop, a 3-inverter ring oscillating at 2·3·delay, the Ticker's beat, an overloaded output failing as a real one does, an unpowered Nug floating, a glitch shorter than the delay filtered out, and an RC delay triggering a Nug at −RC·ln 0.3 plus one delay.

**Active parts** (`Active`, phase 3): the **Sativa FET** (N-channel) and **Indica FET** (P-channel), MOSFETs on SPICE's level 1 square law (V_t 1 Kb, K 0.02 Ig/Kb², λ 0.01/Kb): off below threshold, a voltage-controlled resistor at low drain voltage, saturated above, drain and source swapping when the current reverses, the gate insulated, a fetlim-style step limit on the gate. The **Hype Amp**, an op-amp: gain 10⁵ on its input difference, its output limited smoothly (tanh) to 2% inside its supply, through 100 Φ; inputs 1 GΦ; under 1.5 Kb of supply its output floats; a pnjlim-style limit lets its input difference leave saturation only to the edge of the linear region, or Newton throws the output from rail to rail for ever. They stamp transconductances, so the solver is the general one (unsymmetric values on a symmetric pattern). Each loop's reference node is the minus pin of its stiffest source, which keeps precision when a node hangs off one transistor. `ActiveTest` proves: saturation current on the square law, off below threshold and the linear region, a current mirror, a CMOS inverter switching rail to rail at half supply and falling monotonically, a four-transistor CMOS NAND's truth table, the op-amp as follower, non-inverting amplifier (gain 2) and inverting amplifier (gain −2 about a reference), comparing near the rails, and floating unpowered.

**The Hit Box** is now one two-port transformer in the solve (both windings in one network, its faces passive).

**Now wired into the game**: `domain/Circuit` keeps its API, but its solve runs on this engine. Hookahs and Stashes are `LimitedSource` (a Newton device whose current-limit edges are stepped to, as pnjlim steps a junction), machines are `PowerLoad` (constant power above rating, a resistance below, stamped by secant relaxation so the matrix stays symmetric), and Lit Bits are `DriverLed`. The probe for loads with no loop, and the brownout continuation, are unchanged. The game's solves use a relative tolerance of 1e-9, as the old solver did, so the energy moved each tick balances; the engine's own default is 1e-6 (SPICE's is 1e-3). Every circuit unit test and game test passes unchanged.

## Part 3: the component system

### 3.1 Pins and wiring

Two-pin parts (resistor, capacitor, inductor, diode, LED, switch) have an **axis**: their two ends are opposite faces, like a real axial part. A diode's band (cathode) is marked. Gates have **named pins on faces**: inputs on the back and sides, output on the front, and VDD/GND on top and bottom, shown on the block.

This brings pin faces back for electronics only. Power machines keep "any side, red is + and black is −", which suits appliances. Components have pins because real components do.

Signals travel on a new thin single-conductor wire for low-current circuits (name needed), which joins pin faces and colours like Wicks. Twin Wicks carry VDD and GND to a row of gates the way a power rail does on a breadboard.

### 3.2 Parts, in build order

| Part | Model | Notes |
|---|---|---|
| Resistor | R, with a power rating | Set its value in its screen (E12 series); over its rating it burns like a Wick. |
| Switch, lever, button | 0 Φ when closed, an open circuit when open | Player inputs to logic; button is momentary. |
| Capacitor | C, rated voltage | Over-voltage pops it, safely, as a Wick burns. Polarised (electrolytic) variant later. |
| Inductor | L, series resistance | Stores energy in current; makes LC ringing and boost converters possible. |
| Diode, Zener | Shockley + Rₛ + breakdown | Rectifiers, protection, voltage references. |
| LED | Shockley, by colour | Lit Bits move onto this model. |
| Logic gates | Behavioural, 4.2 Kb logic | NOT, AND, OR, NAND, NOR, XOR, XNOR, buffer; tri-state buffer. |
| Latches and flip-flops | Behavioural | SR latch, D flip-flop with clock; built from gates too, for players who want to. |
| Clock | Behavioural oscillator | Set frequency; or build one from a 555-style circuit later. |
| Probe and meter | Inspoweedometer | Reads a node's voltage against a reference; shows a pin's logic level. |
| MOSFET (N, P) | Square-law | Phase 3, built: Sativa FET, Indica FET. |
| Op-amp | Behavioural, finite gain, rails | Phase 3, built: Hype Amp. |

Logic runs at Puff voltage: V_DD = 4.2 Kb, V_IL = 1.3, V_IH = 2.9, V_OL ≤ 0.4, V_OH ≥ 3.8 (the shape of 74HC logic at 5 V, scaled).

### 3.3 Engine plan

1. **Matrix**: sparse LU with partial pivoting and minimum-degree ordering, plus voltage-source branch rows. Same API for the existing parts.
2. **Newton–Raphson** with companion stamps for every nonlinear part, pnjlim, GMIN stepping, source stepping, and KCL-residual convergence with SPICE's tolerances.
3. **Machines and sources** move onto the same stamps (behavioural I–V curves), so there's one method for everything. The probe goes; a part with no loop simply carries nothing, and the meter reports that from the solution.
4. **Hit Box** becomes an ideal two-port (ratio n) with a loss, solved in one matrix with both sides.
5. **Transient**: per-network state (capacitor voltages, inductor currents, gate outputs), trapezoidal integration with backward-Euler starts, LTE step control, substeps within a tick when needed.
6. **Scheduler**: networks with no state still solve only when something changes. Networks with state or logic step every tick. A per-tick time budget, like the Weedweeno's, keeps a huge circuit from lagging the server; over budget, the circuit slows down rather than the server.

### 3.4 Proving it: accuracy tests

Each against an exact answer, so the engine is held to SPICE quality:

- **Linear DC**: Wheatstone bridge, ladder networks, superposition, Thévenin equivalents. Exact to 1e-9.
- **RC**: charging curve V(t) = V₀(1 − e^(−t/RC)) within 0.1% at τ, 2τ and 5τ. Time constant independent of step size.
- **RL**: current rise I(t) = (V/R)(1 − e^(−tR/L)).
- **RLC**: ringing frequency 1/(2π√LC) and the decay envelope for an underdamped circuit; no numerical damping with trapezoidal integration.
- **Diodes**: forward drop and current against the Shockley equation; a half-wave and a bridge rectifier; a Zener regulator holding its voltage across a load range.
- **Logic**: every gate's truth table; half adder, full adder, 4-bit adder (all 512 input cases); SR latch and D flip-flop sequences; a 3-inverter ring oscillator's period; fan-out limits failing where a real gate would.
- **Regression**: every existing circuit game test passes unchanged, with the same readings to 1%.

Optionally, run the same netlists through ngspice offline and keep its numbers as golden values in the tests.

---

## Part 4: decisions

Decided (user, 2026-11-06):

- **Pins**: pin faces for components (3.1). Two-pin parts have their ends on opposite faces; gates have marked pins on faces. Power machines keep any-side red/black wiring.
- **Logic model**: behavioural gate blocks first; transistors (MOSFETs) and op-amps in a later phase.
- **First release**: everything in phases 1 and 2 together: the engine, resistors, switches, capacitors, inductors, diodes, LEDs, logic gates, latches and flip-flops, the clock.

- **Gate delay**: option 3 of 4.1, real delays inside the tick, 1 ms per gate by default, with a per-tick budget.
- **Releases**: test on flatworld as we go; release when the user decides it's ready.

- **Units** (user): capacitance in **Garcias** (one Garcia, two Garcias; Ga) and inductance in **Marleys** (one Marley, two Marleys; Ma). Symbols Ga and Ma chosen by Claude.
- **Names** (user: "go with your gut"):

| Part | Name |
|---|---|
| Signal wire (thin, single conductor) | Hemp Thread |
| Resistor | Resinator |
| Switch / lever / button | Toggle Joint / Kill Switch / Puff Button |
| Capacitor | Kief Cap |
| Inductor | Spliff Coil |
| Diode | One-Hitter |
| Zener diode | Zen Hitter |
| LED | Lit Bit (already) |
| Logic gates | NOT Nug, AND Nug, OR Nug, NAND Nug, NOR Nug, XOR Nug, XNOR Nug, Buffer Nug, Tri-State Nug |
| SR latch / D flip-flop | Latch Nug / Flip-Flop Nug |
| Clock | Ticker |

| N-channel / P-channel MOSFET | Sativa FET / Indica FET |
| Op-amp | Hype Amp |

### 4.1 Gate delay: the options

A real gate responds in nanoseconds; a game tick is 50 ms. The choice is how much of a gate's timing to keep:

1. **One tick per gate.** Every gate reads last tick's inputs. Simple and fully deterministic, like redstone repeaters, but slow: a 4-bit ripple adder takes about 8 ticks (0.4 s) to settle, and anything bigger feels sluggish.
2. **Zero delay, settled within the tick.** Each tick, gates are evaluated repeatedly until nothing changes (an HDL simulator's delta cycles). Combinational logic of any depth settles in the same tick, and latches settle by feedback. A circuit that never settles, like a ring oscillator, is cut off after a fixed number of rounds and carries on next tick. Fast and exact, but there's no timing: no glitches, no races, no oscillator frequency to speak of.
3. **Real delays inside the tick.** Each gate has a propagation delay in game time, say 1 ms, and an event queue orders every change within the tick by its timestamp (a timing simulator). Logic up to about 50 gates deep settles within a tick; ring oscillators run at 1/(2·n·delay); glitches and race conditions happen exactly where they would on a real board; clocked logic can run faster than 20 Hz inside, with what players see updating each tick. Deterministic (ties broken in a fixed order), and the most realistic, at the cost of an event queue and a per-tick budget.
