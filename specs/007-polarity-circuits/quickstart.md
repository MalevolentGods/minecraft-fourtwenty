# Quickstart: Polarity Circuits In Game

On flatworld, with the test jar on both the client and the server, carrying an Inspoweedometer, red and black dye, and Hemp Wicks.

1. **One loop:** a Hookah facing you, with a Snip Station facing you three blocks away. Run a red Wick from the Hookah's left (+) to the station's left, and nothing runs: the meter says "no return path". Run a black Wick from the station's right (−) to the Hookah's right. It runs at 105 W, about 4.2 Kb across it.
2. **Side by side:** lay the red and black lines touching each other the whole way. They stay separate and it still runs. Make both red and it shorts: the Hookah runs flat out into the line, and the station stops.
3. **Parallel:** three stations between the two rails. Each reads about 4.2 Kb, and the rail carries the sum of their currents.
4. **Series:** three stations chained right (−) to left (+), the end ones wired back to the Hookah. The three voltages add up to 4.2 Kb, with one current through all of them.
5. **Backwards:** turn a station around so its + is on the return. It says "wired backwards" and doesn't run.
6. **Hit Box:** a Double Hose loop into the Hit Box's front and back, then a separate Puff loop out of its left and right to a station. It runs, and the two loops share no Wick.
7. **Reference:** sneak-use the black rail with the meter, then use the red rail: it reads about 4.2 Kb from the reference.
8. **Old line:** a 0.20.0 base loads with nothing burned, and every machine says "no return path".
