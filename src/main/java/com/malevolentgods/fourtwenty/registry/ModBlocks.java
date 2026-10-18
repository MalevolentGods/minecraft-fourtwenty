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

    public static final DeferredHolder<Block, WeedBongBlock> WEED_BONG = BLOCKS.register("weed_bong",
      () -> new WeedBongBlock(
        BlockBehaviour.Properties.of()
          .strength(2.0f, 6.0f)
          .sound(SoundType.GLASS)
          .noOcclusion()
      ));
}
