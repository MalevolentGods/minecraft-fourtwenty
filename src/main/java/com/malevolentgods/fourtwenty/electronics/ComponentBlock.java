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
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ON = BooleanProperty.create("on");
    private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);
    public static final int BUTTON_TICKS = 20;

    private final ComponentType type;

    public ComponentBlock(ComponentType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ON, false));
    }

    public ComponentType type() { return type; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ON); }

    /** Placed with its front toward the player, like every powered block. */
    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new ComponentBlockEntity(pos, state); }

    /** The pin on world side {@code side} of this block, or -1. */
    public static int pinAt(BlockState state, Direction side) {
        if (!(state.getBlock() instanceof ComponentBlock c)) return -1;
        return c.type.pinAt(state.getValue(FACING), side);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                                                  @Nonnull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ComponentBlockEntity be)) return InteractionResult.PASS;
        switch (type.part) {
            case TOGGLE -> {
                be.closed(!be.closed());
                level.setBlock(pos, state.setValue(ON, be.closed()), 3);
            }
            case BUTTON -> {
                be.closed(true);
                level.setBlock(pos, state.setValue(ON, true), 3);
                level.scheduleTick(pos, this, BUTTON_TICKS);
            }
            case REDSTONE_SWITCH -> player.displayClientMessage(Component.translatable("message.fourtwenty.component.redstone"), true);
            default -> {
                if (type.values.isEmpty()) return InteractionResult.PASS;
                be.step(player.isShiftKeyDown() ? -1 : 1);
            }
        }
        if (!type.values.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.fourtwenty.component.value",
                    Component.translatable("block.fourtwenty." + type.id), ComponentBlockEntity.format(be.value(), type.unit)), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** A Puff Button lets go. */
    @Override protected void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (type.part == ComponentType.Part.BUTTON && level.getBlockEntity(pos) instanceof ComponentBlockEntity be) {
            be.closed(false);
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
