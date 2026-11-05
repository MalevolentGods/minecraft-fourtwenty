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
    public record Reading(double volts, double amps, double ohms, double watts, boolean wick, double ampRating, double voltRating, double lossWatts) {}

    /** A reading at a Wick or at the BI block there; empty when it's not on a network. */
    public static Optional<Reading> reading(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof WickBlock wick) {
            for (Direction d : Direction.values()) {
                BINode node = level.getCapability(BINode.CAPABILITY, pos.relative(d), d.getOpposite());
                if (node == null) continue;
                Network net = network(level, node, pos.relative(d));
                if (!net.solved()) net.tick(level, of(level).burnTimers);
                for (Network.Wick w : net.wicks) {
                    if (!w.pos().equals(pos)) continue;
                    var rules = com.malevolentgods.fourtwenty.processing.ProcessingConfig.circuit();
                    double v = net.volts(w), a = net.amps(w);
                    return Optional.of(new Reading(v, a, rules.wickOhms(w.tier()), v * a, true, rules.wickAmps(w.tier()), rules.wickVolts(w.tier()), net.wickLoss(w)));
                }
            }
            BIRules.Tier tier = wick.tier();
            var rules = com.malevolentgods.fourtwenty.processing.ProcessingConfig.circuit();
            return Optional.of(new Reading(0, 0, rules.wickOhms(tier), 0, true, rules.wickAmps(tier), rules.wickVolts(tier), 0));
        }
        for (Direction d : Direction.values()) {
            BINode node = level.getCapability(BINode.CAPABILITY, pos, d);
            if (node == null) continue;
            Network net = network(level, node, pos);
            if (!net.solved()) net.tick(level, of(level).burnTimers);
            int i = net.indexOf(node);
            if (i < 0) continue;
            double v = net.memberVolts(i), a = Math.abs(net.memberAmps(i)), w = Math.abs(net.memberWatts(i));
            return Optional.of(new Reading(v, a, a > 0 ? v / a : 0, w, false, 0, 0, 0));
        }
        return Optional.empty();
    }

    /** The voltage and current at a block, for its screen: {volts, amps}. */
    public static float[] meter(Level level, BINode node, BlockPos pos) {
        if (level == null || level.isClientSide || node == null) return new float[2];
        Network net = network(level, node, pos);
        int i = net.indexOf(node);
        if (i < 0) return new float[2];
        return new float[]{(float) net.memberVolts(i), (float) Math.abs(net.memberAmps(i))};
    }

    /** Forget a level's networks (game tests that rebuild in place). */
    public static void reset(Level level) { LEVELS.remove(level); }
}
