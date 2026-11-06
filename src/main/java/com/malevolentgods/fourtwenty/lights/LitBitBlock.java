package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.power.BIBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A Lit Bit: an RGB LED lamp with + and − terminals (left and right, seen from its front). Its colour comes from a Weedweeno in range by
 * ID; how bright it is, from the voltage across it. Its ID is shown and changed in its screen (use it).
 */
public class LitBitBlock extends BIBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 15);

    public LitBitBlock(Properties properties) {
        super(Block.box(0, 0, 0, 16, 16, 16), properties.lightLevel(state -> state.getValue(LEVEL)));
        registerDefaultState(defaultBlockState().setValue(LEVEL, 0));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVEL);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new LitBitBlockEntity(pos, state);
    }

    /** Placed by hand: the ID last broken here, or the lowest one free nearby. The Sticky Finger may renumber it straight after. */
    @Override public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof LitBitBlockEntity lamp)) return;
        var ids = LitBitIds.of(server);
        int back = ids.takeBack(pos, level.getGameTime());
        lamp.assign(back > 0 ? back : ids.lowestFree(pos, LightsConfig.range()), back > 0);
        if (placer instanceof Player player) player.displayClientMessage(Component.translatable("message.fourtwenty.lit_bit.placed", lamp.id()), true);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                                                  @Nonnull BlockHitResult hit) {
        if (level.isClientSide && level.getBlockEntity(pos) instanceof LitBitBlockEntity lamp) com.malevolentgods.fourtwenty.client.LightsClient.openLitBit(pos, lamp.id());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof LitBitBlockEntity lamp && lamp.id() > 0) {
            var ids = LitBitIds.of(server);
            ids.gone(pos);
            ids.remember(pos, lamp.id(), level.getGameTime());
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
