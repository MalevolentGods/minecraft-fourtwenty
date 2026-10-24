package com.malevolentgods.fourtwenty.commands;

import com.malevolentgods.fourtwenty.block.SeatEntity;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /sit: sit down where you stand. On a couch you sit on its cushion; anywhere else on solid ground
 * (a floor, a step, a slab) you sit right there. Sneak to stand up. Open to every player.
 */
@EventBusSubscriber(modid = "fourtwenty")
public final class SitCommand {
    private SitCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sit").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            if (SeatEntity.sitHere(player)) return 1;
            context.getSource().sendFailure(Component.translatable("commands.fourtwenty.sit.failed"));
            return 0;
        }));
    }
}
