package com.malevolentgods.fourtwenty.domain.handbook;

import java.util.List;

/** One piece of a Handbook page, as parsed from the markup (contracts/book-markup.md). */
public sealed interface Element {
    record Heading(List<Span> spans) implements Element {}
    record Para(List<Span> spans) implements Element {}
    record ListItems(List<List<Span>> items) implements Element {}
    record Steps(int start, List<List<Span>> items) implements Element {}
    record Item(String id, String caption) implements Element {}
    record Recipe(String id) implements Element {}
    record Image(String path, String caption, String size) implements Element {}
    /** A table; {@code special} is "parts" or "units" for the generated back-matter tables, else empty. */
    record Table(List<List<String>> rows, String special) implements Element {}
    record Truth(String gate) implements Element {}
    record PageBreak() implements Element {}

    /** Canonical JSON (keys in a fixed order, defaults left out) that the Python reader also writes, so the two can be compared. */
    static String json(List<Element> elements) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) b.append(',');
            b.append(json(elements.get(i)));
        }
        return b.append(']').toString();
    }

    static String json(Element e) {
        return switch (e) {
            case Heading h -> "{\"type\":\"heading\",\"spans\":" + spans(h.spans()) + "}";
            case Para p -> "{\"type\":\"para\",\"spans\":" + spans(p.spans()) + "}";
            case ListItems l -> "{\"type\":\"list\",\"items\":" + items(l.items()) + "}";
            case Steps s -> "{\"type\":\"steps\",\"start\":" + s.start() + ",\"items\":" + items(s.items()) + "}";
            case Item it -> "{\"type\":\"item\",\"id\":" + str(it.id()) + ",\"caption\":" + str(it.caption()) + "}";
            case Recipe r -> "{\"type\":\"recipe\",\"id\":" + str(r.id()) + "}";
            case Image im -> "{\"type\":\"image\",\"path\":" + str(im.path()) + ",\"caption\":" + str(im.caption()) + ",\"size\":" + str(im.size()) + "}";
            case Table t -> {
                StringBuilder b = new StringBuilder("{\"type\":\"table\",\"special\":" + str(t.special()) + ",\"rows\":[");
                for (int i = 0; i < t.rows().size(); i++) {
                    if (i > 0) b.append(',');
                    b.append('[');
                    for (int j = 0; j < t.rows().get(i).size(); j++) {
                        if (j > 0) b.append(',');
                        b.append(str(t.rows().get(i).get(j)));
                    }
                    b.append(']');
                }
                yield b.append("]}").toString();
            }
            case Truth t -> "{\"type\":\"truth\",\"gate\":" + str(t.gate()) + "}";
            case PageBreak p -> "{\"type\":\"page\"}";
        };
    }

    private static String items(List<List<Span>> items) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) b.append(',');
            b.append(spans(items.get(i)));
        }
        return b.append(']').toString();
    }

    private static String spans(List<Span> spans) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < spans.size(); i++) {
            Span s = spans.get(i);
            if (i > 0) b.append(',');
            b.append("{\"t\":").append(str(s.text()));
            if (s.bold()) b.append(",\"b\":true");
            if (s.italic()) b.append(",\"i\":true");
            if (s.colour() != null) b.append(",\"c\":").append(str(s.colour()));
            if (s.link() != null) b.append(",\"link\":").append(str(s.link()));
            if (s.spoiler() != null) b.append(",\"spoiler\":").append(str(s.spoiler()));
            b.append('}');
        }
        return b.append(']').toString();
    }

    private static String str(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }
}
