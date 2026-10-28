package com.malevolentgods.fourtwenty.client.terpene;

import com.malevolentgods.fourtwenty.client.infuser.InfuserClient;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.terpene.TerpinatorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity.*;
import static com.malevolentgods.fourtwenty.terpene.TerpinatorMenu.*;

/**
 * The Terpinator. Left, every colour there is: the 16 Minecraft colours over the 64 hemp colours, those you've
 * caught in the Inspiration Infuser in their own colour, the rest unnamed visions. Pick one; right, the machine:
 * buds in, vials out, Start/Stop, and a line saying what it's doing or what it needs.
 */
public class TerpinatorScreen extends AbstractContainerScreen<TerpinatorMenu> {
    private static final int PANE_X = 134, PANE_W = WIDTH - PANE_X - 8, BTN_Y = ROW + 28, BTN_W = 56;

    public TerpinatorScreen(TerpinatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = INV_X;
        inventoryLabelY = INV_Y - 10;
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        int hover = cellAt(mouseX, mouseY);
        if (hover != NONE) g.renderComponentTooltip(font, describe(hover), mouseX, mouseY);
    }

    // ---- the grid -----------------------------------------------------------------------------------

    /** Product n's cell, relative to the panel: two rows of Minecraft colours, a gap, then 8 rows of hemp colours. */
    private static int[] cell(int product) {
        int i = product >= 64 ? product - 64 : product + 16;
        int row = i / 8, col = i % 8;
        return new int[] {GRID_X + col * CELL, GRID_Y + row * CELL + (row >= 2 ? GRID_GAP : 0)};
    }

    private int cellAt(double mx, double my) {
        for (int p = 0; p < PRODUCTS; p++) {
            int[] c = cell(p);
            if (mx >= leftPos + c[0] && mx < leftPos + c[0] + CELL - 1 && my >= topPos + c[1] && my < topPos + c[1] + CELL - 1) return p;
        }
        return NONE;
    }

    private static boolean caught(int product) { return InfuserClient.caught(target(product)); }

    private static Component name(int product) {
        return caught(product) ? Component.translatable(item(product).getDescriptionId()) : Component.translatable("item.fourtwenty.terpene.unknown.minecraft");
    }

    private static Component familyName(Strain.Family family) {
        return Component.translatable("gui.fourtwenty.terpinator.family." + family.name().toLowerCase(java.util.Locale.ROOT));
    }

    private List<Component> describe(int product) {
        List<Component> out = new ArrayList<>();
        out.add(name(product));
        if (!caught(product)) {
            out.add(Component.translatable("gui.fourtwenty.terpinator.locked").withStyle(ChatFormatting.GRAY));
            return out;
        }
        out.add(Component.translatable("gui.fourtwenty.terpinator.feeds", familyName(family(product))).withStyle(ChatFormatting.GRAY));
        return out;
    }

    // ---- drawing --------------------------------------------------------------------------------------

    @Override protected void renderBg(@Nonnull GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        TerpeneDraw.panel(g, x, y, imageWidth, imageHeight);
        TerpeneDraw.card(g, x + GRID_X - 4, y + GRID_Y - 4, 8 * CELL + 7, 10 * CELL + GRID_GAP + 7);
        int picked = menu.product(), hover = cellAt(mouseX, mouseY);
        for (int p = 0; p < PRODUCTS; p++) {
            int[] c = cell(p);
            int cx = x + c[0], cy = y + c[1], s = CELL - 1;
            if (caught(p)) TerpeneDraw.swatch(g, cx, cy, s, s, color(p));
            else TerpeneDraw.smudge(g, cx, cy, s, s, color(p));
            if (p == picked) outline(g, cx - 1, cy - 1, s + 2, s + 2, 0xFFFFFFFF, 0xFF202020);
            else if (p == hover && caught(p)) outline(g, cx - 1, cy - 1, s + 2, s + 2, 0xAAFFFFFF, 0);
        }
        // The machine.
        TerpeneDraw.card(g, x + PANE_X, y + GRID_Y - 4, PANE_W, 10 * CELL + GRID_GAP + 7);
        TerpeneDraw.slot(g, x + X_BUDS, y + ROW);
        TerpeneDraw.bigSlot(g, x + X_OUT, y + ROW);
        int tint = valid(picked) ? TerpeneDraw.opaque(color(picked)) : TerpeneDraw.TRACK;
        TerpeneDraw.arrow(g, x + X_BUDS + 22, x + X_OUT - 9, y + ROW + 8, menu.progress() / (float) TIME, tint);
        if (valid(picked) && menu.getSlot(OUT).getItem().isEmpty()) {
            g.renderFakeItem(new ItemStack(item(picked)), x + X_OUT, y + ROW);
            g.fill(x + X_OUT, y + ROW, x + X_OUT + 16, y + ROW + 16, 0x998B8B8B);
        }
        boolean hoverBtn = inButton(mouseX, mouseY);
        TerpeneDraw.runButton(g, font, x + X_BUDS - 1, y + BTN_Y, BTN_W, 14, menu.running(), valid(picked), hoverBtn);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) TerpeneDraw.slot(g, x + INV_X + col * 18, y + INV_Y + row * 18);
        }
        for (int col = 0; col < 9; col++) TerpeneDraw.slot(g, x + INV_X + col * 18, y + INV_Y + 58);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int c, int inner) {
        g.fill(x - 1, y - 1, x + w + 1, y, c);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, c);
        g.fill(x - 1, y, x, y + h, c);
        g.fill(x + w, y, x + w + 1, y + h, c);
        if (inner != 0) {
            g.fill(x, y, x + w, y + 1, inner);
            g.fill(x, y + h - 1, x + w, y + h, inner);
        }
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, TerpeneDraw.bold(title), 10, 9, TerpeneDraw.TEXT, false);
        g.drawString(font, TerpeneDraw.t(playerInventoryTitle), inventoryLabelX, inventoryLabelY, TerpeneDraw.TEXT, false);
        int picked = menu.product(), at = GRID_Y + 2, w = PANE_W - 12;
        if (!valid(picked)) {
            for (var line : font.split(TerpeneDraw.t("gui.fourtwenty.terpinator.pick"), w)) {
                g.drawString(font, line, PANE_X + 6, at, TerpeneDraw.TEXT, false);
                at += 10;
            }
        } else {
            for (var line : font.split(TerpeneDraw.bold(name(picked)), w)) {
                g.drawString(font, line, PANE_X + 6, at, TerpeneDraw.opaque(TerpeneDraw.mix(color(picked), 0x000000, 0.35f)), false);
                at += 10;
            }
            g.drawString(font, TerpeneDraw.t(Component.translatable("gui.fourtwenty.terpinator.feeds", familyName(family(picked)))),
                    PANE_X + 6, at + 1, TerpeneDraw.MUTED, false);
        }
        String why = status(menu.running(), picked, menu.getSlot(BUDS).getItem(), menu.getSlot(OUT).getItem());
        Component status = TerpeneDraw.t("gui.fourtwenty.terpinator.status." + (why == null ? "running" : why));
        int at2 = BTN_Y + 20;
        for (var line : font.split(status, w)) {
            g.drawString(font, line, PANE_X + 6, at2, why == null ? 0xFF3E8E57 : TerpeneDraw.MUTED, false);
            at2 += 10;
        }
    }

    private boolean inButton(double mx, double my) {
        int bx = leftPos + X_BUDS - 1, by = topPos + BTN_Y;
        return mx >= bx && mx < bx + BTN_W && my >= by && my < by + 14;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int p = cellAt(mouseX, mouseY);
            if (p != NONE) {
                if (caught(p) && p != menu.product()) press(BUTTON_PICK + p);
                return true;
            }
            if (inButton(mouseX, mouseY) && valid(menu.product())) {
                press(BUTTON_TOGGLE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void press(int id) {
        if (menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }
}
