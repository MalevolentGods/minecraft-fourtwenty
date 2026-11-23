package com.malevolentgods.fourtwenty.handbook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Def;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Kind;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Req;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ranks and Stoner Badges (specs/016-weedcraft-handbook, contracts/scouting-data.md), read from the mod's own files, the same on a
 * client and a server so both agree. Badges that need a mod that isn't loaded are left out.
 */
public final class ScoutingData {
    private ScoutingData() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String BASE = "/assets/fourtwenty/handbook/scouting/";
    private static volatile List<Def> all;

    /** Every rank (in order) and badge whose mod is present. */
    public static List<Def> defs() {
        List<Def> d = all;
        if (d == null) {
            d = load();
            all = d;
        }
        return d;
    }

    public static Def byEarnKey(String earnKey) {
        for (Def d : defs()) if (d.earnKey().equals(earnKey)) return d;
        return null;
    }

    private static List<Def> load() {
        List<Def> out = new ArrayList<>();
        try {
            JsonObject index = read("index.json");
            for (JsonElement e : index.getAsJsonArray("defs")) {
                Def d = parse(read(e.getAsString() + ".json"));
                if (d.needs() == null || ModList.get().isLoaded(d.needs())) out.add(d);
            }
        } catch (Exception ex) {
            LOG.error("Couldn't read the Handbook's ranks and badges", ex);
        }
        out.sort(java.util.Comparator.comparing((Def d) -> !d.rank()).thenComparingInt(Def::order));
        return List.copyOf(out);
    }

    private static JsonObject read(String file) throws java.io.IOException {
        try (InputStream in = ScoutingData.class.getResourceAsStream(BASE + file)) {
            if (in == null) throw new java.io.FileNotFoundException(BASE + file);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    static Def parse(JsonObject o) {
        boolean rank = o.get("kind").getAsString().equals("rank");
        List<Req> reqs = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("requirements")) {
            JsonObject r = e.getAsJsonObject();
            boolean tracked = r.get("kind").getAsString().equals("tracked");
            Map<String, String> subject = new LinkedHashMap<>();
            if (r.has("subject")) r.getAsJsonObject("subject").entrySet().forEach(s -> subject.put(s.getKey(), s.getValue().getAsString()));
            reqs.add(new Req(r.get("n").getAsString(), tracked ? Kind.TRACKED : Kind.HONOUR, tracked ? r.get("deed").getAsString() : null,
                    subject, r.has("count") ? r.get("count").getAsInt() : 1));
        }
        return new Def(o.get("id").getAsString(), rank, rank ? o.get("order").getAsInt() : 0,
                o.has("needs") ? o.get("needs").getAsString() : null, o.has("entry") ? o.get("entry").getAsString() : null, List.copyOf(reqs));
    }
}
