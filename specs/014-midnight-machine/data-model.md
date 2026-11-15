# Data Model: Midnight Machine

## album.json (asset, written by the tools)

```json
{ "title": "Midnight Machine", "artist": "ATLemental", "tracks": [
  { "id": "track1", "title": "Rise and Peak", "length_ms": 265000, "bpm": 120.0,
    "beats_ms": [ ... ], "accents_ms": [ ... ] } ] }
```

## AlbumPosition (saved per Builder, beside the Lifted buff)

| Field | Meaning |
|---|---|
| colony, citizen | key (as Lifted) |
| track | 0-6 |
| positionMs | how far into the track |
| playing | whether her music is running (not saved: she always loads landed) |

**Rules (`AlbumClock`, pure):**
- **Advancing:** `advance(pos, ms, lengths)` moves the position on, rolling over to the next track and looping after the last.
- **Pausing:** `pause` keeps the position.
- **Starting:** a new Builder starts at track 0, 0 ms.

## Play state (memory, per Builder on the server)

Last sent: playing, track, position, game time. Used to send only on changes, and for the 30-second drift check.

## Client state (per Builder in view)

- **Her sound:** the `AlbumSoundInstance`.
- **Beat clock:** the local clock (track and start time in game ms), from which `DanceRules` reads the pose.

## Config (`fourtwenty-builders-server.toml`, new `[music]` section)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | true | Midnight Machine music on or off (the boombox and dancing stay) |
| `fallbackBpm` | 100 | Dance tempo when the music is off |
| `danceBob` | 0.12 | Bob height in blocks |
| `spinCooldownSeconds` | 2 | Least time between spins |
