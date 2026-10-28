package com.malevolentgods.fourtwenty.terpene;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A Minecraft-colour terpene (mc_white ... mc_black): the 16 colours Landrace weed inspires, caught in the
 * Inspiration Infuser. Until the player has caught its colour it's an unnamed vision (its colour, from
 * the texture, always shows); then its name and flavour line.
 */
public class MinecraftTerpeneItem extends Item {
    private final DyeColor dye;

    public MinecraftTerpeneItem(DyeColor dye, Properties properties) {
        super(properties);
        this.dye = dye;
    }

    public DyeColor dye() { return dye; }

    /** "mc_light_blue". */
    public String code() { return "mc_" + dye.getSerializedName(); }

    @Override public @Nonnull Component getName(@Nonnull ItemStack stack) {
        if (!TerpeneKnowledge.knows(code())) return Component.translatable("item.fourtwenty.terpene.unknown.minecraft");
        return super.getName(stack);
    }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> tooltip,
                                          @Nonnull TooltipFlag flag) {
        if (!TerpeneKnowledge.knows(code())) {
            tooltip.add(Component.translatable("item.fourtwenty.terpene.unknown.minecraft_hint").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.addAll(TerpeneItem.flavour(code(), 40));
    }
}
