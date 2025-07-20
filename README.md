# FourTwenty 🌿

A comprehensive Minecraft mod that adds a cannabis cultivation system to your game. Grow, harvest, and craft cannabis into various consumable products with unique gameplay effects.

## 🎮 Features

### Cannabis Cultivation System
- **Weed Seeds**: Plant these to start your cannabis farm
- **Growing Cycle**: Cannabis plants grow through 8 distinct stages (0-7), just like vanilla crops
- **Weed Crop Block**: A fully functional crop that requires proper farming conditions
- **Harvesting**: Mature plants (stage 7) can be right-clicked to harvest buds while replanting automatically
- **Sustainable Farming**: 5% chance for seed drops from mature harvests, breaking immature crops drops 1 seed for replanting

### Natural Wild Cannabis
- **Wild Weed Spawning**: Cannabis plants now spawn naturally in the wild across specific biomes
- **Target Biomes**: Plains, Savanna, Grove, and Meadow biomes feature natural cannabis growth
- **Random Growth Stages**: Wild plants spawn at various ages (3-7) for realistic diversity
- **Harvestable Resources**: Wild cannabis can be harvested for seeds and buds, providing an alternative source
- **Balanced Rarity**: Wild plants are uncommon enough to feel special but common enough to find with exploration

### Consumption Methods

#### Smoking Items
- **Weed Joint**: Single-use consumable made from weed buds and paper
- **Weed Pipe**: Reusable smoking device with 4 uses and durability system
- **Instant Effects**: Immediate onset with 1-2 minute duration
- **Crafting Recipes**: Joints use vertical pattern (Paper → Weed Bud → Paper), pipes require premium materials

#### Edible Items (v0.2.0)
- **Weed Cookies**: Basic tier edible with delayed onset
  - Shapeless recipe (cookie + weed bud)
  - 10-second delay, 3-minute duration
  - Provides nutrition (2 hunger + 2.4 saturation)
  - Moderate effects with food benefits
- **Weed Brownies**: Premium tier edible with enhanced effects
  - Complex shaped recipe (cocoa, wheat, egg, sugar, weed bud)
  - 15-second delay, 5-minute duration
  - Enhanced nutrition (4 hunger + 4.8 saturation)
  - Premium effects including Jump Boost II and Speed I
- **Overdose Prevention**: Cannot consume multiple edibles while effects are pending

#### Stationary Equipment (v0.3.0)
- **Weed Bong**: Advanced stationary smoking apparatus
  - Multi-state block with visual feedback (empty, water-filled, loaded, lit)
  - Interactive system: water bucket → weed buds → flint & steel → use
  - Area-of-effect benefits (4-block radius for group sessions)
  - Enhanced effects stronger than individual consumption
  - 15-second burn duration with automatic weed consumption
  - Particle effects and ambient sounds
  - Social gameplay encouraging group activities

### Gameplay Effects

#### Smoking Effects (Joints & Pipes)
When consuming joints or using pipes, players experience immediate effects:
- **Regeneration II** (30 seconds) - Enhanced health recovery
- **Night Vision** (2 minutes) - See clearly in the dark
- **Hunger II** (10 seconds) - Increased appetite (munchies effect)
- **Luck I** (1.5 minutes) - Better loot drops and fishing results
- **Confusion** (20 seconds, 30% chance) - Occasional disorientation effect

#### Edible Effects (Cookies & Brownies)
Edibles provide delayed but longer-lasting effects:

**Weed Cookies (Basic Tier):**
- **Delayed Onset**: 10-second delay before effects begin
- **Duration**: 3 minutes (longer than smoking)
- **Effects**: Standard weed effects plus food nutrition
- **Special**: Moderate potency, good for beginners

**Weed Brownies (Premium Tier):**
- **Delayed Onset**: 15-second delay before effects begin
- **Duration**: 5 minutes (extended experience)
- **Effects**: Enhanced standard effects plus Jump Boost II and Speed I
- **Special**: Premium potency with bonus mobility effects

#### Bong Effects (Group Sessions)
The bong provides the strongest effects with social benefits:
- **Area Effect**: All players within 4 blocks receive benefits
- **Enhanced Potency**: Stronger and longer effects than individual consumption
- **Group Bonuses**: Additional Damage Boost and Speed effects
- **Continuous Benefits**: Effects apply every second while the bong is lit
- **Social Gameplay**: Encourages communal use and shared experiences

### Creative Mode Integration
- Custom "Four Twenty" creative tab containing all mod items
- Easy access to seeds, buds, and joints for creative building

#### Website:
TBD

## 🏗️ Technical Architecture

### Project Structure
This mod is built for **Minecraft 1.21.1** using **NeoForge 21.1.92** and **Java 21**.

#### Core Components

**Registry Classes:**
- `ModItems.java` - Registers all mod items (seeds, buds, joints, pipes, edibles, bong)
- `ModBlocks.java` - Registers the weed crop block, wild weed block, and bong block
- `ModCreativeTabs.java` - Creates the custom creative mode tab
- `ModFeatures.java` - Registers world generation features for natural spawning
- `ModBlockEntities.java` - Registers block entities for advanced blocks (bong)

**Block Implementation:**
- `WeedCropBlock.java` - Extends `CropBlock` for cannabis cultivation mechanics with right-click harvesting
- `WildWeedBlock.java` - Extends `BushBlock` for naturally spawning wild cannabis
- `WeedBongBlock.java` - Advanced block entity with multi-state mechanics and area effects
- Supports 8 growth stages with appropriate blockstate definitions
- Integrates with Minecraft's crop growth system and block entity ticking

**Item Implementation:**
- `WeedJointItem.java` - Single-use consumable with immediate effects
- `WeedPipeItem.java` - Reusable smoking device with durability and cooldown
- `WeedEdibleItem.java` - Base class for edibles with delayed effect system
- `WeedCookieItem.java` - Basic tier edible with moderate delayed effects
- `WeedBrownieItem.java` - Premium tier edible with enhanced delayed effects
- Uses various `UseAnim` types for different consumption experiences

**Advanced Systems:**
- `DelayedEffectManager.java` - Server tick-based system for edible effect timing
- `WeedBongBlockEntity.java` - Block entity handling bong state, effects, and area mechanics
- UUID-based player tracking for multiplayer compatibility
- Thread-safe effect scheduling and overdose prevention

**Resource Structure:**
- `assets/fourtwenty/` - Client-side resources (models, textures, lang files)
- `data/fourtwenty/` - Data-driven content (recipes, loot tables, world generation)
- `data/fourtwenty/worldgen/` - World generation features and biome modifiers
- Comprehensive blockstate definitions for all 8 crop growth stages
- Custom item models for seeds, buds, and joints

**World Generation:**
- `wild_weed_patch.json` - Configured feature defining natural cannabis spawning
- `wild_weed_patch.json` - Placed feature controlling spawn frequency and conditions
- `add_wild_weed.json` - Biome modifier adding wild cannabis to target biomes
- Uses weighted state providers for random age distribution (ages 3-7)
- Validates placement conditions to ensure plants spawn on suitable surfaces

#### Dependencies
- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.92+
- **Java**: 21 (required)

### Game Integration
The mod seamlessly integrates with vanilla Minecraft mechanics:
- Cannabis crops follow standard farming rules (require farmland, water, light)
- Loot tables ensure balanced drop rates (buds only from mature crops)
- Recipe system uses standard crafting mechanics
- Effect system uses vanilla potion effects for compatibility

## For Users

### Installation & Usage

1. **Requirements**: Ensure you have Minecraft 1.21.1 with NeoForge 21.1.92+ installed
2. **Download**: Get the latest release from CurseForge (link TBD)
3. **Install**: Place the mod JAR file in your `mods/` folder
4. **Play**: Start Minecraft and look for the "Four Twenty" creative tab

### Getting Started
1. **Obtain Seeds**: Find weed seeds in the Four Twenty creative tab, or harvest them from wild cannabis plants
2. **Find Wild Cannabis**: Explore Plains, Savanna, Grove, and Meadow biomes to discover naturally spawning cannabis
3. **Plant**: Place seeds on tilled farmland (requires water source nearby)
4. **Wait**: Cannabis takes time to grow through 8 stages - be patient!
5. **Harvest**: Right-click mature crops (stage 7) to harvest buds and replant automatically
6. **Craft Your Preferred Method**:
   - **Joints**: Paper → Weed Bud → Paper (vertical pattern) for immediate effects
   - **Pipes**: Complex recipe with wood types and charcoal for reusable option
   - **Cookies**: Cookie + Weed Bud (shapeless) for basic delayed effects
   - **Brownies**: Cocoa + Wheat + Egg + Sugar + Weed Bud (shaped) for premium delayed effects
   - **Bong**: Glass blocks + Water Bucket (shaped) for group sessions
7. **Consume**: 
   - **Smoking**: Right-click and hold to smoke joints/pipes for instant effects
   - **Edibles**: Right-click to eat, then wait 10-15 seconds for delayed onset
   - **Bong**: Place → Add water → Load weed → Light → Use for enhanced group effects

**Pro Tips:**
- **Edibles**: Cannot consume multiple while effects are pending - plan your doses!
- **Bong Setup**: Requires water, weed, and flint & steel - perfect for base builds
- **Seed Management**: Only 5% seed drop chance from harvests - save wild seeds!
- **Group Play**: Bong provides strongest effects when multiple players are nearby
- **Progression**: Start with joints → try edibles → build a bong for end-game social gameplay

You can find all our versions on Curseforge:
TBD



### Issues & Support

**Creating an Issue:**
If you encounter bugs or have feature requests:
1. Check existing issues on our GitHub repository
2. Provide detailed reproduction steps
3. Include your Minecraft version, NeoForge version, and mod version
4. Attach relevant crash logs or screenshots

**Known Limitations:**
- Crop growth rates follow vanilla mechanics (affected by light, water, bone meal)
- Effects are based on vanilla potion systems
- Textures and models may be placeholder in early versions

## For Developers

#### Setup Development Environment

**Prerequisites:**
- Java JDK 21 (required for Minecraft 1.21.1)
- Git for version control
- IDE (IntelliJ IDEA recommended)
- Gradle (included via wrapper)

**Clone and Setup:**
```bash
git clone https://github.com/MalevolentGods/minecraft-fourtwenty.git
cd fourtwenty
./gradlew build
```

**Development Tasks:**
```bash
./gradlew runClient    # Launch Minecraft client for testing
./gradlew runServer    # Launch dedicated server
./gradlew runData      # Generate data files
./gradlew build        # Build the mod JAR
```

### Compiling FourTwenty

IMPORTANT: Please report any issues you have, as there might be some problems with the documentation! Also make sure you know EXACTLY what you're doing! It's not our fault if your OS crashes, becomes corrupted, etc.

#### Setup Java
The Java JDK is used to compile FourTwenty.

1. Download and install the Java JDK 21.
    * [Windows](https://adoptopenjdk.net/): Choose OpenJDK 21 (LTS) version and HotSpot JVM, then click the `latest release` button. After the download is complete, open the file, accept the license agreement, and in a custom setup make sure that `Add to Path` and `Set JAVA_HOME` are set to `Entire feature will be installed on your local hard drive`. Then choose `Install` and wait for the installation to finish.
    * Linux: Installation methods for certain popular flavors of Linux are listed below. If your distribution is not listed, follow the instructions specific to your package manager or install it manually [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
		* Gentoo: `emerge dev-java/openjdk-bin:21`
		* Archlinux: `pacman -S jdk21-openjdk`
		* Ubuntu/Debian: `apt-get install openjdk-21-jdk`
		* Fedora: `yum install java-1.21.0-openjdk`
2. Open up your command line and run `javac`. If it spews out a bunch of possible options and the usage, you're good to go. If not, try the steps again.

**Troubleshooting**
If the `javac` command does not work on Windows:
* Go to `Control Panel\System and Security\System` and click on `Advanced System Settings` on the left-hand side.
* Click on `Environment Variables`.
* Under `System Variables`, click `New`.
* For `Variable Name`, input `JAVA_HOME`.
* For `Variable Value`, input something similar to `C:\Program Files\Java\jdk1.8.0_45` exactly as shown (or wherever your Java JDK installation is) and click `OK`.
* Scroll down to a variable named `Path` and double-click on it.
* Append `;%JAVA_HOME%\bin` EXACTLY AS SHOWN and click `OK`. Make sure the location is correct; double-check to make sure.

#### Setup Gradle (Optional)
Gradle is used to execute the various build tasks when compiling FourTwenty.

1. Download and install Gradle.
	* [Windows/Mac download link](http://www.gradle.org/downloads). You only need the binaries, but choose whatever flavor you want.
		* Unzip the package and put it wherever you want, for example, `C:\Gradle`.
	* Linux: Installation methods for certain popular flavors of Linux are listed below. If your distribution is not listed, follow the instructions specific to your package manager or install it manually [here](http://www.gradle.org/downloads).
		* Gentoo: `emerge dev-java/gradle-bin`
		* Archlinux: You'll have to install it from the [AUR](https://aur.archlinux.org/packages/gradle).
		* Ubuntu/Debian: `apt-get install gradle`
		* Fedora: Install Gradle manually from its website (see above), as Fedora ships a "broken" version of Gradle. Use `yum install gradle` only if you know what you're doing.
2. Windows: Set environment variables for Gradle.
	* Go back to `Environment Variables` and then create a new system variable.
	* For `Variable Name`, input `GRADLE_HOME`.
	* For `Variable Value`, input something similar to `C:\Gradle-3.0` exactly as shown (or wherever your Gradle installation is), and click `Ok`.
	* Scroll down to `Path` again, and append `;%GRADLE_HOME%\bin` EXACTLY AS SHOWN and click `Ok`. Once again, double-check the location.
3. Open up your command line and run `gradle`. If it says 'Welcome to Gradle [version].', then you're good to go. If not, try the steps again.

#### Setup Git
Git is used to clone FourTwenty and update your local copy.

1. Download and install Git [here](http://git-scm.com/download/).
2. *Optional*: Download and install a Git GUI client, such as Gitkraken, SourceTree, GitHub for Windows/Mac, SmartGitHg, TortoiseGit, etc. A nice list is available [here](http://git-scm.com/downloads/guis).


##### Troubleshooting
If Gradle synchronization fails, make sure:
1. `File → Project structure → Project → Project SDK` is set to your installed JDK version.
2. `File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM` is set to your installed JDK version.

#### Updating Your Repository
To get the most up-to-date builds, you'll have to update your local repository periodically.

1. Open up your command line.
2. Navigate to `basefolder` in the console.
3. Ensure you have not made any changes to the local repository (if you did, there might be issues with Git).
	* If you have, try reverting them to their status when you last updated your repository.
4. Execute `git pull version/1.16.3`. This pulls all commits from the official repository that do not yet exist on your local repository and updates it (with GitKraken, click the small pull arrow at the top).

#### Troubleshooting
- Sometimes Gradle tasks fail because of missing memory. For that, you can find system-wide settings in the `.gradle` folder of your `HOME` directory (`~/.gradle/gradle.properties` or on Windows in `C:\Users\username\.gradle\gradle.properties`).
- Sometimes after a branch change, libraries can not be resolved. Running another refresh in IntelliJ or the command line usually solves this issue.

### Contributing

#### Submitting a PR
Found a bug in our code? Think you can make it more efficient? Want to help in general? Great!

1. If you haven't already, create a GitHub account.
2. Click the `Fork` icon located at the top right.
3. Make your changes and commit them.
	* If you're making changes locally, you'll have to do `git commit -a` and `git push` in your command line (or with GitKraken stage the changes, commit them, and push them first).
4. Click `Pull Request` in the middle.
5. Click 'New pull request' to create a pull request for this comparison, enter your PR's title, and create a detailed description telling us what you changed.
6. Click `Create pull request` and wait for feedback!
