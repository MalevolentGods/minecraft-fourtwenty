package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJob;
import net.minecraft.resources.ResourceLocation;

/** The Tie Dye Shack's worker. Food, sleep and sickness stay with the native citizen AI. */
public final class TieDyerJob extends AbstractJob<TieDyerAI, TieDyerJob> {
    public TieDyerJob(ICitizenData citizen) { super(citizen); }
    @Override public TieDyerAI generateAI() { return new TieDyerAI(this); }
    @Override public ResourceLocation getModel() { return ModModelTypes.DYER_ID; }
}
