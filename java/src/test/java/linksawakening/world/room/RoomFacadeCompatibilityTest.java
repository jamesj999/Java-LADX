package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RoomFacadeCompatibilityTest {
    @Test
    void facadeHasNoDeprecatedMembersOrCompatibilityInheritance() {
        assertEquals(1, RoomSession.class.getConstructors().length);
        assertEquals(
                RoomSessionConfig.class, RoomSession.class.getConstructors()[0].getParameterTypes()[0]);
        assertEquals(Object.class, RoomSession.class.getSuperclass());
        assertEquals(0, RoomSession.class.getInterfaces().length);
        var bridges = Arrays.stream(RoomSession.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Deprecated.class))
                .toList();
        assertEquals(0, bridges.size());
        assertTrue(Arrays.stream(RoomSession.class.getDeclaredFields())
                .noneMatch(f -> f.isAnnotationPresent(Deprecated.class)));
        for (var bridge : bridges)
            assertTrue(bridge.getAnnotation(Deprecated.class).forRemoval(), bridge.toString());
        assertFalse(Arrays.stream(RoomSession.class.getMethods())
                .anyMatch(m -> m.getName().equals("placeBomb")));
    }
}
