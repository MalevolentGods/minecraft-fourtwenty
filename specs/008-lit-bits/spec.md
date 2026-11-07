# Feature Specification: Lit Bits and the Weedweeno

**Feature Branch**: `008-lit-bits`
**Created**: 2026-11-05
**Status**: Draft
**Input**: User description: "Lit Bits and the Weedweeno: programmable RGB lamps for Four Twenty, part of Inspoweednetics. Approved names: Lit Bit (an RGB lamp), Sticky Finger (a glove that places Lit Bits), Weedweeno (a Lua controller block), Bit Chit (an item holding an exported program). A Lit Bit shows any RGB colour and behaves like a real RGB LED in every way we can model: powered from the BI circuit through its + and − terminals, lights only with enough voltage, draws current in proportion to how bright each channel is, dims or goes out when its supply sags, refuses when wired backwards, overloads on too high a voltage, and is controlled by data. Data reaches Lit Bits wirelessly from a Weedweeno within range, addressed by ID. Every Lit Bit placed with the Sticky Finger gets a unique ID; breaking it frees the ID; placing one at the same coordinates as a broken one gives it the old ID back; the ID can be changed in each Lit Bit's screen. The Weedweeno has a simple IDE: a code editor for Lua light-show programs, a few preset modes, and as many saved programs as the player wants. A program can be exported to a Bit Chit, which can be carried, copied and loaded into another Weedweeno. Programs run on the server, sandboxed, with a time budget per tick. Nothing depends on MineColonies. Lua engine bundled in the mod jar."

## Background

Four Twenty's electricity is a real DC circuit (specs 006 and 007): every powered block has a + and a − terminal, and power flows only around closed loops. This feature adds the first programmable electronics, in the school of Inspoweednetics:
- **Lit Bit:** an RGB LED lamp.
- **Sticky Finger:** a glove that places Lit Bits and numbers them.
- **Weedweeno:** a small computer, programmed in Lua, that runs light shows on the Lit Bits within its range.
- **Bit Chit:** an item that carries a program.

All four names are approved by the user (2026-11-05).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A wall of light (Priority: P1)

A player lays a red + rail and a black − rail from a Hookah, and with the Sticky Finger places 30 Lit Bits along them. Each Lit Bit gets the next ID, 1 to 30, and shows it briefly as it's placed. The player places a Weedweeno nearby, powers it from the same line, opens it, picks the "Rainbow" preset and presses Run. The colours sweep along the wall.

**Why this priority**: lamps that light up under a program are the whole feature. Everything else builds on it.

**Independent Test**: power a strip of Lit Bits and a Weedweeno, run a preset, and check that every lamp shows the colour the program gives its ID, tick by tick.

**Acceptance Scenarios**:

1. **Given** a powered Weedweeno and 30 powered Lit Bits in range with IDs 1–30, **When** the Rainbow preset runs, **Then** each lamp shows the colour the preset gives its ID, and the colours move along the IDs over time.
2. **Given** the same, **When** the player presses Stop, **Then** every lamp the program controlled goes dark, and stays dark until something sets it again.
3. **Given** a Lit Bit outside the Weedweeno's range, **When** the show runs, **Then** that lamp doesn't change.

---

### User Story 2 - Lamps behave like real LEDs (Priority: P1)

The Lit Bits are part of the circuit. A lamp lights only with a voltage across its terminals, and draws more current the brighter it is: a lamp set to full white draws three times the current of one set to full red. If the Hookah is sagging, the lamps dim together. Wired backwards, a lamp stays dark and the Inspoweedometer says so. Fed from a Toke line, a Puff Lit Bit overloads and smokes, and never breaks.

**Why this priority**: the user asked for lamps that work exactly like RGB LEDs, powered and controlled through our electronics. Power is half of that.

**Independent Test**: run one lamp at known colours and read its current with the Inspoweedometer, then sag the supply and check that it dims.

**Acceptance Scenarios**:

1. **Given** a lamp on a healthy Puff loop, **When** it's set to (255, 0, 0), then (255, 255, 255), **Then** its current triples, and its power reads accordingly.
2. **Given** a lamp set to black (0, 0, 0), **When** it's read, **Then** it draws only its tiny idle current.
3. **Given** a lamp whose supply is below its forward voltage, **When** it's set to any colour, **Then** it stays dark. Between that and its rated voltage it glows dimmer, in proportion.
4. **Given** a lamp wired backwards, **When** a program sets it, **Then** it stays dark, draws nothing, and the meter says "wired backwards".
5. **Given** a Puff lamp on a Toke loop, **When** powered, **Then** it overloads (smoke, stops, never breaks) until the high voltage is gone.

---

### User Story 3 - Write a show (Priority: P1)

A player opens the Weedweeno and writes their own program in its code editor: a few lines of Lua that set lamps by ID with an RGB colour each frame. The editor shows line numbers, and highlights the line of any error with the message. The player saves the program under a name, alongside the presets and any number of others, and switches between them.

**Why this priority**: the presets show what's possible. Writing your own is the point of a programmable lamp.

**Independent Test**: write a ten-line chase program, run it, and check the lamps. Then write one with a syntax error and check the editor names the line.

**Acceptance Scenarios**:

1. **Given** a program that sets lamp 1 to red and lamp 2 to blue, **When** it runs, **Then** lamp 1 shows red and lamp 2 blue.
2. **Given** a program with a syntax error on line 3, **When** the player presses Run, **Then** nothing runs, and the editor marks line 3 with the error.
3. **Given** a program that loops forever without yielding, **When** it runs, **Then** it's stopped when it uses up its time for the tick, the Weedweeno reports "too slow, stopped", and the server doesn't lag.
4. **Given** a program that tries to read files, run system commands or reach the network, **When** it runs, **Then** those functions simply don't exist for it.
5. **Given** a player with 12 saved programs, **When** they reopen the Weedweeno after a server restart, **Then** all 12 are there, and the one that was running is running again.

---

### User Story 4 - Number the lamps (Priority: P2)

A player sets the Sticky Finger's next ID to 100 and places a row of lamps: 100, 101, 102 and so on. They break lamp 101 by mistake and place a new one in the same spot, which comes back as 101. They open another lamp's screen and change its ID to 7, so it joins group 7 in their program.

**Why this priority**: shows only work if lamps have the right IDs, and big builds need numbering that's quick and forgiving.

**Independent Test**: place, break and re-place lamps, and check the IDs at each step.

**Acceptance Scenarios**:

1. **Given** the Sticky Finger's next ID set to 100, **When** the player places three lamps, **Then** they get 100, 101 and 102, and the glove's next ID becomes 103.
2. **Given** lamp 101 is broken, **When** a Lit Bit is placed at its exact spot, **Then** it gets ID 101 again.
3. **Given** a lamp's screen, **When** the player enters a new ID, **Then** the lamp answers to that ID from then on. Several lamps may share an ID, and then they move together.
4. **Given** a Lit Bit placed by hand without the glove, **When** it's placed, **Then** it gets the lowest ID not used nearby, and it can be renumbered in its screen.

---

### User Story 5 - Share a show (Priority: P2)

A player exports their favourite program to a Bit Chit, hands it to a friend, and the friend loads it into their own Weedweeno. A Bit Chit can be copied with a blank one in a crafting grid.

**Why this priority**: shows are made to be shown off and swapped.

**Independent Test**: export, carry, copy and load a program, and check it's identical and runs.

**Acceptance Scenarios**:

1. **Given** a program in a Weedweeno, **When** the player exports it to a blank Bit Chit, **Then** the Bit Chit's tooltip shows the program's name and length.
2. **Given** a written Bit Chit, **When** it's loaded into another Weedweeno, **Then** the program appears there under its name, unchanged.
3. **Given** a written Bit Chit and a blank one in a crafting grid, **When** crafted, **Then** both come out written.

---

### Edge Cases

- **Two Weedweenos in range of one lamp:** the lamp follows the last colour sent to it.
- **A lamp ID the program never sets:** the lamp keeps its last colour, or stays dark if never set.
- **A Weedweeno without power:** its program stops, and resumes from the top when power returns. Lamps keep their last colour until then.
- **Chunk unloads:** a Weedweeno in an unloaded chunk doesn't run. Lamps in unloaded chunks aren't updated, and catch up when they load.
- **A huge show:** one Weedweeno can address at most a configured number of lamps per tick (default 1,024). Beyond that, changes queue to the next tick.
- **Very long programs:** a program can be up to 30,000 characters (what fits in one message to the server). Longer won't save, and the editor says so.
- **Errors at run time:** the program stops, and the Weedweeno shows the message and its line.
- **Breaking a Weedweeno:** it drops normally, and its saved programs go with it on the item, so nothing is lost.

## Requirements *(mandatory)*

### Functional Requirements

**Lit Bit**
- **FR-001**: A Lit Bit MUST show any 24-bit RGB colour (0–255 per channel) and give off light. Its light level follows its brightest channel.
- **FR-002**: A Lit Bit MUST be a powered block with + and − terminals, like every other powered block (spec 007), and be rated for Puff (4.2 Kb).
- **FR-003**: A Lit Bit's current MUST follow its colour like a real RGB LED. Each channel draws in proportion to its value, up to a set full-channel current, plus a tiny idle current.
- **FR-004**: A Lit Bit MUST stay dark below its forward voltage, glow dimmer between that and its rated voltage, and run fully at its rated voltage. Wired backwards it stays dark and draws nothing. Above its overload voltage it overloads, as every block does.
- **FR-005**: Each Lit Bit MUST have an ID (a whole number from 1 to 65,535), shown and editable in its screen. Several Lit Bits MAY share an ID.

**IDs and the Sticky Finger**
- **FR-006**: Placing a Lit Bit with the Sticky Finger MUST give it the glove's next ID, then raise that by one. The glove's screen MUST let the player set the next ID.
- **FR-007**: Breaking a Lit Bit MUST free its ID and remember it at that position. A Lit Bit later placed at the same position MUST get that ID again by default.
- **FR-008**: A Lit Bit placed by hand, without the glove, MUST get the lowest ID not already used within the range of the nearest Weedweeno. With no Weedweeno nearby, it gets the lowest ID not used within 64 blocks.

**Weedweeno**
- **FR-009**: The Weedweeno MUST be a powered block (+ and − terminals, Puff), drawing a small current while it runs. Without power its program stops.
- **FR-010**: The Weedweeno MUST control every Lit Bit within its range by ID, without wires. The range is set in config, default 64 blocks.
- **FR-011**: The Weedweeno's screen MUST have:
  - a code editor with line numbers;
  - a list of saved programs: new, rename, delete, duplicate;
  - Run and Stop;
  - a status line: running, stopped, or the error and its line.
- **FR-012**: The Weedweeno MUST come with at least four preset programs: Rainbow, Chase, Pulse and Strobe. Presets can be edited, and saved as new programs.
- **FR-013**: A Weedweeno MUST hold as many saved programs as the player makes, kept through restarts and when the block is broken and placed again.
- **FR-014**: Programs MUST be written in Lua, with a small light-show library. Programs MUST be able to:
  - set a lamp's colour by ID (RGB, or hue, saturation and value);
  - read the time;
  - wait for the next frame;
  - list the IDs in range.
- **FR-015**: Programs MUST run on the server, sandboxed. They MUST have no access to files, the operating system, the network or the game beyond the light-show library. Each Weedweeno gets a fixed time budget per tick; a program that exceeds it is stopped and reported, and the server keeps 20 ticks a second.
- **FR-016**: A running program MUST start again when its Weedweeno loads after a restart.

**Bit Chit**
- **FR-017**: A blank Bit Chit MUST take a program exported from a Weedweeno: its name and its code.
- **FR-018**: A written Bit Chit MUST load into any Weedweeno as a new saved program, and copy onto a blank Bit Chit in a crafting grid.

**General**
- **FR-019**: Every tunable number MUST be a config value: range, time budget, lamps per tick, program size, LED currents and voltages. Nothing may depend on MineColonies.
- **FR-020**: The Lua engine MUST ship inside the mod jar, so players install nothing extra.

### Key Entities

- **Lit Bit:** a lamp. It has a position, an ID, its current colour, and its electrical state (lit, dim, dark, reversed, overloaded).
- **ID registry:** per world. Which IDs are in use near a place, and which ID was last at each position where a Lit Bit was broken.
- **Sticky Finger:** a glove that carries its next ID.
- **Weedweeno:** a controller. It has a range, its saved programs (name, code), the one running, and its status.
- **Program:** a name and Lua code, up to 30,000 characters.
- **Bit Chit:** an item that holds one program.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A player can place 30 lamps, power them and run a preset show within five minutes, with no manual.
- **SC-002**: A Weedweeno running 1,024 lamps at 20 frames a second keeps the server at 20 ticks a second.
- **SC-003**: A runaway program, an infinite loop or a deep recursion, is stopped within one tick, and the server never lags because of it.
- **SC-004**: Each lamp's current agrees with the LED model to within 1%, at any colour and voltage.
- **SC-005**: Breaking and re-placing a lamp at the same spot gives back its ID every time.
- **SC-006**: A program carried on a Bit Chit runs the same show in another Weedweeno, colour for colour.

## Assumptions

- **Range from the controller:** Lit Bits within a Weedweeno's range listen to it. With two in range, the latest command wins.
- **Weedweeno power:** it draws about as little as one Lit Bit at idle. It's a microcontroller.
- **Lit Bits are Puff-tier,** like 5 V LED strips. Higher tiers need a Hit Box to step down.
- **Frame rate:** shows update at most once per tick (20 frames a second), and lamps change colour on the client smoothly within that.
- **Who can program:** anyone who can open the block. A server config switch can restrict programming to operators.
- **Out of scope:** reading sensors, redstone, other machines, sound, and Weedweenos talking to each other are later steps.
