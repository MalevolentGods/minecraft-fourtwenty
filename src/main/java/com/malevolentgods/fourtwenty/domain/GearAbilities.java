package com.malevolentgods.fourtwenty.domain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.malevolentgods.fourtwenty.domain.GearRules.Piece;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The gear's ability list, read from {@code data/fourtwenty/gear/abilities.json} in the mod's own resources (the same file on client
 * and server): which colour unlocks which ability on which piece, and what it costs.
 */
public final class GearAbilities {
    private GearAbilities() {}

    public record Ability(Piece piece, String colour, String id, int cost, boolean perTick) {}

    public static final String RESOURCE = "/data/fourtwenty/gear/abilities.json";

    public static final List<Ability> ALL = load();

    private static List<Ability> load() {
        var in = GearAbilities.class.getResourceAsStream(RESOURCE);
        Objects.requireNonNull(in, RESOURCE);
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Couldn't read " + RESOURCE, e);
        }
    }

    public static List<Ability> parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        var out = new ArrayList<Ability>();
        for (var e : root.getAsJsonArray("abilities")) {
            var o = e.getAsJsonObject();
            out.add(new Ability(Piece.valueOf(o.get("piece").getAsString().toUpperCase(Locale.ROOT)), o.get("colour").getAsString(),
                    o.get("ability").getAsString(), o.get("cost").getAsInt(), "tick".equals(o.get("per").getAsString())));
        }
        return List.copyOf(out);
    }

    /** The ability a colour unlocks on a piece, if any. */
    public static Optional<Ability> of(Piece piece, String colour) {
        return ALL.stream().filter(a -> a.piece() == piece && a.colour().equals(colour)).findFirst();
    }

    /** An ability by id on a piece. */
    public static Optional<Ability> byId(Piece piece, String id) {
        return ALL.stream().filter(a -> a.piece() == piece && a.id().equals(id)).findFirst();
    }

    /** Whether a terpene of this colour may go in this piece: it has to be of the piece's hue family. */
    public static boolean fits(Piece piece, String colour) {
        return piece.family != null && HueFamilies.of(colour).map(f -> f == piece.family).orElse(false);
    }
}
