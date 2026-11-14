# Research: Midnight Machine

## R1. Shipping the music

**Measured:** "Awake and Adapt" (1:36) as mono Vorbis q4 at 44.1 kHz is 0.95 MB, so the whole album (26.4 min) is about **15.6 MB**.

**Decision:**
- **Conversion:** `tools/convert_album.py` converts each WAV with ffmpeg (`-ac 1 -c:a libvorbis -q:a 4`) to `assets/fourtwenty/sounds/midnight_machine/trackN.ogg`.
- **Manifest:** it writes `album.json` with titles and exact lengths.
- **WAVs:** the source files stay in the user's folder and are never committed.

**Why mono:** Minecraft only gives a sound a position, and fades it with distance, if the sound is mono.

**Alternatives:** stereo (twice the size, and no position); q2 (smaller, audibly worse on music).

## R2. Playing from her, starting mid-track

**Finding:**
- **The hook:** vanilla 1.21.1's `SoundInstance` has `default CompletableFuture<AudioStream> getStream(SoundBufferLibrary, Sound, boolean)`, so a sound instance can supply its own stream.
- **Vanilla's decoder can't jump ahead:** its streams start at the beginning and can't seek.
- **One that can:** Minecraft bundles LWJGL `stb`, which includes `STBVorbis`, and that can seek.

**Decision:**
- **`AlbumSoundInstance`** extends `AbstractTickableSoundInstance`:
  - it follows the Builder each tick;
  - it plays on `SoundSource.RECORDS` (the Jukebox/Note Blocks slider);
  - attenuation is `LINEAR`, with `attenuation_distance` 24 in `sounds.json`, and the event is marked `stream: true`;
  - it overrides `getStream` to return a `SeekingOggStream`.
- **`SeekingOggStream`** loads the OGG resource into a direct buffer, opens it with `stb_vorbis_open_memory`, seeks with `stb_vorbis_seek(frame)`, and fills `read(size)` with `stb_vorbis_get_samples_short_interleaved`.
- **When she's gone** (out of range, removed, or stopped), the instance stops. A new one starts at the current position when needed.

**Alternatives:**
- **Splitting tracks into 10-second pieces:** gaps between pieces, and messy.
- **Restarting tracks from the top:** breaks resuming (FR-004/005).

## R3. Keeping everyone in time

**Decision:**
- **The server owns the clock** (`AlbumClock`): each Builder has a track and a position in ms.
  - **When it runs:** while she's Lifted, flying, and the music is on, so she hears the music.
  - **Track changes:** when a track ends, it moves to the next one using the lengths in `album.json`, looping after 7.
  - **Saved:** with the Lifted data.
- **The payload** `fourtwenty:midnight_machine` carries entity, playing, track, positionMs and the server's game time.
  - **When it's sent:** at start, stop and track change, to players who start tracking her, and every 30 s as a drift check.
  - **On the client:** position + (client game time − sent time) × 50 ms gives where to start. More than 2 s off restarts the sound at the right place.

## R4. Beat maps

**Decision:**
- **The tool:** `tools/beatmap_album.py`, built on numpy and scipy (librosa doesn't install on Python 3.14). It runs a standard pipeline:
  1. onset strength from spectral flux (STFT, mel-ish bands, log, positive difference);
  2. tempo from the autocorrelation of the onset envelope, weighted toward 90-140 BPM;
  3. beat tracking by dynamic programming (Ellis 2007);
  4. accents: beats whose onset strength is in the top ~12% of the track.
- **The output** in `album.json` per track: `bpm`, `beats_ms[]`, `accents_ms[]`, `length_ms`.
- **Checking:** for each track it writes `out/clicks/trackN_clicks.wav` (the song with clicks on the beats), so the beats can be checked by ear.
- **Overrides:** a track can take a manual `bpm`/`offset_ms` override in a small overrides file.

## R5. Dancing

**Decision:**
- **The rules:** `DanceRules.pose(beatMap, timeMs, moving)` is pure and returns:
  - **bob:** a 0-1 envelope peaking on each beat (quick rise, softer fall), scaled by the config;
  - **spin:** a 360° turn over 0.4 s, starting on each accent (at most one spin per 2 s);
  - **sway:** a small side-to-side tilt over two beats;
  - **arms:** raised and pumping on the beat while waiting, smaller while flying.
- **Body:** `RenderLivingEvent.Pre` for a playing Builder applies bob (translate), spin and sway (rotate) to the pose stack.
- **Arms:** a client mixin on `CitizenModel.setupAnim` (TAIL) adds the arm and head angles.
- **Effect on the game:** none. It changes only her drawn model, so where she is, her bounding box and her AI are untouched (FR-010).
- **Without music** (switched off): 100 BPM, generated, no accents.

## R6. The boombox

**Decision:**
- **Concepts:** `tools/render_midnight_machine_concepts.py` runs Blender in the background. It renders 3-4 designs built only from cuboids (so each can become a Minecraft JSON model), each from 4 angles, published as an artifact for the user to pick from.
- **Look:** retro boombox and synth-panel themes in the album's palette, with straps, cassette door, twin cones, glowing trim and the album art panel.
- **The model:** after the pick, `tools/generate_midnight_machine_model.py` writes two standalone JSON models: the body, and the cones on their own so they can scale. They're registered through `ModelEvent.RegisterAdditional`, so there's no item and no registry change.
- **Textures:** for the shell and the glow, plus the album art panel cut from `MM.png`/`Art.png` and scaled down (32×32) for the front.
- **Drawing it:** `BoomboxLayer` (a `RenderLayer` on the citizen renderer) draws the body on her back, following the body part's pose. Glow parts are drawn full-bright, and the cones are scaled by the beat envelope.
