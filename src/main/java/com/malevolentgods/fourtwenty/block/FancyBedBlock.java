package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
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
 * a dye to recolour both. Rendered as a plain block model, not the vanilla bed renderer.
 */
public class FancyBedBlock extends BedBlock {
    public static final EnumProperty<DyeColor> BLANKET = EnumProperty.create("blanket", DyeColor.class);
    private static final Map<Direction, VoxelShape> HEAD = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> FOOT = new EnumMap<>(Direction.class);

    static {
        // Built for a bed facing north (head end at z 0 of the head block, foot end at z 16 of the foot).
        VoxelShape bed = Block.box(0, 0, 0, 16, 9.5, 16);
        for (var facing : Direction.Plane.HORIZONTAL) {
            HEAD.put(facing, Shapes.or(bed, rotate(0, 0, 0, 16, 15, 2, facing)));
            FOOT.put(facing, Shapes.or(bed, rotate(0, 0, 14, 16, 9.5, 16, facing)));
        }
    }

    /** A box given for a north-facing bed, turned to {@code facing}. */
    private static VoxelShape rotate(double x0, double y0, double z0, double x1, double y1, double z1, Direction facing) {
        return switch (facing) {
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    public FancyBedBlock(Properties properties) {
        super(DyeColor.WHITE, properties);
        registerDefaultState(defaultBlockState().setValue(BLANKET, DyeColor.LIME));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BLANKET);
    }

    /** A plain block model, not the vanilla bed renderer. */
    @Override protected RenderShape getRenderShape(@Nonnull BlockState state) { return RenderShape.MODEL; }

    /** No vanilla bed block entity: it only carries the vanilla renderer's colour. */
    @Override public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return null; }

    @Override protected VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
                                            @Nonnull CollisionContext context) {
        // FACING points from the foot to the head.
        return (state.getValue(PART) == BedPart.HEAD ? HEAD : FOOT).get(state.getValue(FACING));
    }

    /** Dye either half to recolour the blanket on both. */
    @Override protected ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level,
                                                        @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand,
                                                        @Nonnull BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (state.getValue(BLANKET) == dye.getDyeColor()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(BLANKET, dye.getDyeColor()), Block.UPDATE_ALL);
            var other = pos.relative(getConnectedDirection(state));
            var otherState = level.getBlockState(other);
            if (otherState.is(this)) level.setBlock(other, otherState.setValue(BLANKET, dye.getDyeColor()), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
