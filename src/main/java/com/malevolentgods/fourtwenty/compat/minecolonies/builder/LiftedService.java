package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.malevolentgods.fourtwenty.compat.minecolonies.ColonyIntegration;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.Exposure;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.JointService;
import com.malevolentgods.fourtwenty.domain.LiftedRules;
import com.malevolentgods.fourtwenty.domain.SpareJoints;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingWareHouse;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.malevolentgods.fourtwenty.util.JointSmoke;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lifted Builders (specs/013-lifted-builders US3, FR-009 to FR-011, FR-017, FR-018): asking the colony for its spare
 * joints, lighting up when she starts building and when her buff runs out mid-job, counting each joint as a citizen
 * dose, and ending Lifted (which lands her) whenever she shouldn't be flying any more.
 */
public final class LiftedService {
    private LiftedService() {}

    private static final Map<UUID, IToken<?>> REQUESTS = new HashMap<>();
    /** She asks for her next joint when her high has less than this left (a minute), or she isn't high. */
    private static final int NEED_SOON_TICKS = 60 * 20;

    /** Whether this Builder may be Lifted at all right now: Lifted on in the config and on her hut. */
    public static boolean allowed(EntityCitizen citizen) {
        return BuilderConfig.liftedEnabled() && LiftedSetting.on(BuilderAccess.hut(citizen));
    }

    /** Called once a second per Builder by the watchdog's sweep. */
    public static void tick(ServerLevel level, IColony colony, EntityCitizen citizen, int elapsed) {
        var data = citizen.getCitizenData();
        if (data == null) return;
        var lifted = Lifted.get(level);
        boolean building = BuilderAccess.building(citizen);
        boolean isLifted = lifted.isLifted(colony.getID(), data.getId());

        if (isLifted && !allowed(citizen)) {
            end(level, colony, citizen, "switch");
            isLifted = false;
        }
        if (onSmokeBreak(citizen) && (!BuilderAccess.onTheJob(citizen) || !allowed(citizen))) {
            endBreak(citizen);   // off to eat or sleep, or switched off: no joint smoked
        }
        if (isLifted && building && lifted.tick(colony.getID(), data.getId(), elapsed)) {
            // Her buff ran out mid-job: come down, and if she has another joint, go and smoke it at her hut.
            LiftedPayload.send(citizen, false);
            LiftedFlight.land(citizen);
            BuilderLog.landed(colony.getID(), citizen.getName().getString(), "buff");
            if (hasJoint(citizen)) startBreak(level, citizen);
        } else if (!isLifted && building && allowed(citizen) && !onSmokeBreak(citizen) && hasJoint(citizen)) {
            startBreak(level, citizen);
        }

        // Never left floating: gravity off with nothing flying or landing her means a lost flight; bring her down.
        if (citizen.isNoGravity() && !LiftedFlight.tracked(citizen)) {
            LiftedFlight.land(citizen);
            BuilderLog.landed(colony.getID(), citizen.getName().getString(), "orphan");
        }

        // Remind everyone watching whether she shimmers every 30 s, so a missed or out-of-order message (a relog,
        // a chunk reload) can't leave the glow off for good (it went missing in game, 2026-11-14).
        if (level.getGameTime() % 600 < elapsed) LiftedPayload.send(citizen, lifted.isLifted(colony.getID(), data.getId()));
        if (level.getGameTime() % BuilderConfig.requestIntervalTicks() < elapsed) manageRequest(level, colony, citizen);
        MidnightMachineService.tick(level, colony, citizen, elapsed);
    }

    /**
     * A smoke break (the user, 2026-11-14): a Builder about to be Lifted first walks back to her hut and smokes her joint
     * there, and only then does the buff start (glow, boombox, music) and she flies back to work. She lit up wherever she
     * was before, the moment a joint reached her hut.
     */
    private static final class SmokeBreak {
        final long travelUntil;
        long smokeStarted = -1;

        SmokeBreak(long travelUntil) { this.travelUntil = travelUntil; }
    }

    private static final Map<UUID, SmokeBreak> BREAKS = new HashMap<>();
    /** How long she tries to reach her hut before smoking wherever she is. */
    private static final long BREAK_TRAVEL_TICKS = 60 * 20;

    public static boolean onSmokeBreak(EntityCitizen citizen) {
        return BREAKS.containsKey(citizen.getUUID());
    }

    private static void startBreak(ServerLevel level, EntityCitizen citizen) {
        BREAKS.put(citizen.getUUID(), new SmokeBreak(level.getGameTime() + BREAK_TRAVEL_TICKS));
        BuilderLog.diag("smokebreak citizen={} from={}", citizen.getName().getString(), citizen.blockPosition().toShortString());
    }

    private static void endBreak(EntityCitizen citizen) {
        BREAKS.remove(citizen.getUUID());
        if (citizen.getMainHandItem().is(JointService.item(MenuItem.JOINT))) citizen.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    /**
     * Her break, from the Builder's walk-to-site step (so she builds nothing meanwhile): walk her to her hut, smoke there,
     * then light up. True while the break goes on.
     */
    public static boolean smokeBreakStep(EntityCitizen citizen) {
        var brk = BREAKS.get(citizen.getUUID());
        if (brk == null || !(citizen.level() instanceof ServerLevel level)) return false;
        var data = citizen.getCitizenData();
        if (data == null) {
            endBreak(citizen);
            return false;
        }
        long now = level.getGameTime();
        if (brk.smokeStarted < 0) {
            var hut = BuilderAccess.hut(citizen);
            boolean there = hut == null || now >= brk.travelUntil || EntityNavigationUtils.walkToBuilding(citizen, hut);
            if (!there) return true;
            citizen.getNavigation().stop();
            brk.smokeStarted = now;
            JointSmoke.puff(citizen, 2);
        }
        ItemStack joint = peekJoint(citizen);
        if (joint.isEmpty()) {   // her joint went elsewhere meanwhile
            endBreak(citizen);
            return false;
        }
        long smoked = now - brk.smokeStarted;
        citizen.setItemInHand(InteractionHand.MAIN_HAND, joint.copyWithCount(1));
        if (smoked % 20 == 0) citizen.swing(InteractionHand.MAIN_HAND);
        JointSmoke.tick(citizen, smoked, ModDataComponents.strain(joint));
        if (smoked < BuilderConfig.smokeSeconds() * 20L) return true;
        endBreak(citizen);
        if (tryLightUp(level, data.getColony(), citizen)) JointSmoke.puff(citizen, 5, ModDataComponents.strain(joint));
        return false;
    }

    private static boolean hasJoint(EntityCitizen citizen) {
        return !peekJoint(citizen).isEmpty();
    }

    /** The joint she would smoke, without taking it: her own first, then one in her hut. */
    private static ItemStack peekJoint(EntityCitizen citizen) {
        ItemStack mine = peek(citizen.getInventoryCitizen());
        if (!mine.isEmpty()) return mine;
        var hut = BuilderAccess.hut(citizen);
        return hut == null ? ItemStack.EMPTY : peek(hut.getItemHandlerCap());
    }

    private static ItemStack peek(IItemHandler handler) {
        if (handler == null) return ItemStack.EMPTY;
        int slot = JointService.jointSlot(handler);
        return slot < 0 ? ItemStack.EMPTY : handler.getStackInSlot(slot);
    }

    /** Smoke one joint (hers, or one a player left in her hut) and become Lifted. False with none to hand. */
    public static boolean tryLightUp(ServerLevel level, IColony colony, EntityCitizen citizen) {
        if (!allowed(citizen)) return false;
        ItemStack joint = takeJoint(citizen);
        if (joint.isEmpty()) return false;
        Strain strain = ModDataComponents.strain(joint);
        int potency = ProcessingConfig.strainStats().potency(strain.family());
        int ticks = LiftedRules.ticks(strain, potency, BuilderConfig.liftedTuning());
        var data = citizen.getCitizenData();
        Lifted.get(level).start(colony.getID(), data.getId(), strain, ticks);
        countDose(level, citizen);
        level.playSound(null, citizen.blockPosition(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.NEUTRAL, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, citizen.getX(), citizen.getEyeY(), citizen.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        LiftedPayload.send(citizen, true);
        BuilderLog.lifted(colony.getID(), citizen.getName().getString(), strain, ticks / (double) LiftedRules.TICKS_PER_MINUTE);
        return true;
    }

    /** No longer Lifted: drop the buff and bring her down gently. */
    public static void end(ServerLevel level, IColony colony, EntityCitizen citizen, String reason) {
        var data = citizen.getCitizenData();
        if (data == null || !Lifted.get(level).isLifted(colony.getID(), data.getId())) return;
        Lifted.get(level).end(colony.getID(), data.getId());
        LiftedPayload.send(citizen, false);
        LiftedFlight.land(citizen);
        BuilderLog.landed(colony.getID(), citizen.getName().getString(), reason);
    }

    public static boolean isLifted(EntityCitizen citizen) {
        var data = citizen.getCitizenData();
        return data != null && citizen.level() instanceof ServerLevel level
                && Lifted.get(level).isLifted(data.getColony().getID(), data.getId());
    }

    /** Her own joint first, then one from her hut (a player's gift, outside the citizens-first rule). */
    private static ItemStack takeJoint(EntityCitizen citizen) {
        ItemStack taken = take(citizen.getInventoryCitizen());
        if (!taken.isEmpty()) return taken;
        var hut = BuilderAccess.hut(citizen);
        return hut == null ? ItemStack.EMPTY : take(hut.getItemHandlerCap());
    }

    private static ItemStack take(IItemHandler handler) {
        if (handler == null) return ItemStack.EMPTY;
        int slot = JointService.jointSlot(handler);
        return slot < 0 ? ItemStack.EMPTY : handler.extractItem(slot, 1, false);
    }

    /** A joint smoked for Lifted also satisfies her daily joint, if she's owed one (FR-018). */
    private static void countDose(ServerLevel level, EntityCitizen citizen) {
        if (!JointService.due(citizen)) return;
        var data = citizen.getCitizenData();
        var saved = DispensarySavedData.get(level);
        long revision = saved.citizen(data.getColony().getID(), data.getUUID()).revision();
        if (saved.recordConsumption(data.getColony().getID(), data.getUUID(), revision, ColonyIntegration.rules(data, saved))) {
            Exposure.dose(level, data);
            Exposure.secondhand(level, citizen);
            com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CitizenBenefits.refresh(data);
        }
    }

    /** Keep a joint request open for the colony's spare joints only; cancel it when there are none (citizens first). */
    private static void manageRequest(ServerLevel level, IColony colony, EntityCitizen citizen) {
        var hut = BuilderAccess.hut(citizen);
        var data = citizen.getCitizenData();
        if (hut == null || data == null) return;
        var manager = colony.getRequestManager();
        IToken<?> open = REQUESTS.get(citizen.getUUID());
        if (open != null) {
            try {
                var request = manager.getRequestForToken(open);
                if (request == null || request.getState() == RequestState.COMPLETED || request.getState() == RequestState.CANCELLED
                        || request.getState() == RequestState.RECEIVED || request.getState() == RequestState.FAILED) {
                    REQUESTS.remove(citizen.getUUID());
                    open = null;
                }
            } catch (IllegalArgumentException gone) {
                REQUESTS.remove(citizen.getUUID());
                open = null;
            }
        }
        int carrying = count(citizen.getInventoryCitizen()) + count(hut.getItemHandlerCap());
        var buff = Lifted.get(level).get(colony.getID(), data.getId());
        boolean needSoon = buff == null || buff.ticksLeft() < NEED_SOON_TICKS;
        int ask = allowed(citizen) ? SpareJoints.request(needSoon, carrying, spare(colony)) : 0;
        if (ask <= 0) {
            if (open != null) {
                manager.updateRequestState(open, RequestState.CANCELLED);
                REQUESTS.remove(citizen.getUUID());
            }
            return;
        }
        if (open == null) {
            var stack = new ItemStack(JointService.item(MenuItem.JOINT));
            REQUESTS.put(citizen.getUUID(), hut.createRequest(data, new Stack(stack, ask, 1), true));
        }
    }

    /** Joints in the colony's Dispensaries and Warehouses beyond what its citizens are still owed today. */
    private static int spare(IColony colony) {
        int stock = 0;
        for (var building : colony.getServerBuildingManager().getBuildings().values()) {
            if (building instanceof DispensaryBuilding || building instanceof BuildingWareHouse) stock += count(building.getItemHandlerCap());
        }
        int owed = 0;
        for (var data : colony.getCitizenManager().getCitizens()) {
            var entity = data.getEntity();
            if (entity.isPresent() && entity.get() instanceof EntityCitizen c && JointService.due(c)) owed++;
        }
        return SpareJoints.spare(stock, owed);
    }

    private static int count(IItemHandler handler) {
        if (handler == null) return 0;
        var joint = JointService.item(MenuItem.JOINT);
        int n = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            var s = handler.getStackInSlot(i);
            if (s.is(joint)) n += s.getCount();
        }
        return n;
    }
}
