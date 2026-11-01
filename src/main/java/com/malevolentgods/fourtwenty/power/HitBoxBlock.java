package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The transformer. Its front face carries the higher tier and its other faces the tier below, in either direction. Right-click switches it
 * between Toke/Puff and Rip/Toke. Wire it the wrong way round and the higher tier burns it out, like any part.
 */
public class HitBoxBlock extends BIBlock {
    public static final EnumProperty<BIRules.Tier> TIER = EnumProperty.create("tier", BIRules.Tier.class, BIRules.Tier.TOKE, BIRules.Tier.RIP);

    public HitBoxBlock(Properties properties) {
        super(Block.box(0, 0, 0, 16, 16, 16), properties);
        registerDefaultState(defaultBlockState().setValue(TIER, BIRules.Tier.TOKE));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TIER);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new HitBoxBlockEntity(pos, state);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide) {
            BIRules.Tier next = state.getValue(TIER) == BIRules.Tier.TOKE ? BIRules.Tier.RIP : BIRules.Tier.TOKE;
            level.setBlock(pos, state.setValue(TIER, next), 3);
            BIGrid.changed(level);
            player.displayClientMessage(Component.translatable("block.fourtwenty.hit_box.tier",
                    Component.translatable("tier.fourtwenty." + next.getSerializedName()),
                    Component.translatable("tier.fourtwenty." + next.below().getSerializedName())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
