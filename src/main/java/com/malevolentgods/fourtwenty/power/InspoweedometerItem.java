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

    /**
     * A meter reads what it touches and never works it: with it in hand, a block's own right-click (stepping a Resinator, flipping a
     * Toggle Joint, opening a machine) doesn't happen, so the reading is of the circuit as it was. Both sides, so the client agrees.
     */
    public static void rightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof InspoweedometerItem) event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
    }

    @Override public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // use in the air: clears the held reading; sneaking, the Wick reference too
        boolean sneak = player.isSecondaryUseActive();
        if (held(stack) != null || sneak && stack.has(ModDataComponents.METER_REFERENCE.get())) {
            if (!level.isClientSide) {
                if (sneak) stack.remove(ModDataComponents.METER_REFERENCE.get());
                stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
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
        Component line = read(level, player, pos, stack, false);
        if (line != null) {
            var colour = line.getStyle().getColor();
            hold(stack, line.getString(), colour == null ? 0xFFFFFF : colour.getValue());
            player.displayClientMessage(line, true);
        }
        return InteractionResult.SUCCESS;
    }

    /** The reading a click held, shown above the live one until sneak-use in the air clears it; null if none. */
    public static @javax.annotation.Nullable String held(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        var tag = data.copyTag();
        return tag.contains("held") ? tag.getString("held") : null;
    }

    /** The held reading's colour, as the live one had it. */
    public static int heldColour(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null || !data.copyTag().contains("colour") ? 0xFFFFFF : data.copyTag().getInt("colour");
    }

    static void hold(ItemStack stack, String reading, int colour) {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("held", reading);
        tag.putInt("colour", colour);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    /**
     * What the meter says about the block at {@code pos}: a Wick against the reference, a component's pins, a cable's cores, a device's
     * voltage, current and power. {@code live} (it's only being pointed at): null where there is nothing to read, so it says nothing.
     */
    static @javax.annotation.Nullable Component read(Level level, Player player, net.minecraft.core.BlockPos pos, ItemStack stack, boolean live) {
        boolean wick = level.getBlockState(pos).getBlock() instanceof WickBlock;
        GlobalPos reference = stack.get(ModDataComponents.METER_REFERENCE.get());
        if (wick && reference != null) {
            var here = Circuits.potential(level, pos);
            var there = reference.dimension() == level.dimension() ? Circuits.potential(level, reference.pos()) : java.util.Optional.<Circuits.Probe>empty();
            Component line = here.isPresent() && there.isPresent() && here.get().circuit() == there.get().circuit()
                    ? Component.translatable("message.fourtwenty.meter.from_reference", Units.format(here.get().potential() - there.get().potential(), Units.VOLTS))
                    : Component.translatable("message.fourtwenty.meter.other_circuit");
            return line.copy().withStyle(ChatFormatting.AQUA);
        }
        if (level.getBlockState(pos).getBlock() instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlock cb) {
            var type = cb.type();
            var reading = Circuits.componentReading(level, pos);
            if (reading.isEmpty()) {
                return live ? null : Component.translatable("message.fourtwenty.meter.none").withStyle(ChatFormatting.GRAY);
            }
            double[] v = reading.get().pins();
            StringBuilder text = new StringBuilder();
            if (type.chip()) {
                // VDD, then each signal and bus as bits against GND; a bus reads channel 7 first, as a byte is written
                double supply = v[0] - v[1];
                java.util.function.IntFunction<String> bit = i -> {
                    double at = v[i] - v[1];
                    return at >= 0.7 * supply ? "1" : at <= 0.3 * supply ? "0" : "?";
                };
                java.util.function.BiFunction<Integer, Integer, String> word = (from, count) -> {
                    StringBuilder b = new StringBuilder();
                    for (int i = from + count - 1; i >= from; i--) b.append(bit.apply(i));
                    return b.toString();
                };
                text.append("VDD ").append(Units.format(supply, Units.VOLTS)).append(" · ");
                switch (type.part) {
                    case SHIFT_OUT -> text.append("data ").append(bit.apply(2)).append(" clk ").append(bit.apply(3)).append(" latch ").append(bit.apply(4))
                            .append(" · out ").append(word.apply(5, 8)).append(" carry ").append(bit.apply(13));
                    case SHIFT_IN -> text.append("load ").append(bit.apply(2)).append(" clk ").append(bit.apply(3)).append(" · in ").append(word.apply(4, 8))
                            .append(" · out ").append(bit.apply(13));
                    case MUX -> text.append("in ").append(word.apply(2, 8)).append(" · select ").append(word.apply(11, 3)).append(" · out ").append(bit.apply(20));
                    case BITWISE -> text.append(type.describe(level.getBlockEntity(pos) instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity be ? be.value() : 0))
                            .append(" · A ").append(word.apply(2, 9)).append(" · B ").append(word.apply(11, 9)).append(" · out ").append(word.apply(20, 9));
                    case ARITH -> text.append(type.describe(level.getBlockEntity(pos) instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity be ? be.value() : 0))
                            .append(" · A ").append(word.apply(2, 8)).append(" · B ").append(word.apply(11, 8)).append(" · carry in ").append(bit.apply(20))
                            .append(" · out ").append(word.apply(21, 8)).append(" carry ").append(bit.apply(29));
                    case COMPARE -> text.append("A ").append(word.apply(2, 8)).append(" · B ").append(word.apply(11, 8)).append(" · < ").append(bit.apply(20))
                            .append(" = ").append(bit.apply(21)).append(" > ").append(bit.apply(22));
                    case DISPLAY -> text.append(type.describe(level.getBlockEntity(pos) instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity be ? be.value() : 0))
                            .append(" · in ").append(word.apply(2, 8));
                    case REGISTER -> text.append("in ").append(word.apply(2, 9)).append(" · clk ").append(bit.apply(11)).append(" release ").append(bit.apply(12))
                            .append(" · out ").append(word.apply(13, 9));
                    default -> text.append("in ").append(bit.apply(2)).append(" · select ").append(word.apply(3, 3)).append(" · out ").append(word.apply(12, 8));
                }
            } else if (type.fet()) {
                // pins {source, drain, gate}: the gate and drain against the source, and the channel current into the drain
                text.append("Vgs ").append(Units.format(v[2] - v[0], Units.VOLTS)).append(" · Vds ").append(Units.format(v[1] - v[0], Units.VOLTS))
                        .append(" · Id ").append(Units.format(reading.get().amps(), Units.AMPS));
            } else if (type.part == com.malevolentgods.fourtwenty.electronics.ComponentType.Part.OPAMP) {
                // pins {vdd, gnd, out, +, −}, each against GND
                text.append("VDD ").append(Units.format(v[0] - v[1], Units.VOLTS)).append(" · + ").append(Units.format(v[3] - v[1], Units.VOLTS))
                        .append(" · − ").append(Units.format(v[4] - v[1], Units.VOLTS)).append(" · out ").append(Units.format(v[2] - v[1], Units.VOLTS))
                        .append(" · ").append(Units.format(reading.get().amps(), Units.AMPS));
            } else if (!type.logic()) {
                if (level.getBlockEntity(pos) instanceof com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity be && !type.values.isEmpty()) {
                    text.append(com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity.format(be.value(), type.unit)).append(" · ");
                }
                double amps = Math.abs(reading.get().amps());
                text.append(Units.format(v[0] - v[1], Units.VOLTS)).append(" · ").append(Units.format(amps, Units.AMPS))
                        .append(" · ").append(Units.format(Math.abs(v[0] - v[1]) * amps, Units.WATTS));
                if (type.ratedWatts() > 0) text.append(" of ").append(Units.format(type.ratedWatts(), Units.WATTS));
            } else {
                // VDD and GND, then each logic pin against GND, and the bit it reads as at 30% and 70% of the supply
                double supply = v[0] - v[1];
                text.append("VDD ").append(Units.format(supply, Units.VOLTS));
                String[] names = type.layout == com.malevolentgods.fourtwenty.electronics.ComponentType.Layout.TWO_INPUT ? new String[]{"out", "L", "R"}
                        : type.layout == com.malevolentgods.fourtwenty.electronics.ComponentType.Layout.ONE_INPUT ? new String[]{"out", "in"} : new String[]{"out"};
                for (int i = 2; i < v.length; i++) {
                    double at = v[i] - v[1];
                    String bit = at >= 0.7 * supply ? "1" : at <= 0.3 * supply ? "0" : "?";
                    text.append(" · ").append(names[i - 2]).append(' ').append(Units.format(at, Units.VOLTS)).append(" (").append(bit).append(')');
                }
            }
            return Component.translatable("message.fourtwenty.meter.component", Component.translatable("block.fourtwenty." + type.id), text.toString())
                    .withStyle(ChatFormatting.GREEN);
        }
        if (level.getBlockState(pos).getBlock() instanceof CableBlock cable) {
            var why = Circuits.daisyFailure(level, pos);
            if (why.isPresent()) {
                String key = why.get() == com.malevolentgods.fourtwenty.domain.DaisyLine.Reason.RING ? "message.fourtwenty.meter.daisy_ring" : "message.fourtwenty.meter.daisy_branch";
                return Component.translatable(key).withStyle(ChatFormatting.GOLD);
            }
            var c = Circuits.cableReading(level, pos).orElseThrow();
            Component line = cable.kind() == CableBlock.Kind.TWIN
                    ? Component.translatable("message.fourtwenty.meter.twin", Units.format(c.volts(), Units.VOLTS), Units.format(c.plusAmps(), Units.AMPS),
                            Units.format(c.minusAmps(), Units.AMPS), Units.format(c.ampRating(), Units.AMPS), Units.format(c.ohms(), Units.OHMS), Units.format(c.lossWatts(), Units.WATTS))
                    : Component.translatable("message.fourtwenty.meter.daisy", Units.format(c.plusAmps(), Units.AMPS), Units.format(c.ampRating(), Units.AMPS),
                            Units.format(c.volts(), Units.VOLTS), Units.format(c.ohms(), Units.OHMS), Units.format(c.lossWatts(), Units.WATTS));
            return line.copy().withStyle(ChatFormatting.GREEN);
        }
        var reading = Circuits.reading(level, pos);
        if (reading.isEmpty()) {
            return live ? null : Component.translatable("message.fourtwenty.meter.none").withStyle(ChatFormatting.GRAY);
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
        return line.copy().withStyle(colour);
    }

    /** Live: while a player holds the meter, whatever block they look at is read on their action bar, five times a second. */
    public static void playerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) || player.tickCount % 4 != 0) return;
        ItemStack stack = player.getMainHandItem().getItem() instanceof InspoweedometerItem ? player.getMainHandItem()
                : player.getOffhandItem().getItem() instanceof InspoweedometerItem ? player.getOffhandItem() : ItemStack.EMPTY;
        if (stack.isEmpty()) return;
        if (!(player.pick(player.blockInteractionRange(), 0, false) instanceof net.minecraft.world.phys.BlockHitResult hit)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        Component line = read(player.level(), player, hit.getBlockPos(), stack, true);
        if (line != null) player.displayClientMessage(line, true);
    }
}
