package com.malevolentgods.fourtwenty.electronics;

import com.malevolentgods.fourtwenty.power.BIGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An electronic component (a Resinator, a Kief Cap, a Nug...): its pins are on faces fixed relative to the way it faces, and any Wick or
 * Hemp Thread on a pin face joins that pin. Use it to step its value (sneak to step back); a Toggle Joint flips, a Puff Button closes for
 * a second, a Kill Switch closes while it has a redstone signal. {@code ON} shows a switch closed, or a logic part's output high.
 */
public class ComponentBlock extends Block implements EntityBlock {
    /** Where its front points, any of the six ways; with {@link #SPIN}, any of the 24 orientations (Orientation). */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty SPIN =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("spin", 0, 3);
    public static final BooleanProperty ON = BooleanProperty.create("on");
    /** Mounted flush on a Rolling Tray behind it: a thin plate with no poles of its own, its pins wired through the tray. */
    public static final BooleanProperty MOUNTED = BooleanProperty.create("mounted");
    private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);
    /** A mounted part's plate, by the side its back is on. */
    private static final java.util.Map<Direction, VoxelShape> PLATES = new java.util.EnumMap<>(Direction.class);

    static {
        PLATES.put(Direction.NORTH, Block.box(0, 0, 0, 16, 16, 4));
        PLATES.put(Direction.SOUTH, Block.box(0, 0, 12, 16, 16, 16));
        PLATES.put(Direction.WEST, Block.box(0, 0, 0, 4, 16, 16));
        PLATES.put(Direction.EAST, Block.box(12, 0, 0, 16, 16, 16));
        PLATES.put(Direction.DOWN, Block.box(0, 0, 0, 16, 4, 16));
        PLATES.put(Direction.UP, Block.box(0, 12, 0, 16, 16, 16));
    }
    public static final int BUTTON_TICKS = 20;

    private final ComponentType type;

    public ComponentBlock(ComponentType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPIN, 0).setValue(ON, false).setValue(MOUNTED, false));
    }

    public ComponentType type() { return type; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, SPIN, ON, MOUNTED); }

    /**
     * Placed with its front toward the player, whichever of the six ways they look; one placed facing up or down has its top away from
     * them, so it reads the right way round. The Twister turns it after.
     */
    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        // on a Rolling Tray's front: mounted flush, facing out from it
        Direction face = context.getClickedFace();
        BlockState against = context.getLevel().getBlockState(context.getClickedPos().relative(face.getOpposite()));
        if (type.mountable() && against.getBlock() instanceof RollingTrayBlock && RollingTrayBlock.front(against) == face) {
            int spin = face.getAxis().isVertical() ? Orientation.spinFor(face, context.getHorizontalDirection()) : 0;
            return defaultBlockState().setValue(FACING, face).setValue(SPIN, spin).setValue(MOUNTED, true);
        }
        Direction facing = context.getNearestLookingDirection().getOpposite();
        int spin = facing.getAxis().isVertical() ? Orientation.spinFor(facing, context.getHorizontalDirection()) : 0;
        return defaultBlockState().setValue(FACING, facing).setValue(SPIN, spin);
    }

    @Override protected @Nonnull BlockState rotate(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.block.Rotation rotation) {
        int turns = switch (rotation) { case CLOCKWISE_90 -> 1; case CLOCKWISE_180 -> 2; case COUNTERCLOCKWISE_90 -> 3; default -> 0; };
        for (int i = 0; i < turns; i++) state = TwisterItem.turn(state, Direction.Axis.Y, true);
        return state;
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return state.getValue(MOUNTED) ? PLATES.get(state.getValue(FACING).getOpposite()) : SHAPE;
    }

    /** The side a mounted part's tray is on, or null if it isn't mounted. */
    public static @Nullable Direction mountedOn(BlockState state) {
        return state.getBlock() instanceof ComponentBlock && state.getValue(MOUNTED) ? state.getValue(FACING).getOpposite() : null;
    }

    /** A mounted part needs its tray: without it, it falls off. */
    @Override protected boolean canSurvive(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.LevelReader level, @Nonnull BlockPos pos) {
        Direction back = mountedOn(state);
        if (back == null) return true;
        BlockState tray = level.getBlockState(pos.relative(back));
        return tray.getBlock() instanceof RollingTrayBlock && RollingTrayBlock.front(tray) == back.getOpposite();
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState neighbour,
                                                        @Nonnull net.minecraft.world.level.LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighbourPos) {
        if (side == mountedOn(state) && !canSurvive(state, level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, side, neighbour, level, pos, neighbourPos);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new ComponentBlockEntity(pos, state); }

    /** The pin on world side {@code side} of this block, or -1. */
    public static int pinAt(BlockState state, Direction side) {
        // a mounted part's pins are all wired through its tray
        if (!(state.getBlock() instanceof ComponentBlock c) || state.getValue(MOUNTED)) return -1;
        return c.type.pinAt(state.getValue(FACING), state.getValue(SPIN), side);
    }

    /** How many pins are on world side {@code side}: 0, 1, or {@link ComponentType#BUS} for a bus face. */
    public static int widthAt(BlockState state, Direction side) {
        if (!(state.getBlock() instanceof ComponentBlock c) || state.getValue(MOUNTED)) return 0;
        return c.type.widthAt(state.getValue(FACING), state.getValue(SPIN), side);
    }

    /** The pin on world side {@code side} if it's a single pin (a Wick or Hemp Thread can join it), or -1. */
    public static int singlePinAt(BlockState state, Direction side) { return widthAt(state, side) == 1 ? pinAt(state, side) : -1; }

    /** The first pin of the bus face on world side {@code side} (an Eighth Ribbon joins it), or -1. */
    public static int busAt(BlockState state, Direction side) { return widthAt(state, side) == ComponentType.BUS ? pinAt(state, side) : -1; }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                                                  @Nonnull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ComponentBlockEntity be)) return InteractionResult.PASS;
        switch (type.part) {
            case TOGGLE -> {
                be.closed(!be.closed());
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 0.35f, be.closed() ? 0.7f : 0.55f);
                level.setBlock(pos, state.setValue(ON, be.closed()), 3);
            }
            case BUTTON -> {
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON, net.minecraft.sounds.SoundSource.BLOCKS, 0.35f, 0.8f);
                be.closed(true);
                level.setBlock(pos, state.setValue(ON, true), 3);
                level.scheduleTick(pos, this, BUTTON_TICKS);
            }
            case REDSTONE_SWITCH -> player.displayClientMessage(Component.translatable("message.fourtwenty.component.redstone"), true);
            case DISPLAY -> {
                // its hex/decimal toggle is on its back; a mounted one's back is on its tray, so it toggles from the front
                Direction back = Orientation.toWorld(state.getValue(FACING), state.getValue(SPIN), ComponentType.Face.BACK);
                if (!state.getValue(MOUNTED) && hit.getDirection() != back) return InteractionResult.PASS;
                be.step(1);
                BIGrid.changed(level);
                level.sendBlockUpdated(pos, state, state, 2);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, be.value() > 0.5 ? 0.9f : 0.75f);
            }
            default -> {
                if (type.values.isEmpty()) return InteractionResult.PASS;
                be.step(player.isShiftKeyDown() ? -1 : 1);
                // the live circuit is built with each part's value: a new value means a new circuit
                BIGrid.changed(level);
                // a Resinator shows its value in its colour bands
                if (state.hasProperty(ResinatorBlock.VALUE)) level.setBlock(pos, state.setValue(ResinatorBlock.VALUE, be.index()), 3);
            }
        }
        if (!type.values.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.fourtwenty.component.value",
                    Component.translatable("block.fourtwenty." + type.id), type.describe(be.value())), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** A part over its rating smokes from its middle (what it shows says so: Shown.hot). */
    @Override public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof ComponentBlockEntity be)) return;
        // a Slow Burner pushing current breathes out a thin wisp now and then
        if (type == ComponentType.SLOW_BURNER && com.malevolentgods.fourtwenty.domain.electronics.Shown.level(be.shown()) > 0 && random.nextInt(4) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.62, pos.getZ() + 0.5, 0, 0.01, 0);
        }
        if (!com.malevolentgods.fourtwenty.domain.electronics.Shown.hot(be.shown())) return;
        for (int i = 0; i < 2; i++) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.7,
                    pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, 0.03, 0);
        }
    }

    /** A Puff Button lets go. */
    @Override protected void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (type.part == ComponentType.Part.BUTTON && level.getBlockEntity(pos) instanceof ComponentBlockEntity be) {
            be.closed(false);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_OFF, net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 0.7f);
            level.setBlock(pos, state.setValue(ON, false), 3);
        }
    }

    /** A Kill Switch follows redstone: closed while powered. */
    @Override protected void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos from,
                                             boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.isClientSide || type.part != ComponentType.Part.REDSTONE_SWITCH) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (level.getBlockEntity(pos) instanceof ComponentBlockEntity be && be.closed() != powered) {
            be.closed(powered);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 0.25f, powered ? 1.6f : 1.4f);
            level.setBlock(pos, state.setValue(ON, powered), 3);
        }
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) BIGrid.changed(level);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) BIGrid.changed(level);
        super.onRemove(state, level, pos, next, moved);
    }
}
