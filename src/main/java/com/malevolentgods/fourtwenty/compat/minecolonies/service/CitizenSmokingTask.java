package com.malevolentgods.fourtwenty.compat.minecolonies.service;

import com.malevolentgods.fourtwenty.compat.minecolonies.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.malevolentgods.fourtwenty.util.JointSmoke;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.other.SittingEntity;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Dining Hall-style task with independent demand, bounded visits, and no changes to nutrition. */
public final class CitizenSmokingTask {
    public enum State implements IState { VISIT }
    public enum Step { INACTIVE, TRAVEL, WAIT, SEAT, SMOKE }
    public static final int TRAVEL_TICKS = 600, WAIT_TICKS = 200, SEAT_TICKS = 100, SMOKE_TICKS = 100, RETRY_TICKS = 1200;
    private final EntityCitizen citizen;
    private Step step = Step.INACTIVE;
    private DispensaryBuilding building;
    private BlockPos seat;
    private net.minecraft.world.entity.Entity sittingVehicle;
    private long deadline, retryAt, revision, smokeStarted, lastTick;
    private boolean showingJoint;
    private int visitColony;
    private final java.util.Map<BlockPos, Long> failedBuildings = new java.util.HashMap<>();

    public CitizenSmokingTask(EntityCitizen citizen) {
        this.citizen = citizen;
        citizen.getCitizenAI().addTransition(new TickingTransition<>(State.VISIT, () -> true, this::tick, 1));
    }
    public Step step() { return step; }
    public boolean waitingAt(DispensaryBuilding candidate) { return step == Step.WAIT && building == candidate; }
    private long now() { return DispensarySavedData.get((ServerLevel) citizen.level()).time(); }

    /** Additional guard checks cover priorities that native guard WORK decisions do not expose separately. */
    public static boolean available(EntityCitizen citizen) {
        var data = citizen.getCitizenData();
        if (data == null || !citizen.isAlive() || citizen.getTarget() != null || citizen.isOnFire()
                || data.getColony().getRaiderManager().isRaided() || data.getSaturation() <= 10
                || data.getCitizenDiseaseHandler().isSick() || data.getCitizenDiseaseHandler().isHurt()
                || data.getCitizenMournHandler().isMourning() || citizen.getCitizenSleepHandler().isAsleep()
                || citizen.getCitizenSleepHandler().shouldGoSleep() || !WorldUtil.isDayTime(citizen.level())) return false;
        return data.getJob() == null || data.getJob().canAIBeInterrupted();
    }

    /** Only replace ordinary work/idle decisions; never replace eating, medical care, sleep or another mod's state. */
    public IState choose(IState nativeState) {
        if (nativeState != CitizenAIState.WORK && nativeState != CitizenAIState.IDLE) { interrupt(); return nativeState; }
        if (!available(citizen) || !JointService.due(citizen)) { interrupt(); return nativeState; }
        if (step != Step.INACTIVE) return State.VISIT;
        if (now() < retryAt || citizen.isPassenger()) return nativeState;
        visitColony = citizen.getCitizenData().getColony().getID();
        failedBuildings.values().removeIf(until -> now() >= until);
        revision = DispensarySavedData.get((ServerLevel) citizen.level()).citizen(
                citizen.getCitizenData().getColony().getID(), citizen.getCitizenData().getUUID()).revision();
        if (JointService.jointSlot(citizen.getInventoryCitizen()) >= 0) {
            if (!safeStanding(citizen.blockPosition())) { retryAt = now() + RETRY_TICKS; return nativeState; }
            beginSmoke();
        } else {
            var manager = citizen.getCitizenData().getColony().getServerBuildingManager();
            var pos = manager.getBestBuilding(citizen.blockPosition(), DispensaryBuilding.class,
                    b -> suitable(b) && b.getModule(DispensaryRegistry.WORK).hasAssignedCitizen());
            if (pos == null) pos = manager.getBestBuilding(citizen.blockPosition(), DispensaryBuilding.class,
                    this::suitable);
            if (pos == null) { unavailable(); return nativeState; }
            building = (DispensaryBuilding) manager.getBuilding(pos);
            step = Step.TRAVEL;
            deadline = now() + TRAVEL_TICKS;
        }
        lastTick = now();
        citizen.getCitizenData().setWorking(false);
        return State.VISIT;
    }

    private boolean suitable(DispensaryBuilding candidate) {
        return !failedBuildings.containsKey(candidate.getPosition()) && JointService.live(candidate) && candidate.hasServiceStock();
    }

    private IState tick() {
        if (step == Step.INACTIVE) return CitizenAIState.IDLE;
        long time = now();
        if (!available(citizen) || !JointService.due(citizen) || time - lastTick > 20
                || citizen.getCitizenData().getColony().getID() != visitColony
                || (building != null && !JointService.live(building))) return finish(true);
        lastTick = time;
        if (building != null) building.refreshCustomer(citizen, time);
        switch (step) {
            case TRAVEL -> {
                if (time >= deadline) return finish(true);
                if (JointService.atCounter(citizen, building) && EntityNavigationUtils.walkToBuilding(citizen, building)) {
                    citizen.getNavigation().stop(); step = Step.WAIT; deadline = time + WAIT_TICKS;
                } else EntityNavigationUtils.walkToBuilding(citizen, building);
            }
            case WAIT -> {
                if (!JointService.atCounter(citizen, building)) return finish(true);
                boolean self = citizen.getCitizenData().getWorkBuilding() == building
                        || !building.getModule(DispensaryRegistry.WORK).hasAssignedCitizen() || time >= deadline;
                if (JointService.jointSlot(citizen.getInventoryCitizen()) < 0 && self) {
                    JointService.collect(citizen, building, building.getItemHandlerCap());
                }
                if (JointService.jointSlot(citizen.getInventoryCitizen()) >= 0) {
                    building.releaseCustomer(citizen.getUUID());
                    seat = building.reserveSeat(citizen, time);
                    step = Step.SEAT; deadline = time + SEAT_TICKS;
                } else if (time >= deadline) return finish(true);
            }
            case SEAT -> {
                if (seat != null && building.ownsSeat(citizen.getUUID(), seat, time)
                        && !SittingEntity.isSittingPosOccupied(seat, citizen.level()) && time < deadline) {
                    if (EntityNavigationUtils.walkToPos(citizen, seat, 2, true)
                            && SittingEntity.sitDown(seat, citizen, SMOKE_TICKS + 40)) {
                        sittingVehicle = citizen.getVehicle(); beginSmoke();
                    }
                } else {
                    building.releaseSeat(citizen.getUUID()); seat = null;
                    if (!safeStanding(citizen.blockPosition())) return finish(true);
                    beginSmoke();
                }
            }
            case SMOKE -> {
                if (sittingVehicle != null ? citizen.getVehicle() != sittingVehicle : !safeStanding(citizen.blockPosition())) return finish(true);
                int slot = JointService.jointSlot(citizen.getInventoryCitizen());
                if (slot < 0) return finish(true);
                citizen.setItemInHand(InteractionHand.MAIN_HAND, citizen.getInventoryCitizen().getStackInSlot(slot).copyWithCount(1));
                showingJoint = true;
                if (time % 20 == 0) {
                    citizen.swing(InteractionHand.MAIN_HAND);
                }
                if (time - smokeStarted >= SMOKE_TICKS) {
                    if (JointService.consume(citizen, revision)) JointSmoke.puff(citizen, 5);
                    return finish(false);
                }
                JointSmoke.tick(citizen, time - smokeStarted);
            }
            default -> { return finish(true); }
        }
        return State.VISIT;
    }
    private void beginSmoke() {
        citizen.getNavigation().stop(); step = Step.SMOKE; smokeStarted = now();
        JointSmoke.puff(citizen, 2);
    }
    private boolean safeStanding(BlockPos pos) {
        var world = citizen.level();
        return world.hasChunkAt(pos) && world.getWorldBorder().isWithinBounds(pos)
                && world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP)
                && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                && world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).isEmpty()
                && world.getFluidState(pos).isEmpty() && world.getFluidState(pos.below()).isEmpty()
                && !citizen.isInWaterOrBubble() && !citizen.isInLava();
    }
    private void unavailable() {
        retryAt = now() + RETRY_TICKS;
        citizen.getCitizenData().triggerInteraction(new StandardInteraction(
                Component.translatable("fourtwenty.dispensary.unavailable"), ChatPriority.PENDING));
    }
    private IState finish(boolean failed) {
        if (failed && building != null) failedBuildings.put(building.getPosition(), now() + RETRY_TICKS * 3L);
        interrupt();
        if (failed) unavailable();
        return CitizenAIState.IDLE;
    }
    /** Only clean resources owned by this visit. An already transferred joint stays in the real inventory. */
    public void interrupt() {
        if (step == Step.INACTIVE) return;
        if (building != null) { building.releaseCustomer(citizen.getUUID()); building.releaseSeat(citizen.getUUID()); }
        citizen.getNavigation().stop();
        if (sittingVehicle != null && citizen.getVehicle() == sittingVehicle) citizen.stopRiding();
        if (showingJoint && citizen.getMainHandItem().is(com.malevolentgods.fourtwenty.registry.ModItems.WEED_JOINT.get())) {
            citizen.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        building = null; seat = null; sittingVehicle = null; showingJoint = false; step = Step.INACTIVE;
        retryAt = now() + RETRY_TICKS;
    }
}
