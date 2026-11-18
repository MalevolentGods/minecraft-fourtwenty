package com.malevolentgods.fourtwenty.domain.handbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** The game's markup reader gives exactly what the samples expect (tools/handbook_markup.py is held to the same files). */
class MarkupTest {
    private static final Path SAMPLES = Path.of("src/test/resources/handbook/markup-samples");

    @Test
    void readsEverySampleAsExpected() throws IOException {
        List<Path> mds;
        try (Stream<Path> files = Files.list(SAMPLES)) {
            mds = files.filter(p -> p.toString().endsWith(".md")).sorted().toList();
        }
        assertFalse(mds.isEmpty(), "no samples");
        for (Path md : mds) {
            Markup.Entry entry = Markup.parse(Files.readString(md, StandardCharsets.UTF_8));
            String name = md.getFileName().toString();
            JsonObject expected = JsonParser.parseString(Files.readString(
                    md.resolveSibling(name.replace(".md", ".expected.json")), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(expected.get("front"), front(entry.front()), name + " front");
            assertEquals(expected.get("elements"), JsonParser.parseString(Element.json(entry.elements())), name + " elements");
            JsonArray errors = new JsonArray();
            entry.errors().forEach(errors::add);
            assertEquals(expected.get("errors"), errors, name + " errors");
        }
    }

    private static JsonElement front(Markup.Front f) {
        JsonObject o = new JsonObject();
        o.add("title", f.title() == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(f.title()));
        o.add("icon", f.icon() == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(f.icon()));
        JsonArray terms = new JsonArray();
        f.terms().forEach(terms::add);
        o.add("terms", terms);
        o.add("needs", f.needs() == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(f.needs()));
        return o;
    }
}
