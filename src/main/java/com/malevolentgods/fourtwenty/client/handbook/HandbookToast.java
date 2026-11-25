package com.malevolentgods.fourtwenty.client.handbook;

import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * A Handbook notice in the book's own look: a slip of the page's paper with an ink rule, the badge's patch (or the Handbook) as its
 * icon, a green heading and the wording wrapped to fit. Fixed width, so a long requirement wraps instead of stretching the toast
 * (vanilla's system toast tiles its background sideways for long text, which some resource packs draw as a broken strip).
 */
public final class HandbookToast implements Toast {
    private static final int WIDTH = 160, PAD = 6, ICON = 20, TEXT_X = PAD + ICON + 6, LINE = 10, MAX_LINES = 3;
    private static final long SHOW_MS = 5000;
    private static final int PAPER = 0xFFF3E6C4, EDGE = 0xFF6B5330, SHADE = 0xFFE2D2A8;

    private final Component title;
    private final List<FormattedCharSequence> lines;
    private final @Nullable ResourceLocation patch;

    /** {@code badge} is the badge id whose patch to show, or null for the Handbook itself. */
    public HandbookToast(Component title, Component message, @Nullable String badge) {
        this.title = title;
        var font = Minecraft.getInstance().font;
        List<FormattedCharSequence> wrapped = font.split(message, WIDTH - TEXT_X - PAD);
        this.lines = wrapped.size() > MAX_LINES ? wrapped.subList(0, MAX_LINES) : wrapped;
        this.patch = badge == null ? null : ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/handbook/patches/" + badge + ".png");
    }

    @Override public int width() { return WIDTH; }

    @Override public int height() { return Math.max(32, PAD * 2 + LINE + lines.size() * LINE); }

    @Override
    public @Nonnull Visibility render(@Nonnull GuiGraphics g, @Nonnull ToastComponent toasts, long shown) {
        int h = height();
        g.fill(0, 0, WIDTH, h, EDGE);
        g.fill(1, 1, WIDTH - 1, h - 1, PAPER);
        g.fill(1, h - 3, WIDTH - 1, h - 1, SHADE);
        int iy = (h - ICON) / 2;
        if (patch != null) {
            g.blit(patch, PAD, iy, ICON, ICON, 0, 0, 128, 128, 128, 128);
        } else {
            g.renderFakeItem(new ItemStack(ModItems.HANDBOOK.get()), PAD + 2, iy + 2);
        }
        var font = toasts.getMinecraft().font;
        g.drawString(font, title, TEXT_X, PAD, PageRenderer.HEADING, false);
        int y = PAD + LINE + 1;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, TEXT_X, y, PageRenderer.INK, false);
            y += LINE;
        }
        return shown >= SHOW_MS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
