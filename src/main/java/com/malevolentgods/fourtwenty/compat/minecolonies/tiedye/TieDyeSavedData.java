package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import com.malevolentgods.fourtwenty.domain.tiedye.OrderQueue;
import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every colony's tie-dye orders and its colonists' shirts (specs/018-tie-dye-shack data-model.md), saved with the colony's dimension.
 * They outlive any one Shack. Like the College's, it refuses to start empty over an unreadable save.
 */
public final class TieDyeSavedData extends SavedData {
    static final String ID = "fourtwenty_tiedye";
    private final Map<Integer, OrderQueue> orders = new HashMap<>();
    /** colony id -> citizen id -> shirt */
    private final Map<Integer, Map<Integer, ItemStack>> shirts = new HashMap<>();

    public static TieDyeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(() -> {
            var path = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),
                    level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data").resolve(ID + ".dat");
            if (java.nio.file.Files.exists(path)) throw new IllegalStateException("Cannot load the Tie Dye Shack's orders; preserve and repair " + path);
            return new TieDyeSavedData();
        }, TieDyeSavedData::load, null), ID);
    }

    public OrderQueue orders(int colony) { return orders.computeIfAbsent(colony, k -> new OrderQueue()); }

    public Map<Integer, ItemStack> shirts(int colony) { return shirts.computeIfAbsent(colony, k -> new HashMap<>()); }

    public void removeColony(int id) {
        boolean changed = orders.remove(id) != null;
        changed |= shirts.remove(id) != null;
        if (changed) setDirty();
    }

    @Override public @Nonnull CompoundTag save(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        orders.forEach((id, q) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("colony", id);
            c.putInt("next", q.nextId());
            c.putLong("made", q.totalMade());
            ListTag os = new ListTag();
            for (OrderQueue.Order o : q.orders()) {
                CompoundTag t = new CompoundTag();
                t.putInt("id", o.id()); t.putInt("pattern", o.pattern()); t.putString("colourway", o.colourway());
                t.putString("kind", o.kind().name()); t.putString("wood", o.wood()); t.putInt("count", o.count()); t.putInt("made", o.made());
                if (o.by() != null) t.putUUID("by", o.by());
                os.add(t);
            }
            c.put("orders", os);
            if (q.batch() != null) {
                ListTag b = new ListTag();
                List<Integer> delivered = q.delivered();
                for (int i = 0; i < q.batch().size(); i++) {
                    CompoundTag line = new CompoundTag();
                    line.putString("item", q.batch().get(i).item());
                    line.putInt("count", q.batch().get(i).count());
                    line.putInt("delivered", delivered.get(i));
                    b.add(line);
                }
                c.put("batch", b);
                c.putInt("batchItems", q.batchItems());
            }
            list.add(c);
        });
        tag.put("colonies", list);
        ListTag worn = new ListTag();
        shirts.forEach((colony, byCitizen) -> byCitizen.forEach((citizen, shirt) -> {
            if (shirt.isEmpty()) return;
            CompoundTag t = new CompoundTag();
            t.putInt("colony", colony);
            t.putInt("citizen", citizen);
            t.put("shirt", shirt.save(provider));
            worn.add(t);
        }));
        tag.put("shirts", worn);
        tag.putInt("version", 1);
        return tag;
    }

    public static TieDyeSavedData read(CompoundTag tag, HolderLookup.Provider provider) { return load(tag, provider); }

    private static TieDyeSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TieDyeSavedData data = new TieDyeSavedData();
        for (Tag t : tag.getList("colonies", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            List<OrderQueue.Order> os = new ArrayList<>();
            for (Tag ot : c.getList("orders", Tag.TAG_COMPOUND)) {
                CompoundTag o = (CompoundTag) ot;
                TieDyeRules.Kind kind;
                try { kind = TieDyeRules.Kind.valueOf(o.getString("kind")); } catch (IllegalArgumentException e) { continue; }
                os.add(new OrderQueue.Order(o.getInt("id"), o.getInt("pattern"), o.getString("colourway"), kind, o.getString("wood"),
                        o.getInt("count"), o.getInt("made"), o.hasUUID("by") ? o.getUUID("by") : (UUID) null));
            }
            List<TieDyeRules.Supply> batch = null;
            List<Integer> delivered = new ArrayList<>();
            if (c.contains("batch", Tag.TAG_LIST)) {
                batch = new ArrayList<>();
                for (Tag lt : c.getList("batch", Tag.TAG_COMPOUND)) {
                    CompoundTag line = (CompoundTag) lt;
                    batch.add(new TieDyeRules.Supply(line.getString("item"), Math.max(1, line.getInt("count"))));
                    delivered.add(line.getInt("delivered"));
                }
            }
            OrderQueue q = new OrderQueue();
            q.restore(os, c.getInt("next"), batch, c.getInt("batchItems"), delivered, c.getLong("made"));
            data.orders.put(c.getInt("colony"), q);
        }
        for (Tag t : tag.getList("shirts", Tag.TAG_COMPOUND)) {
            CompoundTag s = (CompoundTag) t;
            ItemStack shirt = ItemStack.parseOptional(provider, s.getCompound("shirt"));
            if (!shirt.isEmpty()) data.shirts(s.getInt("colony")).put(s.getInt("citizen"), shirt);
        }
        return data;
    }
}
