package com.malevolentgods.fourtwenty.client.handbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.malevolentgods.fourtwenty.domain.handbook.Element;
import com.malevolentgods.fourtwenty.domain.handbook.Markup;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Handbook's content, read from the resource packs (specs/016-weedcraft-handbook, research R1): {@code
 * assets/fourtwenty/handbook/book.json} for the chapters and {@code handbook/<language>/entries/<chapter>/<entry>.md}
 * for each entry, falling back to en_us. Chapters and entries needing a mod that isn't loaded are left out, and so are
 * links to them (FR-008).
 */
public final class BookData implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final BookData INSTANCE = new BookData();

    public record Chapter(String id, String title, String icon, List<String> entries) {}

    /** An entry: id is "chapter/name". */
    public record Entry(String id, String chapter, Markup.Front front, List<Element> elements) {}

    /** glossary: term, meaning, entry (empty or a shown entry). */
    public record Book(List<Chapter> chapters, Map<String, Entry> entries, List<String[]> glossary, String language) {
        public static final Book EMPTY = new Book(List.of(), Map.of(), List.of(), "en_us");
    }

    private volatile Book book = Book.EMPTY;
    private volatile String loadedLanguage = "";
    private ResourceManager last;

    private BookData() {}

    public static Book book() {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        if (!lang.equals(INSTANCE.loadedLanguage) && INSTANCE.last != null) INSTANCE.load(INSTANCE.last, lang);
        return INSTANCE.book;
    }

    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager manager) {
        last = manager;
        load(manager, Minecraft.getInstance().getLanguageManager().getSelected());
    }

    private void load(ResourceManager manager, String lang) {
        loadedLanguage = lang;
        try {
            Optional<Resource> index = manager.getResource(rl("handbook/book.json"));
            if (index.isEmpty()) { book = Book.EMPTY; return; }
            JsonObject root;
            try (Reader r = index.get().openAsReader()) { root = JsonParser.parseReader(r).getAsJsonObject(); }
            List<Chapter> chapters = new ArrayList<>();
            Map<String, Entry> entries = new LinkedHashMap<>();
            for (JsonElement ce : root.getAsJsonArray("chapters")) {
                JsonObject c = ce.getAsJsonObject();
                if (!available(str(c, "needs"))) continue;
                String id = str(c, "id");
                List<String> ids = new ArrayList<>();
                JsonArray list = c.getAsJsonArray("entries");
                for (JsonElement ee : list) {
                    String entryId = id + "/" + ee.getAsString();
                    Entry entry = readEntry(manager, lang, id, entryId);
                    if (entry == null || !available(entry.front().needs())) continue;
                    entries.put(entryId, entry);
                    ids.add(entryId);
                }
                chapters.add(new Chapter(id, str(c, "title"), str(c, "icon"), List.copyOf(ids)));
            }
            List<String[]> glossary = new ArrayList<>();
            readText(manager, lang, "glossary.md").ifPresent(text -> {
                for (String[] t : com.malevolentgods.fourtwenty.domain.handbook.Contents.parseGlossary(text)) {
                    if (!t[2].isEmpty() && !entries.containsKey(t[2])) continue;   // its entry is hidden (a missing mod)
                    glossary.add(t);
                }
            });
            book = new Book(List.copyOf(chapters), entries, glossary, lang);
        } catch (IOException | RuntimeException e) {
            LOG.error("Couldn't read the Handbook", e);
            book = Book.EMPTY;
        }
    }

    private static Entry readEntry(ResourceManager manager, String lang, String chapter, String id) throws IOException {
        Optional<String> text = readText(manager, lang, "entries/" + id + ".md");
        if (text.isEmpty()) {
            LOG.warn("Handbook entry {} is missing", id);
            return null;
        }
        Markup.Entry parsed = Markup.parse(text.get());
        if (!parsed.errors().isEmpty()) LOG.warn("Handbook entry {}: {}", id, parsed.errors());
        return new Entry(id, chapter, parsed.front(), expandTables(manager, parsed.elements()));
    }

    /** The generated back-of-the-book tables (specs/016 FR-015): the parts and their real names, and the units. */
    private static List<Element> expandTables(ResourceManager manager, List<Element> elements) {
        if (elements.stream().noneMatch(e -> e instanceof Element.Table t && !t.special().isEmpty())) return elements;
        JsonObject terms = null;
        try {
            Optional<Resource> res = manager.getResource(rl("handbook/real_terms.json"));
            if (res.isPresent()) try (Reader r = res.get().openAsReader()) { terms = JsonParser.parseReader(r).getAsJsonObject(); }
        } catch (IOException e) {
            LOG.warn("Couldn't read the real-name table", e);
        }
        List<Element> out = new ArrayList<>();
        for (Element e : elements) {
            if (!(e instanceof Element.Table t) || t.special().isEmpty() || terms == null) { out.add(e); continue; }
            List<List<String>> rows = new ArrayList<>();
            if (t.special().equals("units")) {
                rows.add(List.of("In the mod", "Measures", "Real unit"));
                for (JsonElement u : terms.getAsJsonArray("units")) {
                    JsonObject o = u.getAsJsonObject();
                    rows.add(List.of(o.get("symbol").getAsString(), o.get("measures").getAsString(), o.get("real").getAsString()));
                }
            } else {
                rows.add(List.of("Part", "Real name"));
                List<String[]> parts = new ArrayList<>();
                for (var en : terms.getAsJsonObject("parts").entrySet()) {
                    String id = en.getKey();
                    String key = net.minecraft.client.resources.language.I18n.exists("block.fourtwenty." + id) ? "block.fourtwenty." + id : "item.fourtwenty." + id;
                    parts.add(new String[]{net.minecraft.client.resources.language.I18n.get(key), en.getValue().getAsString()});
                }
                parts.sort(java.util.Comparator.comparing(p -> p[0].toLowerCase(java.util.Locale.ROOT)));
                for (String[] p : parts) rows.add(List.of(p[0], p[1]));
            }
            out.add(new Element.Table(rows, ""));
        }
        return out;
    }

    private static Optional<String> readText(ResourceManager manager, String lang, String path) throws IOException {
        for (String l : lang.equals("en_us") ? List.of("en_us") : List.of(lang, "en_us")) {
            Optional<Resource> res = manager.getResource(rl("handbook/" + l + "/" + path));
            if (res.isPresent()) {
                try (Reader r = res.get().openAsReader()) {
                    StringBuilder b = new StringBuilder();
                    char[] buf = new char[4096];
                    for (int n; (n = r.read(buf)) > 0; ) b.append(buf, 0, n);
                    return Optional.of(b.toString());
                }
            }
        }
        return Optional.empty();
    }

    /** Whether a {@code needs} mod is present (null or empty needs nothing). */
    public static boolean available(String needs) {
        return needs == null || needs.isEmpty() || ModList.get().isLoaded(needs);
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("fourtwenty", path);
    }
}
