package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.processing.RigBlock;
import com.malevolentgods.fourtwenty.processing.RigBlockEntity;
import com.malevolentgods.fourtwenty.processing.RigMenu;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;

/** The Rosin Rig: what goes in, the work arrow, the extracts, and the BI gauge. */
public class RigScreen extends TechScreen<RigMenu> {
    private static final int GAUGE_X = 158, GAUGE_Y = 16, ARROW_X = 70, ARROW_Y = 30, STATUS_Y = 72;

    public RigScreen(RigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = RigMenu.WIDTH;
        imageHeight = RigMenu.HEIGHT;
    }

    @Override protected int tier() { return RigBlock.TIER.ordinal(); }

    @Override protected boolean overloaded() { return menu.overloaded(); }

    @Override protected void details(GuiGraphics g) {
        arrow(g, leftPos + ARROW_X, topPos + ARROW_Y, 1, menu.progress(), menu.total());
        gauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.stored(), menu.capacity(), 0, 0);
    }

    @Override protected String helpKey() { return "rig"; }

    @Override protected SlotHint hint(int slot) {
        if (slot >= RigBlockEntity.SIZE) return null;
        if (slot == RigBlockEntity.INPUT) return new SlotHint(new ItemStack(ModItems.WEED_BUD.get()), Component.translatable("gui.fourtwenty.slot.rig_in"));
        if (slot == RigBlockEntity.BOTTLE) return new SlotHint(new ItemStack(Items.GLASS_BOTTLE), Component.translatable("gui.fourtwenty.slot.rig_bottle"));
        return new SlotHint(new ItemStack(ModPower.EXTRACTS.get(0).get()), Component.translatable("gui.fourtwenty.slot.rig_out"));
    }

    @Override protected void tips(GuiGraphics g, int mouseX, int mouseY) {
        gaugeTip(g, mouseX, mouseY, GAUGE_X, GAUGE_Y, menu.stored(), menu.capacity(), Component.translatable("gui.fourtwenty.gauge.machine"));
        int percent = menu.total() <= 0 ? 0 : 100 * menu.progress() / menu.total();
        arrowTip(g, mouseX, mouseY, ARROW_X, ARROW_Y, Component.translatable(menu.paused() ? "gui.fourtwenty.arrow.paused" : "gui.fourtwenty.arrow.working", percent));
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        if (menu.tubeBlocked()) g.drawString(font, Component.translatable("gui.fourtwenty.machine.tube_blocked"), 8, STATUS_Y, WARN, false);
        else if (menu.free()) g.drawString(font, Component.translatable("gui.fourtwenty.machine.free"), 8, STATUS_Y, GREEN, false);
        else if (menu.paused() && !menu.overloaded()) g.drawString(font, Component.translatable("gui.fourtwenty.machine.waiting"), 8, STATUS_Y, WARN, false);
    }
}
