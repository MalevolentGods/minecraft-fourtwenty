package com.malevolentgods.fourtwenty.compat.minecolonies.service;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryConfig;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Players buy from the Budtender like the cook feeds them: right-click him with an emerald in hand,
 * inside the shop. Colony members are served free; anyone else pays emeralds, which go into the
 * hut's storage. The menu is a server-driven vanilla chest screen, so clients need no extra UI.
 */
public final class PlayerSales {
    private static final ResourceLocation BUDTENDER_MODEL = ResourceLocation.parse("fourtwenty:budtender");

    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getTarget() instanceof EntityCitizen tender)
                || !event.getEntity().getItemInHand(InteractionHand.MAIN_HAND).is(Items.EMERALD)) return;
        if (event.getLevel().isClientSide()) {
            // The client only knows the profession by its model; skip the native citizen window.
            if (BUDTENDER_MODEL.equals(tender.getModelType())) cancel(event);
            return;
        }
        if (tender.getCitizenData() == null || !(tender.getCitizenData().getWorkBuilding() instanceof DispensaryBuilding hut)
                || !JointService.live(hut) || !(event.getEntity() instanceof ServerPlayer player)) return;
        cancel(event);
        if (!hut.isInBuilding(player.blockPosition()) && player.distanceToSqr(tender) > 25) {
            player.displayClientMessage(text("fourtwenty.dispensary.sales.come_in", "Come into the shop to buy."), true);
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SalesMenu(id, inventory, hut, tender),
                text("fourtwenty.dispensary.sales.title", "Dispensary menu")));
    }

    private static void cancel(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(key, fallback, args);
    }

    static Item item(MenuItem entry) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.itemId)); }

    /** Display-only chest: clicking a menu item buys one; nothing can be taken or placed. */
    static final class SalesMenu extends ChestMenu {
        private final SimpleContainer display;
        private final DispensaryBuilding hut;
        private final EntityCitizen tender;
        private final List<MenuItem> menu;

        SalesMenu(int id, Inventory inventory, DispensaryBuilding hut, EntityCitizen tender) {
            this(id, inventory, new SimpleContainer(9), hut, tender);
        }
        private SalesMenu(int id, Inventory inventory, SimpleContainer display, DispensaryBuilding hut, EntityCitizen tender) {
            super(MenuType.GENERIC_9x1, id, inventory, display, 1);
            this.display = display;
            this.hut = hut;
            this.tender = tender;
            this.menu = MenuItem.menu(hut.getBuildingLevel());
            refresh(inventory.player);
        }

        private boolean member(Player player) {
            return hut.getColony().getPermissions().hasPermission(player, Action.MANAGE_HUTS);
        }

        private List<IItemHandler> stock() {
            var sources = new ArrayList<IItemHandler>();
            sources.add(tender.getInventoryCitizen());
            var storage = hut.getItemHandlerCap();
            if (storage != null) sources.add(storage);
            return sources;
        }

        private int count(Item item) {
            int total = 0;
            for (var source : stock()) {
                for (int slot = 0; slot < source.getSlots(); slot++) {
                    if (source.getStackInSlot(slot).is(item)) total += source.getStackInSlot(slot).getCount();
                }
            }
            return total;
        }

        private void refresh(Player player) {
            boolean member = member(player);
            for (int slot = 0; slot < display.getContainerSize(); slot++) {
                if (slot >= menu.size()) { display.setItem(slot, ItemStack.EMPTY); continue; }
                var entry = menu.get(slot);
                int inStock = count(item(entry));
                int price = MenuItem.price(member, DispensaryConfig.price(entry));
                var shown = new ItemStack(item(entry));
                shown.set(DataComponents.LORE, new ItemLore(List.of(
                        price == 0 ? text("fourtwenty.dispensary.sales.free", "Free for colony members")
                                : text("fourtwenty.dispensary.sales.price", "Price: %s emerald(s)", price),
                        inStock > 0 ? text("fourtwenty.dispensary.sales.stock", "In stock: %s", inStock)
                                : text("fourtwenty.dispensary.sales.sold_out", "Sold out"))));
                display.setItem(slot, shown);
            }
        }

        @Override public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (slotId >= 0 && slotId < menu.size() && clickType == ClickType.PICKUP && player instanceof ServerPlayer buyer) {
                buy(buyer, menu.get(slotId));
                refresh(buyer);
            }
            // Never move items in this menu; resync whatever the client predicted.
            sendAllDataToRemote();
        }

        private void buy(ServerPlayer player, MenuItem entry) {
            Item wanted = item(entry);
            int price = MenuItem.price(member(player), DispensaryConfig.price(entry));
            if (player.getInventory().countItem(Items.EMERALD) < price) {
                player.displayClientMessage(text("fourtwenty.dispensary.sales.cost", "That costs %s emerald(s).", price), true);
                return;
            }
            for (var source : stock()) {
                for (int slot = 0; slot < source.getSlots(); slot++) {
                    if (!source.getStackInSlot(slot).is(wanted)) continue;
                    var sold = source.extractItem(slot, 1, false);
                    if (sold.isEmpty()) continue;
                    if (price > 0) pay(player, price);
                    if (!player.getInventory().add(sold)) player.drop(sold, false);
                    tender.swing(InteractionHand.MAIN_HAND);
                    player.displayClientMessage(text("fourtwenty.dispensary.sales.sold", "%s hands you a %s.",
                            tender.getName(), sold.getHoverName()), true);
                    return;
                }
            }
            player.displayClientMessage(text("fourtwenty.dispensary.sales.out", "%s is sold out.", new ItemStack(wanted).getHoverName()), true);
        }

        /** Emeralds go to the hut's storage (or the Budtender), never lost: overflow drops at his feet. */
        private void pay(ServerPlayer player, int price) {
            var inventory = player.getInventory();
            int left = price;
            for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
                var stack = inventory.getItem(slot);
                if (!stack.is(Items.EMERALD)) continue;
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
            var paid = new ItemStack(Items.EMERALD, price);
            var storage = hut.getItemHandlerCap();
            if (storage != null) paid = ItemHandlerHelper.insertItemStacked(storage, paid, false);
            if (!paid.isEmpty()) paid = ItemHandlerHelper.insertItemStacked(tender.getInventoryCitizen(), paid, false);
            if (!paid.isEmpty()) tender.spawnAtLocation(paid);
        }

        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player player) {
            return tender.isAlive() && player.distanceToSqr(tender) <= 64 && JointService.live(hut);
        }
    }

    private PlayerSales() {}
}
