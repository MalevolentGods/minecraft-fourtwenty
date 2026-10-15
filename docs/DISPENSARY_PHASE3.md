# Dispensary Phase 3 handoff

Date: 2026-10-14  
Status: Collection and smoking behavior implemented; packaged native verification passed. Interactive client observation, ordinary dedicated-server acceptance, full-pack qualification and performance remain unverified.

## Delivered behavior

Eligible adults autonomously interrupt ordinary work or idle activity to collect and consume one joint, then return to native citizen AI. Eligibility still uses the assigned home's completed level, including level-three guard housing. Children and adults in lower-level residences remain excluded. Carried joints are used first, without requiring another dispensary trip.

The dedicated `CitizenSmokingTask` follows the pinned Dining Hall pattern: travel, wait at the counter, receive a joint, approach a seat, smoke, and finish. It uses native building lookup, pathfinding, inventory transfer and sitting. A stocked, staffed dispensary is preferred; an unstaffed stocked building can provide self-service. Multiple dispensaries retain independent stock and queues.

A working Budtender serves waiting customers at the counter between crafting actions. Customer eligibility, demand, colony, proximity, personal stock and inventory capacity are rechecked at handoff. Self-service starts after bounded waiting, immediately for an unstaffed hut, or immediately for the Budtender collecting from their own hut. Self-service reads the hut's combined inventory capability: the pinned `getHandlers()` API is unsuitable because it returns no handlers without staff and includes worker inventories even when those workers are elsewhere.

Collection and consumption are separate transactions. Receiving a joint does not grant credit. The smoking task displays the held joint, swings the hand and sends smoke particles during a timed action. Only completion extracts exactly one actual inventory item and records the persisted demand revision. Repeated completion is rejected. Interrupted smoking leaves transferred or previously carried joints intact and grants no credit. Player joint effects and the joint recipe are unchanged; citizen consumption never calls food handlers or player item-use effects.

## Bounds and cleanup

| Behavior | Simulation ticks |
| --- | ---: |
| Travel limit | 600 |
| Staff-service wait before self-service | 200 |
| Seat approach limit | 100 |
| Smoking action | 100 |
| Retry delay after failure or interruption | 1,200 |
| Failed building excluded from selection | 3,600 |
| Temporary customer/seat lease | 40, renewed while active |

These are service defaults, independent of the existing configurable demand/grace/health durations. Expiry uses Phase 1's persisted simulation clock, rather than daylight or wall time. Temporary visits and retries are not saved; loaded citizens select a fresh task against durable demand and their real inventory. A gap in task execution cancels the action instead of completing smoking while the citizen was inactive.

Seats use `sit_in`, `sit` and, in dry weather, `sit_out` tags. Reservations prevent competing customers from selecting the same seat; native occupied seats are excluded. Missing, occupied or unreachable seats fall back to the citizen's current safe standing location near the counter. Unsafe locations cause a retry instead of consumption. The task never teleports citizens or forces chunks; native sitting handles mounting and dismounting.

The mixin only substitutes ordinary native `WORK`/`IDLE` decisions. Eating, medical care, sleep, mourning and foreign states remain in control. Additional checks protect guards against visiting during combat, illness, hunger, sleep or raids. Kingdom's Gate's ritual state is preserved. An outer native decision hook also cleans up when a third-party HEAD injection bypasses the normal return hook. Interruption stops owned navigation, clears the joint visual, dismounts an owned seat and releases reservations. Entity removal performs the same cleanup; expired leases cannot retain unloaded customers.

Native colony IDs and their derived citizen UUIDs can be reused after colony deletion. The integration now subscribes to the native colony-deleted event and removes that colony's demand records, preventing new citizens from inheriting an old colony's consumption credit. No existing save schema or identifiers changed.

## Evidence

The [evidence directory](https://github.com/MalevolentGods/minecraft-fourtwenty/tree/d6a5609ee2b8ba5dc978dd6fa4a83fe2b0b623ce/validation/evidence/dispensary-phase3) records the baseline, artifact hash, build and native logs, JUnit results and launch manifests. All 135 locked installed artifacts still match; the installed directory also contains the older FourTwenty JAR, for 136 total. Kingdom's Gate remains clean at `e2f7c3884a3b40fb0aee48fe2d2ec0dd024faf34`. Pins were preserved and only disposable isolated profiles were used.

Packaged artifact: `build/libs/fourtwenty-0.3.2.jar`  
SHA-256: `659716f2976676ef61d9bc78fa6efc5921485a3801e668487cfe7c3f9dabd527`

| Evidence | Result |
| --- | --- |
| Build and JUnit | `build compileColonytestJava verifyCompatibility` passed; 18 existing JUnit tests passed |
| Python tools | All 6 existing tests passed |
| Dependency checks | 149 resolved mod/platform IDs, 331 checks, 44 nested artifact groups; compile API checksums passed |
| Packaged FourTwenty colony suite | All 37 required tests passed with MineColonies and Kingdom's Gate loaded |
| New service fixtures | All 8 passed, covering transactions, autonomous visits, interruption, fallback, removal, priorities/seats, unreachable retry and Budtender self-service |
| Packaged combined suite | All 46 required tests passed, including all 9 unchanged Kingdom's Gate fixtures |
| Packaged standalone suite | All 17 required tests passed without MineColonies |
| Packaged clients, standalone and colony | Mod loading and resource reload completed, including sound, textures and animation loading |
| Interactive gameplay, ordinary dedicated server, full installed pack, performance, hosted CI, release | Not qualified |

The autonomous visit fixture observes actual employed, unemployed and guard entities navigate, receive stock, use native seating, smoke, consume one joint each, and leave the visit state. It also verifies that a Budtender serves before the self-service deadline, and that children and level-two residents remain excluded. The original courier-to-Budtender production fixture still passes with exact conservation of 16 buds and 32 paper into 16 joints.

Focused fixtures verify contention for the last joint, full inventories, no staff, no seats, an empty nearer hut, native seat occupancy and reservations, hut removal after transfer, interrupted smoking, personal stock, repeated completion, residence downgrade, child and remote-handoff rejection, and an unreachable stocked hut followed by successful service at another hut after the retry delay. Hunger is injected during a real autonomous smoking action and the fixture observes cleanup, retained inventory, a bounded retry and a fresh successful action. Priority tests additionally verify native eating/medical/sleep decisions and a foreign state remain unchanged. The combined sibling suite exercises native ritual entry, raid cancellation and return behavior; it does not establish an interactive dispensary-to-ritual crossover in a player session.

The earlier Phase 2 combined-suite failures did not reproduce in this final 46-test run. Their underlying cause has not been established; retain the earlier evidence as historical rather than claiming this change specifically fixed them.

GameTest fixtures load their disposable area and set deterministic daytime/weather. Some fixtures create buildings directly and supply initial stock. These are native AI and item-conservation observations, not player-built colony, real-client visual or ordinary-server acceptance. The desktop automation inventory did not expose the development Java window, and selecting `java` returned `Invalid app`. Client logs establish startup/resource loading only. Both test clients were deliberately terminated afterward; their termination codes are not clean-exit evidence. No installed mods, player worlds or sibling source were changed. No commit, publication or release was performed.

## Reproduction

Use Java 21 and the repository-local Gradle cache as described in [DEVELOPMENT.md](DEVELOPMENT.md). Use a fresh run directory for each invocation.

```sh
./gradlew build compileColonytestJava verifyCompatibility
python3 -m unittest discover -s tools/tests -v
python3 tools/compatibility.py prepare --profile colony \
  --mods .compat/dispensary-phase1-reference \
  --destination .compat/runs/dispensary-phase3-new \
  --cult-jar .compat/cult-source/build/libs/minecolonies-cult-0.2.0.jar \
  --fourtwenty-jar build/libs/fourtwenty-0.3.2.jar
./gradlew runGameTestServer -Ppackaged=true -PcompatProfile=colony \
  -PcompatRunDir=.compat/runs/dispensary-phase3-new
```

The default combined namespace list now includes `fourtwenty_servicetests`. Use `-PcompatTestNamespaces=fourtwenty_servicetests` for the eight focused fixtures; explicitly label that result as a subset.

## Changed files and next phase

Production changes are the new `compat/minecolonies/service` task/transaction classes and entity interface; the gated `CitizenAIMixin` and `CitizenServiceMixin`; building-owned temporary queues/seats; Budtender service; colony-deletion cleanup; and localized retry text. The mixin manifest enables only the optional hooks. Gradle adds the service test namespace. `DispensaryServiceGameTests` and its disposable structure supply the new native checks. Existing crops, bong behavior, edible exclusion, player effects, recipes, production targets and blueprint assets remain unchanged.

Phase 4 should use the existing completed-consumption record and revision for happiness and the non-stacking +2 maximum-health modifier. This phase records satisfaction and health entitlement deadlines under the established Phase 1 contract but does not yet install either native benefit. Visual/client qualification of smoking, live ritual crossover and comprehensive restart/unload testing remain explicit follow-up evidence, alongside the later blueprint and compatibility phases.
