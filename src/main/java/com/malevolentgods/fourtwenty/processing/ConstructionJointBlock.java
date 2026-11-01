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

/** The Construction Joint, Blunt, Spliff and Cannon: powered crafting benches that make more of the mod's building blocks, and more the better the tier. */
public class ConstructionJointBlock extends BIBlock {
    private final BIRules.Tier tier;

    public ConstructionJointBlock(BIRules.Tier tier, Properties properties) {
        super(Block.box(0, 0, 0, 16, 16, 16), properties);
        this.tier = tier;
    }

    public BIRules.Tier tier() { return tier; }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ConstructionBlockEntity(pos, state);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ConstructionBlockEntity bench) player.openMenu(bench);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
