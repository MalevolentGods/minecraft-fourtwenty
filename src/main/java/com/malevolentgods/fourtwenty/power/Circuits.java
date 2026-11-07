package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Every Wick network on a level, kept between ticks. A network is found from any block or Wick on it and walked again when anything on
 * the level's networks is placed or broken (BIGrid's version), or every few seconds for chunks loading. Sources tick their network, once
 * per game tick however many sources are on it.
 */
public final class Circuits {
    private static final int REWALK = 100;
    private static final Map<Level, Circuits> LEVELS = new WeakHashMap<>();

    private final Map<Object, Network> networks = new HashMap<>();
    private final Map<BlockPos, Integer> burnTimers = new HashMap<>();

    private Circuits() {}

    private static Circuits of(Level level) { return LEVELS.computeIfAbsent(level, l -> new Circuits()); }

    private static boolean fresh(Level level, Network network) {
        return network.version == BIGrid.version(level) && level.getGameTime() - network.walkedAt < REWALK;
    }

    /** The network {@code node} at {@code pos} is on, walked if it has to be. */
    static Network network(Level level, BINode node, BlockPos pos) {
        Circuits circuits = of(level);
        Network network = circuits.networks.get(Network.key(node));
        if (network != null && fresh(level, network) && network.contains(node)) return network;
        network = Network.walk(level, node, pos.immutable());
        for (Object key : network.keys()) circuits.networks.put(key, network);
        if (circuits.networks.size() > 50_000) circuits.networks.values().removeIf(n -> !fresh(level, n));
        return network;
    }

    /** A source's tick: its network solves if it must, and moves this tick's Ws. */
    public static void tick(Level level, BINode node, BlockPos pos) {
        if (level.isClientSide) return;
        network(level, node, pos).tick(level, of(level).burnTimers);
    }

    /** Whether anything on {@code node}'s network but itself wants power: a Hookah lights for that. */
    public static boolean wanted(Level level, BINode node, BlockPos pos) {
        return demandBeyond(level, node, pos) > 0;
    }

    /** The Willards everything on {@code node}'s network but itself asks for, and some for each battery with room. */
    public static double demandBeyond(Level level, BINode node, BlockPos pos) {
        if (level.isClientSide) return 0;
        return network(level, node, pos).demandBeyond(node);
    }

    /** What the Inspoweedometer reads at a block. */
    /** {@code status} is Circuit.OK, BROWNOUT, NO_LOOP or REVERSED for a block; OK for a Wick. Volts on a block are across its terminals. */
    public record Reading(double volts, double amps, double ohms, double watts, boolean wick, double ampRating, double voltRating, double lossWatts, int status) {}

    /** A reading at a Wick or at the BI block there; empty when it's not on a network. */
    /**
     * A block's terminal on the same line as the Wick at {@code pos}: one touching it, or touching any Wick of its colour joined to it,
     * found by walking the line. Empty when the line reaches no terminal.
     */
    private static Optional<Object[]> terminalOnLine(Level level, BlockPos pos) {
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(java.util.List.of(pos.immutable()));
        java.util.Set<BlockPos> seen = new java.util.HashSet<>(queue);
        while (!queue.isEmpty() && seen.size() <= Network.MAX_WICKS) {
            BlockPos at = queue.poll();
            var state = level.getBlockState(at);
            boolean cable = state.getBlock() instanceof CableBlock;
            for (Direction d : Direction.values()) {
                BlockPos next = at.relative(d);
                if (!level.isLoaded(next)) continue;
                var nextState = level.getBlockState(next);
                if (nextState.getBlock() instanceof WickBlock || nextState.getBlock() instanceof CableBlock) {
                    if (wired(state, nextState) && seen.add(next.immutable())) queue.add(next.immutable());
                    continue;
                }
                if (cable) {
                    if (!CableBlock.plugsInto(nextState, d.getOpposite())) continue;
                    BINode owner = Network.ownerOnFace(level, next, d.getOpposite());
                    if (owner != null) return Optional.of(new Object[]{owner, next.immutable()});
                    continue;
                }
                BINode node = level.getCapability(BINode.CAPABILITY, next, d.getOpposite());
                if (node != null) return Optional.of(new Object[]{node, next.immutable()});
            }
        }
        return Optional.empty();
    }

    /** Whether two touching pieces of wiring (Wicks or cables) are joined. */
    private static boolean wired(net.minecraft.world.level.block.state.BlockState a, net.minecraft.world.level.block.state.BlockState b) {
        if (a.getBlock() instanceof WickBlock && b.getBlock() instanceof WickBlock) return WickBlock.joins(a, b);
        if (a.getBlock() instanceof CableBlock && b.getBlock() instanceof CableBlock) return CableBlock.joins(a, b);
        return a.getBlock() instanceof CableBlock ? CableBlock.takesWick(b) : CableBlock.takesWick(a);
    }

    /** A component's reading: each pin's voltage, and the current through it (a logic part's supply current). */
    public record ComponentReading(double[] pins, double amps) {}

    /** What the Inspoweedometer reads at a component: found from the Wick or Hemp Thread on any of its pin faces. */
    public static Optional<ComponentReading> componentReading(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        for (Direction d : Direction.values()) {
            if (com.malevolentgods.fourtwenty.electronics.ComponentBlock.pinAt(state, d) < 0) continue;
            BlockPos wire = pos.relative(d);
            if (!(level.getBlockState(wire).getBlock() instanceof WickBlock)) continue;
            for (Object[] found : terminalOnLine(level, wire).stream().toList()) {
                Network net = network(level, (BINode) found[0], (BlockPos) found[1]);
                double[] pins = net.pinVolts(pos);
                if (pins == null) {
                    // just rebuilt and not run yet: run its tick now, as the other readings do
                    net.tick(level, of(level).burnTimers);
                    pins = net.pinVolts(pos);
                }
                if (pins != null) return Optional.of(new ComponentReading(pins, net.componentAmps(pos)));
            }
        }
        return Optional.empty();
    }

    /** Why the Daisy line through the cable at {@code pos} carries nothing, if it can't be ordered. */
    public static Optional<com.malevolentgods.fourtwenty.domain.DaisyLine.Reason> daisyFailure(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof CableBlock)) return Optional.empty();
        for (Object[] found : terminalOnLine(level, pos).stream().toList()) {
            Network net = network(level, (BINode) found[0], (BlockPos) found[1]);
            return Optional.ofNullable(net.daisyFailed.get(pos));
        }
        return Optional.empty();
    }

    /** A cable's cores read together: the voltage between them, the current in each, and its tier's ratings. */
    public record CableReading(double volts, double plusAmps, double minusAmps, double ohms, double ampRating, double voltRating, double lossWatts) {}

    public static Optional<CableReading> cableReading(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof CableBlock cable)) return Optional.empty();
        var rules = com.malevolentgods.fourtwenty.processing.ProcessingConfig.circuit();
        for (Object[] found : terminalOnLine(level, pos).stream().toList()) {
            Network net = network(level, (BINode) found[0], (BlockPos) found[1]);
            if (!net.solved()) net.tick(level, of(level).burnTimers);
            Network.Wick plus = null, minus = null;
            for (Network.Wick w : net.wicks) {
                if (!w.pos().equals(pos)) continue;
                if (w.core() == Network.Core.PLUS) plus = w;
                else if (w.core() == Network.Core.MINUS) minus = w;
            }
            if (plus == null || minus == null) break;
            return Optional.of(new CableReading(net.potential(plus) - net.potential(minus), net.amps(plus), net.amps(minus), rules.wickOhms(cable.tier()),
                    rules.wickAmps(cable.tier()), rules.wickVolts(cable.tier()), net.wickLoss(plus) + net.wickLoss(minus)));
        }
        return Optional.of(new CableReading(0, 0, 0, rules.wickOhms(cable.tier()), rules.wickAmps(cable.tier()), rules.wickVolts(cable.tier()), 0));
    }

    /** A Wick's potential in its circuit's own terms, with the circuit it's on: two Wicks of one circuit compare by their difference. */
    public record Probe(Object circuit, double potential) {}

    public static Optional<Probe> potential(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof WickBlock)) return Optional.empty();
        for (Object[] found : terminalOnLine(level, pos).stream().toList()) {
            Network net = network(level, (BINode) found[0], (BlockPos) found[1]);
            if (!net.solved()) net.tick(level, of(level).burnTimers);
            for (Network.Wick w : net.wicks) if (w.pos().equals(pos)) return Optional.of(new Probe(net, net.potential(w)));
        }
        return Optional.empty();
    }

    public static Optional<Reading> reading(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof WickBlock wick) {
            for (Object[] found : terminalOnLine(level, pos).stream().toList()) {
                BINode node = (BINode) found[0];
                Network net = network(level, node, (BlockPos) found[1]);
                if (!net.solved()) net.tick(level, of(level).burnTimers);
                for (Network.Wick w : net.wicks) {
                    if (!w.pos().equals(pos)) continue;
                    var rules = com.malevolentgods.fourtwenty.processing.ProcessingConfig.circuit();
                    double v = net.volts(w), a = net.amps(w);
                    return Optional.of(new Reading(v, a, rules.wickOhms(w.tier()) * w.ohmsScale(), v * a, true, rules.wickAmps(w.tier()) * w.ampsScale(), rules.wickVolts(w.tier()), net.wickLoss(w), com.malevolentgods.fourtwenty.domain.Circuit.OK));
                }
            }
            BIRules.Tier tier = wick.tier();
            var rules = com.malevolentgods.fourtwenty.processing.ProcessingConfig.circuit();
            return Optional.of(new Reading(0, 0, rules.wickOhms(tier), 0, true, rules.wickAmps(tier), rules.wickVolts(tier), 0, com.malevolentgods.fourtwenty.domain.Circuit.NO_LOOP));
        }
        for (Direction d : Direction.values()) {
            BINode node = level.getCapability(BINode.CAPABILITY, pos, d);
            if (node == null) continue;
            Network net = network(level, node, pos);
            if (!net.solved()) net.tick(level, of(level).burnTimers);
            int i = net.indexOf(node);
            if (i < 0) continue;
            double v = net.memberVolts(i), a = Math.abs(net.memberAmps(i)), w = Math.abs(net.memberWatts(i));
            return Optional.of(new Reading(v, a, a > 0 ? v / a : 0, w, false, 0, 0, 0, net.status(i)));
        }
        return Optional.empty();
    }

    /** The voltage, current and power at a block, for its screen: {volts, amps, watts}. */
    public static float[] meter(Level level, BINode node, BlockPos pos) {
        if (level == null || level.isClientSide || node == null) return new float[3];
        Network net = network(level, node, pos);
        int i = net.indexOf(node);
        if (i < 0) return new float[3];
        return new float[]{(float) net.memberVolts(i), (float) Math.abs(net.memberAmps(i)), (float) Math.abs(net.memberWatts(i))};
    }

    /** Forget a level's networks (game tests that rebuild in place). */
    public static void reset(Level level) { LEVELS.remove(level); }
}
