package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Wiring: Hemp, Rolled and Glass Wick. It glows while it carries power and burns out past two Hookahs' worth of its tier. Each side is open,
 * joined to another Wick, or plugged into a BI block: a plug's flange sits on the block's edge and its tail reaches into the block's space, so
 * a Wick meets a narrow block instead of stopping short of it. A LOW plug drops down first, into the body of a short block like a Hookah or Stash Jar,
 * whose side doesn't reach wire height.
 */
public class WickBlock extends Block {
    public enum Link implements StringRepresentable {
        NONE, WIRE, PLUG, LOW;

        @Override public @Nonnull String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final Map<Direction, EnumProperty<Link>> SIDES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final int GLOW_TICKS = 30;
    private static final Map<Level, Map<BlockPos, Long>> CARRIED = new java.util.WeakHashMap<>();

    static {
        for (Direction d : Direction.values()) SIDES.put(d, EnumProperty.create(d.getName(), Link.class));
        ARMS.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6));
        ARMS.put(Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6, 6, 6, 10, 10));
        ARMS.put(Direction.EAST, Block.box(10, 6, 6, 16, 10, 10));
        ARMS.put(Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10));
        ARMS.put(Direction.UP, Block.box(6, 10, 6, 10, 16, 10));
    }

    private final BIRules.Tier tier;
    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public WickBlock(BIRules.Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        BlockState state = stateDefinition.any().setValue(LIT, false);
        for (var side : SIDES.values()) state = state.setValue(side, Link.NONE);
        registerDefaultState(state);
        for (BlockState s : stateDefinition.getPossibleStates()) {
            VoxelShape shape = CORE;
            for (Direction d : Direction.values()) if (s.getValue(SIDES.get(d)) != Link.NONE) shape = Shapes.or(shape, ARMS.get(d));
            shapes.put(s, shape);
        }
    }

    public BIRules.Tier tier() { return tier; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
        builder.add(LIT);
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return shapes.get(state);
    }

    private static Link link(BlockState neighbour, Direction side) {
        if (neighbour.getBlock() instanceof WickBlock) return Link.WIRE;
        if (!neighbour.is(ModPower.BI_BLOCKS)) return Link.NONE;
        boolean low = neighbour.getBlock() instanceof com.malevolentgods.fourtwenty.block.WeedBongBlock || neighbour.is(ModPower.STASH_JAR.get());
        return low && side.getAxis().isHorizontal() ? Link.LOW : Link.PLUG;
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) state = state.setValue(SIDES.get(d), link(context.getLevel().getBlockState(context.getClickedPos().relative(d)), d));
        return state;
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbour, @Nonnull LevelAccessor level,
                                                        @Nonnull BlockPos pos, @Nonnull BlockPos neighbourPos) {
        return state.setValue(SIDES.get(direction), link(neighbour, direction));
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) BIGrid.changed(level);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) BIGrid.changed(level);
        super.onRemove(state, level, pos, next, moved);
    }

    /** Power went through: light it, and it stays lit until a whole glow passes with no power. */
    public static void carry(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WickBlock)) return;
        CARRIED.computeIfAbsent(level, l -> new HashMap<>()).put(pos.immutable(), level.getGameTime());
        if (!state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, true), 2);
        if (!level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) level.scheduleTick(pos, state.getBlock(), GLOW_TICKS);
    }

    @Override protected void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        var carried = CARRIED.computeIfAbsent(level, l -> new HashMap<>());
        long left = carried.getOrDefault(pos, Long.MIN_VALUE / 2) + GLOW_TICKS - level.getGameTime();
        if (left > 0) {
            level.scheduleTick(pos, this, (int) left);
            return;
        }
        carried.remove(pos);
        if (state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, false), 2);
    }
}
