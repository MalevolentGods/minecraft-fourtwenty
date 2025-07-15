package com.malevolentgods.fourtwenty.block;

import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.ItemLike;
import com.malevolentgods.fourtwenty.registry.ModItems;

public class WeedCropBlock extends CropBlock {
    public WeedCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.WEED_SEEDS.get();
    }
}
