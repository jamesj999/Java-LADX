package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RoomFacadeCompatibilityTest {
    @Test
    void outputPortExposesOnlyDrainsWithoutLoadedRoomAccess() {
        for (var method : RoomEntityOutputSource.class.getDeclaredMethods()) {
            assertTrue(
                    method.getName().startsWith("consume") || method.getName().startsWith("drain"));
            assertFalse(method.getReturnType().equals(RoomRuntime.class));
            assertFalse(method.getReturnType().equals(linksawakening.world.entity.RoomEntityRuntime.class));
        }
    }

    @Test
    void facadeHasNoDeprecatedMembersOrCompatibilityInheritance() {
        assertEquals(1, RoomSession.class.getConstructors().length);
        assertEquals(
                RoomSessionConfig.class, RoomSession.class.getConstructors()[0].getParameterTypes()[0]);
        assertEquals(Object.class, RoomSession.class.getSuperclass());
        // These operation ports carry drains/boundary dispatch, never legacy API or a current-runtime getter.
        assertTrue(RoomEntityOutputSource.class.isAssignableFrom(RoomSession.class));
        assertTrue(RoomBoundaryOperations.class.isAssignableFrom(RoomTransitionController.class));
        assertFalse(RoomBoundaryOperations.class.isAssignableFrom(RoomSession.class));
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
