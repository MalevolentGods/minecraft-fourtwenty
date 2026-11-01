package com.malevolentgods.fourtwenty.processing;

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

/** The research block: pick a study, load it, and keep a crop growing on a Grow Op within 8 blocks. */
public class ThinkTankBlock extends BIBlock {
    public ThinkTankBlock(Properties properties) {
        super(Block.box(0, 0, 0, 16, 16, 16), properties);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ThinkTankBlockEntity(pos, state);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ThinkTankBlockEntity tank) player.openMenu(tank);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
