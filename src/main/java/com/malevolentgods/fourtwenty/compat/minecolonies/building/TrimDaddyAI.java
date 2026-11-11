package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryConfig;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.BudBarnRules;
import com.malevolentgods.fourtwenty.processing.TrimTrayBlock;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.IDLE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_WORKING;

/**
 * The Trim Daddy: takes cured buds off the racks, hangs trimmed buds, snips rough buds at the trays, fetches rough buds from the hut
 * and asks the colony for more, in that order ({@link BudBarnRules#next}). Uses the trays and racks exactly as a player does.
 */
public final class TrimDaddyAI extends AbstractEntityAIInteract<TrimDaddyJob, BudBarnBuilding> {
    /** Work states; eating is fine between any two steps. */
    enum State implements IAIState {
        COLLECT, HANG, TRIM, FETCH;
        @Override public boolean isOkayToEat() { return true; }
    }

    public static final TagKey<Item> ROUGH_BUDS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("fourtwenty", "rough_buds"));
    private static final int DECIDE_DELAY = 20, WAIT_DELAY = 100, AFTER_TASK_DELAY = 5, DUMP_AFTER = 16;
    private static final double XP = 0.5;
    private BlockPos target;

    @SuppressWarnings({"rawtypes", "unchecked"})   // as MineColonies registers its own targets
    public TrimDaddyAI(TrimDaddyJob job) {
        super(job);
        super.registerTargets(
                new AITarget(IDLE, START_WORKING, 1),
                new AITarget(START_WORKING, this::decide, DECIDE_DELAY),
                new AITarget(State.COLLECT, this::collect, AFTER_TASK_DELAY),
                new AITarget(State.HANG, this::hang, AFTER_TASK_DELAY),
                new AITarget(State.TRIM, this::trim, AFTER_TASK_DELAY),
                new AITarget(State.FETCH, this::fetch, AFTER_TASK_DELAY));
        worker.setCanPickUpLoot(true);
    }

    @Override public Class<BudBarnBuilding> getExpectedBuildingClass() { return BudBarnBuilding.class; }

    @Override protected int getActionsDoneUntilDumping() { return DUMP_AFTER; }

    /** Couriers cannot see tail-end output left in the worker's pockets. */
    @Override protected boolean wantInventoryDumped() {
        return super.wantInventoryDumped() || outputCount(worker.getInventoryCitizen()) > 0;
    }

    /** Do not leave a small final dump waiting several colony days for the native pickup cooldown. */
    @Override public IAIState afterDump() {
        if (isAfterDumpPickupAllowed() && building.getPickUpPriority() > 0 && outputCount(building.getItemHandlerCap()) > 0) {
            building.pickUpDay = building.getColony().getDay();
            building.createPickupRequest(building.getPickUpPriority());
        }
        return super.afterDump();
    }

    private static int outputCount(net.neoforged.neoforge.items.IItemHandler inventory) {
        return inventory == null ? 0 : InventoryUtils.getItemCountInItemHandler(inventory,
                stack -> stack.is(ModItems.WEED_BUD.get()) || stack.is(ModItems.WEED_SEEDS.get()));
    }

    private IAIState decide() {
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        var snapshot = building.snapshot(worker.getInventoryCitizen());
        int buffer = DispensaryConfig.budBarnBuffer();
        switch (BudBarnRules.next(snapshot, buffer)) {
            case COLLECT -> {
                target = rackWhere(true);
                return target == null ? START_WORKING : State.COLLECT;
            }
            case HANG -> {
                if (snapshot.trimmedHeld() == 0) {
                    if (!walkToBuilding()) return getState();
                    take(ModItems.TRIMMED_BUD.get());
                }
                target = rackWhere(false);
                return target == null ? START_WORKING : State.HANG;
            }
            case TRIM -> {
                target = building.trays().getFirst();
                return State.TRIM;
            }
            case FETCH -> {
                return State.FETCH;
            }
            case REQUEST -> {
                checkIfRequestForTagExistOrCreateAsync(ROUGH_BUDS, Math.max(1, building.trays().size()) * DispensaryConfig.budBarnRoughPerTray());
                setDelay(WAIT_DELAY);
                return START_WORKING;
            }
            default -> {
                setDelay(WAIT_DELAY);
                return START_WORKING;
            }
        }
    }

    /** A rack with a finished bud ({@code cured}) or with a free spot. */
    private BlockPos rackWhere(boolean cured) {
        for (BlockPos pos : building.racks()) {
            var rack = building.rack(pos);
            if (rack == null) continue;
            for (ItemStack bud : rack.buds()) {
                if (cured ? bud.is(ModItems.WEED_BUD.get()) : bud.isEmpty()) return pos;
            }
        }
        return null;
    }

    private IAIState collect() {
        if (!walkToWorkPos(target)) return getState();
        var rack = building.rack(target);
        int taken = 0;
        if (rack != null) {
            for (ItemStack bud = rack.take(true); !bud.isEmpty(); bud = rack.take(true)) {
                if (!InventoryUtils.addItemStackToItemHandler(worker.getInventoryCitizen(), bud)) {
                    rack.hang(bud);            // pockets full: put it back, still cured, and empty them first
                    break;
                }
                taken++;
            }
        }
        if (taken > 0) {
            worker.swing(InteractionHand.MAIN_HAND);
            building.made(taken, 0);
            worker.getCitizenExperienceHandler().addExperience(XP * taken);
            incrementActionsDoneAndDecSaturation();
        }
        return START_WORKING;
    }

    private IAIState hang() {
        if (!walkToWorkPos(target)) return getState();
        var rack = building.rack(target);
        if (rack != null) {
            var inventory = worker.getInventoryCitizen();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                while (stack.is(ModItems.TRIMMED_BUD.get()) && !stack.isEmpty() && rack.hang(stack)) {
                    stack.shrink(1);
                }
                inventory.setStackInSlot(slot, stack);
            }
            worker.swing(InteractionHand.MAIN_HAND);
        }
        return START_WORKING;
    }

    private IAIState trim() {
        if (target == null || !(world.getBlockState(target).getBlock() instanceof TrimTrayBlock)) return START_WORKING;
        if (!walkToWorkPos(target)) return getState();
        var inventory = worker.getInventoryCitizen();
        int slot = InventoryUtils.findFirstSlotInItemHandlerWith(inventory, stack -> stack.is(ModItems.ROUGH_BUD.get()));
        if (slot < 0) return START_WORKING;
        ItemStack rough = inventory.getStackInSlot(slot);
        worker.setItemInHand(InteractionHand.MAIN_HAND, rough.copyWithCount(1));
        int seeds = 0;
        var out = TrimTrayBlock.trim(world, target, rough);
        rough.shrink(1);
        inventory.setStackInSlot(slot, rough);
        for (ItemStack made : out) {
            if (made.is(ModItems.WEED_SEEDS.get())) seeds++;
            if (!InventoryUtils.addItemStackToItemHandler(inventory, made)) worker.spawnAtLocation(made);
        }
        worker.swing(InteractionHand.MAIN_HAND);
        worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (seeds > 0) building.made(0, seeds);
        worker.getCitizenExperienceHandler().addExperience(XP);
        incrementActionsDoneAndDecSaturation();
        setDelay(BudBarnRules.trimTicks(building.getBuildingLevel(), levels()));
        return START_WORKING;
    }

    private static int[] levels() {
        int[] out = new int[5];
        for (int level = 1; level <= 5; level++) out[level - 1] = DispensaryConfig.budBarnTrimTicks(level);
        return out;
    }

    private IAIState fetch() {
        if (!walkToBuilding()) return getState();
        take(ModItems.ROUGH_BUD.get());
        return START_WORKING;
    }

    /** Moves a stack of {@code item} from the hut's storage into the worker's pockets. */
    private void take(Item item) {
        int slot = InventoryUtils.findFirstSlotInProviderNotEmptyWith(building, stack -> stack.is(item));
        if (slot >= 0) InventoryUtils.transferItemStackIntoNextFreeSlotFromProvider(building, slot, worker.getInventoryCitizen());
    }

}
