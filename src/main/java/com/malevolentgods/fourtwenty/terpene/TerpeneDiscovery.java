package com.malevolentgods.fourtwenty.terpene;

import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Each player's discovered terpenes, kept in their persisted data (so it survives death). Anything
 * that reaches their inventory is discovered, however it got there: made in a Terpinator, picked
 * up, or passed by a friend (puff, puff, pass). The client gets the list on login and each new
 * discovery as it happens.
 */
@EventBusSubscriber(modid = "fourtwenty")
public final class TerpeneDiscovery {
    private TerpeneDiscovery() {}

    private static final String KEY = "fourtwenty_terpenes";

    private static ListTag list(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(KEY, Tag.TAG_LIST)) persisted.put(KEY, new ListTag());
        return persisted.getList(KEY, Tag.TAG_STRING);
    }

    public static Set<String> known(Player player) {
        Set<String> out = new LinkedHashSet<>();
        for (Tag t : list(player)) out.add(t.getAsString());
        return out;
    }

    /** Records discoveries; returns whether anything was new (and tells the client). */
    public static boolean learn(ServerPlayer player, List<String> keys) {
        Set<String> known = known(player);
        List<String> fresh = new ArrayList<>();
        for (String key : keys) if (!known.contains(key) && !fresh.contains(key)) fresh.add(key);
        if (fresh.isEmpty()) return false;
        ListTag tag = list(player);
        fresh.forEach(key -> tag.add(StringTag.valueOf(key)));
        TerpeneNetwork.send(player, fresh, false);
        return true;
    }

    /** The discovery key of a stack (a terpene code, or the resin), or null. */
    public static String key(ItemStack stack) {
        if (stack.getItem() instanceof TerpeneItem t) return t.code().id();
        if (stack.is(ModTerpenes.RESIN.get())) return TerpeneKnowledge.RESIN;
        return null;
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) TerpeneNetwork.send(player, List.copyOf(known(player)), true);
    }

    /** Once a second, learn whatever terpenes the player is carrying. */
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        List<String> carried = new ArrayList<>();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            String key = key(inventory.getItem(i));
            if (key != null) carried.add(key);
        }
        String held = key(player.containerMenu.getCarried());
        if (held != null) carried.add(held);
        if (!carried.isEmpty()) learn(player, carried);
    }
}
