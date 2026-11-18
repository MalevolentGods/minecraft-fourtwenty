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
