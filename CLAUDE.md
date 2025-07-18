# CLAUDE.md - Development Guide for FourTwenty Mod

This document provides context and guidelines for AI assistants working on the FourTwenty Minecraft mod project.

## 🎯 Project Overview

**FourTwenty** is a Minecraft mod that adds a cannabis cultivation system with realistic farming mechanics, crafting, and consumption effects. The mod is built for **Minecraft 1.21.1** using **NeoForge 21.1.92** and **Java 21**.

### Core Concept
- Cannabis as a farmable crop with 8 growth stages
- Harvest system yielding both consumables (buds) and renewable resources (seeds)
- Crafting system to create joints with gameplay effects
- Balanced effect system using vanilla potion mechanics
- **Natural world generation** in specific biomes (plains, savanna, grove, meadow)

## 🏗️ Architecture Patterns

### Project Structure
```
src/main/java/com/malevolentgods/fourtwenty/
├── FourTwenty.java                    # Main mod class
├── registry/                          # Registration classes
│   ├── ModItems.java                  # Item registration
│   ├── ModBlocks.java                 # Block registration
│   ├── ModCreativeTabs.java          # Creative tab registration
│   └── ModFeatures.java              # World generation feature registration
├── block/                             # Block implementations
│   └── WeedCropBlock.java            # Cannabis crop block
├── item/                              # Item implementations
│   ├── WeedJointItem.java            # Consumable joint item
│   ├── WeedPipeItem.java             # Reusable pipe item
│   ├── WeedEdibleItem.java           # Base class for edibles with delayed effects
│   ├── WeedCookieItem.java           # Cookie edible with moderate effects
│   └── WeedBrownieItem.java          # Premium brownie edible
├── util/                              # Utility classes
│   └── DelayedEffectManager.java     # Tick-based delayed effect system
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
└── data/
    ├── fourtwenty/                   # Mod-specific data
    │   ├── loot_tables/              # Block drop tables  
    │   ├── recipe/                   # Crafting recipes (SINGULAR!)
    │   ├── worldgen/                 # World generation data
    │   │   ├── configured_feature/   # Feature configurations
    │   │   └── placed_feature/       # Feature placements
    │   └── neoforge/                 # NeoForge-specific data
    │       └── biome_modifier/       # Biome modification files
    └── drugcolonies/                 # Legacy namespace (if needed)
        └── recipe/                   # Legacy crafting recipes
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
- **Natural Discovery**: Wild cannabis spawns in specific biomes at medium growth stages
- **Meaningful Effects**: Joint consumption provides beneficial effects with minor drawbacks
- **Vanilla Integration**: Uses existing potion effects and crafting patterns

### Natural World Generation
Wild cannabis spawns naturally in four biomes:
- **Plains**: Most common spawn location
- **Savanna**: Scattered patches in grasslands  
- **Grove**: Rare finds in snowy mountain meadows
- **Meadow**: Hidden among wildflowers

**Spawn Characteristics:**
- Growth stages 2-5 (partially grown, not seeds or fully mature)
- Rarity: 1 in 32 chunks on average
- Requires grass block, dirt, or farmland to spawn on
- Only generates during world generation, not as ongoing spawning

### Effect Design
Joint consumption provides:
- **Beneficial Effects**: Regeneration, Night Vision, Luck (positive gameplay impact)
- **Neutral/Negative**: Hunger (munchies), occasional Confusion (realistic trade-off)
- **Duration**: Balanced timing (30 seconds to 2 minutes based on effect type)

### Edible Effects System
Edible consumption (cookies and brownies) provides:
- **Delayed Onset**: 10-15 second delay before effects begin (realistic)
- **Extended Duration**: 3-5 minutes vs 1-2 minutes for smoking
- **Overdose Prevention**: Cannot consume multiple edibles while effects are pending
- **Tier System**: Cookies (basic effects) vs Brownies (premium effects with Jump/Speed)
- **Food Benefits**: Provides nutrition and saturation like normal food

### Crafting Philosophy
- Simple, intuitive recipes using vanilla materials
- **Joints**: Vertical crafting pattern: Paper → Weed Bud → Paper (makes thematic sense)
- **Edibles**: Shapeless (cookies) and shaped (brownies) recipes using vanilla ingredients
- Single output per craft (balanced resource consumption)
- Premium items require more/rarer ingredients (brownies need cocoa + wheat + weed)

## 🛠️ Technical Implementation Details

### Dependencies
- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.92+
- **Java**: 21 (required)
- **Build System**: Gradle with NeoForge MDK

### Key Technical Decisions
1. **CropBlock Extension**: `WeedCropBlock` extends vanilla `CropBlock` for compatibility
2. **Custom Consumables**: 
   - `WeedJointItem` implements custom consumption with `UseAnim.TOOT_HORN`
   - `WeedPipeItem` adds durability and cooldown mechanics
   - `WeedEdibleItem` base class provides delayed effect system with overdose prevention
3. **Delayed Effect System**: `DelayedEffectManager` uses server tick events for proper timing
4. **Deferred Registration**: All registrations use NeoForge's deferred system
5. **Resource Separation**: Assets in `fourtwenty` namespace, legacy recipes in `drugcolonies` namespace
6. **Effect System**: Uses vanilla `MobEffectInstance` for maximum compatibility
7. **World Generation**: JSON-based data-driven approach for biome modification and feature placement

### Recipe System (NeoForge 1.21.1 Specific)
**CRITICAL**: Recipe folder naming and format requirements discovered through testing:

1. **Folder Structure**: Must use `data/modid/recipe/` (singular) NOT `recipes/` (plural)
2. **Result Format**: Recipe results must use `"id"` key, NOT `"item"` key:
   ```json
   // ✅ CORRECT
   "result": {
     "id": "fourtwenty:weed_pipe",
     "count": 1
   }
   
   // ❌ WRONG  
   "result": {
     "item": "fourtwenty:weed_pipe", 
     "count": 1
   }
   ```
3. **Optional Properties**: `"show_notification": true` can be added for recipe unlock notifications
4. **Testing**: If recipes don't appear in crafting table, check folder name and result format first

### Development Lessons Learned
- **Recipe Debugging**: Always verify folder naming (`recipe` vs `recipes`) and result format (`id` vs `item`)
- **NeoForge Changes**: 1.21.1 introduced stricter recipe validation compared to older versions
- **Build Process**: Clean builds (`./gradlew clean build`) are necessary when changing resource files
- **Delayed Effects**: Use server tick events instead of Thread.sleep() for game-appropriate timing
- **Food Properties**: NeoForge 1.21.1 removed `alwaysEat()` method from FoodProperties.Builder
- **Effect Prevention**: Use centralized manager systems to prevent effect stacking/overdose scenarios

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
- **World Generation**: Add features to `data/fourtwenty/worldgen/`, modify biomes via `data/fourtwenty/neoforge/biome_modifier/`

### Testing Priorities
1. **Crop Growth**: Ensure all 8 stages work correctly
2. **Natural Spawning**: Test wild weed generation in target biomes
3. **Harvest Mechanics**: Verify loot table functionality for both cultivated and wild crops
4. **Crafting**: Test recipe availability and output
5. **Effects**: Validate potion effect application and duration
6. **Creative Tab**: Confirm all items appear correctly
7. **Edible System**: 
   - Test delayed effect onset (10-15 seconds)
   - Verify overdose prevention (cannot consume multiple edibles)
   - Check tier differences (cookies vs brownies)
   - Validate food nutrition application

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

## 🌍 World Generation Implementation Guide

### JSON-Based World Generation Architecture
Modern Minecraft world generation uses a data-driven approach with JSON files for maximum flexibility and mod compatibility.

#### File Structure for World Generation
```
data/fourtwenty/worldgen/
├── configured_feature/
│   └── wild_weed_patch.json          # Feature definition
├── placed_feature/
│   └── wild_weed_patch.json          # Placement rules
└── biome_modifier/
    └── add_wild_weed.json            # Biome integration
```

#### Key Components Explained

**1. Configured Features** (`configured_feature/`)
Defines WHAT spawns and HOW it's configured:
```json
{
  "type": "minecraft:random_patch",     // Scatter multiple blocks
  "config": {
    "tries": 16,                       // Attempts per chunk section
    "xz_spread": 4,                    // Horizontal spread radius
    "y_spread": 2,                     // Vertical spread range
    "feature": {
      "feature": {
        "type": "minecraft:simple_block",
        "config": {
          "to_place": {
            "type": "minecraft:weighted_state_provider",  // Random age selection
            "entries": [
              {"weight": 3, "data": {"Name": "fourtwenty:wild_weed", "Properties": {"age": "3"}}},
              {"weight": 4, "data": {"Name": "fourtwenty:wild_weed", "Properties": {"age": "4"}}},
              // ... more age variants
            ]
          }
        }
      },
      "placement": [
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:replaceable"}},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": {...}}}
      ]
    }
  }
}
```

**2. Placed Features** (`placed_feature/`)
Defines WHERE and HOW OFTEN features spawn:
```json
{
  "feature": "fourtwenty:wild_weed_patch",
  "placement": [
    {"type": "minecraft:count", "count": 3},              // 3 attempts per chunk
    {"type": "minecraft:in_square"},                      // Scatter within chunk
    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},  // Surface level
    {"type": "minecraft:biome"},                          // Only in target biomes
    {"type": "minecraft:rarity_filter", "chance": 128}   // 1 in 128 chance
  ]
}
```

**3. Biome Modifiers** (`neoforge/biome_modifier/`)
Defines WHICH BIOMES get the features:
```json
{
  "type": "neoforge:add_features",
  "biomes": ["minecraft:plains", "minecraft:savanna", "minecraft:grove", "minecraft:meadow"],
  "features": "fourtwenty:wild_weed_patch",
  "step": "vegetal_decoration"
}
```

### Critical World Generation Concepts

#### Block State Providers
- **`simple_state_provider`**: Always places the same block state
- **`weighted_state_provider`**: Randomly selects from weighted options (perfect for age variation)
- **`rotated_block_provider`**: Rotates blocks (useful for directional blocks)

#### Placement Modifiers
- **`minecraft:count`**: How many attempts per feature placement
- **`minecraft:rarity_filter`**: Probability of attempting placement (higher = rarer)
- **`minecraft:heightmap`**: Where vertically to place (surface, ocean floor, etc.)
- **`minecraft:block_predicate_filter`**: Validation checks for placement

#### Common Placement Predicates
- **`minecraft:replaceable`**: Only place in air, water, or other replaceable blocks
- **`minecraft:would_survive`**: Check if the block can survive at this location
- **`minecraft:matching_blocks`**: Only place on/in specific blocks
- **`minecraft:all_of`**: Combine multiple predicates (AND logic)
- **`minecraft:any_of`**: Alternative predicates (OR logic)

### Registration Requirements

#### ModFeatures.java Registration
World generation features must be registered in code:
```java
public class ModFeatures {
    public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES =
        DeferredRegister.create(Registries.CONFIGURED_FEATURE, "fourtwenty");
    
    public static final DeferredRegister<PlacedFeature> PLACED_FEATURES =
        DeferredRegister.create(Registries.PLACED_FEATURE, "fourtwenty");
}
```

#### Main Mod Class Integration
```java
@Mod("fourtwenty")
public class FourTwenty {
    public FourTwenty(IEventBus bus) {
        // Register world generation features
        ModFeatures.CONFIGURED_FEATURES.register(bus);
        ModFeatures.PLACED_FEATURES.register(bus);
    }
}
```

### Block Compatibility for World Generation

#### CropBlock vs BushBlock
- **CropBlock**: Requires farmland, breaks on grass/dirt → Use for cultivation
- **BushBlock**: Can survive on grass/dirt → Use for natural spawning

#### WildWeedBlock Implementation
```java
public class WildWeedBlock extends BushBlock {
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(net.minecraft.tags.BlockTags.DIRT) || 
               state.getBlock() == Blocks.GRASS_BLOCK ||
               state.getBlock() == Blocks.DIRT ||
               state.getBlock() == Blocks.PODZOL ||
               state.getBlock() == Blocks.COARSE_DIRT;
    }
}
```

### Testing and Debugging World Generation

#### Debug Commands
Create debug commands for testing:
```java
@EventBusSubscriber(modid = "fourtwenty")
public class DebugWeedCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // /debugweed - Test configured feature placement
        // /testweed - Test basic block placement
    }
}
```

#### Testing Workflow
1. **Basic Block Placement**: Test if custom blocks can be placed and survive
2. **Feature Registration**: Verify features are registered (check for missing registry errors)
3. **Placement Logic**: Test configured feature with debug commands
4. **Biome Integration**: Generate new chunks in target biomes
5. **Balance Testing**: Adjust rarity and count values based on gameplay

## 💻 Windows PowerShell Command Syntax

### Important PowerShell Considerations
Windows PowerShell has different syntax than Bash - key differences to remember:

#### Command Chaining
- **Bash**: `command1 && command2` (conditional execution)
- **PowerShell**: `command1; command2` (sequential execution)
- **PowerShell Conditional**: `if ($?) { command2 }` (if first succeeded)

#### Directory Navigation
```powershell
# Navigate to project directory
cd "d:\Media\Projects\Coding\Mods\Minecraft\fourtwenty"

# Run Gradle tasks
.\gradlew build
.\gradlew runClient
.\gradlew clean

# Multiple commands in sequence
.\gradlew clean; .\gradlew build
```

#### File Operations
```powershell
# Delete files
Remove-Item -Path "filename.ext"
Remove-Item -Recurse -Force "directory"

# Create directories
New-Item -ItemType Directory -Path "new\directory"

# Copy files
Copy-Item -Path "source" -Destination "target"
```

#### Common PowerShell Patterns for Development
```powershell
# Check if build was successful before running
.\gradlew build; if ($?) { .\gradlew runClient }

# Clean and rebuild
.\gradlew clean; .\gradlew build

# Run data generation
.\gradlew runData

# Build and copy to mods folder (example)
.\gradlew build; Copy-Item "build\libs\*.jar" "C:\MinecraftServer\mods\"
```

### Gradle Task Reference
```powershell
# Development tasks
.\gradlew runClient          # Launch Minecraft client for testing
.\gradlew runServer          # Launch dedicated server
.\gradlew runData            # Generate data files
.\gradlew build              # Build the mod JAR
.\gradlew clean              # Clean build cache
.\gradlew publishToMavenLocal # Install to local Maven repository

# Troubleshooting tasks
.\gradlew --refresh-dependencies build    # Force dependency refresh
.\gradlew clean build --info            # Verbose build output
.\gradlew tasks                         # List all available tasks
```

## 🔧 Common Issues and Solutions

### World Generation Problems

#### Feature Not Spawning
1. **Check Registration**: Ensure `ModFeatures` is registered in main mod class
2. **Verify JSON Syntax**: Use JSON validator, check for trailing commas
3. **Test Placement**: Use debug commands to test feature manually
4. **Check Prerequisites**: Ensure custom blocks can survive at placement location
5. **Generate New Chunks**: Existing chunks won't have new features

#### Block Immediately Breaking
- **Problem**: Block requires specific foundation (e.g., CropBlock needs farmland)
- **Solution**: Use appropriate base class (BushBlock for natural spawning)
- **Debug**: Test manual placement with debug commands

#### JSON Parse Errors
```
Common error: "No key placement" or "No key feature"
Cause: Incorrect nesting in random_patch configuration
Solution: Ensure proper feature/placement structure in JSON
```

### Build and Runtime Issues

#### Configuration Corruption
```
Error: "Cannot invoke java.lang.Boolean.booleanValue() because the return value is null"
Cause: Corrupted FML configuration files
Solution: Delete runs/client/config/ directory and regenerate
```

#### Gradle Build Failures
```powershell
# Clean and retry
.\gradlew clean; .\gradlew build

# Force dependency refresh
.\gradlew --refresh-dependencies build

# Check for specific errors
.\gradlew build --stacktrace --info
```

#### Registry Errors
- **Missing Codec**: Ensure custom blocks implement required codec() method
- **Registration Order**: Register features before using them in JSON files
- **Namespace Issues**: Check that resource locations match exactly

### Development Environment Issues

#### IDE Setup
- **Java Version**: Must use Java 21 for Minecraft 1.21.1
- **Gradle JVM**: Set IDE to use correct Java version for Gradle
- **Project Structure**: Verify Project SDK settings in IDE

#### Hot Reload
- **Client Testing**: Use `.\gradlew runClient` for most testing
- **Data Changes**: May require restart for JSON changes
- **Code Changes**: Usually hot-reloaded during runClient

---

*World generation and PowerShell sections added based on implementation experience. Update as new patterns emerge.*
