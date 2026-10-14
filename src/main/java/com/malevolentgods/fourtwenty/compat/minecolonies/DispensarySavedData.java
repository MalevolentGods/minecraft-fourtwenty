package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.CitizenDemand;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.DemandRules;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Dimension-owned persisted clock and records; no static world state or wall/day-time dependency. */
public final class DispensarySavedData extends SavedData {
    private record Key(int colony, UUID citizen) {
        private Key {
            if (colony < 1 || citizen == null) throw new IllegalArgumentException("Invalid citizen identity");
        }
    }
    private final Map<Key, CitizenDemand> citizens = new HashMap<>();
    private long time;
    public static DispensarySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(() -> {
            // Vanilla swallows deserialization exceptions and calls the constructor. Never overwrite an unreadable save.
            var path = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),
                    level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT))
                    .resolve("data").resolve(DispensaryIds.STATE + ".dat");
            if (java.nio.file.Files.exists(path)) throw new IllegalStateException("Cannot load dispensary state; preserve and repair " + path);
            return new DispensarySavedData();
        }, DispensarySavedData::load, null), DispensaryIds.STATE);
    }
    public long time() { return time; }
    public void advance() { time = Math.incrementExact(time); setDirty(); }
    public CitizenDemand citizen(int colony, UUID citizen) {
        return citizens.getOrDefault(new Key(colony, citizen), CitizenDemand.empty());
    }
    public CitizenDemand observe(int colony, UUID citizen, boolean eligible, DemandRules rules) {
        var key = new Key(colony, citizen);
        var previous = citizen(colony, citizen);
        var next = previous.observe(eligible, time, rules);
        if (!next.equals(previous)) { citizens.put(key, next); setDirty(); }
        return next;
    }
    /** Foundation completion contract; Phase 3 must couple this to actual item consumption and live eligibility. */
    public boolean recordConsumption(int colony, UUID citizen, long expectedRevision, DemandRules rules) {
        var next = citizen(colony, citizen).consume(expectedRevision, time, rules);
        if (next.isEmpty()) return false;
        citizens.put(new Key(colony, citizen), next.get());
        setDirty();
        return true;
    }
    /** Reject unknown or malformed data rather than resetting demand and replaying benefits. */
    public static DispensarySavedData load(CompoundTag root, HolderLookup.Provider registries) {
        require(root, "version", Tag.TAG_INT);
        require(root, "time", Tag.TAG_LONG);
        require(root, "citizens", Tag.TAG_LIST);
        if (root.getInt("version") != DispensaryIds.STATE_VERSION || root.getLong("time") < 0) {
            throw new IllegalArgumentException("Unsupported or invalid FourTwenty dispensary save");
        }
        var data = new DispensarySavedData();
        data.time = root.getLong("time");
        var list = (ListTag) root.get("citizens");
        if (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND) throw new IllegalArgumentException("Invalid citizen list");
        for (var value : list) {
            var tag = (CompoundTag) value;
            require(tag, "colony", Tag.TAG_INT);
            require(tag, "eligible", Tag.TAG_BYTE);
            if (!tag.hasUUID("citizen")) throw new IllegalArgumentException("Missing citizen UUID");
            for (String name : new String[]{"dueAt", "graceUntil", "satisfiedUntil", "healthUntil", "lastConsumedAt", "revision"}) require(tag, name, Tag.TAG_LONG);
            var key = new Key(tag.getInt("colony"), tag.getUUID("citizen"));
            var state = new CitizenDemand(tag.getBoolean("eligible"), tag.getLong("dueAt"), tag.getLong("graceUntil"),
                    tag.getLong("satisfiedUntil"), tag.getLong("healthUntil"), tag.getLong("lastConsumedAt"), tag.getLong("revision"));
            if (state.lastConsumedAt() > data.time || data.citizens.putIfAbsent(key, state) != null) {
                throw new IllegalArgumentException("Invalid or duplicate citizen record");
            }
        }
        return data;
    }
    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing or invalid dispensary field: " + key);
    }
    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        root.putInt("version", DispensaryIds.STATE_VERSION);
        root.putLong("time", time);
        ListTag list = new ListTag();
        citizens.forEach((key, state) -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("colony", key.colony()); tag.putUUID("citizen", key.citizen());
            tag.putBoolean("eligible", state.eligible()); tag.putLong("dueAt", state.dueAt());
            tag.putLong("graceUntil", state.graceUntil()); tag.putLong("satisfiedUntil", state.satisfiedUntil());
            tag.putLong("healthUntil", state.healthUntil()); tag.putLong("lastConsumedAt", state.lastConsumedAt());
            tag.putLong("revision", state.revision()); list.add(tag);
        });
        root.put("citizens", list);
        return root;
    }
}
