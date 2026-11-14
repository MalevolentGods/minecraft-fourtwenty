package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.malevolentgods.fourtwenty.domain.AlbumClock;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The server side of the Midnight Machine (specs/014-midnight-machine FR-002 to FR-006). Each Builder's place in the
 * album moves on while she's Lifted and flying and the music is on, is saved with Lifted, and is sent to the players
 * around her on every start, stop and track change, to anyone who starts seeing her, and every 30 s as a drift check.
 */
public final class MidnightMachineService {
    private MidnightMachineService() {}

    private static final long RESYNC_TICKS = 600;

    private record Sent(boolean playing, boolean lifted, int track, long at) {}

    private static final Map<UUID, Sent> SENT = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MidnightMachineService::startTracking);
    }

    /** Once a sweep (every {@code elapsed} ticks) per Builder, from LiftedService. */
    public static void tick(ServerLevel level, IColony colony, EntityCitizen citizen, int elapsed) {
        var data = citizen.getCitizenData();
        if (data == null) return;
        boolean lifted = LiftedService.isLifted(citizen);
        boolean playing = lifted && BuilderConfig.musicEnabled() && LiftedFlight.inFlight(citizen);
        var store = Lifted.get(level);
        var pos = store.position(colony.getID(), data.getId());
        long[] lengths = Album.lengths();
        if (playing && lengths.length > 0) {
            pos = AlbumClock.advance(pos, elapsed * 50L, lengths);
            store.position(colony.getID(), data.getId(), pos);
        }
        var last = SENT.get(citizen.getUUID());
        long now = level.getGameTime();
        boolean changed = last == null || last.playing() != playing || last.lifted() != lifted || last.track() != pos.track();
        if (changed || playing && now - last.at() >= RESYNC_TICKS) {
            if (last == null && !lifted) return;   // nothing to say about a Builder who's never been Lifted
            MidnightMachinePayload.send(citizen, new MidnightMachinePayload(citizen.getId(), playing, lifted, pos.track(), pos.ms(), now));
            SENT.put(citizen.getUUID(), new Sent(playing, lifted, pos.track(), now));
        }
    }

    /** A player who starts seeing a Builder with her boombox on hears the album from where it is. */
    private static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer) || !(event.getTarget() instanceof EntityCitizen citizen)) return;
        var sent = SENT.get(citizen.getUUID());
        var data = citizen.getCitizenData();
        if (sent == null || !sent.lifted() || data == null || !(citizen.level() instanceof ServerLevel level)) return;
        var pos = Lifted.get(level).position(data.getColony().getID(), data.getId());
        MidnightMachinePayload.send(viewer, new MidnightMachinePayload(citizen.getId(), sent.playing(), true, pos.track(), pos.ms(), level.getGameTime()));
    }
}
