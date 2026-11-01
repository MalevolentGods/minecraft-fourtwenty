package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.TrimRules;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * Tier 1 trimming, by hand: right-click with rough buds, one snip per click. Holds nothing, so it needs no block entity.
 * It stands on a small foot and post, a work surface at waist height.
 */
public class TrimTrayBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TrimTrayBlock> CODEC = simpleCodec(TrimTrayBlock::new);
    private static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(Block.box(3, 0, 3, 13, 2, 13), Block.box(6, 2, 6, 10, 10, 10), Block.box(0, 10, 0, 16, 14, 16));

    public TrimTrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected @Nonnull MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                 @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (!stack.is(ModItems.ROUGH_BUD.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            Strain strain = ModDataComponents.strain(stack);
            stack.consume(1, player);
            give(player, ModDataComponents.withStrain(new ItemStack(ModItems.TRIMMED_BUD.get()), strain));
            if (TrimRules.seeded(ProcessingConfig.chances(), strain, level.random.nextDouble())) {
                give(player, ModDataComponents.withStrain(new ItemStack(ModItems.WEED_SEEDS.get()), strain));
            }
            level.playSound(null, pos, com.malevolentgods.fourtwenty.registry.ModPower.SNIP.get(), SoundSource.BLOCKS, 0.7f, 0.9f + level.random.nextFloat() * 0.3f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
