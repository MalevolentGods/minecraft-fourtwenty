package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.views.BOWindow;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryStatus;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class DispensaryStatusView extends AbstractBuildingModuleView {
    DispensaryStatus status = DispensaryStatus.EMPTY;
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) { status = DispensaryStatus.read(buffer); }
    @Override public BOWindow getWindow() { return new DispensaryStatusWindow(this); }
    @Override public Component getDesc() { return Component.translatable("fourtwenty.dispensary.status.title"); }
    @Override public ResourceLocation getIconResourceLocation() { return ResourceLocation.parse("fourtwenty:textures/gui/dispensary_tab.png"); }
}
