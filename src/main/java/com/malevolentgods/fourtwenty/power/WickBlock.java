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
 * Wiring: Hemp, Rolled, Glass and Gold Wick, a single conductor. It glows while it carries power and burns out past its ratings. Wicks come
 * undyed or in the 16 dye colours (use a dye on one), and a Wick joins only Wicks of its own colour, like insulated wire, so a supply line
 * and a return line can run side by side. Each side is open, joined to another Wick, or plugged into a terminal face of a BI block: a plug
 * tapers to the block's edge, and the block draws the rest of the fitting for that side (client/PortModel).
 */
public class WickBlock extends Block {
    public enum Link implements StringRepresentable {
        NONE, WIRE, PLUG;

        @Override public @Nonnull String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    /** A Wick's colour: undyed, or one of the 16 dyes. */
    public enum Colour implements StringRepresentable {
        NONE, WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, GRAY, LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN, RED, BLACK;

        @Override public @Nonnull String getSerializedName() { return name().toLowerCase(Locale.ROOT); }

        public static Colour of(net.minecraft.world.item.DyeColor dye) { return values()[dye.getId() + 1]; }

        /** The dye this colour is, or null when undyed. */
        public net.minecraft.world.item.DyeColor dye() { return this == NONE ? null : net.minecraft.world.item.DyeColor.byId(ordinal() - 1); }
    }

    public static final EnumProperty<Colour> COLOUR = EnumProperty.create("colour", Colour.class);
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
        BlockState state = stateDefinition.any().setValue(LIT, false).setValue(COLOUR, Colour.NONE);
        for (var side : SIDES.values()) state = state.setValue(side, Link.NONE);
        registerDefaultState(state);
        for (BlockState s : stateDefinition.getPossibleStates()) {
            VoxelShape shape = CORE;
            for (Direction d : Direction.values()) if (s.getValue(SIDES.get(d)) != Link.NONE) shape = Shapes.or(shape, ARMS.get(d));
            shapes.put(s, shape);
        }
    }

    public BIRules.Tier tier() { return tier; }

    /** This wire's resistance and current rating against a Wick of its tier (Hemp Thread is thin). */
    public double ohmsScale() { return 1; }

    public double ampsScale() { return 1; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
        builder.add(LIT, COLOUR);
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return shapes.get(state);
    }

    /** Whether two Wicks join: both Wicks, and the same colour. */
    public static boolean joins(BlockState a, BlockState b) {
        return a.getBlock() instanceof WickBlock && b.getBlock() instanceof WickBlock && a.getValue(COLOUR) == b.getValue(COLOUR);
    }

    /** How this Wick meets {@code neighbour} on {@code side}: a Wick of its colour, a BI block's terminal face, or nothing. */
    private static Link link(BlockState self, Direction side, BlockState neighbour) {
        if (neighbour.getBlock() instanceof WickBlock) return joins(self, neighbour) ? Link.WIRE : Link.NONE;
        if (neighbour.getBlock() instanceof CableBlock) return CableBlock.takesWick(self) ? Link.WIRE : Link.NONE;
        // any colour of Wick or Hemp Thread plugs into a component's pin face
        if (com.malevolentgods.fourtwenty.electronics.ComponentBlock.singlePinAt(neighbour, side.getOpposite()) >= 0) return Link.PLUG;
        // and into a Roach Clip, on its channel
        if (neighbour.getBlock() instanceof com.malevolentgods.fourtwenty.electronics.RoachClipBlock) return Link.PLUG;
        // only red (+) and black (−) Wicks plug into blocks, on any face
        return Terminal.signOf(self) != null && Terminal.pluggable(neighbour, side.getOpposite()) ? Link.PLUG : Link.NONE;
    }

    private BlockState linked(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) state = state.setValue(SIDES.get(d), link(state, d, level.getBlockState(pos.relative(d))));
        return state;
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return linked(defaultBlockState().setValue(COLOUR, colourOf(context.getItemInHand())), context.getLevel(), context.getClickedPos());
    }

    /** The colour a Wick or cable item places in: red and black Wicks are crafted with it (its block_state component). */
    public static Colour colourOf(net.minecraft.world.item.ItemStack stack) {
        var props = stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_STATE, net.minecraft.world.item.component.BlockItemStateProperties.EMPTY);
        Colour c = props.get(COLOUR);
        return c == null ? Colour.NONE : c;
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbour, @Nonnull LevelAccessor level,
                                                        @Nonnull BlockPos pos, @Nonnull BlockPos neighbourPos) {
        return state.setValue(SIDES.get(direction), link(state, direction, neighbour));
    }

    /** Use a dye on a Wick to colour it; it then joins only Wicks of that colour. */
    @Override protected @Nonnull net.minecraft.world.ItemInteractionResult useItemOn(@Nonnull net.minecraft.world.item.ItemStack stack, @Nonnull BlockState state,
            @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.world.entity.player.Player player, @Nonnull net.minecraft.world.InteractionHand hand,
            @Nonnull net.minecraft.world.phys.BlockHitResult hit) {
        if (!(stack.getItem() instanceof net.minecraft.world.item.DyeItem dye)) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Colour colour = Colour.of(dye.getDyeColor());
        if (state.getValue(COLOUR) == colour) return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            BlockState next = linked(state.setValue(COLOUR, colour), level, pos);
            level.setBlock(pos, next, 3);
            for (Direction d : Direction.values()) {
                BlockPos at = pos.relative(d);
                BlockState around = level.getBlockState(at);
                if (around.getBlock() instanceof WickBlock) level.setBlock(at, around.setValue(SIDES.get(d.getOpposite()), link(around, d.getOpposite(), next)), 3);
                else if (around.getBlock() instanceof CableBlock) level.setBlock(at, around.updateShape(d.getOpposite(), next, level, at, pos), 3);
            }
            BIGrid.changed(level);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) BIGrid.changed(level);
        // placed some other way than by hand (a command, a paste): reach toward its neighbours now, as it would have when placed
        if (!level.isClientSide && !old.is(this)) {
            BlockState linked = linked(state, level, pos);
            if (linked != state) level.setBlock(pos, linked, 2);
        }
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) BIGrid.changed(level);
        super.onRemove(state, level, pos, next, moved);
    }

    /** Power went through: light it, and it stays lit until a whole glow passes with no power. */
    public static void carry(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WickBlock) && !(state.getBlock() instanceof CableBlock)) return;
        CARRIED.computeIfAbsent(level, l -> new HashMap<>()).put(pos.immutable(), level.getGameTime());
        if (!state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, true), 2);
        if (!level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) level.scheduleTick(pos, state.getBlock(), GLOW_TICKS);
    }

    @Override protected void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        glowTick(state, level, pos, this);
    }

    /** A Wick's or cable's glow: it goes out once a whole glow passes with no power. */
    static void glowTick(BlockState state, ServerLevel level, BlockPos pos, Block block) {
        var carried = CARRIED.computeIfAbsent(level, l -> new HashMap<>());
        long left = carried.getOrDefault(pos, Long.MIN_VALUE / 2) + GLOW_TICKS - level.getGameTime();
        if (left > 0) {
            level.scheduleTick(pos, block, (int) left);
            return;
        }
        carried.remove(pos);
        if (state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, false), 2);
    }
}
