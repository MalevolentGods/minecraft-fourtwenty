# Dispensary Phase 1 handoff

Date: 2026-10-14. Scope: optional integration, eligibility and timing rules, configuration, and persisted citizen state. Building service, crafting, visits, happiness changes, and health attributes are not implemented yet.

## Implementation baseline

The installed MC-TechColonies directory contains 136 JARs. All 135 artifacts in `validation/compatibility-lock.json` still match, including metadata and SHA-256. The only addition is the previously built `fourtwenty-0.3.2.jar` (`bec76b1af3137f77224f363acb12f32766d6370930ed54931ea8bf5115b0e024`). The strict inventory verifier therefore reports a mismatch. The lock was not changed. Testing copied only the 135 matching entries to `.compat/dispensary-phase1-reference`, then used normal isolated-profile preparation against that copy.

The sibling Kingdom's Gate checkout is clean at `e2f7c3884a3b40fb0aee48fe2d2ec0dd024faf34`. Its previously archived build has matching provenance and SHA-256 `9726fb9ac9decd98aff1cc109c3a86f8f4a26f1125bfbc5beb3ccac287df2d25`. No sibling source or installed mod was edited. Exact core versions and checksums are recorded in [baseline.json](../validation/evidence/dispensary-phase1/baseline.json).

Pinned API inspection used `javap -c` on the installed MineColonies `1.1.1343-1.21.1-snapshot` JAR and the current sibling source:

- `CitizenData.getHomeBuilding()` returns the actual home assignment. `getHomePosition()` has fallback locations and is deliberately not used.
- `GuardBuildingModule` extends `WorkAtHomeBuildingModule`. Its native assignment calls `setHomeBuilding(building)`; removal clears the home. A level 3 guard tower qualifies when it is the assigned home. A level 5 workplace with no home does not qualify.
- Eligibility reads `home.getBuildingLevel()`, never the requested blueprint path or upgrade target. Native fixtures exercise levels 0–5, children, homelessness, a higher workplace, and guard hiring.
- Kingdom's Gate injects `CitizenAI.calculateNextState` at HEAD and selects its ritual state while attending. Its happiness mixins install membership modifiers in both handler constructors before deserialization. Phase 1 adds no competing AI or happiness hooks. Phase 3 must explicitly yield to active rituals; Phase 4 must preserve the existing membership modifier.

## Optional loading and identifiers

`FourTwenty` checks `ModList` before entering `compat.minecolonies.ColonyIntegration`. Core entrypoint signatures contain no MineColonies types. All six locked core artifacts are compile-only dependencies, verified by `verifyColonyCompileDependencies` before compilation and excluded from the release JAR/runtime classpath. Missing or changed cached APIs stop compilation without changing pins.

The optional dependency range is exactly `[1.1.1343-1.21.1-snapshot]`, the inspected API. Absence is supported; other installed MineColonies versions require a new compatibility review. A dedicated mixin configuration has a `LoadingModList` presence gate for future colony hooks; its mixin list is intentionally empty in this phase. The existing Actually Harvest mixin is unchanged.

| Purpose | Stable identity |
| --- | --- |
| Building | `fourtwenty:dispensary` |
| Job | `fourtwenty:budtender` |
| Happiness | `fourtwenty:cannabis_satisfaction` |
| Health attribute modifier | `fourtwenty:cannabis_health` |
| Dimension SavedData file | `data/fourtwenty_dispensary.dat` |
| State schema | Integer `version = 1` |

Building/job/benefit identifiers are reserved constants, not registered gameplay features yet.

## State and timing contract

Each server dimension owns a `SavedData` instance with a persisted simulation clock. It advances once per normally running server level tick. Minecraft tick freeze, server downtime, and a paused/unloaded dimension do not advance it. Citizen chunk unloading does not suspend deadlines while the dimension continues ticking. No static world cache or wall clock is used.

Sleep and `/time` change day time, which is never an input. The pinned `ServerLevel.tick` sleep branch calls `setDayTime`; the native test jumps that value forward and backward and verifies exactly five elapsed ticks, without another dose. This is a native clock test plus source inspection, not an observed player sleeping session.

Records are keyed by `(colony ID, citizen UUID)` within the dimension. Each immutable record stores eligibility, due time, grace expiry, satisfaction expiry, health entitlement expiry, last completed consumption time, and a monotonic completion revision. The clock and records are serialized together. Unknown versions, missing/wrongly typed fields, duplicate identities, and invalid state are rejected. Because Minecraft catches SavedData loader exceptions, the creation fallback also checks for an existing file and refuses to replace an unreadable save with empty state.

The runtime observes native citizen records, including citizens whose entities are unloaded. First observation of an eligible adult starts demand immediately and grants 24,000 ticks of grace. At the exact grace deadline the state becomes unmet. Moving between qualifying homes preserves all deadlines. Loss of eligibility disables demand and penalties and invalidates outstanding completion tokens; inventory is untouched. Earned health entitlement continues to its original deadline. Re-entry before satisfaction expires preserves the remaining satisfaction and grace; later re-entry starts normal initial grace.

Completing consumption while due schedules satisfaction/demand at `now + interval`, penalty at `now + interval + grace`, and health expiry at `now + healthDuration`. Satisfaction and health use exclusive expiry boundaries; demand and unmet status begin at their exact deadlines. Missed intervals never accumulate extra doses. Health has one expiry field; refreshing replaces that expiry rather than stacking entries.

`recordConsumption` is a foundation contract, with no production caller yet. Phase 3 must revalidate native eligibility, check the saved revision, consume exactly one joint, and record completion in the same authoritative server action. Collection, interrupted animation, loading, and status reads must never call it. A stale revision remains invalid even after the next demand interval or an orderly save/reload. Phase 4 must derive benefits from saved entitlement instead of replaying completion. Crash-atomic item/colony saves and interrupted-service lifecycle qualification remain Phase 6 work.

Records are retained as tombstones when eligibility is lost; death, deleted-colony cleanup, and migration policy beyond schema 1 remain Phase 6 work. No inventory, hunger, food history, native happiness, or health attribute is changed in Phase 1.

## Configuration and build setup

MineColonies-enabled worlds receive `serverconfig/fourtwenty-dispensary-server.toml`. `demandIntervalTicks`, `demandGraceTicks`, and `healthDurationTicks` default to 24,000, with an allowed range of 1–8,760,000 ticks. NeoForge validates the server configuration; the pure rules constructor also rejects out-of-range values. Eligibility stays fixed at adult/assigned home/completed level 3+. Configuration changes affect the next newly scheduled deadline, never retroactively shorten or extend a persisted deadline. Qualifying-home reassignment does not reschedule it.

Normal compatibility preparation populates `.compat/dependencies`. To bootstrap only the compile APIs from the pinned installation without changing inventory pins:

```sh
python3 - <<'PY'
import hashlib, json, pathlib, shutil
lock = json.loads(pathlib.Path('validation/compatibility-lock.json').read_text())
destination = pathlib.Path('.compat/dependencies')
destination.mkdir(parents=True, exist_ok=True)
for entry in lock['core']:
    source = pathlib.Path(lock['sourceDirectory']) / entry['file']
    target = destination / entry['file']
    assert hashlib.sha256(source.read_bytes()).hexdigest() == entry['sha256'], source
    if target.exists():
        assert hashlib.sha256(target.read_bytes()).hexdigest() == entry['sha256'], target
    else:
        shutil.copy2(source, target)
PY
```

Verify `"$JAVA_HOME/bin/java" -version` rather than trusting a symlink name. This machine's Homebrew `openjdk@21` path reported Java 22; validation used `/Users/oberon/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.12.1+1/Contents/Home` and repository-local `GRADLE_USER_HOME=.gradle-home`. The first sandboxed build could not open Gradle's lock socket; validation ran with the required execution permission. One missing Minecraft cache entry required an online Gradle run. Dependency locks and checksum metadata were preserved.

## Verification and evidence

Final artifact: `build/libs/fourtwenty-0.3.2.jar`, SHA-256 `782acc6e9b1dbe47041ebca0e811b3e2d28a770616059454f3f5cfe1ac5045d7`. This is a development artifact with the existing version, not a published release.

Evidence is under [validation/evidence/dispensary-phase1](../validation/evidence/dispensary-phase1). Direct launch commands are retained in each capture's `result.json`, with prepared inventories, launch fingerprints, and native logs. Final captures have the `-final` suffix; earlier captures are retained as diagnostic history.

| Evidence type | Result / scope |
| --- | --- |
| Build and JUnit | `./gradlew build verifyCompatibility`: 16 tests pass, including 10 new rules tests; package built |
| Python tooling | `python3 -m unittest discover -s tools/tests -v`: 6 tests pass |
| Dependency validation | Frozen inventory: 149 resolved IDs, 331 checks, 44 nested groups; exact core compile hashes checked |
| Standalone packaged GameTest server | 19 required tests pass in each write/read process, including durable demand and paid-bong restart checks |
| Colony packaged GameTest server | 32 required tests pass with MineColonies, its pinned dependencies, and Kingdom's Gate |
| Standalone packaged client | Mod loading, resource reload, sound, texture atlases, and animation loader complete without MineColonies present |
| Colony packaged client | Same startup path completes with MineColonies and Kingdom's Gate; integration enabled marker recorded |
| Observed client gameplay | Not performed; UI automation could not identify the development Java window. Client evidence is startup/resource logs only |
| Ordinary dedicated server | Not performed; GameTest server execution establishes the server class-loading path, not ordinary-server acceptance |
| Full installed pack, performance, hosted CI, release | Not rerun/performed in this phase |

Client processes were deliberately stopped after resource loading; their termination exit codes are not presented as clean client-exit tests. The standalone client logged a transient NeoForge/NightConfig temporary-file watcher exception and continued through resource loading. Colony fixtures retain native blueprint/sound warnings in their full logs. An initial fixture failed because its detached home lacked a block entity; it was corrected to use registered huts and rerun in a fresh world. No product behavior was changed to mask these diagnostics.

Reproduction after preparing fresh profiles as described in DEVELOPMENT.md:

```sh
./gradlew build verifyCompatibility
python3 -m unittest discover -s tools/tests -v
./gradlew runGameTestServer -Ppackaged=true -PcompatRunDir=FRESH_STANDALONE -PpersistenceStage=write
# Capture write evidence, then deliberately reuse only this checkpoint world:
./gradlew runGameTestServer -Ppackaged=true -PcompatRunDir=FRESH_STANDALONE -PpersistenceStage=read
./gradlew runGameTestServer -Ppackaged=true -PcompatProfile=colony -PcompatRunDir=FRESH_COLONY
./gradlew runClient -Ppackaged=true -PcompatRunDir=FRESH_CLIENT_STANDALONE
./gradlew runClient -Ppackaged=true -PcompatProfile=colony -PcompatRunDir=FRESH_CLIENT_COLONY
```

Changed production files are the Gradle compile dependency/verification wiring, core mod entrypoint, optional metadata and mixin gate, and `compat/minecolonies` package. Tests add pure demand rules, NBT/disk persistence, and native housing/clock fixtures. Existing recipes, registry IDs, player effects, crop/bong code, and edible tags are unchanged. Phase 2 can now implement the Dispensary and Budtender against this contract; service and benefit acceptance remain in their planned phases.
