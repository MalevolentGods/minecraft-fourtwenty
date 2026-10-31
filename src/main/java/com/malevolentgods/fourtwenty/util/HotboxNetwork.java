package com.malevolentgods.fourtwenty.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Tells clients which Hotboxes are holding, wherever they are: the world outline and JourneyMap read {@link #boxes()}. */
public final class HotboxNetwork {
    private static final Set<Box> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile List<Box> received = List.of();

    private HotboxNetwork() {}

    public record Box(ResourceLocation dimension, BlockPos pos) {
        public static final StreamCodec<ByteBuf, Box> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Box::dimension, BlockPos.STREAM_CODEC, Box::pos, Box::new);

        public ResourceKey<Level> level() {
            return ResourceKey.create(Registries.DIMENSION, dimension);
        }

        /** The block bounds of its 3x3 chunks: {min x, min z, max x, max z}, the max exclusive. */
        public int[] area() {
            ChunkPos c = new ChunkPos(pos);
            return new int[] {(c.x - 1) * 16, (c.z - 1) * 16, (c.x + 2) * 16, (c.z + 2) * 16};
        }
    }

    public record Active(List<Box> boxes) implements CustomPacketPayload {
        public static final Type<Active> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "hotboxes"));
        public static final StreamCodec<ByteBuf, Active> CODEC = StreamCodec.composite(
                Box.CODEC.apply(ByteBufCodecs.list(4096)), Active::boxes, Active::new);

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToClient(Active.TYPE, Active.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    received = List.copyOf(payload.boxes());
                    LISTENERS.forEach(Runnable::run);
                })));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) send(player);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> ACTIVE.clear());
    }

    public static void set(ServerLevel level, BlockPos pos, boolean on) {
        Box box = new Box(level.dimension().location(), pos.immutable());
        if (on ? ACTIVE.add(box) : ACTIVE.remove(box)) level.getServer().getPlayerList().getPlayers().forEach(HotboxNetwork::send);
    }

    /** Only to clients that have the channel: a connection without FourTwenty can't take the payload. */
    private static void send(ServerPlayer player) {
        if (player.connection.hasChannel(Active.TYPE)) PacketDistributor.sendToPlayer(player, new Active(List.copyOf(ACTIVE)));
    }

    /** Client side: the Hotboxes the server last told us about. */
    public static List<Box> boxes() {
        return received;
    }

    public static void onChange(Runnable listener) {
        LISTENERS.add(listener);
    }
}
