# Contract: Terminals, Wicks and Readings

## Terminals

| Block | + terminal | − terminal |
|---|---|---|
| Every powered block (Hookahs, Stashes, machines, Grow Ops, benches, Re-Up Stations, Terpinators, the Think Tank, the Rosin Rig, Pheno blocks) | its left face, seen from its front | its right face, seen from its front |
| Hit Box, low-voltage winding | left face | right face |
| Hit Box, high-voltage winding | front face | back face |

- **Marks:** each terminal is marked on the model, with a red + and a black −.
- **Other faces:** a block's other faces don't connect to anything.
- **Infuser:** the Inspiration Infuser has no terminals. It takes its one-off cost from Stashes on a touching Wick.

## Wicks

- **Colours:** every tier comes undyed or in the 16 dye colours. Use a dye on a placed Wick, or craft a Wick with a dye, to colour it.
- **Joining:** a Wick joins only touching Wicks of its own colour, and joins any terminal face it touches.
- **Ratings:** every colour of a tier has the same resistance and ratings, as in 0.20.0.

## Behaviour

1. Current flows only around closed loops: from a source's +, through loads, back to its −.
2. Series parts share one current. Parallel parts share one voltage.
3. A machine wired backwards refuses to run and says "wired backwards".
4. A machine with no closed loop gets nothing and says "no return path".
5. The Hit Box's windings are isolated. Power crosses at 10:1, less 2%.

## Inspoweedometer

| Use | Shows |
|---|---|
| on a block | `4.20 Kb · 25.0 Ig · 168 mΦ · 105 W` (across its terminals), or "no return path", or "wired backwards" |
| on a Wick, no reference | its current, its rating, its share, its resistance and its loss, as in 0.20.0 |
| sneak-use on a Wick | sets that Wick as the reference |
| on a Wick, with a reference | `4.19 Kb from the reference` |
| sneak-use on air | clears the reference |
