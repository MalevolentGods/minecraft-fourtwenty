package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.TrimRules;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * Tier 1 trimming, by hand: right-click with rough buds, one snip per click. Holds nothing, so it needs no block entity.
 * It places like a slab: in the top half it stands on a wooden base with its rim flush with the block's top, a work surface at counter height.
 */
public class TrimTrayBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TrimTrayBlock> CODEC = simpleCodec(TrimTrayBlock::new);
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 4, 16);
    private static final VoxelShape TOP = Block.box(0, 8, 0, 16, 16, 16);

    public TrimTrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, Half.BOTTOM));
    }

    @Override protected @Nonnull MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, HALF); }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        boolean top = face == Direction.DOWN || (face != Direction.UP && context.getClickLocation().y - context.getClickedPos().getY() > 0.5);
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HALF, top ? Half.TOP : Half.BOTTOM);
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return state.getValue(HALF) == Half.TOP ? TOP : BOTTOM;
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
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.5f, 1.2f + level.random.nextFloat() * 0.3f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
