-- Rainbow: every lamp a hue, sliding along the IDs.
local speed = 60   -- degrees of hue a second

while true do
  local list = ids()
  local n = math.max(#list, 1)
  for i, id in ipairs(list) do
    hsv(id, i / n * 360 + time() * speed, 1, 1)
  end
  frame()
end
