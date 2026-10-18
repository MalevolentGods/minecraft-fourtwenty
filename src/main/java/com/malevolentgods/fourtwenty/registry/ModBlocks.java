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

    /** Always-lit coloured lamps (FourTwenty's own, so blueprints need no lighting mod). */
    public static final DeferredHolder<Block, Block> LIME_LAMP = lamp("lime_lamp");
    public static final DeferredHolder<Block, Block> GREEN_LAMP = lamp("green_lamp");
    public static final DeferredHolder<Block, Block> WHITE_LAMP = lamp("white_lamp");
    /** A thin glowing lime plate on any face, like a microblock cover; one lamp crafts eight. */
    public static final DeferredHolder<Block, com.malevolentgods.fourtwenty.block.LampTrimBlock> LIME_LAMP_TRIM = BLOCKS.register("lime_lamp_trim",
      () -> new com.malevolentgods.fourtwenty.block.LampTrimBlock(lampProperties().noOcclusion()));

    private static BlockBehaviour.Properties lampProperties() {
      return BlockBehaviour.Properties.of().strength(0.3f).sound(SoundType.GLASS)
        .lightLevel(state -> 15).emissiveRendering((state, level, pos) -> true);
    }

    private static DeferredHolder<Block, Block> lamp(String name) {
      return BLOCKS.register(name, () -> new Block(lampProperties()));
    }

    public static final DeferredHolder<Block, WeedBongBlock> WEED_BONG = BLOCKS.register("weed_bong",
      () -> new WeedBongBlock(
        BlockBehaviour.Properties.of()
          .strength(2.0f, 6.0f)
          .sound(SoundType.GLASS)
          .noOcclusion()
      ));
}
