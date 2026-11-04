package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJob;
import net.minecraft.resources.ResourceLocation;

/** The Bud Barn's worker. Food, sleep and sickness stay with the native citizen AI. */
public final class TrimDaddyJob extends AbstractJob<TrimDaddyAI, TrimDaddyJob> {
    public TrimDaddyJob(ICitizenData citizen) { super(citizen); }
    @Override public TrimDaddyAI generateAI() { return new TrimDaddyAI(this); }
    @Override public ResourceLocation getModel() { return ModModelTypes.FARMER_ID; }
}
