package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.power.WickBlock.Link;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Transport Tube: carries items between machines, either way along the same tube, with any number sharing a network. No tiers. Like a Wick, each side is open, joined to another tube,
 * or plugged into a block, which draws its own fitting for that side (client/PortModel). The carrying is done by {@link TubeOutput}.
 */
public class TubeBlock extends Block {
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
        ARMS.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
        ARMS.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
        ARMS.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
        ARMS.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
        ARMS.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
    }

    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public TubeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (var side : WickBlock.SIDES.values()) state = state.setValue(side, Link.NONE);
        registerDefaultState(state);
        for (BlockState s : stateDefinition.getPossibleStates()) {
            VoxelShape shape = CORE;
            for (Direction d : Direction.values()) if (s.getValue(WickBlock.SIDES.get(d)) != Link.NONE) shape = Shapes.or(shape, ARMS.get(d));
            shapes.put(s, shape);
        }
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        WickBlock.SIDES.values().forEach(builder::add);
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return shapes.get(state);
    }

    private static Link link(BlockState neighbour) {
        if (neighbour.getBlock() instanceof TubeBlock) return Link.WIRE;
        return neighbour.is(ModPower.TUBE_BLOCKS) ? Link.PLUG : Link.NONE;
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) state = state.setValue(WickBlock.SIDES.get(d), link(context.getLevel().getBlockState(context.getClickedPos().relative(d))));
        return state;
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbour, @Nonnull LevelAccessor level,
                                                        @Nonnull BlockPos pos, @Nonnull BlockPos neighbourPos) {
        return state.setValue(WickBlock.SIDES.get(direction), link(neighbour));
    }
}
