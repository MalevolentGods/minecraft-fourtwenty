# Changelog

All notable changes to the FourTwenty mod will be documented in this file.

## [0.6.0] - Dispensary service, affinity, bongs and lamps

### Added
- **Restaurant-style Dispensary service:**
  - The Budtender walks to customers anywhere in the shop.
  - Player sales: right-click the Budtender with an emerald for a server-driven menu. It is free with hut permission; otherwise joint 1, cookie 1, brownie 2 and pipe 3 emeralds.
- **Dispensary menu by level:** joints (1), weed cookies (2), pipes (3) and weed brownies (4).
  - Production is balanced: one small batch at a time for the least-stocked item.
  - A raw bud reserve is kept for bong sessions and the hookah.
  - The status tab lists each menu item and any missing ingredient, and its icon is the bud art.
- **Take-away by home level:**
  - Levels 1–2 consume at the shop, 3–4 take a pipe or 4 edibles home, and 5 do both.
  - Carried joints, edibles and pipes are used anywhere when due. A pipe holds 4 doses.
  - Missing take-aways fall back with a complaint.
- **Colonist affinity:**
  - The first exposure (hookah area, secondhand smoke, a dose) rolls a preference and a frequency step.
  - Drift: 10% per dose and per daily hookah session, 75% of it upward.
  - A two-day tolerance break follows reaching the top step.
  - The happiness tab shows the profile.
- **Glass bong:** a placeable block; right-click with a bud for the pipe's effects.
- **Six specialty bongs** (glass bong upgrades) with one-day buffs; the Phantom bong grants flight.
  - Level 5 smokers take their shop session at the job-matching specialty bong: +5 to its two skills for a day, +10 on a primary-skill match.
- **Hookah fuels:** flint and steel (30 s), lava bucket (45 s) and blaze powder (60 s).
  - The "Hookah session" happiness bonus applies within 6 blocks.
  - The Budtender tends the front hookah: free water, fuel, buds while customers are around, and requests for fuel and buds.
- **Lamps:** Lamp, Inverted Lamp and Lamp Trim in 16 colours, with ProjectRed's illumar textures and halo.
  - Halos merge between touching lamps.
  - Lit lamp items glow in inventories.
- **Bundled Sensi Heights Dispensary** (levels 1–5) and a 3D two-block counter hut block.
  - `tools/import_dispensary_blueprints.py` re-imports and validates the levels, and converts ProjectRed lamps and CB covers to FourTwenty lamps.
- **Builder costs:** froglights cost a sea lantern, and CB Multipart microblocks cost their source block at 1 per 8 covers.
- **Op-only `/fourtwenty` testing commands:** status, expose, forget, due, clear, break, drift, clock and stock. Also the in-game test playbook.

### Changed
- The Weed Bong is displayed as the **Hookah**. Ids and saves are unchanged.
- **Dispensary demand:** any completed home level qualifies after a first exposure. The save format is now v3; v1 and v2 load, with every colonist unexposed.
- The Budtender no longer requests a courier pickup after every dump.
- FourTwenty potion effects hide their swirl particles.

### Fixed
- Weed crops maintain farmland.
- The hookah block faces its placer.
- The Budtender's bong tending no longer depends on the AI tick landing on a multiple of 20.
- Workers keep carried doses when emptying their inventory.
- Budtenders get no take-away from their own shop.

### Credits
- Lamp textures and halo rendering are adapted from [ProjectRed](https://github.com/MrTJP/ProjectRed) (MIT License, see `licenses/ProjectRed-LICENSE.txt`).

## [0.3.2] - Compatibility candidate

- Target Minecraft 1.21.1 and NeoForge 21.1.218 with consistent packaged version metadata.
- Repair crop and wild-weed loot, remove duplicate seed paths, and provide sustainable farmer replanting.
- Add validated bong automation, paid-charge persistence, legacy recovery, Multi-Piston movement, and complete blockstate models.
- Scope delayed edible effects to the running server and player lifecycle; exclude edibles from citizen food selection.
- Protect FourTwenty crops from Actually Harvest bypassing MineColonies permissions, including area harvests.
- Reject stale post-event crop interactions and preserve seeds when Actually Harvest disables replanting.
- Add isolated pack tooling, dependency inventory, JUnit and native integration fixtures. Full qualification remains subject to the compatibility report's open gates.

### Earlier interface work in 0.3.2

### Added
- **Custom Bong Menu Art**: Beautiful cannabis-themed GUI texture
  - Custom background with cannabis leaf decorations
  - Integrated title design eliminating need for overlay text
  - Professional visual presentation matching mod theme

### Fixed
- **Weed Slot Input Limitation**: Resolved bug allowing unlimited weed bud input
  - Properly enforced 4-bud maximum limit in weed input slot
  - Prevented excess items from being consumed while maintaining count
  - Enhanced container validation for reliable slot restrictions
- **GUI Title Overlap**: Removed redundant title text since it's now part of the artwork

### Technical
- **Slot Validation**: Improved `canPlaceItem` method for stricter input control
- **Container Management**: Enhanced stack size validation per slot type
- **Screen Rendering**: Cleaned up label rendering to prevent text overlap

## [0.3.1] - GUI System Overhaul

### Changed
- **Weed Bong Interface**: Complete redesign from block state to GUI-based system
  - **Brewing Stand-like GUI**: Right-click bong opens inventory interface with 3 slots
  - **Slot System**: Dedicated slots for weed buds, water bottles, and blaze powder
  - **Automatic Processing**: Auto-lights when all required items are present
  - **Simplified Interaction**: No more manual lighting - just add items and get effects
  - **Blaze Powder Fuel**: Replaces flint & steel for more intuitive brewing stand parallel
  - **Area Effects**: Maintained 4-block radius group session benefits
  - **Visual Feedback**: Clean interface without manual interaction buttons

### Technical
- **Menu System**: Added WeedBongMenu with Container interface implementation
- **Client GUI**: WeedBongScreen for rendering and interaction
- **Registry Updates**: ModMenuTypes for custom menu registration
- **Inventory Management**: Full item persistence and automatic consumption

## [0.3.0] - Stationary Equipment

### Added
- **Weed Bong Block**: Stationary smoking apparatus with advanced mechanics
  - Multi-state block with visual indicators for water, weed level, and lit status
  - Interactive system using water buckets, weed buds, and flint & steel
  - Area-of-effect benefits for nearby players (4-block radius)
  - Enhanced effects for group sessions (stronger and longer-lasting)
  - 5-second cooldown system to prevent spam usage
  - 15-second burn duration per lighting
  - Automatic weed consumption (1 bud per burn cycle)
  - Campfire smoke particle effects when active
  - Sound effects for lighting, bubbling, and ambient fire
  - Crafting recipe using glass blocks and water bucket
  - State persistence (remembers water/weed levels when reloaded)

### Changed
- **Group Gameplay**: Introduced social mechanics encouraging communal use
- **Effect Scaling**: Bong provides stronger effects than individual consumption methods

## [0.2.0] - Edibles & Food Integration

### Added
- **Weed Cookies**: Basic tier edible with delayed effects
  - Shapeless crafting recipe (cookie + weed bud)
  - 10-second delayed onset for realistic edible experience
  - 3-minute effect duration (longer than smoking)
  - Provides food nutrition (2 hunger + 2.4 saturation)
  - Moderate effect profile with standard weed benefits
  - Visual and audio feedback during consumption

- **Weed Brownies**: Premium tier edible with enhanced effects
  - Shaped crafting recipe requiring cocoa, wheat, egg, sugar, and weed bud
  - 15-second delayed onset
  - 5-minute effect duration with premium effects
  - Enhanced nutrition (4 hunger + 4.8 saturation)
  - Includes Jump Boost II and Speed I alongside standard effects
  - More complex crafting requirements for balance

- **Delayed Effect System**: Advanced timing mechanism for edibles
  - Server tick-based DelayedEffectManager for precise timing
  - UUID-based player tracking for multiplayer compatibility
  - Overdose prevention (cannot consume multiple edibles while effects pending)
  - Thread-safe operation using ConcurrentHashMap
  - Two-tier system supporting different delay and duration profiles

### Changed
- **Crop Drop System**: Improved sustainability and balance
  - Weed seeds now have 5% drop chance instead of guaranteed drops
  - Immature crops still drop 1 seed for replanting sustainability
  - Implemented direct popResource() drops for reliable functionality
  - Enhanced crop breaking mechanics for better farming experience

### Fixed
- **Loot Table Issues**: Resolved inconsistent loot table loading
  - Switched to direct drop mechanism for guaranteed functionality
  - Documented loot table vs direct drop approaches in development guide
  - Improved crop harvest reliability

## [0.1.0]

### Added
- **Weed Pipe**: New reusable item that provides the same effects as the joint
  - Functions like a tool with 4 uses before breaking
  - Same animation and effects as the joint (regeneration, night vision, luck, hunger, occasional confusion)
  - 5-second cooldown between uses to prevent spam
  - Can be repaired using weed buds in an anvil
  - Premium crafting recipe requiring 3 different wood types, charcoal, and weed bud
  - Does not stack (like other tools)
  - Added to the "Four Twenty" creative tab
- **Smoke Particle Effects**: Both joint and pipe now produce realistic smoke effects
  - Initial smoke when starting to use the item
  - Final exhale effect when finishing
  - Server-synchronized for multiplayer visibility

### Changed
- Nothing yet

### Fixed
- Nothing yet

## [0.0.1] - Initial Release

### Added
- Basic weed cultivation system with seeds and crop blocks
- Weed joints as consumable items with multiple effects
- Wild weed generation in world
- Custom "Four Twenty" creative tab
## v0.5.0 (2026-10-15)

### Feat

- improve player and villager/colonist smoke effect

## v0.4.1 (2026-10-15)

### Fix

- update missing translation for dispensary name and desc

## v0.4.0 (2026-10-15)

### Feat

- add dispensary building and budtender profession phases 5 and 6
- add dispensary building and budtender profession phase 4
- add dispensary building and budtender profession phase 3
- add dispensary building and budtender profession phase 2
- add dispensary building and budtender profession phase 1

### Refactor

- update crop loot and bong automation

## v0.3.2 (2025-07-21)

### Fix

- add missing weed bong gui files

## v0.3.1 (2025-07-20)

### Feat

- add proper bong art
- add stationary bong block with area effect
- add weed edibles with unique effect system
- add reusable weed pipe
- add the ability for weed to spawn naturally in certain biomes

### Fix

- update the bong recipe to match new model
- add missing weed brownie art
- use direct drop mechanism for weed crop loot as workaround
- add language file for weed pipe
- fix crafting recipes and make pipe more difficult
- adjust display values for weed joint to look more natural in first and third person views
- update crop model to use a cross pattern instead of a block

### Refactor

- modify bong to use a custom GUI for interaction
