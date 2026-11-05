package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.GearRules;
import com.malevolentgods.fourtwenty.power.BIBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BINode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Charges gear from the Wicks at its tier's wire rate: the piece hung on it first, then whoever stands on the pad (what they wear and
 * hold, and their Dab Cells). It moves stored BI, so it doesn't run free near a high player: that would make BI from nothing.
 */
public class ReUpStationBlockEntity extends BIBlockEntity {
    private static final int INTAKE_TICKS = 40;
    private final BIRules.Tier tier;
    private final BIBuffer intake;
    private ItemStack hung = ItemStack.EMPTY;
    private double owed;

    public ReUpStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModGear.RE_UP_ENTITY.get(), pos, state);
        tier = ((ReUpStationBlock) state.getBlock()).tier();
        intake = new BIBuffer(tier, (long) Math.ceil(GearRules.stationRate(tier) * INTAKE_TICKS), true, false,
                com.malevolentgods.fourtwenty.domain.BIRules.watts(GearRules.stationRate(tier)), this::setChanged);
    }

    public BINode node() { return intake; }

    public BIRules.Tier tier() { return tier; }

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
        boolean overloaded = overloadTick(intake);
        if (overloaded) {
            lit(false);
            return;
        }
        owed += GearRules.stationRate(tier);
        long budget = Math.min((long) owed, intake.stored());
        owed -= (long) owed;
        long used = 0;
        for (ItemStack target : targets()) {
            if (used >= budget) break;
            used += Gear.fill(target, budget - used);
        }
        if (used > 0) intake.extract(used);
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

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ws", intake.stored());
        if (!hung.isEmpty()) tag.put("hung", hung.save(registries));
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        intake.set(com.malevolentgods.fourtwenty.power.BIBuffer.loadWs(tag, "ws", "bi"));
        hung = tag.contains("hung") ? ItemStack.parseOptional(registries, tag.getCompound("hung")) : ItemStack.EMPTY;
    }
}
