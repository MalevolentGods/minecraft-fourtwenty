package com.malevolentgods.fourtwenty.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class ModFeatures {
    public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES =
        DeferredRegister.create(Registries.CONFIGURED_FEATURE, "fourtwenty");
    
    public static final DeferredRegister<PlacedFeature> PLACED_FEATURES =
        DeferredRegister.create(Registries.PLACED_FEATURE, "fourtwenty");

    // Features and placements will be defined via JSON data files
    // This allows for easier configuration and follows modern MC modding patterns
}
