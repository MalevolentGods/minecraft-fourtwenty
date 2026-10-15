# Dispensary and Budtender implementation plan

Date: 2026-10-14  
Status: Phase 1 implemented and validated; see [the Phase 1 handoff](DISPENSARY_PHASE1.md) for exact evidence and limits. Phase 2 production is implemented; see [the Phase 2 handoff](DISPENSARY_PHASE2.md) for native logistics evidence and remaining qualification checks. Phases 3–6 remain planned.

## Objective

Extend FourTwenty's optional MineColonies integration with a five-level Dispensary and a Budtender profession. Adult residents of level 3 or higher residences develop cannabis demand, collect joints through a Dining Hall-style service, and may smoke at the dispensary. Consumption contributes to happiness and grants a small temporary maximum-health benefit.

Reuse MineColonies' building, worker, crafting, requests, couriers, navigation, seating, and happiness systems wherever practical. The pinned Dining Hall implementation contains food-specific checks, so the dispensary needs its own building and citizen task built around those native patterns.

## Agreed requirements

- The Dispensary has five completed building levels and employs the new Budtender profession.
- Demand applies to adult citizens whose assigned residence has completed level 3 or higher, including eligible guards and unemployed adults. Children do not participate.
- Budtenders craft and distribute the existing joint item. Its current recipe consumes one weed bud and two paper per joint.
- Citizens visit, receive an item, optionally use seating, consume, and resume normal activities in a manner closely matching the Dining Hall.
- Consumption grants a modest happiness benefit. Persistent unmet demand causes a modest happiness penalty.
- Consumption grants **+2 maximum health**, equivalent to one heart. Repeated applications refresh the duration without stacking.
- Blueprints use a rustic timber-and-stone style, growing from a small shop into a larger dispensary and lounge.
- MineColonies remains optional for FourTwenty. The integration must coexist with Kingdom's Gate.

## Proposed gameplay defaults

The following values came from the planning proposal rather than explicit user selections. They are initial implementation defaults and should be recorded if changed during validation.

| Mechanic | Proposed default |
| --- | --- |
| Demand interval | One joint per 24,000 elapsed simulation ticks, equivalent to one ordinary Minecraft day |
| First demand | Begins when an adult first becomes eligible; a queued residence upgrade does not qualify |
| Initial penalty grace | 24,000 ticks from becoming eligible |
| After consumption | Satisfied for 24,000 ticks, followed by a 24,000-tick grace period before an unmet-demand penalty |
| Health duration | 24,000 ticks from completed consumption |
| Health application | Increase maximum health only; do not instantly fill the additional heart |
| Consumption location | Prefer available dispensary seating; use a safe nearby fallback when seating is unavailable |
| Existing personal stock | Consume a carried joint when due before making another collection trip |
| Supply failure | Report the need, return to normal activity, and retry after a bounded delay |
| Loss of eligibility | Stop demand and penalties; do not confiscate carried items |
| Staffing | One Budtender at every completed level; multiple dispensaries per colony are supported |

Demand timing uses elapsed simulation time rather than midnight boundaries. Sleep, daylight changes, and `/time` commands must not produce duplicate consumption opportunities. Server downtime does not advance simulation time. Temporary visits are revalidated after loading; health and satisfaction expiry must use the same persisted timing policy.

Exact happiness strengths will be calibrated in Phase 4 against the native weighted calculation. A modifier factor is not a flat number of happiness points. A successful implementation must demonstrate that satisfaction raises happiness and unmet demand lowers it, subject to native limits.

| Completed dispensary level | Indoor seats | Default joint stock target | Blueprint development |
| --- | ---: | ---: | --- |
| 1 | 2 | 16 | Small shop and counter |
| 2 | 4 | 32 | Expanded preparation and storage area |
| 3 | 6 | 64 | Dedicated lounge |
| 4 | 8 | 96 | Expanded shop and lounge |
| 5 | 10 | 128 | Full dispensary complex |

Stock targets are replenishment targets, not customer limits or inventory slot capacities. Higher levels improve service capacity without increasing the joint's health benefit. A level 1 dispensary can serve adults in level 3 or higher residences.

## Baseline and boundaries

The planning investigation inspected MineColonies `1.1.1343-1.21.1-snapshot`, the current sibling Kingdom's Gate source, and the installed MC-TechColonies inventory. At that inspection, the 135 artifacts in the compatibility lock were unchanged and the installed directory also contained FourTwenty, for 136 JARs total. Recheck the installation and sibling revision at implementation time rather than treating this observation as a permanent inventory.

The existing target is Minecraft 1.21.1, Java 21, NeoForge 21.1.218, Structurize 1.0.831, BlockUI 1.0.211, Domum Ornamentum 1.0.234, Multi-Piston 1.2.58, and TownTalk 1.2.0. Use the [compatibility lock](../validation/compatibility-lock.json), [development workflow](DEVELOPMENT.md), and [qualification report](COMPATIBILITY.md) for exact artifacts and evidence requirements.

Preserve existing registry IDs, joint recipes, player effects, crops, bong behavior, and save data. Cookies and brownies remain excluded from citizens' ordinary food selection. Cannabis demand must not alter hunger, food history, meal quality, or restaurant menus.

Hunger, medical care, sleep, raids, combat, and active Kingdom's Gate rituals take priority over dispensary visits. Missing cannabis must never indefinitely block employment or essential citizen behavior. Use native pathfinding without teleportation or forced chunk loading.

Testing uses disposable isolated worlds and copies of the pack. This plan does not include replacing installed mods, changing player worlds, editing the sibling project, committing, publishing, or releasing.

## Delivery approach

Complete the phases in dependency order. Each phase is a bounded package with its own verification and handoff; passing one phase does not establish acceptance of later phases. Persistence contracts begin in Phase 1 and are implemented alongside each feature, with comprehensive lifecycle qualification in Phase 6.

At each handoff, record changed files, behavior delivered, commands run, evidence paths, known limitations, and remaining work. Keep unit tests, native GameTests, observed client behavior, dedicated-server behavior, performance, hosted CI, and release status separate. No phase is complete merely because the project builds.

## Phase 1: Integration foundation and gameplay rules

**Outcome:** A safely optional integration boundary and tested rules for eligibility, demand, and consumption timing.

**Work:**

- Recheck the pinned dependencies, installed inventory, and current Kingdom's Gate AI and happiness hooks. Record the implementation baseline without silently replacing checksum pins.
- Add an isolated MineColonies integration package and the required compile-time dependencies. Keep optional API classes out of eagerly loaded FourTwenty core signatures.
- Gate integration registration and mixins on MineColonies availability, and define the supported version range from the inspected API.
- Define stable identifiers for the dispensary, Budtender, cannabis satisfaction, health attribute modifier, and versioned citizen state.
- Implement pure eligibility and timing rules: adulthood, assigned residence, completed level, due time, satisfaction expiry, grace expiry, and eligibility transitions.
- Use the native home assignment to resolve the residence. Verify how guard housing is represented; do not substitute a guard's workplace level for the required residence level.
- Define a persisted citizen record keyed by colony and citizen identity, including enough state to prevent duplicate consumption benefits after an orderly save/reload.
- Initialize existing eligible citizens with the normal grace period. Define configuration validation and the effect of later configuration changes.

**Verification and acceptance:**

- Unit tests cover residence levels 0–5, queued upgrades, adulthood, homelessness, reassignment, eligibility loss, and exact timing boundaries.
- Verify elapsed-time behavior across sleep and daylight changes, with no midnight-based extra dose.
- Verify standalone FourTwenty and MineColonies-enabled startup on client and server paths, including optional class loading.
- Preserve the existing cookie/brownie food exclusion and ordinary food behavior.

**Handoff:** Integration skeleton, documented state/configuration contract, rule tests, and startup evidence. Building service is not yet implemented.

## Phase 2: Dispensary building and Budtender production

**Depends on:** Phase 1.

**Outcome:** A buildable, staffed dispensary that obtains ingredients and crafts joints through native colony logistics.

**Work:**

- Register the hut block/item, colony building, block entity, Budtender job, worker module, and standard building controls.
- Support native hiring, dismissal, construction, upgrades, repairs, storage, minimum stock, and building synchronization.
- Expose the hut's inventory through the normal NeoForge item-handler capability required by native worker pickup and couriers.
- Add crafting support for the existing three-column joint recipe from level 1. Ensure the crafting module supports its actual grid dimensions.
- Request buds and paper through MineColonies' request system, deduplicate outstanding requests, and replenish toward the completed level's joint stock target.
- Preserve the exact recipe inputs and output. Define ingredient/output retention so courier pickup does not empty the service supply unintentionally.
- Provide a functional level-1 blueprint with a reachable hut, preparation area, storage, and seating markers.
- Make the Budtender's own needs compatible with the job so the worker cannot deadlock waiting to serve themselves.

**Verification and acceptance:**

- Verify registration, hiring, dismissal, completed-level limits, capabilities, and save/reload.
- Test missing ingredients, full output storage, outstanding requests, and exact material conservation during crafting.
- Observe actual courier delivery and autonomous Budtender crafting in an isolated colony.
- Confirm two dispensaries maintain independent staff, requests, and inventories.

**Handoff:** Functional level-1 production building, native logistics evidence, and remaining service limitations.

## Phase 3: Citizen collection and smoking behavior

**Depends on:** Phases 1–2.

**Outcome:** Eligible citizens autonomously collect and consume joints, then resume their normal activities.

**Work:**

- Add a dedicated citizen task following the pinned `EntityAIEatTask` service pattern: check inventory, find dispensary, travel, wait for service, receive a joint, find a seat, consume, and finish.
- Use native building lookup and navigation, preferring suitable staffed dispensaries and supporting multiple buildings in one colony.
- Implement Budtender customer handling with native inventory-transfer utilities. Revalidate eligibility, demand, proximity, and available inventory space at handoff.
- Adapt the Dining Hall's bounded waiting and self-collection fallback where appropriate. An empty or inaccessible dispensary must release customers to normal AI with a retry delay.
- Use blueprint seating tags and native sitting support. Handle occupied seats and safe fallback consumption positions without trapping citizens.
- Display the held joint and a short timed smoking interaction with smoke particles. Award consumption credit only after a completed action consumes exactly one joint.
- Keep item transfer distinct from consumption: an interrupted customer retains a transferred joint but receives no benefit until consumption completes.
- Yield to essential native tasks and Kingdom's Gate rituals. Clean up navigation, seating, held-item visuals, and customer reservations on interruption.

**Verification and acceptance:**

- Test competing customers, the last available joint, full inventories, no staff, no stock, no seats, unreachable buildings, and building removal during a visit.
- Verify interrupted consumption, repeated callbacks, and existing personal stock cannot duplicate items or grant unearned benefits.
- Observe employed adults, unemployed adults, and eligible guards complete visits; children and residents below level 3 do not participate.
- Observe return to work or normal idle behavior and correct preemption by hunger, illness, sleep, raids, and rituals.

**Handoff:** Complete collection/consumption behavior, item-conservation evidence, and observed AI transitions. Final happiness and health tuning follows in Phase 4.

## Phase 4: Happiness and health integration

**Depends on:** Phases 1–3.

**Outcome:** Completed consumption produces the agreed benefits, while prolonged unmet demand produces a modest native happiness penalty.

**Work:**

- Register a distinct cannabis satisfaction modifier and initialize it on both server and client before happiness deserialization, following the pinned handler's requirements.
- Represent ineligible, grace, satisfied, and unmet states without overwriting food, health, or Kingdom's Gate modifiers.
- Calibrate configurable happiness strengths against the weighted native formula. Test low and high baseline happiness; simply choosing factors above and below one is insufficient evidence of the intended direction.
- Refresh cached happiness and synchronize the citizen view on consumption, expiry, and eligibility changes. Retain the native happiness cap.
- Apply a uniquely identified +2 maximum-health modifier at completed consumption. Refresh its expiry without stacking or granting immediate healing.
- Preserve unrelated attribute modifiers and prevent native citizen attribute recalculation from dropping or duplicating the benefit.
- Remove the cannabis modifier on expiry and safely clamp current health to the resulting maximum. Persist remaining entitlement consistently across reloads and chunk unloading.
- Add localized citizen-facing explanations for satisfaction, pending demand, and unmet demand.

**Verification and acceptance:**

- Verify happiness direction and magnitude across representative baseline states, with and without Kingdom's Gate membership and research bonuses.
- Confirm the visible happiness entry matches authoritative server state after reconnect and reload.
- Test one-heart magnitude, duration refresh, no stacking, native attribute recalculation, unrelated buffs, and safe expiry at elevated current health.
- Confirm collection alone grants no benefit and existing player joint effects remain unchanged.

**Handoff:** Documented happiness tuning, health behavior, synchronization evidence, and lifecycle tests for both benefits.

## Phase 5: Five finished blueprints and player-facing UI

**Depends on:** Phases 2–4.

**Outcome:** A complete five-level building progression with discoverable controls and readable service status.

**Work:**

- Finish five timber-and-stone blueprints using the proposed seating progression and progressively larger preparation, storage, and lounge areas.
- Keep a consistent hut anchor and entrance. Record each footprint and the largest reserved upgrade area before finalizing the structures.
- Provide accessible routes between the entrance, hut, storage, counter, work area, and indoor seats.
- Package the designs as a FourTwenty Structurize pack with a Dispensary category and stable level paths. Document the exact Build Tool selector sequence.
- Add the hut's recipe, models, textures, icons, names, and Budtender appearance using the native registration and rendering conventions.
- Add a status tab showing completed level, staffing, ingredients, joints, stock target, customers, and operating problems. Retain standard MineColonies management tabs.
- Add concise player instructions explaining housing eligibility, supply requirements, consumption, health duration, and unmet demand. Keep contributor detail in development documentation.

**Verification and acceptance:**

- Validate all five blueprint resources, hut anchors, seating tags, supported rotations, and mirror combinations.
- Preview and construct every level, and observe upgrades and repairs through native builder work orders.
- Verify actual worker and customer access after construction, including occupied seats and rain.
- Inspect the hut UI, citizen status, Budtender appearance, smoke effects, and resource reload in a real client.

**Handoff:** Finished assets, stable blueprint paths, footprint documentation, player documentation, and visual/construction evidence.

## Phase 6: Persistence and compatibility qualification

**Depends on:** Phases 1–5.

**Outcome:** A packaged implementation with explicit automated and observed evidence, plus clearly recorded remaining limitations.

**Work:**

- Complete versioned persistence and migration checks for citizen demand, consumption, benefit expiry, building stock, worker assignment, and outstanding requests.
- Rebuild temporary visits and customer reservations from live conditions after loading. Do not restore stale navigation or replay already completed consumption.
- Exercise housing reassignment, colony changes, citizen death, building removal, dismissal, and eligibility loss. Keep state isolated between colonies and server instances.
- Test chunk unloading, reconnects, orderly shutdown, fresh-process restart, and time changes during travel, waiting, transfer, smoking, satisfaction, and penalty states.
- Run standalone FourTwenty, minimal MineColonies, MineColonies with the current Kingdom's Gate build, and the full installed pack in isolated profiles.
- Verify the full supply chain: farmer-grown cannabis, native transport, Budtender crafting, customer collection, consumption, and return to ordinary activity.
- Recheck existing farming permissions, Actually Harvest, bong automation, player effects, and edible exclusion for regressions.
- Use final packaged JARs for client and ordinary dedicated-server acceptance. Record any diagnostic exclusions separately from full-pack results.
- Profile practical colony populations against a matched baseline, checking request volume, pathfinding, tick time, and cleanup after unloading.
- Update the compatibility report, acceptance checklist, and artifact/evidence records without overwriting earlier results.

**Verification and acceptance:**

- Build, unit tests, relevant native GameTests, and dependency checks pass for the recorded source and artifacts.
- Fresh-process restart preserves stock and citizen state without extra items, duplicate benefits, or repeated completed consumption.
- Real client and dedicated-server observations demonstrate building progression, logistics, citizen visits, smoking, UI synchronization, and AI recovery.
- Kingdom's Gate recruitment, happiness, rituals, and return-to-work behavior coexist with the new task.
- Original pack settings and all relevant dependencies are represented in full-pack acceptance; a diagnostic subset is labeled as such.
- Performance evidence records the tested population, configuration, duration, baseline, and limitations. Hosted CI and release status are reported independently.

**Handoff:** Packaged JAR and hash, completed phase checklist, exact reproduction commands, evidence locations, migration notes, and explicit outstanding qualification items. Release publication remains a separate action.

## Phase checklist

- [ ] Phase 1: Integration foundation and gameplay rules
- [ ] Phase 2: Dispensary building and Budtender production — implemented; qualification limits in [handoff](DISPENSARY_PHASE2.md)
- [ ] Phase 3: Citizen collection and smoking behavior
- [ ] Phase 4: Happiness and health integration
- [ ] Phase 5: Five finished blueprints and player-facing UI
- [ ] Phase 6: Persistence and compatibility qualification

## References

- [Development and isolated runs](DEVELOPMENT.md)
- [Compatibility report](COMPATIBILITY.md)
- [Existing acceptance scenarios](ACCEPTANCE.md)
- [Frozen compatibility inventory](../validation/compatibility-lock.json)
- [Existing joint recipe](../src/main/resources/data/fourtwenty/recipe/weed_joint.json)
- [MineColonies Dining Hall documentation](https://minecolonies.com/wiki/buildings/cook/)

Native reference points inspected during planning include `BuildingCook`, `EntityAIWorkCook`, `EntityAIEatTask`, `FoodUtils`, `CitizenAI`, and `CitizenHappinessHandler` from the pinned MineColonies JAR, plus Kingdom's Gate's building registration, inventory capability, citizen AI, and happiness integration. The pinned implementation is authoritative when current online documentation differs.
