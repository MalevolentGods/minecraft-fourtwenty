package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.processing.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/** Every powered trimming and curing machine: inputs, the work arrow, outputs, and the Inspo gauge. */
public class MachineScreen extends TechScreen<MachineMenu> {
    private static final int GAUGE_X = 158, GAUGE_Y = 16, ARROW_X = 70, ARROW_Y = 30;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MachineMenu.WIDTH;
        imageHeight = MachineMenu.HEIGHT;
    }

    @Override protected int tier() { return menu.tier(); }

    @Override protected boolean overloaded() { return menu.overloaded(); }

    @Override protected void details(GuiGraphics g) {
        arrow(g, leftPos + ARROW_X, topPos + ARROW_Y, menu.trims() ? 0 : 1, menu.progress(), menu.total());
        gauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.stored(), menu.capacity(), 0, 0);
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        if (menu.paused() && !menu.overloaded()) g.drawString(font, Component.translatable("gui.fourtwenty.machine.no_power"), 8, 72, WARN, false);
    }
}
