package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The Hookah: a shared session for anyone nearby and, while it burns, a generator of Inspo at its tier. Double Hose and Big Bertha are the
 * same block, upgraded: more hoses, more power.
 */
public class WeedBongBlock extends BaseEntityBlock {
    private final com.malevolentgods.fourtwenty.domain.BIRules.Tier tier;
    
    // Properties for the bong state
    public static final BooleanProperty HAS_WATER = BooleanProperty.create("has_water");
    public static final IntegerProperty WEED_LEVEL = IntegerProperty.create("weed_level", 0, 4); // 0 = empty, 4 = full
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    /** The way the placer was looking, so the top texture reads upright from where they stood. */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    
    // Bong shape - roughly 3x3x5 blocks in size but as a single block
    /** The cabinet (9 x 9 x 9) and the stem, tray and bowl above it (tools/generate_hookah_model.py). */
    private static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(Block.box(3.5, 0.0, 3.5, 12.5, 9.0, 12.5),
            Block.box(5.6, 9.0, 5.6, 10.4, 15.7, 10.4));
    
    public WeedBongBlock(Properties properties) {
        this(com.malevolentgods.fourtwenty.domain.BIRules.Tier.PUFF, properties);
    }

    public WeedBongBlock(com.malevolentgods.fourtwenty.domain.BIRules.Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HAS_WATER, false)
            .setValue(WEED_LEVEL, 0)
            .setValue(LIT, false)
            .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    @Nonnull
    public MapCodec<WeedBongBlock> codec() {
        return MapCodec.unit(this);
    }

    public com.malevolentgods.fourtwenty.domain.BIRules.Tier tier() { return tier; }

    @Override
    protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) com.malevolentgods.fourtwenty.power.BIGrid.changed(level);
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_WATER, WEED_LEVEL, LIT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /** Structure/blueprint rotation (e.g. a rotated colony building) turns the bong with it. */
    @Override
    protected @Nonnull BlockState rotate(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @Nonnull BlockState mirror(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Nonnull
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WeedBongBlockEntity bongEntity) {
            player.openMenu(bongEntity);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new WeedBongBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.WEED_BONG.get(), WeedBongBlockEntity::tick);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            com.malevolentgods.fourtwenty.power.BIGrid.changed(level);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WeedBongBlockEntity bongEntity) {
                bongEntity.beginRemoval();
                net.minecraft.world.Containers.dropContents(level, pos, bongEntity);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** While it burns, smoke puffs off the bowl, now and then a wisp of white. */
    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(2) != 0) return;
        double x = pos.getX() + 0.5 + random.nextGaussian() * 0.04, y = pos.getY() + 15.9 / 16, z = pos.getZ() + 0.5 + random.nextGaussian() * 0.04;
        level.addParticle(random.nextInt(4) == 0 ? net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE : net.minecraft.core.particles.ParticleTypes.SMOKE,
                x, y, z, 0, 0.03, 0);
    }
}
