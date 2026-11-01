package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.TriState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * A Grow Op: a plot a crop sits on, like farmland, that grows it faster. Tier 1 needs no power and outside light; from tier 2 it lights its own
 * crop and runs by the shared power rule. Using the next tier's plot on one upgrades it in place, crop and all. A seed used on any face plants on top; shift-clicking with an empty hand opens its screen.
 */
public class GrowOpBlock extends BIBlock {
    private final int level;
    private final @Nullable BIRules.Tier tier;
    private final Supplier<? extends Block> next;

    /** {@code level} counts from 1; {@code tier} is null for a plot with no power. */
    public GrowOpBlock(int level, @Nullable BIRules.Tier tier, Supplier<? extends Block> next, Properties properties) {
        super(Block.box(0, 0, 0, 16, 16, 16), properties);
        this.level = level;
        this.tier = tier;
        this.next = next;
    }

    public int plotLevel() { return level; }

    public @Nullable BIRules.Tier tier() { return tier; }

    public static boolean isCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock || state.is(BlockTags.CROPS);
    }

    @Override public TriState canSustainPlant(@Nonnull BlockState state, @Nonnull BlockGetter getter, @Nonnull BlockPos soil, @Nonnull Direction facing, @Nonnull BlockState plant) {
        return facing == Direction.UP && isCrop(plant) ? TriState.TRUE : TriState.DEFAULT;
    }

    @Override public boolean isFertile(@Nonnull BlockState state, @Nonnull BlockGetter getter, @Nonnull BlockPos pos) {
        return true;
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new GrowOpBlockEntity(pos, state);
    }

    @Override protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos,
                                                                 @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem seed && isCrop(seed.getBlock().defaultBlockState())) {
            // Any face plants it, as if the top had been clicked; the screen only opens on a shift-click.
            var top = new BlockHitResult(pos.getCenter().add(0, 0.5, 0), Direction.UP, pos, false);
            InteractionResult planted = stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, hand, top));
            return planted.consumesAction() ? ItemInteractionResult.sidedSuccess(world.isClientSide) : ItemInteractionResult.CONSUME;
        }
        if (next == null || !stack.is(next.get().asItem())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!world.isClientSide) {
            world.setBlock(pos, next.get().defaultBlockState().setValue(FACING, state.getValue(FACING)), 3);
            stack.consume(1, player);
        }
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof GrowOpBlockEntity plot) player.openMenu(plot);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
