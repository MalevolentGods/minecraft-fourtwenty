# Contract: Builder config and the hut setting

## Server config: `fourtwenty-builders-server.toml`

This is a new file, only registered when MineColonies is present. Every value has a range, and a value out of range falls back to its default (NeoForge config rules).

### `[diagnostics]`

| Key | Default | Range | Meaning |
|---|---|---|---|
| `logBuilderPathing` | false | bool | FR-001: log each Builder's route searches, step-asides, standing-spot changes and time between placements |

### `[watchdog]`

| Key | Default | Range | Meaning |
|---|---|---|---|
| `enabled` | true | bool | Watchdog on/off |
| `slowSeconds` | 20 | 5-300 | Window over which a slow pace is judged |
| `slowFraction` | 0.33 | 0.05-0.9 | Below this share of her expected placing rate counts as slow |
| `stuckSeconds` | 45 | 10-600 | No progress for this long while building counts as stuck; also the time between escalation steps |
| `rescueRadius` | 4 | 1-16 | How far from her next block a rescue looks for a standing spot |
| `fixStepAside` | true | bool | The R1 fix: forget the standing spot after a step-aside |

### `[lifted]`

| Key | Default | Range | Meaning |
|---|---|---|---|
| `enabled` | true | bool | Lifted as a whole; off means no Builder asks for or smokes joints for it |
| `minutes<Family>` | Landrace 5, Neutrals 5.5, Warm 5.5, Green 6, Cool 6, Biome 6, Cross 7, DoubleCross 8, Ultra 9, Golden 10 | 0.5-60 | Lifted time per joint, by family |
| `potencyBonus` | 0.10 | 0-1 | Extra time at potency 5 (scales from 0 at potency 1) |
| `speedLowest` | 1.25 | 1-4 | Placing speed multiplier for Landrace |
| `speedHighest` | 1.5 | 1-4 | Placing speed multiplier for Golden God (others in between by grade) |
| `reach` | 9 | 4-16 | Higher Ground reach in blocks |
| `flightRadius` | 24 | 8-48 | How far the flight planner searches |
| `stash` | 3 | 1-16 | Joints a Builder asks for at most |
| `clearFirst` | true | bool | Higher Ground clears changing blocks top-down before building an upgrade |
| `landingSpeed` | 0.15 | 0.05-0.5 | Blocks a tick she descends when landing (no fall damage at any value) |
| `requestIntervalTicks` | 100 | 20-1200 | How often a Builder checks the spare and opens or cancels her joint request |
| `hoverLookahead` | 64 | 8-256 | How many upcoming blocks Higher Ground considers when choosing a hover point |
| `hoverGrid` | 3 | 1-6 | Spacing of candidate hover points around the work |

## Builder hut setting

| Key | Type | Default | Label (en_us) |
|---|---|---|---|
| `fourtwenty:flywhenlifted` | MineColonies `BoolSetting` | true | "Fly when Lifted:" |

- **Where it shows:** in the Builder's existing Settings tab, beside "Construction Strategy".
- **Off:** she neither asks for nor smokes joints for Lifted, and lands if she's in the air (FR-014).
- **Joints from the player:** joints a player drops in the Builder's hut are always hers to smoke; the citizens-first rule applies only to the colony's Dispensary and Warehouse stock.

## Payload: `fourtwenty:lifted` (server → client)

| Field | Type | Meaning |
|---|---|---|
| entity | VarInt | citizen entity id |
| on | bool | shimmer and smoke on/off |

- **Who receives it:** players tracking her, on connections that have the channel.
- **On login or start of tracking:** the current Lifted Builders in view are sent.

## Log lines (server log, INFO)

```
[FourTwenty/Builders] rescue <step> colony=<id> citizen=<name> building=<pos> at=<pos> next=<pos>
[FourTwenty/Builders] lifted colony=<id> citizen=<name> strain=<strain> minutes=<m>
[FourTwenty/Builders] landed colony=<id> citizen=<name> reason=<buff|switch|job|state|unload|restart>
```

Diagnostics lines (only with `logBuilderPathing`):

```
[FourTwenty/Builders] path citizen=<name> job=<walkTo|closeTo> target=<pos> from=<pos>
[FourTwenty/Builders] stepaside citizen=<name> block=<pos>
[FourTwenty/Builders] workfrom citizen=<name> old=<pos> new=<pos|none> why=<column|stuck|far|stepaside>
[FourTwenty/Builders] placed citizen=<name> block=<pos> sinceLast=<ticks>
```
