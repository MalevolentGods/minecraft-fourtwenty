package com.malevolentgods.fourtwenty.client.handbook;

import com.malevolentgods.fourtwenty.domain.handbook.Element;
import com.malevolentgods.fourtwenty.domain.handbook.Layout;
import com.malevolentgods.fourtwenty.domain.handbook.Span;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Handbook of Weedcraft, Scouting, and Weed-craft (specs/016-weedcraft-handbook US1): the closed book with the user's
 * cover art, then two-page spreads: the motto, KEEP ONE BACK, alone inside the cover (FR-002b), the title page, contents, every entry
 * (each starting on a fresh page). Links, contents lines and the back button move about; the bookmark remembers the
 * spread (FR-002, FR-003, FR-005).
 */
public class HandbookScreen extends Screen {
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/handbook/book.png");
    private static final ResourceLocation COVER = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/handbook/cover.png");
    /** The spread in book units, each drawn as a whole number of screen pixels so the text stays crisp. */
    static final int BOOK_W = 540, BOOK_H = 345, PAGE_W = 224, PAGE_H = 280, LEFT_X = 27, RIGHT_X = 289, PAGE_Y = 28;
    private static final int COVER_W = 266, COVER_H = 345, TAB_H = 16, SHEET_W = 1024, SHEET_H = 512, BTN_V = 400;

    /** One page of the book. */
    sealed interface Page permits InsideCover, Title, Contents, ChapterPage, EntryPage, Blank {}
    record InsideCover() implements Page {}
    /** A chapter's own opening page (FR-002c): "Chapter N", its title, its subtitle and its painting; number 0 for the unnumbered back. */
    record ChapterPage(int number, String title, String subtitle, String opener) implements Page {}
    record Title() implements Page {}
    record ContentsLine(String text, String entry, boolean chapter) {}
    record Contents(List<ContentsLine> lines) implements Page {}
    record EntryPage(String entry, List<Layout.Placed> placed, int number) implements Page {}
    record Blank() implements Page {}

    private final List<Page> pages = new ArrayList<>();
    private final Map<String, Integer> entryPage = new HashMap<>();
    private final Deque<Integer> back = new ArrayDeque<>();
    /** 0 is the closed cover; spread k (k >= 1) shows pages 2k-2 and 2k-1. */
    private int spread;
    private PageRenderer renderer;
    private float scale = 1;
    private int left, top;
    private final List<int[]> contentsHits = new ArrayList<>();
    private final List<String> contentsTargets = new ArrayList<>();

    public HandbookScreen() {
        super(Component.translatable("handbook.fourtwenty.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new HandbookScreen());
    }

    @Override
    protected void init() {
        renderer = new PageRenderer(font);
        build();
        // As big as the screen allows, at a whole number of real pixels per book unit (the user, 2026-11-18: more resolution)
        var window = minecraft.getWindow();
        double gui = window.getGuiScale(), fit = Math.min(window.getWidth() * 0.96 / BOOK_W, window.getHeight() * 0.94 / (BOOK_H + TAB_H + 18));
        double ppu = fit >= 1 ? Math.floor(fit) : fit;
        scale = (float) (ppu / gui);
        left = (int) ((width - BOOK_W * scale) / 2);
        top = (int) ((height - (BOOK_H + TAB_H + 18) * scale) / 2 + TAB_H * scale);
        spread = Math.max(0, Math.min(Bookmark.spread(), spreads() - 1));
    }

    // ---- building the pages

    private void build() {
        pages.clear();
        entryPage.clear();
        BookData.Book book = BookData.book();
        pages.add(new InsideCover());
        pages.add(new Title());
        // contents: chapter lines and their entries, paginated by line count
        List<ContentsLine> all = new ArrayList<>();
        for (BookData.Chapter c : book.chapters()) {
            if (c.entries().isEmpty()) continue;
            all.add(new ContentsLine(Component.translatable(c.title()).getString(), CHAPTER + c.id(), true));
            for (String id : c.entries()) all.add(new ContentsLine(book.entries().get(id).front().title(), id, false));
        }
        if (book.chapters().stream().noneMatch(c -> c.id().equals("back"))) {
            all.add(new ContentsLine(Component.translatable("handbook.fourtwenty.chapter.back").getString(), GLOSSARY, true));
        }
        if (!book.glossary().isEmpty()) all.add(new ContentsLine(Component.translatable("handbook.fourtwenty.glossary").getString(), GLOSSARY, false));
        all.add(new ContentsLine(Component.translatable("handbook.fourtwenty.index").getString(), INDEX, false));
        int perPage = PAGE_H / PageRenderer.LINE - 2;
        for (int i = 0; i < Math.max(1, all.size()); i += perPage) {
            pages.add(new Contents(all.isEmpty() ? List.of() : all.subList(i, Math.min(all.size(), i + perPage))));
        }
        Layout.Measure m = renderer.measure();
        // a chapter opens on a page of its own (FR-002c), on the left so its first page faces it; its entries then run on one
        // after another, like a textbook's sections
        int number = 0;
        for (BookData.Chapter c : book.chapters()) {
            List<Layout.Section> sections = new ArrayList<>();
            for (String id : c.entries()) {
                BookData.Entry e = book.entries().get(id);
                if (e == null) continue;
                sections.add(new Layout.Section(id, withHeading(e.front().title(), e.elements())));
            }
            if (sections.isEmpty()) continue;
            boolean numbered = !c.id().equals("back");
            if (numbered) number++;
            if (pages.size() % 2 == 1) pages.add(new Blank());
            entryPage.put(CHAPTER + c.id(), pages.size());
            String subtitle = c.title() + ".subtitle";
            pages.add(new ChapterPage(numbered ? number : 0, Component.translatable(c.title()).getString(),
                    I18n.exists(subtitle) ? I18n.get(subtitle) : "", c.opener()));
            Layout.Run run = Layout.run(sections, PAGE_W, PAGE_H, m);
            int base = pages.size();
            run.starts().forEach((id, page) -> entryPage.put(id, base + page));
            String owner = sections.get(0).id();
            for (int i = 0; i < run.pages().size(); i++) {
                for (var start : run.starts().entrySet()) if (start.getValue() == i) { owner = start.getKey(); break; }
                pages.add(new EntryPage(owner, run.pages().get(i), pages.size()));
            }
        }
        // the back of the book: glossary and index (FR-006)
        if (!book.glossary().isEmpty()) {
            addEntry(GLOSSARY, Component.translatable("handbook.fourtwenty.glossary").getString(),
                    com.malevolentgods.fourtwenty.domain.handbook.Contents.glossaryElements(book.glossary()), m);
        }
        java.util.Map<String, List<String>> terms = new java.util.LinkedHashMap<>();
        book.entries().forEach((id, e) -> terms.put(id, e.front().terms()));
        addEntry(INDEX, Component.translatable("handbook.fourtwenty.index").getString(),
                com.malevolentgods.fourtwenty.domain.handbook.Contents.indexElements(com.malevolentgods.fourtwenty.domain.handbook.Contents.index(terms)), m);
        if (pages.size() % 2 == 1) pages.add(new Blank());
    }

    static final String GLOSSARY = "back/glossary", INDEX = "back/index", CHAPTER = "chapter:";

    private static List<Element> withHeading(String title, List<Element> elements) {
        List<Element> els = new ArrayList<>();
        els.add(new Element.Heading(List.of(new Span(title, true, false, null, null, null))));
        els.addAll(elements);
        return els;
    }

    private void addEntry(String id, String title, List<Element> elements, Layout.Measure m) {
        List<Element> els = withHeading(title, elements);
        entryPage.put(id, pages.size());
        for (List<Layout.Placed> page : Layout.paginate(els, PAGE_W, PAGE_H, m)) {
            pages.add(new EntryPage(id, page, pages.size()));
        }
    }

    private int spreads() {
        return 1 + pages.size() / 2;
    }

    // ---- drawing

    @Override
    public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        renderer.beginFrame();
        contentsHits.clear();
        contentsTargets.clear();
        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1);
        int mx = (int) ((mouseX - left) / scale), my = (int) ((mouseY - top) / scale);
        if (spread == 0) {
            int cx = (BOOK_W - COVER_W) / 2;           // the closed book: the user's cover art, at its own shape
            g.blit(COVER, cx, 0, COVER_W, COVER_H, 0, 0, 532, 690, 532, 690);
            g.drawCenteredString(font, Component.translatable("handbook.fourtwenty.open"), BOOK_W / 2, BOOK_H + 4, 0xFFE8DDB5);
        } else {
            g.blit(BOOK, 0, 0, 0, 0, BOOK_W, BOOK_H + 22, SHEET_W, SHEET_H);
            tabs(g, mx, my);
            int i = 2 * spread - 2;
            drawPage(g, pages.get(i), LEFT_X, i);
            if (i + 1 < pages.size()) drawPage(g, pages.get(i + 1), RIGHT_X, i + 1);
            button(g, NEXT_X, BTN_Y, 0, 23, mx, my, spread < spreads() - 1);
            button(g, PREV_X, BTN_Y, 46, 69, mx, my, true);
            if (!back.isEmpty()) {
                boolean hover = in(mx, my, BACK_X, BTN_Y, 13, 11);
                g.blit(BOOK, BACK_X, BTN_Y, hover ? 107 : 92, BTN_V, 13, 11, SHEET_W, SHEET_H);
            }
        }
        for (PageRenderer.Hover h : renderer.hovers) {
            if (in(mx, my, h.x(), h.y(), h.w(), h.h()) && !h.stack().isEmpty()) {
                g.renderTooltip(font, h.stack(), mx, my);
                break;
            }
        }
        g.pose().popPose();
    }

    private void button(GuiGraphics g, int x, int y, int u, int uHover, int mx, int my, boolean shown) {
        if (!shown) return;
        g.blit(BOOK, x, y, in(mx, my, x, y, 18, 10) ? uHover : u, BTN_V, 18, 10, SHEET_W, SHEET_H);
    }

    private static final int NEXT_X = BOOK_W - 42, PREV_X = 24, BACK_X = BOOK_W / 2 - 34, BTN_Y = BOOK_H - 26;

    /** The bookmark tabs over the right page: Contents, Glossary, Index, open from anywhere (the user, 2026-11-18). */
    private record Tab(String key, int x, int w) {}
    private final List<Tab> tabs = new ArrayList<>();

    private void tabs(GuiGraphics g, int mx, int my) {
        tabs.clear();
        int x = RIGHT_X + 8;
        for (String key : List.of("handbook.fourtwenty.contents", "handbook.fourtwenty.glossary", "handbook.fourtwenty.index")) {
            Component label = Component.translatable(key);
            int w = font.width(label) + 14;
            boolean here = isAt(key), hover = in(mx, my, x, -TAB_H, w, TAB_H);
            int body = here ? 0xFFF4EAD0 : hover ? 0xFFE8DCB8 : 0xFFD9C9A0;
            g.fill(x, -TAB_H + (here ? 0 : 2), x + w, 6, body);
            g.renderOutline(x, -TAB_H + (here ? 0 : 2), w, TAB_H + (here ? 6 : 4), 0xFF263A24);
            g.drawString(font, label, x + 7, -TAB_H + (here ? 4 : 6), here ? PageRenderer.HEADING : PageRenderer.INK, false);
            tabs.add(new Tab(key, x, w));
            x += w + 4;
        }
    }

    private boolean isAt(String tabKey) {
        int i = 2 * spread - 2;
        if (spread == 0 || i >= pages.size()) return false;
        Page p = pages.get(i);
        return switch (tabKey) {
            case "handbook.fourtwenty.contents" -> p instanceof Contents;
            case "handbook.fourtwenty.glossary" -> p instanceof EntryPage e && e.entry().equals(GLOSSARY);
            default -> p instanceof EntryPage e && e.entry().equals(INDEX);
        };
    }

    private void openTab(String key) {
        switch (key) {
            case "handbook.fourtwenty.contents" -> { back.push(spread); spread = 2; sound(); }
            case "handbook.fourtwenty.glossary" -> go(GLOSSARY);
            default -> go(INDEX);
        }
    }

    private void drawPage(GuiGraphics g, Page page, int x, int index) {
        switch (page) {
            case InsideCover d -> {
                // the motto, alone, one word to a line, as big as the page allows
                String[] words = Component.translatable("handbook.fourtwenty.motto").getString().split(" ");
                int widest = 1;
                for (String w : words) widest = Math.max(widest, font.width(w));
                float s = Math.min(Math.min(4.5f, PAGE_W * 0.9f / widest), PAGE_H * 0.8f / (words.length * 10));
                float y0 = PAGE_Y + PAGE_H / 2f - words.length * 10 * s / 2;
                for (int k = 0; k < words.length; k++) {
                    g.pose().pushPose();
                    g.pose().translate(x + PAGE_W / 2f, y0 + k * 10 * s, 0);
                    g.pose().scale(s, s, 1);
                    g.drawString(font, words[k], -font.width(words[k]) / 2, 0, 0xFF2E7A1A, false);
                    g.pose().popPose();
                }
            }
            case Title t -> {
                int y = PAGE_Y + 60;
                for (String key : List.of("handbook.fourtwenty.title.line1", "handbook.fourtwenty.title.line2", "handbook.fourtwenty.title.line3")) {
                    boolean big = key.endsWith("line2");
                    g.pose().pushPose();
                    Component c = Component.translatable(key);
                    float s = Math.min(big ? 3f : 1.6f, PAGE_W * 0.94f / Math.max(1, font.width(c)));     // never past the page edge
                    g.pose().translate(x + PAGE_W / 2f, y, 0);
                    g.pose().scale(s, s, 1);
                    g.drawString(font, c, -font.width(c) / 2, 0, big ? 0xFF2F5A1A : PageRenderer.INK, false);
                    g.pose().popPose();
                    y += (int) (10 * s) + 8;
                }
                g.fill(x + 30, y + 4, x + PAGE_W - 30, y + 5, PageRenderer.RULE);
                Component by = Component.translatable("handbook.fourtwenty.author").withStyle(s -> s.withItalic(true));
                g.drawString(font, by, x + (PAGE_W - font.width(by)) / 2, y + 14, PageRenderer.FAINT, false);
            }
            case Contents c -> {
                int y = PAGE_Y;
                if (index == 2) {
                    Component head = Component.translatable("handbook.fourtwenty.contents").withStyle(s -> s.withBold(true));
                    g.drawString(font, head, x + (PAGE_W - font.width(head)) / 2, y, PageRenderer.HEADING, false);
                    y += PageRenderer.LINE * 2;
                }
                for (ContentsLine line : c.lines()) {
                    int indent = line.chapter() ? 0 : 8;
                    String text = font.plainSubstrByWidth(line.text(), PAGE_W - indent - 18);
                    Component comp = line.chapter() ? Component.literal(text).withStyle(s -> s.withBold(true)) : Component.literal(text);
                    g.drawString(font, comp, x + indent, y, line.chapter() ? PageRenderer.HEADING : PageRenderer.INK, false);
                    Integer p = entryPage.get(line.entry());
                    if (!line.chapter() && p != null) {
                        String num = String.valueOf(p + 1);
                        g.drawString(font, num, x + PAGE_W - font.width(num), y, PageRenderer.FAINT, false);
                    }
                    contentsHits.add(new int[]{x, y, PAGE_W, PageRenderer.LINE});
                    contentsTargets.add(line.entry());
                    y += PageRenderer.LINE;
                }
            }
            case ChapterPage cp -> {
                int y = PAGE_Y + (cp.opener() == null ? 90 : 14);
                if (cp.number() > 0) {
                    Component n = Component.translatable("handbook.fourtwenty.chapter_number", cp.number());
                    g.pose().pushPose();
                    g.pose().translate(x + PAGE_W / 2f, y, 0);
                    g.pose().scale(1.25f, 1.25f, 1);
                    g.drawString(font, n, -font.width(n) / 2, 0, PageRenderer.FAINT, false);
                    g.pose().popPose();
                    y += 19;
                }
                float s = 2.0f;                                    // the title, big, wrapped if it must be
                for (FormattedCharSequence line : font.split(Component.literal(cp.title()), (int) (PAGE_W * 0.94f / s))) {
                    g.pose().pushPose();
                    g.pose().translate(x + PAGE_W / 2f, y, 0);
                    g.pose().scale(s, s, 1);
                    g.drawString(font, line, -font.width(line) / 2, 0, 0xFF2F5A1A, false);
                    g.pose().popPose();
                    y += (int) (10 * s);
                }
                y += 3;
                g.fill(x + 50, y, x + PAGE_W - 50, y + 1, PageRenderer.RULE);
                y += 7;
                if (!cp.subtitle().isEmpty()) {
                    // balanced lines: as wide as the text needs, so no lone word is left on the last line
                    Component sub = Component.literal(cp.subtitle()).withStyle(st -> st.withItalic(true));
                    int room = PAGE_W - 24, lines = font.split(sub, room).size();
                    int wrapAt = font.width(sub) / lines;
                    while (wrapAt < room && font.split(sub, wrapAt).size() > lines) wrapAt += 2;
                    for (FormattedCharSequence line : font.split(sub, wrapAt)) {
                        g.drawString(font, line, x + (PAGE_W - font.width(line)) / 2, y, PageRenderer.INK, false);
                        y += PageRenderer.LINE;
                    }
                }
                int[] wh = cp.opener() == null ? null : renderer.imageSize(cp.opener());
                if (wh != null) {                                  // the painting fills what's left, in proportion
                    int top = y + 10, avail = PAGE_Y + PAGE_H - top;
                    int w = PAGE_W, h = w * wh[1] / Math.max(1, wh[0]);
                    if (h > avail) {
                        h = avail;
                        w = h * wh[0] / Math.max(1, wh[1]);
                    }
                    ResourceLocation tex = PageRenderer.image(cp.opener());
                    Minecraft.getInstance().getTextureManager().getTexture(tex).setFilter(true, false);
                    g.blit(tex, x + (PAGE_W - w) / 2, top, w, h, 0, 0, wh[0], wh[1], wh[0], wh[1]);
                }
            }
            case EntryPage e -> {
                for (Layout.Placed p : e.placed()) renderer.draw(g, p, x, PAGE_Y, PAGE_W);
                String num = String.valueOf(e.number() + 1);
                g.drawString(font, num, x + PAGE_W / 2 - font.width(num) / 2, PAGE_Y + PAGE_H + 8, PageRenderer.FAINT, false);
            }
            case Blank b -> {}
        }
    }

    // ---- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) ((mouseX - left) / scale), my = (int) ((mouseY - top) / scale);
        if (spread == 0) { turn(1); return true; }      // the closed cover opens on a click
        for (var box : renderer.signBoxes) {
            if (in(mx, my, box.x(), box.y(), box.w(), box.h())) { ProgressClient.sign(box.key()); return true; }
        }
        for (Tab t : tabs) {
            if (in(mx, my, t.x(), -TAB_H, t.w(), TAB_H)) { openTab(t.key()); return true; }
        }
        if (in(mx, my, NEXT_X, BTN_Y, 18, 10)) { turn(1); return true; }
        if (in(mx, my, PREV_X, BTN_Y, 18, 10)) { turn(-1); return true; }
        if (!back.isEmpty() && in(mx, my, BACK_X, BTN_Y, 13, 11)) { goBack(); return true; }
        for (int i = 0; i < contentsHits.size(); i++) {
            int[] r = contentsHits.get(i);
            if (in(mx, my, r[0], r[1], r[2], r[3])) { go(contentsTargets.get(i)); return true; }
        }
        for (PageRenderer.DrawnLine line : renderer.lines) {
            if (my >= line.y() && my < line.y() + PageRenderer.LINE && mx >= line.x()) {
                Style style = font.getSplitter().componentStyleAtWidth(line.line(), mx - line.x());
                if (style != null && style.getClickEvent() != null && style.getClickEvent().getAction() == ClickEvent.Action.CHANGE_PAGE
                        && style.getClickEvent().getValue().startsWith(PageRenderer.LINK_PREFIX)) {
                    go(style.getClickEvent().getValue().substring(PageRenderer.LINK_PREFIX.length()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) { turn(1); return true; }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) { turn(-1); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !back.isEmpty()) { goBack(); return true; }
        if (spread > 0 && key == GLFW.GLFW_KEY_HOME) { openTab("handbook.fourtwenty.contents"); return true; }
        if (spread > 0 && key == GLFW.GLFW_KEY_G) { openTab("handbook.fourtwenty.glossary"); return true; }
        if (spread > 0 && key == GLFW.GLFW_KEY_I) { openTab("handbook.fourtwenty.index"); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        turn(dy < 0 ? 1 : -1);
        return true;
    }

    private void turn(int by) {
        int to = Math.max(0, Math.min(spreads() - 1, spread + by));
        if (to != spread) {
            spread = to;
            sound();
        }
    }

    private void go(String entry) {
        Integer p = entryPage.get(entry);
        if (p == null) return;
        back.push(spread);
        spread = p / 2 + 1;
        sound();
    }

    private void goBack() {
        spread = back.pop();
        sound();
    }

    private void sound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() {
        Bookmark.save(spread);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** For tests and tools: the lines of text a page would show. */
    List<FormattedCharSequence> linesOf(List<Span> spans) {
        return renderer.wrap(spans, PAGE_W);
    }
}
