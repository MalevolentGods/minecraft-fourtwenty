# Contract: Tie-dye data

## Item component `fourtwenty:tie_dye`

`{pattern: int 0-7, colourway: string}`. Codec and stream codec. An unknown colourway (a data pack removed it) renders in its pattern with the first common colourway's colours and keeps its id, so nothing is lost if the pack comes back.

## Block entity `fourtwenty:tie_dye`

Saved: `colourway`. Synced to clients in the update tag. On load or change on the client, the block is re-meshed. Mined: drops its item with the component `{pattern from state, colourway}`.

## Colourways data `data/fourtwenty/tie_dye/colourways.json`

```json
{ "colourways": [ { "id": "ashtray", "colours": ["mc_black", "mc_gray", "mc_white"] } ] }
```

2-4 colour codes, each one of the 80. Bad entries are logged and left out. Names: `tiedye.fourtwenty.colourway.<id>`.

## Shirts `fourtwenty:shirt` (server → client, optional channel)

`{entity: varint, shirt: optional {pattern, colourway}}`, sent to players tracking the citizen on change and on start tracking.
