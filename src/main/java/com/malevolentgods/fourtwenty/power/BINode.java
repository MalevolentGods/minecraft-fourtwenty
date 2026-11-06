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

    /** Fed from a tier above its own: it stops and smokes until that stops. It never breaks. */
    default void overload(long now) {}

    /** What a node is in the circuit (specs/006-circuit-solver/data-model.md). */
    enum Role { SOURCE, BATTERY, LOAD, LED, NONE }

    /** By default from which way power goes: both ways is a battery, out only a source, in only a load. */
    default Role role() {
        if (canReceive() && canExtract()) return Role.BATTERY;
        if (canExtract()) return Role.SOURCE;
        if (canReceive()) return Role.LOAD;
        return Role.NONE;
    }

    /** Willards a load asks for now; 0 when it asks for nothing. By default a machine's draw of its tier while it has room. */
    default double demandWatts() {
        return stored() < capacity() ? com.malevolentgods.fourtwenty.domain.CircuitRules.ratedWatts(tier().machine()) : 0;
    }

    /** Willards a source can give at most, at its tier; its current limit follows from it. By default its tier's Hookah. */
    default double sourceWatts() {
        return com.malevolentgods.fourtwenty.domain.CircuitRules.hookahWatts(tier());
    }

    /** Whether a source has anything to give. */
    default boolean sourceReady() { return stored() > 0; }

    /** Whether it's overloaded now, so it takes nothing. */
    default boolean overloaded(long now) { return false; }

    /** An LED (a Lit Bit): the current its driver is set to, in Ignus. */
    default double ledAmps() { return 0; }

    /** An LED hears how lit it is after each solve: 0 to 1 up its knee, and its circuit status (Circuit.OK, NO_LOOP, REVERSED). */
    default void lit(double brightness, int status) {}

    /** The share of what reaches it that it keeps; a Hit Box loses a little. */
    default double efficiency() { return 1; }

    default double fill() {
        return capacity() <= 0 ? 0 : (double) stored() / capacity();
    }
}
