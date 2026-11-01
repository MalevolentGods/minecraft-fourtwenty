package com.malevolentgods.fourtwenty.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;

/** The Construction Joint: a bench for the mod's building blocks. */
public class ConstructionJointBlock extends Block {
    public ConstructionJointBlock(Properties properties) { super(properties); }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider((id, inventory, who) -> new ConstructionMenu(id, inventory, ContainerLevelAccess.create(level, pos)), getName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
