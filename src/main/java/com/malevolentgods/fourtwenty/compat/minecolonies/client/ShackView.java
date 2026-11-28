package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.views.BOWindow;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.ShackModule;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The Tie Dye Shack's Orders tab, client side: the last view the server sent, and a counter open windows watch to redraw. */
public final class ShackView extends AbstractBuildingModuleView {
    ShackModule.View view = ShackModule.View.EMPTY;
    int version;

    @Override public void deserialize(RegistryFriendlyByteBuf buffer) {
        view = ShackModule.View.read(buffer);
        version++;
    }

    @Override public BOWindow getWindow() { return new ShackWindow(this); }
    @Override public Component getDesc() { return Component.translatable("tiedye.fourtwenty.orders.title"); }
    @Override public ResourceLocation getIconResourceLocation() { return ResourceLocation.parse("fourtwenty:textures/gui/tiedye_tab.png"); }

    int colonyId() { return buildingView.getColony().getID(); }
    net.minecraft.core.BlockPos position() { return buildingView.getPosition(); }
}
