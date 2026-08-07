package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlayerArrowMotionTest {

    @Test
    void spawnTablesMatchTheRomForAllDirections() {
        assertSpawnData(0, 0x20, 0x00);
        assertSpawnData(1, 0xE0, 0x00);
        assertSpawnData(2, 0x00, 0xE0);
        assertSpawnData(3, 0x00, 0x20);
    }

    @Test
    void regularMovementUsesTheRomSixteenthPixelAccumulatorAndPreservesZ() {
        PlayerArrowMotion motion = new PlayerArrowMotion();
        RoomEntity entity = entity(0x10, 0x20, 0x07, 0);
        motion.initializeSpawn(0, 0);

        PlayerArrowMotion.Update first = motion.advance(entity, null);

        // initializeSpawn models ShootArrow's post-spawn label_140F table,
        // so the no-Piece-of-Power right speed is $40 (four pixels/frame).
        assertEquals(0x14, first.entity().x());
        assertEquals(0x20, first.entity().y());
        assertEquals(0x07, first.entity().z());
        assertEquals(0, first.entity().spriteVariant());
        assertFalse(first.collidedWithWall());
        assertFalse(first.unloaded());
    }

    @Test
    void wallCollisionUsesThePlayerArrowsQuarterSpeedBounce() {
        PlayerArrowMotion motion = new PlayerArrowMotion();
        RoomEntity entity = entity(0x10, 0x20, 0x07, 0);
        motion.initializeSpawn(0, 0);
        motion.setSpeedForTest(0, 0x20, 0xE0, 0);

        PlayerArrowMotion.Update update = motion.advance(
            entity, (candidate, direction, nextX, nextY) -> direction == 2);

        assertTrue(update.collidedWithWall());
        assertFalse(update.unloaded());
        assertEquals(0x12, update.entity().x());
        assertEquals(0x20, update.entity().y());
        assertEquals(0xF8, motion.speedX(0));
        assertEquals(0x08, motion.speedY(0));
        assertEquals(0x18, motion.transitionCountdown(0));
        assertEquals(0x10, motion.speedZ(0));
    }

    @Test
    void wallTransitionSpinsAndAppliesGravityBeforeUnloading() {
        PlayerArrowMotion motion = new PlayerArrowMotion();
        RoomEntity entity = entity(0x10, 0x20, 0x00, 0);
        motion.initializeSpawn(0, 0);
        motion.setTransitionForTest(0, 0x10, 0x00, 0x10);

        PlayerArrowMotion.Update update = motion.advance(entity, null);

        assertEquals(0x0F, motion.transitionCountdown(0));
        assertEquals(0x01, update.entity().z());
        assertEquals(0x0E, motion.speedZ(0));
        assertEquals(0x03, update.entity().spriteVariant());

        motion.setTransitionForTest(0, 0x02, 0x07, 0x10);
        PlayerArrowMotion.Update unload = motion.advance(entity, null);

        assertTrue(unload.unloaded());
        assertEquals(0x01, motion.transitionCountdown(0));
        assertEquals(entity, unload.entity());
    }

    private static void assertSpawnData(int direction, int speedX, int speedY) {
        PlayerArrowMotion.SpawnData actual = PlayerArrowMotion.spawnData(direction);
        assertEquals(0, actual.offsetX());
        assertEquals(0, actual.offsetY());
        assertEquals(speedX, actual.speedX());
        assertEquals(speedY, actual.speedY());
        assertEquals(direction, actual.initialVariant());
    }

    private static RoomEntity entity(int x, int y, int z, int variant) {
        return new RoomEntity(0, -1, 0x00, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x00), variant, 0, 0, z);
    }
}
