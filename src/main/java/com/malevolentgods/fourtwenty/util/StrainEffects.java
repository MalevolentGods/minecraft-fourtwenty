package com.malevolentgods.fourtwenty.util;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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
            case GOLDEN_GOD -> 5 * MIN;
            default -> switch (s.grade()) {
                case 2 -> 4 * MIN;
                case 3 -> 5 * MIN;
                case 4 -> 6 * MIN;
                default -> 3 * MIN;
            };
        };
    }

    /** A strain's helpful effect, as strong and as long as the smoke's traits make it. */
    private static void give(Player player, Traits t, Holder<MobEffect> effect, int ticks, int level) {
        player.addEffect(quiet(effect, (int) (ticks * t.lengthFactor()), Math.max(0, level + t.potencyBonus())));
    }

    private static void noPhantoms(Player player) {
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
    }

    public static void apply(ServerLevel level, Player player, Strain strain) {
        apply(level, player, strain, Traits.DEFAULT);
    }

    public static void apply(ServerLevel level, Player player, Strain strain, Traits t) {
        SmokeEvents.smoked(player);
        switch (strain) {
            case LANDRACE -> {
                give(player, t, MobEffects.REGENERATION, 600, 1);
                give(player, t, MobEffects.NIGHT_VISION, 2 * MIN, 0);
                player.addEffect(quiet(MobEffects.HUNGER, 200, 1));
                give(player, t, MobEffects.LUCK, 1800, 0);
            }
            case COUCHLOCK_HUM -> {
                give(player, t, MobEffects.DAMAGE_RESISTANCE, 3 * MIN, 0);
                player.addEffect(quiet(MobEffects.MOVEMENT_SLOWDOWN, MIN, 0));
            }
            case WAKE_AND_BAKE -> {
                give(player, t, MobEffects.DIG_SPEED, 3 * MIN, 0);
                give(player, t, MobEffects.SATURATION, 20, 0);
            }
            case SPACEBAR_STATIC -> give(player, t, MobEffects.LUCK, 3 * MIN, 1);
            case CLOUD_NINE_DRIFT -> give(player, t, MobEffects.SLOW_FALLING, 2 * MIN, 0);
            case BROWNIE_BATCH -> give(player, t, MobEffects.SATURATION, 60, 0);
            case SUNSHINE_DAYDREAM -> give(player, t, MobEffects.MOVEMENT_SPEED, 3 * MIN, 0);
            case CHERRY_PIE -> give(player, t, MobEffects.DAMAGE_BOOST, 2 * MIN, 0);
            case TANGIE_TANGO -> give(player, t, MobEffects.FIRE_RESISTANCE, 3 * MIN, 0);
            case MOSSY_PHILOSOPHER -> { }                                    // the growing aura (tick)
            case LIME_IN_THE_COCONUT -> give(player, t, MobEffects.JUMP, 3 * MIN, 0);
            case STICKY_ICKY -> give(player, t, MobEffects.REGENERATION, 2 * MIN, 0);
            case MERMAID_HOTBOX -> {
                give(player, t, MobEffects.WATER_BREATHING, 3 * MIN, 0);
                give(player, t, MobEffects.DOLPHINS_GRACE, MIN, 0);
            }
            case PURPLE_HAZE -> {
                give(player, t, MobEffects.NIGHT_VISION, 5 * MIN, 0);
            }
            case COTTON_CANDY_KUSH -> give(player, t, MobEffects.ABSORPTION, 2 * MIN, 0);
            case BLUE_DREAM -> {
                noPhantoms(player);
                give(player, t, MobEffects.REGENERATION, 600, 0);
            }
            case THIRD_EYE_TONIC -> { }                                      // the seeing aura (tick)
            case DUST_DEVIL -> {
                give(player, t, MobEffects.FIRE_RESISTANCE, 3 * MIN, 0);
                give(player, t, MobEffects.DIG_SPEED, 3 * MIN, 0);
            }
            case SNOWBLIND -> {
                give(player, t, MobEffects.MOVEMENT_SPEED, 3 * MIN, 0);
                give(player, t, MobEffects.DAMAGE_RESISTANCE, 2 * MIN, 0);
            }
            case BOG_BUTTER -> {
                give(player, t, MobEffects.WATER_BREATHING, 3 * MIN, 0);
                give(player, t, MobEffects.REGENERATION, 2 * MIN, 0);
            }
            case CANOPY_CREEPER -> {
                give(player, t, MobEffects.JUMP, 3 * MIN, 0);
                give(player, t, MobEffects.SLOW_FALLING, 2 * MIN, 0);
            }
            case TROPICAL_THUNDER -> {
                give(player, t, MobEffects.WATER_BREATHING, 4 * MIN, 0);
                give(player, t, MobEffects.FIRE_RESISTANCE, 4 * MIN, 0);
                give(player, t, MobEffects.DOLPHINS_GRACE, 2 * MIN, 0);
            }
            case RAZZLE_DAZZLE -> {
                give(player, t, MobEffects.DAMAGE_BOOST, 3 * MIN, 1);
                give(player, t, MobEffects.ABSORPTION, 3 * MIN, 1);
            }
            case ALARM_CLOCK_ASSASSIN -> {
                give(player, t, MobEffects.DIG_SPEED, 4 * MIN, 1);
                give(player, t, MobEffects.MOVEMENT_SPEED, 4 * MIN, 1);
            }
            case COUCH_FORT -> {
                give(player, t, MobEffects.DAMAGE_RESISTANCE, 4 * MIN, 1);
                give(player, t, MobEffects.SATURATION, 100, 0);
            }
            case MOSS_BOSS -> give(player, t, MobEffects.REGENERATION, 3 * MIN, 1);
            case DEEP_THOUGHTS -> give(player, t, MobEffects.LUCK, 4 * MIN, 1);
            case LUCID_LAGOON -> {
                noPhantoms(player);
                give(player, t, MobEffects.SLOW_FALLING, 4 * MIN, 0);
                give(player, t, MobEffects.NIGHT_VISION, 5 * MIN, 0);
            }
            case ROCKET_LIME -> {
                give(player, t, MobEffects.JUMP, 4 * MIN, 2);
                give(player, t, MobEffects.NIGHT_VISION, 4 * MIN, 0);
            }
            case HURRICANE_HAZE -> {
                give(player, t, MobEffects.WATER_BREATHING, 5 * MIN, 0);
                give(player, t, MobEffects.CONDUIT_POWER, 5 * MIN, 0);
                give(player, t, MobEffects.MOVEMENT_SPEED, 5 * MIN, 1);
            }
            case SLEDGEHAMMER_SERENADE -> {
                give(player, t, MobEffects.DAMAGE_BOOST, 4 * MIN, 2);
                give(player, t, MobEffects.DAMAGE_RESISTANCE, 4 * MIN, 2);
            }
            case CAFFEINE_COMET -> {
                give(player, t, MobEffects.MOVEMENT_SPEED, 5 * MIN, 2);
                give(player, t, MobEffects.DIG_SPEED, 5 * MIN, 2);
                give(player, t, MobEffects.JUMP, 5 * MIN, 2);
            }
            case OLD_GROWTH -> give(player, t, MobEffects.REGENERATION, 4 * MIN, 1);
            case ORACLE_OF_SNACKS -> {
                give(player, t, MobEffects.LUCK, 5 * MIN, 2);
                give(player, t, MobEffects.SATURATION, 140, 0);
            }
            case POLAR_VORTEX -> {
                give(player, t, MobEffects.SLOW_FALLING, 5 * MIN, 0);
                give(player, t, MobEffects.DAMAGE_RESISTANCE, 5 * MIN, 1);
            }
            case COSMIC_COUCH -> {
                give(player, t, MobEffects.DAMAGE_RESISTANCE, 6 * MIN, 3);
                give(player, t, MobEffects.SLOW_FALLING, 6 * MIN, 0);
                give(player, t, MobEffects.REGENERATION, 6 * MIN, 2);
            }
            case STORM_SHEPHERD -> {
                give(player, t, MobEffects.MOVEMENT_SPEED, 6 * MIN, 3);
                give(player, t, MobEffects.DIG_SPEED, 6 * MIN, 3);
            }
            case THE_ELDER_BUD -> {
                give(player, t, MobEffects.LUCK, 6 * MIN, 3);
                give(player, t, MobEffects.HERO_OF_THE_VILLAGE, 6 * MIN, 0);
            }
            case GOLDEN_GOD -> {
                give(player, t, MobEffects.DAMAGE_RESISTANCE, 5 * MIN, 3);
                give(player, t, MobEffects.DAMAGE_BOOST, 5 * MIN, 2);
                give(player, t, MobEffects.REGENERATION, 5 * MIN, 2);
                give(player, t, MobEffects.MOVEMENT_SPEED, 5 * MIN, 2);
                give(player, t, MobEffects.DIG_SPEED, 5 * MIN, 2);
                give(player, t, MobEffects.FIRE_RESISTANCE, 5 * MIN, 0);
            }
        }
        int length = (int) (duration(strain) * t.lengthFactor());
        long until = level.getGameTime() + length;
        HIGHS.put(player.getUUID(), new High(strain, until));
        markHigh(player, length);
        if (player instanceof ServerPlayer sp) {
            if (strain == Strain.GOLDEN_GOD) GoldenGod.ascend(sp, length);
            PacketDistributor.sendToPlayer(sp, new Tint(strain, length));
            // smoking inspires colours (a colour drift on screen); with nothing new to inspire, sometimes just the drift
            if (!com.malevolentgods.fourtwenty.infuser.Infuser.inspire(sp, strain) && level.random.nextFloat() < 0.3f) {
                com.malevolentgods.fourtwenty.infuser.Infuser.woozy(sp);
            }
        }
    }

    // ---- auras: some strains grow crops around you, some show nearby monsters through walls, Polar Vortex slows them

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
    private static final Map<UUID, Long> NEXT_BOLT = new HashMap<>();
    private static final int BOLT_EVERY = 40;
    private static final float BOLT_DAMAGE = 8;

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        Player player = event.getEntity();
        High h = HIGHS.get(player.getUUID());
        if (h == null) return;
        if (level.getGameTime() > h.until) { HIGHS.remove(player.getUUID()); return; }
        int grow = growingAura(h.strain);
        if (grow > 0 && level.getGameTime() % 40 == 0) {
            BlockPos at = player.blockPosition();
            for (int k = 0; k < grow + 3; k++) {
                BlockPos p = at.offset(level.random.nextInt(grow * 2 + 1) - grow, level.random.nextInt(5) - 2, level.random.nextInt(grow * 2 + 1) - grow);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof CropBlock || s.getBlock() instanceof SaplingBlock) s.randomTick(level, p, level.random);
            }
        }
        int see = seeingAura(h.strain);
        if (see > 0 && level.getGameTime() % 20 == 0) {
            for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(see),
                    e -> e instanceof Enemy && e.isAlive())) {
                mob.addEffect(quiet(MobEffects.GLOWING, 30, 0));
            }
        }
        if (h.strain == Strain.POLAR_VORTEX && level.getGameTime() % 20 == 0) {
            for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(6),
                    e -> e instanceof Enemy && e.isAlive())) {
                mob.addEffect(quiet(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            }
        }
    }

    private static int growingAura(Strain strain) {
        return switch (strain) {
            case MOSSY_PHILOSOPHER, MOSS_BOSS -> 5;
            case OLD_GROWTH, THE_ELDER_BUD -> 8;
            default -> 0;
        };
    }

    private static int seeingAura(Strain strain) {
        return switch (strain) {
            case THIRD_EYE_TONIC, DEEP_THOUGHTS -> 16;
            case ORACLE_OF_SNACKS, THE_ELDER_BUD -> 32;
            default -> 0;
        };
    }

    /** Storm Shepherd: what you hit is struck by lightning that can't touch you. */
    private static void onHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) return;
        High h = HIGHS.get(player.getUUID());
        if (h == null || h.strain != Strain.STORM_SHEPHERD || level.getGameTime() > h.until || event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING)) return;
        long now = level.getGameTime();
        if (NEXT_BOLT.getOrDefault(player.getUUID(), 0L) > now) return;
        NEXT_BOLT.put(player.getUUID(), now + BOLT_EVERY);
        var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.setVisualOnly(true);
        bolt.moveTo(event.getEntity().position());
        level.addFreshEntity(bolt);
        event.getEntity().hurt(level.damageSources().lightningBolt(), BOLT_DAMAGE);
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
        NeoForge.EVENT_BUS.addListener(StrainEffects::onHit);
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToClient(Tint.TYPE, Tint.CODEC,
                (payload, context) -> context.enqueueWork(() -> clientTint.accept(payload))));
    }
}
