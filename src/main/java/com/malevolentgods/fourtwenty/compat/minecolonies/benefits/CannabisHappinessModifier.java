package com.malevolentgods.fourtwenty.compat.minecolonies.benefits;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryIds;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.CitizenDemand.Status;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.happiness.IHappinessModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Server-computed snapshot, also read by native citizen views without accessing server saved data. */
public final class CannabisHappinessModifier implements IHappinessModifier {
    private Status status = Status.INELIGIBLE;
    private double weight;

    public CannabisHappinessModifier() {}
    public CannabisHappinessModifier(Status status, double weight) { this.status = status; this.weight = weight; }
    @Override public String getId() { return DispensaryIds.SATISFACTION; }
    public Status status() { return status; }
    @Override public double getWeight() { return weight; }
    @Override public double getFactor(ICitizenData citizen) {
        if (weight == 0) return 1;
        return switch (status) { case SATISFIED -> 1.1; case UNMET -> 0; default -> 1; };
    }
    public boolean matches(Status next, double nextWeight) { return status == next && weight == nextWeight; }
    @Override public void write(HolderLookup.Provider registries, CompoundTag tag, boolean persist) {
        tag.putString("id", getId());
        tag.putString("modifier", DispensaryIds.SATISFACTION);
        tag.putString("status", status.name());
        tag.putDouble("weight", weight);
    }
    @Override public void read(HolderLookup.Provider registries, CompoundTag tag, boolean persist) {
        status = Status.valueOf(tag.getString("status"));
        weight = tag.getDouble("weight");
        if (!Double.isFinite(weight) || weight < 0 || weight > 1) throw new IllegalArgumentException("Invalid cannabis happiness weight");
    }
}
