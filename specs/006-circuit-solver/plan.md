# Implementation Plan: Circuit Solver

**Branch**: `006-circuit-solver` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md) | **Research**: [research.md](research.md) | **Data model**: [data-model.md](data-model.md) | **Contract**: [contracts/circuit.md](contracts/circuit.md) | **Quickstart**: [quickstart.md](quickstart.md)

## Summary

Replace the BI flow code (each source walking its route and pushing a share to every sink) with a real DC circuit solve per Wick network:
- **Hookahs:** regulated, current-limited sources.
- **Stashes:** batteries whose voltage follows their charge.
- **Machines:** constant-power loads.
- **Wicks:** small resistors with current and insulation ratings.
- **The Hit Box:** a buffered converter between tiers.

Each network is solved when it changes. Every tick, the cached solution moves Ws. Players read volts, current, resistance and power with the Inspoweedometer and on every BI screen.

## Technical Context

- **Language/Version:** Java 21, NeoForge 21.1.218, Minecraft 1.21.1.
- **Primary Dependencies:** none new.
- **Storage:** none new. Buffers keep their `ws` keys from 006-willards.
- **Testing:** JUnit for `domain/` (the solver against hand-calculated circuits); game tests in `src/gametest` for every existing power behaviour and the new loss, step-up, burnout and brownout cases.
- **Target Platform:** dedicated and integrated servers; the client shows readings only.
- **Project Type:** Minecraft mod.
- **Performance Goals:** a stable network costs nothing to solve. A re-solve of a 1,000-Wick, 100-device network takes under 2 ms, so the server keeps 20 ticks a second.
- **Constraints:** energy is never created. Wick resistance is set per block, and runs collapse before the solve.
- **Scale/Scope:**
  - **Replaced:** `BIGrid.route`, `BIPush`, and `BIRules.wire`, `wireBurns` and `flows`.
  - **Extended:** `BINode`.
  - **About 15 device classes** move from pushing to declaring a role.
  - **New:** `domain/CircuitRules`, `domain/Circuit`, `domain/Units`, `power/Circuits`, `power/Network`, `power/CircuitBuilder`, the Inspoweedometer, and two readout slots on every BI screen.

## Constitution Check

- **I. MineColonies optional:** nothing here touches it.
- **II. Terpinator and Infuser keep their game:** only how they're powered changes. The Terpinator becomes a constant-power load at its current cost. The Infuser's one-off cost still drains stored Ws.
- **III. The power rule:** `MachinePower` stays the one rule (free near a high player, else pay from the buffer, else pause). Only how buffers fill changes.
- **IV. Failure is safe:**
  - **Wick burnout:** past twice a Hookah's current of its tier, a Wick burns out in smoke and ash. That is the same rating as today, now measured in Ignus.
  - **Insulation:** a Wick on a line above 1.5× its tier's voltage also burns. This adds a way for wire to break, and blocks still never break.
  - **Blocks:** fed too high a voltage, a block overloads and stops.
  - **Safe:** nothing explodes.
  - A PATCH amendment to IV's wording ("current or insulation rating") follows in the release PR.
- **V. Approved names:** Willards, Ignus, Kerberos, Phu/Phus, Inspoweednetics and Inspoweedometer are all approved (2026-11-04). The two chart names, Phu's Law and VanMule's Kerberage Law, are the user's.
- **VI. Pure rules and tests:** the solver and every number are pure `domain/` classes with JUnit tests. Every number is a config value.
- **VII. Proven in game:** [quickstart.md](quickstart.md) is the walkthrough.
- **Registry change:** one new item, so the client and server need the same jar.

Gates pass.

## Project Structure

```text
specs/006-circuit-solver/   plan.md, research.md, data-model.md, quickstart.md, contracts/circuit.md, tasks.md (next)

src/main/java/com/malevolentgods/fourtwenty/
├── domain/
│   ├── CircuitRules.java      numbers, pu helpers, ratings
│   ├── Circuit.java           elements, MNA, Newton + clamps + brownout, Solution
│   └── Units.java             SI formatting
├── power/
│   ├── BINode.java            + role(), demandWatts(), sourceReady()
│   ├── CircuitBuilder.java    walk → nodes, union-find, run collapse
│   ├── Network.java           signature, solution, energy moves, burn timers
│   ├── Circuits.java          per-level cache and once-per-tick run, readings
│   ├── BIGrid.java            keeps version(), changed(), drain(), stored(); route() and carried() go
│   ├── BIPush.java            removed
│   └── (every device)         declares its role instead of pushing
├── item/InspoweedometerItem.java
├── client/                    readouts on TechScreen-based screens
└── processing/ProcessingConfig.java   circuit group

src/test/.../domain/CircuitTest.java, CircuitRulesTest.java, UnitsTest.java
src/gametest/.../CircuitGameTests.java (+ existing PowerGameTests kept green)
tools/generate_inspoweedometer.py
```

## Build Order

1. **Pure maths:** `CircuitRules`, `Units` and `Circuit`, with tests against hand calculations:
   - a divider;
   - two sources sharing;
   - a current-limited source;
   - battery charging and discharging;
   - a constant-power load;
   - brownout;
   - a ring;
   - a 100-block line.
2. **Builder and network:** walk, collapse, solve and move energy, behind the existing device API. The old push code stays switchable until the game tests pass.
3. **Devices:** every block declares its role. Hookah lighting moves onto "the network wants power", the Hit Box onto two sides, the Stash onto the battery model. Then remove `BIPush` and `BIGrid.route`.
4. **Failure:** Wick burn timers for current and insulation, and overload by voltage.
5. **Readouts:** the Inspoweedometer item, recipe, art and lang, and the screen slots.
6. **Config, docs, changelog;** a full test run; the quickstart in game.

## Risks

- **Behaviour drift:** the old flow and the circuit can differ in corners: Hookah lighting, Stashes settling, Hit Box banks. Mitigation: every existing power game test must stay green, plus new ones.
- **Convergence:** constant-power loads on a very resistive line can fail to converge. Mitigation: damping, a fixed-point fallback, then fair-share brownout; never an exception on the tick.
- **Mixed-Wick builds burn (by the user's choice):** a release note, and a server-side log line naming each Wick that burns for insulation.
- **Performance:** a giant mesh. Mitigation: run collapse, solving only on change, conjugate gradients above 400 nodes, and the existing network cap.
