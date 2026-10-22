package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.ArrayList;
import java.util.List;

/**
 * Joining FourTwenty beds side by side: two beds of the same wood facing the same way make a
 * double, a third makes a king (one block longer, if the three blocks past the foot are free;
 * otherwise it stays a single). Every column stays a real bed; the king's extra length is a
 * {@link BedExtensionBlock} row. Groups are formed when a bed is placed; breaking any part breaks the
 * whole bed, like a vanilla bed's two halves. Positions here are the columns' foot blocks.
 */
public final class BedGroups {
    private BedGroups() {}

    private static Direction left(Direction facing) { return facing.getCounterClockWise(); }

    private static Direction right(Direction facing) { return facing.getClockWise(); }

    /** Whether {@code pos} is the foot of a bed column of this block and facing. */
    private static boolean isColumn(BlockGetter level, BlockPos pos, Block block, Direction facing) {
        BlockState s = level.getBlockState(pos);
        return s.is(block) && s.getValue(FancyBedBlock.PART) == BedPart.FOOT && s.getValue(FancyBedBlock.FACING) == facing;
    }

    private static BedShape shape(BlockGetter level, BlockPos foot) { return level.getBlockState(foot).getValue(FancyBedBlock.SHAPE); }

    /**
     * The king a {@code block} bed placed with its foot at {@code foot}, facing {@code facing}, would
     * make with the beds beside it (left to right), whether or not there is room past the foot; null
     * if it wouldn't make one. Between two singles, or at the open end of a double.
     */
    public static List<BlockPos> kingFor(BlockGetter level, BlockPos foot, Block block, Direction facing) {
        BlockPos l = foot.relative(left(facing)), r = foot.relative(right(facing));
        BedShape ls = isColumn(level, l, block, facing) ? shape(level, l) : null;
        BedShape rs = isColumn(level, r, block, facing) ? shape(level, r) : null;
        if (ls == BedShape.SINGLE && rs == BedShape.SINGLE) return List.of(l, foot, r);
        if (ls == BedShape.RIGHT) return List.of(l.relative(left(facing)), l, foot);
        if (rs == BedShape.LEFT) return List.of(foot, r, r.relative(right(facing)));
        return null;
    }

    /** The blanket a new column at {@code foot} takes when it joins {@code king}: the existing bed's. */
    public static DyeColor kingBlanket(BlockGetter level, List<BlockPos> king, BlockPos foot) {
        return level.getBlockState(king.get(0).equals(foot) ? king.get(1) : king.get(0)).getValue(FancyBedBlock.BLANKET);
    }

    /** Called when a bed is placed (its foot at {@code foot}): join it to the beds beside it. */
    public static void join(Level level, BlockPos foot) {
        BlockState state = level.getBlockState(foot);
        Block block = state.getBlock();
        Direction facing = state.getValue(FancyBedBlock.FACING);
        BlockPos l = foot.relative(left(facing)), r = foot.relative(right(facing));
        boolean lBed = isColumn(level, l, block, facing), rBed = isColumn(level, r, block, facing);
        BedShape ls = lBed ? shape(level, l) : null, rs = rBed ? shape(level, r) : null;
        // A king, if there is room past the foot.
        List<BlockPos> king = kingFor(level, foot, block, facing);
        if (king != null && extensionFree(level, king, facing)) {
            DyeColor color = kingBlanket(level, king, foot);
            set(level, king.get(0), facing, BedShape.KING_LEFT, color);
            set(level, king.get(1), facing, BedShape.KING_MIDDLE, color);
            set(level, king.get(2), facing, BedShape.KING_RIGHT, color);
            for (BlockPos column : king) {
                level.setBlock(column.relative(facing.getOpposite()),
                        ModBlocks.BED_EXTENSION.get().defaultBlockState().setValue(BedExtensionBlock.FACING, facing), Block.UPDATE_ALL);
            }
            return;
        }
        // Beside a single: a double.
        if (ls == BedShape.SINGLE) {
            DyeColor color = level.getBlockState(l).getValue(FancyBedBlock.BLANKET);
            set(level, l, facing, BedShape.LEFT, color);
            set(level, foot, facing, BedShape.RIGHT, color);
        } else if (rs == BedShape.SINGLE) {
            DyeColor color = level.getBlockState(r).getValue(FancyBedBlock.BLANKET);
            set(level, foot, facing, BedShape.LEFT, color);
            set(level, r, facing, BedShape.RIGHT, color);
        }
    }

    /** Whether every block just past the columns' feet can take a king's extension. */
    public static boolean extensionFree(BlockGetter level, List<BlockPos> columns, Direction facing) {
        for (BlockPos column : columns) {
            if (!level.getBlockState(column.relative(facing.getOpposite())).canBeReplaced()) return false;
        }
        return true;
    }

    /** Sets a column's shape (and blanket colour) on both halves. */
    private static void set(Level level, BlockPos foot, Direction facing, BedShape shape, DyeColor color) {
        for (BlockPos p : List.of(foot, foot.relative(facing))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FancyBedBlock) {
                level.setBlock(p, s.setValue(FancyBedBlock.SHAPE, shape).setValue(FancyBedBlock.BLANKET, color), Block.UPDATE_ALL);
            }
        }
    }

    /** The foot positions of every column of the bed whose column at {@code foot} has this shape. */
    public static List<BlockPos> columns(BlockPos foot, Direction facing, BedShape shape) {
        BlockPos first = switch (shape) {
            case RIGHT, KING_MIDDLE -> foot.relative(left(facing));
            case KING_RIGHT -> foot.relative(left(facing), 2);
            default -> foot;
        };
        int count = shape == BedShape.SINGLE ? 1 : shape.king() ? 3 : 2;
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(first.relative(right(facing), i));
        return out;
    }

    /** The foot positions of every column in the bed that {@code foot}'s column belongs to. */
    public static List<BlockPos> members(Level level, BlockPos foot) {
        BlockState state = level.getBlockState(foot);
        if (!(state.getBlock() instanceof FancyBedBlock)) return List.of(foot);
        return columns(foot, state.getValue(FancyBedBlock.FACING), state.getValue(FancyBedBlock.SHAPE));
    }

    /** Set while a creative player breaks a bed, so the rest of it goes without drops. */
    private static final ThreadLocal<Boolean> QUIET = ThreadLocal.withInitial(() -> false);

    /**
     * Breaks a whole joined bed, like a vanilla bed's two halves: every column (each head drops its
     * own bed, blanket colour kept, unless {@link #breakQuietly}) and a king's extension row. Safe to
     * re-enter: each removal below calls back in, finding less left to break each time.
     */
    public static void breakAll(Level level, List<BlockPos> columns, Direction facing) {
        boolean drops = !QUIET.get();
        for (BlockPos column : columns) {
            BlockPos head = column.relative(facing);
            if (level.getBlockState(head).getBlock() instanceof FancyBedBlock) level.destroyBlock(head, drops);
            // The foot drops nothing (the head carries the bed).
            if (level.getBlockState(column).getBlock() instanceof FancyBedBlock) {
                level.setBlock(column, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
            BlockPos ext = column.relative(facing.getOpposite());
            if (level.getBlockState(ext).is(ModBlocks.BED_EXTENSION.get())) level.setBlock(ext, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** {@link #breakAll} with no drops (a creative player breaking the bed). */
    public static void breakQuietly(Level level, List<BlockPos> columns, Direction facing) {
        QUIET.set(true);
        try {
            breakAll(level, columns, facing);
        } finally {
            QUIET.set(false);
        }
    }
}
