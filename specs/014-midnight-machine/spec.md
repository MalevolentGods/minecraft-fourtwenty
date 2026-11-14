# Feature Specification: Midnight Machine

**Feature Branch**: `014-midnight-machine`
**Created**: 2026-11-14
**Status**: Draft
**Input**: "While the builder is high, we play my album (original music) coming from him. We could give him a backpack boombox and make him dance around to the music while he builds: spinning around, bobbing up and down with the beat, being generally stylish and dancy." (user, 2026-11-14)

**Approved name** (user, 2026-11-14): **Midnight Machine**, the boombox backpack, named after the album.

**Builds on:** spec 013, Lifted Builders. Everything here needs MineColonies, which stays optional for Four Twenty.

**Decided before writing** (user, 2026-11-14):
- **Who hears it:** everyone nearby, fading with distance (about 24 blocks), on the game's Jukebox/Note Blocks volume slider, with a server switch to turn it off.
- **The art:** the album art goes on the boombox's front.
- **The look:** a big design effort, with concept renders first for the user to pick from.

## Clarifications

### Session 2026-11-14

- Q: Which boombox concept? → A: **B, Synth Machine** (a tall rack, the album art up top, six VU bars, three knobs, an antenna with a glowing tip), on the condition that the VU bars are really driven by the music. They are: six frequency bands (bass to treble) are measured ahead of time for each track and shipped with it, and each bar shows its band at the moment of the song the player is hearing.

## The album

**ATLemental – Midnight Machine**: the user's own music, 7 tracks, 26.4 minutes in all.

| # | Track | Length |
|---|---|---|
| 1 | Rise and Peak | 4:26 |
| 2 | Smoke and Mirrors | 5:16 |
| 3 | Listen and Learn | 3:27 |
| 4 | Awake and Adapt | 1:36 |
| 5 | Beauty and Light | 4:10 |
| 6 | Far and Away | 3:16 |
| 7 | Man and Machine | 4:12 |

The art: Art.png, MM.png, MM2.png, MM3.png.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The Midnight Machine plays while she's Lifted (Priority: P1)

A Builder who becomes Lifted puts on the Midnight Machine, a boombox backpack, and the album plays from her. Players nearby hear it coming from where she is, louder up close and fading as they walk away. She works through the album in order. When she lands for a break the music pauses, and it picks up where it left off when she's Lifted again.

**Why this priority**: it's the heart of the feature: the user's music, played by their colony.

**Independent Test**: Lift a Builder on the test server. Walk toward her and away, and listen for the music getting louder and fainter. Let her land for a meal and check the music stops, then check it resumes mid-track when she's Lifted again.

**Acceptance Scenarios**:

1. **Given** a Builder becomes Lifted, **When** players are within about 24 blocks, **Then** they hear the album from her position, fading with distance.
2. **Given** the music is playing, **When** a track ends, **Then** the next one starts, and after track 7 it goes back to track 1.
3. **Given** she lands (meal, bedtime, out of joints), **When** she's Lifted again, **Then** the album continues from where it stopped, give or take a few seconds.
4. **Given** a player turns down the Jukebox/Note Blocks slider, **When** the music plays, **Then** it's quieter or silent for them only.
5. **Given** the server switch is off, **When** a Builder is Lifted, **Then** no music plays. She still wears the boombox and still dances on a steady beat.

---

### User Story 2 - The boombox looks awesome (Priority: P1)

While Lifted she wears the Midnight Machine on her back:
- **Its own design:** a boombox designed for this, not a recoloured block.
- **The album art** on its front panel.
- **A glow** in the album's colours.
- **Speaker cones** that pulse in time with the music.

**Why this priority**: the user asked for a big design effort here. It's what players will see.

**Independent Test**: The user picks a concept from rendered options, then sees it in game on a Lifted Builder from several angles, by day and by night.

**Acceptance Scenarios**:

1. **Given** the design work starts, **When** concepts are ready, **Then** the user sees at least three rendered options and picks one (or asks for changes) before the in-game model is made.
2. **Given** a Lifted Builder, **When** players look at her back, **Then** they see the chosen boombox with the album art on its front panel, glowing.
3. **Given** the music is playing, **When** the song's bass, mids and highs change, **Then** the six VU bars rise and fall with them: the bass bar with the kick, the top bars with the hi-hats, all of them low in quiet passages.
4. **Given** she isn't Lifted, **When** players look at her, **Then** she has no boombox.

---

### User Story 3 - She dances to the beat (Priority: P2)

While the music plays she dances in time with it:
- **Bobbing** up and down on the beat.
- **Spinning** on big hits.
- **Swaying and shuffling** between blocks.
- **Stylish moves** while she waits.

**Why this priority**: it makes the music come alive. It comes after the music and boombox because it needs both.

**Independent Test**: Watch a Lifted Builder through a track, and check her movement keeps time with what's heard. Compare her build speed and positions with dancing on and off.

**Acceptance Scenarios**:

1. **Given** the music is playing, **When** a beat lands, **Then** her bob lands on it, as the player hears it.
2. **Given** a big hit (a strong accent the beat map marks), **When** it plays, **Then** she spins or does a bigger move.
3. **Given** she's waiting (between placements, hovering), **When** the music plays, **Then** she does stylish idle moves rather than standing still.
4. **Given** she's dancing, **When** she builds, **Then** she places exactly the same blocks from the same spots at the same speed as without dancing. The dance is purely how she looks.

---

### Edge Cases

- **Several Lifted Builders:** each wears her own boombox and plays from her own position. Each keeps her own place in the album, so they may play different tracks.
- **Arriving mid-song:** a player who comes into range hears the track from where it currently is, not from the start.
- **She unloads, or the server restarts:** her place in the album is kept, so the music resumes about where it was.
- **Old saves:** the music starts at track 1 the first time.
- **Without MineColonies:** nothing changes; there are no Builders to play it.
- **A player with sound off:** everything else (boombox, dancing) still shows.
- **Music switch off on the server:** she dances to a steady default tempo, so she's never frozen.

## Requirements *(mandatory)*

### Functional Requirements

**Music**
- **FR-001**: The album's 7 tracks MUST ship inside the mod and play without any download.
- **FR-002**: While a Builder is Lifted and flying, the album MUST play from her position. Every player within about 24 blocks hears it, fading with distance.
- **FR-003**: The music MUST follow the game's Jukebox/Note Blocks volume slider for each player.
- **FR-004**: The album MUST play in order and loop. Each Builder keeps her own place in it, which pauses when she lands and survives a restart.
- **FR-005**: A player coming into range MUST hear the current track at its current position, within about 2 seconds of where it really is.
- **FR-006**: A server setting MUST switch the music off. The boombox and dancing still show.

**The boombox**
- **FR-007**: The boombox's design MUST be chosen by the user from at least three rendered concepts before the in-game model is made.
- **FR-008**: While Lifted, a Builder MUST wear the Midnight Machine (concept B, Synth Machine) on her back, showing the album art at the top, glowing, with six VU-meter bars driven by the music: each bar's height follows its own frequency band (bass on the left to treble on the right) at the moment of the song the player is hearing. When she isn't Lifted it MUST not show.

**Dancing**
- **FR-009**: While the music plays she MUST dance in time with it, with the moves keyed to beats worked out ahead of time for each track and shipped with the music:
  - bob on beats;
  - spin or make a bigger move on marked accents;
  - sway and shuffle while moving;
  - idle moves while waiting.
- **FR-010**: Dancing MUST only change how she looks. Where she stands, what she builds and how fast MUST be the same as without it.
- **FR-011**: The dance MUST stay in time with the music each player hears: a bob lands within about a tenth of a second of its beat.

**General**
- **FR-012**: Nothing in this feature MUST depend on anything beyond Four Twenty and MineColonies, and Four Twenty MUST still load and play without MineColonies.

### Key Entities

- **Album:** the 7 tracks in order, each with its length and its beat map.
- **Beat map (per track):** the times of each beat and of marked accents (big hits), worked out ahead of time.
- **Band levels (per track):** six frequency-band levels (bass to treble), measured about 20 times a second ahead of time, for the VU bars.
- **Album position (per Builder):** her track and how far into it she is. Saved, so it survives landing and restarts.
- **Midnight Machine:** the boombox's look: model, album-art panel, glow, speaker cones.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A player standing next to a Lifted Builder hears the album clearly. At about 24 blocks it has faded out.
- **SC-002**: After she lands and is Lifted again, the music resumes within 5 seconds of where it stopped.
- **SC-003**: Across a full track, at least 9 bobs in 10 land within a tenth of a second of the beat, as the player hears it.
- **SC-004**: A Lifted Builder's build time for the same job is within 2% with dancing on or off.
- **SC-005**: The user picks a boombox concept from the rendered options, and approves the in-game look.
- **SC-006**: The mod download grows by no more than about 20 MB for the whole album.
- **SC-007**: Without MineColonies, Four Twenty loads and plays as before.

## Assumptions

- **The music is the user's own work** and can ship in the mod. It's converted to a compact, game-friendly format: mono, so it can come from her position.
- **Range:** "nearby" means about 24 blocks, like a jukebox.
- **Tempo:** beat maps are worked out offline from the tracks. A track with a changing tempo gets a beat map that follows it.
- **The boombox shows only while she's Lifted.** It's purely cosmetic: no item or recipe.
- **Concepts:** rendered the same way as earlier Four Twenty machine concepts (the Terpinator), shown to the user before the model is built.
- **Server switch off:** dancing falls back to a steady default tempo of 100 beats a minute.

## Out of Scope

- Players carrying or using the Midnight Machine themselves.
- Other music, or choosing tracks in game.
- Other workers dancing or playing music.
