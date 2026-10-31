# Research: Weed Processing and Boundless Inspiration

## 1. Who counts as high

- **Finding**: `StrainEffects.HIGHS` is set only by joints and pipes (`StrainEffects.apply`). Glass bongs (`SmokeEffects.apply`), the Hookah (`WeedBongBlockEntity`) and edibles (`DelayedEffectManager`) never touch it.
- **Decision**: Add `StrainEffects.markHigh(player, ticks)` and call it from all three routes. `isHigh(player)` reads the map. Applying a strain's aura stays with `apply`.
- **Rationale**: The plan says "any high player", and bong and edible users must count. A separate mark keeps those routes from gaining strain auras.
- **Alternatives**: A real mob effect: rejected, because it adds a visible effect and changes a working system.

## 2. Infuser power

- **Finding**: The Infuser has no block entity and no tick. Its pool is player data; `onCatch` and the Infuse action are single requests with no block position.
- **Decision**: Remember the position when a player opens an Infuser. Catching a colour and infusing a seed each check `MachinePower` once at that position: free if high nearby, else a fixed BI cost drawn from the grid touching that block, else refused with a message. The pool stays with the player, so nothing is lost.
- **Rationale**: This is what "pause and keep progress" means for a machine with no running job. Adding a block entity wouldn't reach Infusers already placed.
- **Alternatives**: Blocking the screen: rejected, because mixing the pool is the gameplay and must not change.

## 3. Grid

- **Decision**: Wicks are plain blocks with no block entity. A per-level `BIGrid` runs a breadth-first search from each source and caches its sinks, invalidated by a level grid version that bumps when a BI block is placed or removed. Sources push each tick, capped by tier, shared across sinks that have room.
- **Rationale**: No per-Wick ticking, so long runs are cheap.
- **Overload**: A source's tier is compared to each Wick and sink on its route. Anything lower burns out on the next push: block replaced with ash, smoke particles, sound.
- **Glow**: A `LIT` state is set when a Wick carries power, cleared by a scheduled tick, and written only on change.
- **Hit Box**: Sends tier N on its face and tier N-1 on the others, both ways, so it steps up or down.
- **Alternatives**: IC2-style packets per Wick: rejected as costly for no player-visible gain. Forge Energy: rejected; the plan's tiers and burnout don't fit it.

## 4. Machine power use

- **Decision**: Each machine has an internal buffer. Progress advances on a tick when free, or when the buffer covers that tick's cost. A Toke machine on a Puff line therefore runs at about half speed.
- **Rationale**: Matches the spec's assumption with no special cases.

## 5. Items through machines

- **Decision**: Four input and six output slots, sharing outputs with seeds. `WorldlyContainer` exposes top and sides to inputs and the bottom to outputs; `SidedInvWrapper` registers the item capability.
- **Rationale**: Items keep their strain as data, so different strains can't merge. Several slots stop one strain blocking another.

## 6. T1 machines

- **Decision**: The Trim Tray has no block entity: a right-click with rough buds converts one and hands over the results. The Curing Rack has eight slots, each with its own timer, drawn on the rack. Cure time counts only while the chunk is loaded.
- **Rationale**: T1 has no inventory to protect, and per-slot timers let a player top up a rack without restarting it.

## 7. Config

- **Decision**: `ProcessingConfig` is a SERVER config (`ModConfigSpec`, as `DispensaryConfig`) with `overrideTuning` for game tests. Registered in `FourTwenty.java`, not in the MineColonies path.

## 8. Existing blocks

- **Decision**: `weed_bong` keeps its name and job. New Hookahs use the plan's names.
