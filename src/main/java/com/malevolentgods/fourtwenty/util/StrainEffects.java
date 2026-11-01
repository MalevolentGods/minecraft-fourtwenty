package com.malevolentgods.fourtwenty.util;

import com.malevolentgods.fourtwenty.domain.Strain;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.malevolentgods.fourtwenty.util.SmokeEffects.quiet;

/**
 * What each strain does when smoked. Effects are quiet (no swirl particles); the strain shows itself instead in the
 * colour of the smoke and a faint tint at the edges of the smoker's screen while the high lasts.
 */
public final class StrainEffects {
    private StrainEffects() {}

    private static final int MIN = 1200;

    /** How long the screen tint (and the auras) last, per strain. */
    public static int duration(Strain s) {
        return switch (s) {
            case LANDRACE -> 2 * MIN;
            case PURPLE_HAZE -> 5 * MIN;
            case THIRD_EYE_TONIC -> 90 * 20;
            default -> 3 * MIN;
        };
    }

    public static void apply(ServerLevel level, Player player, Strain strain) {
        SmokeEvents.smoked(player);
        switch (strain) {
            case LANDRACE -> {
                player.addEffect(quiet(MobEffects.REGENERATION, 600, 1));
                player.addEffect(quiet(MobEffects.NIGHT_VISION, 2 * MIN, 0));
                player.addEffect(quiet(MobEffects.HUNGER, 200, 1));
                player.addEffect(quiet(MobEffects.LUCK, 1800, 0));
            }
            case COUCHLOCK_HUM -> {
                player.addEffect(quiet(MobEffects.DAMAGE_RESISTANCE, 3 * MIN, 0));
                player.addEffect(quiet(MobEffects.MOVEMENT_SLOWDOWN, MIN, 0));
            }
            case WAKE_AND_BAKE -> {
                player.addEffect(quiet(MobEffects.DIG_SPEED, 3 * MIN, 0));
                player.addEffect(quiet(MobEffects.SATURATION, 20, 0));
            }
            case SPACEBAR_STATIC -> player.addEffect(quiet(MobEffects.LUCK, 3 * MIN, 1));
            case CLOUD_NINE_DRIFT -> player.addEffect(quiet(MobEffects.SLOW_FALLING, 2 * MIN, 0));
            case BROWNIE_BATCH -> player.addEffect(quiet(MobEffects.SATURATION, 60, 0));
            case SUNSHINE_DAYDREAM -> player.addEffect(quiet(MobEffects.MOVEMENT_SPEED, 3 * MIN, 0));
            case CHERRY_PIE -> player.addEffect(quiet(MobEffects.DAMAGE_BOOST, 2 * MIN, 0));
            case TANGIE_TANGO -> player.addEffect(quiet(MobEffects.FIRE_RESISTANCE, 3 * MIN, 0));
            case MOSSY_PHILOSOPHER -> { }                                    // the growing aura (tick)
            case LIME_IN_THE_COCONUT -> player.addEffect(quiet(MobEffects.JUMP, 3 * MIN, 0));
            case STICKY_ICKY -> player.addEffect(quiet(MobEffects.REGENERATION, 2 * MIN, 0));
            case MERMAID_HOTBOX -> {
                player.addEffect(quiet(MobEffects.WATER_BREATHING, 3 * MIN, 0));
                player.addEffect(quiet(MobEffects.DOLPHINS_GRACE, MIN, 0));
            }
            case PURPLE_HAZE -> {
                player.addEffect(quiet(MobEffects.NIGHT_VISION, 5 * MIN, 0));
            }
            case COTTON_CANDY_KUSH -> player.addEffect(quiet(MobEffects.ABSORPTION, 2 * MIN, 0));
            case BLUE_DREAM -> {
                player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));   // phantoms leave you be
                player.addEffect(quiet(MobEffects.REGENERATION, 600, 0));
            }
            case THIRD_EYE_TONIC -> { }                                      // the seeing aura (tick)
        }
        long until = level.getGameTime() + duration(strain);
        HIGHS.put(player.getUUID(), new High(strain, until));
        markHigh(player, duration(strain));
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new Tint(strain, duration(strain)));
            // smoking inspires colours (a colour drift on screen); with nothing new to inspire, sometimes just the drift
            if (!com.malevolentgods.fourtwenty.infuser.Infuser.inspire(sp, strain) && level.random.nextFloat() < 0.3f) {
                com.malevolentgods.fourtwenty.infuser.Infuser.woozy(sp);
            }
        }
    }

    // ---- auras: Mossy Philosopher grows crops around you, Third-Eye Tonic shows nearby monsters through walls

    private record High(Strain strain, long until) {}

    /** Everyone who's high, by when it wears off. Bongs and edibles land here too, without a strain's aura. Not saved: a high ends with the server. */
    private static final Map<UUID, Long> HIGH_UNTIL = new HashMap<>();

    public static void markHigh(Player player, int ticks) {
        HIGH_UNTIL.merge(player.getUUID(), player.level().getGameTime() + ticks, Math::max);
    }

    public static boolean isHigh(Player player) {
        Long until = HIGH_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    private static final Map<UUID, High> HIGHS = new HashMap<>();

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        Player player = event.getEntity();
        High h = HIGHS.get(player.getUUID());
        if (h == null) return;
        if (level.getGameTime() > h.until) { HIGHS.remove(player.getUUID()); return; }
        if (h.strain == Strain.MOSSY_PHILOSOPHER && level.getGameTime() % 40 == 0) {
            BlockPos at = player.blockPosition();
            for (int k = 0; k < 8; k++) {
                BlockPos p = at.offset(level.random.nextInt(11) - 5, level.random.nextInt(5) - 2, level.random.nextInt(11) - 5);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof CropBlock || s.getBlock() instanceof SaplingBlock) s.randomTick(level, p, level.random);
            }
        }
        if (h.strain == Strain.THIRD_EYE_TONIC && level.getGameTime() % 20 == 0) {
            for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(16),
                    e -> e instanceof Enemy && e.isAlive())) {
                mob.addEffect(quiet(MobEffects.GLOWING, 30, 0));
            }
        }
    }

    // ---- the client's screen tint

    /** Tells the smoker's client to tint the screen edges in the strain's colour for {@code ticks}. */
    public record Tint(Strain strain, int ticks) implements CustomPacketPayload {
        public static final Type<Tint> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "strain_tint"));
        public static final StreamCodec<ByteBuf, Tint> CODEC = StreamCodec.composite(
                Strain.STREAM_CODEC, Tint::strain, ByteBufCodecs.VAR_INT, Tint::ticks, Tint::new);

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Set on the client (by the client setup) to show a tint. */
    public static Consumer<Tint> clientTint = t -> {};

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(StrainEffects::tick);
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToClient(Tint.TYPE, Tint.CODEC,
                (payload, context) -> context.enqueueWork(() -> clientTint.accept(payload))));
    }
}
