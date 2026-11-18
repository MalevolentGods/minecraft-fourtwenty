package com.malevolentgods.fourtwenty.client.handbook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Where the reader left the Handbook, per world or server, on this machine (research R10). Missing → the cover. */
public final class Bookmark {
    private Bookmark() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fourtwenty-handbook.json");
    }

    private static String world() {
        var mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip;
        if (mc.getSingleplayerServer() != null) return "world:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        return "none";
    }

    private static JsonObject read() {
        try {
            Path f = file();
            if (Files.exists(f)) return JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception ignored) {}
        return new JsonObject();
    }

    public static int spread() {
        JsonObject o = read();
        String key = world();
        return o.has(key) ? o.get(key).getAsInt() : 0;
    }

    public static void save(int spread) {
        try {
            JsonObject o = read();
            o.addProperty(world(), spread);
            Files.writeString(file(), o.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
}
