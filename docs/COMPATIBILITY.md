# FourTwenty MC TechColonies compatibility

**Status: implementation candidate; full qualification remains open.** The corrected source and packaged JAR implement the agreed crop, inventory, movement, citizen-food, and pending-effect behavior. Automated evidence supports specific contracts below. It does not establish the complete interactive modpack acceptance matrix.

The reference installation and sibling source were not modified. No commit, publication, installed-mod replacement, or player-world migration was performed. All runs use isolated disposable directories under `.compat`.

## Frozen target

- Minecraft **1.21.1**, NeoForge **21.1.218**, Java **21**. Local execution used Eclipse Adoptium **21.0.12.1+1**, macOS ARM64.
- FourTwenty source baseline: `67b83ede3cdf0ec152e41922b08dc4a42213b9a2`, plus the uncommitted implementation in this working tree.
- Kingdom's Gate: isolated build of `e2f7c3884a3b40fb0aee48fe2d2ec0dd024faf34`, producing **0.2.0**.
- [Reference lock](../validation/compatibility-lock.json): **135 JARs**, **137 top-level mod IDs**, no duplicate top-level IDs, **19 nested-artifact parents** and **52 nested entries**. `${file.jarVersion}` values are resolved from unfolded manifests.
- Effective full pack: **134 retained reference JARs + fresh Kingdom's Gate + FourTwenty = 136 top-level JARs**, excluding test helpers. Installed Kingdom's Gate 0.1.0 is excluded by mod ID.
- The six core versions are MineColonies `1.1.1343-1.21.1-snapshot`, Structurize `1.0.831-1.21.1`, BlockUI `1.0.211-1.21.1-snapshot`, Domum Ornamentum `1.0.234-snapshot`, Multi-Piston `1.2.58-1.21.1`, and TownTalk `1.2.0`.

[Artifact hashes and provenance](../validation/artifacts.json) identify the actual candidate. FourTwenty's filename, loader metadata and manifest all use **0.3.2**. Its required dependencies remain Minecraft and NeoForge. The declared NeoForge range does not imply that every version in the range has been qualified.

## Phase evidence and handoffs

| Phase | Implemented and demonstrated | Open exit criteria |
| --- | --- | --- |
| 1 Build and environments | Pinned platform/version metadata; recursive inventory; isolated sibling build; fresh preparation/launch profiles; stale/hash/duplicate rejection tests; dependency verification; baseline without FourTwenty; separate helper source sets; macOS Gradle lock and checksums. Actual packaged graph passes 333 present-dependency checks over 150 resolved mod/platform IDs and 44 nested groups. | Linux lock/native build, original-settings dedicated server and complete interactive baseline. |
| 2 Farming | Valid singular `loot_table` resources; guaranteed replant seed; mature bud/bonus loot; one right-click loot roll; wild age-dependent seed loot; additive crop/seed tags; no farmer AI mixin. Native farmer planting/harvest recognition works for three loot-fed cycles. Actually Harvest area replanting and XP are exercised. | Real field assignment, courier seed delivery and autonomous three-cycle harvesting; remaining creative/tool/input scenarios. |
| 3 Bong | Shared validated slots/capability; partial/simulated transfer; two-menu/shift-click and real hopper coverage; immediate paid charge; legacy recovery including partial invalid stacks; exact 1,200-tick burn; movement in all directions, reverse/blocked moves, old menu/handler invalidation, vanilla piston rejection; real disk checkpoint and process restart. | Connected viewers, actual pack transfer devices, redstone-driven movement plus restart, original-settings connected-player chunk lifecycle. |
| 4 Effects | Server-owned UUID queue; one pending dose; food deduction rejection; tick-based deadlines; logout/final death/clone/shutdown cleanup; native dimension transfer; two fixture players; stronger/unrelated effects and milk. PlayerRevive canceled-death/downing, regeneration, native revival and final death are exercised in a documented fixture variation. | Two real network players, client resync under latency, integrated-world switching, Corpse recovery, Wormhole and downed movement/action interaction. |
| 5 Targeted interactions | Citizen food predicate exclusion and normal storage; all nine current Kingdom's Gate GameTests; colony protection regression and fix; every reference JAR inventoried and included in loading/coexistence scope. | Interactive colony/food/logistics/construction, Structurize scan/rotation/building, recipe browsers/crafting, normal fresh-chunk generation, broader pack matrix. |
| 6 Qualification | Packaged standalone/minimal/full-subset runs; isolated original-settings client startup; artifact/metadata inspection; native logs, configuration hashes, commands, test reports and warning records. | Complete original-settings client/server gameplay, EULA permission for ordinary dedicated server, two-player session, Spark baseline comparisons, Linux and remaining visual checks. |

The native farmer fixture invokes the installed AI's planting and harvest-recognition methods, obtains normal loot, and returns that loot to the worker inventory. It does **not** drive a naturally scheduled field or courier. The Multi-Piston fixture invokes the real movement transaction directly; it does not establish redstone timing. Native player fixtures use NeoForge's mock network setup, not real clients.

## Reproduced conflict and correction

With Actually Harvest 1.1.7.0, a nonmember could harvest a FourTwenty crop before MineColonies' event denial took effect. The same direct visitor interaction was denied in the minimal colony stack. Evidence is retained in [the failing regression](../validation/evidence/permission-regression-before-fix/native.log).

FourTwenty now guards Actually Harvest's `tryHarvest` for **FourTwenty cannabis crops only**, before mutation at each target. It calls the colony's existing permission handler through a lazy optional bridge. This preserves the colony's configured resident/operator decisions, prevents boundary-crossing area harvests, and adds no second harvesting event handler. There is no citizen AI mixin and no global protection override. An incompatible optional permission API fails closed for this specific path and logs the problem.

Actually Harvest can also return control after changing a crop, while Minecraft still holds the original mature state. FourTwenty now checks the live state before right-click mutation, preventing a second loot roll or resurrection of a removed crop. The installed Actually Harvest loot sink reserves a seed even with replanting disabled; the scoped mixin preserves FourTwenty's normal loot in that case. The native fake-player/replant-disabled regression passes after these fixes.

Client startup also exposed six unmapped bong blockstate variants. The existing empty/dry/water models now cover those combinations without changing model assets. Final client resource loading is recorded separately from visual inspection.

## Known limitations and evidence boundaries

1. **JourneyMap 1.21.1-6.0.0-beta.54, GameTest player join.** Its permission sync attempts to access `net.minecraft.client.Minecraft` on the dedicated distribution because GameTestServer is unpublished. The failure reproduces with the independent [baseline player probe](../validation/evidence/baseline-player-probe/native.log) and FourTwenty absent. It blocks full-pack mock-player fixtures. This is not evidence that ordinary dedicated servers or the actual client fail in the same way.
2. **Diagnostic isolation.** Player-dependent pack fixtures run with JourneyMap excluded only in a manifest-labelled diagnostic copy. All-mod loading/nonplayer runs retain it. Neither result substitutes for a complete all-mod connected-player test.
3. **Fixture settings.** Better Chunk Loading and Limited Chunks changes remain in no-player profiles. The PlayerRevive fixture temporarily enables `bleedInSingleplayer`; the disabled-replant fixture temporarily changes Actually Harvest and restores it. Original-setting acceptance remains separate.
4. **Existing recipe/data errors.** Oritech crafting recipes and Deeper and Darker's Farmer's Delight cutting recipes report parse errors, and `kohara:main` reports an unknown command. These occur in the clean baseline. Other warnings—including missing optional mixin targets/tags, spawn-placement declarations and structure-set salts—remain in the [individual warning ledger](../validation/warning-ledger.json); they are not broadly suppressed.
5. **Client-only evidence.** Original-settings client logs show startup and resource loading. The UI provider cannot select the unbundled Java process (`Invalid app` for its process name and JDK bundle identifier), so models/screens/gameplay have not been visually accepted. The [client comparison](../validation/evidence/client-warning-comparison.json) finds 463 of 467 candidate warnings/errors verbatim in the baseline without FourTwenty, including Oracle Index's unavailable ONNX native library and third-party subtitle/shader warnings. The four unmatched messages contain generated mixin/union-URL identifiers and measured startup duration; they remain individually recorded. Startup duration is not a performance benchmark.
6. **Linux.** A Docker CLI exists but its daemon socket is absent. Linux dependency resolution and platform-specific lock generation have not run.
7. **Early invalid baseline retained.** `validation/evidence/phase1/baseline-headless.txt` accidentally included main sources through NeoGradle's implicit inclusion. It is explicitly invalid; [baseline-clean](../validation/evidence/baseline-clean/native.log) replaces it. No qualification claim uses the invalid run.
8. **Standalone first-run observations.** Client resource loading completes, but the console reports a NightConfig file-watcher `NoSuchFileException` for the transient `neoforge-common.new.tmp.toml`; this remains an unclassified first-run host/framework observation. The optional `@Pseudo` mixin also logs that Actually Harvest's target class is absent in standalone/minimal profiles; it is skipped and both profiles load. Neither message is suppressed. The complete console is retained alongside the native logger output.

See [ACCEPTANCE.md](ACCEPTANCE.md) for concrete remaining scenarios. Performance is unmeasured: fixture duration is not a tick-time benchmark. Production FourTwenty adds no forced chunk tickets. Existing recipe IDs, world-generation IDs, registry IDs, effect strengths/probabilities and inventory slot numbers remain unchanged.

## Evidence index

The [evidence directory](../validation/evidence) retains each phase's results; later passes do not erase earlier failures. `inputs.json` records JAR/config inputs, `launch.json` (where available) records compiled source/resource hashes at launch, `native.log` is the complete native log, `warnings.txt` contains individual warnings/errors, and `result.json` records outcomes and post-run configuration hashes. Earlier capture-time source fingerprints are not historical launch fingerprints.

- Baseline without FourTwenty: **9** Kingdom's Gate GameTests passed.
- Standalone packaged behavior suite: **15** required tests passed, including the added stale crop-state regression.
- Minimal packaged stack: **28** required tests passed, including nine Kingdom's Gate tests.
- All-136 packaged nonplayer subset: **25** required tests passed in each of two processes (24 behavior fixtures plus a write/read checkpoint).
- Packaged diagnostic profile: **31** required tests passed with JourneyMap explicitly excluded, including disabled replanting and PlayerRevive.
- JUnit: **6** passed. Preparation guard tests: **6** passed.

Earlier runs and failures remain in the evidence directory with their original counts. None of these entries is an interactive or performance pass.

## Final packaged verification record

The clean deliverable is `build/libs/fourtwenty-0.3.2.jar`, SHA-256 **`bec76b1af3137f77224f363acb12f32766d6370930ed54931ea8bf5115b0e024`**. The immediately preceding tested artifact was `42ca90918eefcd76506ad4e3b8b3ea3352f667d984ca7783d11a48ef80b376eb`. All **119 non-directory entries** are byte-for-byte identical; the final reproducibility change excludes empty directory entries. [Payload equivalence](../validation/evidence/release-payload-equivalence.json) records every entry hash. A fresh all-JAR native smoke run and client resource-loading run use the clean deliverable itself.

| Evidence | Result and scope |
| --- | --- |
| [Clean build](../validation/evidence/clean-release-build.txt), [JUnit reports](../validation/evidence/clean-unit-tests) | `clean build` succeeds; six unit tests pass (Gradle reused the matching cached test result). |
| [Preparation tests](../validation/evidence/preparation-tests.txt) | Six guard tests pass, including altered checksums, duplicate mod IDs, stale JARs and wrong launch mode. |
| [Standalone](../validation/evidence/delivery-standalone/result.json) | 15 packaged GameTests pass; optional integrations absent. |
| [Minimal colony stack](../validation/evidence/delivery-colony/result.json) | 28 packaged GameTests pass. |
| [Targeted diagnostic pack](../validation/evidence/packaged-harvest-fixed/result.json) | 31 packaged GameTests pass; explicitly excludes JourneyMap only in this diagnostic profile. |
| [Full write checkpoint](../validation/evidence/delivery-full-write/result.json), [restart/read](../validation/evidence/delivery-full-read/result.json) | 25 required tests pass in each process with all 136 JARs; exact paid charge, inventory and elapsed tick survive disk restart. |
| [Clean deliverable smoke](../validation/evidence/release-smoke/result.json) | All 136 JARs present; 24 nonplayer GameTests pass. Dependency audit passes 333 checks over 150 resolved IDs and 44 nested groups. |
| [Full client resource loading](../validation/evidence/release-client/native.log) | Original copied settings; startup completes and texture atlases load with no remaining FourTwenty missing blockstate/model warnings. The client is stopped after this observation; this is not visual or gameplay acceptance. |
| [Client baseline](../validation/evidence/release-client-baseline/native.log) | Original copied settings, 135 JARs, FourTwenty absent; startup completes. |
| [Standalone client](../validation/evidence/release-client-standalone/native.log) | Clean packaged JAR with no optional mods; sound and all texture atlases load. First-run console observations remain listed above. |
| [Minimal colony client](../validation/evidence/release-client-colony/native.log) | Eight pinned JARs; sound, block textures, BlockUI and MineColonies GUI atlases load. |

Each run directory's `result.json` contains its exact command; the associated `inputs.json` pins its artifacts and settings. The final client comparison and remaining acceptance gates are documented separately. The frozen reference JAR hashes still match and the sibling working tree remains clean.

## Reference JAR accounting

Every row refers to its exact hash and nested dependency declarations in the frozen lock. “Coexistence” means present in the all-JAR packaged headless loading/nonplayer run, subject to the loader's side handling. It does not mean every feature of that mod was exercised. Gameplay and interactive status remain unverified except for the specifically named fixtures above. The installed old Kingdom's Gate is listed as replaced rather than falsely marked tested.

| Reference JAR | Resolved mod IDs and versions | Recorded scope |
| --- | --- | --- |
| `BHMenu-NeoForge-1.21-2.4.4.jar` | `bhmenu` 2.4.4 | Coexistence; targeted gameplay and interactive acceptance pending |
| `CBMultipart-1.21.1-3.5.0.155.jar` | `cb_multipart` 3.5.0.155<br>`cb_microblock` 3.5.0.155<br>`cb_multipart_minecraft` 3.5.0.155 | Coexistence; targeted gameplay and interactive acceptance pending |
| `CodeChickenLib-1.21.1-4.6.1.526.jar` | `codechickenlib` 4.6.1.526 | Coexistence; targeted gameplay and interactive acceptance pending |
| `CreativeCore_NEOFORGE_v2.13.28_mc1.21.1.jar` | `creativecore` 2.13.28 | Coexistence; targeted gameplay and interactive acceptance pending |
| `FarmersDelight-1.21.1-1.2.10.jar` | `farmersdelight` 1.2.10 | Coexistence; targeted gameplay and interactive acceptance pending |
| `HellishTrials-neoforge-1.0.5.jar` | `hellish_trials` 1.0.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `Jade-1.21.1-NeoForge-15.10.5.jar` | `jade` 15.10.5+neoforge | Coexistence; targeted gameplay and interactive acceptance pending |
| `L_Ender's Cataclysm 1.21.1-3.23.jar` | `cataclysm` 3.23 | Coexistence; targeted gameplay and interactive acceptance pending |
| `Platform-neoforge-1.21.1-1.2.11.6.2.jar` | `platform` 1.2.11.6.2 | Coexistence; targeted gameplay and interactive acceptance pending |
| `PlayerRevive_NEOFORGE_v2.1.0_mc1.21.1.jar` | `playerrevive` 2.1.0 | Coexistence; documented native downing/revival fixture; connected-player acceptance pending |
| `ProjectRed-1.21.1-4.22.0-beta+33-core.jar` | `projectred_core` 4.22.0-beta+33 | Coexistence; targeted gameplay and interactive acceptance pending |
| `ProjectRed-1.21.1-4.22.0-beta+33-illumination.jar` | `projectred_illumination` 4.22.0-beta+33 | Coexistence; targeted gameplay and interactive acceptance pending |
| `ProjectRed-1.21.1-4.22.0-beta+33-integration.jar` | `projectred_integration` 4.22.0-beta+33 | Coexistence; targeted gameplay and interactive acceptance pending |
| `ProjectRed-1.21.1-4.22.0-beta+33-transmission.jar` | `projectred_transmission` 4.22.0-beta+33 | Coexistence; targeted gameplay and interactive acceptance pending |
| `SpawnerControl-NEOFORGE-1.21.[0-1]-v1.4.jar` | `spawnercontrol` 1.4 | Coexistence; targeted gameplay and interactive acceptance pending |
| `Stellarity-3.0.6.1.jar` | `stellarity` 3.0.6.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `TreeChop-1.21.1-neoforge-0.19.3.jar` | `treechop` 0.19.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `UndergroundBunkers-1.0.4-1.21.x-neoforge.jar` | `underground_bunkers` 1.0.4 | Coexistence; targeted gameplay and interactive acceptance pending |
| `VanillaBackport-neoforge-1.21.1-1.1.6.3.jar` | `vanillabackport` 1.1.6.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `VanillaCookbook-2.3.2.jar` | `vanillacookbook` 2.3.2 | Coexistence; targeted gameplay and interactive acceptance pending |
| `YungsApi-1.21.1-NeoForge-5.1.6.jar` | `yungsapi` 1.21.1-NeoForge-5.1.6 | Coexistence; targeted gameplay and interactive acceptance pending |
| `YungsBetterNetherFortresses-1.21.1-NeoForge-3.1.5.jar` | `betterfortresses` 1.21.1-NeoForge-3.1.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `YungsBetterStrongholds-1.21.1-NeoForge-5.1.3.jar` | `betterstrongholds` 1.21.1-NeoForge-5.1.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `actuallyharvest-1.21.1-1.1.7.0-NEOFORGE.jar` | `actuallyharvest` 1.1.7.0 | Coexistence; native area/replant/XP and colony-boundary fixtures; interactive tools pending |
| `anvilbalance-1.1-1.21.1.jar` | `anvilbalance` 1.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `appleskin-neoforge-mc1.21-3.0.8.jar` | `appleskin` 3.0.8+mc1.21 | Coexistence; targeted gameplay and interactive acceptance pending |
| `architectury-13.0.8-neoforge.jar` | `architectury` 13.0.8 | Coexistence; targeted gameplay and interactive acceptance pending |
| `athena-neoforge-1.21-4.0.2.jar` | `athena` 4.0.2 | Coexistence; targeted gameplay and interactive acceptance pending |
| `backpacked-neoforge-1.21.1-3.0.3.jar` | `backpacked` 3.0.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `balm-neoforge-1.21.1-21.0.56.jar` | `balm` 21.0.56 | Coexistence; targeted gameplay and interactive acceptance pending |
| `basicweapons-neoforge-2.1.4+1.21.1.jar` | `basicweapons` 2.1.4+1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `betterarcheology-neoforge-1.21.1-1.3.4.jar` | `betterarcheology` 1.21.1-1.3.4 | Coexistence; targeted gameplay and interactive acceptance pending |
| `betterchunkloading-1.21-5.4.jar` | `betterchunkloading` 5.4 | Coexistence; targeted gameplay and interactive acceptance pending |
| `betterdays-1.21.1-3.3.6.2-NEOFORGE.jar` | `betterdays` 3.3.6.2 | Coexistence; targeted gameplay and interactive acceptance pending |
| `biomemusic-1.21-3.5.jar` | `biomemusic` 3.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `biomesize-1.21.1-1.6.jar` | `biomesize` 1.6 | Coexistence; targeted gameplay and interactive acceptance pending |
| `blockui-1.0.211-1.21.1-snapshot.jar` | `blockui` 1.0.211-1.21.1-snapshot | Coexistence; targeted gameplay and interactive acceptance pending |
| `bottleyourxp-1.21.1-3.5.jar` | `bottleyourxp` 3.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `brutalbosses-1.21.1-8.6.jar` | `brutalbosses` 8.6 | Coexistence; targeted gameplay and interactive acceptance pending |
| `bwncr-neoforge-1.21.1-3.20.3.jar` | `bwncr` 3.20.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `chunksending-1.21-2.9.jar` | `chunksending` 2.9 | Coexistence; targeted gameplay and interactive acceptance pending |
| `cleanswing-1.9.jar` | `cleanswing` 1.9 | Coexistence; targeted gameplay and interactive acceptance pending |
| `clickadv-1.21-3.8.jar` | `clickadv` 3.8 | Coexistence; targeted gameplay and interactive acceptance pending |
| `clientcrafting-1.21-2.1.jar` | `clientcrafting` 2.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `cloth-config-15.0.140-neoforge.jar` | `cloth_config` 15.0.140 | Coexistence; targeted gameplay and interactive acceptance pending |
| `collective-1.21.1-8.13.jar` | `collective` 8.13 | Coexistence; targeted gameplay and interactive acceptance pending |
| `comforts-neoforge-9.0.5+1.21.1.jar` | `comforts` 9.0.5+1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `connectivity-1.21.1-7.6.jar` | `connectivity` 7.6 | Coexistence; targeted gameplay and interactive acceptance pending |
| `corpse-neoforge-1.21.1-1.1.13.jar` | `corpse` 1.21.1-1.1.13 | Coexistence; targeted gameplay and interactive acceptance pending |
| `cristellib-neoforge-1.21.1-3.0.3.jar` | `cristellib` 3.0.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `crittersandcompanions-neoforge-1.21.1-2.3.4.jar` | `crittersandcompanions` 1.21.1-2.3.4 | Coexistence; targeted gameplay and interactive acceptance pending |
| `cupboard-1.21-2.9.jar` | `cupboard` 2.9 | Coexistence; targeted gameplay and interactive acceptance pending |
| `curios-neoforge-9.5.1+1.21.1.jar` | `curios` 9.5.1+1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `deepdungeon-1.1.jar` | `deepdungeon` 1.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `deeperdarker-neoforge-1.21.1-1.3.5.jar` | `deeperdarker` 1.3.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `displaydelight-1.4.0.jar` | `displaydelight` 1.4.0 | Coexistence; targeted gameplay and interactive acceptance pending |
| `domum-ornamentum-1.0.234-snapshot-main.jar` | `domum_ornamentum` 1.0.234-snapshot | Coexistence; targeted gameplay and interactive acceptance pending |
| `dragonfight-1.21-4.7.jar` | `dragonfight` 4.7 | Coexistence; targeted gameplay and interactive acceptance pending |
| `dungeons-and-taverns-v4.4.4 [NeoForge].jar` | `mr_dungeons_andtaverns` 1-v4.4.4 | Coexistence; targeted gameplay and interactive acceptance pending |
| `dusk-1.21-3.8.jar` | `dusk` 3.8 | Coexistence; targeted gameplay and interactive acceptance pending |
| `dynamicsurroundings-neoforge-1.21.1-0.4.2.jar` | `dsurround` 0.4.2 | Coexistence; targeted gameplay and interactive acceptance pending |
| `dynview-1.21-4.0.jar` | `dynview` 4.0 | Coexistence; targeted gameplay and interactive acceptance pending |
| `endrem-neoforge-1.21.X-6.0.2.jar` | `endrem` 6.0.2 | Coexistence; targeted gameplay and interactive acceptance pending |
| `entityculling-neoforge-1.9.5-mc1.21.1.jar` | `entityculling` 1.9.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `evolution-1.21.1-1.8.jar` | `evolution` 1.8 | Coexistence; targeted gameplay and interactive acceptance pending |
| `experimentalist-2.0.2+1.21.x-neoforge.jar` | `experimentalist` 2.0.2 | Coexistence; targeted gameplay and interactive acceptance pending |
| `farsight-1.21.1-5.1.jar` | `farsight_view` 5.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `fastasyncworldsave-1.21-2.6.jar` | `fastasyncworldsave` 2.6 | Coexistence; targeted gameplay and interactive acceptance pending |
| `ferritecore-7.0.3-neoforge.jar` | `ferritecore` 7.0.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `forgivingworld-1.21-4.7.jar` | `forgivingworld` 4.7 | Coexistence; targeted gameplay and interactive acceptance pending |
| `framework-neoforge-1.21.1-0.13.11.jar` | `framework` 0.13.11 | Coexistence; targeted gameplay and interactive acceptance pending |
| `geckolib-neoforge-1.21.1-4.8.3.jar` | `geckolib` 4.8.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `globalpacks-neoforge-1.21.1-21.0.6.jar` | `globalpacks` 21.0.6 | Coexistence; targeted gameplay and interactive acceptance pending |
| `gpumemleakfix-1.21-1.8.jar` | `gpumemleakfix` 1.8 | Coexistence; targeted gameplay and interactive acceptance pending |
| `handcrafted-neoforge-1.21.1-4.0.3.jar` | `handcrafted` 4.0.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `hostilevillages-1.21-5.7.jar` | `hostilevillages` 5.7 | Coexistence; targeted gameplay and interactive acceptance pending |
| `immersive_aircraft-1.4.2+1.21.1-neoforge.jar` | `immersive_aircraft` 1.4.2+1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `immersive_armors-1.7.5+1.21.1-neoforge.jar` | `immersive_armors` 1.7.5+1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `immersive_machinery-0.2.0+1.21.1-neoforge.jar` | `immersive_machinery` 0.2.0+1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `inventoryessentials-neoforge-1.21.1-21.1.13.jar` | `inventoryessentials` 21.1.13 | Coexistence; targeted gameplay and interactive acceptance pending |
| `jadecolonies-1.21.1-1.6.0.jar` | `jadecolonies` 1.6.0 | Coexistence; targeted gameplay and interactive acceptance pending |
| `jamlib-neoforge-1.3.5+1.21.1.jar` | `jamlib` 1.3.5+1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `jei-1.21.1-neoforge-19.27.0.336.jar` | `jei` 19.27.0.336 | Coexistence; targeted gameplay and interactive acceptance pending |
| `journeymap-neoforge-1.21.1-6.0.0-beta.54.jar` | `journeymap` 1.21.1-6.0.0-beta.54 | Coexistence; baseline GameTest player-join limitation; client gameplay pending |
| `leaky-1.21-2.4.jar` | `leaky` 2.4 | Coexistence; targeted gameplay and interactive acceptance pending |
| `limitedchunks-1.21-4.1.jar` | `limitedchunks` 4.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `lionfishapi-2.8.jar` | `lionfishapi` 2.8 | Coexistence; targeted gameplay and interactive acceptance pending |
| `lithostitched-1.6.1-neoforge-1.21.1.jar` | `lithostitched` 1.6.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `logprot-1.21-3.5.jar` | `logprot` 3.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `lootintegration_wda-1.7.jar` | `lootintegration_wda` 1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `lootintegrations-1.21.1-4.7.jar` | `lootintegrations` 4.7 | Coexistence; targeted gameplay and interactive acceptance pending |
| `lootintegrations_cataclysm-1.1.jar` | `lootintegrations_cataclysm` 1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `medieval_buildings_nether_edition-1.21.1-1.0.2-neoforge.jar` | `medieval_nether` 1.0.2 | Coexistence; targeted gameplay and interactive acceptance pending |
| `minecolonies-1.1.1343-1.21.1-snapshot.jar` | `minecolonies` 1.1.1343-1.21.1-snapshot | Coexistence; native farmer contract, food exclusion, permissions and cult fixtures; interactive colony pending |
| `minecolonies-cult-0.1.0.jar` | `minecolonies_cult` 0.1.0 | Installed 0.1.0 excluded; fresh pinned 0.2.0 passes nine native tests |
| `modernfix-neoforge-5.26.1+mc1.21.1.jar` | `modernfix` 5.26.1+mc1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `moredragoneggs-5.0.jar` | `moredragoneggs` 5.0 | Coexistence; targeted gameplay and interactive acceptance pending |
| `multipiston-1.2.58-1.21.1.jar` | `multipiston` 1.2.58-1.21.1 | Coexistence; six-direction native transactions and stale-access protection; interactive redstone pending |
| `oracle_index-neoforge-1.1.1.jar` | `oracle_index` 1.1.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `oritech-neoforge-1.21.1-1.1.0.jar` | `oritech` 1.1.0 | Coexistence; targeted gameplay and interactive acceptance pending |
| `overgrowth-1.3.jar` | `overgrowth` 1.3 | Coexistence; targeted gameplay and interactive acceptance pending |
| `owo-lib-neoforge-0.12.15.5-beta.1+1.21.jar` | `owo` 0.12.15.5-beta.1+1.21 | Coexistence; targeted gameplay and interactive acceptance pending |
| `particular-1.21.1-NeoForge-1.1.7.jar` | `particular` 1.1.7 | Coexistence; targeted gameplay and interactive acceptance pending |
| `polymorph-neoforge-1.1.0+1.21.1.jar` | `polymorph` 1.1.0+1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `realmrpg_skeletons-1.1.0-neoforge-1.21.1.jar` | `realmrpg_skeletons` 1.1.0 | Coexistence; targeted gameplay and interactive acceptance pending |
| `recipeessentials-1.21-4.0.jar` | `recipeessentials` 4.0 | Coexistence; targeted gameplay and interactive acceptance pending |
| `resourcefulconfig-neoforge-1.21-3.0.11.jar` | `resourcefulconfig` 3.0.11 | Coexistence; targeted gameplay and interactive acceptance pending |
| `resourcefullib-neoforge-1.21-3.0.12.jar` | `resourcefullib` 3.0.12 | Coexistence; targeted gameplay and interactive acceptance pending |
| `respawningstructures-1.21.1-4.1.jar` | `respawningstructures` 4.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `shutdowner-1.21-2.4.jar` | `shutdowner` 2.4 | Coexistence; targeted gameplay and interactive acceptance pending |
| `smallships-neoforge-1.21.1-2.0.0-b2.1.jar` | `smallships` 2.0.0-b2.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `smoothchunk-1.21-4.1.jar` | `smoothchunk` 4.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `solcarrot-1.21.1-1.16.6.jar` | `solcarrot` 1.16.6 | Coexistence; targeted gameplay and interactive acceptance pending |
| `sophisticatedcore-1.21.1-1.4.5.1499.jar` | `sophisticatedcore` 1.4.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `sophisticatedstorage-1.21.1-1.5.28.1494.jar` | `sophisticatedstorage` 1.5.28 | Coexistence; targeted gameplay and interactive acceptance pending |
| `spark-1.10.124-neoforge.jar` | `spark` 1.10.124 | Coexistence; targeted gameplay and interactive acceptance pending |
| `storagedelight-25.12.09-1.21-neoforge.jar` | `storagedelight` 25.12.09-1.21-neoforge | Coexistence; targeted gameplay and interactive acceptance pending |
| `storageracks-1.13-1.21.1-release.jar` | `storageracks` 1.13-1.21.1-release | Coexistence; targeted gameplay and interactive acceptance pending |
| `structureessentials-1.21.1-4.8.jar` | `structureessentials` 4.8 | Coexistence; targeted gameplay and interactive acceptance pending |
| `structurize-1.0.831-1.21.1-snapshot.jar` | `structurize` 1.0.831-1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `stylecolonies-1.15.54-1.21.1.jar` | `stylecolonies` 1.15.54-1.21.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `supermartijn642configlib-1.1.8-neoforge-mc1.21.jar` | `supermartijn642configlib` 1.1.8 | Coexistence; targeted gameplay and interactive acceptance pending |
| `supermartijn642corelib-1.1.20-neoforge-mc1.21.jar` | `supermartijn642corelib` 1.1.20 | Coexistence; targeted gameplay and interactive acceptance pending |
| `t_and_t-neoforge-fabric-1.13.7+1.21.1.jar` | `t_and_t` 1.13.7 | Coexistence; targeted gameplay and interactive acceptance pending |
| `tectonic-3.0.19-neoforge-1.21.1.jar` | `tectonic` 3.0.19 | Coexistence; targeted gameplay and interactive acceptance pending |
| `towntalk-1.2.0.jar` | `towntalk` 1.2.0 | Coexistence; targeted gameplay and interactive acceptance pending |
| `trashslot-neoforge-1.21.1-21.1.6.jar` | `trashslot` 21.1.6 | Coexistence; targeted gameplay and interactive acceptance pending |
| `treasuredistance-1.0.jar` | `treasuredistance` 1.0 | Coexistence; targeted gameplay and interactive acceptance pending |
| `u_sea-1.2.5+mc1.19-1.21.11.jar` | `u_sea` 1.2.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `vanillaplustools-1.21.1-1.0.5.jar` | `vanillaplustools` 1.0.5 | Coexistence; targeted gameplay and interactive acceptance pending |
| `variantsandventures-neoforge-1.0.23+mc1.21.1.jar` | `variantsandventures` 1.0.23 | Coexistence; targeted gameplay and interactive acceptance pending |
| `weathersettings-1.21-2.1.jar` | `weathersettings` 2.1 | Coexistence; targeted gameplay and interactive acceptance pending |
| `wormhole-1.1.16-neoforge-mc1.21.jar` | `wormhole` 1.1.16 | Coexistence; targeted gameplay and interactive acceptance pending |
| `yet_another_config_lib_v3-3.8.2+1.21.1-neoforge.jar` | `yet_another_config_lib_v3` 3.8.2+1.21.1-neoforge | Coexistence; targeted gameplay and interactive acceptance pending |
| `zmedievalmusic-1.21-2.1.jar` | `medievalmusic` 2.1 | Coexistence; targeted gameplay and interactive acceptance pending |
