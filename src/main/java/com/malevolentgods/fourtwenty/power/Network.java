package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.Circuit;
import com.malevolentgods.fourtwenty.domain.CircuitRules;
import com.malevolentgods.fourtwenty.domain.Terminals;
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
 * One circuit: the BI blocks on it, each between its own + and − terminal, and the Wicks of one colour between them. There's no ground:
 * current flows only around closed loops (specs/007-polarity-circuits). It's walked once (and again when anything on the level's
 * networks changes, or every few seconds for chunks), solved when what its members want changes, and each tick it moves the Ws the last
 * solution says. Runs of plain Wicks collapse into one resistance; each Wick in a run shares the run's current, and its voltage is
 * interpolated along it. See specs/006-circuit-solver/research.md R8 and R9.
 */
public final class Network {
    static final int MAX_WICKS = 4096;
    private static final int GLOW = 10;

    /** A BI block on the network: its node object, where it is, and the circuit nodes of its + and − terminals. */
    record Member(BINode node, BlockPos pos, int plus, int minus) {}

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
    /** Each member's circuit status (Circuit.OK, NO_LOOP, REVERSED, BROWNOUT); sources are OK. */
    private int[] status = new int[0];
    /** The lowest voltage on the network: Wick voltages and insulation are measured from it. */
    private double floor;
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

    /** Device keys compare by identity: one per block node (its owner, whichever terminal it was reached by). */
    private record DeviceKey(BINode node) {
        @Override public boolean equals(Object o) { return o instanceof DeviceKey k && k.node == node; }
        @Override public int hashCode() { return System.identityHashCode(node); }
    }

    /** One terminal of one block: a node of the circuit. */
    private record TerminalKey(BINode owner, Terminals.Sign sign) {
        @Override public boolean equals(Object o) { return o instanceof TerminalKey k && k.owner == owner && k.sign == sign; }
        @Override public int hashCode() { return System.identityHashCode(owner) * 31 + sign.ordinal(); }
    }

    private static TerminalKey keyOf(BINode terminal) {
        return terminal instanceof Terminal t ? new TerminalKey(t.owner(), t.sign()) : new TerminalKey(terminal, Terminals.Sign.PLUS);
    }

    /** The face a terminal is on, or null if the block shows it on none. */
    private static Direction faceOf(Level level, BlockPos at, BINode terminal) {
        for (Direction d : Direction.values()) if (level.getCapability(BINode.CAPABILITY, at, d) == terminal) return d;
        return null;
    }

    /**
     * Walks out from a BI block (its node, at {@code pos}) through its two terminals: along Wicks of one colour, and into the terminals of
     * the blocks they touch, and on through those blocks' other terminals. A terminal touching a Wick joins its centre through half the
     * Wick's resistance; two terminals facing each other join directly.
     */
    static Network walk(Level level, BINode start, BlockPos pos) {
        Union union = new Union();
        Map<BlockPos, BIRules.Tier> wickTiers = new LinkedHashMap<>();
        Map<BlockPos, Set<BlockPos>> wickEdges = new HashMap<>();
        Map<BINode, BlockPos> devices = new LinkedHashMap<>();
        List<Object[]> contacts = new ArrayList<>();
        ArrayDeque<Object[]> queue = new ArrayDeque<>();
        Set<Object> seen = new HashSet<>();
        java.util.function.BiConsumer<BlockPos, BINode> device = (at, owner) -> {
            if (devices.containsKey(owner)) return;
            devices.put(owner, at.immutable());
            for (Terminals.Sign sign : Terminals.Sign.values()) {
                Terminal t = Terminal.of(owner, sign);
                if (seen.add(keyOf(t))) queue.add(new Object[]{at.immutable(), t});
            }
        };
        device.accept(pos, Terminal.owner(start));
        while (!queue.isEmpty()) {
            Object[] item = queue.poll();
            BlockPos at = (BlockPos) item[0];
            BINode terminal = (BINode) item[1];
            Direction only = terminal == null ? null : faceOf(level, at, terminal);
            if (terminal != null && only == null) continue;
            for (Direction d : terminal == null ? Direction.values() : new Direction[]{only}) {
                BlockPos next = at.relative(d);
                if (!level.isLoaded(next)) continue;
                var nextState = level.getBlockState(next);
                if (nextState.getBlock() instanceof WickBlock wick) {
                    if (terminal != null) contacts.add(new Object[]{terminal, next.immutable()});
                    else if (WickBlock.joins(level.getBlockState(at), nextState)) {
                        wickEdges.computeIfAbsent(at, k -> new HashSet<>()).add(next.immutable());
                        wickEdges.computeIfAbsent(next.immutable(), k -> new HashSet<>()).add(at);
                    } else {
                        continue;
                    }
                    if (wickTiers.size() < MAX_WICKS && seen.add(next.immutable())) {
                        wickTiers.put(next.immutable(), wick.tier());
                        queue.add(new Object[]{next.immutable(), null});
                    }
                    continue;
                }
                BINode other = level.getCapability(BINode.CAPABILITY, next, d.getOpposite());
                if (other == null || other == terminal) continue;
                if (terminal != null) union.join(keyOf(terminal), keyOf(other));
                else contacts.add(new Object[]{other, at});
                device.accept(next, Terminal.owner(other));
            }
        }
        // a Wick is plain when no terminal touches it and it has exactly two Wick neighbours: runs of those collapse
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
        for (var e : devices.entrySet()) {
            BINode owner = e.getKey();
            members.add(new Member(owner, e.getValue(), nodeFor.apply(new TerminalKey(owner, Terminals.Sign.PLUS)), nodeFor.apply(new TerminalKey(owner, Terminals.Sign.MINUS))));
        }
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
            BINode t = (BINode) c[0];
            BlockPos w = (BlockPos) c[1];
            TerminalKey key = keyOf(t);
            if (!devices.containsKey(key.owner()) || !wickTiers.containsKey(w)
                    || !linked.add(System.identityHashCode(key.owner()) + key.sign().name() + "@" + w.asLong())) continue;
            runs.add(new Run(nodeFor.apply(key), nodeFor.apply(w), rules.wickOhms(wickTiers.get(w)) / 2));
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

    static Object key(BINode node) { return new DeviceKey(Terminal.owner(node)); }

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
        for (int i = 0; i < members.size(); i++) {
            BINode n = members.get(i).node();
            int plus = members.get(i).plus(), minus = members.get(i).minus();
            if (n.overloaded(now)) continue;
            BIRules.Tier tier = n.tier();
            double volts = rules.volts(tier);
            switch (n.role()) {
                case SOURCE -> {
                    if (!n.sourceReady()) continue;
                    double watts = n.sourceWatts();
                    sourceIndex[i] = circuit.source(plus, minus, volts, rules.sourceDroop() * volts * volts / watts, rules.sourceLimit() * watts / volts, 0);
                }
                case BATTERY -> {
                    double amps = rules.stashAmps(tier);
                    sourceIndex[i] = circuit.source(plus, minus, rules.stashEmf(tier, n.fill()), rules.stashOhms(tier),
                            n.stored() > 0 ? amps : 0, n.stored() < n.capacity() ? amps : 0);
                }
                case LOAD -> {
                    double watts = n.demandWatts();
                    if (watts > 0) loadIndex[i] = circuit.load(plus, minus, watts, rules.minVolts(tier));
                }
                default -> {}
            }
        }
        solution = circuit.solve();
        status = new int[members.size()];
        for (int i = 0; i < members.size(); i++) status[i] = loadIndex[i] >= 0 ? solution.loadStatus()[loadIndex[i]] : Circuit.OK;
        floor = 0;
        for (int i = 0; i < solution.volts().length; i++) floor = i == 0 ? solution.volts()[0] : Math.min(floor, solution.volts()[i]);
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
        for (int i = 0; i < members.size(); i++) {
            if (Math.abs(memberVolts(i)) > rules.overloadVolts(members.get(i).node().tier())) members.get(i).node().overload(now);
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
        // a Wick burns from heat. Each of its halves (to a neighbour, or to a block it feeds) carries its own current, so its heat is the
        // sum of I²R over those halves; the current that would heat a straight run of it as much is the square root of half that sum
        double heat = 0;
        for (int c : w.incident()) heat += solution.conductorAmps()[c] * solution.conductorAmps()[c];
        return Math.sqrt(heat / 2);
    }

    /** A Wick's voltage above the lowest point of its circuit (its − rail, on a simple loop). */
    double volts(Wick w) { return potential(w) - floor; }

    /** A Wick's voltage in the solve's own terms, for comparing two Wicks. */
    double potential(Wick w) {
        if (solution == null) return 0;
        if (w.node() >= 0) return solution.volts()[w.node()];
        if (w.conductor() < 0) return floor;
        Run r = runs.get(w.conductor());
        return solution.volts()[r.start()] - solution.conductorAmps()[w.conductor()] * w.fromStart();
    }

    /** A member's circuit status: Circuit.OK, BROWNOUT, NO_LOOP or REVERSED. */
    int status(int i) { return i >= 0 && i < status.length ? status[i] : Circuit.NO_LOOP; }

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

    /** The voltage across a member, + terminal minus − terminal. */
    double memberVolts(int i) {
        if (solution == null) return 0;
        Member m = members.get(i);
        double[] v = solution.volts();
        return (m.plus() < v.length ? v[m.plus()] : 0) - (m.minus() < v.length ? v[m.minus()] : 0);
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
            if (n == Terminal.owner(except) || !n.canReceive()) continue;
            if (n.canExtract()) total += n.stored() < n.capacity() ? 1 : 0;
            else total += n.demandWatts();
        }
        return total;
    }

    int indexOf(BINode node) {
        BINode owner = Terminal.owner(node);
        for (int i = 0; i < members.size(); i++) if (members.get(i).node() == owner) return i;
        return -1;
    }

    boolean contains(BINode node) { return indexOf(node) >= 0; }

    @Override public int hashCode() { return Objects.hash(version, walkedAt, members.size()); }
}
