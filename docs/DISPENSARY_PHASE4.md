# Dispensary Phase 4 handoff

Date: 2026-10-14

Status: Implemented and verified with packaged native tests and a real-client widget probe. Remaining qualification boundaries are listed below.

## Delivered behavior

Completed citizen consumption now grants native cannabis happiness and a uniquely identified `+2` maximum-health attribute modifier. Receiving or carrying a joint grants neither benefit. The existing consumption revision and saved simulation clock remain authoritative; the version-1 save schema, recipe, player effects and food behavior are unchanged.

`fourtwenty:cannabis_satisfaction` is registered as a native happiness modifier type. Both happiness-handler constructors install it before MineColonies reads server saves or client packets. Its serialized snapshot distinguishes ineligible, pending/grace, satisfied and unmet states. Server reconciliation replaces only this modifier when status or configured weight changes, invalidates the native happiness cache, and marks citizen data dirty for the existing native view packet. Unchanged snapshots do not mark citizens dirty every tick. Food, health and Kingdom's Gate happiness entries retain their own identities and values.

The client uses the authoritative serialized snapshot. Active native happiness rows have localized state-specific labels and explanations. Pending demand gets a neutral text row because MineColonies normally hides neutral modifiers. Ineligible citizens have no cannabis row. Explanations are supplied in English, with Minecraft's normal fallback for other languages.

Health uses the `fourtwenty:cannabis_health` attribute, with amount `2` and native `ADD_VALUE` operation. Its default duration remains 24,000 simulation ticks. Grant and refresh do not heal or stack. Reconciliation only changes this identifier, preserving unrelated buffs. A hook restores the entitlement inside MineColonies' bulk attribute reset before its health clamp, and entity initialization restores it from the saved deadline. Expiry removes it and clamps current health if necessary. The attribute is also serialized as a cache: vanilla loads attributes before current health, so a filled extra heart survives loading without being prematurely clamped. The demand record remains authoritative; expired cached attributes are removed during initialization. Native attribute multipliers retain their usual effects on additive modifiers.

Losing housing/adult eligibility stops demand and happiness effects immediately on reconciliation, but preserves already-earned health until its original deadline, as defined by Phase 1. Unloaded citizens' deadlines continue with the dimension's simulation clock; a returning entity gets only its remaining entitlement. Neither loading nor attribute reconciliation records another consumption. Server downtime and daylight changes do not advance this clock.

## Happiness tuning

The server config adds `satisfactionWeight` and `unmetDemandWeight`, each defaulting to **0.1**, accepting **0–1**. Zero disables that happiness contribution. Weight changes take effect on the next simulation tick; existing duration settings still affect only future deadlines. Satisfied uses factor **1.1**, unmet uses **0**, and pending/ineligible uses neutral factor **1**, which MineColonies excludes from its weighted calculation.

For the other active modifiers, let `S` be the sum of factor times weight, `W` their total weight, and `r` the native happiness research bonus. The pinned handler calculates:

```text
Neutral:   min(10, 10 * S / W * (1 + r))
Satisfied: min(10, 10 * (S + 0.11) / (W + 0.1) * (1 + r))
Unmet:     min(10, 10 * S / (W + 0.1) * (1 + r))
```

For nonnegative native research and a finite uncapped baseline, satisfaction increases happiness; unmet demand decreases positive happiness. At zero there is no lower penalty, and the native cap can hide either change. No native cap or other modifier is overwritten. The pinned handler's pre-existing all-neutral `W = 0` calculation remains unchanged while cannabis is neutral.

Representative results without membership or research:

| Other active weight | Baseline | Satisfied | Unmet |
| --- | ---: | ---: | ---: |
| 4 | 2.0000 | 2.2195 | 1.9512 |
| 4 | 6.0000 | 6.1220 | 5.8537 |
| 4 | 9.0000 | 9.0488 | 8.7805 |
| 20.5 | 2.0000 | 2.0437 | 1.9903 |
| 20.5 | 6.0000 | 6.0243 | 5.9709 |
| 20.5 | 9.0000 | 9.0097 | 8.9563 |

The native calibration fixture tests 80 combinations: other weights 4/20.5, baseline factors 0/0.2/0.6/0.9/1.2, research 0/25%, and actual Kingdom's Gate nonmember/member states. It checks exact weighted results, direction, caps, modifier preservation, and a default change below 0.34 happiness points in these cases. The full table is logged with `Cannabis calibration`. Configured weights approaching 1 intentionally allow larger changes; a weight is not a flat happiness-point value.

## Verification and limits

Evidence is recorded in [the Phase 4 evidence directory](../validation/evidence/dispensary-phase4). The live installed directory contains the same 135 locked artifacts plus FourTwenty, for 136 JARs. All locked metadata and hashes match. Kingdom's Gate remains clean at `e2f7c3884a3b40fb0aee48fe2d2ec0dd024faf34`. No pins, installed mods, player worlds or sibling source were changed.

Packaged artifact: `build/libs/fourtwenty-0.3.2.jar`  
SHA-256: `08ba5849fcea1c26f97cecac025a61bc76bc72533d278529f51abec422ff9daf`

| Evidence | Result |
| --- | --- |
| Build and JUnit | `build compileColonytestJava verifyCompatibility` passed; all 18 JUnit tests passed |
| Python tooling | All 6 tests passed |
| Dependency checks | 149 resolved mod/platform IDs, 331 checks, 44 nested artifact groups; pinned compile API checksums passed |
| Packaged combined native suite | All 51 required tests passed, including all 5 benefit fixtures and all 9 unchanged Kingdom's Gate fixtures |
| Packaged standalone native suite | All 17 required tests passed without MineColonies |
| Packaged real-client probe | Native BlockUI construction and translated happiness labels verified in an isolated integrated world; test client exited normally |
| Interactive visual inspection, actual remote reconnect, ordinary dedicated-server gameplay, full pack, performance, hosted CI, release | Not qualified |

Use the `combined-verified`, `standalone-verified` and `client-native-verified` evidence subdirectories for the final artifact. Earlier runs remain diagnostic history. The final unload test specifically saves a filled extra heart, verifies that loading preserves it without healing, and verifies clamping when the saved entitlement expires while unloaded.

Native fixtures cover exact one-heart addition, repeat refresh, no immediate healing, unrelated additive buffs, native bulk reset and level/research initialization, expiry with elevated health, housing loss, item extraction versus possession, all four happiness states, native server save deserialization, fresh native citizen-view packet readers, unchanged-state dirty suppression, saved deadline/revision round trips, and entity unload/recreation before and after expiry. An additional timed fixture exercises the real level-tick coordinator for satisfaction expiry, health expiry, grace expiry and eligibility loss.

Native packet-reader tests are synchronization evidence, not an observed remote reconnect. Entity recreation uses the native unload reason and entity NBT, not travel that naturally unloads chunks. Cross-process colony restart, actual remote reconnect, visual layout inspection, ordinary dedicated-server gameplay, full installed-pack acceptance, performance, hosted CI and release qualification remain follow-up evidence in Phase 6. Test worlds and test-provided stock are disposable fixtures. No commit or release was performed.

## Reproduction

Use Java 21 and the repository-local Gradle cache as described in [DEVELOPMENT.md](DEVELOPMENT.md). Choose a fresh directory for each preparation.

```sh
./gradlew build compileColonytestJava verifyCompatibility
python3 -m unittest discover -s tools/tests -v
python3 tools/compatibility.py prepare --profile colony \
  --mods .compat/dispensary-phase1-reference \
  --destination .compat/runs/dispensary-phase4-new \
  --cult-jar .compat/cult-source/build/libs/minecolonies-cult-0.2.0.jar \
  --fourtwenty-jar build/libs/fourtwenty-0.3.2.jar
./gradlew runGameTestServer -Ppackaged=true -PcompatProfile=colony \
  -PcompatRunDir=.compat/runs/dispensary-phase4-new
```

The default combined suite includes `fourtwenty_benefitstests`. To run only the five focused fixtures, pass `-PcompatTestNamespaces=fourtwenty_benefitstests` and label the result as a subset. Earlier diagnostic failures remain in the evidence directory; the first calibration fixture used a generic research effect that the native manager intentionally ignores, and was corrected to use `GlobalResearchEffect`.

For the real-client probe, prepare another fresh colony profile at `.compat/runs/dispensary-phase4-client-new` with the same arguments above, then copy only the disposable native-test world:

```sh
mkdir -p .compat/runs/dispensary-phase4-client-new/saves
cp -R .compat/runs/dispensary-phase4-new/world \
  .compat/runs/dispensary-phase4-client-new/saves/benefits
printf 'onboardAccessibility:false\nlang:en_us\n' \
  > .compat/runs/dispensary-phase4-client-new/options.txt
./gradlew runClient -PbenefitClientProbe=true -Ppackaged=true \
  -PcompatProfile=colony -PcompatRunDir=.compat/runs/dispensary-phase4-client-new
```

The explicit probe enables test source sets, quick-loads `saves/benefits`, constructs the actual native happiness widgets, checks localized pending/satisfied/unmet rows, logs `FOURTWENTY_BENEFITS_CLIENT_PROBE_PASSED`, and closes the client. Require that positive marker; Gradle success alone is insufficient. The probe supplies status snapshots to the widgets; server-to-client packet serialization is checked independently by the native tests. It does not claim manual visual acceptance. The first menu-only diagnostic could not create a MineColonies window because its constructor sends a server message. Menu-waiting diagnostic clients were explicitly terminated; only the final connected probe establishes clean exit.

## Changed files and next phase

Production changes are the `benefits` package; happiness, attribute, recalculation and client-window mixins; the gated native happiness registry; config weights; level-tick and completed-consumption reconciliation; mixin manifest; and localized explanations. `DispensaryBenefitsGameTests`, its disposable structure, the test-only client probe and Gradle wiring provide the new verification. The development guide and phase plan link this handoff.

Phase 5 remains the five finished blueprints and player-facing building UI. Phase 6 retains comprehensive restart, reconnect, pack, performance and observed gameplay qualification.
