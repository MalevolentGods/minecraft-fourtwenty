# Quickstart: Checking the Progression Balance

## The model

```sh
python tools/progression_model.py                 # the run on the shipped defaults: each milestone's hour, and the colony check
python tools/progression_model.py --config path/to/fourtwenty-processing-server.toml   # on a server's saved values
```

It passes when Golden God lands at 50 to 75 hours and every other milestone within 25% of its target (spec SC-003). For each stage, it prints the colony's daily joint demand beside its farm's output.

## Tests

```sh
./gradlew test                    # StrainStatsTest: yield rolls, potency steps, plot seed chances, clamping
./gradlew runGameTestServer -PcompatRunDir=.compat/runs/beds-dev --no-daemon --dependency-verification lenient
```

`ProgressionGameTests` checks:

- **Harvest:** a mature Landrace crop drops 1 bud and 1 seed every time.
- **Yield ranges:** a Golden God crop drops 4 to 5 buds.
- **Plot seeds:** a crop on a tier-5 Grow Op gives an extra seed about half the time over many harvests.
- **Trimming:** about 12% of trimmed Landrace buds give a seed.
- **Starting seeds:** with MineColonies present, a new player gets 4 Landrace seeds once.

## In game (flatworld)

1. **Starting seeds:** log in as a player who hasn't had the grant, and confirm 4 Landrace seeds arrive once. Relogging gives none.
2. **Landrace:** plant, grow and harvest it; it gives 1 bud plus its seed.
3. **Tooltips:** read a Golden God bud's tooltip for its yield, potency and growth; compare with Landrace.
4. **Plot seeds:** harvest on a Grow Op and watch for extra seeds.
5. **Potency:** smoke a high-potency strain and confirm the effect is stronger and longer than the same strain with plain traits used to be.
