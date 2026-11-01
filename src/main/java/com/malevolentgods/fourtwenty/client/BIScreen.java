package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.power.BIMenu;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
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
        toggle = addRenderableWidget(new TechButton(leftPos + TEXT_X, topPos + 56, BUTTON_W, BUTTON_H, Component.translatable("gui.fourtwenty.hit_box.switch"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BIMenu.BUTTON_TOGGLE)));
    }

    @Override protected int tier() { return menu.tier(); }

    @Override protected boolean overloaded() { return menu.overloaded(); }

    @Override protected void details(GuiGraphics g) {
        toggle.visible = menu.hitBox();
        gauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.stored(), menu.capacity(), 0, 0);
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        BIRules.Tier tier = BIRules.Tier.values()[Math.max(0, Math.min(menu.tier(), 2))];
        if (menu.hitBox()) {
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

    /** A button in the tech style, with its own switch sound. */
    private static final class TechButton extends Button {
        TechButton(int x, int y, int width, int height, Component message, OnPress press) {
            super(x, y, width, height, message, press, DEFAULT_NARRATION);
        }

        @Override protected void renderWidget(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
            int edge = isHovered() ? GREEN : 0xFF5FA84A;
            g.fill(getX(), getY(), getX() + width, getY() + height, edge);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, isHovered() ? 0xFF2C3A2C : 0xFF23262B);
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, TEXT);
        }

        @Override public void playDownSound(@Nonnull SoundManager sounds) {
            sounds.play(SimpleSoundInstance.forUI(ModPower.SWITCH.get(), 1));
        }
    }
}
