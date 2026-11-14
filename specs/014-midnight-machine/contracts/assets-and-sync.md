# Contract: assets and sync

## Sound events (`assets/fourtwenty/sounds.json`)

`fourtwenty:midnight_machine.track1` … `track7`, each:

```json
"midnight_machine.track1": { "category": "record", "sounds": [
  { "name": "fourtwenty:midnight_machine/track1", "stream": true, "attenuation_distance": 24 } ] }
```

- **Played on:** the Records source, with linear attenuation.
- **Format:** mono OGG Vorbis at 44.1 kHz.

## Payload `fourtwenty:midnight_machine` (server → client)

| Field | Type | Meaning |
|---|---|---|
| entity | VarInt | the Builder |
| playing | bool | music on (false: stop, dance at the fallback tempo if still Lifted) |
| lifted | bool | wear the boombox |
| track | VarInt | 0-6 |
| positionMs | VarLong | position in the track when sent |
| sentAt | VarLong | server game time when sent |

**When it's sent:**
- at start, stop and track change;
- to players who start tracking her;
- every 30 s while playing.

**Who gets it:** only connections that have the channel.

## Client behaviour

- **Starting:** play at `positionMs + (clientGameTime − sentAt) × 50`, on her, through `SeekingOggStream`.
- **Drift:** if the playing sound is more than 2 s from the expected position, restart it there.
- **Track end:** the client starts the next track itself at 0 too, and the server's payload confirms it.
- **Stopping:** when `playing` turns false or she leaves view, the sound stops.
