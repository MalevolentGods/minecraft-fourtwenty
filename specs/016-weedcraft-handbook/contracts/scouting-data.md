# Contract: Ranks, Stoner Badges, requirements, deeds

## Files

`data/fourtwenty/handbook/ranks/<id>.json`, `data/fourtwenty/handbook/badges/<id>.json` (datapack-overridable).

```json
{
  "name": "handbook.fourtwenty.rank.tenderbud",
  "order": 1,
  "grade": 0,
  "emblem": "fourtwenty:textures/gui/handbook/emblems/tenderbud.png",
  "needs": null,
  "requirements": [
    { "n": "1", "text": "handbook.fourtwenty.req.tenderbud.1", "kind": "honour", "entry": "front/oath" },
    { "n": "2a", "text": "...", "kind": "tracked", "deed": "plant", "subject": {"item": "fourtwenty:weed_seeds"}, "entry": "growing/planting" },
    { "n": "2b", "text": "...", "kind": "tracked", "deed": "harvest", "subject": {"strain": "landrace"}, "count": 4, "entry": "growing/harvest" }
  ]
}
```

Badges have `"patch"` instead of `"emblem"`, no `order`/`grade`, and may set `"needs": "minecolonies"`.

## Deeds (fired by the game)

| Deed | Subject keys | Fired from |
|---|---|---|
| `obtain` | item, tag | inventory change (throttled), catch-up |
| `craft` | item, tag | crafting result taken |
| `plant` | item, strain | weed/Pot Pot planted |
| `harvest` | strain, family, grade, block | weed crop / Pot Pot harvested |
| `trim` | strain, block (machine) | Trim Tray and trimmers |
| `cure` | strain, block | Curing Rack and curers |
| `extract` | item (oil, rosin) | extract machines |
| `smoke` | item (joint, pipe, bong, hookah, vape, edible), strain | smoking paths into StrainEffects |
| `high` | strain, family | StrainEffects high start |
| `pollinate` | strain | Pollination |
| `pheno` | grade | Pheno Chamber result |
| `discover` | terpene (code), count of all | TerpeneDiscovery |
| `infuse` | result | Infuser match won |
| `terpinate` | item | Terpinator output |
| `place` | block, tag | block placed by the player |
| `power` | block, tier | a machine runs on BI near/owned by the player |
| `overload` | block | a wick/part burns out (yes, a badge for that) |
| `circuit` | signature (`light`, `inverter`, `and_lock`, `latch`, `clock`, `adder`) | output change + signature (research R9) |
| `wear` | item | the sash/Kush Suit worn |
| `earn` | rank, badge | another rank/badge earned |
| `colony_*` | (MineColonies) `colony_dispensary`, `colony_budtender`, `colony_lifted` | `compat/minecolonies/handbook` |

A subject filter matches when every given key matches. Counts accumulate per requirement.
