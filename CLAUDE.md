# CLAUDE.md - Development Guide for FourTwenty Mod

This document provides context and guidelines for AI assistants working on the FourTwenty Minecraft mod project.

## 🎯 Project Overview

**FourTwenty** is a Minecraft mod that adds a cannabis cultivation system with realistic farming mechanics, crafting, and consumption effects. The mod is built for **Minecraft 1.21.1** using **NeoForge 21.1.92** and **Java 21**.

### Core Concept
- Cannabis as a farmable crop with 8 growth stages
- Harvest system yielding both consumables (buds) and renewable resources (seeds)
- Crafting system to create edibles, joints, and other smokeable items with gameplay effects
- Balanced effect system using vanilla potion mechanics

## 🏗️ Architecture Patterns

### Project Structure
```
src/main/java/com/malevolentgods/fourtwenty/
├── FourTwenty.java                    # Main mod class
├── registry/                          # Registration classes
│   ├── ModItems.java                  # Item registration
│   ├── ModBlocks.java                 # Block registration
│   └── ModCreativeTabs.java          # Creative tab registration
├── block/                             # Block implementations
│   └── WeedCropBlock.java            # Cannabis crop block
├── item/                              # Item implementations
│   └── WeedJointItem.java            # Consumable joint item
└── events/                            # Event handlers (currently minimal)
    └── ModEvents.java                 # Future event handling
```

### Resource Organization
```
src/main/resources/
├── assets/fourtwenty/                 # Client-side resources
│   ├── blockstates/                  # Block state definitions
│   ├── models/                       # Item and block models
│   │   ├── item/                     # Item models
│   │   └── block/                    # Block models (8 crop stages)
│   └── lang/                         # Localization
└── data/drugcolonies/                # Data-driven content
    ├── recipes/                      # Crafting recipes
    └── loot_tables/                  # Block drop tables
```

## 🔧 Development Conventions

### Naming Patterns
- **Mod ID**: `fourtwenty`
- **Package**: `com.malevolentgods.fourtwenty`
- **Registry Names**: Snake_case (e.g., `weed_seeds`, `weed_bud`, `weed_joint`)
- **Class Names**: PascalCase with descriptive suffixes (e.g., `WeedCropBlock`, `WeedJointItem`)
- **Constants**: UPPER_SNAKE_CASE for registry holders

### Code Patterns
- Use `DeferredRegister` for all registrations
- Registry classes are static with public final fields
- Items and blocks follow vanilla patterns and extend appropriate base classes
- Resource locations use mod namespace consistently

### Registration Pattern
```java
public static final DeferredRegister<Item> ITEMS = 
    DeferredRegister.create(Registries.ITEM, "fourtwenty");

public static final DeferredHolder<Item, ItemType> ITEM_NAME = 
    ITEMS.register("item_name", () -> new ItemType(properties));
```

## 🎮 Game Design Principles

### Balance Philosophy
- **Realistic Growth**: Cannabis follows vanilla crop mechanics (farmland, water, light requirements)
- **Sustainable Farming**: Mature crops drop both product and seeds
- **Meaningful Effects**: Joint consumption provides beneficial effects with minor drawbacks
- **Vanilla Integration**: Uses existing potion effects and crafting patterns

### Effect Design
Joint consumption provides:
- **Beneficial Effects**: Regeneration, Night Vision, Luck (positive gameplay impact)
- **Neutral/Negative**: Hunger (munchies), occasional Confusion (realistic trade-off)
- **Duration**: Balanced timing (30 seconds to 2 minutes based on effect type)

### Crafting Philosophy
- Simple, intuitive recipes using vanilla materials
- Vertical crafting pattern: Paper → Weed Bud → Paper (makes thematic sense)
- Single output per craft (balanced resource consumption)

## 🛠️ Technical Implementation Details

### Dependencies
- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.92+
- **Java**: 21 (required)
- **Build System**: Gradle with NeoForge MDK

### Key Technical Decisions
1. **CropBlock Extension**: `WeedCropBlock` extends vanilla `CropBlock` for compatibility
2. **Custom Consumable**: `WeedJointItem` implements custom consumption with `UseAnim.TOOT_HORN`
3. **Deferred Registration**: All registrations use NeoForge's deferred system
4. **Resource Separation**: Assets in `fourtwenty` namespace, data in `drugcolonies` namespace
5. **Effect System**: Uses vanilla `MobEffectInstance` for maximum compatibility

### Build and Development
- **IDE**: Visual Studio Code is being used for this project
- **Testing**: `./gradlew runClient` for in-game testing
- **Building**: `./gradlew build` produces distributable JAR
- **Data Generation**: `./gradlew runData` for generated resources

## 📝 Code Style Guidelines

### Java Conventions
- Use `@Nonnull` annotations for better null safety
- Prefer composition over inheritance where appropriate
- Keep registry classes simple and focused
- Use descriptive variable names (e.g., `player`, `level`, `stack`)

### Resource Conventions
- All growth stages must have corresponding blockstate variants (age=0 through age=7)
- Item models should be simple and clear
- Localization keys follow Minecraft patterns (`item.modid.itemname`)

### Documentation
- README.md should be comprehensive and user-focused
- Code comments for non-obvious game mechanics
- Resource files should be self-documenting through clear naming

## 🎯 Future Development Guidelines

### When Adding New Features
1. **Follow Existing Patterns**: Use the same registration and naming conventions
2. **Maintain Balance**: Consider impact on gameplay and existing systems
3. **Vanilla Compatibility**: Prefer extending vanilla systems over creating new ones
4. **Resource Consistency**: Maintain the asset/data separation pattern

### Common Development Tasks
- **New Items**: Add to `ModItems.java`, create models, add to creative tab
- **New Blocks**: Add to `ModBlocks.java`, create blockstates and models
- **New Recipes**: Add JSON files to `data/drugcolonies/recipes/`
- **New Effects**: Modify existing items or create new consumables

### Testing Priorities
1. **Crop Growth**: Ensure all 8 stages work correctly
2. **Harvest Mechanics**: Verify loot table functionality
3. **Crafting**: Test recipe availability and output
4. **Effects**: Validate potion effect application and duration
5. **Creative Tab**: Confirm all items appear correctly

## 🚨 Important Considerations

### Mod Compatibility
- Uses standard NeoForge registration patterns for maximum compatibility
- Avoids overriding vanilla behaviors unnecessarily
- Effect system uses vanilla potions (compatible with other mods)

### Performance
- Crop blocks use standard growth mechanics (no custom tick systems)
- Minimal event handling to reduce overhead
- Resource loading follows vanilla patterns

### Maintenance
- Keep dependencies up to date but stable
- Test thoroughly after Minecraft/NeoForge updates
- Maintain backward compatibility when possible

## 🔗 Key Resources

- **NeoForge Documentation**: For latest API patterns
- **Minecraft Wiki**: For vanilla mechanic references
- **Project Repository**: `https://github.com/MalevolentGods/minecraft-fourtwenty`
- **Issue Tracker**: GitHub Issues for bug reports and feature requests

---

*This document should be updated as the project evolves. When making significant architectural changes, update the relevant sections to maintain accuracy.*
