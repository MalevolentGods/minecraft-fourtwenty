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

    /** A placed glass bong, used on right-click with a bud. (weed_bong is the hookah.) */
    public static final DeferredHolder<Block, GlassBongBlock> GLASS_BONG = BLOCKS.register("glass_bong",
      () -> new GlassBongBlock(
        BlockBehaviour.Properties.of()
          .strength(0.3f)
          .sound(SoundType.GLASS)
          .noOcclusion()
      ));
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

    public static final DeferredHolder<Block, WeedBongBlock> WEED_BONG = BLOCKS.register("weed_bong",
      () -> new WeedBongBlock(
        BlockBehaviour.Properties.of()
          .strength(2.0f, 6.0f)
          .sound(SoundType.GLASS)
          .noOcclusion()
      ));
}
