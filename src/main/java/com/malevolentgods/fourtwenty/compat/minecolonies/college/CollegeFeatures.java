package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Later features some studies wait for (contracts/colony-research-api.md): a study whose feature isn't in the game shows as locked,
 * "Waiting for …". A feature is present when its registry entry exists.
 */
public final class CollegeFeatures {
    private CollegeFeatures() {}

    public static boolean present(String feature) {
        return switch (feature) {
            case "tie_dye" -> BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath("fourtwenty", "tiedyeshack"));
            case "weedscout_uniform" -> BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("fourtwenty", "weedscout_shirt"));
            default -> false;
        };
    }
}
