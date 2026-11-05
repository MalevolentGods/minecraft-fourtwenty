package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.Units;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import javax.annotation.Nonnull;

/**
 * The Inspoweedometer: reads the Kerberos, Ignus, Phus and Willards at a BI block or a Wick from the circuit's last solution. A plain
 * use puts a line on the action bar; sneaking adds the Wick's ratings in chat.
 */
public class InspoweedometerItem extends Item {
    public InspoweedometerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (level.isClientSide || player == null) return InteractionResult.sidedSuccess(level.isClientSide);
        var reading = Circuits.reading(level, context.getClickedPos());
        if (reading.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.fourtwenty.meter.none").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }
        var r = reading.get();
        Component line = r.wick()
                ? Component.translatable("message.fourtwenty.meter.wick", Units.format(r.volts(), Units.VOLTS), Units.format(r.amps(), Units.AMPS),
                        Units.format(r.ampRating(), Units.AMPS), Math.round(r.amps() / Math.max(r.ampRating(), 1e-12) * 100), Units.format(r.lossWatts(), Units.WATTS))
                : Component.translatable("message.fourtwenty.meter.device", Units.format(r.volts(), Units.VOLTS), Units.format(r.amps(), Units.AMPS),
                        Units.format(r.ohms(), Units.OHMS), Units.format(r.watts(), Units.WATTS));
        player.displayClientMessage(line.copy().withStyle(ChatFormatting.GREEN), !player.isSecondaryUseActive());
        if (player.isSecondaryUseActive() && r.wick()) {
            player.displayClientMessage(Component.translatable("message.fourtwenty.meter.wick_more", Units.format(r.voltRating(), Units.VOLTS),
                    Units.format(r.ohms(), Units.OHMS)).withStyle(ChatFormatting.DARK_GREEN), false);
        }
        return InteractionResult.SUCCESS;
    }
}
