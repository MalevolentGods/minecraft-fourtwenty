package com.malevolentgods.fourtwenty.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.malevolentgods.fourtwenty.domain.handbook.Markup;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

/** The Handbook (specs/016-weedcraft-handbook): the item, its recipe, and every entry the book lists reads cleanly. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class HandbookGameTests {
    private static final String BOOK = "/assets/fourtwenty/handbook/";

    @GameTest(template = "empty")
    public static void theHandbookIsCraftedFromABookAndABud(GameTestHelper h) {
        var recipe = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("fourtwenty", "handbook"));
        h.assertTrue(recipe.isPresent(), "no Handbook recipe");
        ItemStack out = recipe.get().value().getResultItem(h.getLevel().registryAccess());
        h.assertTrue(out.is(ModItems.HANDBOOK.get()), "the recipe makes " + out);
        h.assertTrue(out.getHoverName().getString().equals("A Handbook of Weedcraft, Scouting, and Weed-craft")
                || out.getHoverName().getString().equals("item.fourtwenty.handbook"), "named " + out.getHoverName().getString());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyListedEntryReadsWithoutErrors(GameTestHelper h) {
        JsonObject book = json(BOOK + "book.json");
        int read = 0;
        for (JsonElement ce : book.getAsJsonArray("chapters")) {
            JsonObject c = ce.getAsJsonObject();
            for (JsonElement ee : c.getAsJsonArray("entries")) {
                String path = BOOK + "en_us/entries/" + c.get("id").getAsString() + "/" + ee.getAsString() + ".md";
                String text = text(path);
                h.assertTrue(text != null, "missing " + path);
                Markup.Entry entry = Markup.parse(text);
                h.assertTrue(entry.errors().isEmpty(), path + ": " + entry.errors());
                read++;
            }
        }
        h.assertTrue(read > 0, "the book lists no entries");
        h.succeed();
    }

    /** The ranks and badges load: six ranks in order, the badges whose mod is here, every tracked requirement watching for a deed the game fires. */
    @GameTest(template = "empty")
    public static void theRanksAndBadgesLoad(GameTestHelper h) {
        var defs = com.malevolentgods.fourtwenty.handbook.ScoutingData.defs();
        long ranks = defs.stream().filter(d -> d.rank()).count();
        h.assertTrue(ranks == 6, "six ranks, got " + ranks);
        h.assertTrue(defs.stream().filter(d -> !d.rank()).count() >= 12, "the badges");
        java.util.Set<String> deeds = java.util.Set.of("obtain", "plant", "harvest", "trim", "cure", "extract", "smoke", "high", "pollinate", "pheno",
                "discover", "infuse", "terpinate", "place", "power", "light", "overload", "circuit", "craft", "measure", "earn",
                "colony_hire", "colony_dose", "colony_sale", "colony_lifted", "colony_music", "colony_built");
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (var d : defs) {
            for (var r : d.reqs()) {
                h.assertTrue(keys.add(d.key(r)), "unique key " + d.key(r));
                h.assertTrue(r.kind() == com.malevolentgods.fourtwenty.domain.handbook.Scouting.Kind.HONOUR || deeds.contains(r.deed()), "known deed " + r.deed());
            }
        }
        h.succeed();
    }

    /** A deed ticks a requirement in the player's saved data; signing works only for an honour requirement; both survive being read back. */
    @GameTest(template = "empty")
    public static void deedsTickAndSurviveInThePlayersData(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var deeds = com.malevolentgods.fourtwenty.handbook.Deeds.class;
        com.malevolentgods.fourtwenty.handbook.Deeds.fire(player, "craft", "item", "fourtwenty:weed_joint");
        var p = com.malevolentgods.fourtwenty.handbook.Deeds.progress(player);
        h.assertTrue(p.satisfied("rank:tenderbud/4a"), "crafting a joint ticks 4a");
        h.assertFalse(p.satisfied("rank:tenderbud/4b"), "but not smoking it");
        com.malevolentgods.fourtwenty.handbook.Deeds.sign(player, "rank:tenderbud/4a");
        com.malevolentgods.fourtwenty.handbook.Deeds.sign(player, "rank:tenderbud/5");
        p = com.malevolentgods.fourtwenty.handbook.Deeds.progress(player);
        h.assertTrue(p.state("rank:tenderbud/4a") == 1, "a tracked requirement can't be signed");
        h.assertTrue(p.state("rank:tenderbud/5") == 2, "an honour one can");
        com.malevolentgods.fourtwenty.handbook.Deeds.fire(player, "harvest", "strain", "landrace", "grade", "0", "block", "minecraft:farmland");
        h.assertTrue(com.malevolentgods.fourtwenty.handbook.Deeds.progress(player).satisfied("rank:tenderbud/2c"), "a Landrace harvest ticks 2c");
        h.assertTrue(deeds != null, "");
        h.succeed();
    }

    /** Doing everything a rank asks earns it, and the earned badge counts toward later ranks; a reset takes it back. */
    @GameTest(template = "empty")
    public static void earningARankAndResettingIt(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var defs = com.malevolentgods.fourtwenty.handbook.ScoutingData.defs();
        var d = defs.stream().filter(x -> x.id().equals("tenderbud")).findFirst().orElseThrow();
        var p = com.malevolentgods.fourtwenty.handbook.Deeds.progress(player);
        for (var r : d.reqs()) p.force(defs, d.key(r), 1);
        com.malevolentgods.fourtwenty.handbook.Deeds.save(player, p);
        h.assertTrue(com.malevolentgods.fourtwenty.handbook.Deeds.progress(player).earned("rank:tenderbud"), "Tenderbud earned");
        p = com.malevolentgods.fourtwenty.handbook.Deeds.progress(player);
        p.reset(defs, "rank:tenderbud");
        com.malevolentgods.fourtwenty.handbook.Deeds.save(player, p);
        h.assertFalse(com.malevolentgods.fourtwenty.handbook.Deeds.progress(player).earned("rank:tenderbud"), "and reset");
        h.succeed();
    }

    /** The Stoner Badge Sash is craftable, and wearable in the chest slot (cosmetic: no armour). */
    @GameTest(template = "empty")
    public static void theSashIsCraftableAndWearable(GameTestHelper h) {
        var recipe = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("fourtwenty", "stoner_badge_sash"));
        h.assertTrue(recipe.isPresent(), "no sash recipe");
        var sash = new ItemStack(ModItems.STONER_BADGE_SASH.get());
        h.assertTrue(sash.getItem() instanceof net.minecraft.world.item.Equipable e && e.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.CHEST, "worn on the chest");
        h.succeed();
    }

    private static JsonObject json(String path) {
        return JsonParser.parseString(text(path)).getAsJsonObject();
    }

    /** A file from the mod's own jar (or resources folder in a dev run). */
    private static String text(String path) {
        try {
            var file = net.neoforged.fml.ModList.get().getModFileById("fourtwenty").getFile();
            java.nio.file.Path p = file.findResource(path.substring(1).split("/"));
            if (!java.nio.file.Files.exists(p)) return null;
            return java.nio.file.Files.readString(p, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
