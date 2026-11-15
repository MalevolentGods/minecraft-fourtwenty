# Four Twenty: the CurseForge page

The text for the mod's CurseForge project page. Copy the **Description** section into CurseForge's description editor (it takes Markdown), and the **Changelog** section into each file's changelog when uploading. Update both with every release.

---

## Description

# Four Twenty 🌿

**Grow it, cure it, roll it, and power a whole world with it.** Four Twenty is a cannabis mod for Minecraft 1.21.1 (NeoForge) that grows from a patch of wild weed into farms, strain breeding, a full electrical engineering kit, wearable tech, and, with MineColonies, a colony that runs on it.

Everything works on its own. **MineColonies is optional:** with it, colonists take jobs over and a few colony features appear, but you never need it.

### 🌱 Grow and breed
- Find **wild weed**, plant it, and harvest **rough buds**. Trim them for seeds, cure them on racks, and roll the finished buds.
- **Strains and families:** from Landrace up through hybrids to **Golden God**, about 60 hours of climbing. Higher families give more buds, stronger and longer highs, and rarer seeds.
- **Breeding:** Pheno Chambers, pollination, traits, biome strains, and the Buzz Bot.
- **Grow Ops** in five tiers, with extra seeds the higher you go.

### 🔥 Smoke, eat and share
- **Joints, pipes, cookies and brownies,** each strain with its own effects.
- **Bongs and specialty bongs**, a **hookah** that shares its effects around your base, and a **hotbox**.

### ⚡ Inspoweednetics: power and electronics
- **Boundless Inspiration (BI):** smoking powers machines. Being high near a machine runs it free; otherwise it draws BI from **Stash Jars** to **The Vault**, over **Wicks** that are real DC circuits with red/black polarity.
- **Processing machines:** Trim Tray to Trim Reaper, Curing Rack to The Humidor, the Terpinator, the Inspiration Infuser and the Rosin Rig.
- **Real electronics:** resistors, capacitors, coils, diodes, every logic gate, latches, flip-flops, transistors, an op-amp, shift registers, adders, the Manyfier and Fewinator, 7-segment displays and keypads, solved like SPICE.
- **Lit Bits:** RGB lamps you program in Lua with the **Weedweeno**.
- **The Kush Suit and the Blazer:** wearable gear that runs while you're high, including a Puff Pack that flies.

### 🏘️ With MineColonies
- **The Dispensary and its Budtender:** colonists get their daily joint, with their own tastes, habits and happiness.
- **The Bud Barn and Trim Daddy** trim and cure the colony's harvest.
- **Lifted Builders:** a Builder with a joint takes a smoke break at her hut, then **flies** to work, building faster and from further away, and comes home when her high wears off.
- **The Midnight Machine:** a Lifted Builder wears a glowing boombox and plays the album ***Midnight Machine* by ATLemental**, with live VU bars and a dance for every track.
- **Sensi Heights:** a full MineColonies style pack, every hut at every level.

### Safe by design
Flying can't kill you, overloads burn out in smoke rather than explode, and failed machines just pause.

### Requirements
- **Minecraft 1.21.1**, **NeoForge 21.1.218** or later 21.1.x, **Java 21**.
- **Optional:** MineColonies (for the colony features and Sensi Heights).
- Servers and players need the same version of the mod.

### Credits
- Made by **MalevolentGods**.
- The album *Midnight Machine* is by **ATLemental** and ships inside the mod.
- Licensed under **CC BY-NC-SA 4.0**.

---

## Changelog

### Unreleased

- **Lifted Builders fly everywhere** (with MineColonies): while high, every trip is flown: home to unload or fetch materials, to her racks, and on breaks to the restaurant, bed or the hospital, landing close by.
- **Through doors:** she opens wooden doors and fence gates as she reaches them and closes them behind her.
- **A cruise pose** in flight (leaning back, legs together), and the joint raised to her mouth on a smoke break.
- **Never stranded:** a trip that gets no closer for 30 seconds sends her back to her hut.
- **Servers:** `bodyFlight` in `fourtwenty-builders-server.toml` (on by default).

### 0.26.0

- **Lifted Builders** (with MineColonies): a Builder with a joint walks back to her hut for a smoke break, then **flies** to work with a faint green glow and builds the **Higher Ground** way: reach 9, 25-50% faster.
  - Her high counts down only while she builds. When it runs out, or her job is done, she flies home. Her citizen window shows a badge with her strain and the time left.
  - She asks for one of the colony's spare joints when she needs one (citizens first), and every joint she smokes counts in the colony like anyone's.
  - She flies over walls and rooftops, walks into closed buildings from outside, and is brought home if stranded. A watchdog rescues Builders stuck while building. Her hut's **"Fly when Lifted"** setting is on by default.
- **The Midnight Machine:** a Lifted Builder plays ATLemental's album *Midnight Machine* from a painted synth-rack boombox. Loud close by, fading with distance (highs first), with VU bars and a dance for every track. Each player's own volume and mute are in the pause menu.
- **Racks** are solid again, and players and colonists climb their outside face like a ladder.
- **Sensi Heights v101:** the air pockets under the Warehouse centre stairs are filled.
- **Servers:** `fourtwenty-builders-server.toml` holds the new settings; the defaults need no changes.
