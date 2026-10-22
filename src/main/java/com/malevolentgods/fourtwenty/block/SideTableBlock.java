package com.malevolentgods.fourtwenty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * A wooden side table (nightstand): a slim overhanging top, a body with a drawer front and a brass
 * knob, four slim legs and a low shelf. Decoration in every vanilla wood; the drawer faces whoever
 * placed it. The model is FourTwenty's own (tools/generate_side_tables.py), textured with the
 * wood's vanilla planks and stripped log.
 */
public class SideTableBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SideTableBlock> CODEC = simpleCodec(SideTableBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 14, 1, 15, 16, 15),      // top
            Block.box(2, 8, 2, 14, 14, 14),       // body with the drawer
            Block.box(2, 0, 2, 4, 8, 4), Block.box(12, 0, 2, 14, 8, 4),
            Block.box(2, 0, 12, 4, 8, 14), Block.box(12, 0, 12, 14, 8, 14));

    public SideTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The drawer faces whoever placed it. */
    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override protected VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
                                            @Nonnull CollisionContext context) {
        return SHAPE;   // symmetric apart from the drawer front, so one shape serves every facing
    }
}
