package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MagicRodMotionTest {

    @Test
    void spawnIncludesThePostSpawnOffsetsAndFinalSpeed() {
        assertSpawnData(0, 4, 4, 0x30, 0x00);
        assertSpawnData(1, 0xFC, 4, 0xD0, 0x00);
        assertSpawnData(2, 0xFC, 0xFC, 0x00, 0xD0);
        assertSpawnData(3, 4, 4, 0x00, 0x30);
    }

    @Test
    void movementUsesTheRomSixteenthPixelAccumulator() {
        MagicRodFireballMotion motion = new MagicRodFireballMotion();
        motion.initializeSpawn(0, 0);
        RoomEntity entity = entity(0x10, 0x20);

        RoomEntity first = motion.advancePosition(entity);
        RoomEntity second = motion.advancePosition(first);

        assertEquals(0x13, first.x());
        assertEquals(0x16, second.x());
        assertEquals(0x20, first.y());
    }

    @Test
    void wallTransitionUsesTheSourcePrivateCountdownAndFireFrames() {
        MagicRodFireballMotion motion = new MagicRodFireballMotion();
        motion.initializeSpawn(0, 0);

        motion.beginFireTransition(0);

        assertEquals(MagicRodFireballMotion.FIRE_TRANSITION_COUNTDOWN,
            motion.privateCountdown1(0));
        assertEquals(0, motion.frameVariant(0));
        assertEquals(1, motion.frameVariant(8));
        assertEquals(false, motion.tickFireTransition(0));
        assertEquals(0x2F, motion.privateCountdown1(0));

        for (int tick = 0; tick < 0x2D; tick++) {
            assertEquals(false, motion.tickFireTransition(0));
        }
        assertEquals(0x02, motion.privateCountdown1(0));
        assertEquals(true, motion.tickFireTransition(0));
        assertEquals(1, motion.privateCountdown1(0));
    }

    private static void assertSpawnData(int direction, int offsetX, int offsetY, int speedX, int speedY) {
        MagicRodFireballMotion.SpawnData actual = MagicRodFireballMotion.spawnData(direction);
        assertEquals(offsetX, actual.offsetX());
        assertEquals(offsetY, actual.offsetY());
        assertEquals(speedX, actual.speedX());
        assertEquals(speedY, actual.speedY());
        assertEquals(0, actual.initialVariant());
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, -1, MagicRodFireballMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(
                MagicRodFireballMotion.ENTITY_TYPE), 0);
    }
}
