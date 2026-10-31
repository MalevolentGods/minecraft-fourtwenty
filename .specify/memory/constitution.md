<!--
Sync Impact Report
Version change: template → 1.0.0
Modified principles: all template placeholders replaced
Added sections: Core Principles I-VII, Technical Constraints, Development Workflow, Governance
Removed sections: none
Templates: plan-template.md ✅ (its Constitution Check reads this file), spec-template.md ✅,
tasks-template.md ✅ (no fixed categories conflict)
Follow-up TODOs: none
-->
# Four Twenty Constitution

## Core Principles

### I. MineColonies Stays Optional

Nothing depends on MineColonies. The mod MUST load and be fully playable without it, and every
feature MUST be usable by a player working alone. MineColonies code MUST live under
`compat/minecolonies` and load only when the mod is present. Colonists may take jobs over; they
MUST NOT be the only way to do something.

### II. Existing Machines Keep Their Game

The Terpinator's and the Inspiration Infuser's recipes, outputs and gameplay MUST NOT change. The
only permitted change to them is how they are powered. The Terpinator MUST stay the only source of
terpenes; the Rosin Rig makes extracts only.

### III. The Power Rule

Every powered machine, old and new, MUST use one shared rule: free while any high player is within
21 blocks, otherwise draw Boundless Inspiration, and with neither, pause and keep its progress.
Power MUST NOT be reimplemented per machine.

### IV. Failure Is Safe

Overloads MUST burn out a wick or machine in a cloud of smoke and leave ash; they MUST NOT explode.
Flying MUST NOT kill the wearer: a Puff Plate wearer who sobers up or runs out of BI mid-flight is
lowered gently to the ground. Moon Boots get no such rule. Gear works only while the wearer is high
AND has stored BI.

### V. Approved Names

Names in the approved plan MUST be used exactly. New player-facing names MUST be approved by the
user before they ship.

### VI. Rules Are Pure And Tested

Game rules MUST live in `domain/` as pure classes with JUnit tests. Behaviour in the world MUST have
game tests in `src/gametest`. Tunable numbers MUST be config values, not constants buried in
behaviour.

### VII. Milestones Prove Themselves In Game

Each milestone MUST be tested in game and committed before the next starts. Passing unit tests does
not finish a milestone. Results MUST be reported as they happened, including anything not yet seen
in game.

## Technical Constraints

- Minecraft 1.21.1, NeoForge 21.1.218, Java 21.
- Registry changes require the same jar on client and server.
- Custom payloads MUST be sent only to connections that have the channel.
- Blocks and items that carry weed MUST keep the `fourtwenty:strain` component through every step.
- Custom art comes from generators in `tools/generate_*.py`, so it can be regenerated.
- Compile-only APIs are pinned in `validation/compatibility-lock.json`.

## Development Workflow

- Work happens on branches and reaches `main` through pull requests with passing CI.
- Commits follow Conventional Commits; versions are managed with commitizen (`.cz.toml`).
- Releases bump `gradle.properties` and `.cz.toml`, update README and CHANGELOG, and are tagged
  `v<version>`. CI publishes the jar.
- Live servers are updated only when nobody is online, and only when the user asks.

## Governance

This constitution overrides other practices in this repo for work planned with Spec Kit. Amendments
are made in a pull request that states why, and the user approves them. Versions follow semantic
versioning: MAJOR for a removed or redefined principle, MINOR for a new principle or section, PATCH
for wording. Every plan MUST pass its Constitution Check, and any accepted violation MUST be recorded
with its reason in that plan.

**Version**: 1.0.0 | **Ratified**: 2026-10-31 | **Last Amended**: 2026-10-31
