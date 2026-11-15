package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.Alignment;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Gradient;
import com.ldtteam.blockui.controls.Text;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.HighStatusPayload;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
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
 * The badge on a Builder's citizen window that says how high she is (the user, 2026-11-14): Lifted on what and for
 * how long (counting down live, paused while she isn't building), having a smoke, flying home, or not high and what
 * she's doing about her next joint. A small coloured pill centred in the gap between her happiness and "Skills" (a
 * plain line there ran into the "Skills" heading).
 */
public final class HighStatusClient {
    private HighStatusClient() {}

    private record Status(HighStatusPayload payload, long receivedAt) {}

    private record Badge(WeakReference<Text> text, Gradient border, Gradient fill, int entity) {}

    private static final Map<Integer, Status> STATUS = new HashMap<>();
    private static final List<Badge> BADGES = new ArrayList<>();
    private static final String ID = "fourtwentyHigh";

    /** The gap on the citizen main page: happiness ends at y 86, "Skills" starts at y 104; the page is 210 wide. */
    private static final int CENTRE_X = 105, Y = 89, HEIGHT = 11;
    private static final double SCALE = 0.75;

    // fill colours, ARGB
    private static final int GREEN = 0xFF2E7D32, MUTED_GREEN = 0xFF5F8A62, AMBER = 0xFFB7791F, SLATE = 0xFF4A5A6A,
            GREY = 0xFF77736B, RED = 0xFFA33A2E;

    public static void install() {
        HighStatusPayload.clientHandler = p -> STATUS.put(p.entity(), new Status(p, now()));
        NeoForge.EVENT_BUS.addListener(HighStatusClient::tick);
    }

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    /** From the citizen window as it's built: Builders get the badge on their main page. */
    public static void attach(AbstractWindowSkeleton window, ICitizenDataView citizen) {
        if (!(window instanceof MainWindowCitizen) || citizen.getJob() == null || !citizen.getJob().contains("builder")) return;
        if (window.findPaneByID(ID) != null) return;
        var border = new Gradient();
        var fill = new Gradient();
        var text = new Text();
        text.setID(ID);
        text.setTextScale(SCALE);
        text.setTextAlignment(Alignment.MIDDLE);
        text.setColors(0xFFFFFF);
        window.addChild(border);
        window.addChild(fill);
        window.addChild(text);
        PaneBuilders.tooltipBuilder().hoverPane(text).build().setText(Component.translatable("fourtwenty.high.tooltip"));
        var badge = new Badge(new WeakReference<>(text), border, fill, citizen.getEntityId());
        BADGES.add(badge);
        update(badge);
    }

    private static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            STATUS.clear();
            BADGES.clear();
            return;
        }
        BADGES.removeIf(b -> b.text().get() == null);
        BADGES.forEach(HighStatusClient::update);
        if (BADGES.size() > 32) BADGES.subList(0, BADGES.size() - 32).clear();   // old windows' badges, long closed
    }

    private static void update(Badge badge) {
        Text text = badge.text().get();
        if (text == null) return;
        var status = STATUS.get(badge.entity());
        boolean shown = status != null;
        text.setVisible(shown);
        badge.border().setVisible(shown);
        badge.fill().setVisible(shown);
        if (!shown) return;
        var line = line(status);
        int width = (int) Math.ceil(Minecraft.getInstance().font.width(line.text()) * SCALE) + 10;
        int x = CENTRE_X - width / 2;
        text.setText(line.text());
        text.setPosition(x, Y + 1);
        text.setSize(width, HEIGHT - 1);
        badge.border().setPosition(x - 1, Y - 1);
        badge.border().setSize(width + 2, HEIGHT + 2);
        badge.border().setGradientStart(0x2B, 0x22, 0x18, 0xFF);
        badge.border().setGradientEnd(0x2B, 0x22, 0x18, 0xFF);
        int c = line.colour();
        badge.fill().setPosition(x, Y);
        badge.fill().setSize(width, HEIGHT);
        badge.fill().setGradientStart((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 0xFF);
        badge.fill().setGradientEnd(((c >> 16) & 0xFF) * 3 / 4, ((c >> 8) & 0xFF) * 3 / 4, (c & 0xFF) * 3 / 4, 0xFF);
    }

    private record Line(Component text, int colour) {}

    private static Line line(Status s) {
        var p = s.payload();
        return switch (p.state()) {
            case HighStatusPayload.LIFTED -> {
                long left = Math.max(0, p.ticksLeft() - (p.counting() ? now() - s.receivedAt() : 0));
                long secs = left / 20;
                String time = (secs / 60) + ":" + String.format("%02d", secs % 60);
                yield new Line(Component.translatable(p.counting() ? "fourtwenty.high.lifted" : "fourtwenty.high.paused",
                        Component.translatable(p.strainKey()), time), p.counting() ? GREEN : MUTED_GREEN);
            }
            case HighStatusPayload.SMOKE_BREAK -> new Line(Component.translatable("fourtwenty.high.smoke"), AMBER);
            case HighStatusPayload.FLYING_HOME -> new Line(Component.translatable("fourtwenty.high.home"), SLATE);
            case HighStatusPayload.SOBER -> p.joints() > 0 ? new Line(Component.translatable("fourtwenty.high.joints", p.joints()), GREY)
                    : p.asking() ? new Line(Component.translatable("fourtwenty.high.asking"), AMBER)
                    : !p.spare() ? new Line(Component.translatable("fourtwenty.high.nospare"), RED)
                    : new Line(Component.translatable("fourtwenty.high.sober"), GREY);
            default -> new Line(Component.translatable("fourtwenty.high.off"), GREY);
        };
    }
}
