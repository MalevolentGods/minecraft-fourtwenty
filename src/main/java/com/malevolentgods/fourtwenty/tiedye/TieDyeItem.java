package com.malevolentgods.fourtwenty.tiedye;

import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A tie-dyed block's item: its tooltip names the pattern and the colourway. Neither names a colour, so nothing here teaches one
 * (specs/018-tie-dye-shack FR-007).
 */
public class TieDyeItem extends BlockItem {
    public TieDyeItem(Block block, Properties properties) { super(block, properties); }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        TieDye look = stack.get(ModDataComponents.TIE_DYE.get());
        if (look == null) return;
        tooltip.add(Component.translatable("tiedye.fourtwenty.pattern." + look.pattern()).withStyle(ChatFormatting.GRAY));
        tooltip.add(colourwayName(look.colourway()).copy().withStyle(ChatFormatting.GRAY));
    }

    /** A colourway's name: it names a palette, never a colour, so everyone sees it (research R6, revised 2026-11-28). */
    public static Component colourwayName(String id) { return Component.translatable("tiedye.fourtwenty.colourway." + id); }
}
