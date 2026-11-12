package linksawakening.architecture;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import linksawakening.Main;
import linksawakening.app.GameSession;
import linksawakening.app.screen.*;
import linksawakening.world.room.RoomPlayerItems;
import org.junit.jupiter.api.Test;

class AppBoundaryTest {
    @Test
    void entryPointOwnsNoGameStateOrBackend() {
        assertEquals(0, Main.class.getDeclaredFields().length);
        assertEquals(1, Main.class.getDeclaredMethods().length);
    }

    @Test
    void sessionStateIsNeverStaticAndScreensImplementLifecycle() {
        assertFalse(Arrays.stream(GameSession.class.getDeclaredFields())
                .anyMatch(f -> Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()));
        for (var screen : List.of(
                TitleScreen.class,
                CutsceneScreen.class,
                FileMenuScreen.class,
                FileSaveScreen.class,
                GameplayScreen.class)) assertTrue(Screen.class.isAssignableFrom(screen));
    }

    @Test
    void sessionAndGameplayDoNotReferenceNativeBackends() throws Exception {
        for (var type : ClassFileReferences.classes()) {
            boolean isolated = type.name().equals("linksawakening/app/GameSession")
                    || type.name().startsWith("linksawakening/app/gameplay/");
            for (var ref : type.references()) {
                if (isolated) assertFalse(ref.startsWith("org/lwjgl/"), type.name() + " -> " + ref);
                if (type.name().startsWith("linksawakening/app/SessionBootstrap"))
                    assertFalse(ref.startsWith("linksawakening/audio/openal/"), type.name() + " -> " + ref);
            }
        }
    }

    @Test
    void roomItemsExposeOnlyTheCanonicalBombPlacement() {
        var methods = Arrays.stream(RoomPlayerItems.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("placeBomb"))
                .toList();
        assertEquals(1, methods.size());
        assertEquals(4, methods.getFirst().getParameterCount());
    }
}
