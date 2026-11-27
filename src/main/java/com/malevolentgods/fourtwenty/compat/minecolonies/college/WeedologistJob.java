package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJob;
import net.minecraft.resources.ResourceLocation;

/** The Weed College's worker. Food, sleep and sickness stay with the native citizen AI. */
public final class WeedologistJob extends AbstractJob<WeedologistAI, WeedologistJob> {
    public WeedologistJob(ICitizenData citizen) { super(citizen); }
    @Override public WeedologistAI generateAI() { return new WeedologistAI(this); }
    @Override public ResourceLocation getModel() { return ModModelTypes.STUDENT_ID; }
}
