package com.malevolentgods.fourtwenty.compat.minecolonies.college;

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
 * The Weed College board's one payload (contracts/board-actions.md): a player queues a study or takes one off the queue. An optional
 * channel; the client sends it only when the server has it. Every check is the server's: a failed one changes nothing and tells the
 * player why.
 */
public final class CollegeNetwork {
    private CollegeNetwork() {}

    public enum Action { QUEUE, REMOVE }

    public record BoardAction(int colony, BlockPos building, Action action, String study) implements CustomPacketPayload {
        public static final Type<BoardAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "college_board"));
        public static final StreamCodec<ByteBuf, BoardAction> CODEC = StreamCodec.of((buf, p) -> {
            var b = new FriendlyByteBuf(buf);
            b.writeVarInt(p.colony);
            b.writeBlockPos(p.building);
            b.writeEnum(p.action);
            b.writeUtf(p.study, 128);
        }, buf -> {
            var b = new FriendlyByteBuf(buf);
            return new BoardAction(b.readVarInt(), b.readBlockPos(), b.readEnum(Action.class), b.readUtf(128));
        });

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional().playToServer(BoardAction.TYPE, BoardAction.CODEC,
                (p, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    String refused = act(player, p.colony(), p.building(), p.action(), p.study());
                    if (refused != null) player.sendSystemMessage(Component.translatable(refused));
                })));
    }

    /**
     * Carries out a board action for {@code player}: the colony must exist in the player's dimension and give them MineColonies'
     * {@code MANAGE_HUTS}, and a Weed College must stand at {@code pos}. Returns why it was refused, or null when done.
     */
    public static String act(ServerPlayer player, int colonyId, BlockPos pos, Action action, String study) {
        IColony colony = IColonyManager.getInstance().getColonyByWorld(colonyId, player.level());
        if (colony == null || !colony.getPermissions().hasPermission(player, com.minecolonies.api.colony.permissions.Action.MANAGE_HUTS)) {
            return "college.fourtwenty.refused.permission";
        }
        if (!(colony.getServerBuildingManager().getBuilding(pos) instanceof CollegeBuilding)) return "college.fourtwenty.refused.no_college";
        return action == Action.QUEUE ? CollegeResearch.queue(colony, study) : CollegeResearch.remove(colony, study);
    }
}
