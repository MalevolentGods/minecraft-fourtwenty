package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.malevolentgods.fourtwenty.domain.college.ColonyResearch;
import com.malevolentgods.fourtwenty.domain.college.Study;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every colony's Weed College research (research R3), saved with the colony's dimension: the queue, progress, the head study's fixed cost
 * and deliveries, the members' colours, and the finished studies. It outlives any one College, so demolishing and rebuilding loses nothing.
 * Like DispensarySavedData it refuses to start empty over an unreadable save.
 */
public final class CollegeSavedData extends SavedData {
    static final String ID = "fourtwenty_college";
    private final Map<Integer, ColonyResearch> colonies = new HashMap<>();

    public static CollegeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(() -> {
            var path = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),
                    level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data").resolve(ID + ".dat");
            if (java.nio.file.Files.exists(path)) throw new IllegalStateException("Cannot load the Weed College's research; preserve and repair " + path);
            return new CollegeSavedData();
        }, CollegeSavedData::load, null), ID);
    }

    /** The colony's research, created empty on first use. Callers that change it must call {@link #setDirty()}. */
    public ColonyResearch colony(int id) {
        return colonies.computeIfAbsent(id, k -> new ColonyResearch());
    }

    public void removeColony(int id) {
        if (colonies.remove(id) != null) setDirty();
    }

    @Override
    public @Nonnull CompoundTag save(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        colonies.forEach((id, r) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("colony", id);
            c.put("queue", strings(r.queue()));
            CompoundTag progress = new CompoundTag();
            r.queue().forEach(s -> { if (r.progress(s) > 0) progress.putInt(s, r.progress(s)); });
            c.put("progress", progress);
            CompoundTag done = new CompoundTag();
            r.done().forEach(done::putLong);
            c.put("done", done);
            c.put("colours", strings(r.membersColours()));
            if (r.cost() != null) {
                ListTag cost = new ListTag();
                List<Integer> delivered = r.delivered();
                for (int i = 0; i < r.cost().size(); i++) {
                    Study.Supply s = r.cost().get(i);
                    CompoundTag line = new CompoundTag();
                    line.putString("item", s.item());
                    line.putInt("count", s.count());
                    if (s.strain() != null) line.putString("strain", s.strain());
                    line.putInt("delivered", delivered.get(i));
                    cost.add(line);
                }
                c.put("cost", cost);
            }
            c.putInt("badBatches", r.badBatches());
            c.putInt("unitsWorked", r.unitsWorked());
            list.add(c);
        });
        tag.put("colonies", list);
        tag.putInt("version", 1);
        return tag;
    }

    /** Reads a saved copy back (the colony tests round-trip a save through this). */
    public static CollegeSavedData read(CompoundTag tag, HolderLookup.Provider provider) { return load(tag, provider); }

    private static CollegeSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CollegeSavedData data = new CollegeSavedData();
        for (Tag t : tag.getList("colonies", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            List<String> queue = readStrings(c.getList("queue", Tag.TAG_STRING));
            Map<String, Integer> progress = new LinkedHashMap<>();
            CompoundTag p = c.getCompound("progress");
            for (String k : p.getAllKeys()) progress.put(k, p.getInt(k));
            Map<String, Long> done = new LinkedHashMap<>();
            CompoundTag d = c.getCompound("done");
            for (String k : d.getAllKeys()) done.put(k, d.getLong(k));
            Set<String> colours = new LinkedHashSet<>(readStrings(c.getList("colours", Tag.TAG_STRING)));
            List<Study.Supply> cost = null;
            List<Integer> delivered = new ArrayList<>();
            if (c.contains("cost", Tag.TAG_LIST)) {
                cost = new ArrayList<>();
                for (Tag lt : c.getList("cost", Tag.TAG_COMPOUND)) {
                    CompoundTag line = (CompoundTag) lt;
                    cost.add(new Study.Supply(line.getString("item"), Math.max(1, line.getInt("count")),
                            line.contains("strain") ? line.getString("strain") : null, false));
                    delivered.add(line.getInt("delivered"));
                }
            }
            ColonyResearch r = new ColonyResearch();
            r.restore(queue, progress, done, colours, cost, delivered, c.getInt("badBatches"), c.getInt("unitsWorked"));
            data.colonies.put(c.getInt("colony"), r);
        }
        return data;
    }

    private static ListTag strings(Iterable<String> values) {
        ListTag out = new ListTag();
        for (String v : values) out.add(StringTag.valueOf(v));
        return out;
    }

    private static List<String> readStrings(ListTag list) {
        List<String> out = new ArrayList<>();
        for (Tag t : list) out.add(t.getAsString());
        return out;
    }
}
