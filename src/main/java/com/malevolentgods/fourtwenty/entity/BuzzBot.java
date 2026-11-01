package com.malevolentgods.fourtwenty.entity;

import com.malevolentgods.fourtwenty.block.Pollination;
import com.malevolentgods.fourtwenty.power.MachinePower;
import com.malevolentgods.fourtwenty.power.StashBlockEntity;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import javax.annotation.Nullable;
import java.util.List;

/**
 * A little robot bee. One Cured Bud runs it for about an hour, and it runs free while a high player is near. Running, it wanders like a bee from weed plant to
 * weed plant, never more than 16 blocks from one, and now and then, where two ripe crops can cross, it carries pollen between them for a guaranteed hybrid at
 * the next harvest.
 * Running low, it tops itself up at any Stash in reach. Out of power, it flies back to the block it was set down in and sleeps until it has power again.
 * Hit it and it packs up, giving back its unspent buds. Nothing else can hurt it.
 */
public class BuzzBot extends Mob {
    public static final int REACH = 16, MAX_BUDS = 16;
    private static final EntityDataAccessor<Boolean> DORMANT = SynchedEntityData.defineId(BuzzBot.class, EntityDataSerializers.BOOLEAN);
    private static final int LOOK_EVERY = 20, SURVEY_EVERY = 200, VISIT_TICKS = 80, DRIFT_TICKS = 40, LOW_POWER = 6000;
    private static final double SPEED = 0.08, ARRIVED = 0.5;

    private BlockPos home;
    private BlockPos lastPlant;
    private BlockPos from, to;
    private BlockPos charger;
    private boolean carrying, high;
    private Vec3 wander;
    private int rest;
    private int power;
    private int cooldown;
    private int surveyIn = LOOK_EVERY;
    private List<BlockPos> plants = List.of();

    public BuzzBot(EntityType<? extends BuzzBot> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4);
    }

    @Override protected void defineSynchedData(@Nonnull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DORMANT, false);
    }

    public boolean dormant() { return entityData.get(DORMANT); }

    public int power() { return power; }

    public void setPower(int ticks) { power = ticks; }

    public int cooldown() { return cooldown; }

    public void setHome(BlockPos pos) { home = pos.immutable(); }

    public void addBuds(int buds) { power = Math.min(MAX_BUDS * ProcessingConfig.botBudTicks(), power + buds * ProcessingConfig.botBudTicks()); }

    @Override public boolean isPushable() { return false; }

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (home == null) home = blockPosition();
        setDeltaMovement(Vec3.ZERO);
        boolean looking = tickCount % LOOK_EVERY == 0;
        if (looking) high = MachinePower.highNear(server, blockPosition());
        if (!high && power > 0) power--;
        if (cooldown > 0) cooldown--;
        if (charge(server, looking)) return;
        if (!high && power <= 0) {
            from = to = null;
            wander = null;
            goToSleep();
            return;
        }
        entityData.set(DORMANT, false);
        if (from == null) {
            boolean survey = --surveyIn <= 0;
            if (survey) surveyIn = SURVEY_EVERY;
            if (survey || plants.isEmpty() && looking) plants = Pollination.plants(server, blockPosition(), REACH);
            if (survey && cooldown <= 0) {
                BlockPos[] pair = Pollination.findPair(server, plants);
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
                if (Pollination.pollinate(server, from, to)) cooldown = ProcessingConfig.botCrossTicks();
                from = to = null;
            }
        }
    }

    /** Running low with a Stash in reach: goes and tops up. True while that's what it's doing. */
    private boolean charge(ServerLevel server, boolean looking) {
        if (charger == null) {
            if (!looking || power >= LOW_POWER) return false;
            charger = findStash(server);
            if (charger == null) return false;
            entityData.set(DORMANT, false);
        }
        if (!(server.getBlockEntity(charger) instanceof StashBlockEntity stash) || stash.node().stored() < ProcessingConfig.botChargeBI()) {
            charger = null;
            return false;
        }
        if (flyTo(Vec3.atCenterOf(charger).add(0, 0.8, 0))) {
            stash.node().extract(ProcessingConfig.botChargeBI());
            addBuds(1);
            charger = null;
        }
        return true;
    }

    private @Nullable BlockPos findStash(ServerLevel server) {
        BlockPos at = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-REACH, -6, -REACH), at.offset(REACH, 6, REACH))) {
            if (pos.closerThan(at, REACH) && server.getBlockEntity(pos) instanceof StashBlockEntity stash && stash.node().stored() >= ProcessingConfig.botChargeBI()) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** Back to the block it was set down in, to settle there. */
    private void goToSleep() {
        Vec3 bed = Vec3.atBottomCenterOf(home).add(0, 0.1, 0);
        if (dormant()) return;
        if (flyTo(bed)) {
            setPos(bed);
            entityData.set(DORMANT, true);
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
        BlockPos plant = plants.isEmpty() ? null : plants.get(random.nextInt(plants.size()));
        if (plant != null && !server.getBlockState(plant).is(ModBlocks.WEED_CROP.get())) plant = null;
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

    private void report(Player player) {
        player.displayClientMessage(Component.translatable("entity.fourtwenty.buzz_bot.power", power / 1200), true);
    }

    @Override protected @Nonnull InteractionResult mobInteract(@Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level().isClientSide) {
            if (held.is(ModItems.WEED_BUD.get()) && power + ProcessingConfig.botBudTicks() <= MAX_BUDS * ProcessingConfig.botBudTicks()) {
                held.consume(1, player);
                addBuds(1);
            }
            report(player);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Only a player can do anything to it, and that packs it up. */
    @Override public boolean hurt(@Nonnull DamageSource source, float amount) {
        if (level().isClientSide || !(source.getEntity() instanceof Player)) return false;
        spawnAtLocation(ModPower.BUZZ_BOT_ITEM.get());
        int buds = power / ProcessingConfig.botBudTicks();
        if (buds > 0) spawnAtLocation(new ItemStack(ModItems.WEED_BUD.get(), buds));
        discard();
        return true;
    }

    @Override public void addAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Power", power);
        tag.putInt("Cooldown", cooldown);
        tag.putBoolean("Dormant", dormant());
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        if (lastPlant != null) tag.put("LastPlant", NbtUtils.writeBlockPos(lastPlant));
    }

    @Override public void readAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        power = tag.getInt("Power");
        cooldown = tag.getInt("Cooldown");
        entityData.set(DORMANT, tag.getBoolean("Dormant"));
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        lastPlant = NbtUtils.readBlockPos(tag, "LastPlant").orElse(null);
    }
}
