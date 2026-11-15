# Research: Lifted Body

## R1. Taking over every walk

**Finding:** every MineColonies citizen walk goes through its navigator, `MinecoloniesAdvancedPathNavigate`: `EntityNavigationUtils.walkToPos / walkToBuilding / walkToPosInBuilding / walkCloseToXNearY` and the AI states call `walkTo(...)`, `moveTo(...)` or `setPathJob(job, destination, speed, safeDestination)`, then decide arrival themselves by distance to the target. `isDone()` reports whether the navigator is still moving; `tick()` steps the path.

**Decision:** `NavigatorTakeoverMixin` on the citizen navigator, active only while the citizen is a Lifted Builder allowed to fly:
- `setPathJob` HEAD: record the destination as her **errand** (`LiftedFlight.errand(citizen, destination, purpose)`), start or retarget the flight, and return a path result that reads as "in progress" without computing a walking path.
- `isDone()`: false while the errand flight is under way; true once she's there (within the same distance MineColonies uses: `EntityNavigationUtils.REACHED_DIST`, or 1.5 blocks).
- `tick()` and `moveTo(...)`: cancelled while an errand flies (our flight moves her).
- Spec 013's build-site approach (`walkToConstructionSite`) stays as it is; it becomes one more source of errands.

**Rationale:** one hook sees job, unload, fetch, meal, bed and hospital walks alike, including states MineColonies adds later.
**Alternatives:** a mixin per AI state (fragile, misses new states); replacing the navigator class (MineColonies constructs it internally per citizen; too invasive).
**To prove first (P1 task 1):** what `setPathJob` callers do with the returned `PathResult`, and that a result in "computing/in progress" makes them wait rather than retry each tick.

## R2. Doors and fence gates

**Decision:**
- `FlightPlanner` treats a cell holding a **wooden door or fence gate** (the ones MineColonies' walking opens) as passable at a small extra cost, and reports which route cells are doorways.
- `Doorways`: as she reaches a doorway cell on her route (within 1.5 blocks), open it (door and its other half, or gate) if closed, and remember it; once she's 2 blocks past and nothing else is in it, close what she opened. Never anything that isn't a door or gate (FR-004); never a door needing redstone (iron doors) or one that's locked (modded).
- A doorway is approached at its height (doors are 2 tall): flying through at walking height, upright.

**Rationale:** mirrors what walking citizens do, and keeps players' doors as they left them.
**Alternatives:** teleporting through doors (looks wrong); leaving doors open (players hate it).

## R3. Breaks: restaurant, bed, hospital

**Finding:** eating: the citizen walks to the restaurant and sits (rides a seat entity). Sleep: walks to bed, then the sleep handler sets her into the bed. Hospital: walks to the hospital bed.
**Decision:** all three arrive through R1 like any errand. The flight ends with a landing at the destination (not mid-air); MineColonies then seats her or puts her to bed itself. While she rides a seat or sleeps, flight lets go (`isPassenger()` / sleeping ends any flight).
**To prove:** that her high staying "paused" through breaks (spec 013) is unchanged, and the flight lands where the sleep/seat code expects.

## R4. Stuck errands

**Decision:** one rule for every errand (pure `Errand`): progress = getting closer to the destination by at least 2 blocks within 30 s; otherwise **recall** her (MineColonies' own teleport to her hut, or to the errand's building for breaks). Replaces spec 013's separate stranded check and walk-in fallback (the walk-in becomes a door route).

## R5. The Lifted model

**Decision:** our own model built in code as a `ModelPart` tree (registered through `EntityRenderersEvent.RegisterLayerDefinitions`), stylised and smoother than the citizen: torso in two parts (spine bend), neck and head, upper and lower arms (elbows), upper and lower legs (knees), hands that hold props. Its exact proportions come from the design pass (R8).
**Rendering:** MineColonies' citizen renderer still runs (name, status bubble, interaction icons), with its body parts and held-item layers hidden while she's Lifted (`CitizenModelHideMixin`); `LiftedRenderer` draws our model in the same frame (`RenderLivingEvent.Post`), reusing spec 014's dance transform and adding the boombox, glow, smoke and held props.
**Fallback (FR-012):** any exception while drawing our model, a missing texture, or the player's toggle off → she's drawn MineColonies' way (with 0.26.0's boombox, glow and dance).
**Alternatives:** cancelling MineColonies' render (lose name/status icons); swapping MineColonies' model class (its layers assume a humanoid model).

## R6. Per-Builder tint

**Decision:**
- The painted sheet is imported into a **base** texture (shading and fixed details) plus **masks** for hair, skin and two clothing colours (`tools/import_lifted_sheet.py`).
- `SkinPalette` (pure) reads a Builder's MineColonies skin (the standard 64x64 layout): hair from the top and back of the head, skin from the face, clothes from the torso front and the legs, each the dominant colour of its region.
- `LiftedSkins` (client) composes one texture per distinct skin (base × tinted masks), cached by skin and released when unused.
**Rationale:** one painted design, every Builder recognisable (clarification Q3), works with any skin, including other mods'.

## R7. Poses

**Decision:** `LiftedPose` (pure) blends named poses into joint angles: **cruise** (reclined, arm behind head, a nod on the beat: clarification Q4), **hover** (upright), **smoke** (joint to mouth between puffs), and the **dance** (spec 014's routines, now able to bend elbows, knees and spine). It eases between them over 0.3-0.5 s by what she's doing (flying, hovering to place, smoking, dancing), so there's never a jump.

## R8. The design pass

**Decision:** as with the boombox:
1. `tools/render_lifted_concepts.py` renders three silhouettes in Blender (for example: lanky and loose, chunky and cute, sleek and tall), front, back and side, in the cruise and hover poses.
2. The user picks one.
3. A texture-sheet prompt with the model's UV panels laid out on magenta (one generation).
4. `tools/import_lifted_sheet.py` cuts it, builds base and masks, and writes them at sizes in multiples of 16.

## R9. Switches

**Decision:**
- Server, in `fourtwenty-builders-server.toml`: `bodyFlight` (errands, doors, breaks; off = 0.26.0's movement) and `liftedLook` (off = 0.26.0's look for everyone).
- Client: **"Lifted look"** on/off in the pause menu beside the Midnight Machine volume, in `fourtwenty-midnight-machine-client.toml` (name pending the user's approval).

## R10. Performance

**Decision:** errands reuse the existing planner (legs of ~20 blocks, 4000 cells) and plan only when the destination changes or a leg is done. Doorway checks only run for cells on the route. The model is one draw per Builder; tinted textures are built once per skin. SC-006 is measured with 3 Lifted Builders on flatworld.
