package com.malevolentgods.fourtwenty.compat.minecolonies.benefits;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryIds;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.happiness.IHappinessModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * "Bong session": a temporary bonus for colonists who were near a burning bong. Like the cannabis
 * satisfaction modifier, it is a server-computed snapshot ({@code active}) so client views never
 * need server time; {@link BongSessionBenefits} flips it when the deadline passes.
 */
public final class BongSessionHappinessModifier implements IHappinessModifier {
    private boolean active;
    private long expires;
    private double weight;

    public BongSessionHappinessModifier() {}
    public BongSessionHappinessModifier(boolean active, long expires, double weight) {
        this.active = active; this.expires = expires; this.weight = weight;
    }

    @Override public String getId() { return DispensaryIds.BONG_SESSION; }
    public boolean active() { return active; }
    public long expires() { return expires; }
    @Override public double getWeight() { return weight; }
    @Override public double getFactor(ICitizenData citizen) { return active && weight > 0 ? 1.1 : 1; }

    @Override public void write(HolderLookup.Provider registries, CompoundTag tag, boolean persist) {
        tag.putString("id", getId());
        tag.putString("modifier", DispensaryIds.BONG_SESSION);
        tag.putBoolean("active", active);
        tag.putLong("expires", expires);
        tag.putDouble("weight", weight);
    }
    @Override public void read(HolderLookup.Provider registries, CompoundTag tag, boolean persist) {
        active = tag.getBoolean("active");
        expires = tag.getLong("expires");
        weight = tag.getDouble("weight");
        if (!Double.isFinite(weight) || weight < 0 || weight > 1) throw new IllegalArgumentException("Invalid bong session happiness weight");
    }
}
