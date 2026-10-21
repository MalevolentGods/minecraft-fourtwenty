package com.malevolentgods.fourtwenty.client.terpene;

import com.malevolentgods.fourtwenty.domain.TerpenePalette;
import com.malevolentgods.fourtwenty.domain.Terpenes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Drawing for the Terpinator screens, in vanilla's GUI style: bevelled panels, inset slots, arrows, swatches, code chips. */
public final class TerpeneDraw {
    private TerpeneDraw() {}

    public static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADOW = 0xFF555555, EDGE = 0xFF000000, SLOT = 0xFF8B8B8B,
            SLOT_DARK = 0xFF373737, TEXT = 0xFF404040, MUTED = 0xFF6B6B6B, ACCENT = 0xFF8A5212, TRACK = 0xFF8B8B8B;

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
        Component text = Component.translatable("terpene.fourtwenty." + base.code() + ".abbr");
        int w = font.width(text) + 4;
        g.fill(x, y, x + w, y + 10, opaque(TerpenePalette.color(Terpenes.Code.of(base))));
        g.drawString(font, text, x + 2, y + 1, 0xFFFFFFFF, false);
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
