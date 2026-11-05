package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.block.WeedBongBlock;
import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.Terminals;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * One terminal of a powered block, + or −: what its BI capability gives for that face. It stands for its owner (the block's node) in
 * everything a caller asks, and tells the circuit which side of the owner it is. Each owner has exactly one of each, so they compare by
 * identity.
 */
public final class Terminal implements BINode {
    private static final Map<BINode, Terminal[]> PAIRS = new WeakHashMap<>();
    private final BINode owner;
    private final Terminals.Sign sign;

    private Terminal(BINode owner, Terminals.Sign sign) {
        this.owner = owner;
        this.sign = sign;
    }

    public static synchronized Terminal of(BINode owner, Terminals.Sign sign) {
        Terminal[] pair = PAIRS.computeIfAbsent(owner, o -> new Terminal[]{new Terminal(o, Terminals.Sign.PLUS), new Terminal(o, Terminals.Sign.MINUS)});
        return pair[sign.ordinal()];
    }

    /** The node behind a capability: a terminal's owner, or the node itself. */
    public static BINode owner(BINode node) { return node instanceof Terminal t ? t.owner : node; }

    public BINode owner() { return owner; }

    public Terminals.Sign sign() { return sign; }

    /** The way a powered block's model faces. A Hookah takes its facing from where its placer looked, so its front is the other way. */
    public static Direction front(BlockState state) {
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        return state.getBlock() instanceof WeedBongBlock ? facing.getOpposite() : facing;
    }

    /** Whether {@code face} of a powered block in {@code state} is a terminal, from the block state alone (for drawing Wick plugs). */
    public static boolean isTerminalFace(BlockState state, Direction face) {
        Direction front = front(state);
        if (Terminals.at(front, face) != null) return true;
        return state.getBlock() instanceof HitBoxBlock && Terminals.highWinding(front, face) != null;
    }

    /** What an ordinary powered block's capability gives on {@code side}: its + or − terminal, or nothing. */
    public static @Nullable BINode side(BlockEntity entity, BINode node, @Nullable Direction side) {
        if (node == null) return null;
        Terminals.Sign sign = Terminals.at(front(entity.getBlockState()), side);
        return sign == null ? null : of(node, sign);
    }

    @Override public BIRules.Tier tier() { return owner.tier(); }
    @Override public long stored() { return owner.stored(); }
    @Override public long capacity() { return owner.capacity(); }
    @Override public long receive(long amount) { return owner.receive(amount); }
    @Override public long extract(long amount) { return owner.extract(amount); }
    @Override public boolean canReceive() { return owner.canReceive(); }
    @Override public boolean canExtract() { return owner.canExtract(); }
    @Override public void overload(long now) { owner.overload(now); }
    @Override public boolean overloaded(long now) { return owner.overloaded(now); }
    @Override public Role role() { return owner.role(); }
    @Override public double demandWatts() { return owner.demandWatts(); }
    @Override public double sourceWatts() { return owner.sourceWatts(); }
    @Override public boolean sourceReady() { return owner.sourceReady(); }
    @Override public double efficiency() { return owner.efficiency(); }
    @Override public double fill() { return owner.fill(); }
}
