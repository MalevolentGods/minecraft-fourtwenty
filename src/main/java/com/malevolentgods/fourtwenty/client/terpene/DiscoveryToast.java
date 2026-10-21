package com.malevolentgods.fourtwenty.client.terpene;

import com.malevolentgods.fourtwenty.domain.TerpenePalette;
import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

/** "Terpene discovered": the item, its name in its colour, and its flavour line. */
public class DiscoveryToast implements Toast {
    private static final int WIDTH = 200, SHOWN_FOR = 7000, MAX_LINES = 4;
    private final ItemStack icon;
    private final Component name;
    private final int color;
    /** The flavour line, wrapped now: the toast list asks for the height before the first render. */
    private final List<FormattedCharSequence> wrapped = new java.util.ArrayList<>();

    public DiscoveryToast(String key) {
        if (TerpeneKnowledge.RESIN.equals(key)) {
            icon = new ItemStack(ModTerpenes.RESIN.get());
            name = Component.translatable("item.fourtwenty.sticky_situation");
            color = 0xC4801C;
        } else {
            Terpenes.Code code = Terpenes.Code.parse(key).orElseThrow();
            icon = new ItemStack(ModTerpenes.terpene(code));
            name = Component.translatable("item.fourtwenty.terpene_" + key);
            color = TerpenePalette.color(code);
        }
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        for (Component verse : com.malevolentgods.fourtwenty.terpene.TerpeneItem.flavour(key, 200)) wrapped.addAll(font.split(verse, WIDTH - 34));
    }

    @Override public int width() { return WIDTH; }

    @Override public int height() { return 26 + Math.min(MAX_LINES, wrapped.size()) * 9; }

    @Override public @Nonnull Visibility render(@Nonnull GuiGraphics g, @Nonnull ToastComponent toasts, long time) {
        Font font = toasts.getMinecraft().font;
        int h = height();
        g.fill(0, 0, WIDTH, h, 0xF0101410);
        g.fill(0, 0, 3, h, TerpeneDraw.opaque(color));
        g.renderFakeItem(icon, 8, 5);
        g.drawString(font, Component.translatable("gui.fourtwenty.toast.discovered"), 30, 4, 0xFFB8B8A8, false);
        g.drawString(font, name, 30, 14, TerpeneDraw.opaque(TerpeneDraw.mix(color, 0xFFFFFF, 0.2f)), false);
        for (int i = 0; i < Math.min(MAX_LINES, wrapped.size()); i++) g.drawString(font, wrapped.get(i), 30, 25 + i * 9, 0xFF9FC8C0, false);
        return time >= SHOWN_FOR * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
