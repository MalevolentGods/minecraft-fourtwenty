package com.malevolentgods.fourtwenty.client.terpene;

import com.malevolentgods.fourtwenty.domain.TerpenePalette;
import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge;
import com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity;
import com.malevolentgods.fourtwenty.terpene.TerpinatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity.*;
import static com.malevolentgods.fourtwenty.terpene.TerpinatorMenu.*;

/**
 * The Terpinator. A header (title, Guide), a hint strip that walks a new player through their
 * first session, then two cards: Press & refine (buds -> resin -> base, the method dial, a
 * Start/Stop button per stage) and Blend (three numbered ports -> output, a live preview of what
 * they'll make, Start/Stop), then the inventory. Every stage says in words what it's doing.
 */
public class TerpinatorScreen extends AbstractContainerScreen<TerpinatorMenu> {
    // Layout, relative to the panel (slot positions live in TerpinatorMenu).
    private static final int CARD1_Y = 46, CARD1_H = 64, CARD2_Y = 114, CARD2_H = 58;
    private static final int TITLE1_Y = CARD1_Y + 4, STATUS1_Y = ROW_PRESS + 20, CONTROLS_Y = CARD1_Y + 46, TITLE2_Y = CARD2_Y + 4,
            PREVIEW_Y = CARD2_Y + 40;
    private static final int[] PRESS_BTN = {12, CONTROLS_Y, 54, 13}, REFINE_BTN = {170, CONTROLS_Y, 54, 13}, BLEND_BTN = {170, PREVIEW_Y - 2, 54, 13};
    private static final int DIAL_X = 74, DIAL_W = 22, GUIDE_X = WIDTH - 50, GUIDE_W = 42;

    public TerpinatorScreen(TerpinatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = INV_X;
        inventoryLabelY = INV_Y - 9;
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        extraTooltips(g, mouseX, mouseY);
    }

    private ItemStack item(int slot) { return menu.getSlot(slot).getItem(); }

    private Optional<Terpenes.Code> blendResult() { return TerpinatorBlockEntity.result(item(PORT1), item(PORT2), item(PORT3)); }

    private boolean in(double mx, double my, int[] r) {
        return mx >= leftPos + r[0] && mx < leftPos + r[0] + r[2] && my >= topPos + r[1] && my < topPos + r[1] + r[3];
    }

    private int[] dial(int i) { return new int[] {DIAL_X + i * (DIAL_W + 1), CONTROLS_Y, DIAL_W, 13}; }

    // ---- background ---------------------------------------------------------------------------

    @Override protected void renderBg(@Nonnull GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        TerpeneDraw.panel(g, x, y, imageWidth, imageHeight);
        // Hint strip.
        g.fill(x + 8, y + 21, x + imageWidth - 8, y + 42, 0xFFB4AA96);
        g.fill(x + 9, y + 22, x + imageWidth - 8, y + 42, 0xFFF2EAD8);
        // Press & refine.
        TerpeneDraw.card(g, x + 7, y + CARD1_Y, imageWidth - 14, CARD1_H);
        TerpeneDraw.slot(g, x + X_BUDS, y + ROW_PRESS);
        TerpeneDraw.slot(g, x + X_RESIN, y + ROW_PRESS);
        TerpeneDraw.bigSlot(g, x + X_BASE_OUT, y + ROW_PRESS);
        int base = TerpenePalette.color(Terpenes.Code.of(method(menu.method())));
        TerpeneDraw.arrow(g, x + X_BUDS + 22, x + X_RESIN - 4, y + ROW_PRESS + 8, menu.progress(DATA_PRESS) / (float) PRESS_TIME, 0xFFC4801C);
        TerpeneDraw.arrow(g, x + X_RESIN + 22, x + X_BASE_OUT - 9, y + ROW_PRESS + 8, menu.progress(DATA_REFINE) / (float) REFINE_TIME,
                TerpeneDraw.opaque(base));
        boolean locked = menu.running(STAGE_REFINE);
        for (int i = 0; i < 4; i++) {
            Terpenes.Base b = Terpenes.Base.values()[i];
            int[] r = dial(i);
            boolean on = menu.method() == i;
            int c = TerpenePalette.color(Terpenes.Code.of(b));
            g.fill(x + r[0], y + r[1], x + r[0] + r[2], y + r[1] + r[3], on ? 0xFF202020 : TerpeneDraw.SHADOW);
            int face = on ? TerpeneDraw.opaque(c) : locked ? 0xFFA4A4A4 : in(mouseX, mouseY, r) ? 0xFFDADADA : 0xFFCACACA;
            g.fill(x + r[0] + 1, y + r[1] + 1, x + r[0] + r[2] - 1, y + r[1] + r[3] - 1, face);
            Component label = TerpeneDraw.bold(Component.translatable("terpene.fourtwenty." + b.code() + ".abbr"));
            int ink = on ? 0xFFFFFFFF : locked ? 0xFF8A8A8A : TerpeneDraw.opaque(TerpeneDraw.mix(c, 0x000000, 0.15f));
            g.drawString(font, label, x + r[0] + (r[2] - font.width(label)) / 2 + 1, y + r[1] + 3, ink, false);
        }
        runButton(g, PRESS_BTN, STAGE_PRESS, mouseX, mouseY);
        runButton(g, REFINE_BTN, STAGE_REFINE, mouseX, mouseY);
        // Blend.
        TerpeneDraw.card(g, x + 7, y + CARD2_Y, imageWidth - 14, CARD2_H);
        int[][] ports = {{X_P1, PORT1}, {X_P2, PORT2}, {X_P3, PORT3}};
        for (int i = 0; i < 3; i++) {
            TerpeneDraw.slot(g, x + ports[i][0], y + ROW_BLEND);
            if (item(ports[i][1]).isEmpty()) {
                Component n = TerpeneDraw.bold(Component.literal(String.valueOf(i + 1)));
                g.drawString(font, n, x + ports[i][0] + (16 - font.width(n)) / 2 + 1, y + ROW_BLEND + 5, 0xFF9E9E9E, false);
            }
        }
        TerpeneDraw.bigSlot(g, x + X_BLEND_OUT, y + ROW_BLEND);
        Optional<Terpenes.Code> made = blendResult();
        int blendColor = made.map(c -> TerpeneDraw.opaque(TerpenePalette.color(c))).orElse(TerpeneDraw.TRACK);
        TerpeneDraw.arrow(g, x + X_P3 + 22, x + X_BLEND_OUT - 9, y + ROW_BLEND + 8, menu.progress(DATA_BLEND) / (float) BLEND_TIME, blendColor);
        if (made.isPresent() && item(BLEND_OUT).isEmpty()) {
            g.renderFakeItem(new ItemStack(ModTerpenes.terpene(made.get())), x + X_BLEND_OUT, y + ROW_BLEND);
            g.fill(x + X_BLEND_OUT, y + ROW_BLEND, x + X_BLEND_OUT + 16, y + ROW_BLEND + 16, 0x998B8B8B);
        }
        runButton(g, BLEND_BTN, STAGE_BLEND, mouseX, mouseY);
        // Inventory.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) TerpeneDraw.slot(g, x + INV_X + col * 18, y + INV_Y + row * 18);
        }
        for (int col = 0; col < 9; col++) TerpeneDraw.slot(g, x + INV_X + col * 18, y + INV_Y + 58);
        // Guide button.
        int[] gr = {GUIDE_X, 5, GUIDE_W, 13};
        boolean gh = in(mouseX, mouseY, gr);
        g.fill(x + gr[0], y + gr[1], x + gr[0] + gr[2], y + gr[1] + gr[3], 0xFF202020);
        g.fill(x + gr[0] + 1, y + gr[1] + 1, x + gr[0] + gr[2] - 1, y + gr[1] + gr[3] - 1, gh ? 0xFFE6D3A8 : 0xFFD9C597);
        Component guide = TerpeneDraw.bold(Component.translatable("gui.fourtwenty.terpinator.guide"));
        g.drawString(font, guide, x + gr[0] + (gr[2] - font.width(guide)) / 2, y + gr[1] + 3, 0xFF4A3410, false);
        firstSession(g, x, y);
    }

    private void runButton(GuiGraphics g, int[] r, int stage, int mouseX, int mouseY) {
        boolean on = menu.running(stage);
        boolean enabled = on || TerpinatorBlockEntity.cannotStart(stage, item(BASE_OUT)) == null;
        TerpeneDraw.runButton(g, font, leftPos + r[0], topPos + r[1], r[2], r[3], on, enabled, in(mouseX, mouseY, r));
    }

    // ---- labels ---------------------------------------------------------------------------------

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, TerpeneDraw.bold(title), 9, 7, TerpeneDraw.TEXT, false);
        g.drawString(font, TerpeneDraw.t(playerInventoryTitle), inventoryLabelX, inventoryLabelY, TerpeneDraw.MUTED, false);
        List<FormattedCharSequence> hint = font.split(TerpeneDraw.t(hint()), imageWidth - 26);
        for (int i = 0; i < Math.min(2, hint.size()); i++) g.drawString(font, hint.get(i), 13, 24 + i * 9, TerpeneDraw.ACCENT, false);
        // Press and Refine: titles over their columns, statuses centred under their arrows.
        g.drawString(font, TerpeneDraw.bold(Component.translatable("gui.fourtwenty.terpinator.stage.press")), 12, TITLE1_Y, TerpeneDraw.TEXT, false);
        g.drawString(font, TerpeneDraw.bold(Component.translatable("gui.fourtwenty.terpinator.stage.refine")), X_RESIN - 2, TITLE1_Y,
                TerpeneDraw.TEXT, false);
        centredStatus(g, STAGE_PRESS, X_BUDS + 22, X_RESIN - 4);
        centredStatus(g, STAGE_REFINE, X_RESIN + 22, X_BASE_OUT - 9);
        stageTitle(g, "blend", STAGE_BLEND, 12, TITLE2_Y, X_BLEND_OUT - 20);
        // The blend preview: its colour always, its name once discovered; or what's wrong with the ports.
        Optional<Terpenes.Code> made = blendResult();
        int maxW = BLEND_BTN[0] - X_P1 - 18;
        if (made.isPresent()) {
            Terpenes.Code code = made.get();
            TerpeneDraw.swatch(g, X_P1, PREVIEW_Y, 10, 10, TerpenePalette.color(code));
            Component name = TerpeneKnowledge.knows(code) ? TerpeneDraw.bold(Component.translatable("item.fourtwenty.terpene_" + code.id()))
                    : TerpeneDraw.bold(Component.translatable("gui.fourtwenty.terpinator.unknown"));
            g.drawString(font, font.split(name, maxW).get(0), X_P1 + 14, PREVIEW_Y + 1, TerpeneDraw.TEXT, false);
        } else {
            Component why = portAdvice();
            if (why != null) g.drawString(font, font.split(TerpeneDraw.t(why), maxW + 14).get(0), X_P1, PREVIEW_Y + 1, TerpeneDraw.MUTED, false);
        }
    }

    private int statusColor(int stage) {
        if (!menu.running(stage)) return TerpeneDraw.MUTED;
        return status(stage).getString().equals(Component.translatable("gui.fourtwenty.terpinator.status.running").getString())
                ? TerpeneDraw.GOOD : TerpeneDraw.WARN;
    }

    /** A stage's status, centred between x0 and x1 on the line under the slots. */
    private void centredStatus(GuiGraphics g, int stage, int x0, int x1) {
        Component s = TerpeneDraw.t(status(stage));
        List<FormattedCharSequence> lines = font.split(s, x1 - x0);
        if (lines.isEmpty()) return;
        g.drawString(font, lines.get(0), x0 + (x1 - x0 - font.width(lines.get(0))) / 2, STATUS1_Y, statusColor(stage), false);
    }

    /** "Blend · running" etc., clipped to its column. */
    private void stageTitle(GuiGraphics g, String stage, int index, int x, int y, int width) {
        Component name = TerpeneDraw.bold(Component.translatable("gui.fourtwenty.terpinator.stage." + stage));
        g.drawString(font, name, x, y, TerpeneDraw.TEXT, false);
        Component status = TerpeneDraw.t(status(index));
        int sx = x + font.width(name) + 5;
        int color = statusColor(index);
        List<FormattedCharSequence> lines = font.split(status, Math.max(10, width - (sx - x)));
        if (!lines.isEmpty()) g.drawString(font, lines.get(0), sx, y, color, false);
    }

    /** What a stage is doing, in words. */
    private Component status(int stage) {
        boolean on = menu.running(stage);
        String key = switch (stage) {
            case STAGE_PRESS -> !on ? "stopped" : item(BUDS).getCount() < 2 ? "needs_buds"
                    : item(RESIN).getCount() >= item(RESIN).getMaxStackSize() ? "full" : "running";
            case STAGE_REFINE -> !on ? (item(BASE_OUT).isEmpty() ? "stopped" : "empty_output") : item(RESIN).isEmpty() ? "needs_resin"
                    : item(BASE_OUT).getCount() > item(BASE_OUT).getMaxStackSize() - 2 ? "full" : "running";
            default -> !on ? "stopped" : blendResult().isEmpty() ? "needs_terpenes"
                    : item(BLEND_OUT).getCount() >= item(BLEND_OUT).getMaxStackSize() ? "full" : "running";
        };
        return Component.translatable("gui.fourtwenty.terpinator.status." + key);
    }

    /** What's wrong with the ports, in plain words, or null when they're empty. */
    private Component portAdvice() {
        Terpenes.Code p1 = TerpinatorBlockEntity.code(item(PORT1)), p2 = TerpinatorBlockEntity.code(item(PORT2)),
                p3 = TerpinatorBlockEntity.code(item(PORT3));
        if (p1 == null && p2 == null && p3 == null) return Component.translatable("gui.fourtwenty.terpinator.advice.empty");
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

    /** Pulses what the current step is about. */
    private void firstSession(GuiGraphics g, int x, int y) {
        List<int[]> spots = new ArrayList<>();
        if (!TerpeneKnowledge.knows(TerpeneKnowledge.RESIN)) {
            spots.add(new int[] {X_BUDS, ROW_PRESS, 16, 16});
            if (!menu.running(STAGE_PRESS)) spots.add(PRESS_BTN);
        } else if (TerpeneKnowledge.count(1) < 4) {
            spots.add(new int[] {DIAL_X, CONTROLS_Y, 4 * (DIAL_W + 1) - 1, 13});
            if (!menu.running(STAGE_REFINE)) spots.add(REFINE_BTN);
        } else if (TerpeneKnowledge.count(2) == 0) {
            spots.add(new int[] {X_P1, ROW_BLEND, 16, 16});
            spots.add(new int[] {X_P2, ROW_BLEND, 16, 16});
            if (!menu.running(STAGE_BLEND)) spots.add(BLEND_BTN);
        } else if (TerpeneKnowledge.count(3) == 0) {
            spots.add(new int[] {X_P1, ROW_BLEND, 16, 16});
            spots.add(new int[] {X_P3, ROW_BLEND, 16, 16});
        }
        for (int[] s : spots) TerpeneDraw.pulse(g, x + s[0], y + s[1], s[2], s[3]);
    }

    // ---- input ----------------------------------------------------------------------------------

    private void click(int id) {
        if (menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        } else {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0f, 0.4f));
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < 4; i++) {
                if (in(mouseX, mouseY, dial(i))) {
                    click(i);
                    return true;
                }
            }
            int[][] buttons = {PRESS_BTN, REFINE_BTN, BLEND_BTN};
            for (int s = 0; s < 3; s++) {
                if (in(mouseX, mouseY, buttons[s])) {
                    click(BUTTON_TOGGLE + s);
                    return true;
                }
            }
            if (in(mouseX, mouseY, new int[] {GUIDE_X, 5, GUIDE_W, 13})) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                minecraft.setScreen(new TerpeneGuideScreen(this));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void extraTooltips(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < 4; i++) {
            if (!in(mouseX, mouseY, dial(i))) continue;
            Terpenes.Base b = Terpenes.Base.values()[i];
            Component base = TerpeneKnowledge.knows(b.code()) ? Component.translatable("item.fourtwenty.terpene_" + b.code())
                    : Component.translatable("gui.fourtwenty.terpinator.unknown");
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.fourtwenty.terpinator.method",
                    Component.translatable("gui.fourtwenty.terpinator.method." + b.method()), base));
            if (menu.running(STAGE_REFINE)) tip.add(Component.translatable("gui.fourtwenty.terpinator.refuse.locked")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
            return;
        }
        if (in(mouseX, mouseY, REFINE_BTN) && !menu.running(STAGE_REFINE) && !item(BASE_OUT).isEmpty()) {
            g.renderTooltip(font, Component.translatable("gui.fourtwenty.terpinator.refuse.output"), mouseX, mouseY);
            return;
        }
        int px = leftPos + X_P1, py = topPos + PREVIEW_Y;
        if (mouseX >= px && mouseX < px + 10 && mouseY >= py && mouseY < py + 10) {
            blendResult().ifPresent(code -> g.renderComponentTooltip(font, TerpeneGuideScreen.describe(code), mouseX, mouseY));
        }
    }
}
