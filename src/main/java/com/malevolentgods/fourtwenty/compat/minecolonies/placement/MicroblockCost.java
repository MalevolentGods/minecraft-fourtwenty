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
 * blocks. Across a whole blueprint, {@link #allocate} spreads that cost so each material is paid
 * exactly once per whole block of volume (8 thin covers = 1 block, the CB saw ratio); {@link #blocks}
 * is the per-position fallback (at least one block per position). Pure registry-id rules so they
 * can be unit tested without CB.
 */
public final class MicroblockCost {
    /** One saved part: CB part id (e.g. "cb_microblock:face"), shape byte, material ("ns:block//state"). */
    public record Part(String id, int shape, String material) {}

    private static final int BLOCK = 512;

    /** Source block id -> volume in 1/512ths of a block, in first-seen order. Parts without a material are free. */
    public static Map<String, Integer> volumes(List<Part> parts) {
        var volume = new LinkedHashMap<String, Integer>();
        for (var part : parts) {
            String block = sourceBlock(part.material());
            if (block.isEmpty()) continue;
            volume.merge(block, volume(part), Integer::sum);
        }
        return volume;
    }

    /** Per-position fallback: source block id -> whole blocks, at least one per material. */
    public static Map<String, Integer> blocks(List<Part> parts) {
        var cost = new LinkedHashMap<String, Integer>();
        volumes(parts).forEach((block, v) -> cost.put(block, Math.max(1, wholeBlocks(v))));
        return cost;
    }

    /**
     * Spread the cost over every multipart position of a blueprint, in a fixed order: a position
     * pays a source block whenever the running volume of that material crosses into a new whole
     * block. Totals per material equal its whole volume rounded up; e.g. 48 thin covers pay 6 blocks,
     * at the 1st, 9th, 17th... cover.
     */
    public static List<Map<String, Integer>> allocate(List<Map<String, Integer>> volumesInOrder) {
        var running = new java.util.HashMap<String, Integer>();
        var charges = new java.util.ArrayList<Map<String, Integer>>();
        for (var volumes : volumesInOrder) {
            var charge = new LinkedHashMap<String, Integer>();
            volumes.forEach((block, v) -> {
                int before = running.getOrDefault(block, 0);
                int after = before + v;
                running.put(block, after);
                int pay = wholeBlocks(after) - wholeBlocks(before);
                if (pay > 0) charge.put(block, pay);
            });
            charges.add(charge);
        }
        return charges;
    }

    private static int wholeBlocks(int volume) { return (volume + BLOCK - 1) / BLOCK; }

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
