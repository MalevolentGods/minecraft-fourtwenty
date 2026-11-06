package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.power.BIBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** The Weedweeno: a small Lua computer that runs light shows on the Lit Bits within its range. Use it to open its editor. */
public class WeedweenoBlock extends BIBlock {
    public WeedweenoBlock(Properties properties) {
        super(Block.box(1, 0, 1, 15, 6, 15), properties);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new WeedweenoBlockEntity(pos, state);
    }

    /** Whether this player may edit and run programs here (the server can keep it to operators). */
    public static boolean mayProgram(Player player) {
        return !LightsConfig.operatorsOnly() || player.hasPermissions(2);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                                                  @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof WeedweenoBlockEntity weedweeno) {
            if (!mayProgram(sp)) {
                sp.displayClientMessage(Component.translatable("message.fourtwenty.weedweeno.operators"), true);
            } else {
                weedweeno.watch(sp);
                LightsNetwork.sendState(sp, weedweeno, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
