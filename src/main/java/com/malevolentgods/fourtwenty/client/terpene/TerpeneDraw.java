package com.malevolentgods.fourtwenty.client.terpene;

import com.malevolentgods.fourtwenty.domain.TerpenePalette;
import com.malevolentgods.fourtwenty.domain.Terpenes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Drawing for the Terpinator screens: bevelled panels, inset cards and slots, arrows, buttons,
 * swatches and code chips, and text in the Terpinator's font (Atkinson Hyperlegible at 6x
 * oversampling, so it stays crisp at any GUI scale).
 */
public final class TerpeneDraw {
    private TerpeneDraw() {}

    public static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath("fourtwenty", "terpinator");
    public static final ResourceLocation FONT_BOLD = ResourceLocation.fromNamespaceAndPath("fourtwenty", "terpinator_bold");

    public static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADOW = 0xFF555555, EDGE = 0xFF000000, SLOT = 0xFF8B8B8B,
            SLOT_DARK = 0xFF373737, TEXT = 0xFF3A3A3A, MUTED = 0xFF686868, ACCENT = 0xFF8A5212, TRACK = 0xFF9A9A9A,
            CARD = 0xFFBDBDBD, CARD_EDGE = 0xFF9E9E9E, GOOD = 0xFF2F7A47, WARN = 0xFFA4501A;

    /** Text in the Terpinator font. */
    public static MutableComponent t(Component text) { return text.copy().withStyle(s -> s.withFont(FONT)); }

    public static MutableComponent t(String key, Object... args) { return t(Component.translatable(key, args)); }

    /** Bold text in the Terpinator font. */
    public static MutableComponent bold(Component text) { return text.copy().withStyle(s -> s.withFont(FONT_BOLD)); }

    public static int opaque(int rgb) { return 0xFF000000 | rgb; }

    /** A vanilla-style raised panel. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, EDGE);
        g.fill(x, y + 1, x + w, y + h - 1, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
    }

    /** A recessed card that groups one stage. */
    public static void card(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, CARD_EDGE);
        g.fill(x + 1, y + 1, x + w, y + h, LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, CARD);
    }

    /** An inset 18x18 slot whose item sits at (x, y). */
    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT);
    }

    /** A wider output slot (26x26) around an item at (x, y). */
    public static void bigSlot(GuiGraphics g, int x, int y) {
        g.fill(x - 5, y - 5, x + 21, y + 21, SLOT_DARK);
        g.fill(x - 4, y - 4, x + 21, y + 21, LIGHT);
        g.fill(x - 4, y - 4, x + 20, y + 20, SLOT);
    }

    /** A progress arrow from x0 to x1 (centre line at y), filled by {@code done} (0..1) in {@code color}. */
    public static void arrow(GuiGraphics g, int x0, int x1, int y, float done, int color) {
        int head = 6;
        g.fill(x0, y - 2, x1 - head, y + 2, TRACK);
        for (int i = 0; i < head; i++) g.fill(x1 - head + i, y - head + i + 1, x1 - head + i + 1, y + head - i, TRACK);
        int fill = Math.round((x1 - x0) * Math.max(0, Math.min(1, done)));
        if (fill <= 0) return;
        g.fill(x0, y - 2, Math.min(x0 + fill, x1 - head), y + 2, color);
        for (int i = 0; i < head && x1 - head + i < x0 + fill; i++) g.fill(x1 - head + i, y - head + i + 1, x1 - head + i + 1, y + head - i, color);
    }

    /**
     * A Start/Stop button: a play triangle or a stop square and its label. {@code enabled = false}
     * greys it out; {@code on} lights it in green.
     */
    public static void runButton(GuiGraphics g, Font font, int x, int y, int w, int h, boolean on, boolean enabled, boolean hover) {
        int face = !enabled ? 0xFFA8A8A8 : on ? 0xFF3E8E57 : hover ? 0xFFD8D8D8 : 0xFFCFCFCF;
        g.fill(x, y, x + w, y + h, 0xFF202020);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, on ? 0xFF5DB176 : 0xFFF0F0F0);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, on ? 0xFF2A6A3E : 0xFF8A8A8A);
        int ink = !enabled ? 0xFF7A7A7A : on ? 0xFFFFFFFF : TEXT;
        int ix = x + 6, iy = y + h / 2;
        if (on) {
            g.fill(ix, iy - 3, ix + 6, iy + 3, ink);                           // stop square
        } else {
            for (int i = 0; i < 4; i++) g.fill(ix + 1 + i, iy - 3 + i, ix + 2 + i, iy + 4 - i, ink);      // play triangle
        }
        Component label = t(on ? "gui.fourtwenty.terpinator.stop" : "gui.fourtwenty.terpinator.start");
        g.drawString(font, label, x + 16 + (w - 20 - font.width(label)) / 2, y + (h - 8) / 2, ink, false);
    }

    /** A colour swatch with a dark edge and a soft highlight, like the codex. */
    public static void swatch(GuiGraphics g, int x, int y, int w, int h, int rgb) {
        g.fill(x, y, x + w, y + h, 0xFF202020);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, opaque(rgb));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, opaque(mix(rgb, 0xFFFFFF, 0.25f)));
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, opaque(mix(rgb, 0x000000, 0.2f)));
    }

    /** An undiscovered swatch: the family's colour, muted and hatched, so it reads as "somewhere here". */
    public static void smudge(GuiGraphics g, int x, int y, int w, int h, int rgb) {
        int muted = mix(mix(rgb, 0x8A8A8A, 0.65f), 0x000000, 0.1f);
        g.fill(x, y, x + w, y + h, 0xFF303030);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, opaque(muted));
        int dark = opaque(mix(muted, 0x000000, 0.12f));
        for (int i = -h; i < w; i += 3) {
            for (int j = 0; j < h - 2; j++) {
                int px = x + 1 + i + j;
                if (px >= x + 1 && px < x + w - 1) g.fill(px, y + 1 + j, px + 1, y + 2 + j, dark);
            }
        }
    }

    /** A base's abbreviation on its colour, as in the codex; returns its width. */
    public static int chip(GuiGraphics g, Font font, Terpenes.Base base, int x, int y) {
        Component text = t("terpene.fourtwenty." + base.code() + ".abbr");
        int w = font.width(text) + 5;
        g.fill(x, y, x + w, y + 10, opaque(TerpenePalette.color(Terpenes.Code.of(base))));
        g.drawString(font, text, x + 3, y + 1, 0xFFFFFFFF, false);
        return w;
    }

    /** A full code as chips; returns the width. */
    public static int chips(GuiGraphics g, Font font, Terpenes.Code code, int x, int y) {
        int at = x;
        for (Terpenes.Base b : code.letters()) at += chip(g, font, b, at, y) + 1;
        return at - x - 1;
    }

    /** A pulsing outline, for the guided first session. */
    public static void pulse(GuiGraphics g, int x, int y, int w, int h) {
        float t = (float) (Math.sin(System.currentTimeMillis() / 220.0) * 0.5 + 0.5);
        int a = (int) (90 + 140 * t) << 24;
        int c = a | 0xF2C14E;
        g.fill(x - 2, y - 2, x + w + 2, y - 1, c);
        g.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, c);
        g.fill(x - 2, y - 1, x - 1, y + h + 1, c);
        g.fill(x + w + 1, y - 1, x + w + 2, y + h + 1, c);
    }

    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int gr = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (gr << 8) | bl;
    }
}
