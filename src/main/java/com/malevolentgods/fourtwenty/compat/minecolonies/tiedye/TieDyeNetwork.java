package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;

/**
 * The Tie Dye Shack's order payload (contracts/orders.md): a player adds an order or cancels one. An optional channel; every check is the
 * server's, and a refusal changes nothing and tells the player why.
 */
public final class TieDyeNetwork {
    private TieDyeNetwork() {}

    public enum Action { ADD, CANCEL }

    public record OrderAction(int colony, BlockPos building, Action action, int pattern, String colourway, TieDyeRules.Kind kind, String wood,
                              int count, int order) implements CustomPacketPayload {
        public static final Type<OrderAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "tie_dye_order"));
        public static final StreamCodec<ByteBuf, OrderAction> CODEC = StreamCodec.of((buf, p) -> {
            var b = new FriendlyByteBuf(buf);
            b.writeVarInt(p.colony); b.writeBlockPos(p.building); b.writeEnum(p.action); b.writeVarInt(p.pattern); b.writeUtf(p.colourway, 64);
            b.writeEnum(p.kind); b.writeUtf(p.wood, 64); b.writeVarInt(p.count); b.writeVarInt(p.order);
        }, buf -> {
            var b = new FriendlyByteBuf(buf);
            return new OrderAction(b.readVarInt(), b.readBlockPos(), b.readEnum(Action.class), b.readVarInt(), b.readUtf(64),
                    b.readEnum(TieDyeRules.Kind.class), b.readUtf(64), b.readVarInt(), b.readVarInt());
        });

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional().playToServer(OrderAction.TYPE, OrderAction.CODEC,
                (p, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    String refused = act(player, p);
                    if (refused != null) player.sendSystemMessage(Component.translatable(refused));
                })));
    }

    /** Carries out an order action for {@code player}; returns why it was refused, or null. */
    public static String act(ServerPlayer player, OrderAction p) {
        IColony colony = IColonyManager.getInstance().getColonyByWorld(p.colony(), player.level());
        if (colony == null || !colony.getPermissions().hasPermission(player, com.minecolonies.api.colony.permissions.Action.MANAGE_HUTS)) {
            return "tiedye.fourtwenty.refused.permission";
        }
        if (!(colony.getServerBuildingManager().getBuilding(p.building()) instanceof ShackBuilding)) return "tiedye.fourtwenty.refused.no_shack";
        return p.action() == Action.ADD
                ? TieDyeOrders.add(colony, player.getUUID(), p.pattern(), p.colourway(), p.kind(), p.wood(), p.count())
                : TieDyeOrders.cancel(colony, p.order());
    }
}
