package com.malevolentgods.fourtwenty.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * A road: a path block in three tiers (cobble path, paved road, yellow brick road), each faster to
 * walk on than the last. Like a dirt path it stands 15/16 high and shapes its light occlusion to
 * that, so the top pixel row of a block beside it still shows: a lamp or glowstone set flush beside
 * a road reads as a strip of light along it.
 *
 * Speed: everything that walks on it (players, mobs, colonists) moves 5, 10 or 15% faster. Vanilla
 * multiplies an entity's velocity by the block's speed factor each tick, and ground friction
 * (0.6 x 0.91) carries it into the next, so the factor compounds: a factor m gives a steady speed
 * (1 - 0.546) / (1 - 0.546 m) times normal. {@link #speedFactorFor} solves that for the wanted gain.
 *
 * Joining: like the rugs, a road knows which of its eight neighbours are the same road, and each
 * quarter of its top is drawn as an outer corner, an edge, an inner corner or field
 * (tools/generate_paths.py), so the border runs only round the outside of the whole road.
 * Minecraft only tells a block about its side neighbours, so a road placed or removed refreshes its
 * diagonal neighbours itself.
 */
public class HempPathBlock extends Block {
    public static final MapCodec<HempPathBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(b -> b.tier), propertiesCodec()).apply(i, HempPathBlock::new));
    public static final BooleanProperty NORTH = BooleanProperty.create("north"), EAST = BooleanProperty.create("east"),
            SOUTH = BooleanProperty.create("south"), WEST = BooleanProperty.create("west"),
            NORTH_EAST = BooleanProperty.create("northeast"), SOUTH_EAST = BooleanProperty.create("southeast"),
            SOUTH_WEST = BooleanProperty.create("southwest"), NORTH_WEST = BooleanProperty.create("northwest");
    private static final int[][] OFFSETS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}};
    private static final BooleanProperty[] PROPERTIES = {NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST};
    protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);
    /** Ground friction a walker carries from tick to tick (block friction 0.6 x air drag 0.91). */
    private static final double CARRY = 0.6 * 0.91;

    private final int tier;

    public HempPathBlock(int tier, Properties properties) {
        super(properties.speedFactor(speedFactorFor(gainFor(tier))));
        this.tier = tier;
        BlockState state = stateDefinition.any();
        for (BooleanProperty p : PROPERTIES) state = state.setValue(p, false);
        registerDefaultState(state);
    }

    /** The walking speed gain of a tier: 5, 10, 15%. */
    public static double gainFor(int tier) { return 0.05 * tier; }

    /** The block speed factor that makes an entity's steady walking speed (1 + gain) times normal. */
    public static float speedFactorFor(double gain) {
        return (float) ((1 - (1 - CARRY) / (1 + gain)) / CARRY);
    }

    public int tier() { return tier; }

    @Override protected @Nonnull MapCodec<? extends Block> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPERTIES);
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    /** Light and face culling follow the 15/16 shape, as for a dirt path: the strip above shows. */
    @Override protected boolean useShapeForLightOcclusion(@Nonnull BlockState state) { return true; }

    @Override protected boolean isPathfindable(@Nonnull BlockState state, @Nonnull PathComputationType type) { return false; }

    /** This road's state with its connections to the same road around it. */
    public BlockState connected(LevelReader level, BlockPos pos, BlockState state) {
        for (int i = 0; i < OFFSETS.length; i++) {
            state = state.setValue(PROPERTIES[i], level.getBlockState(pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1])).is(this));
        }
        return state;
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return connected(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbor,
                                                       @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? connected(level, pos, state) : state;
    }

    private void refreshDiagonals(Level level, BlockPos pos) {
        for (int i = 4; i < OFFSETS.length; i++) {
            BlockPos p = pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1]);
            BlockState there = level.getBlockState(p);
            if (there.is(this)) {
                BlockState updated = connected(level, p, there);
                if (updated != there) level.setBlock(p, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this)) refreshDiagonals(level, pos);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!level.isClientSide && !newState.is(this)) refreshDiagonals(level, pos);
    }
}
