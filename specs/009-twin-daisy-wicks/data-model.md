# Data Model: Twin Wick and Daisy Wick

## CableBlock (block, 8 registered)

| Field | Values | Notes |
|---|---|---|
| kind | TWIN, DAISY | fixed per block |
| tier | PUFF (Hemp), TOKE (Rolled), RIP (Glass), BLAZE (Gold) | fixed per block; each core rated as one single Wick of the tier |
| colour (state) | the 17 Wick colours | dye to change; joins only cables of the same kind and colour |
| north…down (state) | NONE, WIRE, PLUG | WIRE: a cable of its kind and colour, or a red/black single Wick (Twin); PLUG: any powered block |
| lit (state) | boolean | glows while either core carries power |

IDs: `hemp_twin_wick`, `rolled_twin_wick`, `glass_twin_wick`, `gold_twin_wick`, `hemp_daisy_wick`, `rolled_daisy_wick`, `glass_daisy_wick`, `gold_daisy_wick`.

## Conductor (walk only, not saved)

`Conductor(BlockPos pos, Core core)`, with `Core` WIRE (single Wick), PLUS or MINUS (cable cores). Replaces `BlockPos` as the key of the wire graph in `Network.walk`; `Network.Wick` gains `core`.

## DaisyLine (domain, pure)

- **Input**: `cables`: id → cable neighbour ids; `devices`: id → list of (cable id, face index); end ordering key per cable.
- **Output**: `Result.ok(List<Item>)` where `Item` is `Cable(id)` or `Device(id, inline)`, or `Result.fail(Reason)` with `Reason` BRANCH, RING, EMPTY.
- **Rules**: research R3. Splice orientation is the caller's (R4): upstream terminal − for sources, + otherwise.

## Validation

- A Daisy line that fails carries nothing; its blocks are kept with the reason for readings.
- A core over its tier's rating for `burnTicks` burns the block (both cores go with it).
