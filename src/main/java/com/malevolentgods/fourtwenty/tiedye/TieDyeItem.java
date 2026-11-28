package com.malevolentgods.fourtwenty.tiedye;

import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A tie-dyed block's item: its tooltip names the pattern, and the colourway only to a holder who has caught every colour in it
 * (specs/018-tie-dye-shack FR-007): nothing here teaches a colour.
 */
public class TieDyeItem extends BlockItem {
    public TieDyeItem(Block block, Properties properties) { super(block, properties); }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        TieDye look = stack.get(ModDataComponents.TIE_DYE.get());
        if (look == null) return;
        tooltip.add(Component.translatable("tiedye.fourtwenty.pattern." + look.pattern()).withStyle(ChatFormatting.GRAY));
        tooltip.add(colourwayName(look.colourway()).copy().withStyle(ChatFormatting.GRAY));
    }

    /** "Terrapin", or "An Undiscovered Colourway" until every colour in it has been caught. On a server everything is known. */
    public static Component colourwayName(String id) {
        for (String code : Colourways.get(id).colours()) {
            if (!TerpeneKnowledge.knows(code)) return Component.translatable("tiedye.fourtwenty.colourway_unknown");
        }
        return Component.translatable("tiedye.fourtwenty.colourway." + id);
    }
}
