package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A block with a road's top: a road ({@link HempPathBlock}) or a road slab ({@link HempPathSlabBlock}).
 * Two of them join (no border between them) when they're the same tier and their tops are at the same
 * height: a road, a double slab and a top slab all reach 15/16; a bottom slab 7/16. A road also joins
 * the stairs of a ramp leading up to it.
 */
public interface RoadSurface {
    BooleanProperty NORTH = BooleanProperty.create("north"), EAST = BooleanProperty.create("east"),
            SOUTH = BooleanProperty.create("south"), WEST = BooleanProperty.create("west"),
            NORTH_EAST = BooleanProperty.create("northeast"), SOUTH_EAST = BooleanProperty.create("southeast"),
            SOUTH_WEST = BooleanProperty.create("southwest"), NORTH_WEST = BooleanProperty.create("northwest");
    /** The eight neighbours, as (dx, dz), with their properties. */
    int[][] OFFSETS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}};
    BooleanProperty[] PROPERTIES = {NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST};

    int tier();

    /** The height of this state's top in pixels (15 or 7). */
    int surface(BlockState state);

    static boolean joins(BlockState a, BlockState b) {
        return a.getBlock() instanceof RoadSurface ra && b.getBlock() instanceof RoadSurface rb
                && ra.tier() == rb.tier() && ra.surface(a) == rb.surface(b);
    }

    /**
     * Whether the block at offset (dx, dz) from a road surface carries its road on: another surface
     * of the same height, or a ramp leading up to it, a stair of the same road whose high back side
     * faces this block (so a road at the top of a ramp has no border across it).
     */
    static boolean joins(BlockState self, BlockState other, int dx, int dz) {
        if (joins(self, other)) return true;
        if (!(self.getBlock() instanceof RoadSurface rs) || rs.surface(self) != 15) return false;
        if (!(other.getBlock() instanceof HempPathStairBlock stair) || stair.tier() != rs.tier()
                || other.getValue(net.minecraft.world.level.block.StairBlock.HALF) != net.minecraft.world.level.block.state.properties.Half.BOTTOM) {
            return false;
        }
        net.minecraft.core.Direction back = other.getValue(net.minecraft.world.level.block.StairBlock.FACING);
        return -dx * back.getStepX() - dz * back.getStepZ() > 0;      // this block lies on the stair's back side
    }

    /** A road surface's state with its joins to the road round it. */
    static BlockState connected(LevelReader level, BlockPos pos, BlockState state) {
        for (int i = 0; i < OFFSETS.length; i++) {
            state = state.setValue(PROPERTIES[i], joins(state, level.getBlockState(pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1])), OFFSETS[i][0], OFFSETS[i][1]));
        }
        return state;
    }

    static BlockState unconnected(BlockState state) {
        for (BooleanProperty p : PROPERTIES) state = state.setValue(p, false);
        return state;
    }

    /** Minecraft only tells a block about its side neighbours, so a road changed refreshes its diagonal ones. */
    static void refreshDiagonals(Level level, BlockPos pos) {
        for (int i = 4; i < OFFSETS.length; i++) {
            BlockPos p = pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1]);
            BlockState there = level.getBlockState(p);
            if (there.getBlock() instanceof RoadSurface) {
                BlockState updated = connected(level, p, there);
                if (updated != there) level.setBlock(p, updated, Block.UPDATE_CLIENTS);
            }
        }
    }
}
