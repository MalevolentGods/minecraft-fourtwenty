package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

/** Anything on a Wick network, by the side it's reached from. See specs/001-weed-processing/contracts/bi-grid.md. */
public interface BINode {
    BlockCapability<BINode, Direction> CAPABILITY =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("fourtwenty", "bi"), BINode.class);

    BIRules.Tier tier();

    long stored();

    long capacity();

    /** Takes up to {@code amount}; returns what it took. */
    long receive(long amount);

    /** Gives up to {@code amount}; returns what it gave. */
    long extract(long amount);

    boolean canReceive();

    boolean canExtract();

    default double fill() {
        return capacity() <= 0 ? 0 : (double) stored() / capacity();
    }
}
