package com.malevolentgods.fourtwenty.infuser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One player's Inspiration Infuser: how inspired they are by each colour (0-5, never fades), the colours they've
 * caught (1 = caught, 2 = spot on), their pool (specks per resin, kept between visits), the colour they're chasing,
 * and the colour the pool is sealed on after a catch (until it's drained). Personal: every player has their own.
 */
public final class InfuserState {
    public static final int MAX = 5;
    /** Everyone starts fully inspired by Green, the colour of the plant. */
    public static final String FIRST = "green";

    public final Map<String, Integer> inspiration = new LinkedHashMap<>();
    public final Map<String, Integer> caught = new LinkedHashMap<>();
    public int[] pool;
    public String chasing;
    public String sealed;

    public InfuserState(int resins) {
        pool = new int[resins];
    }

    public static InfuserState fresh(int resins) {
        InfuserState s = new InfuserState(resins);
        s.inspiration.put(FIRST, MAX);
        s.chasing = FIRST;
        return s;
    }

    public int level(String id) { return inspiration.getOrDefault(id, 0); }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        CompoundTag in = new CompoundTag();
        inspiration.forEach(in::putInt);
        t.put("inspiration", in);
        CompoundTag c = new CompoundTag();
        caught.forEach(c::putInt);
        t.put("caught", c);
        t.putIntArray("pool", pool);
        if (chasing != null) t.putString("chasing", chasing);
        if (sealed != null) t.putString("sealed", sealed);
        return t;
    }

    public static InfuserState load(CompoundTag t, int resins) {
        InfuserState s = new InfuserState(resins);
        CompoundTag in = t.getCompound("inspiration");
        for (String k : in.getAllKeys()) s.inspiration.put(k, Math.min(MAX, in.getInt(k)));
        CompoundTag c = t.getCompound("caught");
        for (String k : c.getAllKeys()) s.caught.put(k, c.getInt(k));
        int[] p = t.getIntArray("pool");
        if (p.length == resins) s.pool = p;
        s.chasing = t.contains("chasing", Tag.TAG_STRING) ? t.getString("chasing") : null;
        s.sealed = t.contains("sealed", Tag.TAG_STRING) ? t.getString("sealed") : null;
        return s;
    }
}
