# Tasks: Midnight Machine

**Input**: `specs/014-midnight-machine/` (plan, spec, research, data-model, contracts/assets-and-sync.md, quickstart)

**Builds on:** spec 013 (Lifted, `LiftedService`, `LiftedFlight`, `LiftedClient`, `BuilderConfig`).

**Paths**:
- `J` = `src/main/java/com/malevolentgods/fourtwenty`
- `MC` = `J/compat/minecolonies`
- `A` = `src/main/resources/assets/fourtwenty`
- `T` = `src/test/java/com/malevolentgods/fourtwenty/domain`
- `CT` = `src/colonytest/java/com/malevolentgods/fourtwenty/colonytest`
- `ALBUM` = `C:\Users\Ignus\OneDrive\Desktop\ATLemental - Midnight Machine` (source WAVs and art; never committed)

**Tests:** the constitution requires JUnit tests for `domain/` rules and game tests for world behaviour, so they're included.

## Phase 1: Setup

- [X] T001 Write `tools/convert_album.py`:
  - **Conversion:** for each `ALBUM/N - Title.wav`, run `ffmpeg -ac 1 -ar 44100 -c:a libvorbis -q:a 4` to `A/sounds/midnight_machine/trackN.ogg`.
  - **Manifest:** write `A/midnight_machine/album.json` with `title`, `artist` and `tracks[{id, title, length_ms}]` (length from the WAV header).
  - **Check:** print sizes, and fail if the total is over 20 MB.
- [X] T002 Run `tools/convert_album.py`; commit the 7 OGGs and `album.json`; add the 7 events to `A/sounds.json` as in contracts/assets-and-sync.md (`category: record`, `stream: true`, `attenuation_distance: 24`).
- [ ] T003 [P] Add a `[music]` section to `MC/builder/BuilderConfig.java`: `enabled` (true), `fallbackBpm` (100, 40-200), `danceBob` (0.12, 0-0.5) and `spinCooldownSeconds` (2, 0.5-30), with typed accessors.

## Phase 2: Foundational

- [ ] T004 [P] Write `T/AlbumClockTest.java`:
  - advancing within a track;
  - rolling over into the next track, including across several short ones in one step;
  - looping after track 7;
  - pausing keeps the position;
  - a bad saved track or position is clamped back to a valid one.
- [ ] T005 Create `J/domain/AlbumClock.java` (pure):
  - `record Position(int track, long ms)`;
  - `Position advance(Position, long ms, long[] lengths)`;
  - `Position start()`;
  - `Position clamp(Position, long[] lengths)`.

  Make T004 pass.
- [ ] T006 Create `MC/builder/Album.java`: load `album.json` from the mod's resources on both sides (`ResourceManager`, or a classpath read) and expose `lengths()`, `title(i)` and `beatMap(i)` (`bpm`, `beats_ms`, `accents_ms`; empty until the beat maps exist).
- [ ] T007 Save the album position with Lifted: add `AlbumClock.Position` to `MC/builder/Lifted.java`, in a separate map `positions` keyed like the buffs, with save/load. Old saves have none, so they start at track 0.

**Checkpoint:** builds; the `AlbumClock` tests pass.

---

## Phase 3: User Story 1, the music (P1) 🎯 MVP

**Goal:** the album plays from a Lifted Builder, positional, on the Records slider, resuming and in sync (FR-001 to FR-006).
**Test:** quickstart M1.

- [ ] T008 [US1] Create `MC/builder/MidnightMachinePayload.java` (`fourtwenty:midnight_machine`: entity, playing, lifted, track, positionMs, sentAt) with `register(bus)` and `send(entity, ...)` to trackers that have the channel. Register it in `MC/ColonyIntegration.java`.
- [ ] T009 [US1] Create `MC/builder/MidnightMachineService.java`:
  - **Each sweep:** call it from `LiftedService.tick` (every 20 ticks). Playing = Lifted, flying (`LiftedFlight.inFlight`) and `[music] enabled`.
  - **While playing:** advance her `AlbumClock` position by the elapsed ms, and save it.
  - **Sending:** send the payload on start, stop, track change and every 30 s, and from `startTracking` too (as `LiftedPayload` does).
- [ ] T010 [US1] Create `MC/client/SeekingOggStream.java`, an `AudioStream` over LWJGL `STBVorbis`:
  - **Opening:** read the resource into a direct buffer, call `stb_vorbis_open_memory`, then `stb_vorbis_seek` to the start sample.
  - **Format:** `getFormat()` is 44.1 kHz, 16-bit, mono.
  - **Reading:** `read(size)` fills from `stb_vorbis_get_samples_short_interleaved`.
  - **Closing:** `close()` frees the decoder and buffer.
- [ ] T011 [US1] Create `MC/client/AlbumSoundInstance.java`, extending `AbstractTickableSoundInstance`:
  - **Sound:** the track's event, on `SoundSource.RECORDS`, with `Attenuation.LINEAR`.
  - **Movement:** follows the entity every tick, and stops when she's removed.
  - **Stream:** `getStream` returns `SeekingOggStream` at the start ms.
- [ ] T012 [US1] Create `MC/client/MidnightMachineClient.java`:
  - **Payload handling:** start, stop or restart her sound at position + (now − sentAt) × 50 ms.
  - **Clock:** keep a beat clock per Builder (track, start game-ms).
  - **Track end:** start the next track locally.
  - **Drift:** restart if more than 2 s off.

  Install it from `client/ModClientEvents.java` when MineColonies is loaded.
- [ ] T013 [P] [US1] Write `CT/AlbumPositionGameTests.java`: positions survive a `Lifted` save and load, kept apart by colony and citizen, and an old save without positions loads.
- [ ] T014 [US1] **In game (M1):** quickstart M1 steps 1-6 on flatworld, and record the results.

**Checkpoint:** the music works in game. Commit.

---

## Phase 4: User Story 2, the boombox (P1)

**Goal:** an awesome Midnight Machine on her back, chosen by the user (FR-007, FR-008).
**Test:** quickstart M2.

- [X] T015 [US2] Write `tools/render_midnight_machine_concepts.py` for Blender 5.1 (`"C:\Program Files\Blender Foundation\Blender 5.1\blender.exe" -b -P`):
  - **Designs:** 4 cuboid-only concepts in the album's palette, sampled from `MM.png`/`Art.png`. Themes:
    - a retro twin-cone boombox with a carry handle and cassette door;
    - a synth/machine panel with VU meters;
    - a chunky street boombox with a bass port;
    - a slim neon "midnight" deck.
  - **On each:** shoulder straps, the album-art panel and emissive trim.
  - **Renders:** each on a citizen-sized stand-in, from front, back, three-quarter and side.
- [X] T016 [US2] (Published 2026-11-14: https://claude.ai/artifact/KvisK9PFzG5gYpBD9SES3j. **The user picked B, Synth Machine**, with VU bars driven by the music.) Run it, publish the renders as one artifact page for the user, and **stop for the user's pick** (gate).
- [ ] T017 [US2] Write `tools/generate_midnight_machine_model.py` for the chosen design:
  - **Models:** `A/models/midnight_machine/body.json` and `bar.json` (one VU bar, scaled per band) (cuboid elements, at most 16 px per axis per element).
  - **Textures:** in `A/textures/entity/midnight_machine/`: the shell, the glow, and a 32×32 album-art panel cut from `ALBUM/MM.png`, quantised to the palette.
  - **Check:** run `tools/check_model_overlaps.py` on the output.
- [ ] T018 [US2] Create `MC/client/BoomboxLayer.java`, a `RenderLayer` on the citizen renderer, added in `LiftedClient.addLayers`:
  - **Placement:** while she's Lifted (from the payload's `lifted`), draw `body` on her back by following `model.body`'s pose.
  - **Glow:** full-bright.
  - **VU bars:** draw the six VU bars with heights from `trackN.bands` at the player's playback position (interpolated between frames). With no music playing, follow the dance beat instead.
  - **Model loading:** register the models with `ModelEvent.RegisterAdditional`.
- [ ] T019 [US2] **In game (M2):** quickstart M2, and the user approves the look (SC-005).

**Checkpoint:** the boombox is approved. Commit.

---

## Phase 5: User Story 3, dancing (P2)

**Goal:** beat-synced dancing that never changes her building (FR-009 to FR-011).
**Test:** quickstart M3.

- [X] T020 [US3] Write `tools/beatmap_album.py` (numpy, scipy), following research R4:
  - **Analysis:** onset strength, then tempo, then dynamic-programming beats, then accents (top 12%).
  - **Output:** write `bpm`, `beats_ms` and `accents_ms` into `album.json`.
  - **VU bands:** also measure six frequency bands (bass to treble, log-spaced, roughly 40-12,000 Hz) every 50 ms, normalised per track, and write them as `A/midnight_machine/trackN.bands` (6 bytes a frame, 0-255).
  - **Clicks:** write `out/clicks/trackN_clicks.wav` (gitignored), and print the tempo and beat count per track.
  - **Overrides:** honour `tools/beatmap_overrides.json` (`bpm`, `offset_ms` per track).
- [ ] T021 [US3] (Ran 2026-11-14: 95.7, 90.7, 172.3, 178.2, 166.7, 166.7 and 95.7 BPM; beat spacing tight (9-42 ms spread). Tracks 3-6 may be double-time, so dancing bobs every other beat above 140 BPM. Click tracks are in %TEMP%\mm_clicks for the user to spot-check.) Run it. Listen to each click track (the user can spot-check), fix any track with an override, and commit `album.json`.
- [ ] T022 [P] [US3] Write `T/DanceRulesTest.java`:
  - the bob peaks on each beat (±1 frame);
  - a spin starts on an accent and lasts 0.4 s, with no second spin within the cooldown;
  - the sway period is two beats;
  - waiting gives bigger arms than moving;
  - an empty beat map follows the fallback BPM.
- [ ] T023 [US3] Create `J/domain/DanceRules.java` (pure): `Pose pose(BeatMap, long ms, boolean moving, Tuning)` gives `bob`, `spinDeg`, `swayDeg`, `armPitch`, `armRoll`, `headNod`, and `envelope` (for the cones). Make T022 pass.
- [ ] T024 [US3] Create `MC/client/DanceAnimation.java`: on `RenderLivingEvent.Pre`, for a dancing Builder, translate by `bob` and rotate by `spinDeg` and `swayDeg` around her centre, then pop in `Post`.
- [ ] T025 [US3] Create `MC/mixin/CitizenDanceMixin.java` (client), injecting at TAIL of MineColonies `CitizenModel.setupAnim`: add `armPitch`, `armRoll` and `headNod` for dancing Builders. Register it in the `client` list of `fourtwenty.minecolonies.mixins.json`.
- [ ] T026 [US3] **In game (M3):** quickstart M3, including the timing clip (SC-003) and build timing with dancing on and off (SC-004).

**Checkpoint:** she dances in time. Commit.

---

## Phase 6: Polish

- [ ] T027 [P] CHANGELOG `[Unreleased]`: the Midnight Machine. README "What's new" at the release bump.
- [ ] T028 Without MineColonies, the standalone game tests pass (SC-007). Check the jar grew by 20 MB or less (SC-006).
- [ ] T029 Update memory (`project_fourtwenty_lifted_builders.md`, or a new `project_fourtwenty_midnight_machine.md`), and mark every task [X].
- [ ] T030 PR(s) to `main` for spec 013 and spec 014, on the user's go. Release together.

## Dependencies

- **Order:** Setup → Foundational → US1 (music) → US3 (dancing needs the client beat clock from T012).
- **US2 (boombox):** T015-T016 can start straight away (renders only). T018 needs T012 for the beat envelope.
- **The gate:** T016 must have the user's pick before T017.

## Parallel

- T003 alongside T001-T002.
- T004 alongside T006.
- **T015-T016 alongside all of US1** (the long wait on the user).
- T020-T021 alongside US1.
- T022 alongside T024-T025.

## Strategy

- **The wait on the user:** start the boombox concepts first. While the user looks, build the music (US1) and the beat maps.
- **Then:** the boombox model, then the dancing.
