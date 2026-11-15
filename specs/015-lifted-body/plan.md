# Implementation Plan: Lifted Body

**Branch**: `015-lifted-body` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/015-lifted-body/spec.md`

## Summary

While a Builder is Lifted, Four Twenty owns her body. Two independent halves:

1. **Movement (server):** every walk MineColonies asks of her becomes a flight. Its navigator takes every walk as a destination (`setPathJob`) and judges arrival by distance, so one hook turns any walk request (job, hut, racks, restaurant, bed, hospital) into a flight to the same place, with the navigator reporting "still going" until she's there. The flight planner learns **doors and fence gates**: it routes through them, opens them as she reaches them and closes them once she's past. Building on spec 013's flight, legs, stall replans, landing and recall.
2. **Looks (client):** a new stylised **Lifted model** (more joints: elbows, knees, spine, neck), drawn by our own render pass while MineColonies' renderer still draws her name and status. Its painted design (one texture sheet, made like the boombox's) is **tinted per Builder** from her MineColonies skin. Poses blend between a **laid-back cruise** in flight, upright hovering to build, smoking, and spec 014's dance. The boombox, glow, smoke and VU bars move onto the new body.

Switches: the server turns each half off separately; each player turns the look off in the pause menu. If the look can't be drawn, she falls back to 0.26.0's look.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: NeoForge 21.1.218 for Minecraft 1.21.1; MineColonies 1.1.1343 (optional, `compat/minecolonies` only); Mixin
**Storage**: none new (flight state stays in memory, as now; settings in config files)
**Testing**: JUnit for pure rules (`domain/`), game tests (`src/colonytest` for MineColonies), in-game checks on flatworld
**Target Platform**: NeoForge client and dedicated server (Hera, flatworld), Windows client
**Project Type**: Minecraft mod (single project)
**Performance Goals**: with 3 Lifted Builders, tick time within 10% of the same colony sober (SC-006); the Lifted model draws in under 0.5 ms a frame per Builder
**Constraints**: nothing visual may change her position, facing, hitbox or building; flying never hurts her; MineColonies stays optional; client and server run the same jar
**Scale/Scope**: a handful of Builders per colony; routes within the existing flight search limits (legs of ~20 blocks, 4000 cells)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|---|---|---|
| I. MineColonies stays optional | All code under `compat/minecolonies`, mixins in the colony mixin config (loaded only with MineColonies); the standalone game tests run without it (SC-007). | Pass |
| II. Existing machines keep their game | No machine touched. | Pass |
| III. The power rule | Not involved. | Pass |
| IV. Failure is safe | Flying never hurts her (landing and fall distance as now); a broken render falls back to MineColonies' look (FR-012); a failed route recalls her rather than strands her (FR-002). | Pass |
| V. Approved names | New player-facing words: the pause-menu toggle label and its tooltip. Proposed: **"Lifted look"** (on/off), for the user to approve. | Pass (pending approval) |
| VI. Rules are pure and tested | Pure, unit-tested: door routing in the planner, the trip/arrival rules, skin colour sampling, the pose blender, the tint rules. | Pass |
| VII. Milestones prove themselves in game | Each story ends with a flatworld check in quickstart.md before the next. | Pass |

Post-design re-check: still passes; the design adds no MineColonies dependency outside `compat/minecolonies`, and every new rule has a pure, tested home.

## Project Structure

### Documentation (this feature)

```text
specs/015-lifted-body/
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1: the in-game checks
├── contracts/
│   └── config-and-ui.md # settings, the toggle, the art sheet
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
src/main/java/com/malevolentgods/fourtwenty/
├── domain/
│   ├── FlightPlanner.java          # + doors and gates as passable cells (cost), door cells reported on the route
│   ├── Errand.java                 # NEW pure: where a walk request is going, arrival rule, progress/stall rule
│   ├── LiftedPose.java             # NEW pure: pose blending (cruise, hover, smoke, dance) into joint angles
│   └── SkinPalette.java            # NEW pure: hair/skin/cloth colours from a skin's pixels
├── compat/minecolonies/
│   ├── builder/
│   │   ├── LiftedFlight.java       # + errands (any destination), doors on the way, breaks
│   │   ├── Doorways.java           # NEW: open as she reaches, close once past, never anything but doors/gates
│   │   └── BuilderConfig.java      # + bodyFlight, liftedLook switches
│   ├── mixin/
│   │   ├── NavigatorTakeoverMixin.java   # NEW: setPathJob/isDone/tick on the citizen navigator while Lifted
│   │   └── CitizenModelHideMixin.java    # NEW (client): hide MineColonies' body and held items while our model draws
│   └── client/
│       ├── LiftedModel.java        # NEW: the stylised model (ModelPart tree, more joints)
│       ├── LiftedRenderer.java     # NEW: draws the model, props, boombox, glow; fallback on failure
│       ├── LiftedSkins.java        # NEW: per-Builder tinted textures from the painted sheet + SkinPalette
│       ├── BoomboxLayer.java       # → attaches to the new body (keeps its VU bars)
│       └── MidnightMachineSettings.java  # + the "Lifted look" toggle
tools/
├── render_lifted_concepts.py       # NEW: Blender concept renders of 3 silhouettes for the user to pick
└── import_lifted_sheet.py          # NEW: the painted sheet → base texture + tint masks
src/test/java/.../domain/           # FlightPlannerDoorsTest, ErrandTest, LiftedPoseTest, SkinPaletteTest
src/colonytest/java/...             # NavigatorTakeoverGameTests, DoorwaysGameTests
```

**Structure Decision**: the existing single mod project; new code in the established places (`domain/` pure rules, `compat/minecolonies/` for anything MineColonies, `tools/` generators).

## Phases

1. **P1 movement (Stories 1-2):** errands via the navigator hook, doors in the planner and on the way, breaks, the 30 s recall. In game: a Warehouse upgrade with no rescues (SC-001/002/003).
2. **The design pass (Story 3, user):** concept renders of three silhouettes → the user picks → a texture-sheet prompt → the user's one generation → import.
3. **P2 looks (Story 3):** the model, renderer, tints, poses, props, boombox on the new body, the toggle, fallback. In game: SC-004/005.
4. **Polish:** switches, performance check (SC-006), standalone tests (SC-007), CHANGELOG, CURSEFORGE.md, README at release.

## Complexity Tracking

| Choice | Why needed | Simpler alternative rejected because |
|---|---|---|
| Hooking MineColonies' navigator (`setPathJob`) | One place that sees every walk, so every trip (job, breaks) flies without hooking each AI state | Hooking each state (unload, fetch, eat, sleep) separately: many fragile mixins, and new MineColonies states would slip through |
| A second render pass while hiding MineColonies' model | Her name, status bubble and interaction icons keep coming from MineColonies' renderer | Cancelling MineColonies' render: we'd have to redraw the name and status icons ourselves and track their changes |
| Per-Builder tinted textures generated on the client | Each Builder stays recognisable (clarification Q3) from one painted design | One shared look (rejected by the user); per-skin hand art (unbounded art work) |
