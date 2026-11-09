package com.malevolentgods.fourtwenty.electronics;

import com.malevolentgods.fourtwenty.power.BIGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * The Roach Clip: a piece of Eighth Ribbon (all its wires pass straight through) that clips one channel out to a single pin. Any Wick or
 * Hemp Thread touching it, or a component's single pin face, joins that channel. Use it to step the channel (sneak to step back): 0 to 7,
 * then C, the carry. Its band shows the channel in the resistor colour code, the carry in grey.
 */
public class RoachClipBlock extends RibbonBlock {
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, ComponentType.BUS - 1);

    public RoachClipBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CHANNEL, 0));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHANNEL);
    }

    @Override protected VoxelShape core() { return Block.box(4, 4, 4, 12, 12, 12); }

    public static int channel(BlockState state) { return state.getValue(CHANNEL); }

    /** A channel's name: its number, or C for the carry. */
    public static String name(int channel) { return channel == ComponentType.BUS - 1 ? "C" : Integer.toString(channel); }

    /** A channel's colour: the resistor colour code's digit, the carry grey. */
    public static int colour(int channel) {
        return channel == ComponentType.BUS - 1 ? 0x808080 : com.malevolentgods.fourtwenty.domain.ResistorCode.digit(channel);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                                                  @Nonnull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int next = Math.floorMod(channel(state) + (player.isShiftKeyDown() ? -1 : 1), ComponentType.BUS);
        level.setBlock(pos, state.setValue(CHANNEL, next), 3);
        BIGrid.changed(level);
        player.displayClientMessage(Component.translatable("message.fourtwenty.roach_clip.channel", name(next)), true);
        return InteractionResult.SUCCESS;
    }
}
