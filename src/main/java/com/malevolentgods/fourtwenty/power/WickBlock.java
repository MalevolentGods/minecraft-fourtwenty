package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.registry.ModPower;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nonnull;

/** Wiring: Hemp, Rolled and Glass Wick. It joins to Wicks and BI blocks on all six sides, and glows faintly while it carries power. */
public class WickBlock extends PipeBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final int GLOW_TICKS = 30;
    private final BIRules.Tier tier;

    public WickBlock(BIRules.Tier tier, Properties properties) {
        super(0.125f, properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false).setValue(LIT, false));
    }

    public BIRules.Tier tier() { return tier; }

    @Override protected @Nonnull MapCodec<? extends PipeBlock> codec() { return MapCodec.unit(this); }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, LIT);
    }

    private static boolean joins(BlockState state) {
        return state.getBlock() instanceof WickBlock || state.is(ModPower.BI_BLOCKS);
    }

    private BlockState connected(BlockGetter level, BlockPos pos) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(d), joins(level.getBlockState(pos.relative(d))));
        return state;
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return connected(context.getLevel(), context.getClickedPos());
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbour, @Nonnull LevelAccessor level,
                                                        @Nonnull BlockPos pos, @Nonnull BlockPos neighbourPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), joins(neighbour));
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) BIGrid.changed(level);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) BIGrid.changed(level);
        super.onRemove(state, level, pos, next, moved);
    }

    /** Power went through: light it, and a tick later unless power comes again it goes dark. */
    public static void carry(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WickBlock)) return;
        if (!state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, true), 2);
        if (!level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) level.scheduleTick(pos, state.getBlock(), GLOW_TICKS);
    }

    @Override protected void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, false), 2);
    }
}
