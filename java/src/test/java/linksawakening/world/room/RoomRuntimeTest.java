package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RoomRuntimeTest {
    @Test
    void aLoadedGenerationOwnsItsRoomRuntimeAndFreshCollisionScratch() {
        var session = RoomRenderAllocationTest.room();
        var room = session.rooms().activeRoom();
        var entities = session.components.entityFrame().runtime;
        var first = new RoomRuntime(room, entities);
        var replacement = new RoomRuntime(room, entities);
        assertSame(room, first.room);
        assertSame(entities, first.entities);
        assertNotSame(first.collisions, replacement.collisions);
        assertNotSame(first.collisions.objectSamples, replacement.collisions.objectSamples);
        assertNotSame(first.collisions.groundResults, replacement.collisions.groundResults);
    }

    @Test
    void absenceCannotBePackagedAsALoadedGeneration() {
        var session = RoomRenderAllocationTest.room();
        assertThrows(NullPointerException.class, () -> new RoomRuntime(null, session.components.entityFrame().runtime));
        assertThrows(
                NullPointerException.class,
                () -> new RoomRuntime(session.rooms().activeRoom(), null));
    }
}
