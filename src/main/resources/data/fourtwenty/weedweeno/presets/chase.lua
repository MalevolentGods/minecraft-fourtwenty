-- Chase: one lamp lit at a time, running along the IDs.
local list = ids()
local i = 0

while true do
  for n, id in ipairs(list) do
    if n == i % math.max(#list, 1) + 1 then set(id, 255, 120, 0) else off(id) end
  end
  i = i + 1
  sleep(0.1)
end
