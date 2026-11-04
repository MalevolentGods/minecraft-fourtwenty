package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/** A handheld battery: charged at a Re-Up Station, it tops up the gear its holder wears and holds, once a second. */
public class DabCellItem extends Item implements Gear.GearPiece {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    public DabCellItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public Piece piece() { return Piece.DAB_CELL; }

    @Override public void inventoryTick(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || level.getGameTime() % 20 != 0) return;
        topUp(player, stack);
    }

    /** Moves BI from {@code cell} into the gear {@code player} wears and holds, up to the cell's rate for a second. */
    public static int topUp(Player player, ItemStack cell) {
        int budget = Math.min(Gear.charge(cell), GearConfig.dabCell() * 20), moved = 0;
        for (EquipmentSlot slot : SLOTS) {
            if (budget - moved <= 0) break;
            ItemStack gear = player.getItemBySlot(slot);
            if (gear == cell || Gear.piece(gear).filter(p -> p != Piece.DAB_CELL).isEmpty()) continue;
            moved += Gear.fill(gear, budget - moved);
        }
        if (moved > 0) Gear.set(cell, Gear.state(cell).withCharge(Gear.charge(cell) - moved));
        return moved;
    }

    @Override public boolean isBarVisible(@Nonnull ItemStack stack) { return true; }
    @Override public int getBarWidth(@Nonnull ItemStack stack) { return KushArmorItem.GearTooltips.bar(stack); }
    @Override public int getBarColor(@Nonnull ItemStack stack) { return KushArmorItem.BAR; }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        KushArmorItem.GearTooltips.add(stack, lines);
        lines.add(Component.translatable("tooltip.fourtwenty.dab_cell.use"));
    }
}
