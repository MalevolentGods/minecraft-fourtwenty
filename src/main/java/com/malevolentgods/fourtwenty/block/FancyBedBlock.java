package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

/**
 * A wooden bed with a headboard between two posts, a footboard, side rails, a mattress, a pillow
 * and a blanket, in FourTwenty's own model (tools/generate_beds.py). It is a real bed: it extends
 * {@link BedBlock} (two halves, sleeping, respawn) and is in {@code #minecraft:beds}, so colonies
 * count it. The blanket colour is a block state (lime when placed); right-click either half with
 * a dye to recolour both. Rendered as a plain block model, not the vanilla bed renderer. Beds of
 * the same wood placed side by side join into a double or a king ({@link BedGroups}); SHAPE says
 * which column of it this is.
 */
public class FancyBedBlock extends BedBlock {
    public static final EnumProperty<DyeColor> BLANKET = EnumProperty.create("blanket", DyeColor.class);
    public static final EnumProperty<BedShape> SHAPE = EnumProperty.create("shape", BedShape.class);
    private static final Map<Direction, VoxelShape> HEAD = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> FOOT = new EnumMap<>(Direction.class);
    /** A king's foot half: no footboard (that's on its extension). */
    private static final Map<Direction, VoxelShape> KING_FOOT = new EnumMap<>(Direction.class);

    static {
        // Built for a bed facing north (head end at z 0 of the head block, foot end at z 16 of the foot).
        VoxelShape bed = Block.box(0, 0, 0, 16, 9.5, 16);
        for (var facing : Direction.Plane.HORIZONTAL) {
            HEAD.put(facing, Shapes.or(bed, rotate(0, 0, 0, 16, 15, 2, facing)));
            FOOT.put(facing, Shapes.or(bed, rotate(0, 0, 14, 16, 9.5, 16, facing)));
            KING_FOOT.put(facing, bed);
        }
    }

    /** A box given for a north-facing bed, turned to {@code facing}. */
    static VoxelShape rotate(double x0, double y0, double z0, double x1, double y1, double z1, Direction facing) {
        return switch (facing) {
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    public FancyBedBlock(Properties properties) {
        super(DyeColor.WHITE, properties);
        registerDefaultState(defaultBlockState().setValue(BLANKET, DyeColor.LIME).setValue(SHAPE, BedShape.SINGLE));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BLANKET, SHAPE);
    }

    /** A plain block model, not the vanilla bed renderer. */
    @Override protected RenderShape getRenderShape(@Nonnull BlockState state) { return RenderShape.MODEL; }

    /** No vanilla bed block entity: it only carries the vanilla renderer's colour. */
    @Override public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return null; }

    @Override protected VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
                                            @Nonnull CollisionContext context) {
        // FACING points from the foot to the head.
        if (state.getValue(PART) == BedPart.HEAD) return HEAD.get(state.getValue(FACING));
        return (state.getValue(SHAPE).king() ? KING_FOOT : FOOT).get(state.getValue(FACING));
    }

    /** Join the beds beside it once both halves are down. */
    @Override public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer,
                                      @Nonnull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockState(pos).is(this)) BedGroups.join(level, pos);
    }

    /** Removing either half of a joined column splits its bed back into singles. */
    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState,
                                      boolean moved) {
        if (!level.isClientSide && !newState.is(this) && state.getValue(SHAPE) != BedShape.SINGLE) {
            Direction facing = state.getValue(FACING);
            BlockPos foot = state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(facing.getOpposite());
            BedGroups.split(level, foot, facing, state.getValue(SHAPE), foot);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Dye any part of a bed to recolour the whole blanket (every column of a joined bed). */
    @Override protected ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level,
                                                        @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand,
                                                        @Nonnull BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (state.getValue(BLANKET) == dye.getDyeColor()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos foot = state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(facing.getOpposite());
            for (BlockPos column : BedGroups.members(level, foot)) {
                for (BlockPos p : java.util.List.of(column, column.relative(facing))) {
                    BlockState s = level.getBlockState(p);
                    if (s.is(this)) level.setBlock(p, s.setValue(BLANKET, dye.getDyeColor()), Block.UPDATE_ALL);
                }
            }
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
