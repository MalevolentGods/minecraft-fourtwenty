package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.blockentity.HotboxBlockEntity;
import com.malevolentgods.fourtwenty.domain.HotboxRules;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** A creative-only chunk loader: no recipe, no drops, and a survival player can't break it. LIT while it's holding its chunks. */
public class HotboxBlock extends Block implements EntityBlock {
    public static final MapCodec<HotboxBlock> CODEC = simpleCodec(HotboxBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public HotboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override protected @Nonnull MapCodec<? extends Block> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new HotboxBlockEntity(pos, state);
    }

    @Override @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.HOTBOX.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<HotboxBlockEntity>) (world, pos, blockState, box) ->
                HotboxBlockEntity.serverTick((ServerLevel) world, pos, blockState, box);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HotboxBlockEntity box) {
            player.displayClientMessage(box.ticksLeft() > 0
                    ? Component.translatable("block.fourtwenty.hotbox.status", HotboxRules.minutesLeft(box.ticksLeft()))
                    : Component.translatable("block.fourtwenty.hotbox.idle"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level instanceof ServerLevel server) HotboxBlockEntity.hold(server, pos, false);
        super.onRemove(state, level, pos, next, moved);
    }

    @Override public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.03, 0);
        }
    }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        tooltip.add(Component.translatable("block.fourtwenty.hotbox.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
