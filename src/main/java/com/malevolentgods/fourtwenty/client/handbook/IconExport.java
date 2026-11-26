package com.malevolentgods.fourtwenty.client.handbook;

import com.malevolentgods.fourtwenty.domain.handbook.Element;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/**
 * Renders the pictures of every item the Handbook shows (specs/016-weedcraft-handbook T062, research R6), exactly as the game draws
 * them in an inventory, for the PDF edition: a client command for the book's authors, {@code /handbook_icons}. It walks the loaded
 * book (entry and chapter icons, item lines, and every recipe's ingredients and result from this world's recipes) and saves each
 * at 128x128 to {@code handbook-icons/<namespace>_<id>.png} in the game folder. Copy them to tools/art/handbook_icons/;
 * tools/build_handbook_pdf.py uses them over its flat-texture fallback.
 */
public final class IconExport {
    private IconExport() {}

    private static final int SIZE = 128;

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("handbook_icons").executes(ctx -> {
            run(ctx.getSource());
            return 1;
        }));
    }

    private static void run(CommandSourceStack source) {
        Path out = FMLPaths.GAMEDIR.get().resolve("handbook-icons");
        try {
            Files.createDirectories(out);
            Set<ResourceLocation> ids = wanted();
            int n = 0;
            for (ResourceLocation id : ids) {
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item == Items.AIR) continue;
                render(new ItemStack(item), out.resolve(id.getNamespace() + "_" + id.getPath() + ".png"));
                n++;
            }
            source.sendSystemMessage(Component.literal("Rendered " + n + " item pictures to " + out));
        } catch (Exception ex) {
            source.sendSystemMessage(Component.literal("Icon export failed: " + ex));
        }
    }

    /** Every item the loaded book shows anywhere. */
    private static Set<ResourceLocation> wanted() {
        Set<ResourceLocation> ids = new TreeSet<>();
        var book = BookData.book();
        for (var c : book.chapters()) add(ids, c.icon());
        var level = Minecraft.getInstance().level;
        for (var e : book.entries().values()) {
            add(ids, e.front().icon());
            for (Element el : e.elements()) {
                if (el instanceof Element.Item it) add(ids, it.id());
                if (el instanceof Element.Recipe r && level != null) {
                    ResourceLocation rl = ResourceLocation.tryParse(r.id());
                    if (rl == null) continue;
                    level.getRecipeManager().byKey(rl).ifPresent(holder -> {
                        for (Ingredient ing : holder.value().getIngredients()) {
                            for (ItemStack s : ing.getItems()) ids.add(BuiltInRegistries.ITEM.getKey(s.getItem()));
                        }
                        ids.add(BuiltInRegistries.ITEM.getKey(holder.value().getResultItem(level.registryAccess()).getItem()));
                    });
                }
            }
        }
        ids.remove(BuiltInRegistries.ITEM.getKey(Items.AIR));
        return ids;
    }

    private static void add(Set<ResourceLocation> ids, String id) {
        if (id == null || id.isEmpty()) return;
        ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
        if (rl != null) ids.add(rl);
    }

    /** One item, drawn the inventory way (GUI lighting, its own display transform) at 8x into an off-screen picture. */
    private static void render(ItemStack stack, Path file) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        TextureTarget target = new TextureTarget(SIZE, SIZE, true, Minecraft.ON_OSX);
        target.setClearColor(0, 0, 0, 0);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, SIZE, SIZE, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        mv.translate(0, 0, -11000);
        RenderSystem.applyModelViewMatrix();

        GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        g.pose().scale(SIZE / 16f, SIZE / 16f, 1f);
        g.renderItem(stack, 0, 0);
        g.flush();

        NativeImage img = new NativeImage(SIZE, SIZE, false);
        RenderSystem.bindTexture(target.getColorTextureId());
        img.downloadTexture(0, false);
        img.flipY();
        img.writeToFile(file);
        img.close();

        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        target.destroyBuffers();
        mc.getMainRenderTarget().bindWrite(true);
    }
}
