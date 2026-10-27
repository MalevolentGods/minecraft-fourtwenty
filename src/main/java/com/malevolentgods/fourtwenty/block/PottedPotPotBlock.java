package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * A Potted Pot Pot: a Pot Pot with a weed plant in it. Placed, it holds a sprout; the plant grows
 * through the crop's eight stages in the light (bone meal speeds it), and right-clicked when mature
 * it's harvested like a field crop (the crop's own loot, one seed kept back for the plant) and starts
 * again from a sprout. Breaking it gives back the Potted Pot Pot.
 */
public class PottedPotPotBlock extends Block implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final int MAX_AGE = 7;
    /** A random tick's chance to grow (about a field crop's on hydrated farmland). */
    private static final int GROWTH_CHANCE = 8;

    public PottedPotPotBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return PotPotBlock.SHAPE;
    }

    @Override protected void randomTick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (state.getValue(AGE) < MAX_AGE && level.getRawBrightness(pos, 0) >= 9 && random.nextInt(GROWTH_CHANCE) == 0) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override public boolean isValidBonemealTarget(@Nonnull LevelReader level, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override public boolean isBonemealSuccess(@Nonnull Level level, @Nonnull RandomSource random, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        return true;
    }

    /** As a crop: two to five stages. */
    @Override public void performBonemeal(@Nonnull ServerLevel level, @Nonnull RandomSource random, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 2 + random.nextInt(4))), Block.UPDATE_CLIENTS);
    }

    /** Harvest a mature plant like the field crop: its loot, one seed kept for the plant, which starts again. */
    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                 @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (state.getValue(AGE) < MAX_AGE) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            BlockState crop = ModBlocks.WEED_CROP.get().getStateForAge(MAX_AGE);
            var drops = Block.getDrops(crop, server, pos, null, player, player.getMainHandItem());
            level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_ALL);
            boolean kept = false;
            for (ItemStack stack : drops) {
                if (!kept && stack.is(ModItems.WEED_SEEDS.get()) && !stack.isEmpty()) {
                    stack.shrink(1);
                    kept = true;
                }
                popResource(level, pos, stack);
            }
            player.awardStat(Stats.BLOCK_MINED.get(ModBlocks.WEED_CROP.get()));
            player.causeFoodExhaustion(0.005f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
