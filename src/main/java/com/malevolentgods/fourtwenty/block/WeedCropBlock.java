package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.domain.GrowRules;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.power.GrowOpBlock;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nonnull;

public class WeedCropBlock extends CropBlock implements EntityBlock {
    /** The planted strain; growth and harvest keep it (vanilla's getStateForAge would reset it). */
    public static final EnumProperty<Strain> STRAIN = EnumProperty.create("strain", Strain.class);

    public WeedCropBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STRAIN, Strain.LANDRACE));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STRAIN);
    }

    /** This state at another age, same strain. */
    public BlockState atAge(BlockState state, int age) {
        return state.setValue(getAgeProperty(), Math.min(age, getMaxAge()));
    }

    @Override
    protected void randomTick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || !plotted(level, pos, state.getValue(STRAIN))) return;
        if (state.getValue(STRAIN).grade() > 1 && random.nextDouble() >= ProcessingConfig.gradeSpeed(state.getValue(STRAIN))) return;
        if (level.getRawBrightness(pos, 0) >= 9) {
            int age = getAge(state);
            if (age < getMaxAge()) {
                float speed = getGrowthSpeed(state, level, pos);
                if (net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (25.0F / speed) + 1) == 0)) {
                    level.setBlock(pos, atAge(state, age + 1), 2);
                    net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
                }
            }
        }
    }

    /** Whether the strain's grade will grow on what's under {@code pos}. */
    public static boolean plotted(LevelReader level, BlockPos pos, Strain strain) {
        int need = GrowRules.gradePlot(strain.grade());
        return need == 0 || level.getBlockState(pos.below()).getBlock() instanceof GrowOpBlock plot && plot.plotLevel() >= need;
    }

    /** Hybrids take their time: bone meal does nothing for them. */
    @Override
    public boolean isValidBonemealTarget(@Nonnull LevelReader level, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        return state.getValue(STRAIN).grade() <= 1 && super.isValidBonemealTarget(level, pos, state);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new WeedCropBlockEntity(pos, state); }

    @Override
    public void growCrops(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        level.setBlock(pos, atAge(state, getAge(state) + getBonemealAgeIncrease(level)), 2);
    }

    @Override
    public @Nonnull ItemStack getCloneItemStack(@Nonnull LevelReader level, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        ItemStack seed = ModDataComponents.withStrain(new ItemStack(ModItems.WEED_SEEDS.get()), state.getValue(STRAIN));
        return ModDataComponents.withTraits(seed, WeedCropBlockEntity.at(level, pos));
    }

    @Override
    protected @Nonnull ItemLike getBaseSeedId() {
        return ModItems.WEED_SEEDS.get();
    }

    /** Roll standard loot once, retaining one seed for the crop that remains planted. */
    @Override
    protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level,
            @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        // An earlier interaction listener may already have harvested this position. The
        // caller can still hold the pre-event state (notably Actually Harvest with an empty hand).
        if (!isMaxAge(state) || !level.getBlockState(pos).equals(state)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            var drops = getDrops(state, server, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
            if (com.malevolentgods.fourtwenty.power.GrowOps.bigHarvest(level, pos)) com.malevolentgods.fourtwenty.power.GrowOps.raise(state, server, pos, drops);
            ItemStack crossed = Pollination.harvest(state, server, pos, level.getBlockEntity(pos) instanceof WeedCropBlockEntity crop ? crop : null);
            if (crossed != null) popResource(level, pos, crossed);
            if (!level.setBlock(pos, atAge(state, 0), 3)) return InteractionResult.PASS;
            boolean retainedSeed = false;
            for (ItemStack stack : drops) {
                if (!retainedSeed && stack.is(ModItems.WEED_SEEDS.get()) && !stack.isEmpty()) {
                    stack.shrink(1);
                    retainedSeed = true;
                }
                popResource(level, pos, stack);
            }
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
