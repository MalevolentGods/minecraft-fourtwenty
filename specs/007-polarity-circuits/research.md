# Research: Polarity Circuits

## R1. Two-terminal parts in the solver

**Decision:** every part in `domain/Circuit` sits between two nodes, a + node and a − node, instead of between a node and ground.
- **Source:** an EMF from − to +, behind its internal resistance, as a Norton stamp across the two nodes. Its current limits apply to the current out of +.
- **Battery:** the same, with the current limited both ways.
- **Load:** a constant power across the pair, relaxed as the conductance `P / Vd²`, where `Vd = V(+) − V(−)`. When `Vd` is below zero the load is reversed: it takes nothing and is flagged `REVERSED` (FR-014). Below its minimum voltage it acts as the resistance it has there, as now.
- **Conductor:** unchanged.
- **Breakpoint stepping** watches each source's `Vd` against its current-limit edges.

**The reference node:** there's no ground in the world, but the matrix needs a reference. Every node keeps its tiny leak (`gmin`, 10⁻¹² of a conductance). That pins each isolated loop at a defined voltage and carries no current worth measuring, about 10⁻¹² Ig, which is the gauge choice the plan asked for. A load with one terminal unconnected therefore gets only that leak: effectively nothing, reported as "no return path" (FR-010).

**Alternatives:**
- **Pick one reference node per connected component:** cleaner, but the leak already does the job, and keeps isolated parts solvable.
- **Keep a ground node:** rejected by the user.

`Circuit.GROUND` stays for the unit tests' resistors to a reference, but the world never uses it.

## R2. Which faces are the terminals

**Decision:** for every powered block, + is the face on the left and − the face on the right, as seen from the block's front (user, 2026-11-05).
- **In code:** with `front` the way the model faces, `plus = front.getClockWise()` and `minus = front.getCounterClockWise()`.
- **Blocks whose `FACING` is the placer's direction:** the Hookahs use `FACING = context.getHorizontalDirection()`, so their front is `FACING.getOpposite()`. Each block's `front(state)` says which applies, so the marks and the maths always agree.
- **Capability:** the block's capability returns a terminal for those two sides and null for the other four.

**The Hit Box:**
- **Low winding:** + on its left and − on its right, like every other block.
- **High winding:** + on its front, the face that today is "the higher tier", and − on its back.
- **Isolated:** the two windings never share a node.

**The Inspiration Infuser:** it has no running draw, only a one-off cost when it catches a colour. It keeps taking that from the Stashes on any Wick touching it. That's an energy draw, not a circuit part, so it has no terminals.

## R3. Coloured Wicks

**Decision:** a `colour` blockstate property on `WickBlock`: undyed plus the 16 dye colours.
- **Dyeing:** using a dye on a placed Wick recolours it.
- **Joining:** a Wick links only to touching Wicks of its own colour, but to any terminal face.
- **Rendering:** the colour comes from a block-colour handler on the sheath's tint index, so no new textures are needed. Items carry the colour on a data component, so a dyed stack places dyed Wicks, and dyeing works in the crafting grid (Wick + dye).
- **Cost:** 17 × 3⁶ × 2 ≈ 24,800 states per Wick tier, about 100,000 for all four tiers. That's within reason, since redstone wire alone has 1,296. Their models are multipart, so baking stays cheap.

**Alternatives:**
- **A block per colour:** 68 blocks. The same number of states, plus registry clutter.
- **A block entity per Wick:** Wicks are plain blocks by design, so that's rejected.
- **Only + and − Wicks:** the user chose colours.

## R4. Terminals in the network walk

**Decision:** the walk keys a terminal as (block node, sign).
- **Contacts:** a terminal touching a Wick connects to the Wick's centre through half the Wick's resistance, as now.
- **Touching terminals:** two terminals facing each other directly join with no resistance (spec assumption), so blocks can be chained − to + for series.
- **Elements:** each block adds its element between its + and − nodes.
- **Hit Box:** it adds two elements, one per winding: the input winding is a load, the output winding a source, coupled through its banks, as now.

## R5. Reversed and open blocks

**Decision:** each solved member reports one of `OK`, `NO_LOOP` (no current path), `REVERSED` (negative voltage) or `OVERLOADED`.
- **`NO_LOOP`:** a block with demand whose current comes out under 10⁻⁶ of its rated current.
- **Where it shows:** the status reaches the Inspoweedometer and the screens' meter line ("no return path", "wired backwards").

## R6. The meter's reference

**Decision:** a `meter_reference` data component on the Inspoweedometer, holding the dimension and the position.
- **Setting it:** sneak-use a Wick to set the reference; sneak-use the air to clear it.
- **Reading:** a Wick read with a reference set shows the voltage between the two. Without one, it shows the current through the Wick and its ratings, as now.

## R7. Marks on the models

**Decision:** `tools/generate_terminals.py` adds two thin plates to every powered block's model: a red "+" on the left face and a black "−" on the right.
- **Idempotent:** each plate is tagged by its element name, so running the script again changes nothing.
- **Rotation:** the blockstate's y-rotation turns the plates with the block.
- **Hit Box:** it gets four plates.
- **Textures:** drawn here from scratch.

## R8. Old worlds

**Decision:**
- **No migration:** blocks keep their state, and Wicks load undyed.
- **No damage:** a line that's only a supply puts every load in `NO_LOOP`. Floating nodes sit near the line voltage, so no Wick sees too much voltage, and nothing burns or overloads.
- **Release notes:** the changelog and the 0.21.0 notes explain how to add a return line.
