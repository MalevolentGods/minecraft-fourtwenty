package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.WeedBongBlock;
import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.power.WickBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A breadboard for power game tests: a + rail and a − rail of Wicks along x, and a row of blocks between them, each turned so its +
 * terminal (its left, seen from its front) touches the + rail and its − the − rail. Every block on the row is in parallel.
 *
 * <pre>
 *   z = plus     + + + + + + +    (Wicks)
 *   z = plus+1   H . M . M . S    (blocks, fronts facing west)
 *   z = plus+2   - - - - - - -    (Wicks)
 * </pre>
 */
final class Ladder {
    final GameTestHelper h;
    final int y, plusZ;

    Ladder(GameTestHelper h, int y, int plusZ) {
        this.h = h;
        this.y = y;
        this.plusZ = plusZ;
    }

    int rowZ() { return plusZ + 1; }

    int minusZ() { return plusZ + 2; }

    BlockPos at(int x) { return new BlockPos(x, y, rowZ()); }

    BlockPos plus(int x) { return new BlockPos(x, y, plusZ); }

    BlockPos minus(int x) { return new BlockPos(x, y, minusZ()); }

    /** Both rails, from {@code x0} to {@code x1}, in one Wick. */
    Ladder rails(Block wick, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            h.setBlock(plus(x), wick);
            h.setBlock(minus(x), wick);
        }
        return this;
    }

    /** {@code block} at {@code x} on the row, its front to the west so + is north and − south. A Hookah faces the way its placer looked. */
    BlockPos put(int x, Block block) {
        h.setBlock(at(x), facingWest(block));
        return at(x);
    }

    static BlockState facingWest(Block block) {
        BlockState state = block.defaultBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return state;
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, block instanceof WeedBongBlock ? Direction.EAST : Direction.WEST);
    }

    /** A Hookah of {@code block} at {@code x}, with buds, water and charcoal, so it lights when the row needs it. */
    BlockPos hookah(int x, Block block) {
        BlockPos pos = put(x, block);
        var hookah = (WeedBongBlockEntity) h.getBlockEntity(pos);
        hookah.setItem(WeedBongBlockEntity.WEED_SLOT, new ItemStack(com.malevolentgods.fourtwenty.registry.ModItems.WEED_BUD.get(), 4));
        hookah.setItem(WeedBongBlockEntity.WATER_SLOT, new ItemStack(Items.WATER_BUCKET));
        hookah.setItem(WeedBongBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL, 16));
        return pos;
    }

    /** A Wick of {@code block} in {@code colour} at {@code pos}. */
    static void wick(GameTestHelper h, BlockPos pos, Block block, WickBlock.Colour colour) {
        h.setBlock(pos, block.defaultBlockState().setValue(WickBlock.COLOUR, colour));
    }
}
