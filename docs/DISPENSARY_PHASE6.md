# Dispensary Phase 6 handoff

Date: 2026-10-15

Status: Persistence hardening and reproducible qualification tooling implemented. The evidence matrix below distinguishes automated fixtures, client probes, ordinary-server startup, and remaining gameplay/performance acceptance. The overall acceptance checkbox stays open until the outstanding observations are recorded.

## Delivered behavior

Permanent removal through MineColonies' citizen manager now deletes the citizen's dispensary record, interrupts any active visit, and clears the cached benefits. This covers native death and departure. Entity removal for chunk unloading only interrupts the temporary task; it retains demand, paid entitlement and personal inventory. Service transactions also verify that the citizen still belongs to the authoritative manager, so an old live entity cannot recreate a removed record or complete a stale transaction.

Building deserialization now clears customer and seat reservations, including reload into an existing object. Native building inventory, worker assignments and request data remain owned by MineColonies. Fresh entities begin with an inactive visit task; an unconsumed personal joint remains available for a new, fully timed smoking action. No saved navigation, seat lease, or partially elapsed smoking action is restored.

Dispensary module producers now register in the serial building-registry callback. The pinned MineColonies producer constructor mutates a shared `HashMap` and increments an unsynchronized runtime-ID counter, so constructing producers during parallel mod construction could corrupt lookup or duplicate runtime IDs. A packaged diagnostic crashed in `CustomRecipeManager.buildLootData` when a copied producer key could no longer be looked up. The new native registration check verifies lookup identity and unique runtime IDs across all producers.

Registry IDs, blueprint paths and dimensions, joint recipes, player effects, crop/bong behavior and schema version remain unchanged. MineColonies classes remain behind the optional integration boundary. The release JAR excludes all qualification helpers.

## Persistence and migration contract

The supported dispensary format remains **version 1**. Pre-dispensary worlds have no record and initialize normally. There is no released version-0 format to migrate: unversioned, zero-version, future-version, malformed and duplicate records are rejected, rather than interpreted as a fresh state. Tests also reject a negative or incorrectly typed clock and consumption in the future. A supported v1 record round-trips without changing its fields. The existing SavedData factory refuses to replace an existing unreadable file when vanilla falls back after a load error.

Deadlines use the dimension's saved simulation clock. Server downtime and day-time commands do not accrue extra consumption opportunities. Settings changes affect newly scheduled deadlines; they do not rewrite existing entitlements. Reassignment between eligible homes preserves the schedule; losing eligibility suppresses demand and penalty while leaving an already earned health entitlement until expiry. Reentry cannot award another early dose.

The new disk checkpoint creates a native colony, six native home assignments, a level-three dispensary, a Budtender, seven stored joints, three buds, six paper and one outstanding production request. Six citizen records represent unpaid travel/wait, transferred/smoking personal stock, paid satisfaction, and unmet demand. A separately launched JVM retrieves the native colony and exact stock, staff, jobs, requests, personal inventory and revisions from disk, rejects a stale completion token, and checks that repeated production ticks do not duplicate requests. The expected-results file contains identities/revisions only; the reader does not reconstruct the colony or insert its stock.

These checkpoint categories exercise durable outcomes, not a process shutdown at every naturally reached animation/navigation frame. Actual connected-player reconnects, original-settings chunk unloading and every live travel/wait/transfer/smoke cutpoint remain separate acceptance work.

## Reproduction

Use Java 21 and the repository-local Gradle cache as described in [DEVELOPMENT](DEVELOPMENT.md). The cached installation used here was `/Users/oberon/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.12.1+1/Contents/Home`.

```sh
export JAVA_HOME=/Users/oberon/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.12.1+1/Contents/Home
export GRADLE_USER_HOME="$PWD/.gradle-home"
./gradlew build compileColonytestJava verifyCompatibility
python3 -m unittest discover -s tools/tests -v
```

`tools/qualify_dispensary.py` prepares an exclusive run directory, uses the packaged JAR, records launch fingerprints, requires positive native markers, captures logs/configuration changes, and writes a scoped `qualification.json`. Evidence names must be new. It never replaces the reference installation or accepts the EULA without the explicit `--accept-eula` flag. The server owner authorized that flag for this session's disposable servers.

The reference directory used here is `.compat/phase6-reference/mods`. It contains links to the 135 checksum-verified installed reference artifacts, with links to the installation's current configuration/resource-pack directories in its parent. The extra installed FourTwenty JAR is not part of that frozen reference; the runner supplies the freshly built artifact. The compatibility lock is unchanged. For reproduction, use an equivalent verified reference directory whose parent contains the original pack settings.

```sh
python3 tools/qualify_dispensary.py gametest --name example-restart-write \
  --mods .compat/phase6-reference/mods \
  --cult-jar .compat/cult-source/build/libs/minecolonies-cult-0.2.0.jar \
  --gradle-arg=-PcompatTestNamespaces=fourtwenty_restarttests \
  --gradle-arg=-PdispensaryLifecycleStage=write
python3 tools/qualify_dispensary.py gametest --name example-restart-read \
  --reuse .compat/runs/dispensary-phase6-example-restart-write \
  --mods .compat/phase6-reference/mods \
  --gradle-arg=-PcompatTestNamespaces=fourtwenty_restarttests \
  --gradle-arg=-PdispensaryLifecycleStage=read
```

Run the two supplemental lifecycle cases in a fresh profile with `--gradle-arg=-PcompatTestNamespaces=fourtwenty_lifecycletests`. They are deliberately separate from the default suites, and the restart namespace must also run independently. For the general suites, omit the two `--gradle-arg` selections. Choose `--profile standalone`, `--profile colony --without-cult`, `--profile colony` with the pinned sibling JAR, or `--profile full`. Full no-player GameTests use `--headless-adjustments --gradle-arg=-PcompatTestSelection=nonplayer`. The separately labeled all-fixture diagnostic also excludes JourneyMap with an explicit reason; that exclusion must never be presented as full-pack acceptance.

For client probes, replace `gametest` with `client` and use `--gradle-arg=-PstandaloneClientProbe=true`, or `--gradle-arg=-PpresentationClientProbe=true --world PATH/TO/DISPOSABLE/GAMETEST/world`. For ordinary servers, use `server --accept-eula`; the runner binds to loopback port 25576, waits for startup, observes ten seconds idle, sends `stop`, and requires a successful save/shutdown. Original copied mod settings are retained on client and ordinary-server runs. Launch commands and exact input hashes are recorded in each evidence directory.

## Evidence

The append-only record is [validation/evidence/dispensary-phase6](https://github.com/MalevolentGods/minecraft-fourtwenty/tree/d6a5609ee2b8ba5dc978dd6fa4a83fe2b0b623ce/validation/evidence/dispensary-phase6). Final results and artifact identity are recorded in `summary.json`. Earlier phase records are retained.

Final artifact: `build/libs/fourtwenty-0.3.2.jar`, SHA-256 **`9d658fbf11f0d5a84aa27f088520de37ada01a2f70e6b6288c5deda1a7b01d6e`**. Source baseline: `a65d2ee8c8a9d8235097ec4342d2b2016f53f2f7` plus this working tree. All 18 final scenarios below used that exact JAR; helpers remained outside it. The pinned sibling artifact is `9726fb9ac9decd98aff1cc109c3a86f8f4a26f1125bfbc5beb3ccac287df2d25`.

| Evidence class | Result | Record / scope |
| --- | --- | --- |
| Build and unit tests | Pass: 18 JUnit, 9 Python | `build-final.log`, `junit/`, `python-delivery.log` |
| Dependency checks | Pass: reference 149 IDs / 331 checks; packaged full profile 150 IDs / 334 checks; both 44 nested groups | `dependencies-final.log`; loader range boundaries also pass |
| Standalone GameTests | 17 passed | `standalone-final` |
| Minimal MineColonies, without Kingdom's Gate | 45 passed | `minimal-final`, seven JARs |
| MineColonies plus Kingdom's Gate, combined suites | **2 failed of 54** | `combined-final`, eight JARs |
| Full pack, nonplayer GameTests | 50 passed | `full-nonplayer-final`, all 136 JARs, documented headless settings |
| Full pack, all-fixture diagnostic | **4 failed of 57** | `full-diagnostic-final`, 135 JARs with JourneyMap excluded, headless settings; not full-pack acceptance |
| Kingdom's Gate suite in isolation, with FourTwenty | 9 passed | `cult-isolated-final`, eight JARs |
| Targeted pack suite in isolation | 3 passed | `pack-isolated-final`, same explicit diagnostic exclusion/settings |
| Supplemental lifecycle | 2 passed | `lifecycle-release`: unload hook, native removal/death, stale actor, housing, eligibility, reservations |
| Fresh-process native colony checkpoint | Write 1 / read 1 passed | `restart-release-write`, `restart-release-read`; exact stock, assignment, requests and citizen records |
| Ordinary dedicated servers | All four profiles passed startup, ten-second idle and orderly save/shutdown | `standalone-server-final`, `minimal-server-final`, `combined-server-final`, `full-server-final`; original mod settings, no exclusions; no connected gameplay |
| Packaged client probes | All four profiles passed | `standalone-client-final`, `minimal-client-final`, `combined-client-final`, `full-client-final`; original settings and all full-pack JARs |
| Baseline without FourTwenty | 9 Kingdom's Gate tests passed; ordinary server startup/save/shutdown passed | `baseline-tests` (headless fixtures), `baseline-server` (original settings) |
| Practical population performance | **Unmeasured** | Short fixture and idle durations do not establish performance |
| Connected gameplay, construction, Linux, hosted CI, release | **Open / not performed** | Acceptance ledger below |

The combined failures are `recruitmentandritualcleanup` and `booksusenativerequestsandlimitconversions`. The full diagnostic additionally fails `fakeplayerharvestrespectsdisabledreplanting` and `actuallyharvestownsclicksandareareplanting`. All four reproduce using the retained Phase 5 client JAR `3859e66fffb3656b45a91be6497e3377f663bd4ea89dfe0cab8843d0dc1313ea` with the same current test helpers and diagnostic profile (`phase5-comparison`). That older artifact also fails `levelticksrefreshmovementandqueuewithoutinventorychanges`, which passes with the current JAR. This is a controlled artifact comparison, not a rerun of the historical Phase 5 test set. Separate-suite passes suggest fixture state/order interactions, but do not establish the root cause or turn the combined suites green. See `phase5-failure-comparison.json`.

Client probes check resource reload, registered models, native status/crafter views, all five blueprint lookups/direct renders and live stock changing from zero to nine. Screenshots were captured for all colony profiles. Inspection confirms nine joints and nine available at counter in both minimal and full-pack native screens; the full-pack tutorial overlay partially obscures the title. These are probe/visual observations, not native builder or smoking acceptance (`visual-observations.json`).

All 15 recipe parsing errors in the final full ordinary server also occur in the no-FourTwenty baseline, with no recipe error unique to the candidate (`server-recipe-comparison-final.json`). Other warnings remain individually recorded and are not blanket-classified as harmless.

Completed Phase 6 implementation checks:

- [x] Preserve version-1 records; reject unsupported/malformed records without silently replacing unreadable saved data.
- [x] Clear transient reservations on load and permanent citizen state on native removal/death; reject stale actor transactions.
- [x] Verify housing/eligibility changes and retain unloaded citizens' durable state and personal stock through the native unload hook.
- [x] Preserve native building stock, worker, outstanding request and citizen outcomes across separate JVMs without replay or duplicate requests.
- [x] Exercise final packaged JARs across standalone, minimal, combined and full profiles; preserve exact failures, original-setting client/server evidence and explicit diagnostic exclusions.
- [x] Append the artifact record, reproduction commands, logs and acceptance limitations.
- [ ] Resolve combined-suite failures and complete connected gameplay, natural lifecycle cutpoints, builder/logistics observations and matched population profiling.


## Diagnostic history and limits

The first lifecycle fixture used an invalid cross-namespace annotation template; the second omitted the restart generator through namespace filtering. Both records are retained. Restart now has its own namespace and the runner requires its explicit write/read marker.

An initial combined lifecycle/restart run allowed the ordinary fixture's cleanup to delete the checkpoint colony. The restart-only namespace prevents that interaction. The first death fixtures also failed to reach lethal native damage: `Entity.kill()` bypasses the citizen death routine, and MineColonies caps individual hits at 20% of maximum health. The final fixture starts at one health, uses native damage, and asserts both citizen-manager and dispensary cleanup.

Subsequent restart diagnostics exposed two fixture requirements: `GameTestServer` supplies no profile cache, although MineColonies needs UUID lookup to restore permissions; and direct `setHomeBuilding` is insufficient to persist native residence assignment. The final fixture uses the native living-assignment module and installs a test-only, local UUID cache before colony loading. It has no authentication service and is absent from the packaged mod. These diagnostic workarounds do not modify production colony persistence.

Disk exhaustion during repeated preparation was resolved by removing only redundant `mods` copies from this session's completed disposable profiles, after capturing their manifests and logs. `disposable-mod-cleanup.json` lists those copies; worlds and all evidence remain. Reproduce older runs by preparing fresh profiles rather than reusing a cleaned directory. No installed JAR or player save was removed.

The lifecycle removal fixture invokes the native entity-unload hook; it does not establish actual full-pack chunk eviction. Native construction, upgrades, repairs, the complete farmer-to-courier-to-Budtender supply chain, visual smoke inspection, connected-client reconnects, practical-population performance and interactive Kingdom's Gate ritual/recruitment coexistence still require the observations in [ACCEPTANCE](ACCEPTANCE.md). Automated production, courier, service, benefit, food exclusion and farming fixtures are narrower evidence. No hosted CI, Linux qualification, release publication, installed-mod replacement or player-world migration is implied.

## Changed areas

Production: serial module initialization in `DispensaryRegistry`, `DispensarySavedData`, `DispensaryBuilding`, `JointService`, optional `CitizenRemovalMixin` registration. Verification: native lifecycle/restart fixtures, malformed-save cases, minimal-without-cult preparation and its test, packaged qualification runner, and Gradle console forwarding/restart selection. Documentation: this handoff, the plan, compatibility report, acceptance checklist, development instructions and append-only artifact/evidence records.
