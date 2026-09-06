package io.github.drcas.gentlemobs.neoforge;

import com.google.gson.*;
import io.github.drcas.gentlemobs.GentleMode;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Server-local config; validate a complete candidate before replacing the live settings. */
public final class GentleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public record Settings(GentleMode mode, double distance, double speed, int duration,
                           int neutralTimeout, Map<String, GentleMode> overrides,
                           boolean netherStar, boolean dragonsBreath) {
        public Settings { overrides = Map.copyOf(overrides); }
        public static Settings defaults() {
            return new Settings(GentleMode.PASSIVE, 12, 1.3, 60, 600, Map.of(), true, true);
        }
    }
    private final Path path;
    private volatile Settings settings = Settings.defaults();
    private int revision;
    public GentleConfig(Path path) { this.path = path; }
    public Settings get() { return settings; }
    public int revision() { return revision; }
    public synchronized void load() throws IOException {
        if (!Files.exists(path)) { save(settings); return; }
        try {
            Settings candidate = parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            settings = candidate;
            revision++;
        } catch (RuntimeException ex) {
            throw new IOException("Invalid " + path + ": " + ex.getMessage(), ex);
        }
    }
    public synchronized void setMode(GentleMode mode) throws IOException {
        Settings s = settings;
        save(new Settings(mode, s.distance, s.speed, s.duration, s.neutralTimeout, s.overrides, s.netherStar, s.dragonsBreath));
    }
    public synchronized void override(String id, GentleMode mode) throws IOException {
        Settings s = settings;
        Map<String, GentleMode> overrides = new TreeMap<>(s.overrides);
        if (mode == null) overrides.remove(id); else overrides.put(id, mode);
        save(new Settings(s.mode, s.distance, s.speed, s.duration, s.neutralTimeout, overrides, s.netherStar, s.dragonsBreath));
    }
    private void save(Settings s) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("mode", s.mode.name());
        JsonObject flee = new JsonObject();
        flee.addProperty("distance", s.distance); flee.addProperty("speed", s.speed); flee.addProperty("duration-ticks", s.duration);
        root.add("flee", flee);
        root.addProperty("neutral-timeout-ticks", s.neutralTimeout);
        JsonObject overrides = new JsonObject();
        new TreeMap<>(s.overrides).forEach((id, mode) -> overrides.addProperty(id, mode.name()));
        root.add("mob-overrides", overrides);
        JsonObject recipes = new JsonObject();
        JsonObject star = new JsonObject(); star.addProperty("enabled", s.netherStar);
        JsonObject breath = new JsonObject(); breath.addProperty("enabled", s.dragonsBreath);
        recipes.add("nether-star", star); recipes.add("dragons-breath", breath); root.add("recipes", recipes);
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), "gentlemobs-", ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(root) + System.lineSeparator());
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
        settings = s;
        revision++;
    }
    static Settings parse(JsonObject root) {
        Settings d = Settings.defaults();
        GentleMode mode = parseMode(string(root, "mode", d.mode.name()));
        JsonObject flee = object(root, "flee");
        double distance = number(flee, "distance", d.distance, 1, 64);
        double speed = number(flee, "speed", d.speed, 0.1, 4);
        int duration = integer(flee, "duration-ticks", d.duration, 1, 12000);
        int timeout = integer(root, "neutral-timeout-ticks", d.neutralTimeout, 20, 72000);
        Map<String, GentleMode> overrides = new TreeMap<>();
        object(root, "mob-overrides").entrySet().forEach(e -> {
            String id = normalizeId(e.getKey());
            if (overrides.put(id, parseMode(e.getValue().getAsString())) != null)
                throw new IllegalArgumentException("Duplicate entity override: " + id);
        });
        JsonObject recipes = object(root, "recipes");
        return new Settings(mode, distance, speed, duration, timeout, overrides,
            bool(object(recipes, "nether-star"), "enabled", true), bool(object(recipes, "dragons-breath"), "enabled", true));
    }
    public static String normalizeId(String value) {
        String id = value.toLowerCase(Locale.ROOT);
        if (!id.contains(":")) id = "minecraft:" + id;
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("Invalid entity ID: " + value);
        return id;
    }
    public static GentleMode parseMode(String value) { return GentleMode.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
    private static JsonObject object(JsonObject o, String key) { return o.has(key) ? o.getAsJsonObject(key) : new JsonObject(); }
    private static String string(JsonObject o, String key, String fallback) { return o.has(key) ? o.get(key).getAsString() : fallback; }
    private static boolean bool(JsonObject o, String key, boolean fallback) {
        if (!o.has(key)) return fallback;
        if (!o.get(key).isJsonPrimitive() || !o.getAsJsonPrimitive(key).isBoolean()) throw new IllegalArgumentException(key + " must be boolean");
        return o.get(key).getAsBoolean();
    }
    private static double number(JsonObject o, String key, double fallback, double min, double max) {
        double value = o.has(key) ? o.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
        return value;
    }
    private static int integer(JsonObject o, String key, int fallback, int min, int max) {
        double value = number(o, key, fallback, min, max);
        if (value != Math.rint(value)) throw new IllegalArgumentException(key + " must be an integer");
        return (int)value;
    }
}
