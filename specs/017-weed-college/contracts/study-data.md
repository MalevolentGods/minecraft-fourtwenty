# Contract: Study data

**Source of truth:** one table in `tools/generate_college_studies.py`, the way `tools/generate_handbook_scouting.py` holds the ranks and badges.

**What it writes:**
- one JSON file per study in `src/main/resources/data/fourtwenty/college/studies/<id>.json`;
- the lang keys (`college.fourtwenty.study.<id>`, `.desc`, and the branch names);
- the crafter recipes for unlock studies.

It validates the tree as it writes, and fails on: an unknown parent, a cycle, an unreachable study, an unknown item or terpene code, or a tier-6 study whose branch has no tier-5 study.

## Study JSON

```json
{
  "branch": "botany",
  "tier": 2,
  "parents": ["fourtwenty:bot_soil_science"],
  "kind": "strains",
  "supplies": [
    {"item": "fourtwenty:weed_seeds", "count": 8, "strain": "fourtwenty:landrace"},
    {"item": "minecraft:sand", "count": 4},
    {"item": "fourtwenty:weed_bud", "count": 8}
  ],
  "gives": {"strains": ["fourtwenty:dust_devil"], "seeds_each": 4},
  "icon": "fourtwenty:weed_seeds",
  "needs": null
}
```

## What each `kind` gives

| Kind | Fields |
|---|---|
| `colours` | `colours`: list of terpene codes (`mc_white`, `myr_pin_lim`, …). Supplies may give a per-colour share with `"per_colour": true`, which the caught-colour discount halves (R9). |
| `strains` | `strains` and `seeds_each`: a seed batch delivered to the hut at write-up. |
| `effect` | `effect` (a `ColonyEffects.Effect` name) and `strength`. |
| `unlock` | `research_effect`: a MineColonies effect id, applied when the study finishes. |

**Loading:**
- Study data is loaded by a reload listener on both the server and the client.
- A datapack may override any study, but the generator's validation also runs at load time: an invalid tree logs an error and disables just the invalid studies, never the College.
