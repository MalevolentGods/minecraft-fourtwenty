package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.power.BIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/** A Stash or Hit Box: the gauge, the numbers, and on a Hit Box the switch between tiers. */
public class BIScreen extends TechScreen<BIMenu> {
    private static final int GAUGE_X = 14, GAUGE_Y = 20, TEXT_X = 38, BUTTON_W = 100, BUTTON_H = 18;

    private Button toggle;

    public BIScreen(BIMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 84;
    }

    @Override protected void init() {
        super.init();
        toggle = addRenderableWidget(new HelpScreen.TechButton(leftPos + TEXT_X, topPos + 56, BUTTON_W, BUTTON_H, Component.translatable("gui.fourtwenty.hit_box.switch"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BIMenu.BUTTON_TOGGLE)));
    }

    @Override protected int tier() { return menu.tier(); }

    @Override protected boolean overloaded() { return menu.overloaded(); }

    @Override protected void details(GuiGraphics g) {
        toggle.visible = menu.hitBox();
        gauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.stored(), menu.capacity(), 0, 0);
    }

    @Override protected String helpKey() { return menu.plot() ? "plot" : menu.hitBox() ? "hit_box" : "stash"; }

    @Override protected void tips(GuiGraphics g, int mouseX, int mouseY) {
        gaugeTip(g, mouseX, mouseY, GAUGE_X, GAUGE_Y, menu.stored(), menu.capacity(), Component.translatable(menu.plot() ? "gui.fourtwenty.gauge.machine" : menu.hitBox() ? "gui.fourtwenty.gauge.hit_box" : "gui.fourtwenty.gauge.stash"));
        if (menu.hitBox()) hover(g, mouseX, mouseY, TEXT_X, 56, BUTTON_W, BUTTON_H, Component.translatable("gui.fourtwenty.hit_box.switch.tip"));
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        BIRules.Tier tier = BIRules.Tier.values()[Math.max(0, Math.min(menu.tier(), BIRules.Tier.values().length - 1))];
        if (menu.plot()) {
            g.drawString(font, Component.translatable("gui.fourtwenty.plot.speed", com.malevolentgods.fourtwenty.processing.ProcessingConfig.plotSpeed(menu.plotLevel())), TEXT_X, 24, TEXT, false);
            g.drawString(font, Component.translatable(menu.plotLevel() == 1 ? "gui.fourtwenty.plot.unpowered" : "gui.fourtwenty.plot.powered"), TEXT_X, 36, TEXT, false);
        } else if (menu.hitBox()) {
            g.drawString(font, Component.translatable("gui.fourtwenty.hit_box.front", tierName(tier)), TEXT_X, 24, TEXT, false);
            g.drawString(font, Component.translatable("gui.fourtwenty.hit_box.sides", tierName(tier.below())), TEXT_X, 36, TEXT, false);
        } else {
            g.drawString(font, Component.translatable("gui.fourtwenty.power.stored", String.format("%,d", menu.stored())), TEXT_X, 24, TEXT, false);
            g.drawString(font, Component.translatable("gui.fourtwenty.power.of", String.format("%,d", menu.capacity())), TEXT_X, 36, TEXT, false);
            g.drawString(font, Component.translatable("gui.fourtwenty.power.tier", tierName(tier)), TEXT_X, 48, GREEN, false);
        }
    }

    private static Component tierName(BIRules.Tier tier) {
        return Component.translatable("tier.fourtwenty." + tier.getSerializedName());
    }
}
