package com.malevolentgods.fourtwenty.item;

import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.domain.GrowRules;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
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
        // its family: what it makes in a Terpinator
        if (strain != Strain.LANDRACE) {
            lines.add(Component.translatable("strain.fourtwenty.family." + strain.family().name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        var climate = stack.get(ModDataComponents.CLIMATE.get());
        if (climate != null) lines.add(Component.translatable("tooltip.fourtwenty.climate", Component.translatable(climate.translationKey())).withStyle(net.minecraft.ChatFormatting.GREEN));
        Traits t = ModDataComponents.traits(stack);
        lines.add(Component.translatable("tooltip.fourtwenty.traits", t.potency(), t.yield(), t.length()).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    /** The plot a strain needs, or null if it grows anywhere. */
    public static @Nullable Component plotName(Strain strain) {
        return switch (GrowRules.gradePlot(strain.grade())) {
            case 1 -> Component.translatable("block.fourtwenty.closet_grow");
            case 3 -> Component.translatable("block.fourtwenty.hydro_bed");
            case 4 -> Component.translatable("block.fourtwenty.aero_pod");
            case 5 -> Component.translatable("block.fourtwenty.cosmic_kush_pod");
            default -> null;
        };
    }

    /** Weed seeds: plant the strain they carry. */
    public static class Seeds extends ItemNameBlockItem {
        public Seeds(Block crop, Properties properties) { super(crop, properties); }

        @Override
        protected @Nullable BlockState getPlacementState(@Nonnull BlockPlaceContext context) {
            BlockState state = super.getPlacementState(context);
            if (state == null || !state.hasProperty(WeedCropBlock.STRAIN)) return state;
            Strain strain = ModDataComponents.strain(context.getItemInHand());
            if (WeedCropBlock.plotted(context.getLevel(), context.getClickedPos(), strain)) return state.setValue(WeedCropBlock.STRAIN, strain);
            if (context.getPlayer() != null && !context.getLevel().isClientSide) {
                context.getPlayer().displayClientMessage(Component.translatable("item.fourtwenty.weed_seeds.needs_plot", strainName(strain), plotName(strain)), true);
            }
            return null;
        }

        @Override
        public @Nonnull Component getName(@Nonnull ItemStack stack) { return name(stack, super.getName(stack), getDescriptionId()); }

        @Override
        public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
            tooltip(stack, lines);
            Component plot = plotName(ModDataComponents.strain(stack));
            if (plot != null) lines.add(Component.translatable("item.fourtwenty.weed_seeds.grows_on", plot).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /** Fresh from the plant: trim it, and the tooltip says the strain's seed chance. */
    public static class RoughBud extends Bud {
        public RoughBud(Properties properties) { super(properties); }

        @Override
        public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
            super.appendHoverText(stack, context, lines, flag);
            int chance = com.malevolentgods.fourtwenty.domain.TrimRules.percent(com.malevolentgods.fourtwenty.processing.ProcessingConfig.chances(), ModDataComponents.strain(stack));
            lines.add(Component.translatable("item.fourtwenty.rough_bud.seed_chance", chance).withStyle(net.minecraft.ChatFormatting.GRAY));
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
