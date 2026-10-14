package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        // so the no-Piece-of-Power right speed is $30 (three pixels/frame).
        assertEquals(0x13, first.entity().x());
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
        assertEquals(0, update.entity().spriteVariant());
        assertEquals(3, motion.advance(update.entity(), null).entity().spriteVariant());

        motion.setTransitionForTest(0, 0x02, 0x07, 0x10);
        PlayerArrowMotion.Update unload = motion.advance(entity, null);

        assertTrue(unload.unloaded());
        assertEquals(0x01, motion.transitionCountdown(0));
        assertEquals(entity, unload.entity());
    }

    @Test
    void sideViewReboundFallsOnYAndPreservesHeight() {
        PlayerArrowMotion motion = new PlayerArrowMotion();
        RoomEntity entity = entity(0x40, 0x50, 7, 0);
        motion.setSpeedForTest(0, 0xF0, 0, 0x10);
        motion.setTransitionForTest(0, 0x18, 7, 0x10);

        for (int frame = 1; frame <= 9; frame++) {
            PlayerArrowMotion.Update update = motion.advance(entity, null, frame, 0, true);
            assertFalse(update.unloaded());
            entity = update.entity();
            assertEquals(7, entity.z());
            assertEquals(0x10, motion.speedZ(0));
        }

        // Movement uses the old Y speed: sum(0, 2, ..., 16) / 16 = 4 pixels.
        assertEquals(0x37, entity.x());
        assertEquals(0x54, entity.y());
        assertEquals(18, motion.speedY(0));
        assertEquals(0x0F, motion.transitionCountdown(0));
    }

    @ParameterizedTest
    @CsvSource({"0, 0x0C, 0x0C, 0x40", "1, 0x0C, 0x0B, 8",
        "1, 0xF4, 0xF5, 8", "2, 0x0C, 0x0B, 0x40", "3, 0xF4, 0xF5, 0x40"})
    void sideViewReboundUsesGroundSpeedLimitAndEightFrameDrag(
        int groundStatus, int initialX, int expectedX, int expectedY) {
        PlayerArrowMotion motion = new PlayerArrowMotion();
        RoomEntity entity = entity(0x40, 0x50, 7, 0);
        motion.setSpeedForTest(0, initialX, expectedY - 1, 0x10);
        motion.setTransitionForTest(0, 0x18, 7, 0x10);

        PlayerArrowMotion.Update first = motion.advance(entity, null, 7, groundStatus, true);
        assertEquals(initialX, motion.speedX(0));
        assertEquals(expectedY, motion.speedY(0));
        motion.advance(first.entity(), null, 8, groundStatus, true);
        assertEquals(expectedX, motion.speedX(0));
        assertEquals(expectedY, motion.speedY(0));
    }

    @Test
    void reboundSpinDisplaysTheStoredFrameBeforeSelectingTheNextOne() {
        PlayerArrowMotion motion = new PlayerArrowMotion();
        RoomEntity entity = entity(0x40, 0x50, 0, 0);
        motion.initializeSpawn(0, 0);
        entity = motion.advance(entity,
            (candidate, direction, nextX, nextY) -> direction == 0).entity();

        // After impact: direction 0 once, then left for eight frames,
        // down for eight, and right until the countdown-1 unload pass.
        for (int frame = 1; frame <= 22; frame++) {
            PlayerArrowMotion.Update update = motion.advance(entity, null);
            assertFalse(update.unloaded());
            int expected = frame == 1 ? 0 : frame <= 9 ? 1 : frame <= 17 ? 3 : 0;
            assertEquals(expected, update.entity().spriteVariant(), "frame " + frame);
            entity = update.entity();
        }
        assertTrue(motion.advance(entity, null).unloaded());

        motion.clear(0);
        motion.initializeSpawn(0, 2);
        motion.setTransitionForTest(0, 0x18, 0, 0x10);
        assertEquals(2, motion.advance(entity, null).entity().spriteVariant());
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
