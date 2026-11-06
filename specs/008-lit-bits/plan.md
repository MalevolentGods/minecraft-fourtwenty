# Implementation Plan: Lit Bits and the Weedweeno

**Branch**: `008-lit-bits` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md) | **Research**: [research.md](research.md) | **Data model**: [data-model.md](data-model.md) | **Lua API**: [contracts/lua-api.md](contracts/lua-api.md) | **Quickstart**: [quickstart.md](quickstart.md)

## Summary

Four new things:
- **Lit Bit:** an RGB LED lamp with + and − terminals. It's a new LED element in the circuit solver, and it's drawn by a block-entity renderer.
- **Sticky Finger:** a glove that places Lit Bits and numbers them.
- **Weedweeno:** a powered controller that runs sandboxed Lua shows (Cobalt, bundled), with an editor screen, presets and unlimited saved programs.
- **Bit Chit:** an item that carries one program.

Colours reach lamps in range wirelessly, by ID.

## Technical Context

- **Language/Version:** Java 21, NeoForge 21.1.218, Minecraft 1.21.1.
- **New dependency:** Cobalt 0.9.9 (`cc.tweaked:cobalt`, MIT), bundled with jarJar. It needs the squiddev Maven repository, updated dependency locks and verification metadata.
- **Builds on:** spec 007 (terminals, loops, `Circuit`, `Network`, `Terminal`).
- **Storage:**
  - **Block entities:** Lit Bit (ID, colour, light level); Weedweeno (programs, status).
  - **Per-level SavedData:** remembered IDs.
  - **Item components:** `next_id`, `program`, `weedweeno_programs`.
- **Testing:**
  - **JUnit:** `LedRules`, the LED element in `Circuit`, ID rules, program limits, the HSV conversion, and the Lua sandbox (no `io`, `os`, `debug` or `dofile`; an infinite loop interrupted; string guards).
  - **Game tests:** a lamp's current against colour and voltage, reversed, IDs re-used at a position, the Sticky Finger counting, a Weedweeno setting lamps by ID within range and not beyond, stop turning lamps off, a Bit Chit round trip, a restart resuming the show.
- **Performance:** 1,024 lamps at 20 frames a second with the server at 20 TPS. The Lua budget is 2 ms per Weedweeno per tick and 10 ms in all.
- **Registry change:** four blocks or items, components, payloads and a menu, so the client and server need the same jar.

## Constitution Check

- **I. MineColonies optional:** untouched.
- **II. Terpinator and Infuser:** untouched.
- **III. The power rule:** Lit Bits and the Weedweeno are circuit loads, but not `MachinePower` machines. A lamp shines from what its terminals carry. It doesn't run free near a high player, because light from nothing would break the circuit's books.

  This is a deliberate exception to "every powered machine uses one rule", recorded here: a lamp isn't a machine with a job, and its brightness is its current. The Weedweeno does follow the rule, so it runs free near a high player.
- **IV. Failure is safe:** lamps overload and never break, a reversed lamp is dark, and only wire burns.
- **V. Approved names:** Lit Bit, Sticky Finger, Weedweeno and Bit Chit (user, 2026-11-05).
- **VI. Pure and tested:** LED law, ID rules and program limits in `domain/` with JUnit tests; world behaviour in game tests; every number in config.
- **VII. Proven in game:** the quickstart covers it.
- **Payloads:** all five are guarded with `hasChannel`.
- **Bundled:** Cobalt ships in the jar, so nothing has to be installed separately.

Gates pass, with the power-rule exception for lamps recorded above.

## Project Structure

```text
src/main/java/com/malevolentgods/fourtwenty/
├── domain/LedRules.java, LitBitIdRules.java, Programs.java, Circuit.java (+ LED element)
├── lights/
│   ├── LitBitBlock.java, LitBitBlockEntity.java      the lamp, its node, colour and ID
│   ├── LitBitIds.java                                   per-level IDs (SavedData) and the live index
│   ├── StickyFingerItem.java                            places and numbers lamps
│   ├── WeedweenoBlock.java, WeedweenoBlockEntity.java   programs, status, the Bit Chit slot
│   ├── WeedweenoMenu.java
│   ├── LuaShow.java                                     Cobalt state, the bits library, frames, the watchdog
│   ├── BitChitItem.java, BitChitCopyRecipe.java
│   ├── LightsNetwork.java                               payloads (LampColours, WeedweenoState, WeedweenoEdit, SetLitBitId, SetFingerId)
│   └── ModLights.java                                   registrations
├── power/Network.java, BINode.java                      LED role
└── client/LitBitRenderer.java, WeedweenoScreen.java, LitBitScreen.java, StickyFingerScreen.java, LampColoursClient.java

src/main/resources/data/fourtwenty/weedweeno/presets/{rainbow,chase,pulse,strobe}.lua
tools/generate_lit_bits.py                              lamp, glove, Weedweeno and Bit Chit art
```

## Build Order

1. **Bundling:** Cobalt with jarJar, repository, locks and verification. Prove it loads in a dev run and in the built jar.
2. **Pure rules:** `LedRules`, the LED element in `Circuit`, `LitBitIdRules`, `Programs`, with tests.
3. **Lit Bit:** the block, block entity, LED node in `Network`, the renderer, colour payloads and light level. Game tests for current and reversal.
4. **IDs:** `LitBitIds`, placing (remembered, glove, by hand), the Sticky Finger, and the lamp and glove screens.
5. **Shows:** `LuaShow` (sandbox, `bits` library, frames, watchdog, string guards, size sampling), with sandbox JUnit tests.
6. **Weedweeno:** the block, programs and presets, restart resume, the editor screen and its payloads.
7. **Bit Chit:** export, import and the copy recipe.
8. **Art, recipes, lang, README, CHANGELOG;** the timing test with 1,024 lamps; the quickstart in game on flatworld.

## Risks

- **Bundling** a non-mod library with NeoGradle 7's jarJar, under strict dependency verification. If jarJar fights the build, the fallback is shading Cobalt's classes, relocated, into the jar.
- **Server safety:** Lua is untrusted code. The mitigations are the sandbox, interrupts, the string guards, the size sampling, a global cap and an operators-only switch. Memory can't be fully capped without allocation accounting, so that's noted for server owners.
- **Client cost** of 1,024 block-entity renderers. Mitigation: a culled, simple cube per lamp, drawn only within render distance.
- **Light engine churn** from strobes. Mitigation: a lamp's light level changes at most every 10 ticks.
