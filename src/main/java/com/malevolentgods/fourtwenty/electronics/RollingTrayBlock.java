package com.malevolentgods.fourtwenty.electronics;

import com.malevolentgods.fourtwenty.power.BIGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;

/**
 * The Rolling Tray: a panel board (a backplane) for the parts people touch and read. A Puff Button, Toggle Joint or Numb Nug placed on
 * its front mounts flush, with no poles of its own; a lamp in front of it is its lamp. Trays side by side are one board for power: a red
 * Wick on any of them feeds every part's +, a black its −. Signals stay apart: each tray's part has its own back face, a Wick or Hemp
 * Thread there for one signal, an Eighth Ribbon for a Numb Nug's nine. Set a tray to a channel (use it; sneak to step back) and its
 * part's signal also joins that channel of the board's bus, which any Ribbon on a free face of the board carries.
 */
public class RollingTrayBlock extends Block {
    /** Its front, where its part goes; its back, opposite, is where that part's signal comes out. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** The bus channel its part's signal joins: 0 to 7, 8 the carry, {@link #NONE} for none. */
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, ComponentType.BUS);
    public static final int NONE = ComponentType.BUS;

    /** Its wires, as the network names them: the part's signal lines 0 to 8, the board's + and − rails, and the board bus 0 to 8. */
    public static final int PLUS = 20, MINUS = 21, BUS = 30;

    public RollingTrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CHANNEL, NONE));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, CHANNEL); }

    /** Its front toward the player, whichever of the six ways they look. */
    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    public static Direction front(BlockState state) { return state.getValue(FACING); }

    public static int channel(BlockState state) { return state.getValue(CHANNEL); }

    /** How many signal lines its part has: 1 for a switch or a lamp, the bus for a Numb Nug, 0 with nothing on its front. */
    public static int signals(net.minecraft.world.level.BlockGetter level, BlockPos tray, BlockState state) {
        BlockState part = level.getBlockState(tray.relative(front(state)));
        if (part.getBlock() instanceof ComponentBlock c && ComponentBlock.mountedOn(part) == front(state).getOpposite()) return c.type().mountSignals();
        if (part.getBlock() instanceof com.malevolentgods.fourtwenty.block.LampBlock) return 1;
        return 0;
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                                                  @Nonnull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int next = Math.floorMod(channel(state) + (player.isShiftKeyDown() ? -1 : 1), ComponentType.BUS + 1);
        level.setBlock(pos, state.setValue(CHANNEL, next), 3);
        BIGrid.changed(level);
        player.displayClientMessage(next == NONE ? Component.translatable("message.fourtwenty.rolling_tray.no_channel")
                : Component.translatable("message.fourtwenty.rolling_tray.channel", RoachClipBlock.name(next)), true);
        return InteractionResult.SUCCESS;
    }

    @Override protected @Nonnull BlockState rotate(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected @Nonnull BlockState mirror(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) BIGrid.changed(level);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) BIGrid.changed(level);
        super.onRemove(state, level, pos, next, moved);
    }
}
