package com.malevolentgods.fourtwenty.client.handbook;

import com.malevolentgods.fourtwenty.domain.handbook.Element;
import com.malevolentgods.fourtwenty.domain.handbook.Layout;
import com.malevolentgods.fourtwenty.domain.handbook.Span;
import com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws Handbook elements on a page and measures them for {@link Layout} (specs/016-weedcraft-handbook FR-004).
 * Text is ink on paper; links are underlined green and carry their entry in a click event the screen reads back.
 */
public final class PageRenderer {
    public static final int LINE = 10;
    public static final int INK = 0xFF3A2E20, HEADING = 0xFF2F5A1A, LINK = 0xFF2E7A1A, FAINT = 0xFF8A7A5A, RULE = 0x40624A2A;
    private static final Map<String, Integer> COLOURS = Map.of(
            "lime", 0x3E7D1B, "myr", 0x6B3E8E, "lav", 0x7B5EA7, "pin", 0x2E6B5A,
            "cream", 0x9C8A5A, "gold", 0xA07818, "red", 0xA03020, "gray", 0x6E6A60);
    /** Prefix of a link's click value: the screen turns it back into an entry id. */
    public static final String LINK_PREFIX = "fourtwenty-entry:";

    private final Font font;
    private final Map<String, int[]> imageSizes = new HashMap<>();

    /** Item pictures drawn this frame (for tooltips) and lines of text drawn (for link clicks). */
    public record Hover(int x, int y, int w, int h, ItemStack stack) {}
    public record DrawnLine(int x, int y, FormattedCharSequence line) {}
    public final List<Hover> hovers = new ArrayList<>();
    public final List<DrawnLine> lines = new ArrayList<>();

    public PageRenderer(Font font) {
        this.font = font;
    }

    public void beginFrame() {
        hovers.clear();
        lines.clear();
    }

    // ---- text

    public static Component component(List<Span> spans) {
        MutableComponent out = Component.empty();
        for (Span s : spans) {
            Style style = Style.EMPTY.withColor(TextColor.fromRgb(INK & 0xFFFFFF));
            if (s.bold()) style = style.withBold(true);
            if (s.italic()) style = style.withItalic(true);
            if (s.colour() != null && COLOURS.containsKey(s.colour())) style = style.withColor(TextColor.fromRgb(COLOURS.get(s.colour())));
            if (s.link() != null) style = style.withColor(TextColor.fromRgb(LINK & 0xFFFFFF)).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, LINK_PREFIX + s.link()));
            if (s.spoiler() != null && !TerpeneKnowledge.knows(s.spoiler())) style = style.withObfuscated(true);
            out.append(Component.literal(s.text()).withStyle(style));
        }
        return out;
    }

    public List<FormattedCharSequence> wrap(List<Span> spans, int width) {
        return font.split(component(spans), width);
    }

    public Layout.Measure measure() {
        return new Layout.Measure() {
            @Override public int lines(List<Span> spans, int width) { return Math.max(1, wrap(spans, width).size()); }
            @Override public int lineHeight() { return LINE; }
            @Override public int height(Element e, int width) { return PageRenderer.this.height(e, width); }
        };
    }

    int height(Element e, int width) {
        return switch (e) {
            case Element.Item it -> Math.max(20, wrap(caption(it.caption()), width - 22).size() * LINE + 2);
            case Element.Recipe r -> RecipeView.HEIGHT;
            case Element.Image im -> {
                int[] wh = imageSize(im.path());
                int w = imageWidth(im.size(), width);
                int h = wh == null ? 20 : Math.min(140, w * wh[1] / Math.max(1, wh[0]));
                yield h + (im.caption().isEmpty() ? 0 : wrap(caption(im.caption()), width).size() * LINE + 2);
            }
            case Element.Table t -> t.special().isEmpty() ? t.rows().size() * LINE + 4 : 0;
            case Element.Truth t -> (truth(t.gate()).size()) * LINE + 4;
            default -> LINE;
        };
    }

    private static List<Span> caption(String text) {
        return List.of(new Span(text, false, true, null, null, null));
    }

    // ---- drawing

    /** Draws one placed element (or slice) with its top-left at x, y on a page of this width. */
    public void draw(GuiGraphics g, Layout.Placed p, int x, int y, int width) {
        int top = y + p.y();
        switch (p.element()) {
            case Element.Heading h -> {
                for (FormattedCharSequence line : font.split(component(h.spans()).copy().withStyle(s -> s.withBold(true)
                        .withColor(TextColor.fromRgb(HEADING & 0xFFFFFF))), width)) {
                    text(g, line, x, top);
                    top += LINE;
                }
            }
            case Element.Para para -> {
                List<FormattedCharSequence> all = wrap(para.spans(), width);
                for (int i = p.from(); i < p.to() && i < all.size(); i++) {
                    text(g, all.get(i), x, top);
                    top += LINE;
                }
            }
            case Element.ListItems l -> {
                for (int i = p.from(); i < p.to(); i++) {
                    g.drawString(font, "•", x + 2, top, LINK, false);
                    for (FormattedCharSequence line : wrap(l.items().get(i), width - Layout.INDENT)) {
                        text(g, line, x + Layout.INDENT, top);
                        top += LINE;
                    }
                }
            }
            case Element.Steps s -> {
                for (int i = p.from(); i < p.to(); i++) {
                    String n = (s.start() + i) + ".";
                    g.drawString(font, n, x + Layout.INDENT - 2 - font.width(n), top, HEADING, false);
                    for (FormattedCharSequence line : wrap(s.items().get(i), width - Layout.INDENT)) {
                        text(g, line, x + Layout.INDENT, top);
                        top += LINE;
                    }
                }
            }
            case Element.Item it -> {
                ItemStack stack = stack(it.id());
                g.renderItem(stack, x + 1, top + 1);
                hovers.add(new Hover(x + 1, top + 1, 16, 16, stack));
                int ty = top + (wrap(caption(it.caption()), width - 22).size() == 1 ? 5 : 1);
                for (FormattedCharSequence line : wrap(caption(it.caption()), width - 22)) {
                    text(g, line, x + 22, ty);
                    ty += LINE;
                }
            }
            case Element.Recipe r -> RecipeView.draw(g, font, r.id(), x + (width - RecipeView.WIDTH) / 2, top, hovers);
            case Element.Image im -> {
                int[] wh = imageSize(im.path());
                int w = imageWidth(im.size(), width);
                int h = wh == null ? 20 : Math.min(140, w * wh[1] / Math.max(1, wh[0]));
                int ix = x + (width - w) / 2;
                if (wh != null) g.blit(image(im.path()), ix, top, w, h, 0, 0, wh[0], wh[1], wh[0], wh[1]);
                g.renderOutline(ix - 1, top - 1, w + 2, h + 2, RULE);
                int ty = top + h + 2;
                for (FormattedCharSequence line : wrap(caption(im.caption()), width)) {
                    g.drawString(font, line, x + (width - font.width(line)) / 2, ty, FAINT, false);
                    ty += LINE;
                }
            }
            case Element.Table t -> {
                if (t.special().isEmpty()) table(g, t.rows(), x, top, width);
            }
            case Element.Truth t -> table(g, truth(t.gate()), x, top, width);
            default -> {}
        }
    }

    private void text(GuiGraphics g, FormattedCharSequence line, int x, int y) {
        g.drawString(font, line, x, y, INK, false);
        lines.add(new DrawnLine(x, y, line));
    }

    private void table(GuiGraphics g, List<List<String>> rows, int x, int y, int width) {
        if (rows.isEmpty()) return;
        int cols = rows.stream().mapToInt(List::size).max().orElse(1), cw = width / cols;
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                String cell = font.plainSubstrByWidth(row.get(c), cw - 3);
                Component text = r == 0 ? Component.literal(cell).withStyle(s -> s.withBold(true)) : Component.literal(cell);
                g.drawString(font, text, x + c * cw + 2, y + 2 + r * LINE, r == 0 ? HEADING : INK, false);
            }
            if (r == 0) g.fill(x, y + LINE + 1, x + width, y + LINE + 2, RULE);
        }
        g.renderOutline(x, y, width, rows.size() * LINE + 3, RULE);
    }

    /** The truth table of a gate: header, then every input combination. */
    public static List<List<String>> truth(String gate) {
        List<List<String>> rows = new ArrayList<>();
        if (gate.equals("NOT")) {
            rows.add(List.of("A", "Q"));
            rows.add(List.of("0", "1"));
            rows.add(List.of("1", "0"));
            return rows;
        }
        rows.add(List.of("A", "B", "Q"));
        for (int a = 0; a <= 1; a++) {
            for (int b = 0; b <= 1; b++) {
                boolean q = switch (gate) {
                    case "AND" -> a == 1 && b == 1;
                    case "OR" -> a == 1 || b == 1;
                    case "NAND" -> !(a == 1 && b == 1);
                    case "NOR" -> !(a == 1 || b == 1);
                    case "XOR" -> a != b;
                    case "XNOR" -> a == b;
                    default -> false;
                };
                rows.add(List.of(String.valueOf(a), String.valueOf(b), q ? "1" : "0"));
            }
        }
        return rows;
    }

    // ---- helpers

    public static ItemStack stack(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(rl));
    }

    private static int imageWidth(String size, int width) {
        return switch (size) {
            case "wide" -> width;
            case "small" -> width / 2;
            default -> width * 3 / 4;
        };
    }

    private static ResourceLocation image(String path) {
        return ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/handbook/" + path + ".png");
    }

    private int[] imageSize(String path) {
        return imageSizes.computeIfAbsent(path, p -> {
            var res = Minecraft.getInstance().getResourceManager().getResource(image(p));
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                return new int[]{img.getWidth(), img.getHeight()};
            } catch (Exception e) {
                return null;
            }
        });
    }
}
