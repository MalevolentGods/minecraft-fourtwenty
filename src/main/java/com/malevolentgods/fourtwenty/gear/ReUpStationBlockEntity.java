package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.GearRules;
import com.malevolentgods.fourtwenty.power.BIBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BINode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Charges gear from a battery of its own: the piece hung on it first, then whoever stands on the pad (what they wear and hold, and their
 * Dab Cells). The battery fills from the Wicks at one Hookah of its tier, inside the Wick's rating, and empties into gear faster than
 * that (GearRules). It moves stored BI, so it doesn't run free near a high player: that would make BI from nothing.
 */
public class ReUpStationBlockEntity extends BIBlockEntity implements Container, MenuProvider {
    public static final int DATA_STORED = 0, DATA_CAPACITY = 1, DATA_OVERLOADED = 2, DATA_TIER = 3, DATA_IN = 4, DATA_OUT = 5, DATA_STATE = 6, DATA_COUNT = 7;
    /** What the station is doing, for its screen. */
    public static final int STATE_IDLE = 0, STATE_CHARGING = 1, STATE_EMPTY = 2, STATE_FULL = 3;
    private final BIRules.Tier tier;
    private final BIBuffer battery;
    private ItemStack hung = ItemStack.EMPTY;
    private double owed;
    private int state;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_STORED -> (int) battery.stored();
                case DATA_CAPACITY -> (int) battery.capacity();
                case DATA_OVERLOADED -> level != null && battery.overloaded(level.getGameTime()) ? 1 : 0;
                case DATA_TIER -> tier.ordinal();
                case DATA_IN -> (int) Math.round(BIRules.watts(GearRules.stationInput(tier)));
                case DATA_OUT -> (int) Math.round(BIRules.watts(chargeRate()));
                case DATA_STATE -> state;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}

        @Override public int getCount() { return DATA_COUNT; }
    };

    public ReUpStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModGear.RE_UP_ENTITY.get(), pos, state);
        tier = ((ReUpStationBlock) state.getBlock()).tier();
        battery = new BIBuffer(tier, GearRules.stationCapacity(tier, GearConfig.stationStoreMinutes()), true, false,
                BIRules.watts(GearRules.stationInput(tier)), this::setChanged);
    }

    public BINode node() { return battery; }

    public BIRules.Tier tier() { return tier; }

    /** Ws per tick it puts into gear. */
    public double chargeRate() { return GearRules.stationOutput(tier, GearConfig.stationChargeHookahs()); }

    public ItemStack hung() { return hung; }

    public void hang(ItemStack stack) {
        hung = stack;
        setChanged();
    }

    public ItemStack takeDown() {
        ItemStack out = hung;
        hung = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    @Override public void serverTick() {
        if (overloadTick(battery)) {
            lit(false);
            return;
        }
        List<ItemStack> targets = targets();
        owed = Math.min(owed + chargeRate(), chargeRate() + 1);
        long budget = Math.min((long) owed, battery.stored());
        long used = 0;
        for (ItemStack target : targets) {
            if (used >= budget) break;
            used += Gear.fill(target, budget - used);
        }
        if (used > 0) {
            owed -= used;
            battery.extract(used);
        }
        state = used > 0 ? STATE_CHARGING : targets.isEmpty() ? STATE_IDLE : battery.stored() <= 0 ? STATE_EMPTY : STATE_FULL;
        lit(used > 0);
    }

    /** What this tick charges: the hung piece, then the pad's players' worn and held gear and Dab Cells. */
    private List<ItemStack> targets() {
        var out = new ArrayList<ItemStack>();
        if (!hung.isEmpty()) out.add(hung);
        if (level == null) return out;
        AABB pad = new AABB(worldPosition).move(0, 0.75, 0).inflate(0, 0.5, 0);
        for (Player player : level.getEntitiesOfClass(Player.class, pad)) {
            for (ItemStack worn : player.getArmorSlots()) if (Gear.piece(worn).isPresent()) out.add(worn);
            for (ItemStack held : player.getInventory().items) if (Gear.piece(held).isPresent()) out.add(held);
            ItemStack off = player.getOffhandItem();
            if (Gear.piece(off).isPresent()) out.add(off);
        }
        return out;
    }

    // ---- the one gear slot, for its screen and for hoppers ----

    @Override public int getContainerSize() { return 1; }

    @Override public boolean isEmpty() { return hung.isEmpty(); }

    @Override public @Nonnull ItemStack getItem(int slot) { return slot == 0 ? hung : ItemStack.EMPTY; }

    @Override public @Nonnull ItemStack removeItem(int slot, int amount) { return slot == 0 && amount > 0 ? takeDown() : ItemStack.EMPTY; }

    @Override public @Nonnull ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, 1); }

    @Override public void setItem(int slot, @Nonnull ItemStack stack) { if (slot == 0) hang(stack); }

    @Override public int getMaxStackSize() { return 1; }

    @Override public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) { return slot == 0 && Gear.piece(stack).isPresent(); }

    @Override public boolean stillValid(@Nonnull Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override public void clearContent() { hung = ItemStack.EMPTY; }

    @Override public @Nonnull Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new ReUpMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ws", battery.stored());
        if (!hung.isEmpty()) tag.put("hung", hung.save(registries));
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        battery.set(BIBuffer.loadWs(tag, "ws", "bi"));
        hung = tag.contains("hung") ? ItemStack.parseOptional(registries, tag.getCompound("hung")) : ItemStack.EMPTY;
    }
}
