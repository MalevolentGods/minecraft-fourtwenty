package com.malevolentgods.fourtwenty.domain.electronics;

/**
 * A component of a {@link Netlist}. Each Newton iteration it stamps its companion model, the tangent to its I–V curve at the present
 * voltages (a conductance and a current), into the system; a linear part stamps the same thing every time.
 */
public interface Part {
    /** The two nodes it joins; for a part with more, the pair that ties it into a loop (for finding references). */
    int[] nodes();

    /** Every node it ties into one loop (a multi-pin part ties all its pins): by default its two {@link #nodes()}. */
    default int[] joined() { return nodes(); }

    /** Whether it joins its two nodes into one loop in this analysis: an open switch doesn't, nor a capacitor in DC. */
    default boolean conducts(Analysis analysis) { return true; }

    /** Stamps its companion model at the present guess. */
    void stamp(Stamp stamp, Analysis analysis);

    /** Whether its stamp depends on the voltages (so the solve must iterate). */
    default boolean nonlinear() { return false; }

    /**
     * After a Newton step: whether its own current is consistent with the new voltages, to SPICE's tolerances; a linear part always is.
     * {@code analysis} carries the solved voltages.
     */
    default boolean converged(Analysis analysis) { return true; }

    /** A time step was accepted at these voltages: remember what the next step needs (a capacitor's voltage, an inductor's current). */
    default void accept(Analysis analysis) {}

    /** The current through it, from its first node to its second, at these voltages. */
    double current(Analysis analysis);
}
