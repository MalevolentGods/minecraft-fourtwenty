package com.malevolentgods.fourtwenty.tiedye;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.malevolentgods.fourtwenty.domain.TerpenePalette;
import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules.Colourway;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.DyeColor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 16 curated colourways (data-model.md), read from the mod's own data file on the client and the server alike, so a dyed item
 * looks the same everywhere and with or without MineColonies (as the Handbook's scouting data). A bad entry is logged and left out.
 */
public final class Colourways {
    private Colourways() {}

    private static final String FILE = "/data/fourtwenty/tie_dye/colourways.json";
    private static volatile Map<String, Colourway> all;

    public static Map<String, Colourway> all() {
        Map<String, Colourway> a = all;
        if (a == null) {
            a = load();
            all = a;
        }
        return a;
    }

    public static List<Colourway> list() { return List.copyOf(all().values()); }

    /** The colourway, or the first one for an id no longer known (the item keeps its id; research: contracts/tie-dye-data.md). */
    public static Colourway get(String id) {
        Colourway c = all().get(id);
        return c != null ? c : all().values().iterator().next();
    }

    public static boolean known(String id) { return all().containsKey(id); }

    /** A colour code's RGB: a hemp colour's final terpene colour, or a Minecraft colour's Infuser colour. */
    public static int rgb(String code) {
        if (code.startsWith("mc_")) {
            DyeColor dye = DyeColor.byName(code.substring(3), null);
            return dye == null ? TerpenePalette.NATURAL : TerpenePalette.minecraft(dye);
        }
        return Terpenes.Code.parse(code).map(TerpenePalette::color).orElse(TerpenePalette.NATURAL);
    }

    /** The RGB of colour slot {@code slot} of a colourway. */
    public static int slotRgb(String colourway, int slot) { return rgb(get(colourway).colour(slot)); }

    private static Map<String, Colourway> load() {
        Map<String, Colourway> out = new LinkedHashMap<>();
        try (InputStream in = Colourways.class.getResourceAsStream(FILE)) {
            if (in == null) throw new java.io.FileNotFoundException(FILE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement e : root.getAsJsonArray("colourways")) {
                try {
                    JsonObject o = e.getAsJsonObject();
                    List<String> colours = new ArrayList<>();
                    o.getAsJsonArray("colours").forEach(c -> colours.add(c.getAsString()));
                    Colourway c = new Colourway(o.get("id").getAsString(), colours);
                    out.put(c.id(), c);
                } catch (RuntimeException bad) {
                    LogUtils.getLogger().error("Tie-dye: a colourway is unreadable and left out: {}", bad.getMessage());
                }
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("Tie-dye: couldn't read the colourways", ex);
        }
        if (out.isEmpty()) out.put("ashtray", new Colourway("ashtray", List.of("mc_black", "mc_gray", "mc_white")));
        return out;
    }
}
