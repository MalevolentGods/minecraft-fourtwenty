# Changelog

All notable changes to the FourTwenty mod will be documented in this file.

## [0.3.2] - Interface Polish & Bug Fixes

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