package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.GearAbilities;
import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A Kush Suit piece. It protects like armour always; its powers (in {@link GearPowers}) need its wearer high and BI in it. It never
 * wears out: the bar under it is its charge.
 */
public class KushArmorItem extends ArmorItem implements Gear.GearPiece {
    public static final int BAR = 0x7BFF5A;
    private final Piece piece;

    public KushArmorItem(Piece piece, Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.stacksTo(1));
        this.piece = piece;
    }

    @Override public Piece piece() { return piece; }

    @Override public boolean isBarVisible(@Nonnull ItemStack stack) { return true; }

    @Override public int getBarWidth(@Nonnull ItemStack stack) { return GearTooltips.bar(stack); }

    @Override public int getBarColor(@Nonnull ItemStack stack) { return BAR; }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        GearTooltips.add(stack, lines);
    }

    /** Shared tooltip lines for every piece. */
    public static final class GearTooltips {
        private GearTooltips() {}

        public static int bar(ItemStack stack) {
            int cap = Gear.capacity(stack);
            return cap <= 0 ? 0 : Math.round(13f * Math.min(1f, Gear.charge(stack) / (float) cap));
        }

        public static void add(ItemStack stack, List<Component> lines) {
            lines.add(Component.translatable("tooltip.fourtwenty.gear.charge", String.format("%,d", Gear.charge(stack)), String.format("%,d", Gear.capacity(stack)))
                    .withStyle(ChatFormatting.GREEN));
            var piece = Gear.piece(stack).orElse(null);
            if (piece == null || piece.slots == 0) return;
            var terps = Gear.state(stack).terps();
            lines.add(Component.translatable("tooltip.fourtwenty.gear.slots", terps.size(), piece.slots).withStyle(ChatFormatting.DARK_GREEN));
            for (String colour : terps) {
                var ability = GearAbilities.of(piece, colour);
                lines.add(ability.isPresent()
                        ? Component.translatable("tooltip.fourtwenty.gear.ability", Component.translatable("gear.fourtwenty.ability." + ability.get().id())).withStyle(ChatFormatting.AQUA)
                        : Component.translatable("tooltip.fourtwenty.gear.no_ability").withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
