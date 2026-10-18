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
    @Test void blueprintAllocationChargesOneSourceBlockPerEightCovers() {
        // The Dispensary roof trim: 48 positions, one 1/8 lamp cover each -> 6 lamps in total.
        var cover = MicroblockCost.volumes(List.of(new MicroblockCost.Part("cb_microblock:face", 16, LAMP + "//lit.true")));
        var charges = MicroblockCost.allocate(java.util.Collections.nCopies(48, cover));
        assertEquals(6, charges.stream().mapToInt(c -> c.getOrDefault(LAMP, 0)).sum());
        assertEquals(Map.of(LAMP, 1), charges.get(0));
        for (int i = 1; i < 8; i++) assertTrue(charges.get(i).isEmpty(), "cover " + i + " rides on the first lamp");
        assertEquals(Map.of(LAMP, 1), charges.get(8));
    }
    @Test void allocationTracksEachMaterialSeparatelyAndRoundsUp() {
        var stoneHalf = Map.of("minecraft:stone", 256);
        var glassCover = Map.of("minecraft:glass", 64);
        var charges = MicroblockCost.allocate(List.of(stoneHalf, glassCover, stoneHalf, stoneHalf));
        assertEquals(Map.of("minecraft:stone", 1), charges.get(0));
        assertEquals(Map.of("minecraft:glass", 1), charges.get(1));
        assertTrue(charges.get(2).isEmpty());                       // two halves = the first stone block
        assertEquals(Map.of("minecraft:stone", 1), charges.get(3)); // a third half needs a second block
    }
    @Test void unknownPartsCountAsWholeBlocksAndMissingMaterialIsFree() {
        assertEquals(512, MicroblockCost.volume(new MicroblockCost.Part("someaddon:fancy", 16, "minecraft:stone")));
        assertTrue(MicroblockCost.blocks(List.of(new MicroblockCost.Part("cb_microblock:face", 16, ""))).isEmpty());
        assertEquals("minecraft:stone", MicroblockCost.sourceBlock("minecraft:stone"));
    }
}
