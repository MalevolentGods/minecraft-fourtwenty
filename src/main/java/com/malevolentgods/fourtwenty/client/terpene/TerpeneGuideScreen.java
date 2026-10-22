package com.malevolentgods.fourtwenty.client.terpene;

import com.malevolentgods.fourtwenty.domain.TerpenePalette;
import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.terpene.TerpeneItem;
import com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * The Terpinator's guide: progress, the first-session checklist and the legend on the left; the
 * map of all 64 colours on the right (families 2x2; in each, a row per accord and a column per
 * tone). An undiscovered colour shows its family, hatched, until its accord is known, then its own
 * colour; its name once made. Hovering anything tells you how it's made.
 */
public class TerpeneGuideScreen extends Screen {
    private static final int W = 420, H = 236, LEFT_W = 168, CELL = 15, PITCH = 17, ROW_LABEL = 40;
    private final Screen parent;
    private int x0, y0;

    public TerpeneGuideScreen(Screen parent) {
        super(Component.translatable("gui.fourtwenty.guide.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        x0 = (width - W) / 2;
        y0 = Math.max(4, (height - H) / 2);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(x0 + W - 52, y0 + H - 22, 46, 16).build());
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        List<Component> tip = null;
        tip = left(g, mouseX, mouseY, tip);
        tip = map(g, mouseX, mouseY, tip);
        if (tip != null) g.renderComponentTooltip(font, tip, mouseX, mouseY);
    }

    @Override public void renderBackground(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        TerpeneDraw.panel(g, x0, y0, W, H);
        g.fill(x0 + LEFT_W + 6, y0 + 8, x0 + LEFT_W + 7, y0 + H - 8, 0xFFA8A8A8);
    }

    // ---- left: progress, first session, legend ---------------------------------------------

    private List<Component> left(GuiGraphics g, int mx, int my, List<Component> tip) {
        int x = x0 + 9, y = y0 + 8;
        g.drawString(font, TerpeneDraw.bold(title), x, y, TerpeneDraw.TEXT, false);
        int found = TerpeneKnowledge.count(1) + TerpeneKnowledge.count(2) + TerpeneKnowledge.count(3);
        g.drawString(font, TerpeneDraw.t(Component.translatable("gui.fourtwenty.guide.progress", found, Terpenes.all().size())), x, y + 11, TerpeneDraw.ACCENT, false);
        y += 26;
        g.drawString(font, TerpeneDraw.bold(Component.translatable("gui.fourtwenty.guide.first_session")), x, y, TerpeneDraw.TEXT, false);
        y += 11;
        boolean[] done = {
                TerpeneKnowledge.knows(TerpeneKnowledge.RESIN), TerpeneKnowledge.count(1) > 0, TerpeneKnowledge.count(1) == 4,
                TerpeneKnowledge.count(2) > 0, TerpeneKnowledge.count(3) > 0};
        String[] steps = {"press", "refine", "all_bases", "accord", "finish"};
        for (int i = 0; i < steps.length; i++) {
            boolean ok = done[i];
            g.drawString(font, ok ? "✔" : "○", x, y, ok ? 0xFF2F7A47 : TerpeneDraw.MUTED, false);
            g.drawString(font, TerpeneDraw.t(Component.translatable("gui.fourtwenty.guide.step." + steps[i])), x + 10, y, ok ? TerpeneDraw.MUTED : TerpeneDraw.TEXT, false);
            y += 10;
        }
        y += 8;
        // The legend: what each base does in each port.
        g.drawString(font, TerpeneDraw.bold(Component.translatable("gui.fourtwenty.guide.legend")), x, y, TerpeneDraw.TEXT, false);
        y += 11;
        int c1 = x + 30, c2 = x + 78, c3 = x + 122;
        g.drawString(font, TerpeneDraw.t(Component.translatable("gui.fourtwenty.guide.port", 1)), c1, y, TerpeneDraw.MUTED, false);
        g.drawString(font, TerpeneDraw.t(Component.translatable("gui.fourtwenty.guide.port", 2)), c2, y, TerpeneDraw.MUTED, false);
        g.drawString(font, TerpeneDraw.t(Component.translatable("gui.fourtwenty.guide.port", 3)), c3, y, TerpeneDraw.MUTED, false);
        y += 10;
        for (Terpenes.Base b : Terpenes.Base.values()) {
            int w = TerpeneDraw.chip(g, font, b, x, y);
            String k = "terpene.fourtwenty." + b.code();
            g.drawString(font, TerpeneDraw.t(Component.translatable(k + ".family")), c1, y + 1, TerpeneDraw.TEXT, false);
            g.drawString(font, TerpeneDraw.t(Component.translatable(k + ".hue")), c2, y + 1, TerpeneDraw.TEXT, false);
            g.drawString(font, TerpeneDraw.t(Component.translatable(k + ".tone")), c3, y + 1, TerpeneDraw.TEXT, false);
            if (mx >= x && mx < x + w && my >= y && my < y + 10) tip = describe(Terpenes.Code.of(b));
            y += 12;
        }
        y += 4;
        for (FormattedLine line : wrap(TerpeneDraw.t(Component.translatable("gui.fourtwenty.guide.rule")), LEFT_W - 12)) {
            g.drawString(font, line.text, x, y, TerpeneDraw.MUTED, false);
            y += 9;
        }
        return tip;
    }

    private record FormattedLine(net.minecraft.util.FormattedCharSequence text) {}

    private List<FormattedLine> wrap(Component text, int width) {
        List<FormattedLine> out = new ArrayList<>();
        font.split(text, width).forEach(l -> out.add(new FormattedLine(l)));
        return out;
    }

    // ---- right: the map --------------------------------------------------------------------

    private List<Component> map(GuiGraphics g, int mx, int my, List<Component> tip) {
        int mapX = x0 + LEFT_W + 16, mapY = y0 + 8;
        g.drawString(font, TerpeneDraw.bold(Component.translatable("gui.fourtwenty.guide.map")), mapX, mapY, TerpeneDraw.TEXT, false);
        g.drawString(font, TerpeneDraw.t(Component.translatable("gui.fourtwenty.guide.columns")), mapX, mapY + 11, TerpeneDraw.MUTED, false);
        int famW = ROW_LABEL + 4 * PITCH, famH = 12 + 4 * PITCH;
        for (int f = 0; f < 4; f++) {
            Terpenes.Base family = Terpenes.Base.values()[f];
            int fx = mapX + (f % 2) * (famW + 8), fy = mapY + 26 + (f / 2) * (famH + 8);
            g.drawString(font, TerpeneDraw.bold(Component.translatable("terpene.fourtwenty.family." + family.code())), fx, fy, TerpeneDraw.TEXT, false);
            for (int h = 0; h < 4; h++) {
                Terpenes.Code accord = Terpenes.Code.of(family, Terpenes.Base.values()[h]);
                int ry = fy + 12 + h * PITCH;
                int w = TerpeneDraw.chips(g, font, accord, fx, ry + 2);
                if (!TerpeneKnowledge.knows(accord)) g.fill(fx, ry + 2, fx + w, ry + 12, 0x99C6C6C6);
                if (mx >= fx && mx < fx + w && my >= ry + 2 && my < ry + 12) tip = describe(accord);
                for (int t = 0; t < 4; t++) {
                    Terpenes.Code code = Terpenes.finish(accord, Terpenes.TONE_ORDER.get(t));
                    int cx = fx + ROW_LABEL + t * PITCH;
                    if (TerpeneKnowledge.knows(code) || TerpeneKnowledge.knows(accord)) {
                        TerpeneDraw.swatch(g, cx, ry, CELL, CELL, TerpenePalette.color(code));
                        if (TerpeneKnowledge.knows(code)) g.fill(cx + CELL - 4, ry + 1, cx + CELL - 1, ry + 4, 0xFFFFFFFF);
                    } else {
                        TerpeneDraw.smudge(g, cx, ry, CELL, CELL, TerpenePalette.color(Terpenes.Code.of(family)));
                    }
                    if (mx >= cx && mx < cx + CELL && my >= ry && my < ry + CELL) {
                        tip = describe(code);
                        TerpeneDraw.pulse(g, cx, ry, CELL, CELL);
                    }
                }
            }
        }
        return tip;
    }

    // ---- descriptions (also used by the Terpinator's preview) -------------------------------

    private static MutableComponent name(Terpenes.Code code) {
        return TerpeneKnowledge.knows(code) ? Component.translatable("item.fourtwenty.terpene_" + code.id())
                : Component.translatable("gui.fourtwenty.terpinator.unknown");
    }

    /** A terpene's tooltip: name (or ???), flavour line once known, code, and how it's made. */
    public static List<Component> describe(Terpenes.Code code) {
        List<Component> out = new ArrayList<>();
        boolean known = TerpeneKnowledge.knows(code);
        out.add(name(code).withStyle(s -> s.withColor(TextColor.fromRgb(TerpeneDraw.mix(TerpenePalette.color(code), 0xFFFFFF, 0.15f)))));
        if (known) out.addAll(TerpeneItem.flavour(code.id(), 40));
        out.add(TerpeneItem.codeLine(code));
        List<Terpenes.Base> l = code.letters();
        if (code.isBase()) {
            out.add(Component.translatable("gui.fourtwenty.guide.how.base",
                    Component.translatable("gui.fourtwenty.terpinator.method." + l.get(0).method())).withStyle(ChatFormatting.GRAY));
        } else if (code.isAccord()) {
            out.add(Component.translatable("gui.fourtwenty.guide.how.accord", name(Terpenes.Code.of(l.get(0))), name(Terpenes.Code.of(l.get(1))))
                    .withStyle(ChatFormatting.GRAY));
            out.add(Component.translatable("gui.fourtwenty.guide.hue", Component.translatable("terpene.fourtwenty." + code.id() + ".hue"),
                    Component.translatable("terpene.fourtwenty.family." + l.get(0).code())).withStyle(ChatFormatting.GRAY));
        } else {
            out.add(Component.translatable("gui.fourtwenty.guide.how.final", name(code.accord()), name(Terpenes.Code.of(l.get(2))),
                    Component.translatable("terpene.fourtwenty." + l.get(2).code() + ".tone")).withStyle(ChatFormatting.GRAY));
            if (known) out.add(Component.translatable("gui.fourtwenty.guide.dye").withStyle(ChatFormatting.DARK_GRAY));
        }
        return out;
    }
}
