import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.neoforged.neoforgespi.language.MavenVersionAdapter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/** Audits the frozen inventory with the exact loader's Maven version-range parser. */
public final class VerifyCompatibility {
    private static boolean accepts(String range, String version) {
        return MavenVersionAdapter.createFromVersionSpec(range).containsVersion(new DefaultArtifactVersion(version));
    }

    private static void nested(JsonObject artifact, Map<String, List<JsonObject>> groups) {
        for (JsonElement element : artifact.getAsJsonArray("nested")) {
            JsonObject node = element.getAsJsonObject();
            JsonObject id = node.getAsJsonObject("declaration").getAsJsonObject("identifier");
            String key = id.get("group").getAsString() + ":" + id.get("artifact").getAsString();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(node);
            nested(node.getAsJsonObject("artifact"), groups);
        }
    }

    public static void main(String[] args) throws Exception {
        JsonObject lock = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        // Standalone audit: supply the same launch identity before the loader's support matrix initializes.
        var versionField = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("versionInfo");
        versionField.setAccessible(true);
        versionField.set(null, new net.neoforged.fml.loading.VersionInfo("21.1.218", "4.0.42", "1.21.1", "20240808.144430"));
        List<JsonObject> artifacts = new ArrayList<>();
        lock.getAsJsonArray(lock.has("artifacts") ? "artifacts" : "mods").forEach(a -> artifacts.add(a.getAsJsonObject()));
        Map<String, List<JsonObject>> groups = new TreeMap<>();
        for (JsonObject artifact : artifacts) nested(artifact, groups);
        List<String> errors = new ArrayList<>();
        for (var group : groups.entrySet()) {
            JsonObject selected = group.getValue().stream().filter(candidate -> {
                String version = candidate.getAsJsonObject("declaration").getAsJsonObject("version").get("artifactVersion").getAsString();
                return group.getValue().stream().allMatch(requirement -> accepts(
                    requirement.getAsJsonObject("declaration").getAsJsonObject("version").get("range").getAsString(), version));
            }).max(Comparator.comparing(c -> new DefaultArtifactVersion(c.getAsJsonObject("declaration")
                    .getAsJsonObject("version").get("artifactVersion").getAsString()))).orElse(null);
            if (selected == null) errors.add("No nested version satisfies all declarations: " + group.getKey());
            else artifacts.add(selected.getAsJsonObject("artifact"));
        }
        Map<String, String> versions = new TreeMap<>();
        versions.put("minecraft", lock.get("minecraft").getAsString());
        versions.put("neoforge", lock.get("neoforge").getAsString());
        for (JsonObject artifact : artifacts) {
            for (JsonElement element : artifact.getAsJsonArray("mods")) {
                JsonObject mod = element.getAsJsonObject();
                String id = mod.get("id").getAsString();
                if (versions.putIfAbsent(id, mod.get("version").getAsString()) != null) errors.add("Duplicate resolved mod ID: " + id);
            }
            if (artifact.has("loaderVersion") && !artifact.get("loaderVersion").isJsonNull()
                    && "javafml".equals(artifact.get("loader").getAsString())
                    && !accepts(artifact.get("loaderVersion").getAsString(), "4.0.42")) {
                errors.add("Unsupported loader in " + artifact.get("file"));
            }
        }
        int checked = 0;
        for (JsonObject artifact : artifacts) {
            for (var owner : artifact.getAsJsonObject("dependencies").entrySet()) {
                for (JsonElement element : owner.getValue().getAsJsonArray()) {
                    JsonObject dependency = element.getAsJsonObject();
                    String id = dependency.get("modId").getAsString();
                    String type = dependency.has("type") ? dependency.get("type").getAsString() :
                        dependency.has("mandatory") && dependency.get("mandatory").getAsBoolean() ? "required" : "optional";
                    String range = dependency.has("versionRange") ? dependency.get("versionRange").getAsString() : "[0,)";
                    String installed = versions.get(id);
                    if (installed == null && type.equals("required")) errors.add(owner.getKey() + " requires missing " + id);
                    if (installed != null) {
                        checked++;
                        boolean direct = accepts(range, installed);
                        boolean matches = net.neoforged.fml.loading.VersionSupportMatrix.testVersionSupportMatrix(
                            MavenVersionAdapter.createFromVersionSpec(range), id, "mod",
                            (key, requirement) -> requirement.containsVersion(new DefaultArtifactVersion(versions.get(key))));
                        if (!direct && matches) System.out.println("Loader compatibility override: " + owner.getKey() + " -> " + id + " " + range);
                        if ((type.equals("required") || type.equals("optional")) && !matches)
                            errors.add(owner.getKey() + " requires " + id + " " + range + ", found " + installed);
                        if (type.equals("incompatible") && matches) errors.add(owner.getKey() + " incompatible with " + id);
                    }
                }
            }
        }
        if (!accepts("[21.1.218,21.2)", "21.1.218") || accepts("[21.1.218,21.2)", "21.2.0") ||
                accepts("[1.21.1]", "1.21") || !accepts("[1.1.1343-1.21.1-snapshot]", "1.1.1343-1.21.1-snapshot"))
            throw new IllegalStateException("Version boundary assertions failed");
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("\n", errors));
        System.out.printf("PASS: %d resolved mod/platform IDs, %d dependency checks, %d nested artifact groups; loader range boundaries passed.%n",
                versions.size(), checked, groups.size());
    }
}
