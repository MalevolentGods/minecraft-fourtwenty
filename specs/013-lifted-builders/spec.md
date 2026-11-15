# Feature Specification: Lifted Builders and Higher Ground

**Feature Branch**: `013-lifted-builders`
**Created**: 2026-11-09
**Status**: Draft
**Input**: User description: "Lifted builders and Higher Ground: smarter, faster, flying MineColonies Builders." (full text in the conversation of 2026-11-09; summarised below)

**Approved names** (user, 2026-11-09): **Lifted** (the builder's buff) and **Higher Ground** (the way a Lifted builder builds).

Everything in this feature applies only when MineColonies is installed. Nothing else in Four Twenty depends on it, and a world without MineColonies sees no change.

## Background

Players see two problems with MineColonies' Builder:

1. **The crawl:** a Builder slows to about one block every few seconds, even at top skills. She goes back to normal speed only after the player pushes her right off the block she's standing on.
2. **Getting trapped:**
   - Upgrades never clear the old building first. She rebuilds it in place from the bottom up, so she works inside a half-old, half-new building and gets trapped.
   - Seen on Tavern upgrades and on the Warehouse 2 to 3 upgrade, on the stairs down to the centre.

Reading MineColonies' code points to a loop behind the crawl:
- When the next block is in her own column, she steps one block aside but keeps her old standing spot.
- So every block costs her a fresh route search back to that spot.

This has to be confirmed in game before it is patched.

This feature first fixes walking Builders. It then adds the fun part: a Builder who smokes a joint becomes **Lifted**, flies, and builds the **Higher Ground** way. That gives colonies a use for their spare weed.

## Clarifications

### Session 2026-11-09

- Q (2026-11-14, after in-game testing): Should Higher Ground clear an upgrade first? → A: **No.** Rebuilding in place is how MineColonies upgrades are meant to work. Clearing first was dropped: she flies and finds her own way, so the old building no longer traps her, and its long clearing stage kept a Warehouse upgrade from placing anything for hours. The clear-first acceptance scenarios and requirement below are withdrawn; the storage-contents rule still applies to any block she breaks.

- Q: Does Lifted also make her place blocks faster? → A: Yes, modestly: about 25% faster for Landrace, rising with potency up to about 50% for Golden God (in the config).
- Q: When joints are short, who gets them first: citizens' daily joints or Lifted builders? → A: Citizens first; a Builder's joint request is only filled from stock above what citizens are owed today.
- Q: What happens to the contents of a storage block that an upgrade removes? → A: She takes them and delivers them to the colony's Warehouse (her own hut if there is none); whatever doesn't fit drops at her hut.
- Q: Does "fly when Lifted" start on or off? → A: On by default, for new huts and for existing colonies after the update (safe because builders only get spare joints).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Builders keep their pace (Priority: P1)

A player queues a build or an upgrade. The Builder places blocks at the speed her skills allow from start to finish. She never drops into the crawl, and the player never has to push her off a block to get her going again.

**Why this priority**: it is a bug every colony meets on every big job, and it makes upgrades take many times longer than they should. It is also the fallback for every Builder who isn't Lifted.

**Independent Test**: on the test server, with logging on, run the upgrades where the crawl was seen (Warehouse 2 to 3 and a Tavern upgrade). Then run them again with the fix and compare blocks placed per minute and how many route searches she makes per block.

**Acceptance Scenarios**:

1. **Given** a Builder whose next block is in her own column, **When** she steps aside to place it, **Then** she carries on from a new standing spot out of the way, and does not search for a route back to the old one for each block.
2. **Given** an upgrade where the crawl used to happen, **When** it runs with the fix, **Then** her pace stays within the normal range for her skills (SC-001), with no help from the player.
3. **Given** the logging switched on, **When** a Builder works, **Then** the log shows her route searches and step-asides, enough to confirm or rule out the loop.

---

### User Story 2 - A watchdog rescues stuck Builders, and only them (Priority: P1)

When a Builder is truly stuck or crawling while she builds, the colony gets her going again by itself. A Builder who is busy with anything else is never touched: eating, going to the restaurant, sleeping, fetching materials, waiting for a delivery, or hiding from a raid.

**Why this priority**: the patch fixes one known loop, and MineColonies has other ways of getting stuck (walls going up around her, odd stair shapes). The watchdog catches whatever remains.

**Independent Test**: trap a building Builder in a sealed box on the test server and check that she is rescued within the stuck time. Then send a Builder to eat, and to fetch materials, for longer than the stuck time, and check that she is left alone.

**Acceptance Scenarios**:

1. **Given** a Builder who is building but placing far slower than her skills allow, **When** this lasts past the slow-time setting, **Then** she is given a fresh standing spot beside her next block (what the player does by hand today).
2. **Given** a Builder who is building but has made no progress for the stuck time (about 45 seconds by default), **When** the watchdog checks, **Then** she is moved to a free standing spot near her next block. If there is none, she is moved to one just outside the building. As a last resort she is recalled to her hut.
3. **Given** a Builder who is eating, at the restaurant, sleeping, fetching materials, waiting for a delivery or in a raid, **When** any amount of time passes, **Then** the watchdog never acts on her.
4. **Given** any rescue, **When** it happens, **Then** it is written to the server log with the Builder, the building, where she was, and what was done.
5. **Given** the server config, **When** an admin turns the watchdog off or changes its times, **Then** the change applies without editing anything else.

---

### User Story 3 - Lifted: a Builder on a joint flies (Priority: P2)

A colony with weed to spare lets its Builders smoke on the job.
1. A Builder asks for joints through the colony's normal requests, just as she asks for food, and a Dispensary, a Warehouse or the player fills them.
2. When she starts a job, and again whenever the buff runs out mid-job, she lights a joint and becomes **Lifted**.
3. While Lifted she flies: she hovers about the site trailing a little smoke, and shimmers faintly green, like a see-through green Golden God.
4. She never needs a walking route, so she can't get trapped.

**Why this priority**: it is the headline fun and the colony's weed sink. It needs the request flow, flight, the shimmer and the buff. It builds on Part 1's fixes but doesn't depend on them to be tested.

**Independent Test**: give a Builder joints, queue a build, and watch her light up, fly, shimmer and smoke-trail through the job. Turn the hut switch off and check she walks and builds as before.

**Acceptance Scenarios**:

1. **Given** a Builder with "fly when Lifted" on and no joints, **When** she has a job, **Then** she asks for joints through the colony's requests, and a Dispensary, a Warehouse or the player can fill them.
2. **Given** a Builder with a joint and a job, **When** she starts building, **Then** she smokes one joint and becomes Lifted for its strain's Lifted time (FR-010).
3. **Given** a Lifted Builder, **When** she works, **Then** she flies to where the work is without walking routes, trails a little smoke, and shows the green shimmer to every player who can see her.
4. **Given** a Lifted Builder mid-job, **When** the buff runs out, **Then** she smokes another joint if she has one and carries on. If she has none, she comes down gently and carries on walking.
5. **Given** a Builder who is mid-air, **When** she stops being Lifted for any reason (runs out, the switch is turned off, the job ends, the chunk unloads, the server restarts), **Then** she is lowered gently to the ground and takes no damage.
6. **Given** the hut switch is off, **When** the Builder works, **Then** she neither asks for nor smokes joints for Lifted, and builds as an ordinary Builder.

---

### User Story 4 - Higher Ground: building that makes sense (Priority: P2)

A Lifted Builder builds the Higher Ground way:
- **Speed:** she places blocks about 25% faster on Landrace, up to about 50% faster on Golden God.
- **Reach:** her reach is longer, about 8-10 blocks instead of 4-5.
- **Hover points:** she picks a spot above or beside the work and places everything she can reach from there before she moves on.
- **Upgrades:** she first removes what is changing, from the top down, and then builds the new level from the bottom up. She is never working inside a half-old building.

**Why this priority**: it is what makes Lifted worth the weed. A Lifted Builder finishes jobs noticeably faster, and upgrades stop trapping her.

**Independent Test**: run the same upgrade twice, Warehouse 2 to 3 and a Tavern upgrade, once with a Lifted Builder and once with a walking Builder of the same skills. Compare the time to finish, how often she moves, and that the Lifted run never stalls.

**Acceptance Scenarios**:

1. **Given** a Lifted Builder, **When** she builds, **Then** she places every block within her Higher Ground reach of her hover point before flying to the next one.
2. **Given** a Lifted Builder upgrading a building, **When** she starts, **Then** she first removes the blocks that the new level changes or no longer has, from the top down, and only then builds the new level from the bottom up.
3. **Given** a block that holds items (a rack, a chest) that the upgrade keeps, **When** she clears for the upgrade, **Then** it is left in place with its contents.
4. **Given** a storage block with items that the upgrade removes, **When** she clears it, **Then** she takes the items and delivers them to the colony's Warehouse (her own hut if there is none), and anything that doesn't fit drops at her hut. Nothing spills where the block was.
5. **Given** the hut block, **When** she clears, **Then** it is never removed.

---

### User Story 5 - Lifted costs weed, balanced (Priority: P3)

Keeping a Builder Lifted takes a fair amount of weed, without being crazy:
- **Rate:** about one joint per five minutes of flying, so 3-4 joints a game day for a busy Builder.
- **Strains:** better strains last longer.
- **Supply:** one weed field can keep a couple of Builders flying, but not for free.
- **Tuning:** the numbers are set in the config and checked against the balance model.

**Why this priority**: it is the tradeoff that keeps it fair, and it puts the colony's spare joints to use.

**Independent Test**: run the balance model with Lifted Builders added to the colony's demand, and check the weed field's spare against two busy Builders.

**Acceptance Scenarios**:

1. **Given** the default config, **When** a Landrace joint is smoked, **Then** the Builder is Lifted for about five minutes. Better strains last longer, by family and potency.
2. **Given** the balance model with two busy Lifted Builders, **When** it runs, **Then** one field still covers the colony's own joints, with Builders taking a clear share of the spare.
3. **Given** an admin, **When** they change the Lifted times or turn Lifted off in the config, **Then** the change applies.

---

### Edge Cases

- **Not building:** a Lifted Builder who stops building (end of job, mealtime, bedtime, a raid) lands gently. She keeps whatever buff time is left; nothing is wasted mid-air.
- **Chunk unloads:** a Builder Lifted in a chunk that unloads, or a server restart, comes back on the ground, never stuck in the air.
- **Lifted buff vs. a normal high:**
  - Lifted is separate from the normal high.
  - Joints a Builder smokes as an ordinary citizen (Dispensary demand) don't make her Lifted.
  - Smoking for Lifted also counts as having smoked for the colony's happiness and demand, the same as any citizen joint, so she isn't double-billed.
- **Short of joints:** when the stock only covers what citizens are owed today, the Builder's request waits. She builds walking until spare joints turn up, and the colony's happiness never suffers for her flight.
- **Getting there:** flying never takes her through solid blocks. If no flight path exists, she falls back to walking, and the watchdog covers her.
- **Hover points:** a hover point inside the building's footprint must not end up sealed in as the building rises. She moves to her next point before the blocks around her close up.
- **Leaving the site:** flying is only for her own build site and the trip there. She never flies off to restaurants, homes or elsewhere.
- **Other workers:** only Builders can become Lifted, and no other worker is affected.
- **Without MineColonies:** nothing changes.
- **Old saves:** a colony from before this feature loads unchanged, except that its Builders' huts get "fly when Lifted" switched on. They only ever take spare joints (citizens first), so nobody's daily joint is spent on flight.

## Requirements *(mandatory)*

### Functional Requirements

**Part 1: walking Builders**

- **FR-001**: The feature MUST include switchable diagnostic logging of a Builder's route searches, step-asides and standing-spot changes. It is used to confirm the crawl's cause on the test server before the fix is written.
- **FR-002**: When a Builder steps aside from her own column to place a block, she MUST carry on from a new standing spot out of the way. She MUST NOT search for a route back to the old spot for each following block.
- **FR-003**: The watchdog MUST only watch a Builder while her work task is building: placing blocks, breaking a block in the way, or walking to her build site. It MUST NEVER act while she is eating, going to or at the restaurant, sleeping, fetching or waiting for materials, waiting for a delivery, in a raid, or doing anything else.
- **FR-004**: The watchdog MUST judge progress by the build itself (blocks placed or removed, the build moving on), not by whether she moves.
- **FR-005**: The watchdog MUST step in at two levels:
  1. **Slow:** when she is far slower than her skills allow for longer than the slow time, she gets a fresh standing spot beside her next block.
  2. **Stuck:** after the stuck time with no progress, she is moved to a free standing spot near her next block. If there is none, she is moved just outside the building. As a last resort she is recalled to her hut.
- **FR-006**: Every watchdog rescue MUST be logged with the Builder, the building, where she was, and what was done.
- **FR-007**: The watchdog's on/off switch, slow time and stuck time MUST be in the server config.
- **FR-008**: The Sensi Heights Warehouse levels 1-4 MUST have the hidden one-block air pockets under the top steps of the centre stairs filled in. The stairs' looks must not change.

**Part 2: Lifted and Higher Ground**

- **FR-009**: Each Builder's hut MUST have a "fly when Lifted" switch, on by default, both for new huts and for huts in colonies from before the update.
- **FR-010**: With the switch on, a Builder MUST ask for joints through the colony's normal requests, as she does for food, so that a Dispensary, a Warehouse or the player can fill them. Joints are the only thing she smokes for Lifted. Citizens come first: a Dispensary or Warehouse MUST fill a Builder's request only from joints above what the colony's citizens are still owed today. Joints a player drops in the Builder's hut are always hers to smoke; the citizens-first rule applies only to the colony's Dispensary and Warehouse stock.
- **FR-011**: A Builder with the switch on and a joint MUST smoke one and become Lifted:
  - **When:** when she starts building, and when her Lifted buff runs out while she is still building.
  - **How long:** the Lifted time is longer than a normal high: about five minutes for Landrace by default, longer for better families and higher potency. All times are in the config.
- **FR-012**: A Lifted Builder MUST fly while she is building: she moves through the air around her build site without walking routes and without passing through solid blocks.
- **FR-013**: A Lifted Builder MUST show, to every player who can see her:
  - a faint smoke trail;
  - a semi-transparent green shimmer on her body, a green, see-through version of the Golden God glow.
- **FR-014**: A Builder who stops being Lifted, or stops building, while in the air MUST be lowered gently to the ground and take no damage from it. This covers every cause, including the switch going off, the job ending, a chunk unloading and a server restart.
- **FR-015**: A Lifted Builder MUST use Higher Ground:
  1. a reach of about 8-10 blocks (in the config);
  2. hover points from which she places everything within reach before moving on;
  3. on upgrades, removing what is changing from the top down before building bottom-up;
  4. faster placing: blocks go down about 25% faster for Landrace, rising with the smoked strain's potency to about 50% for Golden God. Both ends are in the config.
- **FR-016**: Higher Ground MUST keep everything MineColonies' Builder already protects:
  - the hut block;
  - blocks the blueprint says to keep (placeholders, keep-terrain);
  - blocks that already match the blueprint;
  - storage blocks the new level keeps, with their contents.

  It MUST also need the same materials as an ordinary build. When the Higher Ground clearing step removes a storage block that holds items, she MUST take the items and deliver them to the colony's Warehouse (her own hut if there is none). Anything that doesn't fit drops at her hut, never where the block stood.
- **FR-017**: A Lifted Builder with no joint left when her buff ends MUST land and carry on building as an ordinary walking Builder, with Part 1's fixes.
- **FR-018**: A joint smoked for Lifted MUST also count as a citizen joint for the colony's happiness and demand.
- **FR-019**: The config MUST hold the Lifted times by family and potency, the placing speed boost (lowest and highest), and an on/off switch for Lifted as a whole. The defaults MUST be checked with the balance model (FR-020).
- **FR-020**: The balance model MUST include Lifted Builders' joint use in the colony's daily demand, and show the spare left with two busy Lifted Builders.

### Key Entities

- **Builder watch:** for each Builder, what she is doing, when the build last moved on, her pace, and any rescues made. It is not saved, because it starts fresh after a restart.
- **Lifted:** a Builder-only buff with the strain smoked and the time left. While it lasts she flies and builds the Higher Ground way. It is saved with the citizen, so a restart lands her and resumes the time left.
- **Fly when Lifted:** a setting on each Builder's hut, on by default.
- **Lifted times:** config values for the Lifted time by family and potency, and the master switch.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a Warehouse 2 to 3 upgrade and a Tavern upgrade on the test server, a walking Builder with the fix keeps a pace within 25% of her skill's normal placing rate (MineColonies' own placing delay for her skills and research; see research R3) for the whole job, with no player help. Before the fix, the crawl is about one block every few seconds.
- **SC-002**: A Builder trapped in a sealed box while building is building again within the stuck time plus 10 seconds.
- **SC-003**: Over a full in-game day of normal colony life on the test server, the watchdog acts on zero Builders who were eating, sleeping, at the restaurant, fetching materials or in a raid.
- **SC-004**: A Lifted Builder finishes the Warehouse 2 to 3 upgrade at least twice as fast as a walking Builder with the same skills, and never stalls.
- **SC-005**: No Builder takes fall damage, or is left in the air, in any of the landing cases (buff runs out, switch off, job ends, chunk unload, restart).
- **SC-006**: With the default numbers, the balance model shows one weed field covering a mid-size colony's own joints with two busy Lifted Builders, and still leaving a spare.
- **SC-007**: Without MineColonies installed, Four Twenty loads and plays exactly as before.

## Assumptions

- **Version:** the MineColonies version is 1.1.1343, as on the servers. The crawl's cause comes from reading that version's code and is confirmed by FR-001 before it is fixed.
- **Watchdog defaults:** stuck time 45 seconds; slow time 20 seconds at under a third of her normal rate.
- **Lifted defaults:**
  - **Times:** Landrace 5 minutes; each step up the ladder adds time, up to about 10 minutes for Golden God.
  - **Cost:** about 3-4 joints a game day for a Builder building all day. Final numbers come from the balance model.
- **Flight:** her speed is about her walking speed. Flying is for her build site and the trip to it only.
- **Requests:** joints come through the same channels as citizen joints today (Dispensary, Warehouse, player).
- **Who gets it:** only Builders for now. Other workers (Miners, Woodcutters) may come later and are out of scope.
- **The shimmer:** it re-uses the Golden God glow's look in green and see-through. It is seen by every player in sight.
- **Warehouse pockets:** filling them is a Sensi Heights pack change, shipped through the Four Twenty jar like every pack update.

## Out of Scope

- Other workers flying or becoming Lifted.
- Changing what MineColonies' Builder builds, its materials, or its request rules beyond joints.
- Reporting the crawl to MineColonies upstream. Worth doing once confirmed, but not part of this feature.
