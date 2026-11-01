package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.processing.ConstructionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;

import javax.annotation.Nonnull;

/** The crafting table's screen, recipe book and all, with a line saying what the bench is doing. */
public class ConstructionScreen extends CraftingScreen {
    private static final int READY = 0x3F7A2A, WAITING = 0x9A5A10, STATUS_X = 90;

    public ConstructionScreen(CraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (!(menu instanceof ConstructionMenu bench)) return;
        Component status = bench.overloaded() ? Component.translatable("gui.fourtwenty.construction.overloaded")
                : bench.ready() ? Component.translatable("gui.fourtwenty.construction.ready", bench.factor()) : Component.translatable("gui.fourtwenty.machine.waiting");
        g.drawString(font, status, STATUS_X, inventoryLabelY, bench.ready() ? READY : WAITING, false);
    }
}
