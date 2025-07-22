package com.malevolentgods.fourtwenty.screen;

import com.malevolentgods.fourtwenty.menu.DrugCraftingBenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

public class DrugCraftingBenchScreen extends AbstractContainerScreen<DrugCraftingBenchMenu> {
    // Using vanilla crafting table texture as placeholder for now
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    public DrugCraftingBenchScreen(DrugCraftingBenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // Position title and labels as per vanilla crafting table
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render the title and inventory labels
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        
        // Render crafting progress bar if applicable
        int progress = this.menu.getCraftingProgress();
        int maxTime = this.menu.getMaxCraftingTime();
        
        if (progress > 0 && maxTime > 0) {
            // Draw progress bar (similar to furnace)
            int progressWidth = (progress * 24) / maxTime;
            guiGraphics.drawString(this.font, "Crafting...", 70, 50, 0x55FF55, false);
            
            // You could add a progress bar here later
            // For now, just show text feedback
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // Render crafting progress arrow/indicator
        int progress = this.menu.getCraftingProgress();
        int maxTime = this.menu.getMaxCraftingTime();
        
        if (progress > 0 && maxTime > 0) {
            // Draw a simple progress arrow (using vanilla furnace arrow coordinates as reference)
            int progressHeight = (progress * 17) / maxTime;
            // This would draw a progress indicator - for now using the vanilla arrow area
            // You could customize this with custom textures later
        }
    }
}
