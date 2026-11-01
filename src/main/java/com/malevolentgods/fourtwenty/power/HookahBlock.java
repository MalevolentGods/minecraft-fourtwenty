package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
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

/** The generators: Single Hose Hookah, Double Hose and Big Bertha. Right-click with weed to fuel one; the smoke gets heavier with the tier. */
public class HookahBlock extends BIBlock {
    private final BIRules.Tier tier;

    public HookahBlock(BIRules.Tier tier, Properties properties) {
        super(Block.box(3, 0, 3, 13, 16, 13), properties);
        this.tier = tier;
    }

    public BIRules.Tier tier() { return tier; }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new HookahBlockEntity(pos, state);
    }

    @Override protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                 @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (ProcessingConfig.fuel(stack) <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HookahBlockEntity hookah) hookah.fuel(stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HookahBlockEntity hookah) {
            player.displayClientMessage(Component.translatable("block.fourtwenty.power.stored", hookah.node().stored(), hookah.node().capacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (!state.getValue(LIT)) return;
        for (int i = 0; i <= tier.ordinal(); i++) {
            if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 1,
                    pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, 0.04, 0);
        }
    }
}
