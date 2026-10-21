package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
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
 * {@link BedExtensionBlock} row. Groups are formed when a bed is placed and split back into singles
 * when any part is removed. Positions here are the columns' foot blocks.
 */
public final class BedGroups {
    private BedGroups() {}

    private static Direction left(Direction facing) { return facing.getCounterClockWise(); }

    private static Direction right(Direction facing) { return facing.getClockWise(); }

    /** Whether {@code pos} is the foot of a bed column of this block and facing. */
    private static boolean isColumn(Level level, BlockPos pos, Block block, Direction facing) {
        BlockState s = level.getBlockState(pos);
        return s.is(block) && s.getValue(FancyBedBlock.PART) == BedPart.FOOT && s.getValue(FancyBedBlock.FACING) == facing;
    }

    private static BedShape shape(Level level, BlockPos foot) { return level.getBlockState(foot).getValue(FancyBedBlock.SHAPE); }

    /** Called when a bed is placed (its foot at {@code foot}): join it to the beds beside it. */
    public static void join(Level level, BlockPos foot) {
        BlockState state = level.getBlockState(foot);
        Block block = state.getBlock();
        Direction facing = state.getValue(FancyBedBlock.FACING);
        BlockPos l = foot.relative(left(facing)), r = foot.relative(right(facing));
        boolean lBed = isColumn(level, l, block, facing), rBed = isColumn(level, r, block, facing);
        BedShape ls = lBed ? shape(level, l) : null, rs = rBed ? shape(level, r) : null;
        // Between two singles, or at the open end of a double: a king, if there is room past the foot.
        List<BlockPos> king = null;
        if (ls == BedShape.SINGLE && rs == BedShape.SINGLE) king = List.of(l, foot, r);
        else if (ls == BedShape.RIGHT) king = List.of(l.relative(left(facing)), l, foot);
        else if (rs == BedShape.LEFT) king = List.of(foot, r, r.relative(right(facing)));
        if (king != null && extensionFree(level, king, facing)) {
            DyeColor color = level.getBlockState(king.get(0).equals(foot) ? king.get(1) : king.get(0)).getValue(FancyBedBlock.BLANKET);
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

    private static boolean extensionFree(Level level, List<BlockPos> columns, Direction facing) {
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

    /** The foot positions of every column in the group that {@code foot}'s column belongs to. */
    public static List<BlockPos> members(Level level, BlockPos foot) {
        BlockState state = level.getBlockState(foot);
        if (!(state.getBlock() instanceof FancyBedBlock)) return List.of(foot);
        Direction facing = state.getValue(FancyBedBlock.FACING);
        BedShape shape = state.getValue(FancyBedBlock.SHAPE);
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

    /** Splits the group {@code foot}'s column was in back into single beds (skipping {@code gone}). */
    public static void split(Level level, BlockPos foot, Direction facing, BedShape shape, BlockPos gone) {
        BlockPos first = switch (shape) {
            case RIGHT, KING_MIDDLE -> foot.relative(left(facing));
            case KING_RIGHT -> foot.relative(left(facing), 2);
            default -> foot;
        };
        int count = shape.king() ? 3 : 2;
        List<BlockPos> columns = new ArrayList<>();
        for (int i = 0; i < count; i++) columns.add(first.relative(right(facing), i));
        // Singles first, so removing the extensions below finds no king left to split again.
        for (BlockPos column : columns) {
            if (column.equals(gone)) continue;
            for (BlockPos p : List.of(column, column.relative(facing))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof FancyBedBlock && s.getValue(FancyBedBlock.SHAPE) != BedShape.SINGLE) {
                    level.setBlock(p, s.setValue(FancyBedBlock.SHAPE, BedShape.SINGLE), Block.UPDATE_ALL);
                }
            }
        }
        if (!shape.king()) return;
        for (BlockPos column : columns) {
            BlockPos ext = column.relative(facing.getOpposite());
            if (level.getBlockState(ext).is(ModBlocks.BED_EXTENSION.get())) level.setBlock(ext, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
