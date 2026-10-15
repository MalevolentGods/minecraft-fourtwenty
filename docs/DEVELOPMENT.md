# Development and compatibility runs

Use Java 21, Python 3.11+, the committed Gradle 8.13 wrapper, and NeoGradle 7.0.189. Set `JAVA_HOME` to a Java 21 JDK and `GRADLE_USER_HOME` to a disposable local cache when needed. Runtime requires only Minecraft and NeoForge. Compilation also requires the six checksum-pinned colony API JARs in `.compat/dependencies`; they are compile-only and never bundled. The dispensary integration is gated by MineColonies presence and its inspected exact version. See the [Phase 1 contract and evidence](DISPENSARY_PHASE1.md), [Phase 2 production handoff](DISPENSARY_PHASE2.md), [Phase 3 service handoff](DISPENSARY_PHASE3.md), and [Phase 4 benefits handoff](DISPENSARY_PHASE4.md). Optional colony permission/configuration classes resolve lazily; the Actually Harvest mixin is a pseudo mixin and is absent when that mod is absent.

## Make commands

Run `make` or `make help` for the full target list. GNU Make 3.81 or newer is
supported. Make delegates to the committed Gradle wrapper and Python tooling;
the direct commands below remain available without Make.

`make build` packages the mod and runs Gradle checks. `make test` runs Java and
Python unit tests; `make check` runs Gradle checks plus Python tests. `make ci`
builds and runs those tests with `--no-daemon`; it does not configure hosted CI
or establish native/full-pack qualification. Native tests use `make gametest`
after explicitly preparing an isolated run.

```sh
make ci
make test-java GRADLE_ARGS='--offline --stacktrace'
make compat-prepare RUN_DIR=.compat/runs/standalone-new
make gametest RUN_DIR=.compat/runs/standalone-new
make capture RUN_DIR=.compat/runs/standalone-new \
  EVIDENCE_DIR=validation/evidence/standalone-new \
  COMMAND='make gametest RUN_DIR=.compat/runs/standalone-new'
```

Run preparation rejects existing destinations. Launch targets never prepare or
replace a world implicitly. `make clean` removes Gradle build outputs only;
isolated worlds, evidence, caches, and local Maven publications remain intact.
`make rebuild` performs `clean build` in order, and Make serializes targets even
when invoked with `-j` to avoid concurrent writes to shared outputs.

| Variable | Default / use |
| --- | --- |
| `GRADLE`, `PYTHON` | `./gradlew`, `python3` |
| `GRADLE_ARGS` | Extra Gradle arguments, such as `--offline` or `-PpersistenceStage=write` |
| `COMPAT_ARGS` | Extra tooling arguments, such as `--cult-jar PATH`, `--fourtwenty-jar PATH`, `--mods PATH`, or `--headless-adjustments` |
| `PROFILE` | `standalone`; also `colony`, `full`, or `baseline` |
| `RUN_DIR` | `.compat/runs/$(PROFILE)`; choose a fresh path for each scenario |
| `PACKAGED` | `false`; set `true` for packaged/baseline launches and run verification |
| `TEST_SELECTION` | `all`; use `nonplayer` for the documented narrower native suite |
| `INVENTORY` | `validation/compatibility-lock.json`, used by `compat-ranges` |
| `CULT_DEST` | `.compat/cult-source-new`, must be fresh |
| `CULT_GRADLE_HOME` | `.compat/cult-gradle-home` |
| `EVIDENCE_DIR`, `COMMAND` | Required by `capture`: fresh output path and exact launch command |

`JAVA_HOME` and `GRADLE_USER_HOME` are inherited normally. Path variables are
quoted; argument-list variables use normal shell quoting. For example:

```sh
make compat-prepare PROFILE=full RUN_DIR=.compat/runs/full-new \
  COMPAT_ARGS='--cult-jar .compat/cult-source/build/libs/minecolonies-cult-0.2.0.jar --fourtwenty-jar build/libs/fourtwenty-0.3.2.jar --headless-adjustments'
make gametest PROFILE=full RUN_DIR=.compat/runs/full-new PACKAGED=true TEST_SELECTION=nonplayer
```

`publish-local` writes to your local Maven repository; `publish-repo` uses the
current Gradle publication destination, this checkout's `repo/` directory.
Neither uploads a release. Dependency lock regeneration and inventory snapshot
replacement remain explicit direct commands requiring review as described below.

## Build and dependency verification

```sh
export GRADLE_USER_HOME="$PWD/.gradle-home"
./gradlew build
python3 -m unittest discover -s tools/tests -v
python3 tools/compatibility.py verify
./gradlew verifyCompatibility
```

`build` includes six JUnit scheduling tests, ten dispensary demand-rule tests, and two production-rule tests. Native GameTests are separate. The release JAR excludes `gametest`, `colonytest`, `packtest`, `baselineprobe`, `launcher`, and `verification` source sets. The empty launcher helper gives NeoGradle a source anchor in packaged/baseline runs without substituting FourTwenty's main output.

`validation/compatibility-lock.json` records all 135 installed JARs, recursively inspected JarJar artifacts, resolved manifest versions, source revisions, and the six frozen core artifacts. Never regenerate it to make a mismatch disappear. Preparation rejects changed hashes, duplicate IDs, existing destinations, incorrect cult provenance, and stale destination JARs. Installed copies of FourTwenty and Kingdom's Gate are excluded by mod ID.

`verifyCompatibility` uses the pinned FML loader's Maven range parser and compatibility support matrix, including its Minecraft 1.21 compatibility aliases. It checks required and present optional dependencies and nested candidate ranges. Native loading remains the final check of JarJar resolution. To audit the actual prepared pack:

```sh
./gradlew verifyCompatibility -PcompatInventory=.compat/runs/example/compatibility-run.json
```

The committed macOS dependency lock and SHA-256 verification metadata cover the resolved build/test configurations. Hashes were bootstrapped from the downloaded repository artifacts; they are integrity pins, not independent signature attestations. Linux uses `gradle/linux.lockfile`; a Linux runtime was unavailable in this session, so that lock and Linux qualification remain pending. On a real Linux worker, generate and review them with:

```sh
./gradlew resolveCompatibilityDependencies build --write-locks --write-verification-metadata sha256
```

Ordinary verification runs omit both write flags. Preserve platform lock differences instead of copying macOS resolution into Linux. No CI execution or release publication is implied by local results.

## Build the pinned sibling without changing it

```sh
python3 tools/compatibility.py build-cult \
  --destination .compat/cult-source-new \
  --gradle-home .compat/cult-gradle-home
```

This archives the exact sibling revision into a new directory, copies verified core JARs, runs its build, and records revision/JAR provenance in `build-record.json`. Use the resulting JAR as `--cult-jar` below. Repeated builds need a new destination. The original sibling checkout is read-only throughout this workflow.

## Prepare isolated profiles

Every new scenario needs a fresh destination. Do not merge mods into an existing installation. Examples use the already recorded sibling artifact:

```sh
python3 tools/compatibility.py prepare --profile standalone \
  --destination .compat/runs/standalone-new

python3 tools/compatibility.py prepare --profile colony \
  --destination .compat/runs/colony-new \
  --cult-jar .compat/cult-source/build/libs/minecolonies-cult-0.2.0.jar

python3 tools/compatibility.py prepare --profile full \
  --destination .compat/runs/full-new \
  --cult-jar .compat/cult-source/build/libs/minecolonies-cult-0.2.0.jar \
  --headless-adjustments
```

`full` and `baseline` copy `config`, `defaultconfigs`, `datapacks`, `global_packs`, and `resourcepacks` when present. Only the two resource-pack-selection lines are copied from `options.txt`. Saves, server lists, launcher profiles, and account data are not copied. `compatibility-run.json` records hashes, selections, configuration changes, and any diagnostic exclusions. Launch records add hashes of the actual compiled classes/resources and configuration before startup; evidence capture records configuration afterward.

`--headless-adjustments` changes only the isolated no-player configuration: Better Chunk Loading prediction/smart-loading/smart-postprocessing/waiting optimization are disabled, and Limited Chunks permits `forced`, `gametest`, `start`, and `unknown` tickets. Original-settings client/dedicated-server tests must omit the option.

For packaged runs, add `--fourtwenty-jar build/libs/fourtwenty-0.3.2.jar`, and launch with `-Ppackaged=true`. The resulting full profile has 136 top-level JARs. A baseline uses `--profile baseline`, no FourTwenty JAR, and `-Ppackaged=true`: it has 135 JARs, including the newly built Kingdom's Gate.

## Launch and native tests

```sh
./gradlew runGameTestServer -PcompatRunDir=.compat/runs/standalone-new
./gradlew runGameTestServer -PcompatProfile=colony -PcompatRunDir=.compat/runs/colony-new
./gradlew runGameTestServer -PcompatProfile=full -PcompatRunDir=.compat/runs/full-new
```

The native exit guard requires a fresh positive GameTest completion message, since Minecraft can exit zero after startup failure. Baseline runs also reject logs showing FourTwenty loaded. No native pass is inferred from Gradle's exit code alone.

JourneyMap beta.54 has a reproduced **GameTest-only player-join failure** without FourTwenty: it treats the unpublished GameTest server as an integrated server and accesses a client class. Preserve that full-pack failure. Two explicitly narrower runs are available:

- `-PcompatTestSelection=nonplayer` runs crop, bong, farmer-contract, colony-food, Multi-Piston, and Kingdom's Gate fixtures with all pack JARs present. It excludes player-dependent namespaces.
- Preparing with `--exclude-diagnostic-mod 'journeymap=Confirmed baseline GameTest player-join failure'` and `--headless-adjustments` runs all fixtures in a diagnostic copy. The manifest marks it `diagnostic-only`; it does not establish full-pack qualification.

The PlayerRevive fixture temporarily enables `bleedInSingleplayer` because GameTestServer is unpublished, then restores it. The replant-disabled fixture similarly restores Actually Harvest's setting. These are fixture-specific variations, not changes to the original-settings profile. Farmer fixtures invoke native planting and harvest recognition and feed native loot to a worker inventory; they do not prove field navigation, courier delivery, or an interactive three-cycle farm.

To reproduce the baseline JourneyMap failure, prepare a baseline and add `-PprobePlayers`. This loads an independent player-join probe with no FourTwenty gameplay sources.

For the actual client or ordinary dedicated server, prepare a new full packaged profile **without** headless changes:

```sh
./gradlew runClient -PcompatProfile=full -Ppackaged=true -PcompatRunDir=.compat/runs/client-new
./gradlew runServer -PcompatProfile=full -Ppackaged=true -PcompatRunDir=.compat/runs/server-new
```

The server owner must accept Minecraft's EULA before ordinary dedicated-server startup. Tooling does not accept it automatically. Development clients use their development identity; no launcher credentials are copied.

## Disk persistence checkpoint

Use a fresh prepared run for the first command. Reuse it deliberately for the second command, capturing the first log before the second launch:

```sh
./gradlew runGameTestServer -Ppackaged=true -PcompatRunDir=.compat/runs/persistence-new -PpersistenceStage=write
# Capture write-stage evidence here.
./gradlew runGameTestServer -Ppackaged=true -PcompatRunDir=.compat/runs/persistence-new -PpersistenceStage=read
```

Add the matching `-PcompatProfile` and test selection for colony/full profiles. The fixture writes a paid bong at a reserved position in the disposable world, flushes the chunk, and then checks the exact persisted inventory and burn tick after a new process starts. Never run this fixture in a player world. This deliberate checkpoint reuse is the exception to fresh-world isolation.

## Preserve evidence

```sh
python3 tools/compatibility.py capture \
  --destination .compat/runs/example \
  --output validation/evidence/example \
  --command './gradlew runGameTestServer -PcompatRunDir=.compat/runs/example'
```

Evidence destinations are exclusive. Capture the exact command and retain build output, test reports, JAR hashes, and observations alongside the native logs. Earlier evidence predates launch fingerprints; its capture-time source hashes must not be described as historical compiled-source hashes. `validation/evidence/phase1/INVALID-BASELINE.txt` identifies the discarded early baseline that accidentally included main sources.

Do not overwrite the reference installation, edit the sibling source, auto-commit, publish, or migrate a player save as part of compatibility testing. See the [qualification report](COMPATIBILITY.md) and [remaining acceptance scenarios](ACCEPTANCE.md) before declaring compatibility.

Dispensary native fixture selection can be set explicitly with `-PcompatTestNamespaces=fourtwenty_colonytests,fourtwenty_logisticstests`. The default still includes the existing suites and now also runs autonomous logistics. Report subset runs as subsets. Regenerate all five blueprints with `python3 tools/generate_dispensary.py` and their pixel assets with `python3 tools/generate_dispensary_assets.py`; its native blueprint, inventory, request and courier fixtures are described in the Phase 2 handoff.


## Dispensary Phase 5 authoring and presentation

See [the Phase 5 handoff](DISPENSARY_PHASE5.md). Blueprint paths stay at `blueprints/fourtwenty/dispensary/dispensary/dispensary{1..5}.blueprint`; pack identity is `FourTwenty`. The local hut anchor is `(width/2, 1, 3)` (integer division), so expansion moves the local offset but preserves the placed hut and entrance in world space. Local X/Z dimensions are 9×11, 11×14, 13×17, 15×20, 17×23. Heights are 10, 11, 12, 13, 14. The maximum anchor-relative bounds are `(-8,-1,-3)` through `(8,12,19)`, inclusive, before rotation/mirroring. Indoor `sit_in` tags number 2/4/6/8/10. `work` and `stand_in` tags remain accessible. Racks are native colony stock; barrels are decorative/general-purpose storage.

The transient `fourtwenty:dispensary_status` module uses native building view serialization. The server-level tick listener checks loaded huts for changed snapshots every 100 ticks of the saved simulation clock; initial view requests serialize fresh state. Do not schedule this through `ITickingModule.onColonyTick`: MineColonies invokes that hook on a staggered 500-tick transition, which is not aligned with world-time boundaries. The snapshot combines the hut/rack capability exactly once with each assigned citizen inventory. Counter availability follows existing collection sources. Customer leases are pruned using the saved simulation clock. This introduces no saved-state schema or custom client commands. Client views and model registration stay behind the existing optional-mod/client boundaries.

The explicit `-PpresentationClientProbe=true` flag enables test sources and quick-loads `saves/benefits` in a disposable colony profile. It renders synthetic status packets, reloads resources, checks model registration and native pack lookup, directly places the five blueprints, checks live crafter/building views and a server-side stock change, and captures screenshots. `-PstandaloneClientProbe=true` instead checks standalone title-screen startup without loading colony test sources. Require `FOURTWENTY_PRESENTATION_CLIENT_PROBE_PASSED`; process success alone is insufficient. Direct placement is not native construction, upgrade, repair, or observed autonomous customer acceptance. This probe must never target a player world.

## Dispensary Phase 6 qualification

See [the Phase 6 handoff](DISPENSARY_PHASE6.md) for exact commands, migration rules and evidence scope. `tools/qualify_dispensary.py` runs a single packaged scenario and captures it under an exclusive evidence name. `prepare --profile colony --without-cult` provides the minimal MineColonies profile; ordinary colony profiles still require a verified sibling JAR. Full and baseline profiles cannot use this omission.

The opt-in `-PdispensaryLifecycleStage=write|read` checkpoint uses namespace `fourtwenty_restarttests`; select it explicitly to keep unrelated colony cleanup out of restart tests. Run the supplemental native lifecycle suite explicitly with `-PcompatTestNamespaces=fourtwenty_lifecycletests` in a separate fresh profile. Keeping its death/unload scenarios separate avoids changing the world timing of the existing ritual/logistics fixtures. The checkpoint's local profile-cache shim is test-only because vanilla GameTestServer has no profile services. Never enable the checkpoint against player worlds.

Ordinary-server commands now receive standard input so `stop` triggers native saving. The qualification runner requires explicit `--accept-eula`, binds its server to loopback port 25576, and records startup/idle/shutdown separately from connected gameplay.
