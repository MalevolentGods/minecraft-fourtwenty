# Quickstart: Lit Bits In Game

On flatworld, with the test jar on both sides.

1. **A strip:** run red and black Hemp rails from a Hookah, 30 blocks long. With the Sticky Finger, its next ID set to 1, place 30 Lit Bits between the rails: IDs 1 to 30. Each shows its ID as it's placed.
2. **Rainbow:** place a Weedweeno on the same rails, open it, pick Rainbow, press Run. The colours sweep along the strip, and the server stays at 20 TPS (check with `/neoforge tps`).
3. **Your own show:** write `set(1, 255, 0, 0) set(2, 0, 0, 255) while true do frame() end` and run it. Lamp 1 turns red and lamp 2 blue.
4. **A bad loop:** `while true do end` is stopped within a tick with "too slow: call frame() in your loop". The server doesn't lag.
5. **An error:** a typo on line 3 is marked on line 3.
6. **LED behaviour:**
   - The Inspoweedometer on a lamp at full white reads three times its current at full red.
   - Take the Hookah's charcoal out and let the Stash run low: the lamps dim together.
   - A lamp turned the wrong way stays dark and the meter says "wired backwards".
7. **IDs:**
   - Break lamp 12 and place a new Lit Bit in its spot: it's 12 again.
   - Open lamp 5 and change it to 7: lamps 5 and 7 now move together.
8. **Bit Chit:** export the show to a Bit Chit, copy it with a blank one, and load it into a second Weedweeno. Same show.
9. **Restart:** restart the server, and the running show comes back.
