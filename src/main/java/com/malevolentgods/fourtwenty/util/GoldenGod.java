package com.malevolentgods.fourtwenty.util;

import com.malevolentgods.fourtwenty.registry.ModPower;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Golden God's high: the smoker shines gold for everyone and can fly until it wears off. Flight can never kill you: when it ends they float down. Flight is only
 * ever lent: a player who could already fly keeps theirs, and a mark in their saved data takes back any flight left over from a crash.
 */
public final class GoldenGod {
    private GoldenGod() {}

    private static final String LENT = "fourtwenty.god_flight";
    public static final int LIGHT_LASTS = 8;
    private static final int LANDING = 1200, LIGHT_EVERY = 3;
    private static final double LIGHT_REACH = 2.5;
    private static final Map<UUID, Long> GODS = new HashMap<>();

    public static boolean shining(ServerPlayer player) { return GODS.containsKey(player.getUUID()); }

    public static void ascend(ServerPlayer player, int ticks) {
        GODS.put(player.getUUID(), player.level().getGameTime() + ticks);
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
            player.getPersistentData().putBoolean(LENT, true);
        }
        shine(player, true);
    }

    /** Ends it, whatever ended it. */
    public static void descend(ServerPlayer player) {
        if (GODS.remove(player.getUUID()) != null) shine(player, false);
        takeBackFlight(player);
    }

    private static void takeBackFlight(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(LENT)) return;
        player.getPersistentData().remove(LENT);
        boolean inAir = !player.onGround();
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        if (inAir && player.isAlive()) player.addEffect(SmokeEffects.quiet(MobEffects.SLOW_FALLING, LANDING, 0));
    }

    private static void shine(ServerPlayer player, boolean on) {
        Shine shine = new Shine(player.getId(), on);
        for (ServerPlayer viewer : player.server.getPlayerList().getPlayers()) {
            if (viewer.connection.hasChannel(Shine.TYPE)) viewer.connection.send(shine);
        }
    }

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Long until = GODS.get(player.getUUID());
        if (until == null) return;
        if (player.level().getGameTime() > until) descend(player);
        else if (player.level().getGameTime() % LIGHT_EVERY == 0) shed(player);
    }

    /** Keeps a block of bright light at their head. Only ever in air, and it clears itself. */
    public static void shed(ServerPlayer player) {
        BlockPos at = BlockPos.containing(player.getEyePosition());
        var level = player.serverLevel();
        if (!level.getBlockState(at).is(Blocks.AIR)) return;
        level.setBlock(at, ModPower.GOD_LIGHT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        level.scheduleTick(at, ModPower.GOD_LIGHT.get(), LIGHT_LASTS);
    }

    /** Whether a god is close enough to a block of their light to keep it on. It overlaps the next one as they walk, so the light never drops out. */
    public static boolean lighting(Level level, BlockPos pos) {
        return level.players().stream().anyMatch(p -> GODS.containsKey(p.getUUID()) && BlockPos.containing(p.getEyePosition()).closerThan(pos, LIGHT_REACH));
    }

    private static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        takeBackFlight(player);
        if (!player.connection.hasChannel(Shine.TYPE)) return;
        for (ServerPlayer god : player.server.getPlayerList().getPlayers()) {
            if (shining(god)) player.connection.send(new Shine(god.getId(), true));
        }
    }

    /** Tells a client someone's shining, or has stopped. */
    public record Shine(int entity, boolean on) implements CustomPacketPayload {
        public static final Type<Shine> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "golden_god"));
        public static final StreamCodec<ByteBuf, Shine> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Shine::entity, ByteBufCodecs.BOOL, Shine::on, Shine::new);

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Set on the client, by client/GoldenGodClient. */
    public static Consumer<Shine> clientShine = s -> {};

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(GoldenGod::tick);
        NeoForge.EVENT_BUS.addListener(GoldenGod::loggedIn);
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) descend(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) descend(player);
        });
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToClient(Shine.TYPE, Shine.CODEC,
                (payload, context) -> context.enqueueWork(() -> clientShine.accept(payload))));
    }
}
