# Implementation Plan: Midnight Machine

**Branch**: `014-midnight-machine` | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/014-midnight-machine/spec.md` (builds on spec 013, branch `013-lifted-builders`)

## Summary

- **Assets:** offline Python tools (`tools/`) convert the album to mono OGG and work out each track's beat map. Both ship in `assets/fourtwenty/`, about 15.6 MB.
- **Server:** keeps each Lifted Builder's place in the album and tells clients when and where to play.
- **Client:**
  - **Music:** plays from her position on the Records slider, starting mid-track through a seeking audio stream (vanilla `SoundInstance.getStream`, decoded with the LWJGL `STBVorbis` Minecraft already bundles).
  - **Dancing:** driven by pure dance rules from the beat map and the playback clock (body bob, spin, sway, arm moves), and only on her drawn model.
  - **Boombox:** drawn on her back as a separate baked model (no new item or block): album art on the front, emissive glow parts, speaker cones that scale with the beat.
- **Design first:** the boombox design starts as Blender concept renders for the user to choose from.

## Technical Context

**Language/Version**: Java 21 (Minecraft 1.21.1, NeoForge 21.1.218). Python 3.14, ffmpeg 8.1 and Blender 5.1 for the offline tools.

**Primary Dependencies**:
- MineColonies 1.1.1343 (optional, as spec 013).
- LWJGL `stb` (bundled with Minecraft) for decoding OGG with seeking.
- numpy and scipy for the beat tool. These are offline only and never ship.

**Storage**:
- **Album position:** each Builder's place in the album, saved alongside Lifted (`LiftedSavedData`).
- **Assets:**
  - OGG files and beat maps in `assets/fourtwenty/sounds/midnight_machine/` and `assets/fourtwenty/midnight_machine/`;
  - the boombox model and textures in `assets/fourtwenty/models/midnight_machine/` and `textures/entity/midnight_machine/`.

**Testing**:
- **Unit tests:** JUnit for the pure rules: `AlbumClock` (position, track changes, looping, resuming) and `DanceRules` (pose from beat map and time).
- **Beat-map check:** the beat tool prints each track's tempo and beat count, and writes a click track to listen to.
- **Game tests:** the album position survives a save and reload.
- **In game:** sync, range, slider and dance timing are checked in game (Principle VII). Audio can't be game-tested.

**Target Platform**: NeoForge client and server (same jar).
**Project Type**: Minecraft mod (MineColonies add-on part).

**Performance Goals**:
- **Server:** one small payload per start, stop or track change per Builder, and nothing per tick.
- **Client:** decoding happens on Minecraft's sound thread, and dance poses cost a few arithmetic operations per frame.

**Constraints**:
- **Mono:** required for positional sound.
- **Jar size:** under 20 MB added (SC-006).
- **Never in the jar:** the source WAVs stay outside the repo; only the converted OGGs go in.
- **Optional MineColonies:** nothing may load without it (Principle I).

**Scale/Scope**: a handful of Lifted Builders per colony at once.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How |
|---|---|---|
| I. MineColonies Stays Optional | ✅ | Everything sits under `compat/minecolonies` and the MineColonies-only client setup in `ModClientEvents`. The sound assets do nothing without it. |
| II, III, IV | ✅ | No machines, no power, no flight changes. |
| V. Approved Names | ✅ | "Midnight Machine" approved 2026-11-14. Track titles are the user's own. |
| VI. Rules Are Pure And Tested | ✅ | `AlbumClock` and `DanceRules` are pure, with JUnit tests. The position save is game-tested. Tunables (server switch, fallback tempo, dance sizes) are in the builder config; the ~24-block range is set in `sounds.json` (attenuation distance), with a config volume. |
| VII. Milestones Prove Themselves In Game | ✅ | Milestones below, each tested on flatworld. |
| Constraints: generators | ✅ | `tools/convert_album.py`, `tools/beatmap_album.py`, `tools/render_midnight_machine_concepts.py` and `tools/generate_midnight_machine_model.py`. |
| Constraints: payloads guarded, same jar | ✅ | One new payload, `fourtwenty:midnight_machine`, sent only where the channel exists. No registry changes (the boombox is a standalone model). |

No violations.

## Project Structure

```text
specs/014-midnight-machine/  plan.md, research.md, data-model.md, quickstart.md, contracts/assets-and-sync.md, tasks.md

tools/
├── convert_album.py                     # WAV → mono OGG (ffmpeg, libvorbis q4) + manifest of track lengths
├── beatmap_album.py                     # numpy/scipy beat tracker → beat maps + click tracks to check by ear
├── render_midnight_machine_concepts.py  # Blender: 3-4 boombox concepts, rendered from 4 angles
└── generate_midnight_machine_model.py   # the chosen design → JSON models + textures (album art panel)

src/main/resources/assets/fourtwenty/
├── sounds.json                                   # + midnight_machine.track1..7 (stream, attenuation_distance 24)
├── sounds/midnight_machine/track1..7.ogg         # ~15.6 MB
├── midnight_machine/album.json                   # titles, lengths, beat maps (beats_ms, accents_ms, bpm)
├── models/midnight_machine/{body,cones}.json     # baked standalone models
└── textures/entity/midnight_machine/*.png        # body, glow, album art panel

src/main/java/com/malevolentgods/fourtwenty/
├── domain/AlbumClock.java          # where a Builder is in the album; advance, pause, resume, loop
├── domain/DanceRules.java          # pose (bob, spin, sway, arms) from beat map + playback time
├── compat/minecolonies/builder/
│   ├── Album.java                  # loads album.json on the server (track lengths)
│   ├── MidnightMachineService.java # per-Builder play state from Lifted + flying; saves position; sends the payload
│   └── MidnightMachinePayload.java # entity, playing, track, positionMs, sentAtGameTime
└── compat/minecolonies/client/
    ├── MidnightMachineClient.java  # payload → sound instances, beat clock per Builder
    ├── AlbumSoundInstance.java     # follows her, Records source, LINEAR attenuation, getStream → seeking stream
    ├── SeekingOggStream.java       # AudioStream over STBVorbis, starts at a position
    ├── BoomboxLayer.java           # RenderLayer: boombox on her back, glow, cone pulse
    └── DanceAnimation.java         # RenderLivingEvent.Pre pose + CitizenModel.setupAnim arms (mixin)
src/main/java/.../compat/minecolonies/mixin/CitizenDanceMixin.java   # arm/head moves after MineColonies' setupAnim (client)
src/test/java/.../domain/AlbumClockTest.java, DanceRulesTest.java
src/colonytest/java/.../colonytest/AlbumPositionGameTests.java
```

## Milestones

1. **M1, music:**
   - convert the album and register the sounds;
   - the seeking stream;
   - the server clock and the payload;
   - test range, slider, resuming and players arriving mid-song on flatworld.
2. **M2, boombox:**
   - concept renders and the user's pick, which **gates** the model;
   - the model, glow and album art;
   - the cone pulse.
3. **M3, dancing:**
   - beat maps;
   - `DanceRules`;
   - body and arm animation;
   - check timing in game against what's heard (SC-003), and that build speed is unchanged (SC-004).
4. **M4, docs and the release:** on the user's go, released together with spec 013.

## Complexity Tracking

| Risk | Mitigation |
|---|---|
| A seeking decoder on the sound thread | `SeekingOggStream` decodes from an in-memory copy of the file with `STBVorbis` (the decoder Minecraft already bundles), frees its native memory on close, and is tested by playing every track from the start, the middle and the last second. |
| Drift between players | Each start carries the server's game time. Clients compensate, and the server re-sends position every track change and to new watchers. A client more than 2 s off restarts at the right place. |
| The beat tracker misses on some tracks | It writes a click track for each song so its beats can be checked by ear. A track that's wrong gets a hand-set tempo and offset in `album.json`. |
