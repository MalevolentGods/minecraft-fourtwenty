package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBoardModule;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeNetwork;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What the Weed College's windows share on the client: a study's name and icon as this player may see them, and sending a board action.
 * Colour privacy (research R10): an accord study is named, and shows its terpene, only to a player who has discovered that accord.
 */
final class CollegeClient {
    private CollegeClient() {}

    /** The accord an accord study teaches ("myr_lim"), or null for any other study. */
    static String accord(CollegeBoardModule.Entry e) { return Study.accordOf(e.kind(), e.icon()); }

    static Component name(CollegeBoardModule.Entry e) {
        String accord = accord(e);
        if (accord == null) return Component.translatable(nameKey(e.id()));
        if (!TerpeneKnowledge.knows(accord)) return Component.translatable("college.fourtwenty.board.accord_unknown");
        return Component.translatable("college.fourtwenty.board.accord", icon(e).getHoverName());
    }

    static Component name(CollegeBoardModule.Board board, String id) {
        CollegeBoardModule.Entry e = board.entry(id);
        return e == null ? Component.literal("?") : name(e);
    }

    static ItemStack icon(CollegeBoardModule.Entry e) {
        String accord = accord(e);
        if (accord != null && !TerpeneKnowledge.knows(accord)) return new ItemStack(Items.GLASS_BOTTLE);
        var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(e.icon()));
        return item.map(ItemStack::new).orElseGet(() -> new ItemStack(Items.BOOK));
    }

    static String nameKey(String id) { return "college.fourtwenty.study." + id.substring(id.indexOf(':') + 1); }

    static Component branch(Study.Branch branch) { return Component.translatable("college.fourtwenty.branch." + branch.key()); }

    /** Sends a board action if the server has the channel; the server checks everything and refreshes the board. */
    static void send(CollegeBoardView view, CollegeNetwork.Action action, String study) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(CollegeNetwork.BoardAction.TYPE)) return;
        PacketDistributor.sendToServer(new CollegeNetwork.BoardAction(view.colonyId(), view.position(), action, study));
    }
}
