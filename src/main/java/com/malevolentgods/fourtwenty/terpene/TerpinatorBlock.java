package com.malevolentgods.fourtwenty.terpene;

import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** The Terpinator: a brass-and-glass still on a cabinet. LIT while any of its processes runs. */
public class TerpinatorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TerpinatorBlock> CODEC = simpleCodec(TerpinatorBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 10, 15), Block.box(3, 10, 3, 13, 16, 13));

    public TerpinatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override protected @Nonnull MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, LIT); }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
                                                     @Nonnull CollisionContext context) { return SHAPE; }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TerpinatorBlockEntity t && player instanceof ServerPlayer sp) {
            sp.openMenu(t, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new TerpinatorBlockEntity(pos, state); }

    @Override public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
                                                                                     @Nonnull BlockEntityType<T> type) {
        if (level.isClientSide || type != ModTerpenes.TERPINATOR_ENTITY.get()) return null;
        return (l, p, s, e) -> TerpinatorBlockEntity.tick(l, p, s, (TerpinatorBlockEntity) e);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState,
                                      boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TerpinatorBlockEntity t) {
            Containers.dropContents(level, pos, t);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** A wisp of vapour from the still while it works. */
    @Override public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5 + random.nextGaussian() * 0.06, pos.getY() + 1.02,
                pos.getZ() + 0.5 + random.nextGaussian() * 0.06, 0, 0.02, 0);
    }
}
