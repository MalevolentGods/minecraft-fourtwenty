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

## × and ÷: one sequential core

Both run MSB first, one bit per clock, 16 clocks from a Ticker (2 ms): about 32 ms, effectively instant.

- Multiply, P = A × X: `P ← 2P + (X[i] ? A : 0)` for i = 15 … 0. A carry out of 16 bits anywhere sets the error flag.
- Divide, Q = A ÷ X: `T ← 2R + A[i]; if T ≥ X: R ← T − X, Q ← 2Q + 1 else R ← T, Q ← 2Q`. X = 0 sets the error flag first.

Datapath (16-bit pairs throughout):

| Block | Parts |
|---|---|
| W1 (P or R), W2 (Q), the step counter i | Jar Nug pairs; i a 4-bit Jar + Stacker (−1) loop |
| 2·W1 (+ A[i] on the carry in when dividing) | Stacker pair |
| bit i of X, bit i of A | two Sampler Nugs per bus, the high select bit choosing between them |
| + A masked by X[i] (×) | Blend AND pair against a mask fan of X[i], then a Stacker pair |
| T − X and its borrow (÷) | Stacker pair, subtract |
| W1's next value: ×: the sum; ÷: T − X or T | staging Jars on a shared bus, released by the operation and the borrow |
| 2·W2 + (T ≥ X) | Stacker pair, the carry in |

Sequencer: `=` after × or ÷ loads W1 = W2 = 0 (staging all released) and i = 15, sets RUN; the Ticker clocks the datapath while RUN; i
wrapping past 0 clears RUN and pulses DONE, which loads X from W1 (×) or W2 (÷) through one more staging pair. The error flip-flop is
cleared by C and any load of X but DONE.

The operation register widens from one bit (+/−) to two (+, −, ×, ÷); ± and the signs apply to × and ÷ as to + and − (the result's sign
the xor of A's and X's).

## Order of work

1. Dankoder to 24 keys; Numb Nug E. Unit and game tests. (Mod change: needs a jar on server and client.)
2. Router v2 with colours and hops, proved on the v1 machine (same logic, flat board): smaller and faster to route.
3. The × / ÷ core, built and game-tested on its own (CalculatorCore tests), then joined to the keypad machine.
4. The building on the new site: the board under glass with a walkway round it, the keypad and display at the front.
