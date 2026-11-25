package com.malevolentgods.fourtwenty.domain.handbook;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTest {
    /** One character per unit of width, 10 high lines, every picture 40 high. */
    private static final Layout.Measure M = new Layout.Measure() {
        @Override public int lines(List<Span> spans, int width) {
            int n = spans.stream().mapToInt(s -> s.text().length()).sum();
            return Math.max(1, (n + width - 1) / width);
        }
        @Override public int lineHeight() { return 10; }
        @Override public int height(Element e, int width) { return 40; }
    };

    private static List<Span> text(int chars) { return List.of(new Span("x".repeat(chars), false, false, null, null, null)); }

    @Test
    void aLongParagraphSplitsBetweenLines() {
        var pages = Layout.paginate(List.of(new Element.Para(text(100 * 25))), 100, 100, M);   // 25 lines, 10 a page
        assertEquals(3, pages.size());
        assertEquals(0, pages.get(0).get(0).from());
        assertEquals(10, pages.get(0).get(0).to());
        assertEquals(10, pages.get(1).get(0).from());
        assertEquals(25, pages.get(2).get(0).to());
    }

    @Test
    void picturesMoveWholeToTheNextPage() {
        var pages = Layout.paginate(List.of(new Element.Para(text(100 * 8)), new Element.Item("a", "")), 100, 100, M);
        assertEquals(2, pages.size(), "an 80-high paragraph leaves no room for a 40-high picture");
        assertEquals(0, pages.get(1).get(0).y());
    }

    @Test
    void stepsSplitBetweenItemsAndKeepTheirNumbers() {
        List<List<Span>> items = new ArrayList<>();
        for (int i = 0; i < 15; i++) items.add(text(10));
        var pages = Layout.paginate(List.of(new Element.Steps(1, items)), 100, 100, M);
        assertEquals(2, pages.size());
        assertEquals(10, pages.get(0).get(0).to());
        assertEquals(10, pages.get(1).get(0).from(), "the second page carries on from step 11");
    }

    @Test
    void aHeadingNeverSitsAloneAtTheFoot() {
        var pages = Layout.paginate(List.of(new Element.Para(text(100 * 8)), new Element.Heading(text(5)), new Element.Para(text(300))), 100, 100, M);
        assertTrue(pages.get(1).get(0).element() instanceof Element.Heading, "the heading moved to the next page with its text");
    }

    @Test
    void aChaptersEntriesRunOnInsteadOfStartingFreshPages() {
        var one = new Layout.Section("a", List.of(new Element.Heading(text(5)), new Element.Para(text(100 * 2))));
        var two = new Layout.Section("b", List.of(new Element.Heading(text(5)), new Element.Para(text(100 * 2))));
        var run = Layout.run(List.of(one, two), 100, 200, M);                  // each entry: a 40-high heading and 2 lines
        assertEquals(1, run.pages().size(), "two short entries share a page");
        assertEquals(0, run.starts().get("a"));
        assertEquals(0, run.starts().get("b"));
    }

    @Test
    void anEntryWithNoRoomForItsHeadingAndTwoLinesStartsOnTheNextPage() {
        var one = new Layout.Section("a", List.of(new Element.Para(text(100 * 7))));
        var two = new Layout.Section("b", List.of(new Element.Heading(text(5)), new Element.Para(text(100 * 3))));
        var run = Layout.run(List.of(one, two), 100, 100, M);
        assertEquals(1, run.starts().get("b"));
        assertTrue(run.pages().get(1).get(0).element() instanceof Element.Heading);
    }

    @Test
    void noSingleLineIsLeftAloneAtTheTopOfAPage() {
        var pages = Layout.paginate(List.of(new Element.Para(text(100 * 11))), 100, 100, M);    // 11 lines, 10 a page
        assertEquals(9, pages.get(0).get(0).to(), "one line comes over so the next page starts with two");
        assertEquals(2, pages.get(1).get(0).to() - pages.get(1).get(0).from());
    }

    @Test
    void noSingleLineIsLeftAloneAtTheFootOfAPage() {
        var pages = Layout.paginate(List.of(new Element.Para(text(100 * 9)), new Element.Para(text(100 * 3))), 100, 100, M);
        assertEquals(1, pages.get(0).size(), "the 3-line paragraph doesn't leave its first line alone at the foot");
        assertEquals(0, pages.get(1).get(0).from());
    }

    @Test
    void aPageBreakStartsANewPage() {
        var pages = Layout.paginate(List.of(new Element.Para(text(10)), new Element.PageBreak(), new Element.Para(text(10))), 100, 100, M);
        assertEquals(2, pages.size());
    }

    @Test
    void anEmptyEntryStillHasAPage() {
        assertEquals(1, Layout.paginate(List.of(), 100, 100, M).size());
    }
}
