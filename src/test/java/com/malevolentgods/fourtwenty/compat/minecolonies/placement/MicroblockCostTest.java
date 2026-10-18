package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MicroblockCostTest {
    private static final String LAMP = "projectred_illumination:lime_inverted_illumar_lamp";

    @Test void thinCoverCostsOneSourceBlock() {
        // The Dispensary roof trim: one 1/8 face of a lit lime lamp per block.
        var cost = MicroblockCost.blocks(List.of(new MicroblockCost.Part("cb_microblock:face", 16, LAMP + "//lit.true")));
        assertEquals(Map.of(LAMP, 1), cost);
    }
    @Test void volumeAddsUpPerMaterialAndRoundsUp() {
        // Two half-slab faces of stone = exactly one block; a third half slab needs a second block.
        var two = List.of(new MicroblockCost.Part("cb_microblock:face", 4 << 4, "minecraft:stone"),
                new MicroblockCost.Part("cb_microblock:face", (4 << 4) | 1, "minecraft:stone"));
        assertEquals(Map.of("minecraft:stone", 1), MicroblockCost.blocks(two));
        var three = new java.util.ArrayList<>(two);
        three.add(new MicroblockCost.Part("cb_microblock:face", (4 << 4) | 2, "minecraft:stone"));
        assertEquals(Map.of("minecraft:stone", 2), MicroblockCost.blocks(three));
    }
    @Test void edgesAndCornersAreSmall() {
        // Six 2/8 stone edges (the workshop basin rim): 6 * 2*2*8 = 192/512 -> one block.
        var rim = java.util.stream.IntStream.range(0, 6).mapToObj(i -> new MicroblockCost.Part("cb_microblock:edge", 32 + i, "minecraft:stone")).toList();
        assertEquals(Map.of("minecraft:stone", 1), MicroblockCost.blocks(rim));
        assertEquals(1, MicroblockCost.volume(new MicroblockCost.Part("cb_microblock:corner", 16, "minecraft:stone")));
    }
    @Test void mixedMaterialsAreChargedSeparately() {
        var cost = MicroblockCost.blocks(List.of(new MicroblockCost.Part("cb_microblock:face", 16, "minecraft:oak_planks"),
                new MicroblockCost.Part("cb_microblock:face", 17, "minecraft:glass")));
        assertEquals(Map.of("minecraft:oak_planks", 1, "minecraft:glass", 1), cost);
    }
    @Test void unknownPartsCountAsWholeBlocksAndMissingMaterialIsFree() {
        assertEquals(512, MicroblockCost.volume(new MicroblockCost.Part("someaddon:fancy", 16, "minecraft:stone")));
        assertTrue(MicroblockCost.blocks(List.of(new MicroblockCost.Part("cb_microblock:face", 16, ""))).isEmpty());
        assertEquals("minecraft:stone", MicroblockCost.sourceBlock("minecraft:stone"));
    }
}
