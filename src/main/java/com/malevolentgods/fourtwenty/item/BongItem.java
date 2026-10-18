package com.malevolentgods.fourtwenty.item;

import com.malevolentgods.fourtwenty.domain.BongVariety;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.List;

/** A placeable bong; specialty bongs list their one-day buff and the colonist skills they boost. */
public class BongItem extends BlockItem {
    private final BongVariety variety;

    public BongItem(BongVariety variety, Block block, Properties properties) {
        super(block, properties);
        this.variety = variety;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        lines.add(Component.translatableWithFallback("item.fourtwenty.bong.use", "Right-click with a Weed Bud to take a hit")
                .withStyle(ChatFormatting.GRAY));
        if (!variety.specialty()) return;
        lines.add(Component.translatable("item.fourtwenty." + variety.path + ".buff").withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatableWithFallback("item.fourtwenty.bong.colonists", "Level 5 colonists: +%s %s and %s for a day",
                BongVariety.SKILL_BONUS, variety.skills.get(0), variety.skills.get(1)).withStyle(ChatFormatting.DARK_AQUA));
    }
}
