# Dispensary Phase 2 handoff

Date: 2026-10-14. Scope: optional Dispensary registration, level-one prototype, Budtender employment, native joint crafting and replenishment. Citizen collection/smoking and happiness/health effects remain Phases 3–4. Finished five-level art and service UI remain Phase 5.

## Delivered behavior

- MineColonies presence gates the block, hut item, block entity, building entry, job and module registration. The stable IDs are `fourtwenty:dispensary` and `fourtwenty:budtender`. Standalone FourTwenty does not register these optional objects; the hut recipe and loot table have mod-loaded conditions.
- The building uses native construction/repair management, hiring and dismissal, inventory, request controls, statistics, minimum stock, crafting views and synchronization. Completed level zero has no worker slot; levels 1–5 have one Budtender. Multiple dispensaries have distinct staff, inventory and building-owned request trees.
- The hut exposes the native combined inventory through `Capabilities.ItemHandler.BLOCK` on every side. Native worker pickup and couriers can reach its hut storage and registered containers.
- The standard native crafter AI navigates to the hut and tagged work position, obtains ingredients, crafts, and deposits output. It retains ordinary citizen hunger, illness, sleep and other priority handling. Stock requests are asynchronous building requests; the Budtender never waits for personal cannabis service in this phase. Phase 3 must support self-collection without waiting on the worker's own service queue.
- The existing `PWP` joint recipe is automatically taught at completed level one, after checking the live recipe manager for the expected inputs/output. The module supports native small and large crafting, including its actual three-column grid. One joint costs exactly one bud and two paper. Recipe improvement is disabled for this module, so skill/research cannot discount these inputs. The existing player recipe is unchanged.
- A hut requests up to 16 additional joints per batch toward completed-level targets **16/32/64/96/128**. Counts include both building storage and the assigned worker's carried output. Its native crafting request creates native ingredient subrequests; warehouse supplies are delivered by native couriers. An outstanding joint request prevents another stock request, including after native building NBT reload. Requests never use an invented currency, timer-based production or injected ingredients.
- Courier pickup retains the joint target plus at least 16 buds and 32 paper. Native request reservations and player minimum-stock requirements are merged using the larger retention amount. Worker dumping is separate from building retention.
- The pinned native crafting/dumping code can lose an insertion remainder if capacity is insufficient. Budtenders simulate output capacity before consuming ingredients and conservatively reserve enough storage for their carried inventory before native dumping. Full storage keeps output carried and retries; essential citizen AI still preempts the job. Freeing adequate storage allows dumping to resume. Completed native requests can leave a batch carried; the explicit dump retry returns it to service stock instead of leaving the worker idle with all the joints.

## Blueprint and player access

Build Tool → **Switch Pack → FourTwenty → Dispensary → `dispensary1`**. The stable resource is `blueprints/fourtwenty/dispensary/dispensary/dispensary1.blueprint`. The first `dispensary` directory is the pack root; the second is its category.

The original prototype is **9 × 7 × 11 blocks**, with hut anchor `(4,1,3)`, a north entrance, timber walls/roof, stone foundation, crafting table, barrel, two indoor stair seats, a native `work` tag and a `stand_in` tag. `tools/generate_dispensary.py` writes deterministic Structurize NBT and checks entrance-connected, two-block-high paths to the hut, preparation area, barrel and seat approaches. The native blueprint reader also verifies its anchor and contents.

The hut uses vanilla material textures and is available in the Functional Blocks creative tab. Its provisional recipe uses six planks, two paper and one weed bud. The Budtender uses the native citizen model. These provide functional access now; distinct worker appearance, final hut art, rotated/mirrored visual qualification and levels 2–5 blueprints remain Phase 5. Although the registered building supports five completed levels, only level one can currently be selected and constructed from this pack.

Construction uses native building controls and work orders. Tests place the prototype directly and complete its level through the native building API; they do not establish a builder-observed construction/repair session.

## Baseline and persistence

All 135 locked installed artifacts still match their metadata and SHA-256. The installation has 136 JARs because it also contains the older FourTwenty build. The lock was preserved; isolated preparation uses the verified 135-artifact reference copy. Kingdom's Gate remains clean at `e2f7c3884a3b40fb0aee48fe2d2ec0dd024faf34`, with the matching archived `0.2.0` artifact. See [baseline.json](../validation/evidence/dispensary-phase2/baseline.json).

Building level, staffing, inventories, crafting recipes, task queues and request identities use MineColonies' existing serializers. No new parallel request cache or production counter is stored. Phase 1 citizen state and simulation timing are unchanged. Native fixtures round-trip building/tile NBT and recheck assignment, stock and outstanding-request deduplication. Comprehensive fresh-process colony/request recovery remains Phase 6; this is not a claim of crash-atomic saves.

## Evidence and limits

Evidence is in [validation/evidence/dispensary-phase2](../validation/evidence/dispensary-phase2). The development artifact retains version `0.3.2`; no commit, installation, publication or release was performed.

Final JAR: `build/libs/fourtwenty-0.3.2.jar`  
SHA-256: `aac7b7d9eac1ae6f4e605a27dff39d3736815fc037b8a603c40b8357482274f1`

| Evidence | Result |
| --- | --- |
| Build/JUnit | `build verifyCompatibility` passed; 18 tests, including 2 new production-rule tests |
| Python tooling | 6 existing tests passed; deterministic blueprint generation and route assertions passed |
| Dependency ranges | 149 resolved mod/platform IDs, 331 checks, 44 nested artifact groups; compile API checksum verification passed |
| Packaged FourTwenty colony suite | 29 required tests passed with MineColonies and Kingdom's Gate loaded, including all 6 new production/logistics fixtures |
| Packaged standalone suite | 17 required tests passed without MineColonies |
| Packaged Kingdom's Gate subset | All 9 unchanged sibling fixtures passed independently with FourTwenty loaded |
| Autonomous logistics | Actual native courier navigation, warehouse pickup, delivery, Budtender crafting and storage of 16 joints; exact conservation of 16 buds and 32 paper; no actor teleportation after initial placement |
| Mixed control without colony/dispensary fixtures | 26 required tests passed (existing FourTwenty core/player tests plus all 9 sibling tests) |
| Combined packaged suite | 36/38 passed; the two Kingdom's Gate ritual/book fixtures fail in the shared run, while all 9 sibling fixtures pass in their isolated run. Combined-suite qualification remains open |
| Standalone and colony packaged clients | Both completed mod loading, resource reload, sound/texture atlas and animation loading; no missing dispensary model/texture error |
| Interactive client gameplay | Pending: UI automation could not identify the development Java window. Client logs establish startup/resource loading only |
| Ordinary dedicated server, full installed pack, performance, hosted CI, release | Not qualified in this phase |

The logistics fixture loads its disposable area, supplies the warehouse once, spawns/hire-binds two native citizens, and ticks native building/request services because GameTestServer has no real visiting player. It does not transfer ingredients into the dispensary, invoke crafting directly, move actors after setup, or force chunks in production code. Other focused fixtures cover unbuilt/completed staffing, dismissal, sided capabilities, persistence, missing paper, exact native recipe consumption, independent huts, request deduplication, output capacity and blueprint parsing.

Earlier diagnostic logs are retained separately. They exposed and led to fixes for a server-side model reference, a fixture resource-classloader lookup, insufficient output capacity and carried finished stock. The final combined packaged suite still fails `recruitmentAndRitualCleanup` (no first-day speech credit) and `booksUseNativeRequestsAndLimitConversions` (native request not marked fulfilled). All 9 sibling fixtures pass in a separate fresh process with the same final JARs. A further mixed control containing the existing FourTwenty core/player tests plus all sibling tests passed 26/26 without the colony/dispensary fixture namespaces. This narrows the failure to the larger shared fixture run, but does not establish its cause. Do not equate these successful subsets with combined-suite success. No sibling code or assertion was changed. Both clients were deliberately terminated after resource loading; their termination codes are not clean-exit evidence.

## Reproduction and next handoff

Use the Java 21 JDK and repository-local Gradle cache documented in [DEVELOPMENT.md](DEVELOPMENT.md). Do not replace installed JARs or reuse player worlds.

```sh
python3 tools/generate_dispensary.py
./gradlew build verifyCompatibility
python3 -m unittest discover -s tools/tests -v
python3 tools/compatibility.py prepare --profile colony \
  --mods .compat/dispensary-phase1-reference \
  --destination .compat/runs/dispensary-phase2-new \
  --cult-jar .compat/cult-source/build/libs/minecolonies-cult-0.2.0.jar \
  --fourtwenty-jar build/libs/fourtwenty-0.3.2.jar
./gradlew runGameTestServer -Ppackaged=true -PcompatProfile=colony \
  -PcompatRunDir=.compat/runs/dispensary-phase2-new \
  -PcompatTestNamespaces=fourtwenty_tests,fourtwenty_colonytests,fourtwenty_logisticstests,fourtwenty_playertests,fourtwenty_permissiontests
```

`compatTestNamespaces` is an explicit fixture-selection override; omit it for the existing combined default plus the new logistics namespace. A selected subset must not be labeled as the complete combined suite. Run Kingdom's Gate fixtures independently with `-PcompatTestNamespaces=minecolonies_cult` in another fresh profile.

Changed production files are the optional registry/building/job/crafting/AI classes, pure stock rules, integration registration, hut assets and blueprint. Tests add production rules, native building/crafting/capacity/persistence fixtures and autonomous logistics. Gradle adds explicit namespace selection and includes logistics in its default native suite. Existing crops, bong behavior, edibles, food-exclusion tags, joint/player effects and citizen demand schema remain unchanged.

Phase 3 can use `DispensaryBuilding`, its native handlers, and the `sit_in` markers for collection and consumption. It must preserve the reserved service inventory, distinguish transfer from consumption, and allow Budtender self-collection. No customer reservations, cannabis consumption, happiness modifier or health benefit exists yet.
