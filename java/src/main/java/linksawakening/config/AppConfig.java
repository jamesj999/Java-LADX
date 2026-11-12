package linksawakening.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import linksawakening.input.InputConfig;

/** Launch preferences and key bindings loaded from a strict JSON object. */
public final class AppConfig {

    private static final String RESOURCE_PATH = "config/config.json";

    /** Supported configured starting map categories. */
    public enum StartLocationType {
        OVERWORLD
    }

    /** Selects the startup location policy. */
    public enum StartMode {
        CONFIGURED_LOCATION,
        NORMAL
    }

    /** Initial player inventory policy. */
    public enum ItemProfile {
        DEBUG_ALL_ITEMS,
        NEW_GAME
    }

    /** Configured map category and room identifier. */
    public record StartLocation(StartLocationType type, int roomId) {}

    private final boolean showTitleScreen;
    private final boolean playIntroStory;
    private final boolean debugEnabled;
    private final StartMode startMode;
    private final StartLocation startLocation;
    private final ItemProfile itemProfile;
    private final InputConfig inputConfig;

    private AppConfig(
            boolean showTitleScreen,
            boolean playIntroStory,
            boolean debugEnabled,
            StartMode startMode,
            StartLocation startLocation,
            ItemProfile itemProfile,
            InputConfig inputConfig) {
        this.showTitleScreen = showTitleScreen;
        this.playIntroStory = playIntroStory;
        this.debugEnabled = debugEnabled;
        this.startMode = startMode;
        this.startLocation = startLocation;
        this.itemProfile = itemProfile;
        this.inputConfig = inputConfig;
    }

    public boolean showTitleScreen() {
        return showTitleScreen;
    }

    public boolean playIntroStory() {
        return playIntroStory;
    }

    public boolean debugEnabled() {
        return debugEnabled;
    }

    public StartMode startMode() {
        return startMode;
    }

    public StartLocation startLocation() {
        return startLocation;
    }

    public ItemProfile itemProfile() {
        return itemProfile;
    }

    public InputConfig inputConfig() {
        return inputConfig;
    }

    public static AppConfig defaults() {
        return new AppConfig(
                true,
                false,
                true,
                StartMode.CONFIGURED_LOCATION,
                new StartLocation(StartLocationType.OVERWORLD, 0x92),
                ItemProfile.DEBUG_ALL_ITEMS,
                InputConfig.fromKeyNames(Map.of()));
    }

    public static AppConfig loadFromResources() {
        return loadFromResources(AppConfig.class.getClassLoader());
    }

    static AppConfig loadFromResources(ClassLoader loader) {
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                System.err.println("App config not found at " + RESOURCE_PATH + "; using defaults");
                return defaults();
            }
            return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Failed to read app config: " + e.getMessage());
            return defaults();
        }
    }

    public static AppConfig parse(String content) {
        AppConfig defaults = defaults();
        Map<String, Object> entries = object(MiniJson.parse(content), "config");
        Map<String, Boolean> booleans = booleanEntries(entries);

        boolean showTitleScreen = booleans.getOrDefault("showTitleScreen", defaults.showTitleScreen);
        boolean playIntroStory = booleans.getOrDefault("playIntroStory", defaults.playIntroStory);
        boolean debugEnabled = booleans.getOrDefault("debugEnabled", defaults.debugEnabled);
        Map<String, String> stringEntries = stringEntries(entries);

        StartMode startMode = parseStartMode(stringEntries.get("startMode"), defaults.startMode);
        StartLocation startLocation = parseStartLocation(entries, defaults.startLocation);
        ItemProfile itemProfile = parseItemProfile(stringEntries.get("itemProfile"));
        InputConfig inputConfig = InputConfig.fromKeyNames(stringEntries(section(entries, "controls")));

        return new AppConfig(
                showTitleScreen, playIntroStory, debugEnabled, startMode, startLocation, itemProfile, inputConfig);
    }

    private static StartMode parseStartMode(String value, StartMode fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return StartMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown start mode: " + value + " (falling back to " + fallback + ")");
            return fallback;
        }
    }

    private static StartLocation parseStartLocation(Map<String, Object> content, StartLocation fallback) {
        Map<String, String> entries = stringEntries(section(content, "startLocation"));
        StartLocationType type = parseStartLocationType(entries.get("type"), fallback.type);
        int roomId = parseRoomId(entries.get("roomId"), fallback.roomId);
        return new StartLocation(type, roomId);
    }

    private static StartLocationType parseStartLocationType(String value, StartLocationType fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return StartLocationType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown start location type: " + value + " (falling back to " + fallback + ")");
            return fallback;
        }
    }

    private static int parseRoomId(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("0x")) {
                return Integer.parseInt(normalized.substring(2), 16) & 0xFF;
            }
            return Integer.parseInt(normalized) & 0xFF;
        } catch (NumberFormatException e) {
            System.err.println(
                    "Invalid start room id: " + value + " (falling back to " + String.format("0x%02X", fallback) + ")");
            return fallback;
        }
    }

    private static ItemProfile parseItemProfile(String value) {
        if (value == null) {
            return ItemProfile.DEBUG_ALL_ITEMS;
        }
        try {
            return ItemProfile.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown item profile: " + value + " (falling back to DEBUG_ALL_ITEMS)");
            return ItemProfile.DEBUG_ALL_ITEMS;
        }
    }

    private static Map<String, Object> object(Object value, String name) {
        if (!(value instanceof Map<?, ?> map))
            throw new IllegalArgumentException("Config " + name + " must be an object");
        Map<String, Object> result = new HashMap<>();
        map.forEach((key, entry) -> result.put((String) key, entry));
        return result;
    }

    private static Map<String, Object> section(Map<String, Object> entries, String name) {
        return entries.containsKey(name) ? object(entries.get(name), name) : Map.of();
    }

    private static Map<String, String> stringEntries(Map<String, Object> entries) {
        Map<String, String> result = new HashMap<>();
        entries.forEach((key, value) -> {
            if (value instanceof String text) result.put(key, text);
        });
        return result;
    }

    private static Map<String, Boolean> booleanEntries(Map<String, Object> entries) {
        Map<String, Boolean> result = new HashMap<>();
        for (String key : new String[] {"showTitleScreen", "playIntroStory", "debugEnabled"}) {
            if (!entries.containsKey(key)) continue;
            if (!(entries.get(key) instanceof Boolean value))
                throw new IllegalArgumentException("Config " + key + " must be a boolean");
            result.put(key, value);
        }
        return result;
    }
}
