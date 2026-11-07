package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.power.BIBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** The Re-Up Station, one per tier: hang a piece of gear or a Dab Cell on it, or stand on its pad, and it charges from its battery. Use it empty-handed for its screen. */
public class ReUpStationBlock extends BIBlock {
    private final BIRules.Tier tier;

    public ReUpStationBlock(BIRules.Tier tier, Properties properties) {
        super(Block.box(0, 0, 0, 16, 12, 16), properties);
        this.tier = tier;
    }

    public BIRules.Tier tier() { return tier; }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ReUpStationBlockEntity(pos, state);
    }

    @Override protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                 @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (Gear.piece(stack).isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ReUpStationBlockEntity station) {
            if (station.hung().isEmpty()) {
                station.hang(stack.split(1));
                player.displayClientMessage(Component.translatable("message.fourtwenty.re_up.hung", station.hung().getHoverName()), true);
            } else {
                player.openMenu(station);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ReUpStationBlockEntity station) player.openMenu(station);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ReUpStationBlockEntity station && !station.hung().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, station.takeDown());
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
