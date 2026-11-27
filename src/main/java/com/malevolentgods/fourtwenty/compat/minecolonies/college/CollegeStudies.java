package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.malevolentgods.fourtwenty.domain.college.StudyBoard;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the Weed College's studies from data/&lt;ns&gt;/college/studies/*.json (contracts/study-data.md) into a {@link StudyBoard}. A study
 * that doesn't parse, or that the tree's validation rejects, is left out with an error in the log; the College never fails over it.
 */
public final class CollegeStudies extends SimpleJsonResourceReloadListener {
    private static volatile StudyBoard board = new StudyBoard(List.of());

    private CollegeStudies() { super(new com.google.gson.Gson(), "college/studies"); }

    public static StudyBoard board() { return board; }

    public static void register(AddReloadListenerEvent event) { event.addListener(new CollegeStudies()); }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> files, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        List<Study> studies = new ArrayList<>();
        for (var entry : files.entrySet()) {
            try {
                studies.add(parse(entry.getKey().toString(), entry.getValue().getAsJsonObject()));
            } catch (RuntimeException e) {
                LogUtils.getLogger().error("Weed College: study {} is unreadable and left out: {}", entry.getKey(), e.getMessage());
            }
        }
        StudyBoard candidate = new StudyBoard(studies);
        Set<String> bad = new HashSet<>();
        for (String error : candidate.validate()) {
            LogUtils.getLogger().error("Weed College: {}", error);
            bad.add(error.substring(0, error.indexOf(':')));
        }
        if (!bad.isEmpty()) {
            studies.removeIf(s -> bad.contains(s.id()));
            candidate = new StudyBoard(studies);
        }
        board = candidate;
        LogUtils.getLogger().info("Weed College: {} studies loaded", board.size());
    }

    static Study parse(String id, JsonObject o) {
        List<String> parents = new ArrayList<>();
        o.getAsJsonArray("parents").forEach(p -> parents.add(p.getAsString()));
        List<Study.Supply> supplies = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("supplies")) {
            JsonObject s = e.getAsJsonObject();
            supplies.add(new Study.Supply(s.get("item").getAsString(), s.get("count").getAsInt(),
                    s.has("strain") ? s.get("strain").getAsString() : null, s.has("per_colour") && s.get("per_colour").getAsBoolean()));
        }
        JsonObject g = o.getAsJsonObject("gives");
        Study.Gives gives = new Study.Gives(strings(g, "colours"), strings(g, "strains"), g.has("seeds_each") ? g.get("seeds_each").getAsInt() : 0,
                text(g, "effect"), g.has("strength") ? g.get("strength").getAsDouble() : 0, text(g, "research_effect"));
        return new Study(id, Study.Branch.of(o.get("branch").getAsString()), o.get("tier").getAsInt(), parents,
                Study.Kind.of(o.get("kind").getAsString()), supplies, gives, o.get("icon").getAsString(), text(o, "needs"));
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            a.forEach(e -> out.add(e.getAsString()));
        }
        return out;
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
