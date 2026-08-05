package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EntityBackgroundCollisionResultTest {

    @Test
    void collisionFlagsMatchTheRomDirectionTable() {
        assertEquals(0x01, EntityBackgroundCollisionResult.collisionFlagForDirection(0));
        assertEquals(0x02, EntityBackgroundCollisionResult.collisionFlagForDirection(1));
        assertEquals(0x04, EntityBackgroundCollisionResult.collisionFlagForDirection(2));
        assertEquals(0x08, EntityBackgroundCollisionResult.collisionFlagForDirection(3));
    }

    @Test
    void passableResultCarriesNoCollisionBit() {
        EntityBackgroundCollisionResult result = EntityBackgroundCollisionResult.passable(
            2, 0x07, 0x34, 0x56);

        assertEquals(false, result.blocked());
        assertEquals(EntityBackgroundCollisionResult.NO_OBJECT, result.objectId());
        assertEquals(0x07, result.physicsFlag());
        assertEquals(2, result.direction());
        assertEquals(0, result.collisionFlag());
        assertEquals(0x34, result.sampleX());
        assertEquals(0x56, result.sampleY());
    }

    @Test
    void booleanCollisionAdapterAddsTheDirectionalBit() {
        RoomEntityBackgroundCollision booleanCollision = (entity, direction, nextX, nextY) -> true;
        RoomEntityBackgroundInteraction interaction = RoomEntityBackgroundInteraction.fromBoolean(
            booleanCollision);

        EntityBackgroundCollisionResult result = interaction.probe(
            null, EntityBackgroundCollisionResult.DOWN, 0x12, 0x34);

        assertEquals(true, result.blocked());
        assertEquals(EntityBackgroundCollisionResult.NO_OBJECT, result.objectId());
        assertEquals(0x08, result.collisionFlag());
        assertEquals(0x12, result.sampleX());
        assertEquals(0x34, result.sampleY());
    }
}
