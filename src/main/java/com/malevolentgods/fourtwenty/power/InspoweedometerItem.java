package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.Circuit;
import com.malevolentgods.fourtwenty.domain.Units;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * The Inspoweedometer: reads a BI block across its terminals (Kerberos, Ignus, Phus, Willards, and why it gets nothing if it doesn't), or a
 * Wick's current, ratings and loss. Sneak-use a Wick to make it the reference, like a meter's black probe; then any Wick of the same
 * circuit reads its voltage from the reference. Sneak-use the air to clear it.
 */
public class InspoweedometerItem extends Item {
    public InspoweedometerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && stack.has(ModDataComponents.METER_REFERENCE.get())) {
            if (!level.isClientSide) {
                stack.remove(ModDataComponents.METER_REFERENCE.get());
                player.displayClientMessage(Component.translatable("message.fourtwenty.meter.cleared").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (level.isClientSide || player == null) return InteractionResult.sidedSuccess(level.isClientSide);
        var pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        boolean wick = level.getBlockState(pos).getBlock() instanceof WickBlock;
        if (wick && player.isSecondaryUseActive()) {
            stack.set(ModDataComponents.METER_REFERENCE.get(), GlobalPos.of(level.dimension(), pos.immutable()));
            player.displayClientMessage(Component.translatable("message.fourtwenty.meter.reference").withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.SUCCESS;
        }
        GlobalPos reference = stack.get(ModDataComponents.METER_REFERENCE.get());
        if (wick && reference != null) {
            var here = Circuits.potential(level, pos);
            var there = reference.dimension() == level.dimension() ? Circuits.potential(level, reference.pos()) : java.util.Optional.<Circuits.Probe>empty();
            Component line = here.isPresent() && there.isPresent() && here.get().circuit() == there.get().circuit()
                    ? Component.translatable("message.fourtwenty.meter.from_reference", Units.format(here.get().potential() - there.get().potential(), Units.VOLTS))
                    : Component.translatable("message.fourtwenty.meter.other_circuit");
            player.displayClientMessage(line.copy().withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.SUCCESS;
        }
        var reading = Circuits.reading(level, pos);
        if (reading.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.fourtwenty.meter.none").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }
        var r = reading.get();
        Component line;
        ChatFormatting colour = ChatFormatting.GREEN;
        if (r.wick()) {
            line = Component.translatable("message.fourtwenty.meter.wick", Units.format(r.volts(), Units.VOLTS), Units.format(r.amps(), Units.AMPS),
                    Units.format(r.ampRating(), Units.AMPS), Math.round(r.amps() / Math.max(r.ampRating(), 1e-12) * 100), Units.format(r.ohms(), Units.OHMS),
                    Units.format(r.lossWatts(), Units.WATTS), Units.format(r.voltRating(), Units.VOLTS));
        } else if (r.status() == Circuit.NO_LOOP) {
            line = Component.translatable("message.fourtwenty.meter.no_loop");
            colour = ChatFormatting.GOLD;
        } else if (r.status() == Circuit.REVERSED) {
            line = Component.translatable("message.fourtwenty.meter.reversed", Units.format(r.volts(), Units.VOLTS));
            colour = ChatFormatting.GOLD;
        } else {
            line = Component.translatable(r.status() == Circuit.BROWNOUT ? "message.fourtwenty.meter.brownout" : "message.fourtwenty.meter.device",
                    Units.format(r.volts(), Units.VOLTS), Units.format(r.amps(), Units.AMPS), Units.format(r.ohms(), Units.OHMS), Units.format(r.watts(), Units.WATTS));
        }
        player.displayClientMessage(line.copy().withStyle(colour), true);
        return InteractionResult.SUCCESS;
    }
}
