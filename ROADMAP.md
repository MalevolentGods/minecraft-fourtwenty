# FourTwenty Mod - Development Roadmap 🌿

This document outlines planned features and improvements for future versions of the FourTwenty mod. Each item can be checked off as it's completed.

## 📋 Version Planning

### 🍪 Version 0.2.0 - Edibles & Food Integration ✅ COMPLETED
- [x] **Weed Cookies** - Consumable food item with longer-lasting effects
  - [x] Crafting recipe (cookies + weed bud)
  - [x] Extended effect duration compared to joints/pipes
  - [x] Food saturation benefits alongside weed effects
  - [x] Custom eating animation and particles

- [x] **Weed Brownies** - Premium edible with stronger effects
  - [x] Multi-ingredient crafting recipe (cocoa, wheat, weed bud)
  - [x] Stronger effect potency than cookies
  - [x] Longer duration than other consumption methods
  - [x] Rare ingredient requirements for balance

- [x] **Edible Effects System**
  - [x] Delayed onset (10-15 seconds after consumption)
  - [x] Longer duration (3-5 minutes vs 1-2 for smoking)
  - [x] Different effect profiles per edible type
  - [x] Prevent stacking/overdose mechanics

### 🏺 Version 0.3.0 - Stationary Equipment ✅ COMPLETED
- [x] **Weed Bong Block** - Stationary smoking apparatus
  - [x] GUI-based brewing stand-like interface
  - [x] 3-slot system (weed, water bottle, blaze powder)
  - [x] Automatic processing and lighting
  - [x] Area-of-effect benefits for nearby players
  - [x] Enhanced effects for group sessions
  - [x] Visual and audio feedback

- [x] **Bong Mechanics**
  - [x] Inventory-based item management
  - [x] Auto-consumption of blaze powder for lighting
  - [x] Area effect radius (4-block radius)
  - [x] 15-second burn duration with auto-extinguish
  - [x] Simplified interaction through GUI

### ✨ Version 0.3.1 - GUI System Enhancement ✅ COMPLETED
- [x] **Interface Overhaul**
  - [x] Complete redesign from block states to GUI system
  - [x] Brewing stand-inspired user interface
  - [x] Automatic processing without manual interactions
  - [x] Clean, intuitive slot-based system
  - [x] Client-server synchronization for multiplayer

### 🔬 Version 0.4.0 - Advanced Crafting System
- [x] **Drug Crafting Bench** - Specialized workstation
  - [x] Custom GUI with dedicated slots
  - [x] Exclusive crafting location for all weed items
  - [ ] Multi-step crafting processes
  - [x] Tool requirements (grinder, papers, etc.)
  - [ ] Recipe unlocking system

- [ ] **Pipe Loading System**
  - [ ] Remove direct weed consumption from pipes
  - [ ] Require "loading" pipes at crafting bench
  - [ ] Loaded pipes show different texture/state
  - [ ] Loading capacity affects number of uses
  - [ ] Empty pipes require refilling at bench

- [ ] **Crafting Bench Features**
  - [ ] Rolling animation for joint creation
  - [ ] Grinding mechanics for enhanced potency
  - [ ] Experience/skill progression
  - [ ] Custom sounds and particle effects
  - [ ] Quality system (poor/good/excellent results) - this is a stretch goal

### 🌈 Version 0.5.0 - Strain Diversity System
- [ ] **Base Strain System**
  - [ ] Separate seed types for different strains
  - [ ] Unique growth characteristics per strain
  - [ ] Different harvest yields and qualities
  - [ ] Strain identification system
  - [ ] Cross-breeding mechanics

- [ ] **Regular Strains**
  - [ ] **Indica Strain** - Strong relaxation effects (Slowness, Regeneration)
  - [ ] **Sativa Strain** - Energy/focus effects (Speed, Jump Boost)
  - [ ] **Hybrid Strain** - Balanced effects (current joint effects)
  - [ ] **Purple Kush** - Rare strain with unique visual effects

- [ ] **Dimensional Strains**
  - [ ] **Enderweed** - Grows in End dimension
    - [ ] Teleportation resistance effects
    - [ ] Ender particle effects when consumed
    - [ ] Requires End soil or special growing conditions
    - [ ] Rare drops from End mobs or structures

  - [ ] **Netherweed** - Grows in Nether dimension
    - [ ] Glowing texture (like glowstone/shroomlight)
    - [ ] Fire resistance and heat-based effects
    - [ ] Requires soul soil or nether growing medium
    - [ ] Red/orange particle effects
    - [ ] Higher potency due to harsh growing conditions

- [ ] **Strain Effects Matrix**
  - [ ] Each strain has 3-5 unique effects
  - [ ] Effect combinations vary by consumption method
  - [ ] Potency scales with growing conditions
  - [ ] Rare strains have exclusive effects (Night Vision, Luck, etc.)

### 🏘️ Version 0.6.0 - Village Integration
- [ ] **Villager Professions**
  - [ ] **Herbalist Villager** - Trades weed-related items
    - [ ] Buys: Seeds, low-quality buds
    - [ ] Sells: Rare seeds, crafting materials, papers
    - [ ] Trades: Strain identification services
    - [ ] Advanced trades: Rare strain seeds

  - [ ] **Botanist Villager** - Advanced plant trading
    - [ ] Buys: High-quality buds, rare strains
    - [ ] Sells: Growing equipment, fertilizers
    - [ ] Trades: Strain breeding consultation
    - [ ] Offers: Custom strain development contracts

- [ ] **Village Economy Integration**
  - [ ] Weed as alternative currency for specific trades
  - [ ] Village demand/supply fluctuation
  - [ ] Reputation system based on product quality
  - [ ] Bulk trading contracts for large quantities
  - [ ] Seasonal trading events

- [ ] **Village Structures**
  - [ ] **Herbalist Hut** - New village building type
  - [ ] **Community Garden** - Shared growing space
  - [ ] **Smoking Lounge** - Social gathering building
  - [ ] Natural integration with existing village generation

## 🔮 Future Expansion Ideas (Version 0.7.0+)
- [ ] **Automation Compatibility**
  - [ ] Hopper interaction with crafting bench
  - [ ] Automated growing systems
  - [ ] ComputerCraft integration for farms

- [ ] **Multiplayer Features**
  - [ ] Shared smoking sessions with bonus effects
  - [ ] Trading between players
  - [ ] Group cultivation projects

- [ ] **Environmental Effects**
  - [ ] Weather effects on crop growth
  - [ ] Seasonal growing cycles
  - [ ] Biome-specific strain preferences

- [ ] **Advanced Chemistry**
  - [ ] Extraction and concentration systems
  - [ ] Custom effect brewing
  - [ ] Scientific equipment blocks

## 📝 Development Notes

### Technical Considerations
- Each major version should maintain backward compatibility
- New items require proper localization in `en_us.json`
- Complex mechanics need comprehensive testing
- Performance impact assessment for area effects
- Integration testing with popular mod packs

### Content Guidelines
- Maintain PG-13 appropriate content and descriptions
- Focus on gameplay mechanics over realism
- Ensure balanced progression and not overpowered effects
- Consider mod compatibility (JEI, ComputerCraft, MineColonies)

### Quality Assurance Checkpoints
- [ ] Recipe validation and testing
- [ ] Effect balance and duration testing
- [ ] Multiplayer synchronization verification
- [ ] Resource pack compatibility
- [ ] Performance benchmarking

---

*This roadmap is subject to change based on community feedback, technical constraints, and development priorities. Check off items as they are completed and add new ideas as they emerge.*
