package com.malevolentgods.fourtwenty.domain.handbook;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays an entry's elements onto Handbook pages (specs/016-weedcraft-handbook). Pure: sizes come from a {@link Measure}
 * (the game's font in play, fixed numbers in tests). Paragraphs split between lines, lists and steps between items;
 * everything else moves whole to the next page. A heading never sits alone at the foot of a page.
 */
public final class Layout {
    private Layout() {}

    public interface Measure {
        /** Lines the spans wrap to at this width. */
        int lines(List<Span> spans, int width);
        int lineHeight();
        /** Height of an element that isn't text (item, recipe, image, table, truth table). */
        int height(Element e, int width);
    }

    /** An element (or a slice of one: lines of a paragraph, items of a list or steps) at a height on a page. */
    public record Placed(Element element, int from, int to, int y, int height) {}

    /** Indent of list and step text past the bullet or number, in the same units as the width. */
    public static final int INDENT = 10;

    private static final class Cursor {
        final List<List<Placed>> pages = new ArrayList<>();
        List<Placed> page = new ArrayList<>();
        int y;

        void newPage() {
            pages.add(page);
            page = new ArrayList<>();
            y = 0;
        }
    }

    public static List<List<Placed>> paginate(List<Element> elements, int width, int pageHeight, Measure m) {
        Cursor c = new Cursor();
        int line = m.lineHeight(), gap = Math.max(1, line / 2);
        for (Element e : elements) {
            if (e instanceof Element.PageBreak) {
                if (!c.page.isEmpty()) c.newPage();
                continue;
            }
            if (!c.page.isEmpty()) c.y += gap;
            switch (e) {
                case Element.Para p -> {
                    int total = m.lines(p.spans(), width), from = 0;
                    while (from < total) {
                        int room = (pageHeight - c.y) / line;
                        if (room <= 0) { c.newPage(); continue; }
                        int to = Math.min(total, from + room);
                        c.page.add(new Placed(p, from, to, c.y, (to - from) * line));
                        c.y += (to - from) * line;
                        from = to;
                        if (from < total) c.newPage();
                    }
                }
                case Element.ListItems l -> items(c, e, l.items(), width - INDENT, pageHeight, m);
                case Element.Steps s -> items(c, e, s.items(), width - INDENT, pageHeight, m);
                default -> {
                    int h = m.height(e, width);
                    int need = e instanceof Element.Heading ? h + gap + 2 * line : h;    // keeps two lines of what follows
                    if (c.y + need > pageHeight && !c.page.isEmpty()) c.newPage();
                    c.page.add(new Placed(e, 0, 1, c.y, h));
                    c.y += h;
                }
            }
        }
        if (!c.page.isEmpty() || c.pages.isEmpty()) c.pages.add(c.page);
        return c.pages;
    }

    private static void items(Cursor c, Element e, List<List<Span>> items, int width, int pageHeight, Measure m) {
        int line = m.lineHeight(), from = 0;
        while (from < items.size()) {
            int to = from, h = 0;
            while (to < items.size()) {
                int ih = m.lines(items.get(to), width) * line;
                if (c.y + h + ih > pageHeight && (to > from || !c.page.isEmpty())) break;
                h += ih;
                to++;
            }
            if (to == from) { c.newPage(); continue; }
            c.page.add(new Placed(e, from, to, c.y, h));
            c.y += h;
            from = to;
            if (from < items.size()) c.newPage();
        }
    }
}
