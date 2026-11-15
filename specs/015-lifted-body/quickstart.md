# Quickstart: checking Lifted Body in game

On flatworld, with a Builder's hut, a Warehouse at level 4 (closed, with its centre stairs), a restaurant and residences. Builder logging on (`logBuilderPathing = true`) while testing; back off after.

## M1. Every trip flies (Stories 1-2)

1. Give the Builder joints and the Warehouse 4→5 upgrade. She lights up at her hut and flies to it.
2. Fill her inventory (or wait): she flies home, unloads, flies back. No landing on the roof, no walking.
3. Empty her hut of a material she needs: she flies home for it when it arrives.
4. Watch her work inside: she flies to a door, opens it, goes in, closes it; moves between floors up the stairwell; leaves the same way. Every door ends closed.
5. Let her get hungry and wait for night: she flies to the restaurant and in, eats, flies back; at night she flies home to bed.
6. Turn "Fly when Lifted" off mid-flight: she lands gently and walks.
7. **Pass:** the log shows no `stranded`, `recalled` or `rescue` lines over the whole upgrade (SC-001), and no walking while Lifted outside her hut (SC-002).

## M2. The design pass

1. Concept renders of three silhouettes; the user picks one.
2. The user generates the texture sheet from the prompt; import it.

## M3. The look (Story 3)

1. Light her up: she changes to the Lifted model, in her own colours, with the boombox and glow; her name and status bubble still show.
2. Watch her cruise (reclined, arm behind head, nodding), ease upright to build, smoke with the joint at her mouth, and dance with bending elbows and knees.
3. Two Builders with different skins: same design, their own colours.
4. Toggle "Lifted look" off in the pause menu: you see 0.26.0's look; another player still sees the new one.
5. Time a stretch of building with the look on and off (SC-004).
6. Three Lifted Builders: check the server's tick time against the same colony sober (SC-006).
