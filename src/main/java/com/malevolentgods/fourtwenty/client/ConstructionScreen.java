package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.processing.ConstructionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.malevolentgods.fourtwenty.processing.ConstructionBlockEntity.*;

/** The crafting table's screen, recipe book and all, with the bench's reserve as a bar, what the recipe costs, and what the bench is doing. */
public class ConstructionScreen extends CraftingScreen {
    private static final int READY = 0x3F7A2A, WAITING = 0x9A5A10, STATUS_X = 90, COST_X = 133, COST_Y = 60;
    private static final int BAR_X = 152, BAR_Y = 17, BAR_W = 10, BAR_H = 53, FRAME = 0xFF14161A, EMPTY = 0xFF3A3D42, MARK = 0xFFFFFFFF;
    /** The tier colours from the Tech screen: Puff, Toke, Rip, Blaze. */
    private static final int[] FILL = {0xFF60C448, 0xFF3CC4B4, 0xFFA864E8, 0xFFFF8C28};

    public ConstructionScreen(CraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (!(menu instanceof ConstructionMenu bench)) return;
        int capacity = Math.max(1, bench.capacity()), inner = BAR_H - 2, bottom = BAR_Y + BAR_H - 1;
        g.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + BAR_H, FRAME);
        g.fill(BAR_X + 1, BAR_Y + 1, BAR_X + BAR_W - 1, bottom, EMPTY);
        int filled = (int) ((long) Math.min(bench.stored(), capacity) * inner / capacity);
        if (filled > 0) g.fill(BAR_X + 1, bottom - filled, BAR_X + BAR_W - 1, bottom, FILL[bench.tier().ordinal()]);
        if (bench.cost() > 0) {
            int mark = bottom - Math.max(1, (int) ((long) Math.min(bench.cost(), capacity) * inner / capacity));
            g.fill(BAR_X, mark, BAR_X + BAR_W, mark + 1, MARK);
            Component cost = Component.translatable("gui.fourtwenty.construction.cost", String.format("%,d", bench.cost()));
            g.drawString(font, cost, COST_X - font.width(cost) / 2, COST_Y, bench.affordable() ? READY : WAITING, false);
        }
        Component status = bench.overloaded() ? Component.translatable("gui.fourtwenty.construction.overloaded")
                : switch (bench.state()) {
                    case STATE_FULL -> Component.translatable("gui.fourtwenty.construction.ready", bench.factor());
                    case STATE_FREE -> Component.translatable("gui.fourtwenty.construction.free");
                    case STATE_PAID -> Component.translatable("gui.fourtwenty.construction.charging");
                    default -> Component.translatable("gui.fourtwenty.machine.waiting");
                };
        g.drawString(font, status, STATUS_X, inventoryLabelY, bench.overloaded() || bench.state() == STATE_WAITING ? WAITING : READY, false);
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (!(menu instanceof ConstructionMenu bench) || !isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) return;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.fourtwenty.construction.reserve", String.format("%,d", bench.stored()), String.format("%,d", bench.capacity())));
        if (bench.cost() > 0) lines.add(Component.translatable("gui.fourtwenty.construction.costs", String.format("%,d", bench.cost())));
        lines.add(bench.state() == STATE_FREE ? Component.translatable("gui.fourtwenty.construction.rate_free", String.format("%,d", bench.rate()))
                : Component.translatable("gui.fourtwenty.construction.rate", String.format("%,d", bench.rate())));
        lines.add(Component.translatable("gui.fourtwenty.construction.yield", bench.factor()));
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }
}
