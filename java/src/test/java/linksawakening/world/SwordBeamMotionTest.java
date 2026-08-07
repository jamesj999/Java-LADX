package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SwordBeamMotionTest {

    @Test
    void spawnUsesTheSharedPlayerProjectileTables() {
        assertSpawnData(0, 0x20, 0x00);
        assertSpawnData(1, 0xE0, 0x00);
        assertSpawnData(2, 0x00, 0xE0);
        assertSpawnData(3, 0x00, 0x20);
    }

    @Test
    void firstHandlerPassUsesTheRomDirectionOffsetsAndDoublesSpeed() {
        SwordBeamMotion motion = new SwordBeamMotion();
        motion.initializeSpawn(0, 0);

        assertEquals(0, motion.state(0));
        motion.initializeFirstHandler(0, 0);

        assertEquals(1, motion.state(0));
        assertEquals(0x40, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
    }

    @Test
    void positionUsesTheRomSixteenthPixelAccumulator() {
        SwordBeamMotion motion = new SwordBeamMotion();
        motion.initializeSpawn(0, 0);
        motion.initializeFirstHandler(0, 0);
        RoomEntity entity = entity(0x10, 0x20);

        RoomEntity first = motion.advancePosition(entity);
        RoomEntity second = motion.advancePosition(first);

        assertEquals(0x14, first.x());
        assertEquals(0x18, second.x());
        assertEquals(0x20, first.y());
    }

    @Test
    void downwardAndUpwardDirectionsUseTheEntityHandlerDirectionOrder() {
        SwordBeamMotion motion = new SwordBeamMotion();

        motion.initializeSpawn(0, 2);
        motion.initializeFirstHandler(0, 2);
        assertEquals(0xC0, motion.speedY(0));

        motion.clear(0);
        motion.initializeSpawn(0, 3);
        motion.initializeFirstHandler(0, 3);
        assertEquals(0x40, motion.speedY(0));
    }

    private static void assertSpawnData(int direction, int speedX, int speedY) {
        SwordBeamMotion.SpawnData actual = SwordBeamMotion.spawnData(direction);
        assertEquals(0, actual.offsetX());
        assertEquals(direction < 2 ? 0 : 0, actual.offsetY());
        assertEquals(speedX, actual.speedX());
        assertEquals(speedY, actual.speedY());
        assertEquals(direction, actual.initialVariant());
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, -1, SwordBeamMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(SwordBeamMotion.ENTITY_TYPE),
            0);
    }
}
