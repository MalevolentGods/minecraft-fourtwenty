package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nonnull;

/**
 * A coloured lamp. A regular lamp lights while powered by redstone; an inverted lamp is lit unless
 * powered (always on as decoration). Lit lamps glow at full brightness.
 */
public class LampBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private final boolean inverted;

    public LampBlock(boolean inverted, Properties properties) {
        super(properties);
        this.inverted = inverted;
        registerDefaultState(stateDefinition.any().setValue(LIT, inverted));
    }

    public boolean inverted() { return inverted; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(LIT, inverted != context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override protected void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                             @Nonnull Block neighbor, @Nonnull BlockPos neighborPos, boolean moving) {
        if (level.isClientSide()) return;
        boolean lit = inverted != level.hasNeighborSignal(pos);
        if (lit != state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
    }
}
