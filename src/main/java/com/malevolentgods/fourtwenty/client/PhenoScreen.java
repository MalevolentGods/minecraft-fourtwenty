package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.item.StrainItems;
import com.malevolentgods.fourtwenty.processing.PhenoBlockEntity;
import com.malevolentgods.fourtwenty.processing.PhenoMenu;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/** Every Pheno Chamber: two parents, the work arrow, the seeds, the BI gauge, and what the cross will make. */
public class PhenoScreen extends TechScreen<PhenoMenu> {
    private static final int GAUGE_X = 158, GAUGE_Y = 16, ARROW_X = 70, ARROW_Y = 30, STATUS_Y = 72;

    public PhenoScreen(PhenoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PhenoMenu.WIDTH;
        imageHeight = PhenoMenu.HEIGHT;
    }

    @Override protected int tier() { return menu.tier(); }

    @Override protected boolean overloaded() { return menu.overloaded(); }

    @Override protected void details(GuiGraphics g) {
        arrow(g, leftPos + ARROW_X, topPos + ARROW_Y, 1, menu.progress(), menu.total());
        gauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.stored(), menu.capacity(), 0, 0);
    }

    @Override protected String helpKey() { return "pheno"; }

    @Override protected SlotHint hint(int slot) {
        if (slot >= PhenoBlockEntity.SIZE) return null;
        return new SlotHint(new ItemStack(ModItems.WEED_SEEDS.get()),
                Component.translatable(slot < PhenoBlockEntity.PARENTS ? "gui.fourtwenty.slot.parent" : "gui.fourtwenty.slot.cross_out"));
    }

    @Override protected void tips(GuiGraphics g, int mouseX, int mouseY) {
        gaugeTip(g, mouseX, mouseY, GAUGE_X, GAUGE_Y, menu.stored(), menu.capacity(), Component.translatable("gui.fourtwenty.gauge.machine"));
        int percent = menu.total() <= 0 ? 0 : 100 * menu.progress() / menu.total();
        arrowTip(g, mouseX, mouseY, ARROW_X, ARROW_Y, Component.translatable(menu.paused() ? "gui.fourtwenty.arrow.paused" : "gui.fourtwenty.arrow.working", percent));
    }

    private static String chamber(int tier) {
        return tier == BIRules.Tier.RIP.ordinal() ? "block.fourtwenty.pheno_lab" : "block.fourtwenty.pheno_cathedral";
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        if (menu.needs() >= 0) {
            g.drawString(font, Component.translatable("gui.fourtwenty.pheno.needs", Component.translatable(chamber(menu.needs()))), 8, STATUS_Y, WARN, false);
        } else if (menu.makes() >= 0) {
            g.drawString(font, Component.translatable("gui.fourtwenty.pheno.makes", StrainItems.strainName(Strain.values()[menu.makes()])), 8, STATUS_Y, TEXT, false);
        } else if (menu.free()) {
            g.drawString(font, Component.translatable("gui.fourtwenty.machine.free"), 8, STATUS_Y, GREEN, false);
        } else if (menu.paused() && !menu.overloaded()) {
            g.drawString(font, Component.translatable("gui.fourtwenty.machine.waiting"), 8, STATUS_Y, WARN, false);
        }
    }
}
