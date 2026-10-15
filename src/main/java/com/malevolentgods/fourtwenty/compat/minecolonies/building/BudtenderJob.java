package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import net.minecraft.resources.ResourceLocation;

/** Native crafter needs (food, sleep, illness) remain under CitizenAI control. */
public final class BudtenderJob extends AbstractJobCrafter<BudtenderAI, BudtenderJob> {
    public BudtenderJob(ICitizenData citizen) { super(citizen); }
    @Override public BudtenderAI generateAI() { return new BudtenderAI(this); }
    @Override public ResourceLocation getModel() { return ResourceLocation.parse("fourtwenty:budtender"); }
}
