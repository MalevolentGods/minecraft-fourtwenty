package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.domain.college.ColonyResearch;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.malevolentgods.fourtwenty.domain.college.StudyBoard;
import com.malevolentgods.fourtwenty.domain.college.StudyRules;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.UUID;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.IDLE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_WORKING;

/**
 * The Weedologist (specs/017-weed-college FR-012, FR-013; research R2). Works the colony's study in progress: asks the warehouse for its
 * supplies and carries them in, then works in cycles: a plot (harvests it if ripe, plants it if bare), the Think Tank, the desk. Each cycle
 * written up at the desk is one unit of the study, unless it was a bad batch. Plants and harvests through the same use action a player's
 * click goes through, so the College's crops grow, drop and replant like anyone's.
 */
public final class WeedologistAI extends AbstractEntityAIInteract<WeedologistJob, CollegeBuilding> {
    enum State implements IAIState {
        FETCH, PLOT, TANK, DESK;
        @Override public boolean isOkayToEat() { return true; }
    }

    private static final int DECIDE_DELAY = 20, WAIT_DELAY = 100, AFTER_TASK_DELAY = 5, DUMP_AFTER = 32;
    private static final double XP = 0.5;
    private static final GameProfile HANDS = new GameProfile(UUID.fromString("4f2d5c9e-1d20-4c47-9b1e-0420c011e9e0"), "[Weedologist]");
    private BlockPos target;
    private int nextPlot;

    @SuppressWarnings({"rawtypes", "unchecked"})   // as MineColonies registers its own targets
    public WeedologistAI(WeedologistJob job) {
        super(job);
        super.registerTargets(
                new AITarget(IDLE, START_WORKING, 1),
                new AITarget(START_WORKING, this::decide, DECIDE_DELAY),
                new AITarget(State.FETCH, this::fetch, AFTER_TASK_DELAY),
                new AITarget(State.PLOT, this::plot, AFTER_TASK_DELAY),
                new AITarget(State.TANK, this::tank, AFTER_TASK_DELAY),
                new AITarget(State.DESK, this::desk, AFTER_TASK_DELAY));
        worker.setCanPickUpLoot(true);
    }

    @Override public Class<CollegeBuilding> getExpectedBuildingClass() { return CollegeBuilding.class; }

    @Override protected int getActionsDoneUntilDumping() { return DUMP_AFTER; }

    private int phaseTicks() { return Math.max(1, CollegeConfig.unitTicks() / 3); }

    private IAIState decide() {
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        var colony = building.getColony();
        ColonyResearch r = CollegeResearch.record(colony);
        if (r.head() == null) {
            building.report(CollegeBuilding.Activity.IDLE, null, null);
            setDelay(WAIT_DELAY);
            return START_WORKING;
        }
        StudyBoard.Standing standing = CollegeStudies.board().headStanding(r, CollegeResearch.collegeLevel(colony), CollegeFeatures::present);
        if (standing.state() != StudyBoard.State.IN_PROGRESS) {
            building.report(CollegeBuilding.Activity.PAUSED, "college.fourtwenty.lock." + standing.lock().name().toLowerCase(java.util.Locale.ROOT), standing.arg());
            setDelay(WAIT_DELAY);
            return START_WORKING;
        }
        CollegeResearch.startHead(colony);
        List<Study.Supply> missing = r.missing();
        if (!missing.isEmpty()) {
            if (hutHolds(missing)) return State.FETCH;
            for (Study.Supply s : missing) request(s);
            building.report(CollegeBuilding.Activity.WAITING, "college.fourtwenty.status.waiting", null);
            setDelay(WAIT_DELAY);
            return START_WORKING;
        }
        if (building.plots().isEmpty() || building.tank() == null || building.desk() == null) {
            String needs = building.plots().isEmpty() ? "plot" : building.tank() == null ? "tank" : "desk";
            building.report(CollegeBuilding.Activity.PAUSED, "college.fourtwenty.status.needs_" + needs, null);
            setDelay(WAIT_DELAY);
            return START_WORKING;
        }
        building.report(CollegeBuilding.Activity.WORKING, null, null);
        List<BlockPos> plots = building.plots();
        target = plots.get(nextPlot++ % plots.size());
        return State.PLOT;
    }

    private boolean hutHolds(List<Study.Supply> missing) {
        IItemHandler hut = building.getItemHandlerCap();
        if (hut == null) return false;
        for (Study.Supply s : missing) {
            if (InventoryUtils.getItemCountInItemHandler(hut, stack -> CollegeBuilding.matches(stack, s)) > 0) return true;
        }
        return false;
    }

    /** Asks the colony for one missing supply line: any strain unless the line names one. */
    private void request(Study.Supply s) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(s.item()));
        ItemStack want = new ItemStack(item, Math.min(s.count(), item.getDefaultMaxStackSize()));
        boolean exact = s.strain() != null;
        if (exact) {
            var strain = com.malevolentgods.fourtwenty.domain.Strain.byId(s.strain());
            if (strain != null) want = com.malevolentgods.fourtwenty.registry.ModDataComponents.withStrain(want, strain);
        }
        checkIfRequestForItemExistOrCreateAsync(want, s.count(), 1, exact);
    }

    /** Carries the study's supplies from the hut into the study: they're used up, and counted as delivered. */
    private IAIState fetch() {
        if (!walkToBuilding()) return getState();
        var colony = building.getColony();
        ColonyResearch r = CollegeResearch.record(colony);
        List<Study.Supply> cost = r.cost();
        IItemHandler hut = building.getItemHandlerCap();
        if (cost == null || hut == null) return START_WORKING;
        List<Integer> delivered = r.delivered();
        for (int line = 0; line < cost.size(); line++) {
            Study.Supply s = cost.get(line);
            int left = s.count() - delivered.get(line);
            for (int slot = 0; slot < hut.getSlots() && left > 0; slot++) {
                ItemStack stack = hut.getStackInSlot(slot);
                if (!CollegeBuilding.matches(stack, s)) continue;
                ItemStack taken = hut.extractItem(slot, Math.min(left, stack.getCount()), false);
                if (taken.isEmpty()) continue;
                worker.setItemInHand(InteractionHand.MAIN_HAND, taken.copyWithCount(1));
                r.deliver(line, taken.getCount());
                left -= taken.getCount();
            }
        }
        worker.swing(InteractionHand.MAIN_HAND);
        worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        CollegeResearch.changed(colony);
        return START_WORKING;
    }

    /** A plot: harvest the crop on it if it's ripe, plant it if it's bare, otherwise just tend it. */
    private IAIState plot() {
        if (target == null) return START_WORKING;
        if (!walkToWorkPos(target)) return getState();
        if (world instanceof ServerLevel server) {
            BlockPos crop = target.above();
            BlockState state = server.getBlockState(crop);
            var hands = FakePlayerFactory.get(server, HANDS);
            hands.setPos(worker.getX(), worker.getY(), worker.getZ());
            if (state.getBlock() instanceof WeedCropBlock weed && weed.isMaxAge(state)) {
                state.useWithoutItem(server, hands, new BlockHitResult(Vec3.atCenterOf(crop), Direction.UP, crop, false));
            } else if (state.isAir()) {
                plant(server, hands, target);
            }
        }
        worker.swing(InteractionHand.MAIN_HAND);
        setDelay(phaseTicks());
        target = building.tank();
        return target == null ? START_WORKING : State.TANK;
    }

    private void plant(ServerLevel server, net.neoforged.neoforge.common.util.FakePlayer hands, BlockPos plot) {
        var pockets = worker.getInventoryCitizen();
        int slot = InventoryUtils.findFirstSlotInItemHandlerWith(pockets, stack -> stack.is(ModItems.WEED_SEEDS.get()));
        if (slot < 0) {
            IItemHandler hut = building.getItemHandlerCap();
            int hutSlot = hut == null ? -1 : InventoryUtils.findFirstSlotInItemHandlerWith(hut, stack -> stack.is(ModItems.WEED_SEEDS.get()));
            if (hutSlot < 0) return;
            ItemStack one = hut.extractItem(hutSlot, 1, false);
            if (one.isEmpty() || !InventoryUtils.addItemStackToItemHandler(pockets, one)) return;
            slot = InventoryUtils.findFirstSlotInItemHandlerWith(pockets, stack -> stack.is(ModItems.WEED_SEEDS.get()));
            if (slot < 0) return;
        }
        ItemStack seeds = pockets.getStackInSlot(slot);
        hands.setItemInHand(InteractionHand.MAIN_HAND, seeds.copyWithCount(1));
        var result = hands.getMainHandItem().useOn(new UseOnContext(hands, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(plot).add(0, 0.5, 0), Direction.UP, plot, false)));
        if (result.consumesAction()) pockets.extractItem(slot, 1, false);
        hands.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private IAIState tank() {
        if (target == null) return START_WORKING;
        if (!walkToWorkPos(target)) return getState();
        worker.swing(InteractionHand.MAIN_HAND);
        setDelay(phaseTicks());
        target = building.desk();
        return target == null ? START_WORKING : State.DESK;
    }

    /** The desk: the cycle is written up as one unit of the study, unless it was a bad batch; the last unit finishes it. */
    private IAIState desk() {
        if (target == null) return START_WORKING;
        if (!walkToWorkPos(target)) return getState();
        worker.swing(InteractionHand.MAIN_HAND);
        var colony = building.getColony();
        ColonyResearch r = CollegeResearch.record(colony);
        Study study = r.head() == null ? null : CollegeStudies.board().get(r.head());
        if (study != null && r.missing().isEmpty() && r.started()) {
            int intelligence = worker.getCitizenData().getCitizenSkillHandler().getLevel(Skill.Intelligence);
            boolean bad = StudyRules.badBatch(worker.getRandom().nextDouble(), intelligence, CollegeConfig.badBase(), CollegeConfig.badFloor(),
                    CollegeConfig.badStep());
            int units = r.work(bad);
            CollegeResearch.changed(colony);
            if (units >= study.units()) {
                CollegeResearch.finishHead(colony);
                building.finishedOne();
            }
            worker.getCitizenExperienceHandler().addExperience(XP);
            incrementActionsDoneAndDecSaturation();
        }
        setDelay(phaseTicks());
        return START_WORKING;
    }
}
