package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Transient server snapshot sent only over the native permission-checked building view. */
public record DispensaryStatus(int level, String worker, int buds, int paper, int joints,
        int available, int target, int customers, List<String> conditions) {
    public DispensaryStatus { conditions = List.copyOf(conditions); }
    public static final DispensaryStatus EMPTY = new DispensaryStatus(0, "", 0, 0, 0, 0, 0, 0, List.of("unfinished"));
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(level); buffer.writeUtf(worker);
        buffer.writeVarInt(buds); buffer.writeVarInt(paper); buffer.writeVarInt(joints);
        buffer.writeVarInt(available); buffer.writeVarInt(target); buffer.writeVarInt(customers);
        buffer.writeCollection(conditions, (out, value) -> out.writeUtf(value));
    }
    public static DispensaryStatus read(RegistryFriendlyByteBuf buffer) {
        return new DispensaryStatus(buffer.readVarInt(), buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readList(in -> in.readUtf()));
    }
}
