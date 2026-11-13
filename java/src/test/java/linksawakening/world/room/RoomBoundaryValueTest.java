package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RoomBoundaryValueTest {
    @Test
    void unknownBytesSurviveWithoutInventingMotionStates() {
        var motion = new LinkMotion(false, 0xE1, false, 0, 0, 0);
        assertEquals(0xE1, motion.rawState());
        assertTrue(motion.knownState().isEmpty());
        var ground = new LinkGroundFrame(1, 2, false, false, false, false, 0xE1, 0, 0);
        assertEquals(0xE1, ground.rawMotionState());
        assertTrue(ground.knownState().isEmpty());
        assertEquals(0xE1, new LinkPosition(1, 2, 0, 0xE1, 0).rawDirection());
        assertEquals(0xE1, new EntityPickupInputs.Link(1, 2, false, true, 0xE1, 0).rawDirection());
    }

    @Test
    void eachProbeStoresOnlyOneRepresentation() {
        for (var type : new Class<?>[] {
            LinkPosition.class, LinkMotion.class, LinkGroundFrame.class, EntityPickupInputs.Link.class
        }) {
            assertEquals(1, type.getConstructors().length);
            assertFalse(Arrays.stream(type.getConstructors()).anyMatch(c -> c.isAnnotationPresent(Deprecated.class)));
            assertEquals(
                    1,
                    Arrays.stream(type.getRecordComponents())
                            .filter(c -> c.getName().startsWith("raw"))
                            .count());
            assertFalse(Arrays.stream(type.getRecordComponents())
                    .anyMatch(c -> c.getType().isEnum()));
        }
    }
}
