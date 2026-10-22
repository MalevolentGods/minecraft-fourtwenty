package com.malevolentgods.fourtwenty.item;

import com.malevolentgods.fourtwenty.block.FancyBedBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A FourTwenty bed item. Its blanket colour rides in the vanilla {@code minecraft:block_state}
 * component ({@code blanket=red}), which placement applies to both halves and breaking copies back
 * (the loot table's copy_state); no component means the lime a bed is crafted with.
 */
public class FancyBedItem extends BedItem {
    public FancyBedItem(Block block, Properties properties) { super(block, properties); }

    /** The blanket colour this stack places. */
    public static DyeColor blanket(ItemStack stack) {
        var props = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        String value = props.properties().get(FancyBedBlock.BLANKET.getName());
        DyeColor color = value == null ? null : DyeColor.byName(value, null);
        return color == null ? DyeColor.LIME : color;
    }

    /** This bed with a {@code color} blanket. */
    public ItemStack withBlanket(DyeColor color) {
        var stack = new ItemStack(this);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(FancyBedBlock.BLANKET, color));
        return stack;
    }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> tooltip,
                                          @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.fourtwenty.bed.blanket",
                Component.translatable("color.minecraft." + blanket(stack).getName())).withStyle(ChatFormatting.GRAY));
    }
}
