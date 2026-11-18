package com.malevolentgods.fourtwenty.domain.handbook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The back of the Handbook (specs/016-weedcraft-handbook FR-006): the index and glossary, built from the entries. */
public final class Contents {
    private Contents() {}

    public record IndexLine(String term, String entry) {}

    /** Every term every entry explains, sorted A to Z (a term in two entries is listed twice, in entry order). */
    public static List<IndexLine> index(Map<String, List<String>> termsByEntry) {
        List<IndexLine> out = new ArrayList<>();
        termsByEntry.forEach((entry, terms) -> {
            for (String t : terms) {
                if (!t.isBlank()) out.add(new IndexLine(t.strip(), entry));
            }
        });
        out.sort(Comparator.comparing((IndexLine l) -> l.term().toLowerCase(Locale.ROOT)).thenComparing(IndexLine::entry));
        List<IndexLine> unique = new ArrayList<>();
        for (IndexLine l : out) if (!unique.contains(l)) unique.add(l);
        return unique;
    }

    /** The index as book elements: a letter heading, then the terms under it, each a link to its entry. */
    public static List<Element> indexElements(List<IndexLine> lines) {
        Map<Character, List<List<Span>>> byLetter = new LinkedHashMap<>();
        for (IndexLine l : lines) {
            char c = Character.toUpperCase(l.term().charAt(0));
            if (!Character.isLetter(c)) c = '#';
            byLetter.computeIfAbsent(c, k -> new ArrayList<>()).add(List.of(new Span(l.term(), false, false, null, l.entry(), null)));
        }
        List<Element> out = new ArrayList<>();
        byLetter.forEach((letter, items) -> {
            out.add(new Element.Heading(List.of(new Span(String.valueOf(letter), true, false, null, null, null))));
            out.add(new Element.ListItems(items));
        });
        return out;
    }

    /** The glossary as book elements: the term in bold linking to its entry, then its meaning. */
    public static List<Element> glossaryElements(List<String[]> terms) {
        List<Element> out = new ArrayList<>();
        for (String[] t : terms) {         // term, meaning, entry (may be empty)
            String link = t.length > 2 && !t[2].isEmpty() ? t[2] : null;
            out.add(new Element.Para(List.of(new Span(t[0], true, false, null, link, null), new Span(": " + t[1], false, false, null, null, null))));
        }
        return out;
    }

    /** Parses glossary lines: "Term: meaning -> entry:id". */
    public static List<String[]> parseGlossary(String text) {
        List<String[]> out = new ArrayList<>();
        for (String line : text.replace("\r", "").split("\n")) {
            int colon = line.indexOf(':'), arrow = line.lastIndexOf("-> entry:");
            if (colon <= 0) continue;
            String term = line.substring(0, colon).strip();
            String meaning = (arrow > colon ? line.substring(colon + 1, arrow) : line.substring(colon + 1)).strip();
            String target = arrow > colon ? line.substring(arrow + "-> entry:".length()).strip() : "";
            out.add(new String[]{term, meaning, target});
        }
        return out;
    }
}
