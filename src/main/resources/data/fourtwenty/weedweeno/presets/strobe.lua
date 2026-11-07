-- Strobe: everything flashes white.
local on = false

while true do
  on = not on
  if on then all(255, 255, 255) else all(0, 0, 0) end
  sleep(0.1)
end
