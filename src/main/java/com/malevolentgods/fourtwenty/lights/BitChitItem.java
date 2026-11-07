package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.Programs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nonnull;
import java.util.List;

/** A Bit Chit: blank, or holding one Weedweeno program to carry, copy (with a blank one in a crafting grid) and load elsewhere. */
public class BitChitItem extends Item {
    public BitChitItem(Properties properties) {
        super(properties);
    }

    public static Programs.Program program(ItemStack stack) { return stack.get(ModLights.PROGRAM.get()); }

    public static boolean blank(ItemStack stack) { return stack.is(ModLights.BIT_CHIT.get()) && program(stack) == null; }

    public static ItemStack written(Programs.Program program) {
        ItemStack stack = new ItemStack(ModLights.BIT_CHIT.get());
        stack.set(ModLights.PROGRAM.get(), program);
        return stack;
    }

    @Override public int getMaxStackSize(@Nonnull ItemStack stack) { return program(stack) == null ? 16 : 1; }

    @Override public boolean isFoil(@Nonnull ItemStack stack) { return program(stack) != null; }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        var program = program(stack);
        if (program == null) {
            lines.add(Component.translatable("tooltip.fourtwenty.bit_chit.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.literal(program.name()).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.fourtwenty.bit_chit.size", program.code().lines().count(), program.code().length()).withStyle(ChatFormatting.DARK_AQUA));
    }
}
