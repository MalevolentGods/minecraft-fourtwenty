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
- Q: How does the dance come together? → A (2026-11-14, after the first dance was seen in game): her **facing, position and actions are all separate**. Where she is and which way she really faces stay MineColonies' and her flight's; the dance only changes how she's drawn, and her placing arm still swings. On top of the bob, sway and spins she has now, **each track gets its own bespoke routine** that plays along with it: the track's sections (intro, build, drop, breakdown, outro) found ahead of time, a move for each from that track's own set, and a **flourish** now and then on the biggest hits and at section changes. Moves are original, made of ordinary dance vocabulary (step-touch, shimmy, lean-back pump, windmill, air synth, conducting, robot pops, a slow float, and so on): **no Fortnite emotes** or other copied game dances. Each track's flavour follows its title (Rise and Peak climbs to a peak, Smoke and Mirrors is hazy and mirrored, Listen and Learn conducts and cups an ear, Awake and Adapt pops like a robot and loosens up, Beauty and Light opens wide, Far and Away floats and looks to the horizon, Man and Machine swaps between robot and human). The routines are plain files a person can hand-tune.
- Q: Anything between tracks? → A: **A few seconds of silence** (3 s) between tracks; she takes a breather from dancing in it.
- Q: How should the music fall off with distance? → A: Faster than open air: **inverse cube** (-9 dB per doubling of distance), full volume within 4 blocks at 60% of the slider, a smooth fade to silence between 36 and 48 blocks.
