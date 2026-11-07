# Contract: The Weedweeno's Lua API

Lua 5.2 (Cobalt), with the standard `table`, `string`, `math`, `utf8` and `coroutine` libraries. There is no `io`, `os`, `debug`, `require` or `dofile`, and `print` writes to the Weedweeno's console. All light-show functions live in the global table `bits`, and the most used are also globals.

| Function | Does |
|---|---|
| `set(id, r, g, b)` | sets every lamp with that ID to the colour, 0–255 a channel |
| `hsv(id, h, s, v)` | the same from hue (0–360), saturation (0–1) and value (0–1) |
| `off(id)` | sets it to black |
| `all(r, g, b)` | sets every lamp in range |
| `ids()` | a sorted list of the IDs in range |
| `count()` | how many IDs are in range |
| `frame()` | waits for the next tick (1/20 s); call it in every loop |
| `sleep(seconds)` | waits that long, a whole number of ticks at least one |
| `time()` | seconds since the program started |
| `tick()` | ticks since the program started |
| `bits.rgb(h, s, v)` | returns `r, g, b` for a hue, saturation and value |
| `print(...)` | adds a line to the console (the last 20 are kept) |

## Rules

- **Run:** a program runs from the top when you press Run, and again when its Weedweeno loads after a restart.
- **Frames:** a program that never calls `frame()` or `sleep()` within its tick budget is stopped with "too slow: call frame() in your loop".
- **Errors:** they stop the program, and the editor marks the line.
- **Stop:** turns off every lamp the program set.
- **Many lamps:** at most 1,024 lamps change per tick per Weedweeno. Beyond that, changes wait for the next tick.

## Example (the Chase preset)

```lua
local list = ids()
local i = 0
while true do
  for n, id in ipairs(list) do
    if n == i % #list + 1 then set(id, 255, 120, 0) else off(id) end
  end
  i = i + 1
  sleep(0.1)
end
```
