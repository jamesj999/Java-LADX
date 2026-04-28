package linksawakening.config;

import linksawakening.input.InputConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppConfig {

    private static final String RESOURCE_PATH = "config/config.json";
    private static final Pattern STRING_ENTRY_PATTERN =
        Pattern.compile("\"([A-Za-z0-9_]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern BOOLEAN_ENTRY_PATTERN =
        Pattern.compile("\"([A-Za-z0-9_]+)\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OBJECT_PATTERN_TEMPLATE =
        Pattern.compile("\"%s\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);

    public enum StartLocationType {
        OVERWORLD
    }

    public enum StartMode {
        CONFIGURED_LOCATION,
        NORMAL
    }

    public enum ItemProfile {
        DEBUG_ALL_ITEMS,
        NEW_GAME
    }

    public record StartLocation(StartLocationType type, int roomId) {
    }

    private final boolean showTitleScreen;
    private final boolean playIntroStory;
    private final boolean debugEnabled;
    private final StartMode startMode;
    private final StartLocation startLocation;
    private final ItemProfile itemProfile;
    private final InputConfig inputConfig;

    private AppConfig(boolean showTitleScreen, boolean playIntroStory, boolean debugEnabled,
                      StartMode startMode,
                      StartLocation startLocation, ItemProfile itemProfile,
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
            InputConfig.fromKeyNames(Map.of())
        );
    }

    public static AppConfig loadFromResources() {
        try (InputStream stream = AppConfig.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
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
        Map<String, Boolean> booleans = readBooleanEntries(content);

        boolean showTitleScreen = booleans.getOrDefault("showTitleScreen", defaults.showTitleScreen);
        boolean playIntroStory = booleans.getOrDefault("playIntroStory", defaults.playIntroStory);
        boolean debugEnabled = booleans.getOrDefault("debugEnabled", defaults.debugEnabled);
        Map<String, String> stringEntries = readStringEntries(content);

        StartMode startMode = parseStartMode(stringEntries.get("startMode"), defaults.startMode);
        StartLocation startLocation = parseStartLocation(content, defaults.startLocation);
        ItemProfile itemProfile = parseItemProfile(stringEntries.get("itemProfile"));
        InputConfig inputConfig = InputConfig.fromKeyNames(readStringEntries(objectSection(content, "controls")));

        return new AppConfig(showTitleScreen, playIntroStory, debugEnabled, startMode,
                             startLocation, itemProfile, inputConfig);
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

    private static StartLocation parseStartLocation(String content, StartLocation fallback) {
        Map<String, String> entries = readStringEntries(objectSection(content, "startLocation"));
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
            System.err.println("Invalid start room id: " + value + " (falling back to "
                + String.format("0x%02X", fallback) + ")");
            return fallback;
        }
    }

    private static ItemProfile parseItemProfile(String value) {
        if (value == null) {
            return ItemProfile.DEBUG_ALL_ITEMS;
        }
        try {
            ItemProfile requested = ItemProfile.valueOf(value.toUpperCase(Locale.ROOT));
            if (requested == ItemProfile.NEW_GAME) {
                System.err.println("NEW_GAME item profile is not implemented yet; using DEBUG_ALL_ITEMS");
                return ItemProfile.DEBUG_ALL_ITEMS;
            }
            return requested;
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown item profile: " + value + " (falling back to DEBUG_ALL_ITEMS)");
            return ItemProfile.DEBUG_ALL_ITEMS;
        }
    }

    private static String objectSection(String content, String name) {
        Pattern pattern = Pattern.compile(String.format(OBJECT_PATTERN_TEMPLATE.pattern(), Pattern.quote(name)),
                                          Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static Map<String, String> readStringEntries(String content) {
        Map<String, String> entries = new HashMap<>();
        Matcher matcher = STRING_ENTRY_PATTERN.matcher(content);
        while (matcher.find()) {
            entries.put(matcher.group(1), matcher.group(2));
        }
        return entries;
    }

    private static Map<String, Boolean> readBooleanEntries(String content) {
        Map<String, Boolean> entries = new HashMap<>();
        Matcher matcher = BOOLEAN_ENTRY_PATTERN.matcher(content);
        while (matcher.find()) {
            entries.put(matcher.group(1), Boolean.parseBoolean(matcher.group(2)));
        }
        return entries;
    }
}
