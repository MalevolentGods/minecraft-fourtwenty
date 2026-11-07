package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.power.WickBlock.Colour;
import com.malevolentgods.fourtwenty.power.WickBlock.Link;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * A two-core cable (specs/009-twin-daisy-wicks): a supply (+) core and a return (−) core in one block, each rated as one single Wick of its
 * tier. A Twin Wick puts every powered block it touches across its cores, a Daisy Wick splices them into one series string (see
 * Network.walk). It joins cables of its own kind and colour, red and black single Wicks, and any powered block on any face.
 */
public class CableBlock extends Block {
    public enum Kind { TWIN, DAISY }

    private static final VoxelShape CORE = Block.box(5, 6, 5, 11, 10, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.NORTH, Block.box(5, 6, 0, 11, 10, 5));
        ARMS.put(Direction.SOUTH, Block.box(5, 6, 11, 11, 10, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6, 5, 5, 10, 11));
        ARMS.put(Direction.EAST, Block.box(11, 6, 5, 16, 10, 11));
        ARMS.put(Direction.DOWN, Block.box(5, 0, 6, 11, 6, 10));
        ARMS.put(Direction.UP, Block.box(5, 10, 6, 11, 16, 10));
    }

    private final Kind kind;
    private final BIRules.Tier tier;
    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public CableBlock(Kind kind, BIRules.Tier tier, Properties properties) {
        super(properties);
        this.kind = kind;
        this.tier = tier;
        BlockState state = stateDefinition.any().setValue(WickBlock.LIT, false).setValue(WickBlock.COLOUR, Colour.NONE);
        for (var side : WickBlock.SIDES.values()) state = state.setValue(side, Link.NONE);
        registerDefaultState(state);
        for (BlockState s : stateDefinition.getPossibleStates()) {
            VoxelShape shape = CORE;
            for (Direction d : Direction.values()) if (s.getValue(WickBlock.SIDES.get(d)) != Link.NONE) shape = Shapes.or(shape, ARMS.get(d));
            shapes.put(s, shape);
        }
    }

    public Kind kind() { return kind; }

    public BIRules.Tier tier() { return tier; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        WickBlock.SIDES.values().forEach(builder::add);
        builder.add(WickBlock.LIT, WickBlock.COLOUR);
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return shapes.get(state);
    }

    /** Two cables join when they're the same kind and the same colour. */
    public static boolean joins(BlockState a, BlockState b) {
        return a.getBlock() instanceof CableBlock ca && b.getBlock() instanceof CableBlock cb && ca.kind == cb.kind
                && a.getValue(WickBlock.COLOUR) == b.getValue(WickBlock.COLOUR);
    }

    /** A single Wick meets a cable only when it's red (the + core) or black (the − core). */
    public static boolean takesWick(BlockState wick) {
        if (!(wick.getBlock() instanceof WickBlock)) return false;
        Colour c = wick.getValue(WickBlock.COLOUR);
        return c == Colour.RED || c == Colour.BLACK;
    }

    /** Whether a cable plugs into {@code neighbour} on its face {@code face}: any face of a powered block, the Hit Box's four sides only. */
    public static boolean plugsInto(BlockState neighbour, Direction face) { return Terminal.pluggable(neighbour, face); }

    private static Link link(BlockState self, Direction side, BlockState neighbour) {
        if (neighbour.getBlock() instanceof CableBlock) return joins(self, neighbour) ? Link.WIRE : Link.NONE;
        if (neighbour.getBlock() instanceof WickBlock) return takesWick(neighbour) ? Link.WIRE : Link.NONE;
        return plugsInto(neighbour, side.getOpposite()) ? Link.PLUG : Link.NONE;
    }

    private BlockState linked(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) state = state.setValue(WickBlock.SIDES.get(d), link(state, d, level.getBlockState(pos.relative(d))));
        return state;
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return linked(defaultBlockState().setValue(WickBlock.COLOUR, WickBlock.colourOf(context.getItemInHand())), context.getLevel(), context.getClickedPos());
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbour, @Nonnull LevelAccessor level,
                                                        @Nonnull BlockPos pos, @Nonnull BlockPos neighbourPos) {
        return state.setValue(WickBlock.SIDES.get(direction), link(state, direction, neighbour));
    }

    /** Use a dye on a cable to colour it; it then joins only cables of its kind in that colour. */
    @Override protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                               @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Colour colour = Colour.of(dye.getDyeColor());
        if (state.getValue(WickBlock.COLOUR) == colour) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            level.setBlock(pos, linked(state.setValue(WickBlock.COLOUR, colour), level, pos), 3);
            BIGrid.changed(level);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) BIGrid.changed(level);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) BIGrid.changed(level);
        super.onRemove(state, level, pos, next, moved);
    }

    @Override protected void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        WickBlock.glowTick(state, level, pos, this);
    }
}
