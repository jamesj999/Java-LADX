package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RoomFacadeCompatibilityTest {
    @Test
    void onlyEntityTestBridgesRemainWithoutCompatibilityInheritance() {
        assertEquals(2, RoomSession.class.getConstructors().length);
        var legacyConstructor = Arrays.stream(RoomSession.class.getConstructors())
                .filter(c -> c.getParameterCount() == 7)
                .findFirst()
                .orElseThrow();
        assertTrue(legacyConstructor.getAnnotation(Deprecated.class).forRemoval());
        assertEquals(Object.class, RoomSession.class.getSuperclass());
        assertEquals(0, RoomSession.class.getInterfaces().length);
        var bridges = Arrays.stream(RoomSession.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Deprecated.class))
                .toList();
        assertEquals(26, bridges.size());
        for (var bridge : bridges)
            assertTrue(bridge.getAnnotation(Deprecated.class).forRemoval(), bridge.toString());
        assertFalse(Arrays.stream(RoomSession.class.getMethods())
                .anyMatch(m -> m.getName().equals("placeBomb")));
    }
}
