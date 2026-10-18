package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Transient server snapshot sent only over the native permission-checked building view. */
public record DispensaryStatus(int level, String worker, int buds, int paper, int joints,
        int available, int target, int customers, List<String> conditions, List<MenuLine> menu) {
    /** Stock of a menu item other than joints (those keep their own line), against its target. */
    public record MenuLine(String itemId, int held, int target) {}
    public DispensaryStatus { conditions = List.copyOf(conditions); menu = List.copyOf(menu); }
    public DispensaryStatus(int level, String worker, int buds, int paper, int joints, int available, int target, int customers, List<String> conditions) {
        this(level, worker, buds, paper, joints, available, target, customers, conditions, List.of());
    }
    public static final DispensaryStatus EMPTY = new DispensaryStatus(0, "", 0, 0, 0, 0, 0, 0, List.of("unfinished"));
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(level); buffer.writeUtf(worker);
        buffer.writeVarInt(buds); buffer.writeVarInt(paper); buffer.writeVarInt(joints);
        buffer.writeVarInt(available); buffer.writeVarInt(target); buffer.writeVarInt(customers);
        buffer.writeCollection(conditions, (out, value) -> out.writeUtf(value));
        buffer.writeCollection(menu, (out, line) -> { out.writeUtf(line.itemId()); out.writeVarInt(line.held()); out.writeVarInt(line.target()); });
    }
    public static DispensaryStatus read(RegistryFriendlyByteBuf buffer) {
        return new DispensaryStatus(buffer.readVarInt(), buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readList(in -> in.readUtf()),
                buffer.readList(in -> new MenuLine(in.readUtf(), in.readVarInt(), in.readVarInt())));
    }
}
