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
    /** Open honour requirements drawn this frame: clicking the box signs it. */
    public record SignBox(int x, int y, int w, int h, String key) {}
    public final List<SignBox> signBoxes = new ArrayList<>();

    public PageRenderer(Font font) {
        this.font = font;
    }

    public void beginFrame() {
        hovers.clear();
        lines.clear();
        signBoxes.clear();
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
                int h = wh == null ? 20 : Math.min(220, w * wh[1] / Math.max(1, wh[0]));
                yield h + (im.caption().isEmpty() ? 0 : wrap(caption(im.caption()), width).size() * LINE + 2);
            }
            case Element.Heading h -> headingLines(h, width).size() * LINE;
            case Element.Scouting s -> scoutHeight(s.ref(), width);
            case Element.Table t -> t.special().isEmpty() ? tableHeight(t.rows(), width) : 0;
            case Element.Truth t -> tableHeight(truth(t.gate()), width);
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
                for (FormattedCharSequence line : headingLines(h, width)) {
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
                int h = wh == null ? 20 : Math.min(220, w * wh[1] / Math.max(1, wh[0]));
                int ix = x + (width - w) / 2;
                if (wh != null) {
                    // smooth filtering: scenes are drawn large (4x) and scaled to the page, so they stay crisp at any book size
                    Minecraft.getInstance().getTextureManager().getTexture(image(im.path())).setFilter(true, false);
                    g.blit(image(im.path()), ix, top, w, h, 0, 0, wh[0], wh[1], wh[0], wh[1]);
                }
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
            case Element.Scouting s -> scout(g, s.ref(), x, top, width);
            default -> {}
        }
    }

    // ---- ranks and badges

    private static com.malevolentgods.fourtwenty.domain.handbook.Scouting.Def scoutDef(String ref) {
        int slash = ref.indexOf('/');
        if (slash < 0) return null;
        boolean rank = ref.startsWith("rank/");
        String id = ref.substring(slash + 1);
        for (var d : com.malevolentgods.fourtwenty.handbook.ScoutingData.defs()) if (d.rank() == rank && d.id().equals(id)) return d;
        return null;
    }

    private Component reqText(com.malevolentgods.fourtwenty.domain.handbook.Scouting.Def d, com.malevolentgods.fourtwenty.domain.handbook.Scouting.Req r, boolean live) {
        var progress = ProgressClient.get();
        String key = d.key(r);
        net.minecraft.network.chat.MutableComponent t = Component.literal(r.n() + ". ").append(Component.translatable(ProgressClient.reqKey(key)));
        if (r.count() > 1) t.append(Component.literal(" (" + (live ? progress.count(key) : 0) + "/" + r.count() + ")"));
        if (r.kind() == com.malevolentgods.fourtwenty.domain.handbook.Scouting.Kind.HONOUR && (!live || progress.state(key) == 0)) {
            t.append(Component.literal(" ").append(Component.translatable("handbook.fourtwenty.sign").withStyle(s -> s.withItalic(true))));
        }
        return t;
    }

    private static final int PATCH = 28, CELL_H = 52, COLS = 4;

    private int scoutHeight(String ref, int width) {
        if (ref.equals("sash")) {
            long badges = com.malevolentgods.fourtwenty.handbook.ScoutingData.defs().stream().filter(d -> !d.rank()).count();
            return (int) ((badges + COLS - 1) / COLS) * CELL_H;
        }
        if (ref.equals("overview")) {
            var defs = com.malevolentgods.fourtwenty.handbook.ScoutingData.defs();
            int ranks = (int) defs.stream().filter(d -> d.rank()).count();
            return (ranks + 1 + defs.size() - ranks) * LINE + LINE * 3;
        }
        var d = scoutDef(ref);
        if (d == null) return LINE;
        int h = LINE + 2;
        for (var r : d.reqs()) h += Math.max(1, font.split(reqText(d, r, false), width - 13).size()) * LINE + 2;
        return h;
    }

    private void scout(GuiGraphics g, String ref, int x, int top, int width) {
        var progress = ProgressClient.get();
        if (ref.equals("sash")) {
            int i = 0, cw = width / COLS;
            for (var d : com.malevolentgods.fourtwenty.handbook.ScoutingData.defs()) {
                if (d.rank()) continue;
                int cx = x + (i % COLS) * cw, cy = top + (i / COLS) * CELL_H;
                boolean earned = progress.earned(d.earnKey());
                var tex = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/handbook/patches/" + d.id() + ".png");
                if (!earned) g.setColor(0.35f, 0.35f, 0.35f, 0.7f);
                g.blit(tex, cx + (cw - PATCH) / 2, cy, PATCH, PATCH, 0, 0, 16, 16, 16, 16);
                g.setColor(1f, 1f, 1f, 1f);
                int ty = cy + PATCH + 2;
                for (FormattedCharSequence line : font.split(Component.translatable("handbook.fourtwenty.badge." + d.id()), cw - 2)) {
                    if (ty > cy + CELL_H - LINE + 2) break;
                    g.drawString(font, line, cx + (cw - font.width(line)) / 2, ty, earned ? INK : FAINT, false);
                    ty += LINE;
                }
                i++;
            }
            return;
        }
        if (ref.equals("overview")) {
            int y = top;
            var defs = com.malevolentgods.fourtwenty.handbook.ScoutingData.defs();
            g.drawString(font, Component.translatable("handbook.fourtwenty.overview.ranks").withStyle(s -> s.withBold(true)), x, y, HEADING, false);
            y += LINE + 2;
            for (var d : defs) {
                if (!d.rank()) continue;
                checkbox(g, x, y, progress.earned(d.earnKey()) ? 1 : 0);
                g.drawString(font, Component.translatable("handbook.fourtwenty.rank." + d.id()), x + 13, y, INK, false);
                y += LINE;
            }
            y += LINE;
            g.drawString(font, Component.translatable("handbook.fourtwenty.overview.badges").withStyle(s -> s.withBold(true)), x, y, HEADING, false);
            y += LINE + 2;
            for (var d : defs) {
                if (d.rank()) continue;
                checkbox(g, x, y, progress.earned(d.earnKey()) ? 1 : 0);
                long done = d.reqs().stream().filter(r -> progress.satisfied(d.key(r))).count();
                g.drawString(font, Component.translatable("handbook.fourtwenty.badge." + d.id()).append(Component.literal("  " + done + "/" + d.reqs().size())),
                        x + 13, y, INK, false);
                y += LINE;
            }
            return;
        }
        var d = scoutDef(ref);
        if (d == null) return;
        int y = top;
        long done = d.reqs().stream().filter(r -> progress.satisfied(d.key(r))).count();
        Component status = progress.earned(d.earnKey()) ? Component.translatable("handbook.fourtwenty.earned") : Component.literal(done + " / " + d.reqs().size());
        g.drawString(font, status, x, y, progress.earned(d.earnKey()) ? LINK : FAINT, false);
        y += LINE + 2;
        for (var r : d.reqs()) {
            String key = d.key(r);
            int state = progress.state(key);
            checkbox(g, x, y, state);
            if (r.kind() == com.malevolentgods.fourtwenty.domain.handbook.Scouting.Kind.HONOUR && state == 0) signBoxes.add(new SignBox(x - 1, y - 1, 11, 11, key));
            int ty = y;
            for (FormattedCharSequence line : font.split(reqText(d, r, true), width - 13)) {
                g.drawString(font, line, x + 13, ty, state == 0 ? INK : 0xFF5E8A48, false);
                ty += LINE;
            }
            y = Math.max(ty, y + LINE) + 2;
        }
    }

    /** A little box: empty, or ticked (done), or ticked in a darker hand (signed on honour). */
    private void checkbox(GuiGraphics g, int x, int y, int state) {
        g.renderOutline(x, y, 9, 9, INK);
        if (state == 0) return;
        int c = state == 2 ? 0xFF2F5A1A : 0xFF3E9A1A;
        int[][] px = {{2, 4}, {3, 5}, {4, 6}, {5, 5}, {6, 4}, {7, 3}, {8, 2}, {3, 4}, {4, 5}, {5, 4}, {6, 3}, {7, 2}};
        for (int[] p : px) g.fill(x + p[0] - 1, y + p[1] - 1, x + p[0], y + p[1], c);
    }

    /** A heading is bold, so it wraps (and is measured) as bold. */
    private List<FormattedCharSequence> headingLines(Element.Heading h, int width) {
        return font.split(component(h.spans()).copy().withStyle(s -> s.withBold(true).withColor(TextColor.fromRgb(HEADING & 0xFFFFFF))), width);
    }

    private int tableHeight(List<List<String>> rows, int width) {
        int cols = rows.stream().mapToInt(List::size).max().orElse(1), cw = width / Math.max(1, cols), h = 0;
        for (List<String> row : rows) h += rowLines(row, cw) * LINE;
        return h + 4;
    }

    private int rowLines(List<String> row, int cw) {
        int n = 1;
        for (String c : row) n = Math.max(n, font.split(Component.literal(c), cw - 4).size());
        return n;
    }

    private void text(GuiGraphics g, FormattedCharSequence line, int x, int y) {
        g.drawString(font, line, x, y, INK, false);
        lines.add(new DrawnLine(x, y, line));
    }

    /** A table whose cells wrap rather than run off the page. */
    private void table(GuiGraphics g, List<List<String>> rows, int x, int y, int width) {
        if (rows.isEmpty()) return;
        int cols = rows.stream().mapToInt(List::size).max().orElse(1), cw = width / cols, ry = y + 2;
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                Component text = r == 0 ? Component.literal(row.get(c)).withStyle(s -> s.withBold(true)) : Component.literal(row.get(c));
                int ly = ry;
                for (FormattedCharSequence line : font.split(text, cw - 4)) {
                    g.drawString(font, line, x + c * cw + 2, ly, r == 0 ? HEADING : INK, false);
                    ly += LINE;
                }
            }
            ry += rowLines(row, cw) * LINE;
            if (r == 0) g.fill(x, ry - 1, x + width, ry, RULE);
        }
        g.renderOutline(x, y, width, ry - y + 1, RULE);
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
