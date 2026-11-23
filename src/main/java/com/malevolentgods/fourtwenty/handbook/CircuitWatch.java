package com.malevolentgods.fourtwenty.handbook;

import com.malevolentgods.fourtwenty.electronics.ComponentBlock;
import com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity;
import com.malevolentgods.fourtwenty.electronics.ComponentType;
import com.malevolentgods.fourtwenty.lights.LitBitBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notices the Handbook's six example circuits working (specs/016-weedcraft-handbook research R9): not by matching a layout, but by their
 * behaviour. Once a second it looks at the parts around each player and keeps what it has seen each part do; when a part has shown both
 * halves of what the circuit is for, the circuit counts. A deed {@code circuit} with {@code id} light, inverter, and_lock, latch, clock
 * or adder.
 */
@EventBusSubscriber(modid = "fourtwenty")
public final class CircuitWatch {
    private CircuitWatch() {}

    private static final int RADIUS = 7, SWITCH_REACH = 2;
    private static final int OPEN = 1, CLOSED = 2, ON = 4, OFF = 8, BOTH_ON = 16, BOTH_OFF = 32, MIXED_OFF = 64, SUM_OFF_CARRY_ON = 128, STARTED = 256;

    /** What each watched part has been seen doing, by position and player. */
    private static final Map<UUID, Map<BlockPos, int[]>> SEEN = new HashMap<>();

    private static final Map<UUID, List<BlockPos>> WATCHED = new HashMap<>();

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        Map<BlockPos, int[]> seen = SEEN.computeIfAbsent(player.getUUID(), u -> new HashMap<>());
        BlockPos at = player.blockPosition();
        if (player.tickCount % 20 == 7) {
            List<BlockPos> bits = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-RADIUS, -3, -RADIUS), at.offset(RADIUS, 4, RADIUS))) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof ComponentBlock cb) {
                    switch (cb.type()) {
                        case NOT_NUG, AND_NUG, LATCH_NUG, TICKER -> bits.add(pos.immutable());
                        default -> {}
                    }
                } else if (state.getBlock() instanceof LitBitBlock && state.getValue(LitBitBlock.LEVEL) > 0 && nearSwitch(level, pos, 3, true)) {
                    Deeds.fire(player, "circuit", "id", "light");
                }
            }
            WATCHED.put(player.getUUID(), bits);
            seen.keySet().removeIf(p -> p.distSqr(at) > (RADIUS + 12) * (RADIUS + 12));
        }
        // sampled every 7 ticks, so a one-second clock is seen in both its states
        if (player.tickCount % 7 == 0) {
            for (BlockPos pos : WATCHED.getOrDefault(player.getUUID(), List.of())) watch(player, level, seen, pos);
        }
    }

    @SubscribeEvent
    public static void gone(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SEEN.remove(event.getEntity().getUUID());
        WATCHED.remove(event.getEntity().getUUID());
    }

    private static List<ComponentBlockEntity> switchesNear(ServerLevel level, BlockPos pos, int reach) {
        List<ComponentBlockEntity> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-reach, -reach, -reach), pos.offset(reach, reach, reach))) {
            if (level.getBlockState(p).getBlock() instanceof ComponentBlock cb
                    && (cb.type() == ComponentType.TOGGLE_JOINT || cb.type() == ComponentType.PUFF_BUTTON)
                    && level.getBlockEntity(p) instanceof ComponentBlockEntity be) out.add(be);
        }
        return out;
    }

    private static boolean nearSwitch(ServerLevel level, BlockPos pos, int reach, boolean wantClosed) {
        for (var be : switchesNear(level, pos, reach)) if (be.closed() == wantClosed) return true;
        return false;
    }

    private static void watch(ServerPlayer player, ServerLevel level, Map<BlockPos, int[]> seen, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ComponentBlock cb)) return;
        boolean on = state.getValue(ComponentBlock.ON);
        int[] mask = seen.computeIfAbsent(pos, p -> new int[]{0, 0});     // [flags, edge count]
        var switches = switchesNear(level, pos, SWITCH_REACH);
        long closed = switches.stream().filter(ComponentBlockEntity::closed).count();
        switch (cb.type()) {
            case NOT_NUG -> {
                if (switches.isEmpty()) return;
                if (closed == 0 && on) mask[0] |= OPEN | ON;
                if (closed > 0 && !on) mask[0] |= CLOSED | OFF;
                if ((mask[0] & (OPEN | ON)) == (OPEN | ON) && (mask[0] & (CLOSED | OFF)) == (CLOSED | OFF)) Deeds.fire(player, "circuit", "id", "inverter");
            }
            case AND_NUG -> {
                if (switches.size() < 2) return;
                if (closed == switches.size() && on) mask[0] |= BOTH_ON;
                if (closed > 0 && closed < switches.size() && !on) mask[0] |= MIXED_OFF;
                if ((mask[0] & BOTH_ON) != 0 && (mask[0] & MIXED_OFF) != 0) Deeds.fire(player, "circuit", "id", "and_lock");
                // the carry of an adder: an AND with an XOR beside it
                if (closed == switches.size() && on && xorBeside(level, pos, false)) mask[0] |= SUM_OFF_CARRY_ON;
                if ((mask[0] & SUM_OFF_CARRY_ON) != 0) Deeds.fire(player, "circuit", "id", "adder");
            }
            case LATCH_NUG -> {
                if (switches.isEmpty()) return;
                if (!on) mask[0] |= OFF;
                if (on && closed == 0 && (mask[0] & OFF) != 0) Deeds.fire(player, "circuit", "id", "latch");
            }
            case TICKER -> {
                boolean started = (mask[0] & STARTED) != 0, was = (mask[0] & ON) != 0;
                if (!started) mask[0] |= STARTED | (on ? ON : 0);
                else if (was != on) {
                    mask[1]++;
                    mask[0] = (mask[0] & ~ON) | (on ? ON : 0);
                }
                if (mask[1] >= 4) Deeds.fire(player, "circuit", "id", "clock");
            }
            default -> {}
        }
    }

    /** Whether an XOR Nug sits within three blocks of {@code pos} with its output {@code lit}. */
    private static boolean xorBeside(ServerLevel level, BlockPos pos, boolean lit) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof ComponentBlock cb && cb.type() == ComponentType.XOR_NUG && s.getValue(ComponentBlock.ON) == lit) return true;
        }
        return false;
    }
}
