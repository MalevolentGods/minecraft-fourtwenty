package com.malevolentgods.fourtwenty.compat.journeymap;

import com.malevolentgods.fourtwenty.util.HotboxNetwork;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.common.event.ClientEventRegistry;
import net.minecraft.core.BlockPos;

/** Every holding Hotbox's area on the map, a green polygon as MineColonies draws a colony. JourneyMap finds and loads this class itself. */
@JourneyMapPlugin(apiVersion = "2.0.0")
public class HotboxJourneyMapPlugin implements IClientPlugin {
    private static final String MOD = "fourtwenty";
    private IClientAPI api;

    @Override public void initialize(IClientAPI api) {
        this.api = api;
        HotboxNetwork.onChange(this::refresh);
        ClientEventRegistry.MAPPING_EVENT.subscribe(MOD, event -> {
            if (event.getStage() == MappingEvent.Stage.MAPPING_STARTED) refresh();
        });
    }

    @Override public String getModId() {
        return MOD;
    }

    private void refresh() {
        api.removeAll(MOD);
        for (HotboxNetwork.Box box : HotboxNetwork.boxes()) {
            int[] a = box.area();
            MapPolygon area = new MapPolygon(new BlockPos(a[0], 64, a[1]), new BlockPos(a[2], 64, a[1]), new BlockPos(a[2], 64, a[3]), new BlockPos(a[0], 64, a[3]));
            ShapeProperties shape = new ShapeProperties().setFillColor(0x59FF33).setFillOpacity(0.22f).setStrokeColor(0x59FF33).setStrokeOpacity(0.9f).setStrokeWidth(2f);
            try {
                api.show(new PolygonOverlay(MOD, box.level(), shape, area));
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().warn("Could not show a Hotbox on JourneyMap", e);
            }
        }
    }
}
