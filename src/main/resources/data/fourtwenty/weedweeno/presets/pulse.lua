-- Pulse: every lamp breathes in and out together.
local hue = 300   -- 0 red, 120 green, 240 blue

while true do
  local v = (math.sin(time() * math.pi) + 1) / 2
  for _, id in ipairs(ids()) do
    hsv(id, hue, 1, v)
  end
  frame()
end
