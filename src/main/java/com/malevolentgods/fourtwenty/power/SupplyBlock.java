package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.Units;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A Bottomless Stash of a tier, or the Bench Rig. Use the Rig to step its voltage; sneak-use to step its current limit. */
public class SupplyBlock extends BIBlock {
    private final BIRules.Tier tier;
    private final boolean rig;

    public SupplyBlock(BIRules.Tier tier, boolean rig, Properties properties) {
        super(Block.box(1, 0, 1, 15, 14, 15), properties);
        this.tier = tier;
        this.rig = rig;
    }

    public BIRules.Tier tier() { return tier; }

    public boolean rig() { return rig; }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new SupplyBlockEntity(pos, state); }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                                                  @Nonnull BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof SupplyBlockEntity supply)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (rig) {
            if (player.isShiftKeyDown()) supply.stepAmps(1);
            else supply.stepVolts(1);
        }
        player.displayClientMessage(rig
                ? Component.translatable("message.fourtwenty.bench_rig", Units.format(supply.volts(), Units.VOLTS), Units.format(supply.amps(), Units.AMPS))
                : Component.translatable("message.fourtwenty.bottomless_stash"), true);
        return InteractionResult.SUCCESS;
    }
}
