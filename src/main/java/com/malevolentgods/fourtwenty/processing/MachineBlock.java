package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.power.BIBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A powered trimming or curing machine, tiers 2 to 4. Which job and tier it is is the block's; the machine itself is one class. */
public class MachineBlock extends BIBlock {
    public enum Job { TRIM, CURE }

    private final Job job;
    private final BIRules.Tier tier;

    public MachineBlock(Job job, BIRules.Tier tier, Properties properties) {
        super(Block.box(0, 0, 0, 16, 16, 16), properties);
        this.job = job;
        this.tier = tier;
    }

    public Job job() { return job; }

    public BIRules.Tier tier() { return tier; }

    /** 1 Puff, 2 Toke, 3 Rip: tiers 2, 3 and 4 of the job, the rack and tray being tier 1 (index 0). */
    public int index() { return tier.ordinal() + 1; }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new MachineBlockEntity(pos, state);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) player.openMenu(machine);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
