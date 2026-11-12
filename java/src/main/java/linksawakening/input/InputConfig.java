package linksawakening.input;

import static linksawakening.input.KeyCodes.*;

import java.util.HashMap;
import java.util.Map;
import linksawakening.config.AppConfig;

/** Configurable engine key bindings with the legacy numeric key namespace. */
public final class InputConfig {

    private static final Map<String, Integer> KEY_NAME_TO_CODE = buildKeyTable();

    private final int menuOpenKey;
    private final int upKey;
    private final int downKey;
    private final int leftKey;
    private final int rightKey;
    private final int aKey;
    private final int bKey;
    private final int selectKey;
    private final int recordingKey;

    public InputConfig(int menuOpenKey, int upKey, int downKey, int leftKey, int rightKey, int aKey, int bKey) {
        this(menuOpenKey, upKey, downKey, leftKey, rightKey, aKey, bKey, KEY_TAB);
    }

    public InputConfig(
            int menuOpenKey, int upKey, int downKey, int leftKey, int rightKey, int aKey, int bKey, int selectKey) {
        this(menuOpenKey, upKey, downKey, leftKey, rightKey, aKey, bKey, selectKey, KEY_F9);
    }

    public InputConfig(
            int menuOpenKey,
            int upKey,
            int downKey,
            int leftKey,
            int rightKey,
            int aKey,
            int bKey,
            int selectKey,
            int recordingKey) {
        this.recordingKey = recordingKey;
        this.menuOpenKey = menuOpenKey;
        this.upKey = upKey;
        this.downKey = downKey;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
        this.aKey = aKey;
        this.bKey = bKey;
        this.selectKey = selectKey;
    }

    public int menuOpenKey() {
        return menuOpenKey;
    }

    public int upKey() {
        return upKey;
    }

    public int downKey() {
        return downKey;
    }

    public int leftKey() {
        return leftKey;
    }

    public int rightKey() {
        return rightKey;
    }

    public int aKey() {
        return aKey;
    }

    public int bKey() {
        return bKey;
    }

    public int recordingKey() {
        return recordingKey;
    }

    public int selectKey() {
        return selectKey;
    }

    public static InputConfig loadFromResources() {
        return AppConfig.loadFromResources().inputConfig();
    }

    public static InputConfig fromKeyNames(Map<String, String> entries) {
        int menuOpenKey = resolveKey(entries.getOrDefault("menuOpenKey", "ENTER"));
        int upKey = resolveKey(entries.getOrDefault("upKey", "UP"));
        int downKey = resolveKey(entries.getOrDefault("downKey", "DOWN"));
        int leftKey = resolveKey(entries.getOrDefault("leftKey", "LEFT"));
        int rightKey = resolveKey(entries.getOrDefault("rightKey", "RIGHT"));
        int aKey = resolveKey(entries.getOrDefault("aKey", "Z"));
        int bKey = resolveKey(entries.getOrDefault("bKey", "X"));
        int selectKey = resolveKey(entries.getOrDefault("selectKey", "TAB"));
        String recordingName = entries.getOrDefault("recordingKey", "F9").toUpperCase(java.util.Locale.ROOT);
        int recordingKey = KEY_NAME_TO_CODE.getOrDefault(recordingName, KEY_F9);
        if (!KEY_NAME_TO_CODE.containsKey(recordingName))
            System.err.println("Unknown recording key: " + recordingName + " (falling back to F9)");
        return new InputConfig(menuOpenKey, upKey, downKey, leftKey, rightKey, aKey, bKey, selectKey, recordingKey);
    }

    private static int resolveKey(String name) {
        Integer glfwKey = KEY_NAME_TO_CODE.get(name.toUpperCase());
        if (glfwKey == null) {
            System.err.println("Unknown key name in input config: " + name + " (falling back to ENTER)");
            return KEY_ENTER;
        }
        return glfwKey;
    }

    private static Map<String, Integer> buildKeyTable() {
        Map<String, Integer> table = new HashMap<>();
        table.put("NONE", -1);
        for (int i = 1; i <= 25; i++) table.put("F" + i, 289 + i);
        table.put("ENTER", KEY_ENTER);
        table.put("RETURN", KEY_ENTER);
        table.put("SPACE", KEY_SPACE);
        table.put("TAB", KEY_TAB);
        table.put("ESCAPE", KEY_ESCAPE);
        table.put("ESC", KEY_ESCAPE);
        table.put("BACKSPACE", KEY_BACKSPACE);
        table.put("LEFT_SHIFT", KEY_LEFT_SHIFT);
        table.put("RIGHT_SHIFT", KEY_RIGHT_SHIFT);
        table.put("LEFT_CONTROL", KEY_LEFT_CONTROL);
        table.put("RIGHT_CONTROL", KEY_RIGHT_CONTROL);
        table.put("LEFT_ALT", KEY_LEFT_ALT);
        table.put("RIGHT_ALT", KEY_RIGHT_ALT);
        table.put("UP", KEY_UP);
        table.put("DOWN", KEY_DOWN);
        table.put("LEFT", KEY_LEFT);
        table.put("RIGHT", KEY_RIGHT);
        for (char c = 'A'; c <= 'Z'; c++) {
            table.put(String.valueOf(c), KEY_A + (c - 'A'));
        }
        for (char c = '0'; c <= '9'; c++) {
            table.put(String.valueOf(c), KEY_0 + (c - '0'));
        }
        return table;
    }
}
