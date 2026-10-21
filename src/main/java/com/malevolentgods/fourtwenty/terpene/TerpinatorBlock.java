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
    /** Built for a north-facing machine (tools/generate_terpinator.py): cabinet and deck, then the still, condenser and tank. */
    private static final java.util.Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES.put(facing, Shapes.or(rotate(0.5, 0, 0.5, 15.5, 9, 15.5, facing), rotate(1, 9, 5, 15, 15.5, 15, facing),
                    rotate(10.5, 9, 1.5, 14.5, 12, 5.5, facing)));
        }
    }

    /** A box given for a north-facing machine, turned to {@code facing}. */
    private static VoxelShape rotate(double x0, double y0, double z0, double x1, double y1, double z1, Direction facing) {
        return switch (facing) {
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

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
                                                     @Nonnull CollisionContext context) { return SHAPES.get(state.getValue(FACING)); }

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

    /** Where the valve wheel sits on a north-facing machine (tools/generate_terpinator.py), in pixels. */
    private static final double VALVE_X = 7.25, VALVE_Z = 8.5, VALVE_TOP = 19.5;   // the hub tops out at 16.85; smoke is a few pixels across

    /**
     * A wisp of vapour from the still's valve while it works. It starts above the valve wheel: born
     * inside the wheel, the smoke flickered through its rim (that was the "flashing" on the knob).
     */
    @Override public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(3) != 0) return;
        double x = VALVE_X, z = VALVE_Z;
        switch (state.getValue(FACING)) {            // the blockstate turns the model; turn the valve with it
            case EAST -> { double t = x; x = 16 - z; z = t; }
            case SOUTH -> { x = 16 - x; z = 16 - z; }
            case WEST -> { double t = x; x = z; z = 16 - t; }
            default -> {}
        }
        level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + x / 16 + random.nextGaussian() * 0.03, pos.getY() + VALVE_TOP / 16,
                pos.getZ() + z / 16 + random.nextGaussian() * 0.03, 0, 0.02, 0);
    }
}
