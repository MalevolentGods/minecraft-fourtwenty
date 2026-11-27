package com.malevolentgods.fourtwenty.domain.handbook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lays an entry's elements onto Handbook pages (specs/016-weedcraft-handbook). Pure: sizes come from a {@link Measure}
 * (the game's font in play, fixed numbers in tests). Paragraphs split between lines, lists and steps between items;
 * everything else moves whole to the next page. A heading never sits alone at the foot of a page, and a paragraph never
 * leaves a single line alone at the foot of one page or the top of the next (no orphans, no widows).
 *
 * A chapter's entries run on, as a textbook's sections do: the next entry starts on the same page, under its heading, when there's room
 * for the heading and a few lines; see {@link #run}.
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

    /** One entry of a chapter: its id and its elements (heading first). */
    public record Section(String id, List<Element> elements) {}

    /** A chapter laid out: its pages, and the page (from 0) each entry starts on, the one its heading is on. */
    public record Run(List<List<Placed>> pages, Map<String, Integer> starts) {}

    /** Lays a chapter's entries one after another, each starting below the last with a line's extra space. */
    public static Run run(List<Section> sections, int width, int pageHeight, Measure m) {
        Cursor c = new Cursor();
        Map<String, Integer> starts = new LinkedHashMap<>();
        for (Section s : sections) {
            if (!c.page.isEmpty()) c.y += m.lineHeight();
            boolean first = true;
            for (Element e : s.elements()) {
                place(c, e, width, pageHeight, m);
                if (first) {
                    starts.put(s.id(), c.pages.size());
                    first = false;
                }
            }
            if (first) starts.put(s.id(), c.pages.size());
        }
        if (!c.page.isEmpty() || c.pages.isEmpty()) c.pages.add(c.page);
        return new Run(c.pages, starts);
    }

    public static List<List<Placed>> paginate(List<Element> elements, int width, int pageHeight, Measure m) {
        Cursor c = new Cursor();
        for (Element e : elements) place(c, e, width, pageHeight, m);
        if (!c.page.isEmpty() || c.pages.isEmpty()) c.pages.add(c.page);
        return c.pages;
    }

    private static void place(Cursor c, Element e, int width, int pageHeight, Measure m) {
        int line = m.lineHeight(), gap = Math.max(1, line / 2);
        {
            if (e instanceof Element.PageBreak) {
                if (!c.page.isEmpty()) c.newPage();
                return;
            }
            if (!c.page.isEmpty()) c.y += gap;
            switch (e) {
                case Element.Para p -> {
                    int total = m.lines(p.spans(), width), from = 0;
                    while (from < total) {
                        int room = (pageHeight - c.y) / line;
                        // no orphan: one line alone at the foot of a page goes over with the rest
                        if (room <= 0 || room == 1 && total - from > 1 && !c.page.isEmpty()) { c.newPage(); continue; }
                        int to = Math.min(total, from + room);
                        // no widow: never leave a single line to start the next page; take one more line over instead
                        if (total - to == 1 && to - from >= 3) to--;
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
