# Data Model: Polarity Circuits

## Pure (`domain/`)

### Circuit (changed)
- `source(plus, minus, emf, ohms, maxOut, maxIn)`: an EMF from − to +.
- `load(plus, minus, watts, minVolts)`: constant power across the pair.
- `conductor(a, b, ohms)`: unchanged. `GROUND` stays for tests.
- `Solution` adds a `status` for each load: `OK`, `NO_LOOP`, `REVERSED`, `BROWNOUT`. It also gives the voltage across each part: `volts(plus) − volts(minus)`.

### Terminals
- `plus(front)` returns `front.getClockWise()`, and `minus(front)` returns `front.getCounterClockWise()`. Directions are passed as plain indices, so the class stays free of game types.
- For the Hit Box, the high winding uses `front` and `front.opposite`.

## World (`power/`)

### Terminal
- `(BINode node, Sign sign)`, where `Sign` is `PLUS` or `MINUS`. It's what the capability returns for a face. The Hit Box has one node per winding, each with its own two terminals.

### WickBlock
- A new `colour` property: `none`, plus the 16 dye colours.
- `Link` per side (`NONE`, `WIRE`, `PLUG`): `WIRE` only to a Wick of the same colour, `PLUG` to a terminal face.
- Items: a `wick_colour` data component on the item, which places that colour.

### Network (changed)
- `Member(node, pos, plusNode, minusNode)`.
- Terminal keys replace device keys in the walk. Two terminals touching directly are joined.
- A `status(member)` comes from the solution.

### Inspoweedometer
- A `meter_reference` data component, holding the dimension and the position of a Wick, or empty.
