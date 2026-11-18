package com.malevolentgods.fourtwenty.client.handbook;

import com.malevolentgods.fourtwenty.domain.handbook.Element;
import com.malevolentgods.fourtwenty.domain.handbook.Layout;
import com.malevolentgods.fourtwenty.domain.handbook.Span;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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
 * cover art, then two-page spreads: DON'T PANIC alone inside the cover (FR-002b), the title page, contents, every entry
 * (each starting on a fresh page). Links, contents lines and the back button move about; the bookmark remembers the
 * spread (FR-002, FR-003, FR-005).
 */
public class HandbookScreen extends Screen {
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/handbook/book.png");
    private static final ResourceLocation COVER = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/handbook/cover.png");
    static final int BOOK_W = 360, BOOK_H = 230, PAGE_W = 140, PAGE_H = 176, LEFT_X = 22, RIGHT_X = 198, PAGE_Y = 22;
    private static final int COVER_W = 176, COVER_H = 228;

    /** One page of the book. */
    sealed interface Page permits DontPanic, Title, Contents, EntryPage, Blank {}
    record DontPanic() implements Page {}
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
        scale = Math.min(1f, Math.min((width - 8) / (float) BOOK_W, (height - 8) / (float) (BOOK_H + 16)));
        left = (int) ((width - BOOK_W * scale) / 2);
        top = (int) ((height - (BOOK_H + 16) * scale) / 2);
        spread = Math.max(0, Math.min(Bookmark.spread(), spreads() - 1));
    }

    // ---- building the pages

    private void build() {
        pages.clear();
        entryPage.clear();
        BookData.Book book = BookData.book();
        pages.add(new DontPanic());
        pages.add(new Title());
        // contents: chapter lines and their entries, paginated by line count
        List<ContentsLine> all = new ArrayList<>();
        for (BookData.Chapter c : book.chapters()) {
            if (c.entries().isEmpty()) continue;
            all.add(new ContentsLine(Component.translatable(c.title()).getString(), c.entries().get(0), true));
            for (String id : c.entries()) all.add(new ContentsLine(book.entries().get(id).front().title(), id, false));
        }
        all.add(new ContentsLine(Component.translatable("handbook.fourtwenty.chapter.back").getString(), GLOSSARY, true));
        if (!book.glossary().isEmpty()) all.add(new ContentsLine(Component.translatable("handbook.fourtwenty.glossary").getString(), GLOSSARY, false));
        all.add(new ContentsLine(Component.translatable("handbook.fourtwenty.index").getString(), INDEX, false));
        int perPage = PAGE_H / PageRenderer.LINE - 2;
        for (int i = 0; i < Math.max(1, all.size()); i += perPage) {
            pages.add(new Contents(all.isEmpty() ? List.of() : all.subList(i, Math.min(all.size(), i + perPage))));
        }
        Layout.Measure m = renderer.measure();
        for (BookData.Chapter c : book.chapters()) {
            for (String id : c.entries()) {
                BookData.Entry e = book.entries().get(id);
                addEntry(id, e.front().title(), e.elements(), m);
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

    static final String GLOSSARY = "back/glossary", INDEX = "back/index";

    private void addEntry(String id, String title, List<Element> elements, Layout.Measure m) {
        List<Element> els = new ArrayList<>();
        els.add(new Element.Heading(List.of(new Span(title, true, false, null, null, null))));
        els.addAll(elements);
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
            g.blit(COVER, cx, 1, COVER_W, COVER_H, 0, 0, 360, 466, 360, 466);
            g.drawCenteredString(font, Component.translatable("handbook.fourtwenty.open"), BOOK_W / 2, BOOK_H + 4, 0xFFE8DDB5);
        } else {
            g.blit(BOOK, 0, 0, 0, 0, BOOK_W, 256, 512, 256);
            int i = 2 * spread - 2;
            drawPage(g, pages.get(i), LEFT_X, i);
            if (i + 1 < pages.size()) drawPage(g, pages.get(i + 1), RIGHT_X, i + 1);
            button(g, BOOK_W - 30, BOOK_H - 18, 0, 23, mx, my, spread < spreads() - 1);
            button(g, 12, BOOK_H - 18, 46, 69, mx, my, true);
            if (!back.isEmpty()) {
                boolean hover = in(mx, my, BOOK_W / 2 - 22, BOOK_H - 18, 13, 11);
                g.blit(BOOK, BOOK_W / 2 - 22, BOOK_H - 18, hover ? 107 : 92, 232, 13, 11, 512, 256);
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
        g.blit(BOOK, x, y, in(mx, my, x, y, 18, 10) ? uHover : u, 232, 18, 10, 512, 256);
    }

    private void drawPage(GuiGraphics g, Page page, int x, int index) {
        switch (page) {
            case DontPanic d -> {
                // the motto, alone, in large friendly letters
                Component text = Component.translatable("handbook.fourtwenty.dont_panic");
                float s = 2.6f;
                g.pose().pushPose();
                g.pose().translate(x + PAGE_W / 2f, PAGE_Y + PAGE_H / 2f - 10, 0);
                g.pose().scale(s, s, 1);
                g.drawString(font, text, -font.width(text) / 2, 0, 0xFF2E7A1A, false);
                g.pose().popPose();
            }
            case Title t -> {
                int y = PAGE_Y + 34;
                for (String key : List.of("handbook.fourtwenty.title.line1", "handbook.fourtwenty.title.line2", "handbook.fourtwenty.title.line3")) {
                    boolean big = key.endsWith("line2");
                    g.pose().pushPose();
                    float s = big ? 2f : 1.2f;
                    Component c = Component.translatable(key);
                    g.pose().translate(x + PAGE_W / 2f, y, 0);
                    g.pose().scale(s, s, 1);
                    g.drawString(font, c, -font.width(c) / 2, 0, big ? 0xFF2F5A1A : PageRenderer.INK, false);
                    g.pose().popPose();
                    y += big ? 24 : 16;
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
        if (spread == 0) { turn(1); return true; }
        if (in(mx, my, BOOK_W - 30, BOOK_H - 18, 18, 10)) { turn(1); return true; }
        if (in(mx, my, 12, BOOK_H - 18, 18, 10)) { turn(-1); return true; }
        if (!back.isEmpty() && in(mx, my, BOOK_W / 2 - 22, BOOK_H - 18, 13, 11)) { goBack(); return true; }
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
