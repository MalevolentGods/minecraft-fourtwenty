package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.Studies;
import com.malevolentgods.fourtwenty.power.BIBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BINode;
import com.malevolentgods.fourtwenty.power.GrowOpBlockEntity;
import com.malevolentgods.fourtwenty.power.MachinePower;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * Runs one study. Inputs are taken when it starts, so what's loaded is safe until then. Progress comes from the crops on Grow Ops in range, and
 * stops, keeping its place, whenever there's no crop or no power. A finished study waits for someone to collect it.
 */
public class ThinkTankBlockEntity extends BIBlockEntity implements Container, MenuProvider {
    public static final int SLOTS = 3, NONE = -1, RADIUS = 8, STEP = 20;
    public static final int STATUS_PICK = 0, STATUS_INPUTS = 1, STATUS_NO_PLOT = 2, STATUS_NO_POWER = 3, STATUS_RUNNING = 4, STATUS_DONE = 5;
    public static final int DATA_STUDY = 0, DATA_STATUS = 1, DATA_PROGRESS = 2, DATA_TOTAL = 3, DATA_STORED = 4, DATA_CAPACITY = 5, DATA_OVERLOADED = 6, DATA_MINUTES = 7, DATA_COUNT = 8;
    private static final int PROGRESS_SCALE = 100, BUFFER_TICKS = 400;
    private static final BIRules.Tier TIER = BIRules.Tier.PUFF;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final BIBuffer buffer = new BIBuffer(TIER, (long) Math.ceil(TIER.machine() * BUFFER_TICKS), true, false, this::setChanged);
    private final MachinePower.Meter meter = new MachinePower.Meter();
    private int study = NONE, status = STATUS_PICK;
    private boolean started;
    private double units, plotSpeed;
    private boolean overloaded;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_STUDY -> study;
                case DATA_STATUS -> status;
                case DATA_PROGRESS -> (int) (units * PROGRESS_SCALE);
                case DATA_TOTAL -> study == NONE ? 0 : Studies.ALL.get(study).units() * PROGRESS_SCALE;
                case DATA_STORED -> (int) buffer.stored();
                case DATA_CAPACITY -> (int) buffer.capacity();
                case DATA_OVERLOADED -> overloaded ? 1 : 0;
                case DATA_MINUTES -> minutesLeft();
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}

        @Override public int getCount() { return DATA_COUNT; }
    };

    public ThinkTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.THINK_TANK_ENTITY.get(), pos, state);
    }

    public BINode node() { return buffer; }

    public int status() { return status; }

    public double units() { return units; }

    private int minutesLeft() {
        if (study == NONE || plotSpeed <= 0) return 0;
        return (int) Math.ceil((Studies.ALL.get(study).units() - units) * ProcessingConfig.unitTicks() / plotSpeed / 1200);
    }

    public static ItemStack stack(Studies.Input input) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(input.item())), input.count());
    }

    /** Whether this stack belongs in this slot of the chosen study. Seeds must be Landrace, as at the Infuser. */
    private boolean fits(int slot, ItemStack stack) {
        if (study == NONE || started || slot >= Studies.ALL.get(study).inputs().size()) return false;
        ItemStack want = stack(Studies.ALL.get(study).inputs().get(slot));
        boolean strained = stack.is(com.malevolentgods.fourtwenty.registry.ModItems.WEED_SEEDS.get()) && com.malevolentgods.fourtwenty.registry.ModDataComponents.strain(stack) != com.malevolentgods.fourtwenty.domain.Strain.LANDRACE;
        return stack.is(want.getItem()) && !strained;
    }

    private boolean loaded() {
        var inputs = Studies.ALL.get(study).inputs();
        for (int i = 0; i < inputs.size(); i++) if (!fits(i, items.get(i)) || items.get(i).getCount() < inputs.get(i).count()) return false;
        return true;
    }

    /** The speed of every Grow Op in range with a crop on it, up to the configured limit. */
    private double plots() {
        double total = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-RADIUS, -RADIUS, -RADIUS), worldPosition.offset(RADIUS, RADIUS, RADIUS))) {
            if (level.getBlockEntity(p) instanceof GrowOpBlockEntity plot && plot.running()) total += plot.speed();
        }
        return Math.min(total, ProcessingConfig.maxPlotSpeed());
    }

    @Override public void serverTick() {
        overloaded = overloadTick(buffer);
        if (level.getGameTime() % STEP == 0) plotSpeed = plots();
        if (study == NONE) {
            status = STATUS_PICK;
            lit(false);
            return;
        }
        var chosen = Studies.ALL.get(study);
        if (units >= chosen.units()) {
            status = STATUS_DONE;
            lit(false);
            return;
        }
        if (!started) {
            if (!loaded()) {
                status = STATUS_INPUTS;
                lit(false);
                return;
            }
            for (int i = 0; i < chosen.inputs().size(); i++) items.get(i).shrink(chosen.inputs().get(i).count());
            started = true;
            setChanged();
        }
        if (plotSpeed <= 0) {
            status = STATUS_NO_PLOT;
            lit(false);
            return;
        }
        if (overloaded || MachinePower.steps(level, worldPosition, buffer, TIER.machine(), meter) <= 0) {
            status = STATUS_NO_POWER;
            lit(false);
            return;
        }
        status = STATUS_RUNNING;
        lit(true);
        double before = units;
        units += plotSpeed / ProcessingConfig.unitTicks();
        if ((int) units > (int) before && Studies.badBatch(level.random.nextDouble())) units = (int) before;    // that unit goes again
        setChanged();
    }

    /** A player picks a study: not while one is running, and not for a colour they already have. */
    public void pick(ServerPlayer player, int index) {
        if (started || index < 0 || index >= Studies.ALL.size()) return;
        if (com.malevolentgods.fourtwenty.infuser.Infuser.caught(player, Studies.ALL.get(index).colour())) {
            player.displayClientMessage(Component.translatable("gui.fourtwenty.think_tank.owned"), true);
            return;
        }
        study = index;
        setChanged();
    }

    /** Hands a finished study to whoever collects it. */
    public void collect(ServerPlayer player) {
        if (study == NONE || units < Studies.ALL.get(study).units()) return;
        com.malevolentgods.fourtwenty.infuser.Infuser.research(player, Studies.ALL.get(study).colour());
        player.displayClientMessage(Component.translatable("gui.fourtwenty.think_tank.collected"), true);
        study = NONE;
        started = false;
        units = 0;
        setChanged();
    }

    // Container: only the study's inputs go in, and they come out through the screen until it starts.

    @Override public int getContainerSize() { return SLOTS; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public @Nonnull ItemStack getItem(int slot) { return items.get(slot); }
    @Override public @Nonnull ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }
    @Override public @Nonnull ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, @Nonnull ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) { return fits(slot, stack); }
    @Override public boolean stillValid(@Nonnull Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); }

    @Override public @Nonnull Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new ThinkTankMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("study", study);
        tag.putBoolean("started", started);
        tag.putDouble("units", units);
        tag.putLong("bi", buffer.stored());
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        study = tag.contains("study") ? tag.getInt("study") : NONE;
        if (study >= Studies.ALL.size()) study = NONE;
        started = tag.getBoolean("started");
        units = tag.getDouble("units");
        buffer.set(tag.getLong("bi"));
    }
}
