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
import com.malevolentgods.fourtwenty.block.DrugCraftingBenchBlock;

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

    public static final DeferredHolder<Block, WeedBongBlock> WEED_BONG = BLOCKS.register("weed_bong",
      () -> new WeedBongBlock(
        BlockBehaviour.Properties.of()
          .strength(2.0f, 6.0f)
          .sound(SoundType.GLASS)
          .noOcclusion()
      ));

    public static final DeferredHolder<Block, DrugCraftingBenchBlock> DRUG_CRAFTING_BENCH = BLOCKS.register("drug_crafting_bench",
      () -> new DrugCraftingBenchBlock(
        BlockBehaviour.Properties.of()
          .strength(2.5f, 6.0f)
          .sound(SoundType.WOOD)
      ));
}
