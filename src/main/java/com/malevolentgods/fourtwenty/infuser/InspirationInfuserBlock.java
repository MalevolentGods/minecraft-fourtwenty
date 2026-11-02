package com.malevolentgods.fourtwenty.infuser;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

/**
 * The Inspiration Infuser (Steel or Lab: the same machine in two finishes). Using it opens the player's own pool; there
 * is nothing stored in the block, so every player who uses it sees their own inspiration and their own mix. Without power
 * it shuts down: it goes dark, and its screen won't mix until there's power again.
 */
public class InspirationInfuserBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<InspirationInfuserBlock> CODEC = simpleCodec(InspirationInfuserBlock::new);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int CHECK_TICKS = 20;

    /** Set by the client setup: open the Infuser screen for the block at this position. */
    public static java.util.function.Consumer<BlockPos> clientOpen = pos -> {};

    /** Built for a north-facing machine (tools/generate_infuser_model.py): plinth, basin and rim, jar rail, arm. */
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES.put(facing, Shapes.or(rotate(0, 0, 0, 16, 3, 16, facing), rotate(1.4, 3, 0.4, 14.6, 9.5, 13.6, facing),
                    rotate(1, 3, 13.4, 15, 9.7, 15.6, facing), rotate(7, 3, 15.4, 9, 14.6, 16, facing), rotate(7.1, 10.9, 6.2, 8.9, 14.6, 15.4, facing)));
        }
    }

    private static VoxelShape rotate(double x0, double y0, double z0, double x1, double y1, double z1, Direction facing) {
        return switch (facing) {
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    public InspirationInfuserBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override protected @Nonnull MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, POWERED); }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
                                                     @Nonnull CollisionContext context) { return SHAPES.get(state.getValue(FACING)); }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            refresh(level, pos);
            check(level, pos);
            PacketDistributor.sendToPlayer(sp, new Infuser.Sync(Infuser.get(sp).save()));
        } else if (level.isClientSide) {
            clientOpen.accept(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Sets the block to whether it has power right now. */
    public static void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean powered = Infusion.powered(level, pos);
        if (state.getBlock() instanceof InspirationInfuserBlock && state.getValue(POWERED) != powered) level.setBlock(pos, state.setValue(POWERED, powered), 3);
    }

    private void check(Level level, BlockPos pos) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, CHECK_TICKS);
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        check(level, pos);
    }

    @Override protected void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        refresh(level, pos);
        check(level, pos);
    }

    /** Machines placed before this had no check running: a random tick gets theirs going. */
    @Override protected boolean isRandomlyTicking(@Nonnull BlockState state) { return true; }

    @Override protected void randomTick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        check(level, pos);
    }

    /** A few lime motes rise off the pool. */
    @Override public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (!state.getValue(POWERED) || random.nextInt(4) != 0) return;
        double x = 8 + random.nextGaussian() * 2.5, z = 7 + random.nextGaussian() * 2.5;
        switch (state.getValue(FACING)) {
            case EAST -> { double t = x; x = 16 - z; z = t; }
            case SOUTH -> { x = 16 - x; z = 16 - z; }
            case WEST -> { double t = x; x = z; z = 16 - t; }
            default -> {}
        }
        level.addParticle(new DustParticleOptions(new Vector3f(0.72f, 0.94f, 0.42f), 0.6f), pos.getX() + x / 16, pos.getY() + 7.4 / 16,
                pos.getZ() + z / 16, 0, 0.02, 0);
    }
}
