# Research: Lit Bits and the Weedweeno

## R1. The Lua engine

**Decision:** Cobalt 0.9.9 (`cc.tweaked:cobalt`, MIT, no dependencies), the sandboxed Lua 5.2 that CC: Tweaked uses. It comes from `https://maven.squiddev.cc` and ships inside the mod jar through NeoGradle's jarJar, so players install nothing.

**Rationale:**
- **Sandbox:** `CoreLibraries.standardGlobals` gives only `base`, `table`, `string`, `coroutine`, `math` and `utf8`. The system library (`print`, `dofile`, `collectgarbage`, `io`, `os`) is a separate add-on that we never load. So there is no file, OS or network access by construction.
- **Interrupts:** Cobalt can interrupt a running VM, and coroutines yield cleanly.
- **Proven:** it's what CC: Tweaked runs on every server that has it.

**Alternatives:**
- **LuaJ:** not maintained, and its interrupts are weaker.
- **Writing our own interpreter:** months of work.

## R2. Running a show: frames and the time budget

**Decision:**
- **Coroutine:** each Weedweeno holds one `LuaState` and runs its program as a coroutine.
- **Each tick:** the server resumes the coroutine until the program calls `frame()` or `sleep(n)`, which yield.
- **Budget:** a single shared watchdog thread calls `state.interrupt()` when a Weedweeno's budget for the tick runs out (2 ms by default, config). The `InterruptHandler` then throws "too slow: call frame() in your loop". Cobalt checks the flag inside its interpreter loop and inside pattern matching, so even `while true do end` stops.
- **Global cap:** all Weedweenos together get at most 10 ms a tick (config). Past that, the rest skip a tick in turn.

**Memory:** Cobalt doesn't count memory.
- **String guards:** `string.rep` and string concatenation of more than 1 MB raise an error.
- **Size check:** each Weedweeno's tables are sampled every 100 ticks (entries counted, with a budget), and a program past 200,000 entries is stopped as "too big".
- **Restricting programming:** server owners can limit programming to operators (`weedweeno.operatorsOnly`).

## R3. A Lit Bit as an LED

**Decision:** a new circuit element in `domain/Circuit`, an **LED**: a current-controlled load with a knee, like a real LED driven by a constant-current driver.
- **Current:** `I(V) = I_set × clamp((V − V_f) / (V_r − V_f), 0, 1)`, where `V` is the voltage across the terminals.
- **The setting:** `I_set = (r + g + b) / 255 × I_channel + I_idle`.
- **Voltages:** forward voltage `V_f = 0.6 × 4.2 = 2.52 Kb`; rated voltage `V_r = 4.2 Kb`.
- **Defaults:** `I_channel = 0.238 Ig` (1 W per channel at 4.2 Kb, so full white is 3 W, a hundredth of a machine); `I_idle = 0.002 Ig`.
- **Wired backwards:** `V < 0` gives `I = 0` and the status `REVERSED`.
- **Overload:** as every block, above 1.5 × 4.2 Kb.
- **Solving:** `I(V)` rises with `V`, so the relaxation step uses the conductance `I(V₀) / V₀` and converges.
- **Brightness:** what the player sees is the colour × `clamp((V − V_f) / (V_r − V_f))`.

**Rationale:** it's monotone, so the solve is stable. Its numbers check easily against a hand calculation (SC-004). And it does what a real LED does: dark below its knee, dim between, and steady current above.

**Re-solving:** a light show changes lamps every tick. The network's signature counts each lamp's current to the nearest 1/32 of full, so small changes don't force a solve. A full re-solve of 1,024 lamps is measured in the timing test.

## R4. Getting colours to players

**Decision:**
- **Rendering:** the colour is drawn by a block-entity renderer: a full-bright cube in the lamp's colour inside its glass shell. Changing a colour then never rebuilds a chunk's mesh.
- **Sending:** the server batches changed colours per tick into one `LampColours` payload per player tracking the chunk, holding position and RGB as an int for each. It's sent only to connections that have the channel.
- **Light level:** each lamp emits light from a 0–15 blockstate level, set from its brightest channel × brightness. That level updates at most every 10 ticks per lamp, so a strobe can't thrash the light engine. The colour itself changes every tick.

**Alternatives:**
- **Tinted models:** these need chunk re-meshing on every change, which is far too costly for a show.

## R5. IDs

**Decision:** a per-level `SavedData`, `LitBitIds`.
- **Live lamps:** an in-memory index from ID to positions, rebuilt from block entities as they load and unload.
- **Remembered positions:** position → the ID last there when a Lit Bit was broken. These are saved, and kept until a Lit Bit is placed there or for 30 days of game time.
- **Placing:**
  - **At a remembered position:** it gets that ID back.
  - **With the Sticky Finger:** it gets the glove's next ID, and the glove counts up.
  - **By hand:** it gets the lowest ID not live within 64 blocks.
- **Range queries:** the Weedweeno asks the index for lamps within its range by squared distance. That's fine for tens of thousands of lamps.

## R6. The editor

**Decision:** `client/WeedweenoScreen`.
- **Layout:** a program list on the left, then vanilla's `MultiLineEditBox` with our own line-number gutter and error marker, and a console of the last 20 `print` lines below.
- **Buttons:** New, Duplicate, Rename, Delete, Run, Stop, Export, Import. The Bit Chit sits in one slot.
- **Messages:**
  - **Edits:** the client sends whole programs (name, code) in a `WeedweenoEdit` payload when you save or run.
  - **State:** the server sends the program list and status with `WeedweenoState`.
- **Size limit:** a serverbound custom payload in vanilla is capped at 32,767 bytes, so programs are capped at 30,000 characters, not 32 KB (spec FR edge case adjusted).

## R7. Presets

**Decision:** Lua files in `data/fourtwenty/weedweeno/presets/`: `rainbow.lua`, `chase.lua`, `pulse.lua` and `strobe.lua`.
- **New Weedweenos:** a new Weedweeno holds copies, which you can edit.
- **Restore Presets:** a button adds fresh copies of any presets that are missing.

## R8. Items and recipes (own progression, nothing from the Nether)

| Item | Recipe | Notes |
|---|---|---|
| Lit Bit ×4 | glass, red, green and blue dye, Hemp Wick | |
| Sticky Finger | slime ball, copper ingot, leather, hemp | sticky |
| Weedweeno | copper ×4, a Hemp Wick, an Inspoweedometer, redstone, glass pane | |
| Bit Chit ×4 | paper ×4, an ink sac | |

Copying a Bit Chit is a custom recipe: a written one plus a blank one gives two written ones.

## R9. Who can program

**Decision:** anyone who can open the block, as with any other block, so claims and spawn protection apply. A server config switch, `weedweeno.operatorsOnly`, limits editing and running to operators.
