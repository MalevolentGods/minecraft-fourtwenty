package com.malevolentgods.fourtwenty.screen;

import com.malevolentgods.fourtwenty.client.TechScreen;
import com.malevolentgods.fourtwenty.menu.WeedBongMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/** The Hookah and its upgrades: buds, water and coal, the burn, and the Inspo it's holding. */
public class WeedBongScreen extends TechScreen<WeedBongMenu> {
    private static final int GAUGE_X = 158, GAUGE_Y = 16, ARROW_X = 104, ARROW_Y = 40;

    public WeedBongScreen(WeedBongMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected int tier() { return menu.tier(); }

    @Override protected boolean overloaded() { return false; }

    @Override protected void details(GuiGraphics g) {
        arrow(g, leftPos + ARROW_X, topPos + ARROW_Y, 1, menu.lit() ? menu.burnLength() - menu.burnTime() : 0, menu.burnLength());
        gauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.stored(), menu.capacity(), 0, 0);
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        if (menu.lit()) g.drawString(font, Component.translatable("gui.fourtwenty.hookah.burning"), 104, 60, GREEN, false);
    }
}
