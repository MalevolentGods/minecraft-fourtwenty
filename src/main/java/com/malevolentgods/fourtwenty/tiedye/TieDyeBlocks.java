package com.malevolentgods.fourtwenty.tiedye;

import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import javax.annotation.Nonnull;

/** The tie-dyed blocks (research R1, R2): the pattern is a block state property, the colourway a block entity. */
public final class TieDyeBlocks {
    private TieDyeBlocks() {}

    public static final IntegerProperty PATTERN = IntegerProperty.create("pattern", 0, TieDyeRules.PATTERNS.size() - 1);

    /** The pattern an item being placed carries (0 if it has none). */
    public static int pattern(ItemStack stack) {
        TieDye look = stack.get(ModDataComponents.TIE_DYE.get());
        return look == null ? 0 : look.pattern();
    }

    /** The look of a placed tie-dyed block, or null. */
    public static TieDye look(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TieDyeBlockEntity be ? be.look() : null;
    }

    /** A tie-dyed hemp block: Natural Hemp in a pattern and colourway. */
    public static final class Hemp extends Block implements EntityBlock {
        public Hemp(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(PATTERN, 0));
        }

        @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(PATTERN); }

        @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
            return defaultBlockState().setValue(PATTERN, pattern(context.getItemInHand()));
        }

        @Override public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new TieDyeBlockEntity(pos, state); }

        @Override public void setPlacedBy(@Nonnull net.minecraft.world.level.Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                                          net.minecraft.world.entity.LivingEntity placer, @Nonnull ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            TieDyeFits.refresh(level, pos);
        }

        @Override protected void onRemove(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.Level level, @Nonnull BlockPos pos,
                                          @Nonnull BlockState next, boolean moved) {
            super.onRemove(state, level, pos, next, moved);
            if (!state.is(next.getBlock())) TieDyeFits.refresh(level, pos);
        }
    }
}
