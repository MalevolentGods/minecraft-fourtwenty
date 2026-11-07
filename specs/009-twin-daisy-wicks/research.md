# Research: Twin Wick and Daisy Wick

## R1. Conductors instead of block positions

- **Decision**: the walk's wire graph is keyed by `Conductor(BlockPos pos, Core core)` with `Core` one of `WIRE` (a single Wick), `PLUS` and `MINUS` (a cable's cores). Edges join conductors; contacts join a terminal to a conductor through half a block's resistance, as now. Runs of plain conductors collapse as now. The `Wick` record gains its `Core`, so a cable block appears as two entries in `wicks`.
- **Rationale**: the solver already handles any graph of nodes and resistances; the only thing single-conductor about today's code is the key. Keeping everything after the walk unchanged keeps losses, burnout, glow and readings identical.
- **Alternatives**: (a) a separate network type for cables: duplicates the solve and can't mix with rails. (b) Make a cable two invisible single Wicks: the joins depend on faces and order, which a block-per-conductor model can't express.

## R2. Twin Wick connections

- **Decision**: Twin blocks of one colour join `PLUS`–`PLUS` and `MINUS`–`MINUS`. A powered block beside a Twin block on any face is in contact: its + terminal with `PLUS`, its − with `MINUS`. Two Twin blocks touching the same powered block are joined through its terminals (inline wiring). The Hit Box contacts by face: front or back gives the high winding, left or right the low winding, top and bottom nothing.
- **Rationale**: the spec's "can't be wired backwards". Inline continuity falls out of the contacts: both sides meet the same terminal nodes.
- **Alternatives**: direct core-to-core joins across a block (skipping its terminals): then a broken machine would still pass current, which isn't what a block between two cable ends should do. With contacts, the path goes through the terminal nodes, which exist while the block exists.

## R3. Daisy line order and branches (`domain/DaisyLine`)

Input: cable blocks (ids) with their cable neighbours, and devices (ids) with the cable blocks they touch, plus a fixed order of faces for ties. Output: an ordered list of items (cables and devices) or a failure: `BRANCH`, `RING`, `EMPTY`.

- **Decision**:
  1. Split the cables into components by cable–cable joins. Each component must be a path (no cable with more than two cable neighbours), or `BRANCH`; a component that is a closed ring is `RING`.
  2. A device whose cables all lie in one component is *beside* it: it is spliced right after the earliest of those cables in the component's order. Ties between devices beside the same cable go by face: north, east, south, west, up, down.
  3. A device touching exactly two components, through one end cable of each, is *inline*: it joins the two components. Anything else (three components, or a cable that isn't an end) is `BRANCH`.
  4. Components joined by inline devices must form one path, or `BRANCH` (or `RING` if they close).
  5. The line is read from one end. Which end doesn't matter: R4 makes both give the same circuit. For determinism the end with the lower block position comes first.
- **Rationale**: covers inline, beside, and mixed, and settles corners: a block at a corner touches two cables of one component, so it is beside, not inline.
- **Alternatives**: order devices by distance from a source: needs the source identified and breaks with two sources.

## R4. Splicing and the return core

- **Decision**: walk the ordered items with a current point `P`, starting at the first cable's `MINUS` (the start end cap). A cable item joins `P` to its `PLUS` and moves `P` there. A device item makes a contact from `P` to its upstream terminal and moves `P` to its downstream terminal. Upstream is − for a source (Hookah, Stash) and + for anything else. At the end, `P` joins the last cable's `MINUS` (the far end cap). The `MINUS` cores of consecutive cables in the order are joined, also across an inline device, so the return core runs the whole line.
- **Rationale**: current flows one way through every block; loads take it in at +, sources push it out at +. Reversing the order reverses every block and the loop alike, so the circuit is the same: the readings are independent of the end read from, and of where the sources sit.
- **The Hit Box on a Daisy line** is spliced as a load on the winding the line touches. Feeding a Daisy line from a Hit Box is done through rails into the line's ends (R5).
- **Stashes** are spliced as sources. A Stash only charges when a higher voltage drives current into its +, which a series string with it as a source never does; charging through a Daisy line is out of scope.

## R5. Single Wicks meeting cables

- **Decision**: a red single Wick beside a Twin block joins its `PLUS`, a black one its `MINUS`, through the usual half resistances; other colours don't join. For a Daisy line, if one end cable touches a red Wick and the other end cable a black Wick, the line is read from the red end, `P` starts at the red Wick instead of the first end cap, and ends at the black Wick instead of the far end cap. Otherwise single Wicks beside a Daisy line don't join it.
- **Rationale**: red and black are already the convention for + and − (the demos, the README). It lets a base on rails feed cables.

## R6. Readings

- **Decision**: a Twin block reads the voltage between its two cores and each core's current. A Daisy block reads the current in its supply core (the string's current) and the voltage between its cores there. A Daisy block on a failed line reads the failure: "a Daisy line can't branch", "a Daisy line can't close on itself", or, from the solve, "no source in the loop" and "the loop is open".

## R7. Art

- **Decision**: `tools/generate_cables.py` draws, per tier, a Twin texture (two cores in the tier's material, one red-sheathed and one black, twisted at a 4-pixel pitch) and a Daisy texture (the pair side by side, with a band texture used where a side plugs into a block). Models are a core cube and six arms like single Wicks, drawn as two parallel strands; a side plugged into a block ends in a fork (two short prongs). Colour shows as a sheath stripe in the dye colour, undyed showing none. Lit textures glow as single Wicks do. PortModel draws the block's own fitting where a cable plugs in, as for Wicks, on every face rather than terminal faces only.
