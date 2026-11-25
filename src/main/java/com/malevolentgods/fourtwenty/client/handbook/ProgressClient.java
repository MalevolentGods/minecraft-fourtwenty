package com.malevolentgods.fourtwenty.client.handbook;

import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Progress;
import com.malevolentgods.fourtwenty.handbook.HandbookNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The reader's own progress through the ranks and badges, as the server last told them (specs/016-weedcraft-handbook FR-023), and the
 * small notices when a requirement ticks or a rank or badge is earned. Also what other players' sashes show (FR-036).
 */
public final class ProgressClient {
    private ProgressClient() {}

    private static Progress progress = new Progress();
    /** Entity id to the badge ids it has earned, oldest first, for the sash on whoever wears it. */
    private static final Map<Integer, List<String>> SASHES = new HashMap<>();

    public static Progress get() { return progress; }

    public static List<String> sash(int entityId) { return SASHES.getOrDefault(entityId, List.of()); }

    public static void install() {
        HandbookNetwork.clientProgress = ProgressClient::receive;
        HandbookNetwork.clientSash = p -> SASHES.put(p.entity(), p.badges());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            progress = new Progress();
            SASHES.clear();
        });
    }

    private static void receive(HandbookNetwork.ProgressPayload p) {
        Progress fresh = new Progress();
        fresh.restore(p.states(), p.counts(), p.earned());
        progress = fresh;
        if (p.full()) return;
        var mc = Minecraft.getInstance();
        List<String> ticks = p.ticked().stream().filter(k -> (k.startsWith("rank:") || k.startsWith("badge:")) && k.contains("/")).toList();
        Component heading = Component.translatable("handbook.fourtwenty.notice.tick");
        if (ticks.size() > MAX_TICK_NOTICES) {
            // a lot at once (a command, or one deed that counts for several): one notice, not a wall of them
            mc.getToasts().addToast(new HandbookToast(heading, Component.translatable("handbook.fourtwenty.notice.ticks", ticks.size()), null));
        } else {
            for (String key : ticks) mc.getToasts().addToast(new HandbookToast(heading, Component.translatable(reqKey(key)), badgeOf(key)));
        }
        for (String key : p.earnedNow()) {
            boolean rank = key.startsWith("rank:");
            String id = key.substring(key.indexOf(':') + 1);
            mc.getToasts().addToast(new HandbookToast(Component.translatable("handbook.fourtwenty.notice." + (rank ? "rank" : "badge")),
                    Component.translatable("handbook.fourtwenty." + (rank ? "rank" : "badge") + "." + id), rank ? null : id));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f));
        }
    }

    private static final int MAX_TICK_NOTICES = 3;

    /** The badge a requirement key belongs to ("badge:munchies/3" is munchies), or null for a rank's. */
    private static String badgeOf(String key) {
        return key.startsWith("badge:") ? key.substring(6, key.indexOf('/')) : null;
    }

    /** "rank:tenderbud/2a" to the lang key of its wording. */
    public static String reqKey(String key) {
        return "handbook.fourtwenty.req." + key.replace(':', '.').replace('/', '.');
    }

    public static void sign(String key) {
        if (Minecraft.getInstance().getConnection() != null) PacketDistributor.sendToServer(new HandbookNetwork.SignPayload(key));
    }
}
