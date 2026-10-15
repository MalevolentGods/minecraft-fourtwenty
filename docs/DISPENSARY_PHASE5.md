# Dispensary Phase 5 handoff

Date: 2026-10-14

Status: Implementation delivered. Native construction, upgrades, repairs, and all-level autonomous/rain observations remain unqualified; the phase acceptance checkbox stays open. Automated placement and UI probes are identified separately below.

## Delivered behavior and assets

Five deterministic timber-and-stone shops share the same world-relative hut and entrance. Roofs use spruce stairs with a slab ridge, framed glazed gables and indoor lighting. Levels 3–5 divide preparation/storage from the lounge. Each seat has an accessible aisle approach and roof coverage. Native racks provide colony stock; the original barrel remains general-purpose storage and is not included in native service inventory.

| Level | X × Y × Z | Local hut anchor | Indoor seats | Stock target | Anchor-relative X/Z bounds |
| --- | --- | --- | ---: | ---: | --- |
| 1 | 9 × 10 × 11 | (4,1,3) | 2 | 16 | X -4…4; Z -3…7 |
| 2 | 11 × 11 × 14 | (5,1,3) | 4 | 32 | X -5…5; Z -3…10 |
| 3 | 13 × 12 × 17 | (6,1,3) | 6 | 64 | X -6…6; Z -3…13 |
| 4 | 15 × 13 × 20 | (7,1,3) | 8 | 96 | X -7…7; Z -3…16 |
| 5 | 17 × 14 × 23 | (8,1,3) | 10 | 128 | X -8…8; Z -3…19 |

Reserve the full inclusive anchor-relative volume `(-8,-1,-3)` to `(8,12,19)` before rotation/mirroring. No upgrade moves the hut or entrance. Stable resources are `blueprints/fourtwenty/dispensary/dispensary/dispensary1.blueprint` through `dispensary5.blueprint`. Pack identity remains `FourTwenty`; the pack-relative paths are `dispensary/dispensaryN.blueprint`.

Player selector sequence: **Build Tool → Switch Pack → FourTwenty → Dispensary → dispensary1**, then use the level selector for previews. Construction, upgrades and repairs use the existing management controls. Native client pack discovery/lookup and blueprint loading are tested; manual interaction with every Build Tool selector and builder work order remains an observation item.

The new **Dispensary service** module retains the standard management, worker, crafting, statistics, and minimum-stock views. It reports completed level, assigned Budtender, buds, paper, total joints, target, counter availability and waiting customers. Operating explanations independently report unfinished construction, no staff, staff away from the counter, insufficient buds/paper, empty counter stock and outstanding joint production requests. These are factual diagnostics, not inferred pathfinding failures.

The server combines the hut/rack capability once and adds each assigned worker inventory once, even for a remote or unloaded worker. Only worker stock at the counter is immediately available. Unstaffed hut stock remains visible. Native rack registration becomes visible after the tile's normal capability-refresh tick. Customer leases expire against the saved simulation clock. The read-only snapshot is checked every 100 simulation ticks; changed snapshots mark the native module dirty, and initial view requests serialize current state. There are no new persistent fields or custom client mutation packets.

The hut recipe and item identity are unchanged. New pixel assets provide a leaf sign, timber/stone block faces, preparation surface, pack/status icon and a green-apron Budtender skin using native male/female citizen models. Model initialization is client-only and gated on MineColonies. Pixel sources and blueprint authoring remain reproducible with the two Python generators.

A real staffed-client probe exposed an existing registration defect: `BudtenderJob` extends `AbstractJobCrafter`, whose packet includes a request-data-store token, but the job was registered with `DefaultJobView`. That reader left the token in the buffer and corrupted the following citizen fields. Registration now uses native `CrafterJobView`. The new regression check consumes a real job packet through the registered factory and verifies that the following sentinel and buffer boundary remain intact. Existing saves and job IDs are unchanged.

A longer staffed-client run also exposed the missing native voice lookup for the custom `budtender` job. MineColonies indexes voice events by job path without a missing-entry fallback, so ambient speech or a player greeting could throw a null-pointer exception. Optional common setup now maps Budtender to the existing generic citizen voice set, preserving all other jobs. A native fixture invokes every event lookup for both genders and all four voice profiles. No new sound assets or registry IDs are introduced.

## Evidence and limits

Evidence directory: [dispensary-phase5](https://github.com/MalevolentGods/minecraft-fourtwenty/tree/d6a5609ee2b8ba5dc978dd6fa4a83fe2b0b623ce/validation/evidence/dispensary-phase5). The live installed pack contains all 135 locked artifacts unchanged plus `fourtwenty-0.3.2.jar` (136 total). A direct exact-inventory verification rejects that additional under-test artifact; focused profiles were prepared from the existing verified `.compat/dispensary-phase1-reference` copy without changing the lock or installation. Kingdom's Gate was clean at `e2f7c3884a3b40fb0aee48fe2d2ec0dd024faf34`.

Original Phase 5 packaged artifact (before the status-refresh review fix below): `build/libs/fourtwenty-0.3.2.jar`  
SHA-256: `3859e66fffb3656b45a91be6497e3377f663bd4ea89dfe0cab8843d0dc1313ea`

| Evidence | Result |
| --- | --- |
| Build, unit tests, native fixture compilation | Passed; 18 JUnit tests |
| Python tooling | 8 tests passed, including reproducible bytes, footprint/seating progression and rejected obstructed/unroofed layouts |
| Dependency verification | Passed: 149 resolved IDs, 331 checks, 44 nested artifact groups |
| Final packaged colony suite | 53 required tests passed (`final-colony-isolated`), including prior production, logistics, service, benefits and Kingdom's Gate regressions |
| Blueprint transforms | All five levels across all eight native `RotationMirror` values: 40 combinations; anchors, transformed tags, headroom, roof coverage and connected approaches passed |
| Native status/job packets | Passed; unstaffed storage, local/remote worker distinction, rack accounting, separate huts, pending requests, expired leases and exact packet consumption |
| Final packaged standalone suite | All 17 required tests passed (`final-standalone`) |
| Standalone packaged client | Reached the title screen without MineColonies; positive startup marker and normal exit (`final-standalone-client`) |
| Packaged colony client | Positive probe marker and normal exit (`final-client`); five native pack lookups, model registration, resource reload, five rendered levels, complete live crafter views, retained management tabs and live 0→9 joint synchronization |
| Native builder construction, upgrades and repairs | Not observed; direct fixture placement does not satisfy these criteria |
| Every-level autonomous customer/worker access, occupied seats and rain | Not qualified; existing level-one AI fixtures pass, static all-level accessibility checks pass |
| Ordinary dedicated-server gameplay, full installed pack, performance, hosted CI, release | Not qualified; GameTestServer startup does not establish these results |

Final screenshots are in [final-client/screenshots](https://github.com/MalevolentGods/minecraft-fourtwenty/tree/d6a5609ee2b8ba5dc978dd6fa4a83fe2b0b623ce/validation/evidence/dispensary-phase5/final-client/screenshots). Visual inspection confirmed all five exterior renders, the level-five partition/lounge and green-apron worker, readable wrapped staff names, scrolling problem rows and standard management tabs. [Live stock UI](https://github.com/MalevolentGods/minecraft-fourtwenty/blob/d6a5609ee2b8ba5dc978dd6fa4a83fe2b0b623ce/validation/evidence/dispensary-phase5/final-client/screenshots/status-live-stocked.png) and [lounge/worker](https://github.com/MalevolentGods/minecraft-fourtwenty/blob/d6a5609ee2b8ba5dc978dd6fa4a83fe2b0b623ce/validation/evidence/dispensary-phase5/final-client/screenshots/dispensary-lounge-and-budtender.png) show the final artifact. Gallery structures are directly placed above the disposable world's terrain, not builder constructions. The live view changed from 0 to 9 joints after server-side fixture insertion and normal native synchronization. Smoke animation and actual Build Tool clicks were not visually inspected in this run.

The final gallery log contains initial `null1.blueprint` lookups from native building creation before fixture pack/path assignment; subsequent explicit FourTwenty lookups all passed. This is retained as a fixture limitation, not evidence of successful native builder work orders. No citizen-view decode exceptions appear in the verified client log.

Diagnostic history is retained. `client-diagnostic` records the first test-only resource lookup failure (the test module cannot read another mod's class resources); it was corrected to use `DispensaryBlock` as the resource owner. `rack-fixture-diagnostic` checked a newly registered rack before the native tile refresh and was corrected to exercise that refresh. `client-crafter-diagnostic` contains the genuine citizen-view decoding failure that led to the crafter-view registration fix. Earlier client probe success markers did not assert complete live citizen decoding; the final probe does. `client-voice-diagnostic` preserves the later greeting crash and prompted the native voice fallback and exhaustive lookup fixture. Final validation rejects native voice or citizen-view decoding exceptions as well as requiring positive probe markers. Two diagnostic combined runs (`final-colony` and `final-colony-retry`) with a separate voice-test batch failed pre-existing ritual/book timing checks; one also failed the logistics movement observation. The voice lookup check was moved into the existing status fixture, avoiding an extra colony/test batch and its change to subsequent world timing and placement. All voice cases remain covered, no assertions were removed, and the complete 53-test suite passed in `final-colony-isolated`. The sibling source was not changed. These diagnostics remain available rather than being overwritten.

No player worlds, installed JARs, dependency pins, sibling source, commits or releases were changed. Remaining native construction/gameplay observations must use disposable worlds and be recorded before checking off full Phase 5 acceptance. Phase 6 remains separate.

## Status-refresh review fix — 2026-10-15

Status refresh now runs from the existing server-level listener every 100 ticks of the saved simulation clock. The old world-time modulo check inside MineColonies' staggered slow-colony callback could delay updates for minutes. Loaded huts still mark the native building view dirty only when their snapshots change.

The new event-bus regression failed before the fix and passes after it. It dispatches server-level ticks while world game time stays fixed, then verifies worker departure/return, queue expiry, native status packets and unchanged-snapshot suppression without inventory writes. Build and all 18 JUnit tests passed; the focused `fourtwenty_colonytests` source-based profile passed all 13 required tests. Commands, logs, source hashes and the rebuilt artifact hash are in [status-refresh evidence](https://github.com/MalevolentGods/minecraft-fourtwenty/blob/d6a5609ee2b8ba5dc978dd6fa4a83fe2b0b623ce/validation/evidence/dispensary-phase5/status-refresh/summary.json). The earlier complete packaged suite and client observations above apply to the original artifact; they were not rerun for this fix.

## Reproduction

Use the actual cached Java 21 installation, not the local Homebrew `openjdk@21` alias, which resolved to Java 22 during this run:

```sh
export JAVA_HOME=/Users/oberon/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.12.1+1/Contents/Home
export GRADLE_USER_HOME="$PWD/.gradle-home"
python3 tools/generate_dispensary.py
python3 tools/generate_dispensary_assets.py
python3 -m unittest discover -s tools/tests -v
./gradlew build compileColonytestJava verifyCompatibility
python3 tools/compatibility.py prepare --profile colony \
  --mods .compat/dispensary-phase1-reference \
  --destination .compat/runs/dispensary-phase5-new \
  --cult-jar .compat/cult-source/build/libs/minecolonies-cult-0.2.0.jar \
  --fourtwenty-jar build/libs/fourtwenty-0.3.2.jar
./gradlew runGameTestServer -Ppackaged=true -PcompatProfile=colony \
  -PcompatRunDir=.compat/runs/dispensary-phase5-new
```

Prepare another fresh colony profile for the client. Copy only the disposable test world's `world` directory to its `saves/benefits`, then launch:

```sh
./gradlew runClient -PpresentationClientProbe=true -Ppackaged=true \
  -PcompatProfile=colony -PcompatRunDir=.compat/runs/dispensary-phase5-client-new
```

The explicit probe quick-loads the isolated world, renders synthetic status scenarios, reloads resources, looks up all five native pack paths, directly places a gallery, creates test-assigned workers, and checks live citizen/building views and stock synchronization. Screenshots are saved under that run's `screenshots`. Require the positive `FOURTWENTY_PRESENTATION_CLIENT_PROBE_PASSED` marker and inspect errors and images; process exit alone is insufficient. Fixture workers demonstrate rendering and synchronization; this probe does not qualify natural navigation or builder construction.

## Changed areas

Production changes: the dispensary status snapshot/module and optional client window/model package, registry/job-view wiring, client setup gate, queue-count accessor, block/skin/icon assets, localization and five blueprints. Tooling: deterministic blueprint/pixel generators, Python geometry checks, native presentation fixtures, opt-in client probe and Gradle flag. Player instructions are in README; authoring/protocol details are in DEVELOPMENT. Existing joint recipes, demand and benefit rules, player effects, crops, bongs and saved-state schema are unchanged.
