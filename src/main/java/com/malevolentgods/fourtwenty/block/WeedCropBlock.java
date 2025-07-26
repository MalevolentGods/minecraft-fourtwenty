package com.malevolentgods.fourtwenty.block;

import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import com.malevolentgods.fourtwenty.registry.ModItems;
import java.util.List;

import javax.annotation.Nonnull;

public class WeedCropBlock extends CropBlock {
    public WeedCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @Nonnull ItemLike getBaseSeedId() {
        return ModItems.WEED_SEEDS.get();
    }
    
    @Override
    public int getMaxAge() {
        // Override to use 6 as max age (like Minecolonies) instead of vanilla 7
        // This means 7 growth stages: age 0, 1, 2, 3, 4, 5, 6
        return 7;
    }

    @Override
    public List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        List<ItemStack> drops = new java.util.ArrayList<>();
        
        int age = getAge(state);
        RandomSource random = RandomSource.create();
        
        // Always drop seeds (like breaking any crop)
        drops.add(new ItemStack(ModItems.WEED_SEEDS.get(), 1));
        
        // Only fully mature crops (age 7) drop buds
        if (age == getMaxAge()) {
            // Drop 1-3 weed buds
            int budCount = 1 + random.nextInt(3);
            drops.add(new ItemStack(ModItems.WEED_BUD.get(), budCount));
            
            // 20% chance for bonus seeds
            if (random.nextFloat() < 0.2f) {
                int seedCount = 1 + random.nextInt(3);
                drops.add(new ItemStack(ModItems.WEED_SEEDS.get(), seedCount));
            }
        }
        
        return drops;
    }

    // No custom harvest logic - just work like vanilla wheat
    // Players break the block to harvest, drops handled by getDrops() override
}
