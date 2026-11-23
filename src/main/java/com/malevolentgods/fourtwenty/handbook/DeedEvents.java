package com.malevolentgods.fourtwenty.handbook;

import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the game already announces, turned into Handbook deeds (specs/016-weedcraft-handbook, contracts/scouting-data.md): crafting,
 * placing, picking up, planting a seed, breaking a ripe crop, and holding an Inspoweedometer.
 */
@EventBusSubscriber(modid = "fourtwenty")
public final class DeedEvents {
    private DeedEvents() {}

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack out = event.getCrafting();
        if (out.isEmpty()) return;
        Deeds.fire(player, "craft", Map.of("item", Deeds.id(out.getItem())), Math.max(1, out.getCount()));
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Deeds.fire(player, "place", "block", Deeds.id(event.getPlacedBlock().getBlock()));
    }

    /** A ripe weed crop broken by a player (Actually Harvest and the like come through here; a right-click harvest is counted in the block). */
    @SubscribeEvent
    public static void dropped(BlockDropsEvent event) {
        var state = event.getState();
        if (!(state.getBlock() instanceof WeedCropBlock crop) || !crop.isMaxAge(state) || !(event.getBreaker() instanceof ServerPlayer player)) return;
        Strain strain = state.getValue(WeedCropBlock.STRAIN);
        Deeds.fire(player, "harvest", "strain", strain.id(), "grade", String.valueOf(strain.grade()),
                "block", Deeds.id(event.getLevel().getBlockState(event.getPos().below()).getBlock()));
    }

    // ---- planting: a seed used on something and gone a moment later

    private record Pending(InteractionHand hand, int before, Strain strain, long at) {}

    private static final Map<UUID, Pending> PLANTING = new HashMap<>();

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getItemStack().is(ModItems.WEED_SEEDS.get())) return;
        PLANTING.put(player.getUUID(), new Pending(event.getHand(), event.getItemStack().getCount(), ModDataComponents.strain(event.getItemStack()),
                player.level().getGameTime()));
    }

    // ---- once a second: what's in the pocket, and the meter in the hand

    private static Set<String> wanted;

    private static Set<String> wantedItems() {
        if (wanted == null) {
            Set<String> out = new HashSet<>();
            for (var d : ScoutingData.defs()) {
                for (var r : d.reqs()) {
                    if ("obtain".equals(r.deed()) && r.subject().containsKey("item")) out.add(r.subject().get("item"));
                }
            }
            wanted = out;
        }
        return wanted;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Pending p = PLANTING.get(player.getUUID());
        if (p != null && player.level().getGameTime() > p.at) {
            PLANTING.remove(player.getUUID());
            ItemStack now = player.getItemInHand(p.hand);
            if (now.isEmpty() || now.getCount() < p.before || !now.is(ModItems.WEED_SEEDS.get())) {
                Deeds.fire(player, "plant", "strain", p.strain.id(), "grade", String.valueOf(p.strain.grade()));
            }
        }
        if (player.tickCount % 20 != 0) return;
        Set<String> items = new HashSet<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            String id = Deeds.id(s.getItem());
            if (wantedItems().contains(id)) items.add(id);
        }
        for (String id : items) Deeds.fire(player, "obtain", "item", id);
        if (isMeter(player.getMainHandItem()) || isMeter(player.getOffhandItem())) Deeds.fire(player, "measure");
    }

    private static boolean isMeter(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof com.malevolentgods.fourtwenty.power.InspoweedometerItem;
    }

    @SubscribeEvent
    public static void gone(PlayerEvent.PlayerLoggedOutEvent event) {
        PLANTING.remove(event.getEntity().getUUID());
    }

}
