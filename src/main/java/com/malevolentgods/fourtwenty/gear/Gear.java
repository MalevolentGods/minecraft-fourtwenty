package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.GearAbilities;
import com.malevolentgods.fourtwenty.domain.GearRules;
import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModDataComponents.GearState;
import com.malevolentgods.fourtwenty.util.StrainEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** The one rule every piece of gear follows: a power runs only while its wearer is high and the piece can pay. Nothing else decides. */
public final class Gear {
    private Gear() {}

    /** An item that is a piece of gear. */
    public interface GearPiece { Piece piece(); }

    public static Optional<Piece> piece(ItemStack stack) {
        return stack.getItem() instanceof GearPiece p ? Optional.of(p.piece()) : Optional.empty();
    }

    public static GearState state(ItemStack stack) {
        GearState s = stack.get(ModDataComponents.GEAR.get());
        return s == null ? GearState.EMPTY : s;
    }

    public static void set(ItemStack stack, GearState state) { stack.set(ModDataComponents.GEAR.get(), state); }

    public static int charge(ItemStack stack) { return state(stack).charge(); }

    public static int capacity(ItemStack stack) { return piece(stack).map(p -> p.capacity).orElse(0); }

    /** Whether {@code player} can use a power of {@code stack} costing {@code cost} right now. */
    public static boolean powered(Player player, ItemStack stack, int cost) {
        return piece(stack).isPresent() && GearRules.powered(StrainEffects.isHigh(player), charge(stack), cost);
    }

    /** Pays {@code cost}; true if it could. A high wearer, like a creative player, pays nothing. */
    public static boolean spend(Player player, ItemStack stack, int cost) {
        if (!powered(player, stack, cost)) return false;
        if (!player.getAbilities().instabuild && GearRules.pays(StrainEffects.isHigh(player))) set(stack, state(stack).withCharge(GearRules.spend(charge(stack), cost)));
        return true;
    }

    /** Adds up to {@code offer} BI; returns what went in. */
    public static int fill(ItemStack stack, long offer) {
        int add = GearRules.chargeStep(charge(stack), capacity(stack), offer);
        if (add > 0) set(stack, state(stack).withCharge(charge(stack) + add));
        return add;
    }

    /** The abilities the terpenes in this piece unlock. */
    public static Set<String> unlocked(ItemStack stack) {
        var out = new HashSet<String>();
        piece(stack).ifPresent(p -> state(stack).terps().forEach(c -> GearAbilities.of(p, c).ifPresent(a -> out.add(a.id()))));
        return out;
    }

    public static boolean has(ItemStack stack, String ability) { return unlocked(stack).contains(ability); }

    /** What it costs to use {@code ability} on this piece, from the list; 0 if it isn't on the list. */
    public static int cost(ItemStack stack, String ability) {
        return piece(stack).flatMap(p -> GearAbilities.byId(p, ability)).map(GearAbilities.Ability::cost).orElse(0);
    }

    /** The piece the player wears in {@code slot}, if it's {@code piece}. */
    public static ItemStack worn(Player player, EquipmentSlot slot, Piece piece) {
        ItemStack stack = player.getItemBySlot(slot);
        return piece(stack).filter(p -> p == piece).isPresent() ? stack : ItemStack.EMPTY;
    }
}
