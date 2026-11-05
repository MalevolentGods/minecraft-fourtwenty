# Contract: Circuit Behaviour and Readouts

What players and server owners can rely on. Numbers are defaults; every one is in the server config under `circuit`.

## Tiers

| Tier | Nominal | A Hookah | Wick rating | Wick insulation | Wick resistance per block | Overload above |
|---|---|---|---|---|---|---|
| Puff | 4.2 Kb | 420 W, 100 Ig | 200 Ig | 6.3 Kb | 6.3 µΦ | 6.3 Kb |
| Toke | 42 Kb | 840 W, 20 Ig | 40 Ig | 63 Kb | 315 µΦ | 63 Kb |
| Rip | 420 Kb | 1,680 W, 4 Ig | 8 Ig | 630 Kb | 15.75 mΦ | 630 Kb |
| Blaze | 4,200 Kb | 3,360 W, 0.8 Ig | 1.6 Ig | 6,300 Kb | 0.7875 Φ | 6,300 Kb |

## Rules

1. **Machines** draw their rated power at any voltage up to their overload limit, and draw nothing with a full buffer or with a high player within 21 blocks.
2. **Hookahs** hold the line at their nominal voltage up to their rated current, then give no more.
3. **Stashes** charge from what the loads leave, discharge when the line falls below them, and move at most a Hookah's current of their tier either way.
4. **Brownout:** with every source at its limit, all machines slow by the same fraction.
5. **Wicks** burn out (smoke and ash) after 1 second above their current rating or their insulation voltage. Nothing else breaks.
6. **Blocks** overload above 1.5 × their tier's voltage: they smoke, stop and keep their contents and progress.
7. **The Hit Box** passes power between adjacent tiers at 98%.

## Inspoweedometer

Using it on a block shows a reading on the action bar. Sneak-using it puts a longer line in chat.

- **On a device:** `4.20 Kb · 25.0 Ig · 0.168 Φ · 105 W`
- **On a Wick:** `4.19 Kb · 100 Ig of 200 Ig (50%) · loses 63.0 mW`
- **On an unpowered line:** `0 Kb · 0 Ig`
- **On a block with no power role:** `Not on a Wick network`

## Screens

Every BI block screen (machines, Stashes, Hookahs, plots, benches, Re-Up Stations, the Hit Box) shows the voltage and current at the block next to its charge bar, formatted the same way.

## Config keys (`fourtwenty-processing-server.toml`, group `circuit`)

| Key | Default | Meaning |
|---|---|---|
| `voltsPuff`, `voltsToke`, `voltsRip`, `voltsBlaze` | 4.2, 42, 420, 4200 | nominal Kb per tier |
| `sourceDroop` | 0.005 | a source's internal resistance, pu |
| `sourceLimit` | 1.0 | a source's current limit, pu |
| `stashEmfLow`, `stashEmfHigh` | 0.96, 0.99 | a Stash's EMF empty and full, pu |
| `stashResistance` | 0.01 | pu |
| `stashLimit` | 1.0 | a Stash's current limit each way, pu |
| `loadMinVolts` | 0.5 | below this, the line browns out, pu |
| `loadHysteresis` | 0.10 | the buffer share a load uses before it asks again |
| `wickOhmsPerBlock` | 0.00015 | pu of the tier's base resistance |
| `wickAmpRating` | 2.0 | pu of a Hookah's current |
| `wickVoltRating` | 1.5 | × nominal |
| `burnTicks` | 20 | ticks over a rating before a Wick burns |
| `overloadMargin` | 1.5 | × nominal |
| `hitBoxLoss` | 0.02 | share lost through the Hit Box |
