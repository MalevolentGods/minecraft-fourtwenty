package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import com.malevolentgods.fourtwenty.domain.tiedye.OrderQueue;
import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.IDLE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_WORKING;

/**
 * The Tie Dyer (specs/018-tie-dye-shack FR-011, research R5). Works the colony's order at the head of the queue a batch at a time: asks
 * the warehouse for the batch's plain items and dye supplies and carries them in, dips the batch at a vat, hangs it at a drying rack, and
 * the finished batch goes to the warehouse.
 */
public final class TieDyerAI extends AbstractEntityAIInteract<TieDyerJob, ShackBuilding> {
    enum State implements IAIState {
        FETCH, VAT, RACK;
        @Override public boolean isOkayToEat() { return true; }
    }

    private static final int DECIDE_DELAY = 20, WAIT_DELAY = 100, AFTER_TASK_DELAY = 5, DUMP_AFTER = 32;
    private static final double XP = 0.5;
    private BlockPos target;
    private int nextVat, nextRack;

    @SuppressWarnings({"rawtypes", "unchecked"})   // as MineColonies registers its own targets
    public TieDyerAI(TieDyerJob job) {
        super(job);
        super.registerTargets(
                new AITarget(IDLE, START_WORKING, 1),
                new AITarget(START_WORKING, this::decide, DECIDE_DELAY),
                new AITarget(State.FETCH, this::fetch, AFTER_TASK_DELAY),
                new AITarget(State.VAT, this::vat, AFTER_TASK_DELAY),
                new AITarget(State.RACK, this::rack, AFTER_TASK_DELAY));
        worker.setCanPickUpLoot(true);
    }

    @Override public Class<ShackBuilding> getExpectedBuildingClass() { return ShackBuilding.class; }

    @Override protected int getActionsDoneUntilDumping() { return DUMP_AFTER; }

    private IAIState decide() {
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        var colony = building.getColony();
        OrderQueue q = TieDyeOrders.record(colony);
        OrderQueue.Order head = q.head();
        if (head == null) {
            building.report(ShackBuilding.Activity.IDLE, null);
            setDelay(WAIT_DELAY);
            return START_WORKING;
        }
        // a data-pack change can take a pattern or colourway away; the order waits rather than dyeing what the colony doesn't know
        if (!TieDyeOrders.patternUnlocked(colony, head.pattern()) || !TieDyeOrders.colourwayOrderable(colony, head.colourway())) {
            building.report(ShackBuilding.Activity.PAUSED, "tiedye.fourtwenty.status.lost");
            setDelay(WAIT_DELAY);
            return START_WORKING;
        }
        if (building.vats().isEmpty() || building.racks().isEmpty()) {
            building.report(ShackBuilding.Activity.PAUSED, building.vats().isEmpty() ? "tiedye.fourtwenty.status.needs_vat" : "tiedye.fourtwenty.status.needs_rack");
            setDelay(WAIT_DELAY);
            return START_WORKING;
        }
        TieDyeOrders.startBatch(colony, building.getBuildingLevel());
        List<TieDyeRules.Supply> missing = q.missing();
        if (!missing.isEmpty()) {
            if (hutHolds(missing)) return State.FETCH;
            for (TieDyeRules.Supply s : missing) request(s);
            building.report(ShackBuilding.Activity.WAITING, "tiedye.fourtwenty.status.waiting");
            setDelay(WAIT_DELAY);
            return START_WORKING;
        }
        building.report(ShackBuilding.Activity.WORKING, null);
        List<BlockPos> vats = building.vats();
        target = vats.get(nextVat++ % vats.size());
        return State.VAT;
    }

    private boolean hutHolds(List<TieDyeRules.Supply> missing) {
        IItemHandler hut = building.getItemHandlerCap();
        if (hut == null) return false;
        for (TieDyeRules.Supply s : missing) {
            if (InventoryUtils.getItemCountInItemHandler(hut, stack -> ShackBuilding.matches(stack, s)) > 0) return true;
        }
        return false;
    }

    private void request(TieDyeRules.Supply s) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(s.item()));
        ItemStack want = new ItemStack(item, Math.min(s.count(), item.getDefaultMaxStackSize()));
        checkIfRequestForItemExistOrCreateAsync(want, s.count(), 1, false);
    }

    /** Carries the batch's supplies from the hut into the batch: they're used up, and counted as delivered. */
    private IAIState fetch() {
        if (!walkToBuilding()) return getState();
        var colony = building.getColony();
        OrderQueue q = TieDyeOrders.record(colony);
        List<TieDyeRules.Supply> batch = q.batch();
        IItemHandler hut = building.getItemHandlerCap();
        if (batch == null || hut == null) return START_WORKING;
        List<Integer> delivered = q.delivered();
        for (int line = 0; line < batch.size(); line++) {
            TieDyeRules.Supply s = batch.get(line);
            int left = s.count() - delivered.get(line);
            for (int slot = 0; slot < hut.getSlots() && left > 0; slot++) {
                ItemStack stack = hut.getStackInSlot(slot);
                if (!ShackBuilding.matches(stack, s)) continue;
                ItemStack taken = hut.extractItem(slot, Math.min(left, stack.getCount()), false);
                if (taken.isEmpty()) continue;
                worker.setItemInHand(InteractionHand.MAIN_HAND, taken.copyWithCount(1));
                q.deliver(line, taken.getCount());
                left -= taken.getCount();
            }
        }
        worker.swing(InteractionHand.MAIN_HAND);
        worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        TieDyeOrders.changed(colony);
        return START_WORKING;
    }

    /** The vat: the batch is dipped. */
    private IAIState vat() {
        if (target == null) return START_WORKING;
        if (!walkToWorkPos(target)) return getState();
        worker.swing(InteractionHand.MAIN_HAND);
        world.playSound(null, target, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.2f);
        setDelay(TieDyeConfig.vatTicks());
        List<BlockPos> racks = building.racks();
        if (racks.isEmpty()) return START_WORKING;
        target = racks.get(nextRack++ % racks.size());
        return State.RACK;
    }

    /** The rack: the batch dries, and is done. */
    private IAIState rack() {
        if (target == null) return START_WORKING;
        if (!walkToWorkPos(target)) return getState();
        worker.swing(InteractionHand.MAIN_HAND);
        var colony = building.getColony();
        OrderQueue q = TieDyeOrders.record(colony);
        if (q.batchStarted() && q.missing().isEmpty()) {
            setDelay(TieDyeConfig.rackTicks());
            int n = TieDyeOrders.finishBatch(colony, building);
            building.madeSome(n);
            worker.getCitizenExperienceHandler().addExperience(XP);
            incrementActionsDoneAndDecSaturation();
        }
        return START_WORKING;
    }
}
