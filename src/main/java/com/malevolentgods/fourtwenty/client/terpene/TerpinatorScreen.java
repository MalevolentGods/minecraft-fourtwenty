package com.malevolentgods.fourtwenty.client.terpene;

import com.malevolentgods.fourtwenty.domain.TerpenePalette;
import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge;
import com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity;
import com.malevolentgods.fourtwenty.terpene.TerpinatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.malevolentgods.fourtwenty.terpene.TerpinatorMenu.*;

/**
 * The Terpinator: press and refine on the top row (with the method dial), the three blend ports
 * below with a live preview of what they'll make (its colour always; its name once discovered),
 * a one-line hint that walks a new player through their first session, and the Guide button.
 */
public class TerpinatorScreen extends AbstractContainerScreen<TerpinatorMenu> {
    private static final int DIAL_X = 62, DIAL_Y = 58, DIAL_W = 25, DIAL_H = 13;

    public TerpinatorScreen(TerpinatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
        inventoryLabelY = INV_Y - 11;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.fourtwenty.terpinator.guide"),
                b -> minecraft.setScreen(new TerpeneGuideScreen(this))).bounds(leftPos + imageWidth - 46, topPos + 4, 40, 14).build());
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        dialTooltip(g, mouseX, mouseY);
        previewTooltip(g, mouseX, mouseY);
    }

    // ---- background -------------------------------------------------------------------------

    @Override protected void renderBg(@Nonnull GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        TerpeneDraw.panel(g, x, y, imageWidth, imageHeight);
        // Press -> resin -> refine -> base.
        TerpeneDraw.slot(g, x + X_BUDS, y + ROW_PRESS);
        TerpeneDraw.slot(g, x + X_RESIN, y + ROW_PRESS);
        TerpeneDraw.bigSlot(g, x + X_OUT, y + ROW_PRESS);
        int base = TerpenePalette.color(Terpenes.Code.of(TerpinatorBlockEntity.method(menu.method())));
        TerpeneDraw.arrow(g, x + 38, x + 67, y + ROW_PRESS + 8, menu.progress(TerpinatorBlockEntity.DATA_PRESS) / (float) TerpinatorBlockEntity.PRESS_TIME,
                0xFFC4801C);
        TerpeneDraw.arrow(g, x + 92, x + 136, y + ROW_PRESS + 8,
                menu.progress(TerpinatorBlockEntity.DATA_REFINE) / (float) TerpinatorBlockEntity.REFINE_TIME, TerpeneDraw.opaque(base));
        // The method dial.
        for (int i = 0; i < 4; i++) {
            Terpenes.Base b = Terpenes.Base.values()[i];
            int bx = x + DIAL_X + i * (DIAL_W + 2), by = y + DIAL_Y;
            boolean on = menu.method() == i, hover = inDial(mouseX, mouseY) == i;
            int c = TerpenePalette.color(Terpenes.Code.of(b));
            g.fill(bx, by, bx + DIAL_W, by + DIAL_H, on ? 0xFF202020 : TerpeneDraw.SHADOW);
            g.fill(bx + 1, by + 1, bx + DIAL_W - 1, by + DIAL_H - 1, on ? TerpeneDraw.opaque(c) : hover ? 0xFFB0B0B0 : 0xFFA0A0A0);
            Component label = Component.translatable("terpene.fourtwenty." + b.code() + ".abbr");
            g.drawString(font, label, bx + (DIAL_W - font.width(label)) / 2, by + 3, on ? 0xFFFFFFFF : TerpeneDraw.opaque(c), false);
        }
        // Blend: three numbered ports -> output.
        for (int[] port : new int[][] {{X_P1, 1}, {X_P2, 2}, {X_P3, 3}}) {
            TerpeneDraw.slot(g, x + port[0], y + ROW_BLEND);
            g.drawString(font, String.valueOf(port[1]), x + port[0] + 6, y + ROW_BLEND - 10, TerpeneDraw.MUTED, false);
        }
        TerpeneDraw.bigSlot(g, x + X_OUT, y + ROW_BLEND);
        Optional<Terpenes.Code> made = blendResult();
        int blendColor = made.map(c -> TerpeneDraw.opaque(TerpenePalette.color(c))).orElse(TerpeneDraw.TRACK);
        TerpeneDraw.arrow(g, x + 80, x + 136, y + ROW_BLEND + 8,
                menu.progress(TerpinatorBlockEntity.DATA_BLEND) / (float) TerpinatorBlockEntity.BLEND_TIME, blendColor);
        // The ghost of what the ports will make, in the output slot.
        if (made.isPresent() && menu.getSlot(TerpinatorBlockEntity.BLEND_OUT).getItem().isEmpty()) {
            ItemStack ghost = new ItemStack(com.malevolentgods.fourtwenty.registry.ModTerpenes.terpene(made.get()));
            g.renderFakeItem(ghost, x + X_OUT, y + ROW_BLEND);
            g.fill(x + X_OUT, y + ROW_BLEND, x + X_OUT + 16, y + ROW_BLEND + 16, 0x888B8B8B);
        }
        guide(g, x, y);
    }

    private Optional<Terpenes.Code> blendResult() {
        return TerpinatorBlockEntity.result(slotItem(TerpinatorBlockEntity.PORT1), slotItem(TerpinatorBlockEntity.PORT2),
                slotItem(TerpinatorBlockEntity.PORT3));
    }

    private ItemStack slotItem(int slot) { return menu.getSlot(slot).getItem(); }

    // ---- labels: title, hint, preview ----------------------------------------------------------

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TerpeneDraw.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TerpeneDraw.TEXT, false);
        // The hint: the next step of the first session, or a nudge about the ports.
        List<FormattedCharSequence> lines = font.split(hint(), imageWidth - 16);
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), 8, 18 + i * 9, TerpeneDraw.ACCENT, false);
        // The blend preview line.
        int py = ROW_BLEND + 24;
        Optional<Terpenes.Code> made = blendResult();
        if (made.isPresent()) {
            Terpenes.Code code = made.get();
            TerpeneDraw.swatch(g, X_P1, py, 10, 10, TerpenePalette.color(code));
            Component name = TerpeneKnowledge.knows(code) ? Component.translatable("item.fourtwenty.terpene_" + code.id())
                    : Component.translatable("gui.fourtwenty.terpinator.unknown");
            g.drawString(font, name, X_P1 + 14, py + 1, TerpeneDraw.TEXT, false);
        } else {
            Component why = portAdvice();
            if (why != null) g.drawString(font, font.split(why, imageWidth - 22).get(0), X_P1, py + 1, TerpeneDraw.MUTED, false);
        }
    }

    /** What's wrong with the ports, in plain words, or null when they're empty. */
    private Component portAdvice() {
        Terpenes.Code p1 = TerpinatorBlockEntity.code(slotItem(TerpinatorBlockEntity.PORT1));
        Terpenes.Code p2 = TerpinatorBlockEntity.code(slotItem(TerpinatorBlockEntity.PORT2));
        Terpenes.Code p3 = TerpinatorBlockEntity.code(slotItem(TerpinatorBlockEntity.PORT3));
        if (p1 == null && p2 == null && p3 == null) return null;
        String key;
        if (p1 == null) key = "port1_first";
        else if (p1.isBase() && p2 == null) key = "add_port2";
        else if (p1.isBase() && p3 != null) key = "clear_port3";
        else if (p1.isAccord() && p2 != null) key = "clear_port2";
        else key = "add_port3";
        return Component.translatable("gui.fourtwenty.terpinator.advice." + key);
    }

    /** The current step of the guided first session. */
    private Component hint() {
        if (!TerpeneKnowledge.knows(TerpeneKnowledge.RESIN)) return Component.translatable("gui.fourtwenty.terpinator.step.press");
        int bases = TerpeneKnowledge.count(1);
        if (bases == 0) return Component.translatable("gui.fourtwenty.terpinator.step.refine");
        if (bases < 4) return Component.translatable("gui.fourtwenty.terpinator.step.all_bases", bases);
        if (TerpeneKnowledge.count(2) == 0) return Component.translatable("gui.fourtwenty.terpinator.step.accord");
        if (TerpeneKnowledge.count(3) == 0) return Component.translatable("gui.fourtwenty.terpinator.step.finish");
        return Component.translatable("gui.fourtwenty.terpinator.step.free", TerpeneKnowledge.count(3));
    }

    /** Highlights what the current step is about. */
    private void guide(GuiGraphics g, int x, int y) {
        List<int[]> spots = new ArrayList<>();
        if (!TerpeneKnowledge.knows(TerpeneKnowledge.RESIN)) spots.add(new int[] {X_BUDS, ROW_PRESS, 16, 16});
        else if (TerpeneKnowledge.count(1) < 4) spots.add(new int[] {DIAL_X, DIAL_Y, 4 * DIAL_W + 6, DIAL_H});
        else if (TerpeneKnowledge.count(2) == 0) {
            spots.add(new int[] {X_P1, ROW_BLEND, 16, 16});
            spots.add(new int[] {X_P2, ROW_BLEND, 16, 16});
        } else if (TerpeneKnowledge.count(3) == 0) {
            spots.add(new int[] {X_P1, ROW_BLEND, 16, 16});
            spots.add(new int[] {X_P3, ROW_BLEND, 16, 16});
        }
        for (int[] s : spots) TerpeneDraw.pulse(g, x + s[0], y + s[1], s[2], s[3]);
    }

    // ---- the dial -----------------------------------------------------------------------------

    private int inDial(double mouseX, double mouseY) {
        double mx = mouseX - leftPos - DIAL_X, my = mouseY - topPos - DIAL_Y;
        if (my < 0 || my >= DIAL_H || mx < 0) return -1;
        int i = (int) (mx / (DIAL_W + 2));
        return i < 4 && mx - i * (DIAL_W + 2) < DIAL_W ? i : -1;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = inDial(mouseX, mouseY);
        if (i >= 0 && menu.clickMenuButton(minecraft.player, i)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void dialTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int i = inDial(mouseX, mouseY);
        if (i < 0) return;
        Terpenes.Base b = Terpenes.Base.values()[i];
        Component base = TerpeneKnowledge.knows(b.code()) ? Component.translatable("item.fourtwenty.terpene_" + b.code())
                : Component.translatable("gui.fourtwenty.terpinator.unknown");
        g.renderTooltip(font, Component.translatable("gui.fourtwenty.terpinator.method",
                Component.translatable("gui.fourtwenty.terpinator.method." + b.method()), base), mouseX, mouseY);
    }

    private void previewTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int px = leftPos + X_P1, py = topPos + ROW_BLEND + 24;
        if (mouseX < px || mouseX >= px + 10 || mouseY < py || mouseY >= py + 10) return;
        blendResult().ifPresent(code -> g.renderComponentTooltip(font, TerpeneGuideScreen.describe(code), mouseX, mouseY));
    }
}
