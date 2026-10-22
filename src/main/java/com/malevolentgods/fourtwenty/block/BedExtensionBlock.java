package com.malevolentgods.fourtwenty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * The extra length of a king bed: one per column, just past the foot. Invisible (the bed's own
 * model draws over it), it only gives the king its footprint; it has no item and drops nothing.
 * Breaking it breaks the whole king, like any other part of it. FACING is the bed's.
 */
public class BedExtensionBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BedExtensionBlock> CODEC = simpleCodec(BedExtensionBlock::new);
    private static final java.util.Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.class);

    static {
        // The mattress plus the footboard at the far end (given for a north-facing bed, like FancyBedBlock's).
        for (var facing : Direction.Plane.HORIZONTAL) {
            SHAPES.put(facing, net.minecraft.world.phys.shapes.Shapes.or(FancyBedBlock.rotate(0, 0, 0, 16, 9.5, 14, facing),
                    FancyBedBlock.rotate(0, 0, 14, 16, 9.5, 16, facing)));
        }
    }

    public BedExtensionBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override protected RenderShape getRenderShape(@Nonnull BlockState state) { return RenderShape.INVISIBLE; }

    @Override protected VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
                                            @Nonnull CollisionContext context) { return SHAPES.get(state.getValue(FACING)); }

    /** The king this extends (its columns' feet), or null. */
    private static java.util.List<BlockPos> king(Level level, BlockState state, BlockPos pos) {
        BlockPos foot = pos.relative(state.getValue(FACING));
        BlockState bed = level.getBlockState(foot);
        if (!(bed.getBlock() instanceof FancyBedBlock) || !bed.getValue(FancyBedBlock.SHAPE).king()) return null;
        return BedGroups.columns(foot, bed.getValue(FancyBedBlock.FACING), bed.getValue(FancyBedBlock.SHAPE));
    }

    /** Using the king's foot end uses that column's bed (sleep in it, set your spawn). */
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                            @Nonnull net.minecraft.world.entity.player.Player player,
                                                                            @Nonnull net.minecraft.world.phys.BlockHitResult hit) {
        BlockPos foot = pos.relative(state.getValue(FACING));
        BlockState bed = level.getBlockState(foot);
        if (!(bed.getBlock() instanceof FancyBedBlock)) return net.minecraft.world.InteractionResult.PASS;
        return bed.useWithoutItem(level, player, hit.withPosition(foot));
    }

    @Override public BlockState playerWillDestroy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                                                  @Nonnull net.minecraft.world.entity.player.Player player) {
        var king = level.isClientSide || !player.isCreative() ? null : king(level, state, pos);
        if (king != null) BedGroups.breakQuietly(level, king, state.getValue(FACING));
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState,
                                      boolean moved) {
        var king = level.isClientSide || newState.is(this) ? null : king(level, state, pos);
        if (king != null) BedGroups.breakAll(level, king, state.getValue(FACING));
        super.onRemove(state, level, pos, newState, moved);
    }
}
