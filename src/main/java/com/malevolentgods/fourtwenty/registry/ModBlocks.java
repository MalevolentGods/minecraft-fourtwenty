package com.malevolentgods.fourtwenty.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.block.WildWeedBlock;
import com.malevolentgods.fourtwenty.block.WeedBongBlock;
import com.malevolentgods.fourtwenty.block.GlassBongBlock;
import com.malevolentgods.fourtwenty.domain.BongVariety;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(Registries.BLOCK, "fourtwenty");

    public static final DeferredHolder<Block, WeedCropBlock> WEED_CROP = BLOCKS.register("weed_crop",
      () -> new WeedCropBlock(
        BlockBehaviour.Properties.of()
          .noCollission()
          .noOcclusion()
          .randomTicks()
          .instabreak()
          .sound(SoundType.CROP)
      ));

    public static final DeferredHolder<Block, WildWeedBlock> WILD_WEED = BLOCKS.register("wild_weed",
      () -> new WildWeedBlock(
        BlockBehaviour.Properties.of()
          .noCollission()
          .noOcclusion()
          .instabreak()
          .sound(SoundType.GRASS)
      ));

    /** Placed glass bongs, used on right-click with a bud; specialties add a one-day buff. (weed_bong is the hookah.) */
    public static final java.util.Map<BongVariety, DeferredHolder<Block, GlassBongBlock>> BONGS = registerBongs();
    public static final DeferredHolder<Block, GlassBongBlock> GLASS_BONG = BONGS.get(BongVariety.GLASS);

    private static java.util.Map<BongVariety, DeferredHolder<Block, GlassBongBlock>> registerBongs() {
      var bongs = new java.util.EnumMap<BongVariety, DeferredHolder<Block, GlassBongBlock>>(BongVariety.class);
      for (var variety : BongVariety.values()) {
        bongs.put(variety, BLOCKS.register(variety.path, () -> new GlassBongBlock(variety,
          BlockBehaviour.Properties.of()
            .strength(0.3f)
            .sound(SoundType.GLASS)
            .noOcclusion())));
      }
      return java.util.Collections.unmodifiableMap(bongs);
    }

    /**
     * Coloured lamps in all 16 dye colours (FourTwenty's own, so builds need no lighting mod): a
     * Lamp lights while powered, an Inverted Lamp is lit unless powered, and Lamp Trim is a thin
     * always-lit plate on any face (one inverted lamp crafts eight).
     */
    public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.LampBlock>> LAMPS = lamps(false);
    public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.LampBlock>> INVERTED_LAMPS = lamps(true);
    public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.LampTrimBlock>> LAMP_TRIMS = lampTrims();

    private static BlockBehaviour.Properties lampProperties() {
      return BlockBehaviour.Properties.of().strength(0.3f).sound(SoundType.GLASS)
        .lightLevel(state -> !state.hasProperty(com.malevolentgods.fourtwenty.block.LampBlock.LIT)
            || state.getValue(com.malevolentgods.fourtwenty.block.LampBlock.LIT) ? 15 : 0)
        .emissiveRendering((state, level, pos) -> !state.hasProperty(com.malevolentgods.fourtwenty.block.LampBlock.LIT)
            || state.getValue(com.malevolentgods.fourtwenty.block.LampBlock.LIT));
    }

    private static java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.LampBlock>> lamps(boolean inverted) {
      var lamps = new java.util.EnumMap<net.minecraft.world.item.DyeColor, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.LampBlock>>(net.minecraft.world.item.DyeColor.class);
      for (var color : net.minecraft.world.item.DyeColor.values()) {
        lamps.put(color, BLOCKS.register(color.getName() + (inverted ? "_inverted_lamp" : "_lamp"),
            () -> new com.malevolentgods.fourtwenty.block.LampBlock(color, inverted, lampProperties())));
      }
      return java.util.Collections.unmodifiableMap(lamps);
    }

    private static java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.LampTrimBlock>> lampTrims() {
      var trims = new java.util.EnumMap<net.minecraft.world.item.DyeColor, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.LampTrimBlock>>(net.minecraft.world.item.DyeColor.class);
      for (var color : net.minecraft.world.item.DyeColor.values()) {
        trims.put(color, BLOCKS.register(color.getName() + "_lamp_trim",
            () -> new com.malevolentgods.fourtwenty.block.LampTrimBlock(color, lampProperties().noOcclusion())));
      }
      return java.util.Collections.unmodifiableMap(trims);
    }

    /** Side tables (nightstands) in every vanilla wood but bamboo; see {@link com.malevolentgods.fourtwenty.block.SideTableBlock}. */
    public static final java.util.List<String> SIDE_TABLE_WOODS = java.util.List.of(
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "crimson", "warped");
    public static final java.util.Map<String, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.SideTableBlock>> SIDE_TABLES = sideTables();

    private static java.util.Map<String, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.SideTableBlock>> sideTables() {
      var tables = new java.util.LinkedHashMap<String, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.SideTableBlock>>();
      for (var wood : SIDE_TABLE_WOODS) {
        boolean nether = wood.equals("crimson") || wood.equals("warped");
        tables.put(wood, BLOCKS.register(wood + "_side_table", () -> new com.malevolentgods.fourtwenty.block.SideTableBlock(
            BlockBehaviour.Properties.of().strength(2.0f, 3.0f).noOcclusion()
                .sound(nether ? SoundType.NETHER_WOOD : wood.equals("cherry") ? SoundType.CHERRY_WOOD : SoundType.WOOD))));
      }
      return java.util.Collections.unmodifiableMap(tables);
    }

    /** Beds in the same woods; see {@link com.malevolentgods.fourtwenty.block.FancyBedBlock}. */
    public static final java.util.Map<String, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.FancyBedBlock>> BEDS = beds();

    private static java.util.Map<String, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.FancyBedBlock>> beds() {
      var beds = new java.util.LinkedHashMap<String, DeferredHolder<Block, com.malevolentgods.fourtwenty.block.FancyBedBlock>>();
      for (var wood : SIDE_TABLE_WOODS) {
        beds.put(wood, BLOCKS.register(wood + "_bed", () -> new com.malevolentgods.fourtwenty.block.FancyBedBlock(
            BlockBehaviour.Properties.of().strength(0.2f).sound(SoundType.WOOD).noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).ignitedByLava())));
      }
      return java.util.Collections.unmodifiableMap(beds);
    }

    public static final DeferredHolder<Block, WeedBongBlock> WEED_BONG = BLOCKS.register("weed_bong",
      () -> new WeedBongBlock(
        BlockBehaviour.Properties.of()
          .strength(2.0f, 6.0f)
          .sound(SoundType.GLASS)
          .noOcclusion()
      ));
}
