# Dispensary service, menu and affinity plan

Date: 2026-10-18
Status: Planned. Builds on the implemented Phases 1–6 ([DISPENSARY_PLAN](DISPENSARY_PLAN.md)) and the bong tending work on `feat/bong-fuels-and-colony`.

## Objective

Make the Dispensary run like a MineColonies restaurant: the Budtender crafts a menu that grows with the Dispensary's level, walks to customers anywhere in the shop and hands them what they want, and players can buy there too. Citizens carry supplies away and use them later, so better housing means fewer trips. Each citizen gets a personal **affinity**, covering what they consume and how often. Affinity drifts with use and can end in a tolerance break.

## How it works today

| Area | Current behavior |
| --- | --- |
| Production | `JointCraftingModule` orders 16 joints at a time from its own hut until it holds the level's stock target (16/32/64/96/128). Native private crafting brings buds and paper and crafts. Joints are the only recipe. |
| Pickup request | Native crafter behavior: every inventory dump calls `createPickupRequest`, so a courier pickup is always open. The hut already keeps joints up to target, 16 buds and 32 paper, so the courier only removes extras. |
| Demand | Adults with a **home of level 3 or higher** are due once per 24,000 ticks. Satisfied: happiness factor 1.1 at weight 0.1, plus 1 heart for 24,000 ticks. Unmet: after 24,000 ticks of grace, factor 0. |
| Visit | Travel up to 600 ticks. Wait **within 5 blocks of the hut block** for up to 200 ticks; the Budtender only serves there, otherwise the customer serves themselves. Then sit or stand and smoke for 100 ticks. One joint per visit. A carried joint is used first. |
| Bongs | The Budtender keeps the front bong supplied and lit while customers are around. A burning bong gives a 6-block happiness area effect. |
| Edibles and pipes | Cookies, brownies and pipes exist as items but have no colony role. Edibles are in `minecolonies:excluded_food`, so restaurants never serve them. |

The pinned restaurant (`EntityAIWorkCook` in MineColonies 1.1.1343) has a menu module. The cook finds hungry citizens anywhere inside the building, walks to each one, and puts about 1.5 meals into their inventory, after first restocking his own inventory from the racks. Players with hut permission inside the restaurant are served the same way.

## Agreed requirements

1. **Cook-style service.** The Budtender serves due customers anywhere inside the hut and puts items into their inventory. The 5-block counter rule goes away. Self-service stays only as the no-staff fallback.
2. **Take-away by home level:**
   - Levels 1–2 smoke a joint at the shop. They are newly eligible; today they have no demand.
   - Levels 3–4 receive a **pipe** (4 uses), carry it, and use it whenever due, then return for another.
   - Level 5 smoke a joint at the shop **and** take a pipe away.
   - **No pipes in stock:** any level smokes a joint if one is available, leaves, and complains the way citizens do about a poor restaurant.
3. **Edibles count as the daily dose.** Only the Dispensary serves them; restaurants never do (keep `excluded_food`).
4. **The menu is set by Dispensary level**, not chosen by the player.
5. **Sell to players** in the shop.
6. **Affinity system.** Each citizen has a preference and a frequency:
   - Preference: edibles only, smoke only, both, or none.
   - Frequency: anywhere from several times a day to every few days.
   - Each consumption has a **10%** chance to shift frequency one step, 50/50 up or down.
   - The bong area effect gives the same roll, at most **once per day** per citizen. Shop smokers can therefore roll twice a day.
   - Reaching the top step starts a **2-day tolerance break**: a happiness debuff, no demand, then a return to the citizen's original frequency.

## Proposed defaults

These come from planning, not from explicit decisions. Change them during validation as needed; all become server config values.

### Menu by Dispensary level

| Level | Menu added | Notes |
| ---: | --- | --- |
| 1 | Joints | 1 bud + 2 paper (existing recipe) |
| 2 | Weed cookies | 1 vanilla cookie + 1 bud; cookies are requested from the colony (baker) |
| 3 | Pipes | Existing recipe: 3 plank types + charcoal + bud; 4 uses |
| 4 | Weed brownies | Existing recipe: cocoa + wheat + bud |
| 5 | Shop sessions | Level 5 homes smoke at the shop and take a pipe away |

The stock target for each item scales with level, like today's joint target. Example: level 3 keeps 64 joints, 16 cookies and 8 pipes.

### What a customer gets

| Home level | Smoker | Edible-only | Both |
| --- | --- | --- | --- |
| 1–2 | Joint at the shop | 1 edible eaten at the shop | Best item in stock: brownie, cookie, joint |
| 3–4 | Pipe to carry (4 doses) | 4 edibles to carry (brownies first) | Best take-away in stock: brownies, pipe, cookies |
| 5 | Joint at the shop + pipe to carry | Edible at the shop + 4 to carry | Best on-the-spot item + best take-away |

**"Both" citizens take whatever is in stock, preferring the higher-level menu item.** Ranking: brownie (4), pipe (3), cookie (2), joint (1).

**Fallbacks:**
- **Take-away item out of stock:** the citizen gets an on-the-spot joint or edible if one is in stock, and raises a "poor dispensary" interaction.
- **Nothing at all in stock:** keep the current "unavailable" behavior.

**Using carried supplies:**
- A carried pipe or edible is used whenever the citizen is due, wherever they are, as today's carried joints are. Each use removes 1 pipe durability or 1 edible.
- **Edibles stay out of meals:** they are never counted as food, and the existing exclusion tag keeps native eating from choosing them.

### Affinity scale

| Step | Meaning | Interval (ticks) |
| ---: | --- | ---: |
| 0 | No interest (no demand, no penalty) | — |
| 1 | Every 4 days | 96,000 |
| 2 | Every 2 days | 48,000 |
| 3 | Daily | 24,000 |
| 4 | Twice a day | 12,000 |
| 5 | Three times a day | 8,000 |
| 6 | Four times a day | 6,000 |
| 7 | Over the limit: tolerance break | — |

**New citizens**
- Preference is assigned once:
  - none 20%
  - smoke 35%
  - edibles 20%
  - both 25%
- The starting step is 1–5, weighted toward 3.
- Both are drawn from a seed stable to the citizen's UUID, so a reload never re-rolls.

**Drift**
- **Roll triggers:** each consumption, and the first bong session of each 24,000-tick window, roll 10% for a change, then 50/50 up or down.
- **Range:** steps are clamped at 0 and 7.
- **No interest is reachable but rare:** step 0 can only be reached by drifting down, and only the bong roll can bring a citizen back up from it.
- **Fixed preference:** only the frequency drifts.

**Tolerance break**
- Reaching step 7 starts a 48,000-tick break.
- During the break: happiness factor 0.5 at weight 0.1, no demand, no unmet penalty, no affinity rolls.
- The citizen shows a "taking a tolerance break" interaction.
- Afterwards, the step returns to the citizen's original value.

**Due timing**
- `dueAt` is the last consumption plus the current step's interval.
- Grace before the unmet penalty equals that interval. A missed dose never accumulates, as today.
- The health heart keeps its 24,000-tick duration and refreshes on each dose.

### Players

- **Who gets served:** players inside the shop are served like the cook serves them.
- **Payment:**
  - Players with colony hut permission get menu items free (restaurant parity).
  - Other players pay emeralds, taken from their inventory when served:

    | Item | Price |
    | --- | ---: |
    | Joint | 1 emerald |
    | Weed cookie | 1 emerald |
    | Weed brownie | 2 emeralds |
    | Pipe (4 uses) | 3 emeralds |

  - Prices are server config values.
- **Interaction:** standing in the shop and right-clicking the Budtender opens a small choice of the current menu.

## Phases

Each phase is a separate branch-sized package with its own tests and handoff note, following the earlier phase docs.

### Phase 7: Cook-style service

- **Serving:**
  - The Budtender restocks his own inventory from the racks.
  - Each AI tick he scans the hut's citizens for due customers without supplies, walks to each and transfers the item.
  - `serveCustomer` drops the 5-block `atCounter` check. Customers wait anywhere inside the building, with arrival checked against the building bounds.
- **No more constant pickup:** override `isAfterDumpPickupAllowed` to false so dumping no longer opens a courier pickup every time. The hut still keeps its stock; extras leave on the native schedule or with a manual pickup.
- **Players:**
  - Serve players inside the hut: free with permission, an emerald otherwise.
  - Add the right-click menu choice.
- **Tests:** serve-anywhere (native fixture), the no-pickup-on-dump check, and the player payment rules (pure).

### Phase 8: Menu, pipes, edibles and housing tiers

- **Menu and crafting:**
  - Add a `DispensaryMenu` rule: Dispensary level → allowed items and stock targets.
  - Generalize `JointCraftingModule` to teach the recipes for the current menu, with one production request tree per item.
  - The cookie input is requested from the colony like buds and paper.
- **Visits:**
  - Extend the visit by home level as shown in the table.
  - **Joint at the shop:** at level 5, and as the out-of-stock fallback, the citizen smokes a joint at the shop with today's flow.
  - **Pipe:** a carried pipe is used anywhere when due, with an emote and a puff, and loses durability. The citizen returns once it breaks.
  - **Edibles:** a carried edible is eaten when due. It never touches saturation or food history.
- **Fallbacks:** apply the fallback rules and add the "poor dispensary" interaction.
- **Restaurant exclusion:** confirm edibles stay excluded from restaurants (existing tag) with a native test.
- **Schema:** none; this phase reuses demand v1.
- **Tests:**
  - menu by level (pure)
  - tier selection, including fallbacks (pure)
  - pipe durability and edible consumption crediting exactly one dose (native)
  - restaurant never serving edibles (native)

### Phase 9: Affinity and tolerance

- **Save format v2:** add `preference`, `baseStep`, `step`, `breakUntil` and `lastBongRoll` to each record.
- **Migration from v1** (live worlds hold v1 saves):
  - existing records keep their deadlines
  - each existing citizen draws their affinity on first load
  - the old "reject unknown version" check stays for anything newer than v2
- **Intervals:** affinity steps drive `dueAt` and the grace interval. Step 0 and the tolerance break suppress demand and penalty.
- **Rolls:**
  - after each successful `consume`
  - from the bong session listener at most once per window
  - with a randomness source that tests can inject
- **Happiness:** a new `TOLERANCE_BREAK` happiness status at factor 0.5.
- **Visibility:** add tooltips showing preference, frequency and break status in the citizen window and the Dispensary status tab.
- **Config:** add server settings for all of these defaults.
- **Tests:**
  - assignment distribution and UUID stability
  - drift odds over many rolls
  - clamping, the tolerance break and its reset
  - v1→v2 migration round-trip
  - the bong roll once-per-window limit

## Risks and checks

- **Self-service shortcut:** the restaurant serves only citizens inside the building. Customers who stop at the door must still enter, so arrival uses the building bounds, with self-service after the wait deadline.
- **Many customers at once:** heavy-affinity colonists could visit several times a day; carried pipes keep that bounded. Watch courier load and the cost of bud production.
- **Pipe stacking:** pipes don't stack (durability), so each takes a citizen inventory slot. A citizen keeps at most one pipe at a time.
- **Save migration:** v2 must load every v1 save written so far. Back up `fourtwenty_dispensary.dat` before the first v2 deploy.
- **Shared jar:** registry or happiness additions (the new happiness status, menu UI) need the same jar on client and server.

## Decisions (2026-10-18)

1. Player prices are set per item (table above): the pipe is cheaper per use, and the brownie is premium.
2. "Both" citizens take whatever is in stock, preferring the higher-level item: pipe over joint, brownie over cookie.
3. A tolerance break returns the citizen exactly to their original step.
