package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.processing.MachineBlockEntity;
import com.malevolentgods.fourtwenty.processing.MachineMenu;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/** Every powered trimming and curing machine: inputs, the work arrow, outputs, and the BI gauge. */
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

    @Override protected String helpKey() { return menu.trims() ? "trim" : "cure"; }

    @Override protected SlotHint hint(int slot) {
        if (slot >= MachineBlockEntity.SIZE) return null;
        boolean trims = menu.trims();
        if (slot < MachineBlockEntity.INPUTS) {
            return new SlotHint(new ItemStack(trims ? ModItems.ROUGH_BUD.get() : ModItems.TRIMMED_BUD.get()),
                    Component.translatable(trims ? "gui.fourtwenty.slot.rough" : "gui.fourtwenty.slot.trimmed"));
        }
        return new SlotHint(new ItemStack(trims ? ModItems.TRIMMED_BUD.get() : ModItems.WEED_BUD.get()),
                Component.translatable(trims ? "gui.fourtwenty.slot.trim_out" : "gui.fourtwenty.slot.cure_out"));
    }

    @Override protected void tips(GuiGraphics g, int mouseX, int mouseY) {
        gaugeTip(g, mouseX, mouseY, GAUGE_X, GAUGE_Y, menu.stored(), menu.capacity(), Component.translatable("gui.fourtwenty.gauge.machine"));
        int percent = menu.total() <= 0 ? 0 : 100 * menu.progress() / menu.total();
        arrowTip(g, mouseX, mouseY, ARROW_X, ARROW_Y, Component.translatable(menu.paused() ? "gui.fourtwenty.arrow.paused" : "gui.fourtwenty.arrow.working", percent));
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        if (menu.paused() && !menu.overloaded()) g.drawString(font, Component.translatable("gui.fourtwenty.machine.no_power"), 8, 72, WARN, false);
    }
}
