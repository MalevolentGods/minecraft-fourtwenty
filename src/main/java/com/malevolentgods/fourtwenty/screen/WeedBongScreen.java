package com.malevolentgods.fourtwenty.screen;

import com.malevolentgods.fourtwenty.client.TechScreen;
import com.malevolentgods.fourtwenty.menu.WeedBongMenu;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;

/** The Hookah and its upgrades: buds, water and coal, the burn, and the BI it's holding. */
public class WeedBongScreen extends TechScreen<WeedBongMenu> {
    private static final int GAUGE_X = 158, GAUGE_Y = 16, ARROW_X = 104, ARROW_Y = 40, LIGHT_X = 104, LIGHT_Y = 58, LIGHT_W = 44, LIGHT_H = 16;

    private Button light;

    public WeedBongScreen(WeedBongMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected void init() {
        super.init();
        light = addRenderableWidget(Button.builder(Component.translatable("gui.fourtwenty.hookah.light"),
                        button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WeedBongMenu.LIGHT))
                .bounds(leftPos + LIGHT_X, topPos + LIGHT_Y, LIGHT_W, LIGHT_H)
                .tooltip(Tooltip.create(Component.translatable("gui.fourtwenty.hookah.light.tip"))).build());
    }

    @Override protected void containerTick() {
        super.containerTick();
        light.visible = !menu.lit();
        light.active = menu.canLight();
    }

    @Override protected int tier() { return menu.tier(); }

    @Override protected boolean overloaded() { return false; }

    @Override protected void details(GuiGraphics g) {
        arrow(g, leftPos + ARROW_X, topPos + ARROW_Y, 1, menu.lit() ? menu.burnLength() - menu.burnTime() : 0, menu.burnLength());
        gauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.stored(), menu.capacity(), 0, 0);
    }

    @Override protected String helpKey() { return "hookah"; }

    @Override protected SlotHint hint(int slot) {
        return switch (slot) {
            case 0 -> new SlotHint(new ItemStack(ModItems.WEED_BUD.get()), Component.translatable("gui.fourtwenty.slot.hookah_buds"));
            case 1 -> new SlotHint(new ItemStack(Items.WATER_BUCKET), Component.translatable("gui.fourtwenty.slot.hookah_water"));
            case 2 -> new SlotHint(new ItemStack(Items.CHARCOAL), Component.translatable("gui.fourtwenty.slot.hookah_charcoal"));
            default -> null;
        };
    }

    @Override protected void tips(GuiGraphics g, int mouseX, int mouseY) {
        gaugeTip(g, mouseX, mouseY, GAUGE_X, GAUGE_Y, menu.stored(), menu.capacity(), Component.translatable("gui.fourtwenty.gauge.hookah"));
        arrowTip(g, mouseX, mouseY, ARROW_X, ARROW_Y, menu.lit()
                ? Component.translatable("gui.fourtwenty.arrow.burning", Math.max(0, (menu.burnLength() - menu.burnTime()) / 20))
                : Component.translatable("gui.fourtwenty.arrow.unlit"));
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        if (menu.lit()) g.drawString(font, Component.translatable("gui.fourtwenty.hookah.burning"), 104, 60, GREEN, false);
    }
}
