# Implementation Plan: Extracts, Breeding and the Hybrid Ladder

**Branch**: `003-extracts-and-breeding` | **Spec**: [spec.md](spec.md)

## Technical Context

Java 21, NeoForge 21.1.218, Minecraft 1.21.1. JUnit in `src/test`, game tests in `src/gametest`. Art from `tools/generate_*.py`.

## Constitution Check

- Nothing needs MineColonies: yes.
- Terpinator and Infuser recipes unchanged: yes; hybrids only add colours a smoke can inspire.
- Terpinator stays the only terpene source: yes.
- Machines on the shared rule: yes, through `MachineBlockEntity`.
- Flying can't kill you: the Golden God lands softly (T030).
- Pure rules in `domain/`, numbers in config: yes.

## Key decisions

- **Strain** stays the enum: the crop keeps its strain in block state, so harvest, replanting and colony mods keep working. 22 more values give 39, and block states save by name so old saves stay valid. New values go on the end, because item overrides use ordinals.
- **Traits don't fit in block state** (125 combinations across 39 strains and 8 ages), so crops gain a block entity holding only traits. No entity reads as rating 2, which covers old crops and anything that places a crop without one.
- **Traits** are one data component, absent meaning 2s, so existing stacks stay valid and rated 2 stacks merge with them.
- **Crosses** are a pure table in `domain/Crosses`, keyed on the unordered pair.
- **Golden God** visuals are a client render layer driven by a payload to tracking players, guarded by `hasChannel`.
- **Flight** uses `abilities.mayfly`, restored only if the player didn't already have it, then Slow Falling until they land.

## Build order

1. Strains and traits, no new machines: enum, grades, components, smoking, yield, Jade, seed gating. Existing behaviour pinned by tests.
2. Crosses and Pheno Chambers, with art for the ladder.
3. Biome strains, climates, Cosmic Kush Pod climate.
4. Golden God.
5. Rosin Rig, extracts, Hookah burn lengths.
6. Construction Joint.
7. Docs, then the plan repo.
