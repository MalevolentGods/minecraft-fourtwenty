# Data Model: Lit Bits and the Weedweeno

## Pure (`domain/`)

### LedRules
- **Settings:** `forward` (0.6 of rated), `rated` (4.2 Kb), `channelAmps` (0.238 Ig), `idleAmps` (0.002 Ig).
- `amps(r, g, b, volts)`: the LED current law (research R3).
- `brightness(volts)`: 0 to 1.
- `lightLevel(r, g, b, brightness)`: 0 to 15.
- `rgb(h, s, v)`: HSV to RGB.

### Circuit (extended)
- `led(plus, minus, setAmps, forwardVolts, ratedVolts)`: a new element. Its current `I(V)` is in `Solution.ledAmps[]`, with a status (`OK`, `NO_LOOP`, `REVERSED`).

### LitBitIdRules
- `nextFree(used, from)`: the lowest free ID at or above `from`.
- `clamp(id)`: keeps an ID within 1–65,535.

### Programs
- `Program(name, code)`, with the code at most 30,000 characters and the name at most 32.
- `validName` and the size check.

## World

### LitBitBlockEntity
- **Saved:** `id`, `colour` (RGB as an int), `level` (0–15, also a blockstate property).
- **Runtime:** `lastLevelChange`, `brightness` (from the circuit).
- **Power:** its circuit node is an LED (`BINode` role `LED`). Its demand is `setAmps` from the colour.

### LitBitIds (SavedData, per level)
- **Saved:** `remembered`, a map from position to ID with the game time it was saved.
- **Runtime:** `live`, a map from ID to positions, filled by block entities as they load and unload.

### WeedweenoBlockEntity
- `programs`: a list of `Program`.
- `running`: the index of the running program, or −1.
- `status`: `RUNNING`, `STOPPED`, `ERROR` or `SLOW`, with a message and a line.
- `console`: the last 20 lines.
- **Runtime:** a `LuaState`, a coroutine and a sleep counter.
- **Slot:** one item slot for a Bit Chit.
- **Power:** a load (Puff, 5 W).
- **Persistence:** its programs stay on the item when it's broken, as a `weedweeno_programs` component.

### Items and components
- **Sticky Finger:** a `next_id` int component.
- **Bit Chit:** a `program` component (name, code), absent while blank.

## Payloads (each sent only to connections that have the channel)

| Payload | Way | Carries |
|---|---|---|
| `LampColours` | server → client | positions and RGB colours that changed this tick, per chunk |
| `WeedweenoState` | server → client | the program names, the selected program's code, the status and the console, sent when the screen opens and when something changes |
| `WeedweenoEdit` | client → server | an action (`SAVE`, `NEW`, `DELETE`, `RENAME`, `DUPLICATE`, `SELECT`, `RUN`, `STOP`, `EXPORT`, `IMPORT`, `PRESETS`), a program index, a name and code |
| `SetLitBitId` | client → server | a position and an ID |
| `SetFingerId` | client → server | the next ID |
