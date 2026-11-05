package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.Circuit;
import com.malevolentgods.fourtwenty.domain.CircuitRules;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One Wick network as a circuit: the BI blocks on it and the Wicks between them. It's walked once (and again when anything on the level's
 * networks changes, or every few seconds for chunks), solved when what its members want changes, and each tick it moves the Ws the last
 * solution says. Runs of plain Wicks collapse into one resistance; each Wick in a run shares the run's current, and its voltage is
 * interpolated along it. See specs/006-circuit-solver/research.md R8 and R9.
 */
public final class Network {
    static final int MAX_WICKS = 4096;
    private static final int GLOW = 10;

    /** A BI block on the network: its node object, where it is, and its circuit node. */
    record Member(BINode node, BlockPos pos, int circuitNode) {}

    /** A Wick: its tier and where it sits in the circuit, as a node, or a share along a collapsed run. */
    record Wick(BlockPos pos, BIRules.Tier tier, int node, int conductor, double fromStart, int[] incident) {}

    private record Run(int start, int end, double ohms) {}

    final int version;
    final long walkedAt;
    final List<Member> members;
    final List<Wick> wicks;
    private final List<Run> runs;
    private final int nodes;

    private Circuit.Solution solution;
    private long signature = Long.MIN_VALUE, tickedAt = Long.MIN_VALUE;
    private int[] sourceIndex, loadIndex;
    private double nominal;
    private final Map<BINode, double[]> carry = new IdentityHashMap<>();

    private Network(int version, long walkedAt, List<Member> members, List<Wick> wicks, List<Run> runs, int nodes) {
        this.version = version;
        this.walkedAt = walkedAt;
        this.members = members;
        this.wicks = wicks;
        this.runs = runs;
        this.nodes = nodes;
    }

    // ---- building -----------------------------------------------------------------------------------------------------------------

    private static final class Union {
        final Map<Object, Object> parent = new HashMap<>();

        Object find(Object x) {
            Object p = parent.getOrDefault(x, x);
            if (p.equals(x)) return x;
            Object root = find(p);
            parent.put(x, root);
            return root;
        }

        void join(Object a, Object b) {
            Object ra = find(a), rb = find(b);
            if (!ra.equals(rb)) parent.put(ra, rb);
        }
    }

    /** Device keys compare by identity, so two faces of one block that give the same node are one terminal. */
    private record DeviceKey(BINode node) {
        @Override public boolean equals(Object o) { return o instanceof DeviceKey k && k.node == node; }
        @Override public int hashCode() { return System.identityHashCode(node); }
    }

    /** Walks out from a BI block's node at {@code pos}, through Wicks and blocks that touch it. */
    static Network walk(Level level, BINode start, BlockPos pos) {
        Union union = new Union();
        Map<BlockPos, BIRules.Tier> wickTiers = new LinkedHashMap<>();
        Map<BlockPos, Set<BlockPos>> wickEdges = new HashMap<>();
        Map<BINode, BlockPos> devices = new LinkedHashMap<>();
        // a block touching a Wick connects to its centre through half the Wick's resistance, so the Wick's own current is real
        List<Object[]> contacts = new ArrayList<>();
        ArrayDeque<Object[]> queue = new ArrayDeque<>();
        Set<Object> seen = new HashSet<>();
        devices.put(start, pos);
        seen.add(new DeviceKey(start));
        queue.add(new Object[]{pos, start});
        while (!queue.isEmpty()) {
            Object[] item = queue.poll();
            BlockPos at = (BlockPos) item[0];
            BINode node = (BINode) item[1];
            for (Direction d : Direction.values()) {
                if (node != null && level.getCapability(BINode.CAPABILITY, at, d) != node) continue;
                BlockPos next = at.relative(d);
                if (!level.isLoaded(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof WickBlock wick) {
                    if (node != null) contacts.add(new Object[]{node, next.immutable()});
                    else {
                        wickEdges.computeIfAbsent(at, k -> new HashSet<>()).add(next);
                        wickEdges.computeIfAbsent(next, k -> new HashSet<>()).add(at);
                    }
                    if (wickTiers.size() < MAX_WICKS && seen.add(next)) {
                        wickTiers.put(next, wick.tier());
                        queue.add(new Object[]{next, null});
                    }
                    continue;
                }
                BINode other = level.getCapability(BINode.CAPABILITY, next, d.getOpposite());
                if (other == null || other == node) continue;
                if (node != null) union.join(new DeviceKey(node), new DeviceKey(other));
                else contacts.add(new Object[]{other, at});
                if (seen.add(new DeviceKey(other))) {
                    devices.put(other, next.immutable());
                    queue.add(new Object[]{next.immutable(), other});
                }
            }
        }
        // a Wick is plain when nothing is joined to it and it has exactly two Wick neighbours: runs of those collapse
        Map<Object, Integer> nodeOf = new HashMap<>();
        Map<BlockPos, Boolean> plain = new HashMap<>();
        Set<BlockPos> touched = new HashSet<>();
        for (Object[] c : contacts) touched.add((BlockPos) c[1]);
        for (BlockPos w : wickTiers.keySet()) {
            plain.put(w, !touched.contains(w) && wickEdges.getOrDefault(w, Set.of()).size() == 2);
        }
        int[] count = {0};
        java.util.function.Function<Object, Integer> nodeFor = key -> nodeOf.computeIfAbsent(union.find(key), k -> count[0]++);
        List<Member> members = new ArrayList<>();
        for (var e : devices.entrySet()) members.add(new Member(e.getKey(), e.getValue(), nodeFor.apply(new DeviceKey(e.getKey()))));
        CircuitRules.Numbers rules = ProcessingConfig.circuit();
        List<Run> runs = new ArrayList<>();
        Map<BlockPos, Wick> wicks = new LinkedHashMap<>();
        Set<String> walked = new HashSet<>();
        for (BlockPos w : wickTiers.keySet()) {
            if (plain.get(w)) continue;
            int from = nodeFor.apply(w);
            for (BlockPos first : wickEdges.getOrDefault(w, Set.of())) {
                // follow plain Wicks from w through first until a Wick that isn't plain
                List<BlockPos> chain = new ArrayList<>();
                List<Double> along = new ArrayList<>();
                BlockPos prev = w, at = first;
                double ohms = 0;
                while (true) {
                    ohms += (rules.wickOhms(wickTiers.get(prev)) + rules.wickOhms(wickTiers.get(at))) / 2;
                    if (!plain.get(at)) break;
                    chain.add(at);
                    along.add(ohms);
                    BlockPos back = prev, here = at;
                    // a plain Wick has exactly two Wick neighbours: go on to the one we didn't come from
                    at = wickEdges.get(here).stream().filter(p -> !p.equals(back)).findFirst().orElseThrow();
                    prev = here;
                }
                if (!walked.add(keyOf(w, first, at, prev))) continue;
                int to = nodeFor.apply(at);
                int index = runs.size();
                runs.add(new Run(from, to, ohms));
                for (int i = 0; i < chain.size(); i++) wicks.put(chain.get(i), new Wick(chain.get(i), wickTiers.get(chain.get(i)), -1, index, along.get(i), null));
            }
        }
        Set<String> linked = new HashSet<>();
        for (Object[] c : contacts) {
            BINode n = (BINode) c[0];
            BlockPos w = (BlockPos) c[1];
            if (!devices.containsKey(n) || !wickTiers.containsKey(w) || !linked.add(System.identityHashCode(n) + "@" + w.asLong())) continue;
            runs.add(new Run(nodeFor.apply(new DeviceKey(n)), nodeFor.apply(w), rules.wickOhms(wickTiers.get(w)) / 2));
        }
        for (BlockPos w : wickTiers.keySet()) {
            if (wicks.containsKey(w)) continue;
            if (plain.get(w)) {
                // a ring of plain Wicks with nothing on it carries nothing
                wicks.put(w, new Wick(w, wickTiers.get(w), -1, -1, 0, null));
                continue;
            }
            int n = nodeFor.apply(w);
            List<Integer> incident = new ArrayList<>();
            for (int i = 0; i < runs.size(); i++) if (runs.get(i).start() == n || runs.get(i).end() == n) incident.add(i);
            wicks.put(w, new Wick(w, wickTiers.get(w), n, -1, 0, incident.stream().mapToInt(Integer::intValue).toArray()));
        }
        return new Network(BIGrid.version(level), level.getGameTime(), members, new ArrayList<>(wicks.values()), runs, count[0]);
    }

    /** One key per run however it's walked into: both end Wicks and both Wicks next to them, sorted. */
    private static String keyOf(BlockPos start, BlockPos firstStep, BlockPos end, BlockPos lastStep) {
        String a = start.asLong() + ":" + firstStep.asLong(), b = end.asLong() + ":" + lastStep.asLong();
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    Set<Object> keys() {
        Set<Object> keys = new HashSet<>();
        for (Member m : members) keys.add(new DeviceKey(m.node()));
        for (Wick w : wicks) keys.add(w.pos());
        return keys;
    }

    static Object key(BINode node) { return new DeviceKey(node); }

    // ---- solving ------------------------------------------------------------------------------------------------------------------

    private long signature(long now) {
        long h = version;
        for (Member m : members) {
            BINode n = m.node();
            boolean overloaded = n.overloaded(now);
            BINode.Role role = n.role();
            h = h * 31 + role.ordinal();
            h = h * 31 + (overloaded ? 1 : 0);
            if (overloaded) continue;
            switch (role) {
                case SOURCE -> h = h * 31 + (n.sourceReady() ? 1 : 0);
                case LOAD -> h = h * 31 + Double.hashCode(n.demandWatts());
                case BATTERY -> h = h * 31 + (long) Math.floor(n.fill() * 50) * 3 + (n.stored() > 0 ? 1 : 0) + (n.stored() < n.capacity() ? 2 : 0);
                default -> {}
            }
        }
        return h;
    }

    private void solve(long now) {
        CircuitRules.Numbers rules = ProcessingConfig.circuit();
        Circuit circuit = new Circuit();
        for (int i = 0; i < nodes; i++) circuit.node();
        for (Run r : runs) circuit.conductor(r.start(), r.end(), r.ohms());
        sourceIndex = new int[members.size()];
        loadIndex = new int[members.size()];
        java.util.Arrays.fill(sourceIndex, -1);
        java.util.Arrays.fill(loadIndex, -1);
        BIRules.Tier top = null;
        for (Member m : members) {
            BINode n = m.node();
            if (n.overloaded(now)) continue;
            BINode.Role role = n.role();
            if (role == BINode.Role.SOURCE && n.sourceReady() || role == BINode.Role.BATTERY && n.stored() > 0) {
                if (top == null || n.tier().ordinal() > top.ordinal()) top = n.tier();
            }
        }
        nominal = top == null ? 0 : rules.volts(top);
        double minVolts = top == null ? 1e-9 : rules.minVolts(top);
        for (int i = 0; i < members.size(); i++) {
            BINode n = members.get(i).node();
            int at = members.get(i).circuitNode();
            if (n.overloaded(now)) continue;
            BIRules.Tier tier = n.tier();
            double volts = rules.volts(tier);
            switch (n.role()) {
                case SOURCE -> {
                    if (!n.sourceReady()) continue;
                    double watts = n.sourceWatts();
                    sourceIndex[i] = circuit.source(at, volts, rules.sourceDroop() * volts * volts / watts, rules.sourceLimit() * watts / volts, 0);
                }
                case BATTERY -> {
                    double amps = rules.stashAmps(tier);
                    sourceIndex[i] = circuit.source(at, rules.stashEmf(tier, n.fill()), rules.stashOhms(tier),
                            n.stored() > 0 ? amps : 0, n.stored() < n.capacity() ? amps : 0);
                }
                case LOAD -> {
                    double watts = n.demandWatts();
                    if (watts > 0) loadIndex[i] = circuit.load(at, watts, minVolts);
                }
                default -> {}
            }
        }
        solution = circuit.solve();
    }

    /** Runs this network's tick once per game tick: solve if anything changed, move the Ws, check overloads and Wicks. */
    void tick(Level level, Map<BlockPos, Integer> burnTimers) {
        long now = level.getGameTime();
        if (tickedAt == now) return;
        tickedAt = now;
        long sig = signature(now);
        if (solution == null || sig != signature) {
            solve(now);
            signature = sig;
        }
        move(now);
        checks(level, now, burnTimers);
    }

    private double carry(BINode n, double add) {
        double[] c = carry.computeIfAbsent(n, k -> new double[1]);
        c[0] += add;
        long whole = (long) Math.floor(c[0]);
        c[0] -= whole;
        return whole;
    }

    private void move(long now) {
        double planned = 0, actual = 0;
        for (int i = 0; i < members.size(); i++) {
            if (sourceIndex[i] < 0) continue;
            double watts = solution.sourceWatts()[sourceIndex[i]];
            if (watts <= 0) continue;
            BINode n = members.get(i).node();
            long want = (long) carry(n, watts / BIRules.TICKS_PER_SECOND);
            planned += watts / BIRules.TICKS_PER_SECOND;
            long got = want > 0 ? n.extract(want) : 0;
            actual += got;
            if (got < want) signature = Long.MIN_VALUE;
        }
        // what came out is what goes in: if a source ran short, everything it fed gets its share of the shortfall
        double share = planned <= 0 ? 0 : Math.min(2, actual / planned);
        for (int i = 0; i < members.size(); i++) {
            BINode n = members.get(i).node();
            double watts = 0;
            if (loadIndex[i] >= 0) watts = solution.loadWatts()[loadIndex[i]];
            else if (sourceIndex[i] >= 0 && solution.sourceWatts()[sourceIndex[i]] < 0) watts = -solution.sourceWatts()[sourceIndex[i]];
            if (watts <= 0) continue;
            long give = (long) carry(n, watts * share * n.efficiency() / BIRules.TICKS_PER_SECOND);
            if (give > 0) n.receive(give);
        }
    }

    private void checks(Level level, long now, Map<BlockPos, Integer> burnTimers) {
        CircuitRules.Numbers rules = ProcessingConfig.circuit();
        double[] v = solution.volts();
        for (Member m : members) {
            if (v.length > m.circuitNode() && v[m.circuitNode()] > rules.overloadVolts(m.node().tier())) m.node().overload(now);
        }
        List<BlockPos> burn = new ArrayList<>();
        boolean glow = now % GLOW == 0;
        for (Wick w : wicks) {
            double amps = amps(w), volts = volts(w);
            boolean over = amps > rules.wickAmps(w.tier()) || volts > rules.wickVolts(w.tier());
            if (over) {
                int ticks = burnTimers.merge(w.pos(), 1, Integer::sum);
                if (CircuitRules.burns(ticks, rules.burnTicks())) {
                    burn.add(w.pos());
                    if (volts > rules.wickVolts(w.tier())) com.mojang.logging.LogUtils.getLogger().info(
                            "FourTwenty: a {} Wick at {} burned out on {} Kb, over its insulation", w.tier().getSerializedName(), w.pos().toShortString(), Math.round(volts * 100) / 100.0);
                }
            } else {
                burnTimers.remove(w.pos());
            }
            if (glow && amps > 1e-6 * rules.baseAmps(w.tier())) WickBlock.carry(level, w.pos());
        }
        for (BlockPos pos : burn) {
            burnTimers.remove(pos);
            if (level.getBlockState(pos).getBlock() instanceof WickBlock) BurnOut.burn(level, pos);
        }
    }

    // ---- readings -----------------------------------------------------------------------------------------------------------------

    double amps(Wick w) {
        if (solution == null) return 0;
        if (w.conductor() >= 0) return Math.abs(solution.conductorAmps()[w.conductor()]);
        if (w.node() < 0) return 0;
        // a Wick that's a node carries what flows through it: half of everything in and out of it (what goes in comes out)
        double through = 0;
        for (int c : w.incident()) through += Math.abs(solution.conductorAmps()[c]);
        return through / 2;
    }

    double volts(Wick w) {
        if (solution == null) return 0;
        if (w.node() >= 0) return solution.volts()[w.node()];
        if (w.conductor() < 0) return 0;
        Run r = runs.get(w.conductor());
        return solution.volts()[r.start()] - solution.conductorAmps()[w.conductor()] * w.fromStart();
    }

    /** Current into the circuit from a member: positive out of a source, negative into a load or a charging battery. */
    double memberAmps(int i) {
        if (solution == null) return 0;
        if (sourceIndex[i] >= 0) return solution.sourceAmps()[sourceIndex[i]];
        if (loadIndex[i] >= 0) return -solution.loadAmps()[loadIndex[i]];
        return 0;
    }

    double memberWatts(int i) {
        if (solution == null) return 0;
        if (sourceIndex[i] >= 0) return solution.sourceWatts()[sourceIndex[i]];
        if (loadIndex[i] >= 0) return -solution.loadWatts()[loadIndex[i]];
        return 0;
    }

    double memberVolts(int i) {
        if (solution == null) return 0;
        int n = members.get(i).circuitNode();
        return n < solution.volts().length ? solution.volts()[n] : 0;
    }

    double wickLoss(Wick w) {
        CircuitRules.Numbers rules = ProcessingConfig.circuit();
        double a = amps(w);
        return a * a * rules.wickOhms(w.tier());
    }

    double nominal() { return nominal; }

    boolean solved() { return solution != null; }

    /** What everything on it but {@code except} asks for, in Willards, without solving: loads' demand, and room in batteries. */
    double demandBeyond(BINode except) {
        double total = 0;
        for (Member m : members) {
            BINode n = m.node();
            if (n == except || !n.canReceive()) continue;
            if (n.canExtract()) total += n.stored() < n.capacity() ? 1 : 0;
            else total += n.demandWatts();
        }
        return total;
    }

    int indexOf(BINode node) {
        for (int i = 0; i < members.size(); i++) if (members.get(i).node() == node) return i;
        return -1;
    }

    boolean contains(BINode node) { return indexOf(node) >= 0; }

    @Override public int hashCode() { return Objects.hash(version, walkedAt, members.size()); }
}
