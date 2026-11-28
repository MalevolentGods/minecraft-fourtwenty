package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.views.BOWindow;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeStatusModule;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class CollegeStatusView extends AbstractBuildingModuleView {
    CollegeStatusModule.Status status = CollegeStatusModule.Status.EMPTY;
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) { status = CollegeStatusModule.Status.read(buffer); }
    @Override public BOWindow getWindow() { return new CollegeStatusWindow(this); }
    @Override public Component getDesc() { return Component.translatable("college.fourtwenty.status.title"); }
    @Override public ResourceLocation getIconResourceLocation() { return ResourceLocation.parse("fourtwenty:textures/gui/dispensary_tab.png"); }
}
