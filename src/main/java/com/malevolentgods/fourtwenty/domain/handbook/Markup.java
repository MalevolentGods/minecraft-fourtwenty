package com.malevolentgods.fourtwenty.domain.handbook;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Handbook's entry markup (specs/016-weedcraft-handbook, contracts/book-markup.md), parsed to elements. Pure: the
 * game lays out and draws the elements, and tools/handbook_markup.py reads the same files the same way for the PDF
 * (both are checked against src/test/resources/handbook/markup-samples).
 */
public final class Markup {
    private Markup() {}

    public record Front(String title, String icon, List<String> terms, String needs) {}

    public record Entry(Front front, List<Element> elements, List<String> errors) {}

    private static final Pattern BLOCK_TAG = Pattern.compile("^\\{(item|recipe|image|truth|table|page)(?::([^}]*))?\\}$");
    private static final Pattern STEP = Pattern.compile("^(\\d+)\\.\\s+(.*)$");
    private static final List<String> GATES = List.of("NOT", "AND", "OR", "NAND", "NOR", "XOR", "XNOR");
    public static final List<String> COLOURS = List.of("lime", "myr", "lav", "pin", "cream", "gold", "red", "gray");

    public static Entry parse(String text) {
        List<String> errors = new ArrayList<>();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int i = 0;
        String title = null, icon = null, needs = null;
        List<String> terms = List.of();
        if (lines.length > 0 && lines[0].strip().equals("---")) {
            i = 1;
            for (; i < lines.length && !lines[i].strip().equals("---"); i++) {
                String line = lines[i];
                int colon = line.indexOf(':');
                if (colon < 0) { if (!line.isBlank()) errors.add("front matter line without a key: " + line); continue; }
                String key = line.substring(0, colon).strip(), value = line.substring(colon + 1).strip();
                int hash = value.indexOf(" #");
                if (hash >= 0) value = value.substring(0, hash).strip();
                switch (key) {
                    case "title" -> title = value;
                    case "icon" -> icon = value;
                    case "needs" -> needs = value.isEmpty() ? null : value;
                    case "terms" -> terms = list(value);
                    default -> errors.add("unknown front matter key: " + key);
                }
            }
            if (i >= lines.length) errors.add("front matter not closed");
            i++;
        }
        List<Element> out = new ArrayList<>();
        StringBuilder para = new StringBuilder();
        List<List<Span>> list = null, steps = null;
        int stepStart = 1;
        for (; i < lines.length; i++) {
            String line = lines[i].strip();
            boolean isList = line.startsWith("- ");
            Matcher step = STEP.matcher(line);
            boolean isStep = step.matches();
            if (!isList && list != null) { out.add(new Element.ListItems(list)); list = null; }
            if (!isStep && steps != null) { out.add(new Element.Steps(stepStart, steps)); steps = null; }
            boolean block = line.isEmpty() || line.startsWith("#") || isList || isStep || line.startsWith("{") && BLOCK_TAG.matcher(line).matches()
                    || line.equals("{/table}") || line.startsWith("{table");
            if (block && para.length() > 0) {
                out.add(new Element.Para(spans(para.toString(), errors)));
                para.setLength(0);
            }
            if (line.isEmpty()) continue;
            if (line.startsWith("# ")) { out.add(new Element.Heading(spans(line.substring(2).strip(), errors))); continue; }
            if (isList) {
                if (list == null) list = new ArrayList<>();
                list.add(spans(line.substring(2).strip(), errors));
                continue;
            }
            if (isStep) {
                if (steps == null) { steps = new ArrayList<>(); stepStart = Integer.parseInt(step.group(1)); }
                steps.add(spans(step.group(2).strip(), errors));
                continue;
            }
            Matcher tag = BLOCK_TAG.matcher(line);
            if (tag.matches()) {
                String name = tag.group(1), arg = tag.group(2) == null ? "" : tag.group(2);
                String[] parts = arg.split("\\|", -1);
                switch (name) {
                    case "item" -> out.add(new Element.Item(parts[0], parts.length > 1 ? parts[1] : ""));
                    case "recipe" -> out.add(new Element.Recipe(parts[0]));
                    case "image" -> out.add(new Element.Image(parts[0], parts.length > 1 ? parts[1] : "", parts.length > 2 ? parts[2] : "normal"));
                    case "truth" -> {
                        if (!GATES.contains(arg)) errors.add("unknown gate: " + arg);
                        out.add(new Element.Truth(arg));
                    }
                    case "page" -> out.add(new Element.PageBreak());
                    case "table" -> {
                        if (!arg.isEmpty()) {
                            if (!arg.equals("parts") && !arg.equals("units")) errors.add("unknown table: " + arg);
                            out.add(new Element.Table(List.of(), arg));
                        } else {
                            List<List<String>> rows = new ArrayList<>();
                            i++;
                            for (; i < lines.length && !lines[i].strip().equals("{/table}"); i++) {
                                String row = lines[i].strip();
                                if (row.isEmpty()) continue;
                                if (!row.startsWith("|")) { errors.add("table row without |: " + row); continue; }
                                String inner = row.endsWith("|") && row.length() > 1 ? row.substring(1, row.length() - 1) : row.substring(1);
                                List<String> cells = new ArrayList<>();
                                for (String c : inner.split("\\|", -1)) cells.add(c.strip());
                                rows.add(cells);
                            }
                            if (i >= lines.length) errors.add("table not closed");
                            out.add(new Element.Table(rows, ""));
                        }
                    }
                    default -> errors.add("unknown tag: " + name);
                }
                continue;
            }
            if (line.equals("{/table}")) { errors.add("{/table} without {table}"); continue; }
            if (line.startsWith("{") && !line.startsWith("{c:") && !line.startsWith("{spoiler:")) {
                errors.add("unknown tag line: " + line);
                continue;
            }
            if (para.length() > 0) para.append(' ');
            para.append(line);
        }
        if (para.length() > 0) out.add(new Element.Para(spans(para.toString(), errors)));
        if (list != null) out.add(new Element.ListItems(list));
        if (steps != null) out.add(new Element.Steps(stepStart, steps));
        if (title == null) errors.add("no title");
        return new Entry(new Front(title, icon, terms, needs), out, errors);
    }

    private static List<String> list(String value) {
        String v = value.strip();
        if (v.startsWith("[") && v.endsWith("]")) v = v.substring(1, v.length() - 1);
        List<String> out = new ArrayList<>();
        for (String s : v.split(",")) if (!s.isBlank()) out.add(s.strip());
        return out;
    }

    /** Inline spans: **bold**, *italic*, {c:name}…{/c}, [text](entry:id), {spoiler:terpene:code}…{/spoiler}. */
    public static List<Span> spans(String text, List<String> errors) {
        List<Span> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean bold = false, italic = false;
        String colour = null, spoiler = null;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (text.startsWith("**", i)) {
                flush(out, cur, bold, italic, colour, null, spoiler);
                bold = !bold;
                i += 2;
            } else if (c == '*') {
                flush(out, cur, bold, italic, colour, null, spoiler);
                italic = !italic;
                i++;
            } else if (text.startsWith("{c:", i)) {
                int end = text.indexOf('}', i);
                if (end < 0) { errors.add("unclosed {c:"); cur.append(text.substring(i)); break; }
                flush(out, cur, bold, italic, colour, null, spoiler);
                colour = text.substring(i + 3, end);
                if (!COLOURS.contains(colour)) errors.add("unknown colour: " + colour);
                i = end + 1;
            } else if (text.startsWith("{/c}", i)) {
                flush(out, cur, bold, italic, colour, null, spoiler);
                if (colour == null) errors.add("{/c} without {c:}");
                colour = null;
                i += 4;
            } else if (text.startsWith("{spoiler:terpene:", i)) {
                int end = text.indexOf('}', i);
                if (end < 0) { errors.add("unclosed {spoiler:"); cur.append(text.substring(i)); break; }
                flush(out, cur, bold, italic, colour, null, spoiler);
                spoiler = text.substring(i + "{spoiler:terpene:".length(), end);
                i = end + 1;
            } else if (text.startsWith("{/spoiler}", i)) {
                flush(out, cur, bold, italic, colour, null, spoiler);
                if (spoiler == null) errors.add("{/spoiler} without {spoiler:}");
                spoiler = null;
                i += "{/spoiler}".length();
            } else if (c == '[') {
                int mid = text.indexOf("](entry:", i), end = mid < 0 ? -1 : text.indexOf(')', mid);
                if (mid < 0 || end < 0) { cur.append(c); i++; continue; }
                flush(out, cur, bold, italic, colour, null, spoiler);
                cur.append(text, i + 1, mid);
                flush(out, cur, bold, italic, colour, text.substring(mid + "](entry:".length(), end), spoiler);
                i = end + 1;
            } else {
                cur.append(c);
                i++;
            }
        }
        flush(out, cur, bold, italic, colour, null, spoiler);
        if (bold) errors.add("unclosed **");
        if (italic) errors.add("unclosed *");
        if (colour != null) errors.add("unclosed {c:" + colour + "}");
        if (spoiler != null) errors.add("unclosed {spoiler:}");
        return out;
    }

    private static void flush(List<Span> out, StringBuilder cur, boolean bold, boolean italic, String colour, String link, String spoiler) {
        if (cur.length() == 0) return;
        out.add(new Span(cur.toString(), bold, italic, colour, link, spoiler));
        cur.setLength(0);
    }
}
