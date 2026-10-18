package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Affinity;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Preference;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.JointService;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Op-only testing tools for the Dispensary (permission level 2), usable from chat or the server
 * console. They set up scenarios that otherwise take in-game days (cravings, tolerance breaks,
 * drift) and print exactly what the mod's saved state says.
 *
 * <pre>
 * /fourtwenty status &lt;citizens&gt;                  exposure log, affinity, demand, carried doses
 * /fourtwenty expose &lt;citizens&gt; [type] [step]    first exposure (rolled, or a fixed type/frequency)
 * /fourtwenty forget &lt;citizens&gt;                  back to "never tried it"
 * /fourtwenty due &lt;citizens&gt;                     crave right now
 * /fourtwenty break &lt;citizens&gt; start|end         tolerance break
 * /fourtwenty drift &lt;citizens&gt; &lt;rolls&gt;           run drift rolls with the configured odds
 * /fourtwenty clock &lt;ticks&gt;                       advance the dispensary clock
 * /fourtwenty stock &lt;item&gt; &lt;count&gt;               fill the nearest Dispensary's storage
 * </pre>
 */
public final class DispensaryCommands {
    private static final SuggestionProvider<CommandSourceStack> PREFERENCES = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Preference.values()).map(p -> p.name().toLowerCase(Locale.ROOT)), builder);
    private static final SuggestionProvider<CommandSourceStack> ITEMS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(MenuItem.values()).map(i -> i.name().toLowerCase(Locale.ROOT)), builder);

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fourtwenty").requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").then(Commands.argument("citizens", EntityArgument.entities())
                        .executes(DispensaryCommands::status)))
                .then(Commands.literal("expose").then(Commands.argument("citizens", EntityArgument.entities())
                        .executes(context -> expose(context, null, 0))
                        .then(Commands.argument("type", StringArgumentType.word()).suggests(PREFERENCES)
                                .then(Commands.argument("step", IntegerArgumentType.integer(0, AffinityRules.MAX_STEP - 1))
                                        .executes(context -> expose(context, preference(context), IntegerArgumentType.getInteger(context, "step")))))))
                .then(Commands.literal("forget").then(Commands.argument("citizens", EntityArgument.entities())
                        .executes(DispensaryCommands::forget)))
                .then(Commands.literal("due").then(Commands.argument("citizens", EntityArgument.entities())
                        .executes(DispensaryCommands::due)))
                .then(Commands.literal("break").then(Commands.argument("citizens", EntityArgument.entities())
                        .then(Commands.literal("start").executes(context -> toleranceBreak(context, true)))
                        .then(Commands.literal("end").executes(context -> toleranceBreak(context, false)))))
                .then(Commands.literal("drift").then(Commands.argument("citizens", EntityArgument.entities())
                        .then(Commands.argument("rolls", IntegerArgumentType.integer(1, 1000)).executes(DispensaryCommands::drift))))
                .then(Commands.literal("clock").then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1_000_000))
                        .executes(DispensaryCommands::clock)))
                .then(Commands.literal("stock").then(Commands.argument("item", StringArgumentType.word()).suggests(ITEMS)
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 4096)).executes(DispensaryCommands::stock)))));
    }

    private static List<EntityCitizen> citizens(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var found = new ArrayList<EntityCitizen>();
        for (var entity : EntityArgument.getEntities(context, "citizens")) {
            if (entity instanceof EntityCitizen citizen && citizen.getCitizenData() != null) found.add(citizen);
        }
        if (found.isEmpty()) context.getSource().sendFailure(Component.literal("No colonists matched (use @e[type=minecolonies:citizen,...])."));
        return found;
    }

    private static DispensarySavedData saved(CommandContext<CommandSourceStack> context) {
        return DispensarySavedData.get(context.getSource().getLevel());
    }

    private static Preference preference(CommandContext<CommandSourceStack> context) {
        return Preference.valueOf(StringArgumentType.getString(context, "type").toUpperCase(Locale.ROOT));
    }

    private static void say(CommandContext<CommandSourceStack> context, String text) {
        context.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static String describe(DispensarySavedData saved, EntityCitizen citizen) {
        var data = citizen.getCitizenData();
        long now = saved.time();
        var affinity = saved.affinity(data.getColony().getID(), data.getUUID());
        var demand = saved.citizen(data.getColony().getID(), data.getUUID());
        var line = new StringBuilder(data.getName()).append(": ");
        if (!affinity.exposed()) line.append("first exposure N/A");
        else {
            line.append(affinity.preference().name().toLowerCase(Locale.ROOT)).append(", step ").append(affinity.step())
                    .append(" (base ").append(affinity.baseStep()).append("), exposed ").append(now - affinity.exposedAt()).append(" ticks ago");
            if (affinity.onBreak(now)) line.append(", tolerance break ").append(affinity.breakUntil() - now).append(" ticks left");
        }
        line.append(" | home ").append(JointService.homeLevel(citizen));
        if (!demand.eligible()) line.append(" | no craving");
        else {
            line.append(" | ").append(demand.status(now).name().toLowerCase(Locale.ROOT));
            line.append(demand.due(now) ? ", due now" : ", due in " + (demand.dueAt() - now) + " ticks");
        }
        var doses = new ArrayList<String>();
        var inventory = citizen.getInventoryCitizen();
        for (var item : MenuItem.values()) {
            int count = JointService.count(List.of(inventory), JointService.item(item));
            if (count > 0) doses.add(count + " " + item.name().toLowerCase(Locale.ROOT));
        }
        line.append(" | carrying ").append(doses.isEmpty() ? "nothing" : String.join(", ", doses));
        return line.toString();
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var saved = saved(context);
        var found = citizens(context);
        say(context, "Dispensary clock: " + saved.time());
        for (var citizen : found) say(context, describe(saved, citizen));
        return found.size();
    }

    private static int expose(CommandContext<CommandSourceStack> context, Preference preference, int step) throws CommandSyntaxException {
        var saved = saved(context);
        var level = context.getSource().getLevel();
        var found = citizens(context);
        for (var citizen : found) {
            var data = citizen.getCitizenData();
            long now = saved.time();
            saved.updateAffinity(data.getColony().getID(), data.getUUID(), affinity -> preference == null
                    ? AffinityRules.expose(Affinity.UNEXPOSED, now, level.random::nextDouble)
                    : new Affinity(now, preference, step, step, -1, now));
            say(context, describe(saved, citizen));
        }
        return found.size();
    }

    private static int forget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var saved = saved(context);
        var found = citizens(context);
        for (var citizen : found) {
            saved.updateAffinity(citizen.getCitizenData().getColony().getID(), citizen.getCitizenData().getUUID(), affinity -> Affinity.UNEXPOSED);
            say(context, describe(saved, citizen));
        }
        return found.size();
    }

    private static int due(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var saved = saved(context);
        var found = citizens(context);
        for (var citizen : found) {
            JointService.due(citizen);   // observe first so a just-exposed citizen has a demand record
            if (!saved.forceDue(citizen.getCitizenData().getColony().getID(), citizen.getCitizenData().getUUID())) {
                say(context, citizen.getCitizenData().getName() + ": no craving to make due (expose them, give them a home)");
            } else say(context, describe(saved, citizen));
        }
        return found.size();
    }

    private static int toleranceBreak(CommandContext<CommandSourceStack> context, boolean start) throws CommandSyntaxException {
        var saved = saved(context);
        var found = citizens(context);
        int ticks = DispensaryConfig.tuning().breakTicks();
        for (var citizen : found) {
            var data = citizen.getCitizenData();
            long now = saved.time();
            saved.updateAffinity(data.getColony().getID(), data.getUUID(), affinity -> !affinity.exposed() ? affinity : start
                    ? new Affinity(affinity.exposedAt(), affinity.preference(), affinity.baseStep(), AffinityRules.MAX_STEP, now + ticks, affinity.lastBongRoll())
                    : AffinityRules.tick(new Affinity(affinity.exposedAt(), affinity.preference(), affinity.baseStep(), affinity.step(),
                            affinity.breakUntil() < 0 ? -1 : now, affinity.lastBongRoll()), now));
            say(context, describe(saved, citizen));
        }
        return found.size();
    }

    private static int drift(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var saved = saved(context);
        var level = context.getSource().getLevel();
        int rolls = IntegerArgumentType.getInteger(context, "rolls");
        var found = citizens(context);
        for (var citizen : found) {
            var data = citizen.getCitizenData();
            var steps = new StringBuilder();
            for (int i = 0; i < rolls; i++) {
                var after = saved.updateAffinity(data.getColony().getID(), data.getUUID(),
                        affinity -> AffinityRules.drift(affinity, saved.time(), DispensaryConfig.tuning(), level.random::nextDouble));
                if (i < 50) steps.append(after.step());
            }
            say(context, data.getName() + " steps: " + steps + (rolls > 50 ? "..." : ""));
            say(context, describe(saved, citizen));
        }
        return found.size();
    }

    private static int clock(CommandContext<CommandSourceStack> context) {
        var saved = saved(context);
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        for (int i = 0; i < ticks; i++) saved.advance();
        say(context, "Dispensary clock advanced " + ticks + " ticks to " + saved.time()
                + " (deadlines, grace and tolerance breaks move with it; the world clock does not).");
        return ticks;
    }

    private static int stock(CommandContext<CommandSourceStack> context) {
        MenuItem item;
        try {
            item = MenuItem.valueOf(StringArgumentType.getString(context, "item").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Unknown menu item; use joint, cookie, pipe or brownie."));
            return 0;
        }
        int count = IntegerArgumentType.getInteger(context, "count");
        var hut = nearestDispensary(context.getSource().getLevel(), BlockPos.containing(context.getSource().getPosition()));
        if (hut == null || hut.getItemHandlerCap() == null) {
            context.getSource().sendFailure(Component.literal("No Dispensary with storage found in this colony."));
            return 0;
        }
        var stack = new ItemStack(JointService.item(item));
        int added = 0;
        while (added < count) {
            int batch = Math.min(count - added, stack.getMaxStackSize());
            var rest = ItemHandlerHelper.insertItemStacked(hut.getItemHandlerCap(), stack.copyWithCount(batch), false);
            added += batch - rest.getCount();
            if (!rest.isEmpty()) break;
        }
        int stored = added;
        say(context, "Added " + stored + " " + item.name().toLowerCase(Locale.ROOT) + " to the Dispensary at " + hut.getPosition().toShortString()
                + (stored < count ? " (storage full)" : ""));
        return stored;
    }

    private static DispensaryBuilding nearestDispensary(ServerLevel level, BlockPos at) {
        DispensaryBuilding best = null;
        for (var colony : IColonyManager.getInstance().getColonies(level)) {
            for (var building : colony.getServerBuildingManager().getBuildings().values()) {
                if (building instanceof DispensaryBuilding hut
                        && (best == null || hut.getPosition().distSqr(at) < best.getPosition().distSqr(at))) best = hut;
            }
        }
        return best;
    }

    private DispensaryCommands() {}
}
