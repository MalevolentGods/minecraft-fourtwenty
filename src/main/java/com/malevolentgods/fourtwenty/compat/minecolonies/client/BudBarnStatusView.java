package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.views.BOWindow;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.BudBarnStatusModule;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class BudBarnStatusView extends AbstractBuildingModuleView {
    BudBarnStatusModule.Status status = BudBarnStatusModule.Status.EMPTY;
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) { status = BudBarnStatusModule.Status.read(buffer); }
    @Override public BOWindow getWindow() { return new BudBarnStatusWindow(this); }
    @Override public Component getDesc() { return Component.translatable("fourtwenty.budbarn.status.title"); }
    @Override public ResourceLocation getIconResourceLocation() { return ResourceLocation.parse("fourtwenty:textures/gui/dispensary_tab.png"); }
}
