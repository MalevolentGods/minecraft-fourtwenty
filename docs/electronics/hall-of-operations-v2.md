# Hall of Operations v2: the calculator as a flat circuit board

The user's brief (2026-11-08): rebuild the calculator from scratch on a new site beside the current Hall of Operations, as a **flat circuit
board**: every chip on one floor, every signal a coloured trace on that same level, crossings made with **Nah, Skip Me, Bro** hops,
readable from a gallery or through the glass roof. Add **× and ÷**. The current Hall of Operations stays as it is.

Decisions (user, 2026-11-08):

- Keypad 4×5, one board: `7 8 9 ÷ C / 4 5 6 × ← / 1 2 3 − ± / · 0 · + =` (two blank trays either side of 0).
- A result that can't be shown (× past 65,535, ÷ by zero) shows **E** until C.
- ÷ gives the quotient; the remainder is dropped.

## What stays from v1

Entries to 9999 (a fifth digit refused), + and − to 19998, signed entries (±), back (←), clear (C), the 5-digit display with its minus.
The register-and-staging design (X, A, P; staging registers that let go of a shared bus unless their key was pressed; CLK1 then CLK2
from the delayed key-down pulse) carries over.

## New parts and part changes

1. **Dankoder, up to 24 keys** (a 74C923 does 20): a 5-bit code on channels 0-4. Board mode reads up to 24 keys in reading order; the
   Ribbon inputs stay 16. Codes under 16 are unchanged, so v1 keeps working.
2. **Numb Nug error**: reading 16 bits, the low byte's carry wire (unread until now) means error: the row shows `E`. (The high byte's carry
   wire is already the sign.)
3. **Router v2** (`tools/build_calculator2.py`):
   - Each single-wire signal gets a thread colour (not red or black, which are + and − on BI blocks); wires of different colours may
     run side by side. Pins and Roach Clip taps still join any colour, so those stay apart.
   - A trace may cross another's straight run through a hop, entering and leaving along one axis; the crossed run must be straight
     there and not touch a pin. Ribbons hop Ribbons and wires alike.
   - The board is planar: traces on the chips' own level, VDD a level above (risers), GND the floor below.
   - The check rebuilds every net as the game does, hops and colours included.

## × and ÷: two big chips (user, 2026-11-08)

The sequential core (16 clocked steps, about 110 more chips) was dropped for two new parts, as real calculators use arithmetic chips:

- **Manyfier** (a 16 × 16-bit multiplier): A × B, the 32-bit product on two bus pairs, the low byte's carry high past 65,535.
- **Fewinator** (a 16-bit divider): A ÷ B, quotient and remainder, the low byte's carry high on ÷ 0.

Both stand two blocks tall (`BigChipBlock`): a bus face on every side of each block, the lower block the low byte and the upper the high;
A at the back, B on the left, the result in front, its second half on the right; VDD on the upper block's top, GND under the lower.

In the calculator: the Manyfier's and Fewinator's front pairs (A op X) go through staging Jar pairs onto the S bus beside the
adder's, each released unless its operation is chosen, so `=` loads X from the one chosen. Their flags, and the adder's carry out of 16
bits (X can be up to 65,535 after a ×), set an error flip-flop on `=`; it drives the display's low-byte carry (`E`) until C. The result's
sign is the xor of A's and X's. The router places a big chip as two stacked cells (its upper block on the bridge layer).

The operation register widens from one bit (+/−) to two (+, −, ×, ÷); ± and the signs apply to × and ÷ as to + and −.

## Order of work

1. Dankoder to 24 keys; Numb Nug E. Unit and game tests. (Mod change: needs a jar on server and client.)
2. Router v2 with colours and hops, proved on the v1 machine (same logic, flat board): smaller and faster to route.
3. The Manyfier and Fewinator (done, 8bcc6a09), then joined to the keypad machine: 4×5 keypad, two-bit operation, error flip-flop.
4. The building on the new site: the board under glass with a walkway round it, the keypad and display at the front.
