package com.malevolentgods.fourtwenty.compat.minecolonies.benefits;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryIds;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.CitizenDemand.Status;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Preference;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.happiness.IHappinessModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Server-computed snapshot, also read by native citizen views without accessing server saved data. */
public final class CannabisHappinessModifier implements IHappinessModifier {
    private Status status = Status.INELIGIBLE;
    private double weight;
    /** Shown on the citizen's happiness tab; display only. */
    private Preference preference = Preference.NONE;
    private int step;

    public CannabisHappinessModifier() {}
    public CannabisHappinessModifier(Status status, double weight) { this(status, weight, Preference.NONE, 0); }
    public CannabisHappinessModifier(Status status, double weight, Preference preference, int step) {
        this.status = status; this.weight = weight; this.preference = preference; this.step = step;
    }
    public Preference preference() { return preference; }
    public int step() { return step; }
    @Override public String getId() { return DispensaryIds.SATISFACTION; }
    public Status status() { return status; }
    @Override public double getWeight() { return weight; }
    @Override public double getFactor(ICitizenData citizen) {
        if (weight == 0) return 1;
        return switch (status) { case SATISFIED -> 1.1; case UNMET -> 0; case TOLERANCE_BREAK -> 0.5; default -> 1; };
    }
    public boolean matches(Status next, double nextWeight) { return status == next && weight == nextWeight; }
    public boolean matches(Status next, double nextWeight, Preference nextPreference, int nextStep) {
        return matches(next, nextWeight) && preference == nextPreference && step == nextStep;
    }
    @Override public void write(HolderLookup.Provider registries, CompoundTag tag, boolean persist) {
        tag.putString("id", getId());
        tag.putString("modifier", DispensaryIds.SATISFACTION);
        tag.putString("status", status.name());
        tag.putDouble("weight", weight);
        tag.putString("preference", preference.name());
        tag.putInt("step", step);
    }
    @Override public void read(HolderLookup.Provider registries, CompoundTag tag, boolean persist) {
        status = Status.valueOf(tag.getString("status"));
        weight = tag.getDouble("weight");
        // Older saves and views have no affinity fields.
        preference = tag.contains("preference") ? Preference.valueOf(tag.getString("preference")) : Preference.NONE;
        step = tag.getInt("step");
        if (!Double.isFinite(weight) || weight < 0 || weight > 1) throw new IllegalArgumentException("Invalid cannabis happiness weight");
    }
}
