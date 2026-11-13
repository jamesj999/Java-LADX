package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.world.entity.motion.HeartPiecePickupMotion;
import org.junit.jupiter.api.Test;

class EntityDependencyDefaultsTest {
    @Test
    void unsupportedPresentationPreservesTheExistingEntityAndVariant() {
        var sprites = EntitySpriteViews.unsupported();
        RoomEntity entity = TestEntities.active(0x31).at(40, 50).build();
        var definition = sprites.forSwordShieldPickup(true);
        assertFalse(definition.supported());
        assertSame(entity, sprites.present(entity, definition, 7));
        assertEquals(
                entity.spriteVariant(), sprites.present(entity, definition, -1).spriteVariant());
    }

    @Test
    void absentProbeDiffersFromAnActuallySampledEmptyCellWithoutAllocating() {
        RoomEntity entity = TestEntities.active(0x68).at(40, 50).build();
        var absent = RoomEntityObjectQuery.NONE.sampleOrAbsent(entity);
        var sampledEmpty = RoomEntityObjectSample.none();
        assertEquals(sampledEmpty, absent);
        assertFalse(absent.sampled());
        assertTrue(sampledEmpty.sampled());
        assertSame(absent, RoomEntityObjectQuery.NONE.sampleOrAbsent(entity));
        assertSame(
                EntityBackgroundCollisionResult.NONE,
                RoomEntityBackgroundInteraction.NONE.probe(entity, 2, 80, 90, 0, 10));
    }

    @Test
    void eagerPickupOwnersDistinguishNotCollectedFromStartedAndCompleted() {
        EntityPickupState state = new EntityPickupState();
        assertFalse(state.readHeartPiecePickup(0).started());
        assertFalse(state.readHeartPiecePickup(0).active());
        state.writeHeartPiecePickup(0, new HeartPiecePickupMotion());
        assertTrue(state.readHeartPiecePickup(0).started());
        assertTrue(state.readHeartPiecePickup(0).active());
        assertFalse(state.readHeartPiecePickup(1).started());
    }
}
