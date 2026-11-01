package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.Studies;
import com.malevolentgods.fourtwenty.processing.ThinkTankBlockEntity;
import com.malevolentgods.fourtwenty.processing.ThinkTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nonnull;

/** Pick a study from the swatches, load its inputs, and watch the bar. */
public class ThinkTankScreen extends TechScreen<ThinkTankMenu> {
    private static final int GRID_X = 8, GRID_Y = 20, CELL = 18, COLS = 4, ARROW_X = 98, ARROW_Y = 42, GAUGE_X = 158, GAUGE_Y = 16, STATUS_X = 84, STATUS_Y = 72, COLLECT_W = 54, COLLECT_H = 14;

    private HelpScreen.TechButton collect;

    public ThinkTankScreen(ThinkTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected void init() {
        super.init();
        collect = addRenderableWidget(new HelpScreen.TechButton(leftPos + ARROW_X, topPos + ARROW_Y, COLLECT_W, COLLECT_H, Component.translatable("gui.fourtwenty.think_tank.collect"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ThinkTankMenu.BUTTON_COLLECT)));
    }

    @Override protected int tier() { return 0; }

    @Override protected boolean overloaded() { return menu.overloaded(); }

    @Override protected String helpKey() { return "think_tank"; }

    private int[] cell(int study) {
        return new int[] {GRID_X + study % COLS * CELL, GRID_Y + study / COLS * (CELL - 3)};
    }

    private static DyeColor colour(int study) {
        return DyeColor.byName(Studies.ALL.get(study).colour(), DyeColor.WHITE);
    }

    @Override protected SlotHint hint(int slot) {
        if (slot >= ThinkTankBlockEntity.SLOTS || menu.study() < 0) return null;
        var inputs = Studies.ALL.get(menu.study()).inputs();
        if (slot >= inputs.size()) return null;
        return new SlotHint(ThinkTankBlockEntity.stack(inputs.get(slot)), Component.translatable("gui.fourtwenty.slot.study", inputs.get(slot).count()));
    }

    @Override protected void details(GuiGraphics g) {
        for (int i = 0; i < Studies.ALL.size(); i++) {
            int[] c = cell(i);
            int x = leftPos + c[0], y = topPos + c[1];
            g.fill(x, y, x + CELL - 3, y + CELL - 6, i == menu.study() ? GREEN : 0xFF14161A);
            g.fill(x + 1, y + 1, x + CELL - 4, y + CELL - 7, 0xFF000000 | colour(i).getTextColor());
        }
        boolean done = menu.status() == ThinkTankBlockEntity.STATUS_DONE;
        collect.visible = done;
        if (!done) arrow(g, leftPos + ARROW_X, topPos + ARROW_Y, 1, menu.progress(), menu.total());
        gauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.stored(), menu.capacity(), 0, 0);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < Studies.ALL.size(); i++) {
            int[] c = cell(i);
            double mx = mouseX - leftPos - c[0], my = mouseY - topPos - c[1];
            if (mx >= 0 && mx < CELL - 3 && my >= 0 && my < CELL - 6) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ThinkTankMenu.BUTTON_PICK + i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void tips(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < Studies.ALL.size(); i++) {
            int[] c = cell(i);
            hover(g, mouseX, mouseY, c[0], c[1], CELL - 3, CELL - 6, Component.translatable("gui.fourtwenty.think_tank.study",
                    Component.translatable("color.minecraft." + Studies.ALL.get(i).colour()), Studies.ALL.get(i).units()));
        }
        gaugeTip(g, mouseX, mouseY, GAUGE_X, GAUGE_Y, menu.stored(), menu.capacity(), Component.translatable("gui.fourtwenty.gauge.machine"));
        int percent = menu.total() <= 0 ? 0 : 100 * menu.progress() / menu.total();
        arrowTip(g, mouseX, mouseY, ARROW_X, ARROW_Y, Component.translatable("gui.fourtwenty.think_tank.progress", percent, menu.minutes()));
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        int status = menu.status();
        g.drawString(font, Component.translatable("gui.fourtwenty.think_tank.status." + status), STATUS_X, STATUS_Y, status == ThinkTankBlockEntity.STATUS_RUNNING ? GREEN : WARN, false);
    }
}
