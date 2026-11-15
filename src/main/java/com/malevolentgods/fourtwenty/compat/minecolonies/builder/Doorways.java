package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.malevolentgods.fourtwenty.domain.FlightPlanner;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A Lifted Builder flies through doors (specs/015-lifted-body US2, FR-003, FR-004): wooden doors (any a hand can open)
 * and fence gates count as doorways for her routes; she opens one as she reaches it and closes what she opened once
 * she's past and nothing stands in it. Nothing else is ever opened, and a door someone else opened is left as it is.
 */
public final class Doorways {
    private Doorways() {}

    /** How close she is when she opens a door ahead of her. */
    private static final double OPEN_AT = 1.75;
    /** How far past she is when she closes it. */
    private static final double CLOSE_PAST = 2.0;

    /** Per Builder, the doors and gates she opened and hasn't closed yet (the lower half of a door). */
    private static final Map<UUID, Set<BlockPos>> OPENED = new HashMap<>();

    public static FlightPlanner.Doorway of(ServerLevel level) {
        return (x, y, z) -> isDoorway(level, new BlockPos(x, y, z));
    }

    public static boolean isDoorway(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) return door.type().canOpenByHand();
        return state.getBlock() instanceof FenceGateBlock;
    }

    /** Open the doorways on the next few cells of her route that she's come up to. */
    public static void openAhead(ServerLevel level, EntityCitizen citizen, List<FlightPlanner.Cell> route, int next) {
        for (int i = Math.max(0, next - 1); i < Math.min(route.size(), next + 3); i++) {
            var cell = route.get(i);
            for (int dy = 0; dy <= 1; dy++) {
                var pos = new BlockPos(cell.x(), cell.y() + dy, cell.z());
                if (!isDoorway(level, pos)) continue;
                if (citizen.position().distanceTo(Vec3.atCenterOf(pos)) > OPEN_AT + 1) continue;
                open(level, citizen, pos);
            }
        }
    }

    private static void open(ServerLevel level, EntityCitizen citizen, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            BlockState lowerState = level.getBlockState(lower);
            if (!(lowerState.getBlock() instanceof DoorBlock) || lowerState.getValue(DoorBlock.OPEN)) return;
            door.setOpen(citizen, level, lowerState, lower, true);
            OPENED.computeIfAbsent(citizen.getUUID(), id -> new HashSet<>()).add(lower);
        } else if (state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN)) {
            level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, true), 10);
            level.playSound(null, pos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1, 1);
            level.gameEvent(citizen, GameEvent.BLOCK_OPEN, pos);
            OPENED.computeIfAbsent(citizen.getUUID(), id -> new HashSet<>()).add(pos);
        }
    }

    /** Every tick: close what each Builder opened once she's past it and nothing stands in it. */
    public static void tick(ServerLevel level) {
        if (OPENED.isEmpty()) return;
        var it = OPENED.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var entity = level.getEntity(entry.getKey());
            entry.getValue().removeIf(pos -> {
                if (!level.isLoaded(pos)) return false;
                if (entity != null && entity.isAlive() && entity.position().distanceTo(Vec3.atCenterOf(pos)) < CLOSE_PAST) return false;
                if (!level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(pos).expandTowards(0, 1, 0)).isEmpty()) return false;
                close(level, entity, pos);
                return true;
            });
            if (entry.getValue().isEmpty()) it.remove();
        }
    }

    private static void close(ServerLevel level, net.minecraft.world.entity.Entity by, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            if (state.getValue(DoorBlock.OPEN)) door.setOpen(by, level, state, pos, false);
        } else if (state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN)) {
            level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, false), 10);
            level.playSound(null, pos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1, 1);
            level.gameEvent(by, GameEvent.BLOCK_CLOSE, pos);
        }
    }
}
