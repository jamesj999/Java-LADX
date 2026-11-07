# Runtime Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a single runtime `config.json` for controls, title/story startup flags, debug gating, start location, and item profile.

**Architecture:** Add `linksawakening.config.AppConfig` as the typed runtime config loader and keep `InputConfig` as the input-facing value object. `Main` reads `AppConfig` once during startup, creates `InputConfig` from the nested controls block, gates F1 with `debugEnabled`, and uses the configured overworld room for direct spawn.

**Tech Stack:** Java 21, JUnit 5, Gradle, existing regex-based lightweight JSON parsing style.

---

## File Structure

- Create `java/src/main/java/linksawakening/config/AppConfig.java`: typed config model, defaults, resource loading, lightweight JSON parsing.
- Create `java/src/test/java/linksawakening/config/AppConfigTest.java`: config parsing/default behavior tests.
- Modify `java/src/main/java/linksawakening/input/InputConfig.java`: add a factory for nested control key names while preserving existing key resolution defaults.
- Modify `java/src/main/java/linksawakening/Main.java`: load `AppConfig`, wire controls, startup flags, start room, and debug gate.
- Create `java/src/main/resources/config/config.json`: new unified default config.
- Delete `java/src/main/resources/config/input.json`: replaced by `config.json`.

## Task 1: Config Model And Tests

**Files:**
- Create: `java/src/test/java/linksawakening/config/AppConfigTest.java`
- Create: `java/src/main/java/linksawakening/config/AppConfig.java`
- Modify: `java/src/main/java/linksawakening/input/InputConfig.java`

- [ ] **Step 1: Write failing tests**

```java
package linksawakening.config;

import linksawakening.input.InputConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

final class AppConfigTest {
    @Test
    void defaultsPreserveCurrentStartupBehavior() {
        AppConfig config = AppConfig.defaults();

        assertTrue(config.showTitleScreen());
        assertFalse(config.playIntroStory());
        assertTrue(config.debugEnabled());
        assertEquals(AppConfig.StartLocationType.OVERWORLD, config.startLocation().type());
        assertEquals(0x92, config.startLocation().roomId());
        assertEquals(AppConfig.ItemProfile.DEBUG_ALL_ITEMS, config.itemProfile());
    }

    @Test
    void parsesBooleansStartLocationItemProfileAndControls() {
        AppConfig config = AppConfig.parse("""
            {
              "showTitleScreen": false,
              "playIntroStory": true,
              "debugEnabled": false,
              "startLocation": { "type": "OVERWORLD", "roomId": "0xA1" },
              "itemProfile": "NEW_GAME",
              "controls": {
                "menuOpenKey": "SPACE",
                "upKey": "W",
                "downKey": "S",
                "leftKey": "A",
                "rightKey": "D",
                "aKey": "J",
                "bKey": "K"
              }
            }
            """);

        assertFalse(config.showTitleScreen());
        assertTrue(config.playIntroStory());
        assertFalse(config.debugEnabled());
        assertEquals(0xA1, config.startLocation().roomId());
        assertEquals(AppConfig.ItemProfile.DEBUG_ALL_ITEMS, config.itemProfile());

        InputConfig input = config.inputConfig();
        assertEquals(GLFW_KEY_SPACE, input.menuOpenKey());
        assertEquals(GLFW_KEY_W, input.upKey());
        assertEquals(GLFW_KEY_S, input.downKey());
        assertEquals(GLFW_KEY_A, input.leftKey());
        assertEquals(GLFW_KEY_D, input.rightKey());
        assertEquals(GLFW_KEY_J, input.aKey());
        assertEquals(GLFW_KEY_K, input.bKey());
    }

    @Test
    void decimalRoomIdsAreAccepted() {
        AppConfig config = AppConfig.parse("""
            { "startLocation": { "type": "OVERWORLD", "roomId": "146" } }
            """);

        assertEquals(146, config.startLocation().roomId());
    }
}
```

- [ ] **Step 2: Run the config tests and verify they fail**

Run: `gradle test --tests linksawakening.config.AppConfigTest`

Expected: compilation fails because `AppConfig` does not exist yet.

- [ ] **Step 3: Implement minimal config model and input factory**

Add `AppConfig` with `defaults()`, `parse(String)`, `loadFromResources()`, `showTitleScreen()`, `playIntroStory()`, `debugEnabled()`, `startLocation()`, `itemProfile()`, and `inputConfig()`.

Add `InputConfig.fromKeyNames(Map<String, String>)` and have `InputConfig.loadFromResources()` delegate to it for backwards compatibility.

- [ ] **Step 4: Run config tests and verify they pass**

Run: `gradle test --tests linksawakening.config.AppConfigTest`

Expected: tests pass.

## Task 2: Resource Migration And Main Wiring

**Files:**
- Modify: `java/src/main/java/linksawakening/Main.java`
- Create: `java/src/main/resources/config/config.json`
- Delete: `java/src/main/resources/config/input.json`
- Modify: `java/src/test/java/linksawakening/MainTest.java`

- [ ] **Step 1: Write failing Main tests for pure startup helpers**

Add tests that call package-visible helpers in `Main`:

```java
@Test
void startRoomComesFromConfiguredOverworldLocation() {
    AppConfig config = AppConfig.parse("""
        { "startLocation": { "type": "OVERWORLD", "roomId": "0xA1" } }
        """);

    assertEquals(0xA1, Main.startingOverworldRoomId(config));
}

@Test
void debugDumpRequiresDebugEnabledAndF1Press() {
    AppConfig enabled = AppConfig.parse("{ \"debugEnabled\": true }");
    AppConfig disabled = AppConfig.parse("{ \"debugEnabled\": false }");

    assertTrue(Main.shouldRunDebugDump(enabled, GLFW_KEY_F1, GLFW_PRESS));
    assertFalse(Main.shouldRunDebugDump(disabled, GLFW_KEY_F1, GLFW_PRESS));
    assertFalse(Main.shouldRunDebugDump(enabled, GLFW_KEY_F2, GLFW_PRESS));
    assertFalse(Main.shouldRunDebugDump(enabled, GLFW_KEY_F1, GLFW_RELEASE));
}
```

- [ ] **Step 2: Run Main tests and verify they fail**

Run: `gradle test --tests linksawakening.MainTest`

Expected: compilation fails because helper methods do not exist yet.

- [ ] **Step 3: Wire AppConfig into Main**

Add a static `AppConfig appConfig`, load it after ROM load, use `appConfig.inputConfig()` in `initMenuSystem()`, use `startingOverworldRoomId(appConfig)` in `loadOverworldScreen()` and `loadOverworldTiles()`, skip title screen when `showTitleScreen` is false, warn and direct-spawn when `playIntroStory` is true, and gate F1 through `shouldRunDebugDump`.

- [ ] **Step 4: Add the new resource and remove the old resource**

Create `config/config.json` using the approved default shape. Delete `config/input.json`.

- [ ] **Step 5: Run focused tests**

Run: `gradle test --tests linksawakening.config.AppConfigTest --tests linksawakening.MainTest`

Expected: tests pass.

## Task 3: Full Verification

**Files:**
- No additional files.

- [ ] **Step 1: Run all Java tests**

Run: `gradle test`

Expected: all tests pass.

- [ ] **Step 2: Inspect touched files**

Run: `find java/src/main/resources/config -maxdepth 1 -type f -print`

Expected: only `java/src/main/resources/config/config.json` is listed.
