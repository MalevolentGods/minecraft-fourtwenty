package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a colony builder pays for one CB Multipart block: the blocks its microblocks are cut from.
 *
 * <p>A part's {@code shape} keeps its size in eighths of a block in the high nibble. Volume per
 * part, in 1/512ths of a block: faces and hollow covers size*64, edges size^2*8, corners size^3;
 * any other part counts as a whole block. Each material costs its total volume rounded up to whole
 * blocks (at least one), so even a thin cover costs one source block per block position: builders
 * price each position on its own. Pure registry-id rules so they can be unit tested without CB.
 */
public final class MicroblockCost {
    /** One saved part: CB part id (e.g. "cb_microblock:face"), shape byte, material ("ns:block//state"). */
    public record Part(String id, int shape, String material) {}

    private static final int BLOCK = 512;

    /** Source block id -> whole blocks to charge, in first-seen order. Parts without a material are free. */
    public static Map<String, Integer> blocks(List<Part> parts) {
        var volume = new LinkedHashMap<String, Integer>();
        for (var part : parts) {
            String block = sourceBlock(part.material());
            if (block.isEmpty()) continue;
            volume.merge(block, volume(part), Integer::sum);
        }
        var cost = new LinkedHashMap<String, Integer>();
        volume.forEach((block, v) -> cost.put(block, Math.max(1, (v + BLOCK - 1) / BLOCK)));
        return cost;
    }

    /** "projectred_illumination:lime_inverted_illumar_lamp//lit.true" -> "projectred_illumination:lime_inverted_illumar_lamp". */
    static String sourceBlock(String material) {
        if (material == null) return "";
        int state = material.indexOf("//");
        return (state < 0 ? material : material.substring(0, state)).trim();
    }

    static int volume(Part part) {
        int size = (part.shape() >> 4) & 0xF;
        if (size <= 0 || size >= 8) return BLOCK;
        return switch (part.id()) {
            case "cb_microblock:face", "cb_microblock:hollow" -> size * 64;
            case "cb_microblock:edge" -> size * size * 8;
            case "cb_microblock:corner" -> size * size * size;
            default -> BLOCK;
        };
    }

    private MicroblockCost() {}
}
