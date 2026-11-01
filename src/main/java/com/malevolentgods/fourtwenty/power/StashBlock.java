package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Batteries: Stash Jar, Stash Box and Mother Stash. Right-click for the gauge. */
public class StashBlock extends BIBlock {
    private final BIRules.Tier tier;
    private final long capacity;

    public StashBlock(BIRules.Tier tier, long capacity, VoxelShape shape, Properties properties) {
        super(shape, properties);
        this.tier = tier;
        this.capacity = capacity;
    }

    public BIRules.Tier tier() { return tier; }

    public long capacity() { return capacity; }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new StashBlockEntity(pos, state);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StashBlockEntity stash) player.openMenu(stash);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
