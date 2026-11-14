package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.malevolentgods.fourtwenty.domain.Strain;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Which Builders are Lifted, on what, and for how much longer (specs/013-lifted-builders, data-model "Lifted").
 * Saved with the level, keyed by colony and citizen, so a restart resumes the time left (and lands her, see
 * LiftedFlight). Time only counts down while she's building.
 */
public final class Lifted extends SavedData {
    public static final String ID = "fourtwenty_lifted_builders";

    public record Key(int colony, int citizen) {}

    public record Buff(Strain strain, int ticksLeft) {}

    private final Map<Key, Buff> buffs = new HashMap<>();

    public static Lifted get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(Lifted::new, Lifted::load, null), ID);
    }

    @Nullable
    public Buff get(int colony, int citizen) {
        return buffs.get(new Key(colony, citizen));
    }

    public boolean isLifted(int colony, int citizen) {
        return buffs.containsKey(new Key(colony, citizen));
    }

    public void start(int colony, int citizen, Strain strain, int ticks) {
        buffs.put(new Key(colony, citizen), new Buff(strain, ticks));
        setDirty();
    }

    /** Counts her time down by {@code ticks}; returns true when it has just run out (and removes it). */
    public boolean tick(int colony, int citizen, int ticks) {
        var key = new Key(colony, citizen);
        var buff = buffs.get(key);
        if (buff == null) return false;
        int left = buff.ticksLeft() - ticks;
        if (left <= 0) {
            buffs.remove(key);
            setDirty();
            return true;
        }
        buffs.put(key, new Buff(buff.strain(), left));
        setDirty();
        return false;
    }

    public void end(int colony, int citizen) {
        if (buffs.remove(new Key(colony, citizen)) != null) setDirty();
    }

    public void removeColony(int colony) {
        if (buffs.keySet().removeIf(k -> k.colony() == colony)) setDirty();
    }

    public Map<Key, Buff> all() {
        return Map.copyOf(buffs);
    }

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        var list = new ListTag();
        buffs.forEach((key, buff) -> {
            var tag = new CompoundTag();
            tag.putInt("colony", key.colony());
            tag.putInt("citizen", key.citizen());
            tag.putString("strain", buff.strain().name());
            tag.putInt("ticks", buff.ticksLeft());
            list.add(tag);
        });
        root.put("buffs", list);
        return root;
    }

    public static Lifted load(CompoundTag root, HolderLookup.Provider registries) {
        var out = new Lifted();
        for (Tag t : root.getList("buffs", Tag.TAG_COMPOUND)) {
            var tag = (CompoundTag) t;
            Strain strain;
            try {
                strain = Strain.valueOf(tag.getString("strain"));
            } catch (IllegalArgumentException unknown) {
                continue;   // a strain from a newer or older version: drop that buff
            }
            int ticks = tag.getInt("ticks");
            if (ticks > 0) out.buffs.put(new Key(tag.getInt("colony"), tag.getInt("citizen")), new Buff(strain, ticks));
        }
        return out;
    }
}
