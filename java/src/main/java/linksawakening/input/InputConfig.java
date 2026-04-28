package linksawakening.input;

import linksawakening.config.AppConfig;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public final class InputConfig {

    private static final Map<String, Integer> KEY_NAME_TO_GLFW = buildKeyTable();

    private final int menuOpenKey;
    private final int upKey;
    private final int downKey;
    private final int leftKey;
    private final int rightKey;
    private final int aKey;
    private final int bKey;

    public InputConfig(int menuOpenKey, int upKey, int downKey, int leftKey, int rightKey,
                       int aKey, int bKey) {
        this.menuOpenKey = menuOpenKey;
        this.upKey = upKey;
        this.downKey = downKey;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
        this.aKey = aKey;
        this.bKey = bKey;
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
        return new InputConfig(menuOpenKey, upKey, downKey, leftKey, rightKey, aKey, bKey);
    }

    private static int resolveKey(String name) {
        Integer glfwKey = KEY_NAME_TO_GLFW.get(name.toUpperCase());
        if (glfwKey == null) {
            System.err.println("Unknown key name in input config: " + name + " (falling back to ENTER)");
            return GLFW_KEY_ENTER;
        }
        return glfwKey;
    }

    private static Map<String, Integer> buildKeyTable() {
        Map<String, Integer> table = new HashMap<>();
        table.put("ENTER", GLFW_KEY_ENTER);
        table.put("RETURN", GLFW_KEY_ENTER);
        table.put("SPACE", GLFW_KEY_SPACE);
        table.put("TAB", GLFW_KEY_TAB);
        table.put("ESCAPE", GLFW_KEY_ESCAPE);
        table.put("ESC", GLFW_KEY_ESCAPE);
        table.put("BACKSPACE", GLFW_KEY_BACKSPACE);
        table.put("LEFT_SHIFT", GLFW_KEY_LEFT_SHIFT);
        table.put("RIGHT_SHIFT", GLFW_KEY_RIGHT_SHIFT);
        table.put("LEFT_CONTROL", GLFW_KEY_LEFT_CONTROL);
        table.put("RIGHT_CONTROL", GLFW_KEY_RIGHT_CONTROL);
        table.put("LEFT_ALT", GLFW_KEY_LEFT_ALT);
        table.put("RIGHT_ALT", GLFW_KEY_RIGHT_ALT);
        table.put("UP", GLFW_KEY_UP);
        table.put("DOWN", GLFW_KEY_DOWN);
        table.put("LEFT", GLFW_KEY_LEFT);
        table.put("RIGHT", GLFW_KEY_RIGHT);
        for (char c = 'A'; c <= 'Z'; c++) {
            table.put(String.valueOf(c), GLFW_KEY_A + (c - 'A'));
        }
        for (char c = '0'; c <= '9'; c++) {
            table.put(String.valueOf(c), GLFW_KEY_0 + (c - '0'));
        }
        return table;
    }
}
