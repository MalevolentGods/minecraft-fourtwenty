package com.malevolentgods.fourtwenty.item;

import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Names and tooltips for items that carry a strain ("Blue Dream Bud"); Landrace keeps the plain name. */
public final class StrainItems {
    private StrainItems() {}

    public static Component strainName(Strain strain) {
        return Component.translatable(strain.translationKey()).withStyle(Style.EMPTY.withColor(strain.color()));
    }

    public static Component name(ItemStack stack, Component plain, String key) {
        Strain s = ModDataComponents.strain(stack);
        return s == Strain.LANDRACE ? plain : Component.translatable(key + ".strain", Component.translatable(s.translationKey()));
    }

    public static void tooltip(ItemStack stack, List<Component> lines) {
        Strain strain = ModDataComponents.strain(stack);
        lines.add(Component.translatable("tooltip.fourtwenty.strain", strainName(strain)));
        // what growing it is for: what it inspires, and what it makes in a Terpinator
        net.minecraft.ChatFormatting grey = net.minecraft.ChatFormatting.GRAY;
        if (strain == Strain.LANDRACE) {
            lines.add(Component.translatable("tooltip.fourtwenty.strain.landrace").withStyle(grey));
            lines.add(Component.translatable("tooltip.fourtwenty.strain.terpinator.landrace").withStyle(grey));
            return;
        }
        Component family = Component.translatable("strain.fourtwenty.family." + strain.family().name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(Style.EMPTY.withColor(strain.color()));
        lines.add(Component.translatable("tooltip.fourtwenty.strain.inspires", family,
                Component.translatable("terpene.fourtwenty." + strain.id() + ".hue")).withStyle(grey));
        lines.add(Component.translatable("tooltip.fourtwenty.strain.terpinator", family).withStyle(grey));
    }

    /** Weed seeds: plant the strain they carry. */
    public static class Seeds extends ItemNameBlockItem {
        public Seeds(Block crop, Properties properties) { super(crop, properties); }

        @Override
        protected @Nullable BlockState getPlacementState(@Nonnull BlockPlaceContext context) {
            BlockState state = super.getPlacementState(context);
            return state != null && state.hasProperty(WeedCropBlock.STRAIN)
                    ? state.setValue(WeedCropBlock.STRAIN, ModDataComponents.strain(context.getItemInHand())) : state;
        }

        @Override
        public @Nonnull Component getName(@Nonnull ItemStack stack) { return name(stack, super.getName(stack), getDescriptionId()); }

        @Override
        public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
            tooltip(stack, lines);
        }
    }

    /** Weed buds. */
    public static class Bud extends Item {
        public Bud(Properties properties) { super(properties); }

        @Override
        public @Nonnull Component getName(@Nonnull ItemStack stack) { return name(stack, super.getName(stack), getDescriptionId()); }

        @Override
        public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
            tooltip(stack, lines);
        }
    }
}
