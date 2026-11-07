package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.Units;
import com.malevolentgods.fourtwenty.gear.ModGear;
import com.malevolentgods.fourtwenty.gear.ReUpMenu;
import com.malevolentgods.fourtwenty.gear.ReUpStationBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/** The Re-Up Station: the piece on charge, the station's battery, what it takes from the Wicks and what it puts into gear. */
public class ReUpScreen extends TechScreen<ReUpMenu> {
    private static final int GAUGE_X = 158, GAUGE_Y = 16, TEXT_X = 50;

    public ReUpScreen(ReUpMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ReUpMenu.WIDTH;
        imageHeight = ReUpMenu.HEIGHT;
    }

    @Override protected int tier() { return menu.tier(); }

    @Override protected boolean overloaded() { return menu.overloaded(); }

    @Override protected void details(GuiGraphics g) {
        gauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.stored(), menu.capacity(), 0, 0);
    }

    @Override protected String helpKey() { return "re_up"; }

    @Override protected SlotHint hint(int slot) {
        if (slot != 0) return null;
        return new SlotHint(new ItemStack(ModGear.MOON_BOOTS.get()), Component.translatable("gui.fourtwenty.slot.re_up"));
    }

    @Override protected void tips(GuiGraphics g, int mouseX, int mouseY) {
        gaugeTip(g, mouseX, mouseY, GAUGE_X, GAUGE_Y, menu.stored(), menu.capacity(), Component.translatable("gui.fourtwenty.gauge.re_up"));
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        g.drawString(font, Component.translatable("gui.fourtwenty.re_up.in", Units.format(menu.inWatts(), Units.WATTS)), TEXT_X, 22, TEXT, false);
        g.drawString(font, Component.translatable("gui.fourtwenty.re_up.out", Units.format(menu.outWatts(), Units.WATTS)), TEXT_X, 34, TEXT, false);
        g.drawString(font, Component.translatable("gui.fourtwenty.re_up.stored", Units.format(menu.stored(), Units.WS)), TEXT_X, 46, TEXT, false);
        var status = switch (menu.state()) {
            case ReUpStationBlockEntity.STATE_CHARGING -> Component.translatable("gui.fourtwenty.re_up.charging");
            case ReUpStationBlockEntity.STATE_EMPTY -> Component.translatable("gui.fourtwenty.re_up.empty");
            case ReUpStationBlockEntity.STATE_FULL -> Component.translatable("gui.fourtwenty.re_up.full");
            default -> Component.translatable("gui.fourtwenty.re_up.idle");
        };
        int colour = menu.state() == ReUpStationBlockEntity.STATE_CHARGING ? GREEN : menu.state() == ReUpStationBlockEntity.STATE_EMPTY ? WARN : TEXT;
        if (!menu.overloaded()) g.drawString(font, status, 8, 72, colour, false);
    }
}
