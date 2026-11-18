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

    public record Book(List<Chapter> chapters, Map<String, Entry> entries, Map<String, String> glossary, String language) {
        public static final Book EMPTY = new Book(List.of(), Map.of(), Map.of(), "en_us");
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
            Map<String, String> glossary = new LinkedHashMap<>();
            readText(manager, lang, "glossary.md").ifPresent(text -> {
                for (String line : text.split("\n")) {
                    int colon = line.indexOf(':'), arrow = line.lastIndexOf("-> entry:");
                    if (colon < 0) continue;
                    String term = line.substring(0, colon).strip();
                    String meaning = (arrow > colon ? line.substring(colon + 1, arrow) : line.substring(colon + 1)).strip();
                    String target = arrow > 0 ? line.substring(arrow + "-> entry:".length()).strip() : "";
                    if (!target.isEmpty() && !entries.containsKey(target)) continue;   // its entry is hidden (a missing mod)
                    glossary.put(term, meaning + (target.isEmpty() ? "" : "\u0000" + target));
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
        return new Entry(id, chapter, parsed.front(), parsed.elements());
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
