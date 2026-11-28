package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.views.BOWindow;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBoardModule;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The Weed College's Board tab, client side: the last board the server sent, and a counter that open windows watch to redraw. */
public final class CollegeBoardView extends AbstractBuildingModuleView {
    CollegeBoardModule.Board board = CollegeBoardModule.Board.EMPTY;
    int version;

    @Override public void deserialize(RegistryFriendlyByteBuf buffer) {
        board = CollegeBoardModule.Board.read(buffer);
        version++;
    }

    @Override public BOWindow getWindow() { return new CollegeBoardWindow(this); }
    @Override public Component getDesc() { return Component.translatable("college.fourtwenty.board.title"); }
    @Override public ResourceLocation getIconResourceLocation() { return ResourceLocation.parse("fourtwenty:textures/gui/college_board_tab.png"); }

    int colonyId() { return buildingView.getColony().getID(); }
    net.minecraft.core.BlockPos position() { return buildingView.getPosition(); }
}
