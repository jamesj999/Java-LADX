package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

/** Ensures the intended API remains reachable without the legacy API hierarchy. */
class LinkRoleCompositionTest {
    @Test
    void linkDeclaresItsRetainedRoleAccessors() throws Exception {
        for (String role : new String[] {
            "position", "motion", "animation", "actions", "status", "equipment", "airborne", "sideView", "controls"
        }) {
            var method = Link.class.getDeclaredMethod(role);
            assertTrue(method.getReturnType().isInterface());
            assertFalse(method.isAnnotationPresent(Deprecated.class));
        }
    }

    @Test
    void playerDeclaresItsComponentAccessors() throws Exception {
        PlayerState player = new PlayerState();
        for (String role :
                new String[] {"inventory", "vitals", "ammunition", "progression", "wallet", "equipmentLevels"}) {
            var method = PlayerState.class.getDeclaredMethod(role);
            assertFalse(method.isAnnotationPresent(Deprecated.class));
            assertSame(method.invoke(player), method.invoke(player));
        }
    }

    @Test
    void legacyHierarchyContainsOnlyDeprecatedMethodContracts() {
        for (Class<?> aggregate : new Class<?>[] {Link.class, PlayerState.class}) {
            for (Class<?> layer = aggregate.getSuperclass(); layer != Object.class; layer = layer.getSuperclass()) {
                assertEquals(0, layer.getDeclaredFields().length, layer.getName());
                for (var method : layer.getDeclaredMethods()) {
                    if (!method.isSynthetic())
                        assertTrue(method.isAnnotationPresent(Deprecated.class), method.toString());
                }
            }
        }
    }
}
