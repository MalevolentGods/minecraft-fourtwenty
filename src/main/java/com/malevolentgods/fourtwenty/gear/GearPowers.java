package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.GearRules;
import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The Kush Suit's powers. Each asks {@link Gear#spend} before it acts, so every one works only while its wearer is high and the piece
 * can pay. Running powers are checked and paid once a second, for the whole second.
 */
public final class GearPowers {
    private GearPowers() {}

    public static final String PLATE_FLIGHT = "puff_plate";
    private static final int SECOND = 20, EFFECT_TICKS = 50, NIGHT_VISION_TICKS = 260;
    private static final ResourceLocation TOUGHNESS = ResourceLocation.fromNamespaceAndPath("fourtwenty", "puff_plate_toughness");

    public static void register() {
        FlightLease.register();
        NeoForge.EVENT_BUS.addListener(GearPowers::tick);
        NeoForge.EVENT_BUS.addListener(GearPowers::knockback);
        NeoForge.EVENT_BUS.addListener(GearPowers::fall);
        NeoForge.EVENT_BUS.addListener(GearPowers::jump);
    }

    private static ItemStack helm(Player p) { return Gear.worn(p, EquipmentSlot.HEAD, Piece.HOTBOX_HELM); }
    private static ItemStack plate(Player p) { return Gear.worn(p, EquipmentSlot.CHEST, Piece.PUFF_PLATE); }
    private static ItemStack greaves(Player p) { return Gear.worn(p, EquipmentSlot.LEGS, Piece.COUCHLOCK_GREAVES); }
    private static ItemStack boots(Player p) { return Gear.worn(p, EquipmentSlot.FEET, Piece.MOON_BOOTS); }

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        flight(player);
        if (player.tickCount % SECOND != 0) return;
        helmSecond(player);
        greavesSecond(player);
        bootsSecond(player);
        plateToughness(player);
    }

    /** Pays a running ability for a second: {@code perTick} times twenty. */
    private static boolean second(Player p, ItemStack stack, int perTick) {
        return !stack.isEmpty() && Gear.spend(p, stack, perTick * SECOND);
    }

    private static boolean ability(Player p, ItemStack stack, String id) {
        return Gear.has(stack, id) && second(p, stack, Gear.cost(stack, id));
    }

    private static void give(Player p, net.minecraft.core.Holder<MobEffect> effect, int ticks, int amplifier) {
        p.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, false, true));
    }

    // ---- Hotbox Helm: night vision, ore and mob sense (the HUD is client side)

    private static void helmSecond(ServerPlayer p) {
        ItemStack helm = helm(p);
        if (!second(p, helm, GearConfig.nightVision())) return;
        give(p, MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0);
        ServerLevel level = p.serverLevel();
        if (ability(p, helm, "ore_sense")) {
            BlockPos at = p.blockPosition();
            int r = 8;
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -r, -r), at.offset(r, r, r))) {
                if (level.getBlockState(pos).is(Tags.Blocks.ORES))
                    level.sendParticles(p, ParticleTypes.WAX_ON, true, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0);
            }
        }
        if (ability(p, helm, "mob_sense")) {
            for (var mob : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24), e -> e instanceof Enemy && e.isAlive()))
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, false));
        }
    }

    // ---- Puff Plate: the Puff Pack. Lent flight, paid every tick it's used; anything else ends it, softly.

    private static void flight(ServerPlayer p) {
        ItemStack plate = plate(p);
        boolean flying = p.getAbilities().flying;
        boolean still = p.getDeltaMovement().lengthSqr() < 0.0025;
        int cost = flying && still && Gear.has(plate, "hover") ? Gear.cost(plate, "hover") : GearConfig.flight();
        boolean keep = GearRules.keepFlying(com.malevolentgods.fourtwenty.util.StrainEffects.isHigh(p), Gear.charge(plate), cost, !plate.isEmpty());
        if (!keep) {
            FlightLease.release(p, PLATE_FLIGHT);
            return;
        }
        if (!FlightLease.holds(p, PLATE_FLIGHT)) FlightLease.lend(p, PLATE_FLIGHT);
        if (flying) {
            Gear.spend(p, plate, cost);
            boolean fast = Gear.has(plate, "fast_flight") && Gear.spend(p, plate, Gear.cost(plate, "fast_flight"));
            float speed = fast ? FlightLease.NORMAL_FLY_SPEED * 2 : FlightLease.NORMAL_FLY_SPEED;
            if (p.getAbilities().getFlyingSpeed() != speed) {
                p.getAbilities().setFlyingSpeed(speed);
                p.onUpdateAbilities();
            }
        }
    }

    private static void plateToughness(ServerPlayer p) {
        var attr = p.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (attr == null) return;
        boolean on = ability(p, plate(p), "toughness");
        boolean has = attr.getModifier(TOUGHNESS) != null;
        if (on && !has) attr.addTransientModifier(new AttributeModifier(TOUGHNESS, 3.0, AttributeModifier.Operation.ADD_VALUE));
        else if (!on && has) attr.removeModifier(TOUGHNESS);
    }

    // ---- Couchlock Greaves: no knockback, the stomp, resistance, slow fall

    private static void greavesSecond(ServerPlayer p) {
        ItemStack greaves = greaves(p);
        if (greaves.isEmpty()) return;
        if (ability(p, greaves, "resistance")) give(p, MobEffects.DAMAGE_RESISTANCE, EFFECT_TICKS, 0);
        if (p.getDeltaMovement().y < -0.5 && !p.isShiftKeyDown() && ability(p, greaves, "slow_fall")) give(p, MobEffects.SLOW_FALLING, EFFECT_TICKS, 0);
    }

    private static void knockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Gear.spend(p, greaves(p), GearConfig.knockback())) event.setCanceled(true);
    }

    // ---- Moon Boots: a high jump, no fall damage, water walking, speed

    private static void bootsSecond(ServerPlayer p) {
        ItemStack boots = boots(p);
        if (boots.isEmpty()) return;
        if (ability(p, boots, "speed")) give(p, MobEffects.MOVEMENT_SPEED, EFFECT_TICKS, 0);
        if (Gear.has(boots, "water_walk")) waterWalk(p, boots);
    }

    private static void waterWalk(ServerPlayer p, ItemStack boots) {
        if (p.isShiftKeyDown() || !p.level().getFluidState(p.blockPosition()).isSource() && !p.isInWater()) return;
        if (!p.level().getBlockState(p.blockPosition().above()).isAir()) return;   // only at the surface
        if (!ability(p, boots, "water_walk")) return;
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, Math.max(v.y, 0.12), v.z);
        p.hurtMarked = true;
        p.resetFallDistance();
    }

    private static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack boots = boots(p);
        if (boots.isEmpty() || !Gear.spend(p, boots, GearConfig.jump())) return;
        double boost = Gear.has(boots, "higher_jump") && Gear.spend(p, boots, Gear.cost(boots, "higher_jump")) ? 0.6 : 0.35;
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, v.y + boost, v.z);
        p.hurtMarked = true;
    }

    /** Landing: the Boots soak it up; crouch-landing in the Greaves stomps. */
    private static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        float distance = event.getDistance();
        ItemStack greaves = greaves(p);
        if (!greaves.isEmpty() && GearRules.stomps(distance, p.isShiftKeyDown())) {
            boolean big = Gear.has(greaves, "big_stomp");
            int cost = GearConfig.stomp() + (big ? Gear.cost(greaves, "big_stomp") : 0);
            if (Gear.spend(p, greaves, cost)) stomp(p, distance, big ? 5 : 3);
        }
        ItemStack boots = boots(p);
        if (distance > 3 && !boots.isEmpty() && Gear.spend(p, boots, GearConfig.fall())) {
            event.setDamageMultiplier(0);
            event.setDistance(0);
        }
    }

    private static void stomp(ServerPlayer p, float distance, int radius) {
        ServerLevel level = p.serverLevel();
        float damage = GearRules.stompDamage(distance, GearConfig.stompDamage());
        boolean pvp = level.getServer().isPvpAllowed();
        for (var target : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius),
                e -> e != p && e.isAlive() && (!(e instanceof Player) || pvp))) {
            target.hurt(level.damageSources().playerAttack(p), damage);
            Vec3 away = target.position().subtract(p.position()).normalize();
            target.push(away.x * 0.8, 0.4, away.z * 0.8);
            target.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 0.2, p.getZ(), 24, radius * 0.4, 0.1, radius * 0.4, 0.01);
        level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.MACE_SMASH_GROUND_HEAVY, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.9f);
    }
}
