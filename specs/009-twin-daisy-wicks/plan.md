# Implementation Plan: Twin Wick and Daisy Wick

**Branch**: `009-twin-daisy-wicks` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md)

## Summary

Two two-core cables that make the connections a player would otherwise lay as red and black rails. Twin Wick puts every block it touches across its two cores; Daisy Wick splices every block it touches into one series string and closes the loop through its return core. The network walk learns about cores: a conductor in the circuit becomes a core (a block position and a sign) instead of a block position. Everything after the walk (the solve, losses, ratings, burnout, glow, readings) works on conductors as before. `domain/Circuit` doesn't change.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: NeoForge 21.1.218 (Minecraft 1.21.1)
**Storage**: block states only (colour, side links, lit); nothing saved beyond them
**Testing**: JUnit 5 for `domain/DaisyLine`; game tests in `src/gametest` next to `CircuitGameTests` and `Ladder`
**Target Platform**: NeoForge dedicated server and client
**Project Type**: Minecraft mod
**Performance Goals**: walking a network with cables costs no more than one with the same number of single Wicks (two conductors per cable block, both collapsing into runs)
**Constraints**: `domain/Circuit` unchanged; every 006 and 007 circuit game test passes unchanged; client and server jars match (new blocks)
**Scale/Scope**: 8 blocks (2 kinds × 4 tiers), 17 colours each, up to `Network.MAX_WICKS` conductors per network

## Constitution Check

- **I. MineColonies optional:** untouched.
- **II. Terpinator and Infuser:** untouched.
- **III. The power rule:** unchanged. Cables carry power; they don't use any.
- **IV. Failure is safe:** a core over its rating burns its block in smoke and ash. A branched Daisy line carries nothing rather than guessing.
- **V. Approved names:** Twin Wick and Daisy Wick (user, 2026-11-05), with the tier names already in use: Hemp, Rolled, Glass, Gold.
- **VI. Pure and tested:** Daisy line ordering, branch detection and splice orientation in `domain/DaisyLine` with JUnit tests; the walk's results in game tests; ratings come from the existing circuit config.
- **VII. Proven in game:** the quickstart builds both cables on flatworld.

Gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/009-twin-daisy-wicks/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── blocks-and-readings.md
└── tasks.md
```

### Source Code

```text
src/main/java/com/malevolentgods/fourtwenty/
├── domain/DaisyLine.java          # NEW: orders a Daisy line, finds branches, gives each block's upstream and downstream
├── power/CableBlock.java          # NEW: Twin and Daisy cable blocks (kind, tier, colour, side links, lit, burn)
├── power/Network.java             # walk: conductors keyed by Conductor(pos, core); cables, single-Wick joins, Daisy splicing
├── power/Terminal.java            # which terminal (or Hit Box winding) a cable on a face connects to
├── power/Circuits.java            # readings for cable blocks
├── power/InspoweedometerItem.java # Twin and Daisy readings and the reasons a Daisy line is dead
├── registry/ModPower.java         # 8 blocks and items, BI_BLOCKS-style tag for cables
└── client/PortModel.java          # fork fittings where a cable plugs into a block
src/test/java/.../domain/DaisyLineTest.java
src/gametest/java/.../gametest/CableGameTests.java
tools/generate_cables.py           # NEW: textures, models, blockstates, item models, recipes, lang
```

## Build Order

1. `domain/DaisyLine` with its tests (no game types).
2. The conductor refactor in `Network.walk`: single Wicks keyed by `Conductor(pos, WIRE)`. All 006/007 game tests must still pass before anything else is added.
3. `CableBlock`, registration, links; Twin Wick in the walk; Twin game tests.
4. Daisy Wick in the walk through `DaisyLine`; Daisy game tests.
5. Single-Wick joins (red to +, black to −; Daisy ends); Hit Box windings by face.
6. Readings and Inspoweedometer messages.
7. Art (`tools/generate_cables.py`), port fittings, recipes, lang, README, CHANGELOG.
8. Full test run; flatworld quickstart.

## Risks

- **The walk refactor touches every circuit.** Mitigation: do it alone first, with no behaviour change, and run the full game-test suite before adding cables.
- **Daisy order in odd layouts** (corners beside blocks, two blocks beside one cable). Mitigation: the rules in research R3 are pure and tested case by case.
- **Blockstate count**: 6 side links × 3 values × 17 colours × lit is the same space a single Wick already uses, per block. Eight more such blocks are acceptable; the side link values are limited to NONE, WIRE and PLUG as for Wicks.
