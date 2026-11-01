package com.malevolentgods.fourtwenty.entity;

import com.malevolentgods.fourtwenty.block.Pollination;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * A little robot bee that runs on Cured Buds. With fuel in it, it wanders like a bee from weed plant to weed plant, never more than 7 blocks from one, and
 * where two ripe crops can cross it carries pollen between them, one bud a cross, for a guaranteed hybrid at the next harvest. Out of fuel it just hovers.
 * Hit it and it packs up, giving back its buds. Nothing else can hurt it.
 */
public class BuzzBot extends Mob {
    public static final int REACH = 7, MAX_FUEL = 16;
    private static final int LOOK_EVERY = 20, VISIT_TICKS = 30, DRIFT_TICKS = 8;
    private static final double SPEED = 0.12, ARRIVED = 0.5;

    private BlockPos lastPlant;
    private BlockPos from, to;
    private boolean carrying;
    private Vec3 wander;
    private int rest;
    private int fuel;

    public BuzzBot(EntityType<? extends BuzzBot> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4);
    }

    public int fuel() { return fuel; }

    public void addFuel(int buds) { fuel = Math.min(MAX_FUEL, fuel + buds); }

    @Override public boolean isPushable() { return false; }

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        setDeltaMovement(Vec3.ZERO);
        if (fuel <= 0 && from == null) return;
        if (from == null) {
            if (tickCount % LOOK_EVERY == 0) {
                BlockPos[] pair = Pollination.findPair(server, blockPosition(), REACH);
                if (pair != null) {
                    from = pair[0];
                    to = pair[1];
                    carrying = false;
                    return;
                }
            }
            wander(server);
            return;
        }
        if (!Pollination.canPollinate(server, from, to)) {
            from = to = null;
        } else if (flyTo(Vec3.atBottomCenterOf(carrying ? to : from).add(0, 0.7, 0))) {
            if (!carrying) {
                carrying = true;
            } else {
                if (Pollination.pollinate(server, from, to)) fuel--;
                from = to = null;
            }
        }
    }

    /** Drifts between weed plants, stopping at some as a bee would. With none in reach it goes back to the last one it knew. */
    private void wander(ServerLevel server) {
        if (wander != null && !flyTo(wander)) return;
        wander = null;
        if (rest > 0) {
            rest--;
            return;
        }
        BlockPos plant = Pollination.randomPlant(server, blockPosition(), REACH, random);
        if (plant != null) lastPlant = plant;
        if (lastPlant == null) return;
        if (plant != null && random.nextBoolean()) {
            wander = Vec3.atBottomCenterOf(plant).add(0, 0.7, 0);
            rest = VISIT_TICKS;
        } else {
            double angle = random.nextDouble() * Mth.TWO_PI, away = random.nextDouble() * (REACH - 1);
            wander = Vec3.atBottomCenterOf(lastPlant).add(Math.cos(angle) * away, 1 + random.nextDouble() * 2, Math.sin(angle) * away);
            rest = DRIFT_TICKS;
        }
    }

    /** One step toward {@code target}; true when it's there. */
    private boolean flyTo(Vec3 target) {
        Vec3 away = target.subtract(position());
        double distance = away.length();
        if (distance < ARRIVED) return true;
        Vec3 step = away.scale(Math.min(SPEED, distance) / distance);
        setPos(position().add(step));
        float yaw = (float) (Mth.atan2(away.z, away.x) * Mth.RAD_TO_DEG) - 90;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        return false;
    }

    @Override protected @Nonnull InteractionResult mobInteract(@Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.WEED_BUD.get()) && fuel < MAX_FUEL) {
            if (!level().isClientSide) {
                held.consume(1, player);
                fuel++;
                player.displayClientMessage(Component.translatable("entity.fourtwenty.buzz_bot.fuel", fuel, MAX_FUEL), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide) player.displayClientMessage(Component.translatable("entity.fourtwenty.buzz_bot.fuel", fuel, MAX_FUEL), true);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Only a player can do anything to it, and that packs it up. */
    @Override public boolean hurt(@Nonnull DamageSource source, float amount) {
        if (level().isClientSide || !(source.getEntity() instanceof Player)) return false;
        spawnAtLocation(ModPower.BUZZ_BOT_ITEM.get());
        if (fuel > 0) spawnAtLocation(new ItemStack(ModItems.WEED_BUD.get(), fuel));
        discard();
        return true;
    }

    @Override public void addAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuel", fuel);
        if (lastPlant != null) tag.put("LastPlant", NbtUtils.writeBlockPos(lastPlant));
    }

    @Override public void readAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fuel = tag.getInt("Fuel");
        lastPlant = NbtUtils.readBlockPos(tag, "LastPlant").orElse(null);
    }
}
