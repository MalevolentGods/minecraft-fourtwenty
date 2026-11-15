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
 * she's doing about her next joint. A small two-line badge in the empty right-hand column of the page, under the
 * happiness picture and above the seal: the state on top, the detail under it (a line between happiness and "Skills"
 * had no room: the happiness faces sit lower than the layout says).
 */
public final class HighStatusClient {
    private HighStatusClient() {}

    private record Status(HighStatusPayload payload, long receivedAt) {}

    private record Badge(WeakReference<Text> text, Text detail, Gradient border, Gradient fill, int entity) {}

    private static final Map<Integer, Status> STATUS = new HashMap<>();
    private static final List<Badge> BADGES = new ArrayList<>();
    private static final String ID = "fourtwentyHigh";

    /** The right-hand column of the citizen main page, under the happiness picture and above the seal. */
    private static final int X = 140, Y = 134, WIDTH = 62, HEIGHT = 22;
    private static final double SCALE = 0.75, DETAIL_SCALE = 0.6;

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
        var detail = new Text();
        detail.setTextScale(DETAIL_SCALE);
        detail.setTextAlignment(Alignment.MIDDLE);
        detail.setColors(0xF2EEDD);
        border.setPosition(X - 1, Y - 1);
        border.setSize(WIDTH + 2, HEIGHT + 2);
        border.setGradientStart(0x2B, 0x22, 0x18, 0xFF);
        border.setGradientEnd(0x2B, 0x22, 0x18, 0xFF);
        fill.setPosition(X, Y);
        fill.setSize(WIDTH, HEIGHT);
        text.setPosition(X, Y + 2);
        text.setSize(WIDTH, 9);
        detail.setPosition(X, Y + 12);
        detail.setSize(WIDTH, 8);
        window.addChild(border);
        window.addChild(fill);
        window.addChild(text);
        window.addChild(detail);
        PaneBuilders.tooltipBuilder().hoverPane(fill).build().setText(Component.translatable("fourtwenty.high.tooltip"));
        var badge = new Badge(new WeakReference<>(text), detail, border, fill, citizen.getEntityId());
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
        badge.detail().setVisible(shown);
        badge.border().setVisible(shown);
        badge.fill().setVisible(shown);
        if (!shown) return;
        var line = line(status);
        text.setText(line.text());
        badge.detail().setText(line.detail());
        int c = line.colour();
        badge.fill().setGradientStart((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 0xFF);
        badge.fill().setGradientEnd(((c >> 16) & 0xFF) * 3 / 4, ((c >> 8) & 0xFF) * 3 / 4, (c & 0xFF) * 3 / 4, 0xFF);
    }

    private record Line(Component text, Component detail, int colour) {}

    private static Line line(String key, Component detail, int colour) {
        return new Line(Component.translatable("fourtwenty.high.badge." + key), detail, colour);
    }

    private static Line line(Status s) {
        var p = s.payload();
        return switch (p.state()) {
            case HighStatusPayload.LIFTED -> {
                long left = Math.max(0, p.ticksLeft() - (p.counting() ? now() - s.receivedAt() : 0));
                long secs = left / 20;
                String time = (secs / 60) + ":" + String.format("%02d", secs % 60);
                yield line(p.counting() ? "lifted" : "paused", Component.translatable("fourtwenty.high.detail.time",
                        Component.translatable(p.strainKey()), time), p.counting() ? GREEN : MUTED_GREEN);
            }
            case HighStatusPayload.SMOKE_BREAK -> line("smoke", Component.translatable("fourtwenty.high.detail.smoke"), AMBER);
            case HighStatusPayload.FLYING_HOME -> line("home", Component.translatable("fourtwenty.high.detail.home"), SLATE);
            case HighStatusPayload.SOBER -> p.joints() > 0 ? line("sober", Component.translatable("fourtwenty.high.detail.joints", p.joints()), GREY)
                    : p.asking() ? line("sober", Component.translatable("fourtwenty.high.detail.asking"), AMBER)
                    : !p.spare() ? line("sober", Component.translatable("fourtwenty.high.detail.nospare"), RED)
                    : line("sober", Component.empty(), GREY);
            default -> line("off", Component.translatable("fourtwenty.high.detail.off"), GREY);
        };
    }
}
