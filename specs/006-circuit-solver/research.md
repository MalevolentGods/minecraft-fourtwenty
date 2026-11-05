# Research: Circuit Solver

All numbers below are given **per unit (pu)** of a tier, then in real units. A tier's base is its voltage `V_t` and its Hookah's power `P_t`. That gives a base current `I_t = P_t / V_t` (one Hookah's current) and a base resistance `R_t = V_t² / P_t`.

| Tier | V_t | P_t | I_t | R_t |
|---|---|---|---|---|
| Puff | 4.2 Kb | 420 W | 100 Ig | 0.042 Φ |
| Toke | 42 Kb | 840 W | 20 Ig | 2.1 Φ |
| Rip | 420 Kb | 1,680 W | 4 Ig | 105 Φ |
| Blaze | 4,200 Kb | 3,360 W | 0.8 Ig | 5,250 Φ |

Working in pu means one set of rules covers every tier, and the JUnit tests can check them on round numbers.

## R1. What a machine is in the circuit

**Decision:** a **constant-power load**, like a real switch-mode power supply. A powered device asks for a fixed number of Willards: its rated draw (today's cost per tick × 20). It takes that power at any voltage from `V_min` up to its overload limit, so the current it draws is `I = P / V`. It asks only while its buffer has room. With a full buffer it draws nothing, and it reconnects once it has used 10% (hysteresis, so it doesn't flicker). A machine running free near a high player stops using its buffer, so the buffer fills and the machine falls off the line by itself. MachinePower is unchanged.

**Rationale:** this keeps every base playing as it does now.
- **Speed:** a machine gets its full power on any healthy line, so it runs at full speed.
- **Mixed tiers:** a Toke machine on a Puff line still works, as it does today. A purely resistive Toke load would get 1% of its power at a tenth of the voltage.
- **Honesty:** constant-power input is how real electronics with regulated supplies behave. The meter still shows a resistance: the load's equivalent `R = V² / P` at the moment.

**Alternatives:**
- **Plain resistors sized `R = V²/P`:** the first idea in the spec. Rejected because it starves mixed-tier builds and makes speed depend on small voltage changes.
- **Resistive loads with a 98% speed band:** fiddly, and still breaks mixed tiers.

The spec's FR-004 and FR-005 are updated to match.

## R2. Sources: Hookahs and the Hit Box output

**Decision:** a regulated source with a current limit (CV/CC, like a bench supply). It is an EMF `E = 1.00 pu` behind an internal resistance `r = 0.005 pu` (droop, so parallel sources share). Its output current is capped at `1.0 pu`, the rated current, where it becomes a current source. It never absorbs current. A Hookah is a source while it holds Ws, so it keeps today's buffer: one burn makes 25,200 Ws, and lighting stays "only when something on the line wants power".

**Rationale:**
- **Rated output:** the current limit caps a Hookah at its rated output, which keeps "four machines per Hookah" true.
- **Sharing:** the droop shares load between parallel sources without fighting.
- **Regulated voltage:** the line sits at its nominal voltage, so meters read 4.2 Kb and not something odd.

**Alternatives:** a plain Thevenin source sized for maximum power transfer. Rejected because its voltage floats up to double at light load and confuses overload.

## R3. Stashes are batteries

**Decision:** a Thevenin battery. Its EMF follows its charge, `E = 0.96 + 0.03 × fill` pu, over an internal resistance `r = 0.01 pu`, with current clamped to ±1 pu (today's tier rate) in both directions.
- **Charging:** a lit Hookah (1.00 pu) is always above any Stash, so it charges it with whatever current the loads leave. When the Hookah hits its limit the line sags toward the Stash's EMF, and charging tapers.
- **Discharging:** with no Hookah, the line falls below the Stash's EMF and the Stash discharges into the loads.
- **Settling:** two Stashes settle by the EMF difference, with no ping-pong.

**Rationale:** loads come first and batteries charge from what's left, as a real UPS does. It also replaces the `BIRules.flows` special case.

**Alternatives:**
- **Fixed-voltage Stash:** can't tell charge from discharge.
- **Stash as a constant-power load while charging:** competes evenly with machines. Rejected.

## R4. Not enough power

**Decision:** fair-share brownout. When every source is at its current limit and the loads would pull the line below `V_min = 0.5 pu`, all constant-power loads are scaled by one factor `k` (found by bisection) until the line holds. The loads behave as real smart loads under a brownout. Every machine then gets the same fraction of its power, so all of them slow down evenly (the spec's edge case).

## R5. Wicks

**Decision:** real cables (the user's choice, 2026-11-04).
- **Resistance:** `1.5 × 10⁻⁴ pu` of its tier's `R_t` per block: Puff 6.3 µΦ, Toke 315 µΦ, Rip 15.75 mΦ, Blaze 0.7875 Φ. A 100-block line at its rated 2 pu current drops `2 × 0.015 = 3%`, inside the 2–5% the spec asks for. The same power stepped up a tier runs at half that tier's rating, so it loses 1.5%: half, as SC-002 asks.
- **Current rating:** 2 pu of its tier: Puff 200 Ig, Toke 40 Ig, Rip 8 Ig, Blaze 1.6 Ig. Above that for 20 ticks, it burns.
- **Voltage rating (insulation):** 1.5 × its tier's voltage. On a line above that, it burns after the same 20 ticks.

**Behaviour change:** a Wick of a lower tier on a higher-voltage line now burns (insulation). So does a higher-tier Wick carrying a lower tier's large current. The changelog says so plainly.

## R6. The Hit Box

**Decision:** two decoupled sides around its banks, as now, rather than an ideal transformer joining two networks into one solve.
- **Input side:** a constant-power load at its input tier's rate, asking only while its bank has room and the output side has demand. This is today's rule that a face only takes power when something past the other side can use it.
- **Output side:** a CV/CC source at the output tier.
- **Loss:** 2% into the bank.

**Rationale:** each network stays one tier and solves on its own, small. The bank is the energy buffer real converters have anyway.

## R7. Overload

**Decision:** a block overloads (smoke, stop, keep progress, never break) when the voltage at it is above 1.5 × its own tier's voltage. Tiers are 10× apart, so a Toke source on a Puff block is far over and a healthy line never comes near it. Overload keeps its 60-tick stop. The net effect is the same as today's "fed from a higher tier".

## R8. Solving

**Decision:**
- **Graph:** the network walk reuses BIGrid's flood fill and per-level version, plus its 100-tick rewalk for chunk loading. Wick blocks are graph nodes. Devices attach to the Wick or device they touch: two devices touching directly are merged into one node with union-find. Runs of Wicks with exactly two neighbours and no device are collapsed into one conductance (series resistances add). The voltage along a run is interpolated by cumulative resistance, and every Wick in a run shares the run's current.
- **Equations:** modified nodal analysis, solving `G·v = i` with ground implicit. Each node also gets a tiny leak to ground (`gmin`), so a Wick island with nothing on it still solves.
- **Constant-power loads:** linearised by Newton's method at the last voltage, with conductance `−P/V₀²` and current source `2P/V₀`, damped. If the solve doesn't converge in 30 iterations it falls back to a fixed-point step, `G = P/V₀²`, and then to the fair-share factor.
- **Source limits:** current clamps switch a source between CV and CC between iterations, until no clamp changes.
- **Matrix solve:** dense Gaussian elimination with partial pivoting up to 400 nodes after collapsing; preconditioned conjugate gradients above that, with all-resistive linearisation. Typical bases collapse to a few dozen nodes.
- **When it solves:** only when the network changes, using a signature of each member's discrete state (asking or not, source on or off, a Stash's fill to the nearest 2%). Every tick the cached solution moves energy: Ws per tick = W / 20, with fractional carry.

**Rationale:**
- **Exactness:** Gaussian elimination is exact enough for the hand-calculated tests (SC-003).
- **Cost:** solving only on change keeps the cost to near nothing on a stable base.

**Alternatives:**
- **Solving every tick:** wasteful.
- **Sparse LU:** more code than the networks need.

## R9. Who moves the energy

**Decision:** a per-level `Circuits` manager owns the networks. The first member to tick in a game tick runs its network's tick, once:
- **Re-solve:** if the network is dirty, solve it again.
- **Move energy:** each source's Ws goes out of its buffer and each load's goes into its own.
- **Burnout timers:** advance them.

If a source can't pay its share because its buffer ran dry, the network is marked dirty and the shortfall is taken off the loads in proportion that tick. Energy is never created.

Losses (`I²R` in the Wicks) are just gone, as heat. The Inspoweedometer shows them on a Wick.

## R10. Display

**Decision:** `domain/Units` formats every value with SI prefixes and three significant figures: `6.30 µΦ`, `100 Ig`, `4.20 Kb`, `25.2 kWs`. Screens get two more `ContainerData` slots, volts and amps, as float bits. NeoForge sends full ints with its advanced container data payload, which was checked in the 0.19 jar. The Inspoweedometer is a held item; using it on a block or Wick prints a line on the action bar (server-side, so no new payload) and a longer chat line when sneaking.

## R11. Numbers in config

Every value in R2–R7 is a `ProcessingConfig` value under a `circuit` group:
- **Tiers and sources:** the tier voltages, source droop and current limit.
- **Stashes:** EMF range, internal resistance and current limit.
- **Loads:** `V_min`, the hysteresis band.
- **Wicks:** resistance per block, current and voltage ratings, burn ticks.
- **Overload and the Hit Box:** the overload margin, the Hit Box loss.
- **Solver:** iteration limits.

Defaults live in `domain/CircuitRules`.
