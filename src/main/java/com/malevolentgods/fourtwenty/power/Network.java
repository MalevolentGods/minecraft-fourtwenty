package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.Circuit;
import com.malevolentgods.fourtwenty.domain.CircuitRules;
import com.malevolentgods.fourtwenty.domain.DaisyLine;
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
    record Wick(BlockPos pos, Core core, BIRules.Tier tier, int node, int conductor, double fromStart, int[] incident, double ohmsScale, double ampsScale) {}

    /** A component's pin: a node of the circuit (docs/electronics/audit-and-plan.md 3.1). */
    record PinKey(BlockPos pos, int pin) {}

    /** A component on this network: where it is, what it is, and the circuit node of each of its pins. */
    record Placed(BlockPos pos, com.malevolentgods.fourtwenty.electronics.ComponentType type, int[] pins) {}

    /** One wire of an Eighth Ribbon or Roach Clip. */
    record RibbonKey(BlockPos pos, int channel) {}

    private record Run(int start, int end, double ohms) {}

    final int version;
    final long walkedAt;
    final List<Member> members;
    final List<Wick> wicks;
    private final List<Run> runs;
    private final int nodes;

    private Circuit.Solution solution;
    private long signature = Long.MIN_VALUE, tickedAt = Long.MIN_VALUE;
    private int[] sourceIndex, loadIndex, ledIndex;
    /** Each member's circuit status (Circuit.OK, NO_LOOP, REVERSED, BROWNOUT); sources are OK. */
    private int[] status = new int[0];
    /** The lowest voltage on the network: Wick voltages and insulation are measured from it. */
    private double floor;
    private double nominal;
    private final Map<BINode, double[]> carry = new IdentityHashMap<>();

    /** Each cable of a Daisy line that can't be ordered, and why. */
    final Map<BlockPos, DaisyLine.Reason> daisyFailed;
    /** Coupled windings (Hit Boxes), each {primary, secondary}: one two-port each in the solve. */
    private final List<BINode[]> couplings;
    /** For each member, the transformer it is a winding of in the last solution, or -1. */
    private int[] windingOf = new int[0];

    private static final double[] ONE = {1, 1};
    /** The components on this network (electronics); when there are any, it runs in time every tick (see {@link #tickLive}). */
    final List<Placed> placed;

    private Network(int version, long walkedAt, List<Member> members, List<Wick> wicks, List<Run> runs, int nodes, Map<BlockPos, DaisyLine.Reason> daisyFailed,
                    List<BINode[]> couplings, List<Placed> placed) {
        this.placed = placed;
        this.daisyFailed = daisyFailed;
        this.couplings = couplings;
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


    /** One conductor of the wiring: a single Wick (WIRE), or one core of a two-core cable. */
    enum Core { WIRE, PLUS, MINUS }

    record Conductor(BlockPos pos, Core core) {
        String id() { return pos.asLong() + "/" + core.ordinal(); }
    }

    /** Faces in the order ties are settled along a Daisy line. */
    private static final Direction[] FACE_ORDER = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};

    /**
     * The block a cable touching {@code face} of the powered block at {@code at} connects to: the node behind that face, or, for a face
     * with no terminal, the block's node from any face. The Hit Box connects only through the winding of the face touched.
     */
    static BINode ownerOnFace(Level level, BlockPos at, Direction face) {
        BINode cap = level.getCapability(BINode.CAPABILITY, at, face);
        if (cap != null) return Terminal.owner(cap);
        if (level.getBlockState(at).getBlock() instanceof HitBoxBlock) return null;
        for (Direction d : Direction.values()) {
            cap = level.getCapability(BINode.CAPABILITY, at, d);
            if (cap != null) return Terminal.owner(cap);
        }
        return null;
    }

    /** A block on a Daisy line takes current in at + (a load) or, if it puts power out, at − (a source or battery). */
    private static boolean pushes(BINode owner) { return owner.canExtract(); }

    /**
     * Walks out from a BI block (its node, at {@code pos}) through its two terminals: along Wicks of one colour, and into the terminals of
     * the blocks they touch, and on through those blocks' other terminals. A terminal touching a Wick joins its centre through half the
     * Wick's resistance; two terminals facing each other join directly. Two-core cables (specs/009-twin-daisy-wicks) are walked the same
     * way, each core a conductor: a Twin Wick puts every block it touches across its cores, a Daisy Wick splices them into one string.
     */
    static Network walk(Level level, BINode start, BlockPos pos) {
        Union union = new Union();
        Map<Conductor, BIRules.Tier> wickTiers = new LinkedHashMap<>();
        Map<Conductor, Set<Conductor>> wickEdges = new HashMap<>();
        Map<BINode, BlockPos> devices = new LinkedHashMap<>();
        List<Object[]> contacts = new ArrayList<>();
        ArrayDeque<Object[]> queue = new ArrayDeque<>();
        Set<Object> seen = new HashSet<>();
        // cables: where they are, which join, which blocks and single Wicks they touch
        Map<BlockPos, CableBlock> cables = new LinkedHashMap<>();
        Map<BlockPos, Set<BlockPos>> cableEdges = new HashMap<>();
        Map<String, Object[]> cableContacts = new LinkedHashMap<>(); // owner, cable pos, face of the cable it's on
        Map<String, Object[]> wiresAtCables = new LinkedHashMap<>(); // wire pos, cable pos
        List<BINode[]> couplings = new ArrayList<>();
        Set<BINode> coupled = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        java.util.function.BiConsumer<BlockPos, BINode> device = (at, owner) -> {
            if (devices.containsKey(owner)) return;
            devices.put(owner, at.immutable());
            queue.add(new Object[]{at.immutable(), owner});
        };
        Map<Conductor, double[]> scales = new HashMap<>();
        java.util.function.BiConsumer<BlockPos, WickBlock> addWick = (at, wick) -> {
            Conductor c = new Conductor(at.immutable(), Core.WIRE);
            if (!wickTiers.containsKey(c) && wickTiers.size() < MAX_WICKS) {
                wickTiers.put(c, wick.tier());
                if (wick.ohmsScale() != 1 || wick.ampsScale() != 1) scales.put(c, new double[]{wick.ohmsScale(), wick.ampsScale()});
            }
        };
        // components (electronics): their pins are nodes, joined to any Wick or Hemp Thread on a pin face, or to another component's pin
        Map<BlockPos, com.malevolentgods.fourtwenty.electronics.ComponentType> components = new LinkedHashMap<>();
        java.util.function.Consumer<BlockPos> component = at -> {
            if (components.containsKey(at) || !(level.getBlockState(at).getBlock() instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlock cb)) return;
            components.put(at.immutable(), cb.type());
            queue.add(new Object[]{at.immutable(), null, "component"});
        };
        // Eighth Ribbons and Roach Clips: each wire joined straight through, by union (signal wire, no resistance)
        java.util.function.Consumer<BlockPos> ribbon = at -> {
            if (seen.add(at.immutable())) queue.add(new Object[]{at.immutable(), null, "ribbon"});
        };
        int bus = com.malevolentgods.fourtwenty.electronics.ComponentType.BUS;
        java.util.function.BiConsumer<Conductor, Conductor> edge = (a, b) -> {
            wickEdges.computeIfAbsent(a, k -> new HashSet<>()).add(b);
            wickEdges.computeIfAbsent(b, k -> new HashSet<>()).add(a);
        };
        device.accept(pos, Terminal.owner(start));
        while (!queue.isEmpty()) {
            Object[] item = queue.poll();
            BlockPos at = (BlockPos) item[0];
            BINode terminal = (BINode) item[1];
            var here = level.getBlockState(at);
            if (item.length == 3 && "ribbon".equals(item[2])) {
                // a Ribbon or Roach Clip: wire for wire into the next Ribbon or a bus face; a clip's channel into a Wick or a single pin
                boolean clip = here.getBlock() instanceof com.malevolentgods.fourtwenty.electronics.RoachClipBlock;
                int channel = clip ? com.malevolentgods.fourtwenty.electronics.RoachClipBlock.channel(here) : -1;
                for (Direction d : Direction.values()) {
                    BlockPos next = at.relative(d);
                    if (!level.isLoaded(next)) continue;
                    var nextState = level.getBlockState(next);
                    if (nextState.getBlock() instanceof com.malevolentgods.fourtwenty.electronics.RibbonBlock) {
                        for (int c = 0; c < bus; c++) union.join(new RibbonKey(at, c), new RibbonKey(next.immutable(), c));
                        ribbon.accept(next);
                        continue;
                    }
                    int first = com.malevolentgods.fourtwenty.electronics.ComponentBlock.busAt(nextState, d.getOpposite());
                    if (first >= 0) {
                        for (int c = 0; c < bus; c++) union.join(new RibbonKey(at, c), new PinKey(next.immutable(), first + c));
                        component.accept(next);
                        continue;
                    }
                    if (!clip) continue;
                    int single = com.malevolentgods.fourtwenty.electronics.ComponentBlock.singlePinAt(nextState, d.getOpposite());
                    if (single >= 0) {
                        union.join(new RibbonKey(at, channel), new PinKey(next.immutable(), single));
                        component.accept(next);
                        continue;
                    }
                    if (nextState.getBlock() instanceof WickBlock wick) {
                        contacts.add(new Object[]{new RibbonKey(at, channel), new Conductor(next.immutable(), Core.WIRE)});
                        if (wickTiers.size() < MAX_WICKS && seen.add(next.immutable())) {
                            addWick.accept(next, wick);
                            queue.add(new Object[]{next.immutable(), null});
                        }
                    }
                }
                continue;
            }
            if (item.length == 3) {
                // a component: each pin face, to the Wick or Hemp Thread there, or straight into another component's pin; a bus face to
                // a Ribbon or another bus face, wire for wire; a single pin to a Roach Clip's channel
                for (Direction d : Direction.values()) {
                    int pin = com.malevolentgods.fourtwenty.electronics.ComponentBlock.pinAt(here, d);
                    if (pin < 0) continue;
                    int width = com.malevolentgods.fourtwenty.electronics.ComponentBlock.widthAt(here, d);
                    BlockPos next = at.relative(d);
                    if (!level.isLoaded(next)) continue;
                    var nextState = level.getBlockState(next);
                    if (nextState.getBlock() instanceof com.malevolentgods.fourtwenty.electronics.RibbonBlock) {
                        if (width == bus) {
                            for (int c = 0; c < bus; c++) union.join(new PinKey(at, pin + c), new RibbonKey(next.immutable(), c));
                            ribbon.accept(next);
                        } else if (nextState.getBlock() instanceof com.malevolentgods.fourtwenty.electronics.RoachClipBlock) {
                            union.join(new PinKey(at, pin), new RibbonKey(next.immutable(), com.malevolentgods.fourtwenty.electronics.RoachClipBlock.channel(nextState)));
                            ribbon.accept(next);
                        }
                        continue;
                    }
                    if (width != 1) {
                        int theirs = com.malevolentgods.fourtwenty.electronics.ComponentBlock.busAt(nextState, d.getOpposite());
                        if (width == bus && theirs >= 0) {
                            for (int c = 0; c < bus; c++) union.join(new PinKey(at, pin + c), new PinKey(next.immutable(), theirs + c));
                            component.accept(next);
                        }
                        continue;
                    }
                    if (nextState.getBlock() instanceof WickBlock wick) {
                        contacts.add(new Object[]{new PinKey(at, pin), new Conductor(next.immutable(), Core.WIRE)});
                        if (wickTiers.size() < MAX_WICKS && seen.add(next.immutable())) {
                            addWick.accept(next, wick);
                            queue.add(new Object[]{next.immutable(), null});
                        }
                        continue;
                    }
                    int theirs = com.malevolentgods.fourtwenty.electronics.ComponentBlock.singlePinAt(nextState, d.getOpposite());
                    if (theirs >= 0) {
                        union.join(new PinKey(at, pin), new PinKey(next.immutable(), theirs));
                        component.accept(next);
                    }
                }
                continue;
            }
            if (terminal == null && here.getBlock() instanceof CableBlock cable) {
                cables.put(at, cable);
                for (Direction d : Direction.values()) {
                    BlockPos next = at.relative(d);
                    if (!level.isLoaded(next)) continue;
                    var nextState = level.getBlockState(next);
                    if (nextState.getBlock() instanceof CableBlock) {
                        if (!CableBlock.joins(here, nextState)) continue;
                        cableEdges.computeIfAbsent(at, k -> new HashSet<>()).add(next.immutable());
                        cableEdges.computeIfAbsent(next.immutable(), k -> new HashSet<>()).add(at);
                        if (cables.size() < MAX_WICKS && seen.add(next.immutable())) queue.add(new Object[]{next.immutable(), null});
                        continue;
                    }
                    if (nextState.getBlock() instanceof WickBlock wick) {
                        if (!CableBlock.takesWick(nextState)) continue;
                        wiresAtCables.put(next.asLong() + ">" + at.asLong(), new Object[]{next.immutable(), at});
                        addWick.accept(next, wick);
                        if (seen.add(next.immutable())) queue.add(new Object[]{next.immutable(), null});
                        continue;
                    }
                    if (!CableBlock.plugsInto(nextState, d.getOpposite())) continue;
                    BINode owner = ownerOnFace(level, next, d.getOpposite());
                    if (owner == null) continue;
                    cableContacts.put(System.identityHashCode(owner) + "@" + at.asLong() + ":" + d.ordinal(), new Object[]{owner, at, d});
                    device.accept(next, owner);
                }
                continue;
            }
            if (terminal != null) {
                // a transformer winding brings its other winding into the same circuit: the two are solved as one two-port
                BINode partner = terminal.coupled();
                if (partner != null && coupled.add(terminal) && coupled.add(partner)) {
                    couplings.add(terminal.primary() ? new BINode[]{terminal, partner} : new BINode[]{partner, terminal});
                    device.accept(at, partner);
                }
                // a block: red and black Wicks, and cables, on any of its faces (specs/010-colour-polarity); blocks touching don't join
                for (Direction d : Direction.values()) {
                    if (ownerOnFace(level, at, d) != terminal) continue;
                    BlockPos next = at.relative(d);
                    if (!level.isLoaded(next)) continue;
                    var nextState = level.getBlockState(next);
                    if (nextState.getBlock() instanceof CableBlock) {
                        if (seen.add(next.immutable())) queue.add(new Object[]{next.immutable(), null});
                        continue;
                    }
                    if (!(nextState.getBlock() instanceof WickBlock wick)) continue;
                    Terminals.Sign sign = Terminal.signOf(nextState);
                    if (sign == null) continue;
                    contacts.add(new Object[]{Terminal.of(terminal, sign), new Conductor(next.immutable(), Core.WIRE)});
                    if (wickTiers.size() < MAX_WICKS && seen.add(next.immutable())) {
                        addWick.accept(next, wick);
                        queue.add(new Object[]{next.immutable(), null});
                    }
                }
                continue;
            }
            if (!(here.getBlock() instanceof WickBlock)) continue;
            Terminals.Sign mine = Terminal.signOf(here);
            for (Direction d : Direction.values()) {
                BlockPos next = at.relative(d);
                if (!level.isLoaded(next)) continue;
                var nextState = level.getBlockState(next);
                if (nextState.getBlock() instanceof CableBlock) {
                    // a single Wick meets a cable only red or black
                    if (CableBlock.takesWick(here) && seen.add(next.immutable())) queue.add(new Object[]{next.immutable(), null});
                    continue;
                }
                if (nextState.getBlock() instanceof WickBlock wick) {
                    if (!WickBlock.joins(here, nextState)) continue;
                    edge.accept(new Conductor(at, Core.WIRE), new Conductor(next.immutable(), Core.WIRE));
                    if (wickTiers.size() < MAX_WICKS && seen.add(next.immutable())) {
                        addWick.accept(next, wick);
                        queue.add(new Object[]{next.immutable(), null});
                    }
                    continue;
                }
                if (nextState.getBlock() instanceof com.malevolentgods.fourtwenty.electronics.RoachClipBlock) {
                    contacts.add(new Object[]{new RibbonKey(next.immutable(), com.malevolentgods.fourtwenty.electronics.RoachClipBlock.channel(nextState)), new Conductor(at, Core.WIRE)});
                    ribbon.accept(next);
                    continue;
                }
                if (nextState.getBlock() instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlock) {
                    int pin = com.malevolentgods.fourtwenty.electronics.ComponentBlock.singlePinAt(nextState, d.getOpposite());
                    if (pin >= 0) {
                        contacts.add(new Object[]{new PinKey(next.immutable(), pin), new Conductor(at, Core.WIRE)});
                        component.accept(next);
                    }
                    continue;
                }
                if (mine == null || !Terminal.pluggable(nextState, d.getOpposite())) continue;
                BINode owner = ownerOnFace(level, next, d.getOpposite());
                if (owner == null) continue;
                contacts.add(new Object[]{Terminal.of(owner, mine), new Conductor(at, Core.WIRE)});
                device.accept(next, owner);
            }
        }
        // ---- the cables' cores --------------------------------------------------------------------------------------------------------
        for (var e : cables.entrySet()) {
            wickTiers.put(new Conductor(e.getKey(), Core.PLUS), e.getValue().tier());
            wickTiers.put(new Conductor(e.getKey(), Core.MINUS), e.getValue().tier());
        }
        Map<BlockPos, DaisyLine.Reason> daisyFailed = new HashMap<>();
        // Twin Wick: cores joined core to core, blocks across them, red Wicks on +, black on −
        for (var e : cableEdges.entrySet()) {
            if (cables.get(e.getKey()) == null || cables.get(e.getKey()).kind() != CableBlock.Kind.TWIN) continue;
            for (BlockPos b : e.getValue()) {
                if (!cables.containsKey(b)) continue;
                edge.accept(new Conductor(e.getKey(), Core.PLUS), new Conductor(b, Core.PLUS));
                edge.accept(new Conductor(e.getKey(), Core.MINUS), new Conductor(b, Core.MINUS));
            }
        }
        for (Object[] c : cableContacts.values()) {
            BlockPos at = (BlockPos) c[1];
            if (cables.get(at).kind() != CableBlock.Kind.TWIN) continue;
            BINode owner = (BINode) c[0];
            contacts.add(new Object[]{Terminal.of(owner, Terminals.Sign.PLUS), new Conductor(at, Core.PLUS)});
            contacts.add(new Object[]{Terminal.of(owner, Terminals.Sign.MINUS), new Conductor(at, Core.MINUS)});
        }
        Map<BlockPos, Map<WickBlock.Colour, BlockPos>> wiresByCable = new HashMap<>();
        for (Object[] w : wiresAtCables.values()) {
            BlockPos wire = (BlockPos) w[0], at = (BlockPos) w[1];
            if (!wickTiers.containsKey(new Conductor(wire, Core.WIRE))) continue;
            WickBlock.Colour colour = level.getBlockState(wire).getValue(WickBlock.COLOUR);
            if (cables.get(at).kind() == CableBlock.Kind.TWIN) {
                edge.accept(new Conductor(wire, Core.WIRE), new Conductor(at, colour == WickBlock.Colour.RED ? Core.PLUS : Core.MINUS));
            } else {
                wiresByCable.computeIfAbsent(at, k -> new java.util.EnumMap<>(WickBlock.Colour.class)).putIfAbsent(colour, wire);
            }
        }
        // Daisy Wick: each line ordered, its blocks spliced into the supply core, the return core closing the loop
        daisy(cables, cableEdges, cableContacts, wiresByCable, daisyFailed, union, contacts, edge);
        // ---- runs and nodes, as for single Wicks ------------------------------------------------------------------------------------
        // a conductor is plain when no terminal touches it and it has exactly two neighbours: runs of those collapse
        Map<Object, Integer> nodeOf = new HashMap<>();
        Map<Conductor, Boolean> plain = new HashMap<>();
        Set<Conductor> touched = new HashSet<>();
        for (Object[] c : contacts) touched.add((Conductor) c[1]);
        for (Conductor w : wickTiers.keySet()) {
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
        java.util.function.Function<Conductor, Double> ohmsOf = c -> rules.wickOhms(wickTiers.get(c)) * scales.getOrDefault(c, ONE)[0];
        java.util.function.Function<Conductor, double[]> scaleOf = c -> scales.getOrDefault(c, ONE);
        List<Run> runs = new ArrayList<>();
        Map<Conductor, Wick> wicks = new LinkedHashMap<>();
        Set<String> walked = new HashSet<>();
        for (Conductor w : wickTiers.keySet()) {
            if (plain.get(w)) continue;
            int from = nodeFor.apply(w);
            for (Conductor first : wickEdges.getOrDefault(w, Set.of())) {
                if (!wickTiers.containsKey(first)) continue;
                // follow plain conductors from w through first until one that isn't plain
                List<Conductor> chain = new ArrayList<>();
                List<Double> along = new ArrayList<>();
                Conductor prev = w, at = first;
                double ohms = 0;
                while (true) {
                    ohms += (ohmsOf.apply(prev) + ohmsOf.apply(at)) / 2;
                    if (!plain.get(at)) break;
                    chain.add(at);
                    along.add(ohms);
                    Conductor back = prev, here = at;
                    // a plain conductor has exactly two neighbours: go on to the one we didn't come from
                    at = wickEdges.get(here).stream().filter(p -> !p.equals(back)).findFirst().orElseThrow();
                    prev = here;
                }
                if (!walked.add(keyOf(w, first, at, prev))) continue;
                int to = nodeFor.apply(at);
                int index = runs.size();
                runs.add(new Run(from, to, ohms));
                for (int i = 0; i < chain.size(); i++) wicks.put(chain.get(i), new Wick(chain.get(i).pos(), chain.get(i).core(), wickTiers.get(chain.get(i)), -1, index, along.get(i), null,
                        scaleOf.apply(chain.get(i))[0], scaleOf.apply(chain.get(i))[1]));
            }
        }
        Set<String> linked = new HashSet<>();
        for (Object[] c : contacts) {
            Conductor w = (Conductor) c[1];
            if (!wickTiers.containsKey(w)) continue;
            Object key;
            String id;
            if (c[0] instanceof PinKey pk) {
                if (!components.containsKey(pk.pos())) continue;
                key = pk;
                id = "pin" + pk.pos().asLong() + ":" + pk.pin();
            } else if (c[0] instanceof RibbonKey rk) {
                key = rk;
                id = "ribbon" + rk.pos().asLong() + ":" + rk.channel();
            } else {
                TerminalKey tk = keyOf((BINode) c[0]);
                if (!devices.containsKey(tk.owner())) continue;
                key = tk;
                id = System.identityHashCode(tk.owner()) + tk.sign().name();
            }
            if (!linked.add(id + "@" + w.id())) continue;
            runs.add(new Run(nodeFor.apply(key), nodeFor.apply(w), ohmsOf.apply(w) / 2));
        }
        for (Conductor w : wickTiers.keySet()) {
            if (wicks.containsKey(w)) continue;
            if (plain.get(w)) {
                // a ring of plain conductors with nothing on it carries nothing
                wicks.put(w, new Wick(w.pos(), w.core(), wickTiers.get(w), -1, -1, 0, null, scaleOf.apply(w)[0], scaleOf.apply(w)[1]));
                continue;
            }
            int n = nodeFor.apply(w);
            List<Integer> incident = new ArrayList<>();
            for (int i = 0; i < runs.size(); i++) if (runs.get(i).start() == n || runs.get(i).end() == n) incident.add(i);
            wicks.put(w, new Wick(w.pos(), w.core(), wickTiers.get(w), n, -1, 0, incident.stream().mapToInt(Integer::intValue).toArray(),
                    scaleOf.apply(w)[0], scaleOf.apply(w)[1]));
        }
        List<Placed> placed = new ArrayList<>();
        for (var e : components.entrySet()) {
            int[] pins = new int[e.getValue().layout.pins.size()];
            for (int i = 0; i < pins.length; i++) pins[i] = nodeFor.apply(new PinKey(e.getKey(), i));
            placed.add(new Placed(e.getKey(), e.getValue(), pins));
        }
        return new Network(BIGrid.version(level), level.getGameTime(), members, new ArrayList<>(wicks.values()), runs, count[0], daisyFailed, couplings, placed);
    }

    /**
     * Every Daisy line among {@code cables}: grouped by the cables that join and the blocks they share, ordered by {@link DaisyLine}, and
     * spliced (research R4). A line that can't be ordered gets no connections, and each of its cables remembers why.
     */
    private static void daisy(Map<BlockPos, CableBlock> cables, Map<BlockPos, Set<BlockPos>> cableEdges, Map<String, Object[]> cableContacts,
                              Map<BlockPos, Map<WickBlock.Colour, BlockPos>> wiresByCable, Map<BlockPos, DaisyLine.Reason> failed, Union union,
                              List<Object[]> contacts, java.util.function.BiConsumer<Conductor, Conductor> edge) {
        // group: cables that join, and cables that touch the same block, are one line
        Union groups = new Union();
        Map<BINode, List<Object[]>> byOwner = new LinkedHashMap<>();
        for (Object[] c : cableContacts.values()) {
            BlockPos at = (BlockPos) c[1];
            if (cables.get(at).kind() != CableBlock.Kind.DAISY) continue;
            byOwner.computeIfAbsent((BINode) c[0], k -> new ArrayList<>()).add(c);
        }
        for (var e : cables.entrySet()) {
            if (e.getValue().kind() != CableBlock.Kind.DAISY) continue;
            groups.find(e.getKey());
            for (BlockPos b : cableEdges.getOrDefault(e.getKey(), Set.of())) if (cables.containsKey(b)) groups.join(e.getKey(), b);
        }
        for (List<Object[]> touches : byOwner.values()) for (Object[] c : touches) groups.join(touches.get(0)[1], c[1]);
        Map<Object, List<BlockPos>> lines = new LinkedHashMap<>();
        for (var e : cables.entrySet()) if (e.getValue().kind() == CableBlock.Kind.DAISY) lines.computeIfAbsent(groups.find(e.getKey()), k -> new ArrayList<>()).add(e.getKey());
        for (List<BlockPos> line : lines.values()) {
            line.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
            Map<BlockPos, Integer> id = new HashMap<>();
            for (int i = 0; i < line.size(); i++) id.put(line.get(i), i);
            Map<Integer, Set<Integer>> neighbours = new HashMap<>();
            for (BlockPos c : line) {
                Set<Integer> ns = new HashSet<>();
                for (BlockPos b : cableEdges.getOrDefault(c, Set.of())) if (id.containsKey(b)) ns.add(id.get(b));
                neighbours.put(id.get(c), ns);
            }
            List<BINode> owners = new ArrayList<>();
            Map<Integer, List<DaisyLine.Contact>> touching = new HashMap<>();
            for (var e : byOwner.entrySet()) {
                if (!id.containsKey((BlockPos) e.getValue().get(0)[1])) continue;
                int d = owners.size();
                owners.add(e.getKey());
                for (Object[] c : e.getValue()) {
                    int face = java.util.Arrays.asList(FACE_ORDER).indexOf((Direction) c[2]);
                    touching.computeIfAbsent(d, k -> new ArrayList<>()).add(new DaisyLine.Contact(id.get((BlockPos) c[1]), face));
                }
            }
            DaisyLine.Result result = DaisyLine.order(neighbours, touching);
            if (!result.ok()) {
                for (BlockPos c : line) failed.put(c, result.failure());
                continue;
            }
            // fed from rails: a red Wick at one end and a black at the other
            BlockPos first = line.get(result.firstCable()), last = line.get(result.lastCable());
            BlockPos red = wire(wiresByCable, first, WickBlock.Colour.RED), black = wire(wiresByCable, last, WickBlock.Colour.BLACK);
            if (red == null || black == null) {
                BlockPos r2 = wire(wiresByCable, last, WickBlock.Colour.RED), b2 = wire(wiresByCable, first, WickBlock.Colour.BLACK);
                if (r2 != null && b2 != null) {
                    result = result.reversed();
                    red = r2;
                    black = b2;
                    first = line.get(result.firstCable());
                    last = line.get(result.lastCable());
                }
            }
            boolean rails = red != null && black != null && !first.equals(last);
            Object point = rails ? new Conductor(red, Core.WIRE) : new Conductor(first, Core.MINUS);
            BlockPos previousCable = null;
            for (DaisyLine.Item item : result.items()) {
                if (item.device()) {
                    BINode owner = owners.get(item.id());
                    boolean pushes = pushes(owner);
                    Terminal up = Terminal.of(owner, pushes ? Terminals.Sign.MINUS : Terminals.Sign.PLUS);
                    Terminal down = Terminal.of(owner, pushes ? Terminals.Sign.PLUS : Terminals.Sign.MINUS);
                    connect(point, up, union, contacts, edge);
                    point = down;
                } else {
                    BlockPos c = line.get(item.id());
                    Conductor plus = new Conductor(c, Core.PLUS);
                    connect(point, plus, union, contacts, edge);
                    point = plus;
                    if (!rails && previousCable != null) edge.accept(new Conductor(previousCable, Core.MINUS), new Conductor(c, Core.MINUS));
                    previousCable = c;
                }
            }
            connect(point, rails ? new Conductor(black, Core.WIRE) : new Conductor(last, Core.MINUS), union, contacts, edge);
        }
    }

    private static BlockPos wire(Map<BlockPos, Map<WickBlock.Colour, BlockPos>> wiresByCable, BlockPos cable, WickBlock.Colour colour) {
        var m = wiresByCable.get(cable);
        return m == null ? null : m.get(colour);
    }

    /** Joins two points of a Daisy line: conductor to conductor, a terminal to a conductor, or two terminals directly. */
    private static void connect(Object a, Object b, Union union, List<Object[]> contacts, java.util.function.BiConsumer<Conductor, Conductor> edge) {
        if (a instanceof Conductor ca && b instanceof Conductor cb) edge.accept(ca, cb);
        else if (a instanceof Conductor ca) contacts.add(new Object[]{b, ca});
        else if (b instanceof Conductor cb) contacts.add(new Object[]{a, cb});
        else union.join(keyOf((BINode) a), keyOf((BINode) b));
    }

    /** One key per run however it's walked into: both end conductors and both next to them, sorted. */
    private static String keyOf(Conductor start, Conductor firstStep, Conductor end, Conductor lastStep) {
        String a = start.id() + ":" + firstStep.id(), b = end.id() + ":" + lastStep.id();
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    Set<Object> keys() {
        Set<Object> keys = new HashSet<>();
        for (Member m : members) keys.add(new DeviceKey(m.node()));
        for (Wick w : wicks) keys.add(w.pos());
        for (Placed c : placed) keys.add(c.pos());
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
                case SOURCE -> h = (h * 31 + (n.sourceReady() ? 1 : 0)) * 31 + Double.hashCode(n.emf()) * 7 + Double.hashCode(n.maxAmps());
                case LOAD -> h = h * 31 + Double.hashCode(n.demandWatts());
                // an LED's current to the nearest 1/32 of full white, so a light show doesn't force a solve every tick
                case LED -> h = h * 31 + Math.round(n.ledAmps() / (3 * com.malevolentgods.fourtwenty.lights.LightsConfig.led().channelAmps()) * 32);
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
        ledIndex = new int[members.size()];
        java.util.Arrays.fill(sourceIndex, -1);
        java.util.Arrays.fill(loadIndex, -1);
        java.util.Arrays.fill(ledIndex, -1);
        var led = com.malevolentgods.fourtwenty.lights.LightsConfig.led();
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
        for (Member m : members) if (m.node().role() == BINode.Role.SOURCE && !Double.isNaN(m.node().emf())) nominal = Math.max(nominal, m.node().emf());
        for (int i = 0; i < members.size(); i++) {
            BINode n = members.get(i).node();
            int plus = members.get(i).plus(), minus = members.get(i).minus();
            if (n.overloaded(now)) continue;
            BIRules.Tier tier = n.tier();
            double volts = rules.volts(tier);
            switch (n.role()) {
                case SOURCE -> {
                    if (!n.sourceReady()) continue;
                    double watts = n.sourceWatts(), emf = Double.isNaN(n.emf()) ? volts : n.emf();
                    double limit = Double.isNaN(n.maxAmps()) ? rules.sourceLimit() * watts / volts : n.maxAmps();
                    sourceIndex[i] = circuit.source(plus, minus, emf, rules.sourceDroop() * emf / Math.max(limit, 1e-9) * (Double.isNaN(n.maxAmps()) ? rules.sourceLimit() : 1), limit, 0);
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
                case LED -> ledIndex[i] = circuit.led(plus, minus, n.ledAmps(), led.forwardVolts(), led.ratedVolts(), led.channelAmps());
                default -> {}
            }
        }
        // each Hit Box: one transformer, turns ratio its two tiers' voltages, losing its share at a Hookah's power on its primary
        windingOf = new int[members.size()];
        java.util.Arrays.fill(windingOf, -1);
        for (BINode[] pair : couplings) {
            int hi = indexOf(pair[0]), lo = indexOf(pair[1]);
            if (hi < 0 || lo < 0 || pair[0].overloaded(now) || pair[1].overloaded(now)) continue;
            BIRules.Tier high = pair[0].tier(), low = pair[1].tier();
            double ratio = rules.volts(high) / rules.volts(low);
            int t = circuit.transformer(members.get(hi).plus(), members.get(hi).minus(), members.get(lo).plus(), members.get(lo).minus(), ratio,
                    rules.hitBoxLoss() * rules.baseOhms(high));
            windingOf[hi] = t;
            windingOf[lo] = t;
        }
        solution = circuit.solve();
        status = new int[members.size()];
        for (int i = 0; i < members.size(); i++) {
            status[i] = loadIndex[i] >= 0 ? solution.loadStatus()[loadIndex[i]] : ledIndex[i] >= 0 ? solution.ledStatus()[ledIndex[i]] : Circuit.OK;
        }
        // each Lit Bit hears how lit it is: how far up its knee the voltage across it puts it
        for (int i = 0; i < members.size(); i++) {
            if (ledIndex[i] < 0) continue;
            double across = memberVolts(i);
            members.get(i).node().lit(status[i] == Circuit.OK && !members.get(i).node().overloaded(now) ? glow(i) : 0, status[i]);
        }
        floor = 0;
        for (int i = 0; i < solution.volts().length; i++) floor = i == 0 ? solution.volts()[0] : Math.min(floor, solution.volts()[i]);
    }

    /** Runs this network's tick once per game tick: solve if anything changed, move the Ws, check overloads and Wicks. */
    void tick(Level level, Map<BlockPos, Integer> burnTimers) {
        long now = level.getGameTime();
        if (tickedAt == now) return;
        tickedAt = now;
        if (!placed.isEmpty()) {
            tickLive(level, now);
            move(now);
            checks(level, now, burnTimers);
            return;
        }
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

    // ---- electronics: a network with components runs in time, every tick (docs/electronics/audit-and-plan.md 3.3) ----------------

    private static final double OPEN_SHARE = 1e-4;
    /** All the circuits with components on a server share this much time a tick; past it, the rest wait for the next tick. */
    static final long LIVE_BUDGET_NANOS = 20_000_000;
    private static long liveSpent;
    /** The most ticks in a row a circuit waits for the shared budget before it runs anyway. */
    static final int MAX_WAIT = 5;
    private int liveWaited;

    /** A new server tick: the electronics' shared time starts again. */
    public static void startTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) { liveSpent = 0; }
    private com.malevolentgods.fourtwenty.domain.electronics.Netlist live;
    private long liveSignature = Long.MIN_VALUE;
    private final List<com.malevolentgods.fourtwenty.domain.electronics.Part> liveParts = new ArrayList<>();
    private final List<com.malevolentgods.fourtwenty.domain.electronics.Parts.Resistor> liveRuns = new ArrayList<>();
    private final List<com.malevolentgods.fourtwenty.domain.electronics.Parts.LimitedSource> liveSources = new ArrayList<>();
    private final List<double[]> liveSourceOhms = new ArrayList<>();
    private final List<com.malevolentgods.fourtwenty.domain.electronics.Parts.PowerLoad> liveLoads = new ArrayList<>();
    private final List<com.malevolentgods.fourtwenty.domain.electronics.Parts.RgbLed> liveLeds = new ArrayList<>();
    private final List<com.malevolentgods.fourtwenty.domain.electronics.Parts.Transformer> liveWindings = new ArrayList<>();
    private final List<Double> liveWindingOhms = new ArrayList<>();

    /** One tick of a network with components: build it if its machines changed, then run it for a tick of time, logic and all. */
    private void tickLive(Level level, long now) {
        long sig = signature(now);
        if (live == null || sig != liveSignature) {
            buildLive(level, now);
            liveSignature = sig;
        }
        for (int i = 0; i < placed.size(); i++) {
            if (liveParts.get(i) instanceof com.malevolentgods.fourtwenty.domain.electronics.Parts.Switch sw
                    && level.getBlockEntity(placed.get(i).pos()) instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity be) {
                live.set(sw, be.closed());
            }
        }
        // over the shared budget: this circuit waits a tick (it runs slow, the server doesn't); but none waits more than a few ticks in a
        // row, or the circuits that happen to come first each tick could starve the rest for good
        if (liveSpent > LIVE_BUDGET_NANOS && solution != null && ++liveWaited < MAX_WAIT) return;
        liveWaited = 0;
        long started = System.nanoTime();
        boolean ok = live.run(1.0 / BIRules.TICKS_PER_SECOND);
        liveSpent += System.nanoTime() - started;
        for (int i = 0; i < placed.size(); i++) {
            var part = liveParts.get(i);
            BlockPos pos = placed.get(i).pos();
            if (part == null || !(level.getBlockEntity(pos) instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity be)) continue;
            be.memory(part.save(live.time()));
            Boolean high = null;
            if (part instanceof com.malevolentgods.fourtwenty.domain.electronics.Logic l) high = l.output() == com.malevolentgods.fourtwenty.domain.electronics.Logic.Out.HIGH;
            // a Hype Amp shows its output above half its supply; a FET shows its channel conducting (a milli-Ig or more)
            else if (part instanceof com.malevolentgods.fourtwenty.domain.electronics.Active.OpAmp) {
                int[] p = placed.get(i).pins();
                double supply = live.volts(p[0]) - live.volts(p[1]);
                high = supply >= com.malevolentgods.fourtwenty.domain.electronics.Logic.MIN_SUPPLY && live.volts(p[2]) - live.volts(p[1]) > supply / 2;
            } else if (part instanceof com.malevolentgods.fourtwenty.domain.electronics.Active.Mosfet) high = Math.abs(live.current(part)) >= 1e-3;
            else if (part instanceof com.malevolentgods.fourtwenty.domain.electronics.Chips.Chip chip) high = chip.anyHigh();
            if (high != null) {
                var state = level.getBlockState(pos);
                if (state.hasProperty(com.malevolentgods.fourtwenty.electronics.ComponentBlock.ON)
                        && state.getValue(com.malevolentgods.fourtwenty.electronics.ComponentBlock.ON) != high) {
                    level.setBlock(pos, state.setValue(com.malevolentgods.fourtwenty.electronics.ComponentBlock.ON, high), 2);
                }
            }
        }
        solution = liveSolution(ok);
        var led = com.malevolentgods.fourtwenty.lights.LightsConfig.led();
        status = new int[members.size()];
        for (int i = 0; i < members.size(); i++) {
            status[i] = loadIndex[i] >= 0 ? solution.loadStatus()[loadIndex[i]] : ledIndex[i] >= 0 ? solution.ledStatus()[ledIndex[i]] : Circuit.OK;
        }
        for (int i = 0; i < members.size(); i++) {
            if (ledIndex[i] < 0) continue;
            members.get(i).node().lit(status[i] == Circuit.OK && !members.get(i).node().overloaded(now) ? glow(i) : 0, status[i]);
        }
        floor = 0;
        for (int i = 0; i < solution.volts().length; i++) floor = i == 0 ? solution.volts()[0] : Math.min(floor, solution.volts()[i]);
    }

    /** The live circuit: the machines as solve() makes them, the Wicks, the Hit Boxes, and every component, given back its memory. */
    private void buildLive(Level level, long now) {
        CircuitRules.Numbers rules = ProcessingConfig.circuit();
        var net = new com.malevolentgods.fourtwenty.domain.electronics.Netlist();
        for (int i = 0; i < nodes; i++) net.node();
        liveRuns.clear();
        liveSources.clear();
        liveSourceOhms.clear();
        liveLoads.clear();
        liveLeds.clear();
        liveWindings.clear();
        liveWindingOhms.clear();
        liveParts.clear();
        for (Run r : runs) liveRuns.add(net.resistor(r.start(), r.end(), r.ohms()));
        sourceIndex = new int[members.size()];
        loadIndex = new int[members.size()];
        ledIndex = new int[members.size()];
        java.util.Arrays.fill(sourceIndex, -1);
        java.util.Arrays.fill(loadIndex, -1);
        java.util.Arrays.fill(ledIndex, -1);
        var led = com.malevolentgods.fourtwenty.lights.LightsConfig.led();
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
        for (Member m : members) if (m.node().role() == BINode.Role.SOURCE && !Double.isNaN(m.node().emf())) nominal = Math.max(nominal, m.node().emf());
        for (int i = 0; i < members.size(); i++) {
            BINode n = members.get(i).node();
            int plus = members.get(i).plus(), minus = members.get(i).minus();
            if (n.overloaded(now)) continue;
            BIRules.Tier tier = n.tier();
            double volts = rules.volts(tier);
            switch (n.role()) {
                case SOURCE -> {
                    if (!n.sourceReady()) continue;
                    double watts = n.sourceWatts(), emf = Double.isNaN(n.emf()) ? volts : n.emf();
                    double limit = Double.isNaN(n.maxAmps()) ? rules.sourceLimit() * watts / volts : n.maxAmps();
                    double ohms = rules.sourceDroop() * emf / Math.max(limit, 1e-9) * (Double.isNaN(n.maxAmps()) ? rules.sourceLimit() : 1);
                    sourceIndex[i] = liveSources.size();
                    liveSources.add(net.limitedSource(plus, minus, emf, ohms, limit, 0));
                    liveSourceOhms.add(new double[]{ohms});
                }
                case BATTERY -> {
                    double amps = rules.stashAmps(tier), ohms = rules.stashOhms(tier);
                    sourceIndex[i] = liveSources.size();
                    liveSources.add(net.limitedSource(plus, minus, rules.stashEmf(tier, n.fill()), ohms, n.stored() > 0 ? amps : 0, n.stored() < n.capacity() ? amps : 0));
                    liveSourceOhms.add(new double[]{ohms});
                }
                case LOAD -> {
                    double watts = n.demandWatts();
                    if (watts > 0) {
                        loadIndex[i] = liveLoads.size();
                        liveLoads.add(net.powerLoad(plus, minus, watts, rules.minVolts(tier)));
                    }
                }
                case LED -> {
                    ledIndex[i] = liveLeds.size();
                    liveLeds.add(net.rgbLed(plus, minus, n.ledAmps() / led.channelAmps(), led.forwardVolts(), led.ratedVolts(), led.channelAmps()));
                }
                default -> {}
            }
        }
        windingOf = new int[members.size()];
        java.util.Arrays.fill(windingOf, -1);
        for (BINode[] pair : couplings) {
            int hi = indexOf(pair[0]), lo = indexOf(pair[1]);
            if (hi < 0 || lo < 0 || pair[0].overloaded(now) || pair[1].overloaded(now)) continue;
            BIRules.Tier high = pair[0].tier(), low = pair[1].tier();
            double ohms = rules.hitBoxLoss() * rules.baseOhms(high);
            windingOf[hi] = liveWindings.size();
            windingOf[lo] = liveWindings.size();
            liveWindings.add(net.transformer(members.get(hi).plus(), members.get(hi).minus(), members.get(lo).plus(), members.get(lo).minus(),
                    rules.volts(high) / rules.volts(low), ohms));
            liveWindingOhms.add(ohms);
        }
        Map<com.malevolentgods.fourtwenty.domain.electronics.Part, double[]> memory = new IdentityHashMap<>();
        boolean reactive = false;
        for (Placed c : placed) {
            var be = level.getBlockEntity(c.pos()) instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity b ? b : null;
            double value = be == null ? c.type().defaultValue : be.value();
            int[] p = c.pins();
            com.malevolentgods.fourtwenty.domain.electronics.Part part = switch (c.type().part) {
                case RESISTOR -> net.resistor(p[0], p[1], value);
                case CAPACITOR -> net.capacitor(p[0], p[1], value);
                case INDUCTOR -> net.inductor(p[0], p[1], value);
                case DIODE -> net.diode(p[0], p[1], com.malevolentgods.fourtwenty.domain.electronics.Parts.DiodeModel.SILICON);
                case ZENER -> net.diode(p[0], p[1], com.malevolentgods.fourtwenty.domain.electronics.Parts.DiodeModel.zener(value));
                case TOGGLE, REDSTONE_SWITCH, BUTTON -> net.switchPart(p[0], p[1], be != null && be.closed());
                case GATE -> net.gate(c.type().gate, p[0], p[1], p[2], java.util.Arrays.copyOfRange(p, 3, p.length));
                case TRI_STATE -> net.triState(p[0], p[1], p[2], p[3], p[4]);
                case LATCH -> net.latch(p[0], p[1], p[2], p[3], p[4]);
                case FLIP_FLOP -> net.flipFlop(p[0], p[1], p[2], p[3], p[4]);
                case TICKER -> net.ticker(p[0], p[1], p[2], value);
                case NMOS -> net.mosfet(p[1], p[2], p[0], false);
                case PMOS -> net.mosfet(p[1], p[2], p[0], true);
                case OPAMP -> net.opAmp(p[0], p[1], p[2], p[3], p[4]);
                case SHIFT_OUT -> net.puffPass(p[0], p[1], p[2], p[3], p[4], java.util.Arrays.copyOfRange(p, 5, 13), p[13]);
                case SHIFT_IN -> net.pack(p[0], p[1], p[2], p[3], java.util.Arrays.copyOfRange(p, 4, 12), p[13]);
                case MUX -> net.sampler(p[0], p[1], java.util.Arrays.copyOfRange(p, 2, 10), java.util.Arrays.copyOfRange(p, 11, 14), p[20]);
                case BITWISE -> net.busLogic(com.malevolentgods.fourtwenty.domain.electronics.Chips.Op.values()[(int) Math.round(value) & 7], p[0], p[1],
                        java.util.Arrays.copyOfRange(p, 2, 11), java.util.Arrays.copyOfRange(p, 11, 20), java.util.Arrays.copyOfRange(p, 20, 29));
                case ARITH -> net.adder(value > 0.5, p[0], p[1], java.util.Arrays.copyOfRange(p, 2, 10), java.util.Arrays.copyOfRange(p, 11, 19), p[20],
                        java.util.Arrays.copyOfRange(p, 21, 29), p[29]);
                case COMPARE -> net.comparator(p[0], p[1], java.util.Arrays.copyOfRange(p, 2, 10), java.util.Arrays.copyOfRange(p, 11, 19), p[20], p[21], p[22]);
                case REGISTER -> net.register(p[0], p[1], java.util.Arrays.copyOfRange(p, 2, 11), p[11], p[12], java.util.Arrays.copyOfRange(p, 13, 22));
                case DEMUX -> net.dealer(p[0], p[1], p[2], java.util.Arrays.copyOfRange(p, 3, 6), java.util.Arrays.copyOfRange(p, 12, 20));
            };
            reactive |= c.type().part == com.malevolentgods.fourtwenty.electronics.ComponentType.Part.CAPACITOR
                    || c.type().part == com.malevolentgods.fourtwenty.electronics.ComponentType.Part.INDUCTOR;
            liveParts.add(part);
            if (be != null && be.memory().length > 0) memory.put(part, be.memory());
        }
        net.tolerance(1e-9);
        net.stepLimit(0.25 * Math.max(nominal, 1));
        net.eventBudget(2000);
        if (reactive) net.maxStep(1.0 / BIRules.TICKS_PER_SECOND / 10);
        net.resume(memory);
        live = net;
    }

    /** The live circuit's state, in the shape the rest of this class reads (Circuit.Solution). */
    private Circuit.Solution liveSolution(boolean converged) {
        var an = live.analysis();
        double[] v = new double[nodes];
        for (int i = 0; i < nodes; i++) v[i] = live.volts(i);
        double losses = 0;
        double[] cAmps = new double[liveRuns.size()];
        for (int i = 0; i < cAmps.length; i++) {
            cAmps[i] = live.current(liveRuns.get(i));
            losses += cAmps[i] * cAmps[i] * runs.get(i).ohms();
        }
        double[] sAmps = new double[liveSources.size()], sWatts = new double[liveSources.size()];
        for (int k = 0; k < sAmps.length; k++) {
            var src = liveSources.get(k);
            int[] n = src.nodes();
            double across = live.volts(n[0]) - live.volts(n[1]);
            sAmps[k] = live.current(src);
            double ohms = liveSourceOhms.get(k)[0];
            sWatts[k] = across * sAmps[k] + sAmps[k] * sAmps[k] * ohms;
            losses += sAmps[k] * sAmps[k] * ohms;
        }
        double scale = Math.max(nominal, 1e-9);
        double[] lWatts = new double[liveLoads.size()], lAmps = new double[liveLoads.size()];
        int[] lStatus = new int[liveLoads.size()];
        for (int k = 0; k < lWatts.length; k++) {
            var load = liveLoads.get(k);
            int[] n = load.nodes();
            double across = live.volts(n[0]) - live.volts(n[1]);
            lWatts[k] = load.wattsAt(across);
            lAmps[k] = load.ampsAt(across);
            lStatus[k] = across < -OPEN_SHARE * scale ? Circuit.REVERSED : across <= OPEN_SHARE * scale ? Circuit.NO_LOOP : Circuit.OK;
        }
        double[] ledAmps = new double[liveLeds.size()];
        int[] ledStatus = new int[liveLeds.size()];
        for (int k = 0; k < ledAmps.length; k++) {
            var l = liveLeds.get(k);
            int[] n = l.nodes();
            double across = live.volts(n[0]) - live.volts(n[1]);
            ledAmps[k] = l.ampsAt(across);
            ledStatus[k] = across < -OPEN_SHARE * scale ? Circuit.REVERSED : across <= OPEN_SHARE * scale ? Circuit.NO_LOOP : Circuit.OK;
        }
        double[] tAmps = new double[liveWindings.size()];
        for (int k = 0; k < tAmps.length; k++) {
            tAmps[k] = live.current(liveWindings.get(k));
            losses += tAmps[k] * tAmps[k] * liveWindingOhms.get(k);
        }
        return new Circuit.Solution(v, cAmps, sAmps, sWatts, lWatts, lAmps, losses, 1, converged, lStatus, ledAmps, ledStatus, tAmps);
    }

    /** A component's pin voltages and its part, for the Inspoweedometer; null if it isn't on this network. */
    double[] pinVolts(BlockPos pos) {
        for (int i = 0; i < placed.size(); i++) {
            if (!placed.get(i).pos().equals(pos) || live == null) continue;
            int[] pins = placed.get(i).pins();
            double[] out = new double[pins.length];
            for (int k = 0; k < pins.length; k++) out[k] = live.volts(pins[k]);
            return out;
        }
        return null;
    }

    /** The current through a component (first pin to second; a logic part's supply current). */
    double componentAmps(BlockPos pos) {
        for (int i = 0; i < placed.size(); i++) {
            if (placed.get(i).pos().equals(pos) && live != null && liveParts.get(i) != null) return live.current(liveParts.get(i));
        }
        return 0;
    }

    private void checks(Level level, long now, Map<BlockPos, Integer> burnTimers) {
        CircuitRules.Numbers rules = ProcessingConfig.circuit();
        for (int i = 0; i < members.size(); i++) {
            if (Math.abs(memberVolts(i)) > rules.overloadVolts(members.get(i).node().tier())) members.get(i).node().overload(now);
        }
        // a block burns once however many of its cores are over: both cores of a cable share its timer
        Map<BlockPos, Double> overVolts = new LinkedHashMap<>();
        Map<BlockPos, BIRules.Tier> tierAt = new HashMap<>();
        boolean glow = now % GLOW == 0;
        for (Wick w : wicks) {
            double amps = amps(w), volts = volts(w);
            tierAt.put(w.pos(), w.tier());
            boolean over = amps > rules.wickAmps(w.tier()) * w.ampsScale() || volts > rules.wickVolts(w.tier());
            if (over) overVolts.merge(w.pos(), volts, (x, y) -> Double.isNaN(x) ? y : Math.max(x, y));
            else overVolts.putIfAbsent(w.pos(), Double.NaN);
            if (glow && amps > 1e-6 * rules.baseAmps(w.tier())) WickBlock.carry(level, w.pos());
        }
        List<BlockPos> burn = new ArrayList<>();
        for (var e : overVolts.entrySet()) {
            BlockPos at = e.getKey();
            if (Double.isNaN(e.getValue())) {
                burnTimers.remove(at);
                continue;
            }
            int ticks = burnTimers.merge(at, 1, Integer::sum);
            if (CircuitRules.burns(ticks, rules.burnTicks())) {
                burn.add(at);
                BIRules.Tier tier = tierAt.get(at);
                if (e.getValue() > rules.wickVolts(tier)) com.mojang.logging.LogUtils.getLogger().info(
                        "FourTwenty: a {} Wick at {} burned out on {} Kb, over its insulation", tier.getSerializedName(), at.toShortString(), Math.round(e.getValue() * 100) / 100.0);
            }
        }
        // a Resinator past its power rating burns out like a Wick (docs/electronics/audit-and-plan.md 3.2)
        for (Placed c : placed) {
            double rating = c.type().ratedWatts();
            if (rating <= 0 || live == null) continue;
            double ohms = level.getBlockEntity(c.pos()) instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity be ? be.value() : c.type().defaultValue;
            double amps = componentAmps(c.pos()), watts = amps * amps * ohms;
            if (watts > rating) {
                if (CircuitRules.burns(burnTimers.merge(c.pos(), 1, Integer::sum), rules.burnTicks())) burn.add(c.pos());
            } else {
                burnTimers.remove(c.pos());
            }
        }
        for (BlockPos pos : burn) {
            burnTimers.remove(pos);
            var b = level.getBlockState(pos).getBlock();
            if (b instanceof WickBlock || b instanceof CableBlock || b instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlock) BurnOut.burn(level, pos);
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

    /** How lit a Lit Bit is: the current its diodes really carry, against what its colour asks for at full supply. */
    private double glow(int i) {
        double want = members.get(i).node().ledAmps();
        return want <= 0 ? 0 : Math.max(0, Math.min(1, solution.ledAmps()[ledIndex[i]] / want));
    }

    /** A member's circuit status: Circuit.OK, BROWNOUT, NO_LOOP or REVERSED. */
    int status(int i) { return i >= 0 && i < status.length ? status[i] : Circuit.NO_LOOP; }

    /** Current into the circuit from a member: positive out of a source, negative into a load or a charging battery. */
    double memberAmps(int i) {
        if (solution == null) return 0;
        if (i < windingOf.length && windingOf[i] >= 0) {
            // a winding: the primary takes its current in, the secondary gives n times it out
            double primary = solution.transformerAmps()[windingOf[i]];
            BINode n = members.get(i).node();
            if (n.primary()) return -primary;
            BIRules.Tier high = n.coupled().tier(), low = n.tier();
            var rules = ProcessingConfig.circuit();
            return primary * rules.volts(high) / rules.volts(low);
        }
        if (sourceIndex[i] >= 0) return solution.sourceAmps()[sourceIndex[i]];
        if (loadIndex[i] >= 0) return -solution.loadAmps()[loadIndex[i]];
        if (ledIndex != null && ledIndex[i] >= 0) return -solution.ledAmps()[ledIndex[i]];
        return 0;
    }

    double memberWatts(int i) {
        if (solution == null) return 0;
        if (i < windingOf.length && windingOf[i] >= 0) return memberAmps(i) * memberVolts(i);
        if (sourceIndex[i] >= 0) return solution.sourceWatts()[sourceIndex[i]];
        if (loadIndex[i] >= 0) return -solution.loadWatts()[loadIndex[i]];
        if (ledIndex != null && ledIndex[i] >= 0) return -solution.ledAmps()[ledIndex[i]] * memberVolts(i);
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
        return a * a * rules.wickOhms(w.tier()) * w.ohmsScale();
    }

    double nominal() { return nominal; }

    boolean solved() { return solution != null; }

    /** What everything on it but {@code except} asks for, in Willards, without solving: loads' demand, and room in batteries. */
    double demandBeyond(BINode except) {
        // electronics draw power too: a network with components wants its source lit
        double total = placed.isEmpty() ? 0 : 1;
        for (Member m : members) {
            BINode n = m.node();
            if (n == Terminal.owner(except)) continue;
            // an LED takes no Ws into a store, but a lamp set to a colour still wants its source lit
            // (by its current, not its role: a Hit Box's role asks for this network's demand in turn)
            if (n.ledAmps() > 0) {
                total += n.ledAmps() * Math.max(nominal, 1);
                continue;
            }
            if (!n.canReceive()) continue;
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
