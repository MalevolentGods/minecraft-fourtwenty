package com.malevolentgods.fourtwenty.lights;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lamp colours to players, in one batch per chunk per tick: every lamp that changed what it shows this tick goes out once, to the
 * players who can see that chunk and have our channel. Colours never rebuild a chunk's mesh: the client draws them (LitBitRenderer).
 */
public final class LampSync {
    private LampSync() {}

    private static final Map<Level, Map<ChunkPos, Map<BlockPos, Integer>>> PENDING = new WeakHashMap<>();

    static void queue(ServerLevel level, BlockPos pos, int rgb) {
        PENDING.computeIfAbsent(level, l -> new HashMap<>()).computeIfAbsent(new ChunkPos(pos), c -> new LinkedHashMap<>()).put(pos.immutable(), rgb);
    }

    public static void flush(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var chunks = PENDING.remove(level);
        if (chunks == null) return;
        chunks.forEach((chunk, lamps) -> {
            long[] at = new long[lamps.size()];
            int[] rgb = new int[lamps.size()];
            int i = 0;
            for (var e : lamps.entrySet()) {
                at[i] = e.getKey().asLong();
                rgb[i++] = e.getValue();
            }
            var payload = new LightsNetwork.LampColours(at, rgb);
            for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(chunk, false)) {
                if (player.connection.hasChannel(LightsNetwork.LampColours.TYPE)) PacketDistributor.sendToPlayer(player, payload);
            }
        });
    }
}
