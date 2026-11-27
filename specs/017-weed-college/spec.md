# Feature Specification: The Weed College

**Feature Branch**: `017-weed-college`
**Created**: 2026-11-27
**Status**: Draft
**Input**: User description: "Weed College (spec 017; MineColonies only; buildings plan Phase 6). A new MineColonies hut, fourtwenty:weedcollege, worked by a new job, the Weedologist. Built the way the Bud Barn and Trim Daddy are, with its own hut block model diorama. Its research is colony-wide and physical: the Weedologist takes a study off the College's board, gets its supplies through colony requests, plants, tends, harvests and processes on Think Tanks and Grow Ops inside the hut, and writes it up; a study always succeeds, a bad batch only adds time. The College keeps its own research tree and progress, separate from the University (the University Researcher works a random entry of the colony's whole in-progress list, so College studies must never progress there). Six branches: Botany, Chemistry, Glassworks, Textiles, Engineering, Armory; about 50 to 60 studies, grown from the existing Think Tank studies. Tree shape agreed 2026-10-30: hue rows, not 80 single colours. A colony can unlock a colour nobody has caught; researching a colour is slow and costly so the Infuser stays the quick route; a caught colour makes its research cheaper. Timing like the University's: tiers 1 to 6 take about 1.2, 2.4, 5, 10, 19 and 38 hours. The College's level caps the research tier; five levels, each unlocking the next Grow Op tier; blueprints for all five levels in the Sensi Heights style. The Handbook gains a Weed College page. Nothing beyond Four Twenty and MineColonies; Four Twenty still plays without MineColonies."

## Context

Four Twenty's colony side has a shop (the Dispensary), a trimming hut (the Bud Barn) and a growing processing site (the Weed Lab), but no research of its own: a colony can't learn anything about weed that the player hasn't already worked out at the Inspiration Infuser. The Weed College is that missing piece, a place where a colony slowly and visibly studies weed, colour and craft, and where what it learns belongs to the whole colony.

Two facts shape it:

- **It is separate from the University.** MineColonies' University Researcher picks a random study from everything the colony has in progress, from any branch (checked in MineColonies' code, 2026-11-27). If the College's studies sat in the University's tree, University scholars would do them and the Weedologist would have nothing to do. The College therefore has its own board, its own progress and its own worker; the University never sees it.
- **It is physical.** Research is something the Weedologist is seen doing: fetching supplies, working the plots and benches in the hut, writing it up. It never ticks away on its own.

## Clarifications

### Session 2026-11-27

- Q: What does a colony-researched colour give the players? → A: Only the colony's own workers can use it (the Weed Lab's Terpinators, the Tie Dye Shack). Players still catch their own colours at the Inspiration Infuser; the College never teaches a player.
- Q: What are the Engineering and Armory branches for? → A: Engineering is the colony's power and machines (Weed Lab machine speed, Hookah and Stash efficiency, Grow Op upkeep). Armory is gear: the Weedscout uniform and the **Kush Suit** power armour (Hotbox Helm, Puff Plate with its Puff Pack jetpack, Couchlock Greaves, Moon Boots) with the **Blazer** laser, as in the weed-tech brainstorm (2026-10-30). Each piece of gear is its own later feature; its studies wait, locked, until it ships.
- Q: No colony worker uses colours yet (the Weed Lab's Terpinators are ordinary player-run machines; the Tie Dye Shack doesn't exist), so what does a finished colour study do while 017 is the only part built? → A: Keep FR-021 as is: colour studies are built in 017 and pay off when the Tie Dye Shack (spec 018) uses them; 017 and 018 ship together in one release.
- Q: How are the College's five levels built? → A: All five by design script now; the user may replace any level with a hand build before release, the scripted one being a placeholder until then.
- Q: The "ultimate terpene" breaks Principle II; what tops Chemistry? → A: **Master Blend**: the colony's Terpinators need no BI. Terpinator effects are power-only throughout. The 63 study names in data-model.md are approved as written.
- Q: What does a finished Botany strain study give the colony? → A: Its harvest goes to the warehouse as a batch of that strain's seeds, which the colony's farmers can plant and anyone can take like any warehouse item; yield studies make the colony's weed farms give more buds.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A colony's first study (Priority: P1)

The player builds a Weed College and hires a Weedologist. They open the College, pick one of the tier-1 colour studies from its board and start it. The Weedologist asks the warehouse for the study's supplies, carries them in, plants and tends a crop on the plots inside the hut, harvests it, runs it through the hut's Think Tank, and finally sits down to write it up. Some time later the study is finished and the colony knows that colour.

**Why this priority**: it proves the whole idea end to end: a separate research board, a worker who does the research with his hands, supplies through the colony's normal requests, and a colony-wide result. Everything else is more studies on the same machinery.

**Independent Test**: in a colony with a level 1 College, a hired Weedologist and a stocked warehouse, start one colour study, watch the Weedologist work it through every step, and confirm that at the end the colour is known to the colony and the study shows as done; check that the University's research window never lists it.

**Acceptance Scenarios**:

1. **Given** a built College with a Weedologist and nothing studied yet, **When** the player opens the College's board, **Then** it shows the College's branches and the tier-1 studies as available, each with its supplies, its length and what it gives.
2. **Given** a chosen study, **When** the Weedologist starts it, **Then** he requests its supplies from the warehouse through the colony's normal request system, and the study waits, with its reason shown, until they arrive.
3. **Given** the supplies are in the hut, **When** the Weedologist works, **Then** he visibly plants, tends and harvests a crop on the hut's plots, uses the hut's Think Tank, and writes up at a desk, and the study's progress rises only while he does this work.
4. **Given** a study in progress, **When** a unit of work turns out to be a bad batch, **Then** that unit is worked again (the study takes longer) and the study never fails or loses its supplies.
5. **Given** the study's last unit is done, **When** it is written up, **Then** the colony knows the result, the colony is told, and the study shows as done on the board.
6. **Given** the colony also has a University, **When** a College study is in progress, **Then** the University's researchers never work it and its window never lists it.

---

### User Story 2 - Choosing what to study (Priority: P1)

The player plans the colony's research. The College's board shows the whole tree: six branches, each a set of studies in tiers, with arrows from each study to the ones it unlocks. Studies the colony can start now are marked as available; those that need a parent study, a higher College level or a later feature are shown locked, with the reason. The player can queue studies, see the one in progress with its progress and what it is waiting for, and cancel one that hasn't started.

**Why this priority**: without a readable board the research has no shape; the player must be able to plan dozens of hours of study.

**Independent Test**: open the board in a new College and in one at level 3 with some studies done; check that availability, locks, reasons, the queue and progress all match the rules below.

**Acceptance Scenarios**:

1. **Given** a level 1 College, **When** the board opens, **Then** only tier-1 studies are startable and higher tiers show "needs a College of level N".
2. **Given** a study whose parent isn't done, **When** the player looks at it, **Then** it shows locked, naming the parent study.
3. **Given** a queue of studies, **When** one finishes, **Then** the Weedologist takes the next one in order without the player doing anything.
4. **Given** a queued study that hasn't started, **When** the player removes it, **Then** it leaves the queue and nothing is lost.

---

### User Story 3 - Studying the hemp colours, strains and chemistry (Priority: P2)

Once the colours of the common dyes are known, the colony moves up: tier 2 and 3 studies each research a row of hemp colours (one study unlocks four colours), Botany studies teach the colony biome strains and hybrids, and Chemistry studies speed up the colony's Terpinators and lead to an ultimate terpene at the top of the tree.

**Why this priority**: this is where the College becomes the colony's long game, but it rests on Stories 1 and 2.

**Independent Test**: with a level 3 College and the needed parents done, run one hemp-row study and one Botany study, and check that their effects appear colony-wide as described.

**Acceptance Scenarios**:

1. **Given** a finished hemp-row study, **When** it is written up, **Then** the colony knows all four colours of that row.
2. **Given** a colour that a member of the colony has already caught at the Infuser, **When** the colony studies it, **Then** that study costs less (fewer supplies and less time) than the same study for an uncaught colour.
3. **Given** a finished Botany strain study, **When** it is written up, **Then** a batch of that strain's seeds is in the warehouse; **Given** a finished yield study, **Then** the colony's weed farms give more buds.

---

### User Story 4 - A College that grows (Priority: P2)

The College has five levels, from a shed with a chalkboard to a modern quad with a greenhouse. Each level is a proper Sensi Heights building, and each lets the College study one more tier and gives its plots the next Grow Op tier. Its hut block, in the hand and on the build tool, is a little diorama of its own.

**Why this priority**: the College is a building the player looks at for dozens of hours; it must look the part and grow with its research.

**Independent Test**: build all five levels in a test colony; check each level's look, footprint and required blocks, that its plots are of the right Grow Op tier, and that the board's tier cap follows the level.

**Acceptance Scenarios**:

1. **Given** a College at level N, **When** the board opens, **Then** studies up to tier N are startable (tier 6 at level 5, see FR-016).
2. **Given** a College at level N, **When** it's built, **Then** its plots are of Grow Op tier N (Closet Grow at level 1 up to Cosmic Kush Pod at level 5).
3. **Given** the hut block, **When** it is held or placed, **Then** it shows the College's own diorama model, not a borrowed MineColonies hut.

---

### User Story 5 - Reading about it (Priority: P3)

The Handbook's Colony Life chapter gains a page on the Weed College, in the book's voice: what it is, how to start, what the branches are and how long things take.

**Why this priority**: the College is a big, slow system; players need to know what they're signing up for.

**Independent Test**: open the Handbook with MineColonies installed and find the page; open it without MineColonies and confirm it is hidden.

**Acceptance Scenarios**:

1. **Given** MineColonies is installed, **When** the player opens Colony Life, **Then** a Weed College page is there, linked from the contents and the index.
2. **Given** MineColonies is not installed, **When** the player opens the Handbook, **Then** the page and every link to it are hidden.

### Edge Cases

- **The Weedologist dies, quits or is fired mid-study**: the study keeps its progress and supplies; the next Weedologist picks it up where it stopped.
- **The College is demolished or downgraded**: progress on the board is kept with the colony; a study above the new level's tier pauses, with the reason shown, until the College is back at that level.
- **Supplies never arrive**: the study waits, shows what it is waiting for, and nothing is lost or failed.
- **The colony is unloaded** (no player nearby): research pauses, like the University's, and resumes when the colony is loaded again.
- **A player expects to learn a colour the colony researched**: they don't; the board and the Handbook page say colony colours are for the colony's workers, and the Infuser is how a Weedcrafter learns one.
- **A colour the colony studies is later caught at the Infuser, or the other way round**: knowing a colour twice changes nothing and costs nothing extra.
- **A study belongs to a feature that doesn't exist yet** (Textiles needs the Tie Dye Shack, Glassworks the Glassblower's bongs): it is shown locked, naming what it waits for, until that feature ships.
- **Several Colleges in one colony**: they share the colony's one board and progress (see FR-008).
- **MineColonies is not installed**: there is no College, the Think Tank keeps working for players as today, and Four Twenty loads and plays normally.

## Requirements *(mandatory)*

### Functional Requirements

**The hut and its worker**

- **FR-001**: The mod MUST add a MineColonies hut, the Weed College, with five levels, worked by a new job, the Weedologist, and present only when MineColonies is installed.
- **FR-002**: The College MUST have its own hut block model (a diorama of the building), used for the block and its item; never a borrowed MineColonies hut model.
- **FR-003**: The College's window MUST show its status (the study in progress, its progress and what it is waiting for), the board, and the usual hut controls, as the Bud Barn's does.
- **FR-004**: A College MUST employ one Weedologist per level 1 and 2, and two from level 3 up, working the same study together.

**The board and its progress**

- **FR-005**: The College MUST keep its own research board and progress, separate from the University's tree: University researchers MUST never see, list or progress a College study, and College progress MUST come only from Weedologists' work.
- **FR-006**: The board MUST hold six branches: Botany, Chemistry, Glassworks, Textiles, Engineering and Armory, with about 50 to 60 studies in six tiers in all.
- **FR-007**: Each study MUST have: a branch, a tier, zero or more parent studies, a list of supplies, a length, and the effect it gives when done; the board MUST show all of these.
- **FR-008**: Research MUST be colony-wide: what is studied, in progress or done belongs to the colony, is shared by every College the colony has, and is saved with the colony.
- **FR-009**: The player MUST be able to start a study, queue several, see the queue and remove a queued study that hasn't started; the Weedologist works the queue in order.
- **FR-010**: A study's length MUST follow the University's timing: about 1.2, 2.4, 5, 10, 19 and 38 hours of the Weedologist's work for tiers 1 to 6, and MUST progress only while the colony is loaded and a Weedologist is working it.
- **FR-011**: A study MUST always succeed: a bad batch (about one unit in ten) only means that unit is worked again.

**How a study is worked**

- **FR-012**: The Weedologist MUST request a study's supplies from the warehouse through the colony's normal request system and wait for them, showing what is missing.
- **FR-013**: The Weedologist MUST visibly do the research inside the hut: plant and tend a crop on the hut's Grow Op plots, harvest it, process it on the hut's Think Tank, and write the study up at a desk; progress MUST come from this work and nothing else.
- **FR-014**: Each tier-1 colour study MUST reuse the existing Think Tank study's supplies and length, so the College and a player's own Think Tank agree on what a colour study is.

**Levels**

- **FR-015**: The College's level MUST cap the tier a study can be: a level N College can start studies of tier N and below.
- **FR-016**: Tier 6, the top of the tree, MUST need a level 5 College and every tier-5 study in its branch.
- **FR-017**: The plots in a level N College MUST be of Grow Op tier N: Closet Grow, Grow Tent, Hydro Bed, Aero Pod, Cosmic Kush Pod.
- **FR-018**: Each of the five levels MUST have a blueprint in the Sensi Heights style (earth tones, the user's signature door from level 3 up, doors saved closed), passing the pack's footprint, required-block and door checks; all five are scripted, and any level MAY be replaced before release by a showpiece the user builds by hand.

**What the research gives**

- **FR-019**: The tree MUST research colours as rows, not one colour at a time: tier 1 the sixteen common colours in a few groups, tiers 2 and 3 the sixteen hemp-colour rows, each row unlocking its four colours.
- **FR-020**: A colony MUST be able to research a colour that no player has caught; doing so MUST be slower and costlier than catching it at the Infuser, and a colour already caught by a member of the colony MUST make its study cheaper.
- **FR-021**: A colour the colony has researched MUST be usable by the colony's own workers only: the Weed Lab's Terpinators may make its terpene and the Tie Dye Shack may dye with it. Researching it MUST NOT teach any player the colour; players still catch their own at the Inspiration Infuser.
- **FR-022**: A finished Botany strain study (a biome strain or a hybrid) MUST deliver the study crop's seeds of that strain to the warehouse, a batch the colony's farmers can plant and anyone can take like any warehouse item; Botany yield studies MUST make the colony's weed farms give more buds.
- **FR-023**: Chemistry studies MUST give colony-wide Terpinator effects that change power only (constitution Principle II): the colony's Terpinators draw half their BI (Lean Extraction), then a quarter (Full Spectrum), and none at tier 6 (**Master Blend**, approved by the user 2026-11-27 in place of an "ultimate terpene", which would have broken Principle II).
- **FR-024**: Glassworks studies MUST unlock bong tiers for the colony (the Budtender stocks them); Textiles studies MUST unlock tie-dye patterns for the Tie Dye Shack (spec 018). Until those features ship, their studies MUST show locked, naming what they wait for.
- **FR-025**: Engineering studies MUST give colony-wide power and machine effects: faster Weed Lab machines, more efficient Hookahs and Stashes, and cheaper Grow Op upkeep. Armory studies MUST cover gear: the Weedscout uniform, and the Kush Suit power armour (Hotbox Helm, Puff Plate with the Puff Pack, Couchlock Greaves, Moon Boots) with the Blazer laser; a finished Armory study lets the colony make that piece (its crafters can build it and the warehouse can supply it). Each piece is its own later feature, so its Armory study MUST show locked, naming what it waits for, until that piece ships.
- **FR-026**: When a study is done, the colony MUST be told (as MineColonies announces a finished University study), and the board MUST show it done.

**Everything else**

- **FR-026b**: This feature MUST ship in the same release as the Tie Dye Shack (spec 018), the first colony worker to use the colours the College researches; the board MUST say what a colour study is for (the Tie Dye Shack's dyes) so a finished one never reads as doing nothing.
- **FR-027**: The Handbook's Colony Life chapter MUST gain a Weed College page in the book's voice (needs MineColonies), with the index and glossary updated, and the PDF rebuilt.
- **FR-028**: Nothing in this feature may depend on any mod beyond Four Twenty and MineColonies (and MineColonies' own requirements); Four Twenty MUST still load and play without MineColonies, with the Think Tank working as today.
- **FR-029**: All player-facing text MUST be in the language files and follow the Handbook's voice rules where it is prose.

### Key Entities

- **Weed College**: a colony building with a level (1 to 5), its plots, Think Tank and desk, and its Weedologists.
- **Weedologist**: the College's worker; carries supplies, tends plots, runs the Think Tank, writes studies up.
- **Branch**: one of six groups of studies (Botany, Chemistry, Glassworks, Textiles, Engineering, Armory).
- **Study**: an entry on the board: branch, tier, parents, supplies, length, effect, and whether it waits on a later feature.
- **Colony research record**: per colony, each study's state (locked, available, queued, in progress, done), its progress, and the queue.
- **Colony knowledge**: what the colony's finished studies give it: known colours, strains, and the effects of each branch.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a test colony with a stocked warehouse, a tier-1 colour study finishes in 1.2 hours of the Weedologist's work (±10%, bad batches aside), with every step (fetching, planting, harvesting, the Think Tank, writing up) seen at least once.
- **SC-002**: Over a full study with a University running in the same colony, University researchers progress College studies 0 times, and the University's window lists none of them.
- **SC-003**: Every study on the board can be reached: from a new colony, following parents and levels, all 50 to 60 studies (bar those waiting on later features) can be started and finished.
- **SC-004**: A player new to the College can start their first study within 2 minutes of opening the College, using the board and the Handbook page alone.
- **SC-005**: Removing a Weedologist, unloading the colony or restarting the server mid-study loses 0 progress and 0 supplies in 10 out of 10 tries.
- **SC-006**: All five College levels pass the pack's blueprint checks (footprint, required blocks, closed doors) with no errors.
- **SC-007**: Four Twenty with no MineColonies installed loads, and the Handbook and Think Tank behave as before, in the existing compatibility test runs.

## Assumptions

- **One board per colony**: all of a colony's Colleges share one board, one queue and one set of progress; the study in progress is worked by every Weedologist the colony has.
- **Timing counts the Weedologist's working time**, as the University's counts its researchers'; nights, meals and sleep pause it like any job.
- **The Think Tank stays the player's research machine**: its tier-1 colour studies are unchanged, and a player can still study colours themselves without a colony.
- **Who "the colony's members" are** for a cheaper study (FR-020): the colony's owner and officers, by MineColonies' own permissions.
- **Bad batch chance** stays the Think Tank's, about one unit in ten, in config.
- **Blueprints are made by a design script** in the Sensi Heights repo like the other huts, all five levels (clarified 2026-11-27); the user may swap any level for a hand build before release; the hut ships as drafted and is play-tested in normal play.
- **Study list**: the exact 50 to 60 studies, their supplies and effects are drawn up in planning, inside the shape above, and reviewed by the user before they're built.
- **Tie Dye Shack (spec 018), the Glassblower's bongs, the Weedscout uniform, the Kush Suit and the Blazer** are separate work; this spec only gives them their studies, locked until they exist.
- **Players without MineColonies still get the gear**: as decided 2026-10-30 ("research runs on its own block, independent of MineColonies"), the Kush Suit and the Blazer also get a player route through the Think Tank, specified with the gear itself; the College's Armory is the colony's route.
- **Vials the colony makes** follow today's rule for any vial: a colony-made vial a player is handed teaches its colour like any other. The College itself teaches no one (FR-021). (To revisit in clarify if colony vials should not teach.)
