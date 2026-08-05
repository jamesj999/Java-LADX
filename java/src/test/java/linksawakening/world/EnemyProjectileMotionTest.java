package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnemyProjectileMotionTest {

    @Test
    void moblinSpawnTablesMatchTheRomForAllDirections() {
        assertSpawnData(0x0C, 0, 0x08, 0xFC, 0x20, 0x00, 0);
        assertSpawnData(0x0C, 1, 0xF8, 0xFC, 0xE0, 0x00, 1);
        assertSpawnData(0x0C, 2, 0x04, 0xF8, 0x00, 0xE0, 2);
        assertSpawnData(0x0C, 3, 0xFC, 0x00, 0x00, 0x20, 3);
    }

    @Test
    void octorokSpawnTablesPreserveTheRomAdjacentLabelReads() {
        assertSpawnData(0x0A, 0, 0x08, 0x00, 0x20, 0x00, 0);
        assertSpawnData(0x0A, 1, 0xF8, 0x00, 0xE0, 0x00, 0);
        assertSpawnData(0x0A, 2, 0x00, 0xF8, 0x00, 0xE0, 0);
        assertSpawnData(0x0A, 3, 0x00, 0x08, 0x00, 0x20, 0);
    }

    @Test
    void regularMovementUsesTheRomSixteenthPixelAccumulator() {
        EnemyProjectileMotion motion = new EnemyProjectileMotion();
        RoomEntity entity = entity(0x0C, 0x10, 0x20, 0);
        motion.initializeSpawn(0, entity.type(), 0);

        EnemyProjectileMotion.Update first = motion.advance(entity, null);
        assertEquals(0x12, first.entity().x());
        assertEquals(0x20, first.entity().y());
        assertEquals(0, first.entity().spriteVariant());
        assertFalse(first.collidedWithWall());

        motion.setSpeedForTest(0, 0x08, 0, 0);
        RoomEntity fractional = first.entity();
        EnemyProjectileMotion.Update half = motion.advance(fractional, null);
        assertEquals(0x12, half.entity().x());
        EnemyProjectileMotion.Update whole = motion.advance(half.entity(), null);
        assertEquals(0x13, whole.entity().x());
    }

    @Test
    void wallCollisionStartsTransitionAndBouncesYBeforeX() {
        EnemyProjectileMotion motion = new EnemyProjectileMotion();
        RoomEntity entity = entity(0x0C, 0x10, 0x20, 0);
        motion.initializeSpawn(0, entity.type(), 0);
        motion.setSpeedForTest(0, 0x20, 0xE0, 0);

        EnemyProjectileMotion.Update update = motion.advance(
            entity, (candidate, direction, nextX, nextY) -> direction == 2);

        assertTrue(update.collidedWithWall());
        assertFalse(update.unloaded());
        assertEquals(0x12, update.entity().x());
        assertEquals(0x20, update.entity().y());
        assertEquals(0xFC, motion.speedX(0));
        assertEquals(0x04, motion.speedY(0));
        assertEquals(0x18, motion.transitionCountdown(0));
        assertEquals(0x10, motion.speedZ(0));
    }

    @Test
    void wallTransitionAddsZAndAppliesArrowSpinButNotRockSpin() {
        EnemyProjectileMotion arrowMotion = new EnemyProjectileMotion();
        RoomEntity arrow = entity(0x0C, 0x10, 0x20, 0);
        arrowMotion.initializeSpawn(0, arrow.type(), 0);
        arrowMotion.setTransitionForTest(0, 0x10, 0, 0x10);

        EnemyProjectileMotion.Update arrowUpdate = arrowMotion.advance(arrow, null);
        assertEquals(0x0F, arrowMotion.transitionCountdown(0));
        assertEquals(0x01, arrowUpdate.entity().z());
        assertEquals(0x0E, arrowMotion.speedZ(0));
        assertEquals(0x03, arrowUpdate.entity().spriteVariant());

        EnemyProjectileMotion rockMotion = new EnemyProjectileMotion();
        RoomEntity rock = entity(0x0A, 0x10, 0x20, 0);
        rockMotion.initializeSpawn(0, rock.type(), 0);
        rockMotion.setTransitionForTest(0, 0x10, 0, 0x10);

        EnemyProjectileMotion.Update rockUpdate = rockMotion.advance(rock, null);
        assertEquals(0, rockUpdate.entity().spriteVariant());
    }

    @Test
    void transitionCountdownOneUnloadsBeforeAnotherPhysicsStep() {
        EnemyProjectileMotion motion = new EnemyProjectileMotion();
        RoomEntity entity = entity(0x0C, 0x10, 0x20, 0);
        motion.initializeSpawn(0, entity.type(), 0);
        motion.setTransitionForTest(0, 0x02, 0x07, 0x10);

        EnemyProjectileMotion.Update update = motion.advance(entity, null);

        assertTrue(update.unloaded());
        assertEquals(0x01, motion.transitionCountdown(0));
        assertEquals(entity, update.entity());
    }

    private static void assertSpawnData(int type, int direction, int offsetX, int offsetY,
                                         int speedX, int speedY, int initialVariant) {
        EnemyProjectileMotion.SpawnData actual =
            EnemyProjectileMotion.spawnData(type, direction);
        assertEquals(offsetX, actual.offsetX());
        assertEquals(offsetY, actual.offsetY());
        assertEquals(speedX, actual.speedX());
        assertEquals(speedY, actual.speedY());
        assertEquals(initialVariant, actual.initialVariant());
    }

    private static RoomEntity entity(int type, int x, int y, int z) {
        return new RoomEntity(0, -1, type, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(type), 0, 0, 0, z);
    }
}
