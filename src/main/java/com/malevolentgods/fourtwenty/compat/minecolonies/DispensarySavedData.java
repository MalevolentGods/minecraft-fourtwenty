package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Affinity;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.CitizenDemand;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.ColonistBuff;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Preference;
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
    /** The exposure log: a citizen with no entry has never been exposed (N/A). */
    private final Map<Key, Affinity> affinities = new HashMap<>();
    /** Specialty bong buffs (version 3); expired ones are dropped by the colony tick. */
    private final Map<Key, ColonistBuff> buffs = new HashMap<>();
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
    public Affinity affinity(int colony, UUID citizen) {
        return affinities.getOrDefault(new Key(colony, citizen), Affinity.UNEXPOSED);
    }
    /** Apply a change to a citizen's affinity; returns the stored result. */
    public Affinity updateAffinity(int colony, UUID citizen, java.util.function.UnaryOperator<Affinity> change) {
        var key = new Key(colony, citizen);
        var previous = affinity(colony, citizen);
        var next = change.apply(previous);
        if (!next.equals(previous)) {
            if (next.exposed()) affinities.put(key, next); else affinities.remove(key);
            setDirty();
        }
        return next;
    }
    /**
     * Testing aid (/fourtwenty due): make a craving citizen due now, keeping their grace deadline.
     * Bumps the revision so any in-flight completion from before is rejected. False if not craving.
     */
    public boolean forceDue(int colony, UUID citizen) {
        var state = citizen(colony, citizen);
        if (!state.eligible()) return false;
        var next = new CitizenDemand(true, time, Math.max(state.graceUntil(), time), state.satisfiedUntil(),
                state.healthUntil(), state.lastConsumedAt(), Math.incrementExact(state.revision()));
        citizens.put(new Key(colony, citizen), next);
        setDirty();
        return true;
    }
    public ColonistBuff buff(int colony, UUID citizen) { return buffs.get(new Key(colony, citizen)); }
    /** Set (or with null, clear) a colonist's specialty bong buff. */
    public void setBuff(int colony, UUID citizen, ColonistBuff buff) {
        var key = new Key(colony, citizen);
        var previous = buff == null ? buffs.remove(key) : buffs.put(key, buff);
        if (!java.util.Objects.equals(previous, buff)) setDirty();
    }
    /** Native colony IDs (and their derived citizen UUIDs) can be reused after explicit colony deletion. */
    public void removeColony(int colony) {
        boolean changed = citizens.keySet().removeIf(key -> key.colony() == colony);
        changed |= affinities.keySet().removeIf(key -> key.colony() == colony);
        changed |= buffs.keySet().removeIf(key -> key.colony() == colony);
        if (changed) setDirty();
    }
    /** Permanent native citizen removal, never entity/chunk unloading. */
    public void removeCitizen(int colony, UUID citizen) {
        boolean changed = citizens.remove(new Key(colony, citizen)) != null;
        changed |= affinities.remove(new Key(colony, citizen)) != null;
        changed |= buffs.remove(new Key(colony, citizen)) != null;
        if (changed) setDirty();
    }
    /** JointService couples this checkpoint to live eligibility and actual inventory extraction. */
    public boolean recordConsumption(int colony, UUID citizen, long expectedRevision, DemandRules rules) {
        var next = citizen(colony, citizen).consume(expectedRevision, time, rules);
        if (next.isEmpty()) return false;
        citizens.put(new Key(colony, citizen), next.get());
        setDirty();
        return true;
    }
    /**
     * Reject unknown or malformed data rather than resetting demand and replaying benefits.
     * Version 1 (before affinity) loads with an empty exposure log: every citizen is unexposed.
     * Versions before 3 have no specialty bong buffs.
     */
    public static DispensarySavedData load(CompoundTag root, HolderLookup.Provider registries) {
        require(root, "version", Tag.TAG_INT);
        require(root, "time", Tag.TAG_LONG);
        require(root, "citizens", Tag.TAG_LIST);
        int version = root.getInt("version");
        if (version < 1 || version > DispensaryIds.STATE_VERSION || root.getLong("time") < 0) {
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
        if (version >= 2) {
            require(root, "affinity", Tag.TAG_LIST);
            var log = (ListTag) root.get("affinity");
            if (!log.isEmpty() && log.getElementType() != Tag.TAG_COMPOUND) throw new IllegalArgumentException("Invalid affinity list");
            for (var value : log) {
                var tag = (CompoundTag) value;
                require(tag, "colony", Tag.TAG_INT);
                require(tag, "preference", Tag.TAG_STRING);
                if (!tag.hasUUID("citizen")) throw new IllegalArgumentException("Missing citizen UUID");
                for (String name : new String[]{"exposedAt", "breakUntil", "lastBongRoll"}) require(tag, name, Tag.TAG_LONG);
                for (String name : new String[]{"baseStep", "step"}) require(tag, name, Tag.TAG_INT);
                var affinity = new Affinity(tag.getLong("exposedAt"), Preference.valueOf(tag.getString("preference")),
                        tag.getInt("baseStep"), tag.getInt("step"), tag.getLong("breakUntil"), tag.getLong("lastBongRoll"));
                if (!affinity.exposed() || affinity.exposedAt() > data.time
                        || data.affinities.putIfAbsent(new Key(tag.getInt("colony"), tag.getUUID("citizen")), affinity) != null) {
                    throw new IllegalArgumentException("Invalid or duplicate affinity record");
                }
            }
        }
        if (version >= 3) {
            require(root, "buffs", Tag.TAG_LIST);
            var list3 = (ListTag) root.get("buffs");
            if (!list3.isEmpty() && list3.getElementType() != Tag.TAG_COMPOUND) throw new IllegalArgumentException("Invalid buff list");
            for (var value : list3) {
                var tag = (CompoundTag) value;
                require(tag, "colony", Tag.TAG_INT);
                require(tag, "variety", Tag.TAG_STRING);
                require(tag, "until", Tag.TAG_LONG);
                require(tag, "bonus", Tag.TAG_INT);
                if (!tag.hasUUID("citizen")) throw new IllegalArgumentException("Missing citizen UUID");
                var variety = com.malevolentgods.fourtwenty.domain.BongVariety.byPath(tag.getString("variety"))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown bong variety"));
                var buff = new ColonistBuff(variety, tag.getLong("until"), tag.getInt("bonus"));
                if (data.buffs.putIfAbsent(new Key(tag.getInt("colony"), tag.getUUID("citizen")), buff) != null) {
                    throw new IllegalArgumentException("Duplicate buff record");
                }
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
        ListTag log = new ListTag();
        affinities.forEach((key, affinity) -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("colony", key.colony()); tag.putUUID("citizen", key.citizen());
            tag.putLong("exposedAt", affinity.exposedAt()); tag.putString("preference", affinity.preference().name());
            tag.putInt("baseStep", affinity.baseStep()); tag.putInt("step", affinity.step());
            tag.putLong("breakUntil", affinity.breakUntil()); tag.putLong("lastBongRoll", affinity.lastBongRoll());
            log.add(tag);
        });
        root.put("affinity", log);
        ListTag buffList = new ListTag();
        buffs.forEach((key, buff) -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("colony", key.colony()); tag.putUUID("citizen", key.citizen());
            tag.putString("variety", buff.variety().path); tag.putLong("until", buff.until()); tag.putInt("bonus", buff.bonus());
            buffList.add(tag);
        });
        root.put("buffs", buffList);
        return root;
    }
}
