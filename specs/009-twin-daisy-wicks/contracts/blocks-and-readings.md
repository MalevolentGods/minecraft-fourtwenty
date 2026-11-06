# Contract: blocks, recipes and readings

## Blocks and items

`fourtwenty:{hemp,rolled,glass,gold}_{twin,daisy}_wick`, each a block and its item, in the Four Twenty creative tab, in `fourtwenty:bi_cables` (block tag) and `minecraft:mineable/pickaxe` like Wicks.

Names (lang): "Hemp Twin Wick", "Hemp Daisy Wick", "Rolled Twin Wick", … "Gold Daisy Wick".

## Recipes

- Twin Wick: two single Wicks of the tier side by side (shaped, 1×2 either way) → 1.
- Daisy Wick: two single Wicks of the tier and an oxeye daisy (shapeless) → 1.
- A dye on a placed cable sets its colour, as on Wicks.

## Inspoweedometer

| Target | Reads |
|---|---|
| Twin Wick | `V Kb between its cores · I+ Ig / I− Ig · its resistance per core` |
| Daisy Wick, live | `I Ig through the string · V Kb between its cores here` |
| Daisy Wick, failed | `a Daisy line can't branch` / `a Daisy line can't close on itself` / the solver's `no source in the loop`, `the loop is open` |

Lang keys: `message.fourtwenty.meter.twin`, `message.fourtwenty.meter.daisy`, `message.fourtwenty.meter.daisy_branch`, `message.fourtwenty.meter.daisy_ring`.
