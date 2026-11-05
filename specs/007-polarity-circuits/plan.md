# Implementation Plan: Polarity Circuits

**Branch**: `007-polarity-circuits` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md) | **Research**: [research.md](research.md) | **Contract**: [contracts/terminals.md](contracts/terminals.md) | **Quickstart**: [quickstart.md](quickstart.md)

## Summary

Every powered block gets a + terminal (left face) and a − terminal (right face). Wicks become coloured single conductors that join only their own colour. The implicit ground goes: the solver puts every part between its own two nodes, so only closed loops carry current, and series and parallel come out of Kirchhoff's laws. The Hit Box becomes a transformer with two isolated windings. The Inspoweedometer reads across terminals and against a chosen reference Wick. Reversed machines refuse to run.

## Technical Context

- **Language/Version:** Java 21, NeoForge 21.1.218, Minecraft 1.21.1.
- **Builds on:** 006's `domain/Circuit` (MNA, sparse direct solve, continuation, current limits) and `power/Network` (walk, contacts, run collapse).
- **Storage:** Wick colour is a blockstate property, and the meter reference a data component. Nothing new on block entities.
- **Testing:**
  - **JUnit:** two-terminal sources and loads; series, parallel and mixed circuits; reversed loads; an open loop.
  - **Game tests:** every existing power test rebuilt with return lines; new ones for series, parallel, coloured lines side by side, the Hit Box windings, reversed machines, and a 0.20.0-style line reporting no loop.
- **Performance:** as 006. Each block adds one node, so circuits roughly double in size. Typical bases still solve in well under a millisecond.
- **Registry:** adds a blockstate property, a data component and a recipe, so the client and server need the same jar.

## Constitution Check

- **I. MineColonies optional:** untouched.
- **II. Terpinator and Infuser keep their game:** the Terpinator gets terminals, and only its power changes. The Infuser keeps its one-off draw from Stashes on a touching Wick (research R2).
- **III. The power rule:** `MachinePower` is unchanged.
- **IV. Failure is safe:**
  - **Wicks:** burnout by current or insulation, as now, and only wire breaks.
  - **Overload:** by the voltage across a block's terminals.
  - **Reversed machines:** they refuse safely, and nothing breaks.
  - **Old bases:** nothing burns on update (R8).
- **V. Approved names:** no new names. "Coloured Wicks" are described, not named; each shows as "<Colour> <Wick>".
- **VI. Pure and tested:** solver changes go in `domain/` with JUnit tests, and world behaviour gets game tests. Numbers stay in config.
- **VII. Proven in game:** the quickstart covers it.

Gates pass.

## Project Structure

```text
specs/007-polarity-circuits/   plan.md, research.md, data-model.md, quickstart.md, contracts/terminals.md, tasks.md (next)

src/main/java/com/malevolentgods/fourtwenty/
├── domain/Circuit.java           two-node sources, batteries and loads; REVERSED; status per part
├── domain/Terminals.java         front → plus/minus faces, pure, with tests
├── power/BINode.java             + Terminal (node, sign); capability per side
├── power/BIBlock.java            front(state), plus(state), minus(state)
├── power/WickBlock.java          colour property, dyeing, same-colour joining, links to terminals
├── power/Network.java            terminal keys, two nodes per member, status
├── power/HitBoxBlockEntity.java  two windings on four faces
├── power/InspoweedometerItem.java  reference component, readings across terminals
├── client/                       Wick tint, status lines ("no return path", "wired backwards")
└── registry/ModPower.java        capabilities per side, the Wick dye recipe

tools/generate_terminals.py       + and − plates on every powered block's model
src/test/.../domain/CircuitTest.java, TerminalsTest.java
src/gametest/.../CircuitGameTests.java, PowerGameTests.java (rebuilt with return lines), PolarityGameTests.java
```

## Build Order

1. **`domain/`:** the two-terminal Circuit, with tests for series, parallel, mixed, reversed and open circuits. Then `Terminals`.
2. **Terminals in the world:** the capability per side, `front` for each block, and the Hit Box's four faces.
3. **Network:** terminal keys, two nodes per member, statuses.
4. **Coloured Wicks:** the property, joining, dyeing, the tint and the recipe.
5. **Game tests:**
   - rebuild every power test with return lines;
   - add series, parallel, side-by-side colours, the Hit Box, reversed machines and an old-style line.
6. **The Inspoweedometer:** the reference and its readings; status lines on the screens.
7. **Marks on every model** (`tools/generate_terminals.py`).
8. **Docs and changelog**, the flatworld demo rebuilt with return lines, and the quickstart in game.

## Risks

- **Every existing power test changes**, since each needs a return line. Mitigation: a helper that builds a loop, and the old numbers asserted unchanged.
- **Blockstate count** for coloured Wicks, about 100,000. Mitigation: multipart models, and measuring the load time.
- **Hera's bases stop on update** (the user's strict choice). The release note says how to fix them.
- **The Hookahs' front is flipped** compared with the other blocks. Mitigation: one `front(state)` per block, used by both the maths and the marks, plus a game test per block type that its marked + face is the + terminal.
