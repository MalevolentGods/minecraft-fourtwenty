package com.malevolentgods.fourtwenty.handbook;

import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Def;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /fourtwenty handbook ...} for operators (specs/016-weedcraft-handbook FR-027): reset a player's progress (all of it, or one rank or
 * badge) for a balance run or a do-over, tick a requirement for testing, and show where a player stands.
 */
@EventBusSubscriber(modid = "fourtwenty")
public final class HandbookCommands {
    private HandbookCommands() {}

    private static final SuggestionProvider<CommandSourceStack> DEFS = (ctx, b) ->
            SharedSuggestionProvider.suggest(ScoutingData.defs().stream().map(Def::earnKey), b);

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> handbook = Commands.literal("handbook")
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> reset(c.getSource(), EntityArgument.getPlayer(c, "player"), null))
                        .then(Commands.argument("what", StringArgumentType.greedyString()).suggests(DEFS)
                                .executes(c -> reset(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "what"))))))
                .then(Commands.literal("tick").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("key", StringArgumentType.greedyString())
                                .executes(c -> tick(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "key"))))))
                .then(Commands.literal("show").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "player")))));
        event.getDispatcher().register(Commands.literal("fourtwenty").requires(s -> s.hasPermission(2)).then(handbook));
    }

    private static int reset(CommandSourceStack src, ServerPlayer player, String what) {
        var p = Deeds.progress(player);
        String target = what == null || what.equals("all") ? null : what;
        if (target != null && ScoutingData.byEarnKey(target) == null) {
            src.sendFailure(Component.literal("No such rank or badge: " + target + " (like rank:tenderbud or badge:munchies)"));
            return 0;
        }
        p.reset(ScoutingData.defs(), target);
        Deeds.save(player, p);
        Deeds.fullSync(player);
        HandbookNetwork.sashChanged(player, p);
        src.sendSuccess(() -> Component.literal("Reset " + (target == null ? "all of " + player.getName().getString() + "'s Handbook progress" : target)), true);
        return 1;
    }

    private static int tick(CommandSourceStack src, ServerPlayer player, String key) {
        var p = Deeds.progress(player);
        var r = p.force(ScoutingData.defs(), key, player.level().getGameTime());
        if (!r.any()) {
            src.sendFailure(Component.literal("Nothing to tick for " + key + " (a requirement key like rank:tenderbud/2a, not already done)"));
            return 0;
        }
        Deeds.save(player, p);
        Deeds.sync(player, p, r);
        if (!r.earned().isEmpty()) HandbookNetwork.announce(player, r.earned());
        src.sendSuccess(() -> Component.literal("Ticked " + r.ticked() + (r.earned().isEmpty() ? "" : ", earned " + r.earned())), true);
        return 1;
    }

    private static int show(CommandSourceStack src, ServerPlayer player) {
        var p = Deeds.progress(player);
        int rank = p.rank(ScoutingData.defs());
        StringBuilder b = new StringBuilder(player.getName().getString() + ": rank " + rank + "; badges " + HandbookNetwork.badges(p));
        for (Def d : ScoutingData.defs()) {
            if (p.earned(d.earnKey())) continue;
            long done = d.reqs().stream().filter(r -> p.satisfied(d.key(r))).count();
            if (done > 0) b.append("\n  ").append(d.earnKey()).append(": ").append(done).append("/").append(d.reqs().size());
        }
        src.sendSuccess(() -> Component.literal(b.toString()), false);
        return 1;
    }
}
