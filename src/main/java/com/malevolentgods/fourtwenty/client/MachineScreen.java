package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.processing.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nonnull;

/** Every powered machine's screen, drawn in code in the Terpinator's colours: slots, a progress bar, and a line saying when it's waiting for power. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int PANEL = 0xFF23262B, EDGE = 0xFF5FA84A, SLOT = 0xFF14161A, SLOT_EDGE = 0xFF4A4F57, BAR = 0xFF7BD44F, TEXT = 0xFFE3E6DF, WARN = 0xFFE8B04A;
    private static final int BAR_X = 80, BAR_Y = 42, BAR_W = 14, BAR_H = 4;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MachineMenu.WIDTH;
        imageHeight = MachineMenu.HEIGHT;
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(@Nonnull GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, EDGE);
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT_EDGE);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT);
        }
        g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, SLOT);
        if (menu.total() > 0) g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W * Math.min(menu.progress(), menu.total()) / menu.total(), y + BAR_Y + BAR_H, BAR);
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        if (menu.paused()) g.drawString(font, Component.translatable("gui.fourtwenty.machine.no_power"), 8, 72, WARN, false);
    }
}
