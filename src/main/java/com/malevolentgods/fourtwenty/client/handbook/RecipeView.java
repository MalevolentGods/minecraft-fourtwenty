package com.malevolentgods.fourtwenty.client.handbook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;

/**
 * A recipe drawn from the client's own recipe list (synced from the server, so a datapack's change shows: research R3):
 * a 3x3 grid for crafting, input and fuel flame for smelting, a row of ingredients for anything else; an arrow and the
 * result. Ingredients with several items cycle once a second.
 */
public final class RecipeView {
    private RecipeView() {}

    public static final int SLOT = 18, WIDTH = SLOT * 3 + 26 + SLOT, HEIGHT = SLOT * 3 + 4;
    private static final int SLOT_BG = 0x30624A2A, SLOT_EDGE = 0x60624A2A;

    public static void draw(GuiGraphics g, Font font, String id, int x, int y, List<PageRenderer.Hover> hovers) {
        var level = Minecraft.getInstance().level;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        var holder = level == null || rl == null ? java.util.Optional.<net.minecraft.world.item.crafting.RecipeHolder<?>>empty()
                : level.getRecipeManager().byKey(rl);
        if (holder.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("handbook.fourtwenty.recipe_changed"), x - 10, y + 4, WIDTH + 20, PageRenderer.FAINT);
            return;
        }
        Recipe<?> recipe = holder.get().value();
        List<Ingredient> ings = recipe.getIngredients();
        int w = 3, h = 3;
        if (recipe instanceof ShapedRecipe shaped) { w = shaped.getWidth(); h = shaped.getHeight(); }
        long tick = System.currentTimeMillis() / 1000;
        if (recipe instanceof AbstractCookingRecipe) {
            slot(g, x + SLOT, y, ings.isEmpty() ? null : ings.get(0), tick, hovers);
            int fx = x + SLOT + 4, fy = y + SLOT + 3;      // a little flame, as a furnace shows
            for (int i = 0; i < 9; i++) {
                int half = Math.min(i, 8 - i) / 2 + 1;
                g.fill(fx + 4 - half, fy + i, fx + 5 + half, fy + i + 1, i < 4 ? 0xFFE0A030 : 0xFFC05020);
            }
        } else {
            for (int i = 0; i < 9; i++) {
                int col = i % 3, row = i / 3;
                Ingredient ing = null;
                if (recipe instanceof ShapedRecipe) {
                    if (col < w && row < h) ing = ings.get(row * w + col);
                } else if (i < ings.size()) ing = ings.get(i);
                slot(g, x + col * SLOT, y + row * SLOT, ing, tick, hovers);
            }
        }
        // the arrow and the result
        int ax = x + SLOT * 3 + 4, ay = y + SLOT + 4;
        g.fill(ax, ay + 3, ax + 13, ay + 6, PageRenderer.FAINT);
        for (int i = 0; i < 5; i++) g.fill(ax + 13 + i, ay + i - 0, ax + 14 + i, ay + 9 - i, PageRenderer.FAINT);
        ItemStack result = recipe.getResultItem(level.registryAccess());
        int rx = x + SLOT * 3 + 26, ry = y + SLOT;
        g.fill(rx - 1, ry - 1, rx + SLOT - 1, ry + SLOT - 1, SLOT_BG);
        g.renderOutline(rx - 1, ry - 1, SLOT, SLOT, SLOT_EDGE);
        g.renderItem(result, rx, ry);
        g.renderItemDecorations(font, result, rx, ry);
        hovers.add(new PageRenderer.Hover(rx, ry, 16, 16, result));
    }

    private static void slot(GuiGraphics g, int x, int y, Ingredient ing, long tick, List<PageRenderer.Hover> hovers) {
        g.fill(x, y, x + SLOT - 1, y + SLOT - 1, SLOT_BG);
        g.renderOutline(x, y, SLOT - 1, SLOT - 1, SLOT_EDGE);
        if (ing == null || ing.isEmpty()) return;
        ItemStack[] items = ing.getItems();
        if (items.length == 0) return;
        ItemStack stack = items[(int) (tick % items.length)];
        g.renderItem(stack, x + 1, y + 1);
        hovers.add(new PageRenderer.Hover(x + 1, y + 1, 16, 16, stack));
    }
}
