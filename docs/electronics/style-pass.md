# Electronics style pass

The user's brief (2026-11-07): bespoke connections for everything a Ribbon or wire meets, taken one by one as the power ports were; then a full
texture and animation pass, "seamless and beautiful to behold", in the spirit of the Inspiration Infuser.

Decisions (user, 2026-11-07):

- **Scope:** electronics first (components, Hemp Thread, Eighth Ribbon, Roach Clip, Lit Bit, Weedweeno, Bench Rig, Bottomless Stashes,
  Twin and Daisy Wicks), then the other power blocks, then the rest of the mod, a family at a time.
- **Look:** stoner-lab fantasy, the Infuser's language: brushed steel, dark trim, glass, glowing resin, lime light lines. Parts read by
  what they do, not as photographs of real components. (Their real names stay in their tooltips.)
- **Animation:** driven by the real circuit. What you see is what the circuit is doing.

## The language

Taken from `tools/generate_infuser_model.py`: 256-pixel procedural textures (brushed steel `#555B63`, dark `#17191D`, trim
`#CDD2D8`/`#4C525A`, smoked glass), the lime light line `#9BD42A`, the resin colours, fullbright glows, cutout plus translucent composites.

- **Every part stands on a small steel plinth** with a chamfered trim and a lime light line that is lit while the part is powered (logic)
  or carrying current (passives). The part itself sits on it, under glass where it holds something that glows.
- **Pins are glands:** a brass ferrule for a single pin, its collar coloured by role (red VDD, black GND, gold out, silver in, green gate);
  a bus face is a steel header with nine sockets.
- **Glow means something.** Nothing glows for decoration alone: a glow is a voltage, a current, a bit or a charge.

| Part | Look | What the circuit drives |
|---|---|---|
| Resinator | A glass tube on the plinth, a resin core banded in its value's colour code | The core warms from amber to white with the power it burns; it smokes near its rating |
| Kief Cap | A glass jar | Golden kief fills it with its charge (volts over its rating) |
| Spliff Coil | Copper wound round a rolled spliff | Rings of light round it with its current |
| One-Hitter, Zen Hitter | A glass one-hitter pipe | Its bowl glows ember while it conducts |
| Toggle Joint, Kill Switch, Puff Button | Brass toggle, a relay under glass, a mushroom button | Their lights show closed |
| The Nugs | A glass dome on the plinth, a resin crystal nug inside, the gate's symbol engraved on the plate | The nug glows lime while the output is 1 |
| Bus chips (Puff-Pass, Pack, Sampler, Dealer, Blend, Stacker, Scale, Jar) | A longer steel slab with a glass window of nine resin beads | One bead per output, lit for a 1 |
| Ticker | A small pendulum under glass | Swings and flashes on its beat |
| Sativa FET, Indica FET | A three-legged glass bulb, green or purple resin | Glows with its channel current |
| Hype Amp | A glass prism | A level of light up its face with its output |
| Hemp Thread | High-resolution braided hemp | A lime pulse runs along it while it's high |
| Eighth Ribbon | Ridged ribbon, the nine coloured wires | Each wire glows while its bit is 1 |
| Roach Clip | A brass clip actually biting the ribbon | Its channel's wire glows through the clip |
| Bench Rig | A lab bench supply | Its set voltage and current shown on its face |
| Bottomless Stash | A glass jar of smoke that never settles | |

## Connections, one by one

Like `tools/generate_ports.py` and `client/PortModel.java` for the power blocks: each side of each block gets its own fitting, drawn only when
something is plugged in there.

| Meeting | Fitting |
|---|---|
| Ribbon to Ribbon | Done: auto-shaped straights, mitred and folded corners, T and cross, standing on edge for vertical runs |
| Ribbon to a chip's bus face | An IDC header on the chip, the ribbon crimped into a socket |
| Ribbon to Roach Clip | The clip's jaws on the ribbon |
| Wick or Hemp Thread to a single pin | The pin's gland, the wire seated in it |
| Wick or Hemp Thread to Roach Clip | A lead from the clip's jaw |
| Wick to Bench Rig, Bottomless Stash | Bespoke ports, as the power blocks have (Lit Bit and Weedweeno have theirs) |

## Circuit to client

The live circuit runs on the server. Each component's block entity keeps a few bytes of what it shows (output bits, a level 0 to 15),
updated from the live solution and sent to clients only when it changes, at most four times a second. Ribbons get a small block entity for
their nine bits. Glows are drawn fullbright.

## Phases

1. **Concept board** (an HTML page with the hero parts in 3D, animated by simulated circuits). Approval before building.
2. **Circuit to client:** the visual state, its sync and throttling, tested.
3. **Components** family by family (passives, switches, Nugs, bus chips, actives), each with its pin fittings.
4. **Wires:** Hemp Thread, Ribbon, Roach Clip, and the connections above.
5. **Supplies and lights:** Bench Rig, Bottomless Stash, Lit Bit, Weedweeno, Twin and Daisy Wicks.
6. Then the other power blocks, then the rest of the mod.

Each phase goes to flatworld for a look in game before the next.
