package linksawakening.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpenWorldCollisionTest {
    @Test
    void unbindingRoomRestoresOpenWorldQueries() {
        var collision = OverworldCollision.openWorld();
        assertFalse(collision.hasRoom());
        assertFalse(collision.pointBlocked(32, 32));
        assertEquals(0xFF, collision.objectIdAtPoint(32, 32));
        collision.setRoom(new int[256]);
        assertTrue(collision.hasRoom());
        assertEquals(0, collision.objectIdAtPoint(32, 32));
        collision.setRoom(null);
        assertFalse(collision.hasRoom());
        assertEquals(0xFF, collision.objectIdAtPoint(32, 32));
        assertEquals(PhysicsFlags.NONE, collision.objectPhysicsFlagAtPoint(32, 32));
    }
}
