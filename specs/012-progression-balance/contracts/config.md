# Contract: Config Keys (fourtwenty-processing-server.toml)

Saved config files keep their old values, so these tables are the release notes' "edit these to get the new balance" list.

## Changed defaults

| Key | Old default | New default |
|---|---|---|
| `seedChanceLandrace` | 0.25 | 0.12 |
| `seedChanceNeutrals` | 0.25 | 0.15 |
| `seedChanceCool` | 0.05 | 0.10 |
| `seedChanceBiome` | 0.20 | 0.10 |

## New keys

New keys are written automatically with their defaults; nothing to edit.

| Key | Default | Range |
|---|---|---|
| `yieldLandrace` | "1-1" | min-max, 1 to 8 |
| `yieldNeutrals` | "1-2" | |
| `yieldWarm` | "1-2" | |
| `yieldGreen` | "2-2" | |
| `yieldCool` | "2-2" | |
| `yieldBiome` | "2-2" | |
| `yieldCross` | "2-3" | |
| `yieldDoubleCross` | "3-3" | |
| `yieldUltra` | "3-4" | |
| `yieldGolden` | "4-5" | |
| `potencyLandrace` … `potencyGolden` | 1, 2, 2, 2, 3, 3, 3, 4, 4, 5 | 1 to 5 |
| `plotSeedChance1` … `plotSeedChance5` | 0.10, 0.20, 0.30, 0.40, 0.50 | 0 to 1 |
| `starterSeeds` | 4 | 0 to 64 (0 turns it off; MineColonies only) |

## Unchanged

- Seed chances: `seedChanceWarm`, `seedChanceGreen`, `seedChanceCross`, `seedChanceDoubleCross`, `seedChanceUltra`, `seedChanceGolden`.
- Other settings: `cropSpeed`, `climateChance`, `pollenChance`, and every trim and cure setting.

## Servers to edit at release

Each server's saved toml needs the four changed defaults set by hand:

- **Hera:** `~/minecolonies/mc_data/config`
- **Flatworld:** `/opt/minecolonies/data/config`
- **Local test server:** `config/`
- **Game-test run directory:** `.compat/runs/beds-dev/config`
