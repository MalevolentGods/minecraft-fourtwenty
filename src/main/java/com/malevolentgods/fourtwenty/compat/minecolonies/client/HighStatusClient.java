package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Text;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.HighStatusPayload;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The line on a Builder's citizen window that says how high she is (the user, 2026-11-14): Lifted on what and for how
 * long (counting down live, paused while she isn't building), on a smoke break, flying home, or not high and what she's
 * doing about her next joint. In the free strip between her happiness bar and her skills.
 */
public final class HighStatusClient {
    private HighStatusClient() {}

    private record Status(HighStatusPayload payload, long receivedAt) {}

    private record Line(WeakReference<Text> text, int entity) {}

    private static final Map<Integer, Status> STATUS = new HashMap<>();
    private static final List<Line> LINES = new ArrayList<>();
    private static final String ID = "fourtwentyHigh";

    public static void install() {
        HighStatusPayload.clientHandler = p -> STATUS.put(p.entity(), new Status(p, now()));
        NeoForge.EVENT_BUS.addListener(HighStatusClient::tick);
    }

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    /** From the citizen window as it's built: Builders get the line on their main page. */
    public static void attach(AbstractWindowSkeleton window, ICitizenDataView citizen) {
        if (!(window instanceof MainWindowCitizen) || citizen.getJob() == null || !citizen.getJob().contains("builder")) return;
        if (window.findPaneByID(ID) != null) return;
        var text = new Text();
        text.setID(ID);
        text.setPosition(33, 99);
        text.setSize(170, 11);
        text.setColors(0x2B2B2B);
        window.addChild(text);
        PaneBuilders.tooltipBuilder().hoverPane(text).build().setText(Component.translatable("fourtwenty.high.tooltip"));
        LINES.add(new Line(new WeakReference<>(text), citizen.getEntityId()));
        update(text, citizen.getEntityId());
    }

    private static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            STATUS.clear();
            LINES.clear();
            return;
        }
        var it = LINES.iterator();
        while (it.hasNext()) {
            var line = it.next();
            Text text = line.text().get();
            if (text == null) {
                it.remove();
                continue;
            }
            update(text, line.entity());
        }
        if (LINES.size() > 32) LINES.subList(0, LINES.size() - 32).clear();   // old windows' lines, long closed
    }

    private static void update(Text text, int entity) {
        var status = STATUS.get(entity);
        text.setText(status == null ? Component.empty() : line(status));
    }

    private static Component line(Status s) {
        var p = s.payload();
        return switch (p.state()) {
            case HighStatusPayload.LIFTED -> {
                long left = Math.max(0, p.ticksLeft() - (p.counting() ? now() - s.receivedAt() : 0));
                long secs = left / 20;
                String time = (secs / 60) + ":" + String.format("%02d", secs % 60);
                yield Component.translatable(p.counting() ? "fourtwenty.high.lifted" : "fourtwenty.high.paused",
                        Component.translatable(p.strainKey()), time).withStyle(ChatFormatting.DARK_GREEN);
            }
            case HighStatusPayload.SMOKE_BREAK -> Component.translatable("fourtwenty.high.smoke").withStyle(ChatFormatting.DARK_GREEN);
            case HighStatusPayload.FLYING_HOME -> Component.translatable("fourtwenty.high.home");
            case HighStatusPayload.SOBER -> p.joints() > 0 ? Component.translatable("fourtwenty.high.joints", p.joints())
                    : p.asking() ? Component.translatable("fourtwenty.high.asking")
                    : !p.spare() ? Component.translatable("fourtwenty.high.nospare").withStyle(ChatFormatting.DARK_RED)
                    : Component.translatable("fourtwenty.high.sober");
            default -> Component.translatable("fourtwenty.high.off");
        };
    }
}
