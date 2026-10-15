package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.minecolonies.api.client.render.modeltype.IModelType;
import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.client.render.modeltype.registry.IModelTypeRegistry;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.resources.ResourceLocation;

public final class DispensaryClient {
    public static void registerModel() {
        IModelTypeRegistry.getInstance().register(new IModelType() {
            @Override public ResourceLocation getName() { return ResourceLocation.parse("fourtwenty:budtender"); }
            @Override public ResourceLocation getTexture(AbstractEntityCitizen citizen) { return ResourceLocation.parse("fourtwenty:textures/entity/budtender.png"); }
            @Override public CitizenModel<AbstractEntityCitizen> getMaleModel() { return ModModelTypes.CITIZEN.getMaleModel(); }
            @Override public CitizenModel<AbstractEntityCitizen> getFemaleModel() { return ModModelTypes.CITIZEN.getFemaleModel(); }
        });
    }
    private DispensaryClient() {}
}
