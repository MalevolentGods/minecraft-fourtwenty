package com.malevolentgods.fourtwenty.handbook;

import com.malevolentgods.fourtwenty.registry.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The sash's own place on a player (specs/016-weedcraft-handbook FR-038): Four Twenty's own worn slot, so the sash is an accessory
 * on any install, worn alongside a chestplate, with nothing else needed. The worn sash is kept on the player (saved with them),
 * dropped on death like armour unless keepInventory is on, and whether someone wears one is sent to everyone who can see them.
 *
 * <p>Put on by right-clicking the sash, or through the sash slot drawn beside the player's figure in the inventory (the client
 * sends {@link ClickPayload}; the server swaps the cursor's item with the slot, so the menu's own sync carries the result).
 */
@EventBusSubscriber(modid = "fourtwenty")
public final class SashSlot {
    private SashSlot() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "fourtwenty");
    public static final Supplier<AttachmentType<ItemStack>> WORN = ATTACHMENTS.register("worn_sash",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.OPTIONAL_CODEC).build());

    /** Client side: the entity ids the server says wear a sash in this slot. Filled by the client; plain data, safe on a server. */
    public static final Set<Integer> CLIENT_WORN = ConcurrentHashMap.newKeySet();

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("fourtwenty", path); }

    /** Whether an entity wears a sash in the slot. */
    public record WornPayload(int entity, boolean worn) implements CustomPacketPayload {
        public static final Type<WornPayload> TYPE = new Type<>(id("sash_worn"));
        public static final StreamCodec<ByteBuf, WornPayload> CODEC = StreamCodec.of((buf, p) -> {
            var b = new FriendlyByteBuf(buf);
            b.writeVarInt(p.entity);
            b.writeBoolean(p.worn);
        }, buf -> {
            var b = new FriendlyByteBuf(buf);
            return new WornPayload(b.readVarInt(), b.readBoolean());
        });

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The player clicked the sash slot in their inventory. */
    public record ClickPayload() implements CustomPacketPayload {
        public static final Type<ClickPayload> TYPE = new Type<>(id("sash_click"));
        public static final StreamCodec<ByteBuf, ClickPayload> CODEC = StreamCodec.unit(new ClickPayload());

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    /** Called from HandbookNetwork's registrar (same optional channel version). */
    static void payloads(PayloadRegistrar reg) {
        reg.playToClient(WornPayload.TYPE, WornPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (p.worn()) CLIENT_WORN.add(p.entity()); else CLIENT_WORN.remove(p.entity());
        }));
        reg.playToServer(ClickPayload.TYPE, ClickPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) click(sp);
        }));
    }

    public static ItemStack worn(Player player) {
        return player.getData(WORN);
    }

    private static boolean isSash(ItemStack s) {
        return s.is(ModItems.STONER_BADGE_SASH.get());
    }

    /** Right-click with a sash: put one on if the slot is empty. */
    public static boolean wear(ServerPlayer player, ItemStack held) {
        if (!isSash(held) || !worn(player).isEmpty()) return false;
        set(player, held.split(1));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 1f);
        return true;
    }

    /** The inventory slot: the cursor's sash goes in, or the worn sash comes out onto the cursor, or the two swap. */
    private static void click(ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu) return;
        var menu = player.inventoryMenu;
        ItemStack carried = menu.getCarried(), on = worn(player);
        if (isSash(carried) && on.isEmpty()) {
            set(player, carried.split(1));
        } else if (carried.isEmpty() && !on.isEmpty()) {
            menu.setCarried(on);
            set(player, ItemStack.EMPTY);
        } else if (isSash(carried) && carried.getCount() == 1 && !on.isEmpty()) {
            menu.setCarried(on);
            set(player, carried);
        } else {
            return;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8f, 1f);
        menu.broadcastChanges();
    }

    private static void set(ServerPlayer player, ItemStack stack) {
        player.setData(WORN, stack);
        tell(player, !stack.isEmpty());
    }

    /** Tells the wearer and everyone near enough to see them. */
    private static void tell(ServerPlayer player, boolean worn) {
        var payload = new WornPayload(player.getId(), worn);
        for (ServerPlayer other : player.serverLevel().players()) {
            if ((other == player || other.distanceToSqr(player) <= 128 * 128) && other.connection.hasChannel(WornPayload.TYPE)) {
                PacketDistributor.sendToPlayer(other, payload);
            }
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) tell(p, !worn(p).isEmpty());
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) tell(p, !worn(p).isEmpty());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) tell(p, !worn(p).isEmpty());
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer
                && viewer.connection.hasChannel(WornPayload.TYPE)) {
            PacketDistributor.sendToPlayer(viewer, new WornPayload(target.getId(), !worn(target).isEmpty()));
        }
    }

    /** Dropped on death like armour, unless keepInventory. */
    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        ItemStack on = worn(p);
        if (on.isEmpty()) return;
        event.getDrops().add(new ItemEntity(p.level(), p.getX(), p.getY(), p.getZ(), on.copy()));
        p.setData(WORN, ItemStack.EMPTY);
    }

    /** Kept through death with keepInventory (other respawns keep it anyway). */
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        event.getEntity().setData(WORN, worn(event.getOriginal()).copy());
    }
}
