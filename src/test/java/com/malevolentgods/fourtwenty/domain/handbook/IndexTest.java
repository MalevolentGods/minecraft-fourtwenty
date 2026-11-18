package com.malevolentgods.fourtwenty.domain.handbook;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexTest {
    @Test
    void termsAreSortedAToZIgnoringCaseAndKeepTheirEntries() {
        Map<String, List<String>> terms = new LinkedHashMap<>();
        terms.put("power/wiring", List.of("Wick", "closed loop"));
        terms.put("power/basics", List.of("BI", "wick"));
        var index = Contents.index(terms);
        assertEquals(List.of("BI", "closed loop", "wick", "Wick"), index.stream().map(Contents.IndexLine::term).toList());
        assertEquals("power/basics", index.get(2).entry());
    }

    @Test
    void theIndexGroupsByLetterWithLinks() {
        var els = Contents.indexElements(Contents.index(Map.of("a/b", List.of("Apple", "Avocado", "Banana"))));
        assertEquals(4, els.size(), "A heading, its list, B heading, its list");
        assertTrue(els.get(1) instanceof Element.ListItems l && l.items().get(0).get(0).link().equals("a/b"));
    }

    @Test
    void glossaryLinesParseTermMeaningAndEntry() {
        var g = Contents.parseGlossary("BI: the power. -> entry:power/basics\nLoose: no link\n\nnot a line");
        assertEquals(2, g.size());
        assertEquals("BI", g.get(0)[0]);
        assertEquals("the power.", g.get(0)[1]);
        assertEquals("power/basics", g.get(0)[2]);
        assertEquals("", g.get(1)[2]);
    }
}
