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
    private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);
    public static final int BUTTON_TICKS = 20;

    private final ComponentType type;

    public ComponentBlock(ComponentType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPIN, 0).setValue(ON, false));
    }

    public ComponentType type() { return type; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, SPIN, ON); }

    /**
     * Placed with its front toward the player, whichever of the six ways they look; one placed facing up or down has its top away from
     * them, so it reads the right way round. The Twister turns it after.
     */
    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
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
        return SHAPE;
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new ComponentBlockEntity(pos, state); }

    /** The pin on world side {@code side} of this block, or -1. */
    public static int pinAt(BlockState state, Direction side) {
        if (!(state.getBlock() instanceof ComponentBlock c)) return -1;
        return c.type.pinAt(state.getValue(FACING), state.getValue(SPIN), side);
    }

    /** How many pins are on world side {@code side}: 0, 1, or {@link ComponentType#BUS} for a bus face. */
    public static int widthAt(BlockState state, Direction side) {
        if (!(state.getBlock() instanceof ComponentBlock c)) return 0;
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
        if (!(level.getBlockEntity(pos) instanceof ComponentBlockEntity be) || !com.malevolentgods.fourtwenty.domain.electronics.Shown.hot(be.shown())) return;
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
