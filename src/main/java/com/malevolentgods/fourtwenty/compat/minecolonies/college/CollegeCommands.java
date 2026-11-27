package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.malevolentgods.fourtwenty.domain.college.ColonyResearch;
import com.minecolonies.api.colony.IColony;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /fourtwenty college queue|remove|finish <study>} and {@code /fourtwenty college status}, for the colony you stand in. Operators
 * only: players use the College's board; this is for testing and for fixing a colony by hand.
 */
public final class CollegeCommands {
    private CollegeCommands() {}

    private static final SuggestionProvider<CommandSourceStack> STUDIES = (context, builder) ->
            SharedSuggestionProvider.suggest(CollegeStudies.board().all().stream().map(s -> s.id()).toList(), builder);

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fourtwenty").requires(source -> source.hasPermission(2))
                .then(Commands.literal("college")
                        .then(Commands.literal("status").executes(CollegeCommands::status))
                        .then(Commands.literal("queue").then(Commands.argument("study", StringArgumentType.greedyString()).suggests(STUDIES)
                                .executes(context -> act(context, "queue"))))
                        .then(Commands.literal("remove").then(Commands.argument("study", StringArgumentType.greedyString()).suggests(STUDIES)
                                .executes(context -> act(context, "remove"))))
                        .then(Commands.literal("finish").then(Commands.argument("study", StringArgumentType.greedyString()).suggests(STUDIES)
                                .executes(context -> act(context, "finish"))))));
    }

    private static IColony colony(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        IColony colony = CollegeResearch.colonyAt(source.getLevel(), BlockPos.containing(source.getPosition()));
        if (colony == null) source.sendFailure(Component.translatable("college.fourtwenty.command.no_colony"));
        return colony;
    }

    private static int act(CommandContext<CommandSourceStack> context, String action) {
        IColony colony = colony(context);
        if (colony == null) return 0;
        String study = StringArgumentType.getString(context, "study").trim();
        if (!study.contains(":")) study = "fourtwenty:" + study;
        String refused = switch (action) {
            case "queue" -> CollegeResearch.queue(colony, study);
            case "remove" -> CollegeResearch.remove(colony, study);
            default -> {
                if (CollegeStudies.board().get(study) == null) yield "college.fourtwenty.refused.unknown";
                CollegeResearch.finish(colony, study);
                yield null;
            }
        };
        if (refused != null) {
            context.getSource().sendFailure(Component.translatable(refused));
            return 0;
        }
        String done = study;
        context.getSource().sendSuccess(() -> Component.translatable("college.fourtwenty.command." + action, Component.translatable(CollegeResearch.nameKey(done))), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        IColony colony = colony(context);
        if (colony == null) return 0;
        ColonyResearch r = CollegeResearch.record(colony);
        String head = r.head();
        context.getSource().sendSuccess(() -> Component.literal("College level " + CollegeResearch.collegeLevel(colony) + "; queue " + r.queue()
                + (head == null ? "" : "; " + head + " " + r.progress(head) + " units, missing " + r.missing().size() + " supply lines")
                + "; done " + r.done().size() + "; bad batches " + r.badBatches()), false);
        return 1;
    }
}
