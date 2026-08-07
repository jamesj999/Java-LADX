package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MagicRodMotionTest {

    @Test
    void spawnUsesTheSharedPlayerProjectileSpeedTables() {
        assertSpawnData(0, 0x20, 0x00);
        assertSpawnData(1, 0xE0, 0x00);
        assertSpawnData(2, 0x00, 0xE0);
        assertSpawnData(3, 0x00, 0x20);
    }

    @Test
    void movementUsesTheRomSixteenthPixelAccumulator() {
        MagicRodFireballMotion motion = new MagicRodFireballMotion();
        motion.initializeSpawn(0, 0);
        RoomEntity entity = entity(0x10, 0x20);

        RoomEntity first = motion.advancePosition(entity);
        RoomEntity second = motion.advancePosition(first);

        assertEquals(0x12, first.x());
        assertEquals(0x14, second.x());
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

        for (int tick = 0; tick < 0x2E; tick++) {
            motion.tickFireTransition(0);
        }
        assertEquals(0x01, motion.privateCountdown1(0));
        assertEquals(true, motion.tickFireTransition(0));
        assertEquals(0, motion.privateCountdown1(0));
    }

    private static void assertSpawnData(int direction, int speedX, int speedY) {
        MagicRodFireballMotion.SpawnData actual = MagicRodFireballMotion.spawnData(direction);
        assertEquals(0, actual.offsetX());
        assertEquals(0, actual.offsetY());
        assertEquals(speedX, actual.speedX());
        assertEquals(speedY, actual.speedY());
        assertEquals(direction, actual.initialVariant());
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, -1, MagicRodFireballMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(
                MagicRodFireballMotion.ENTITY_TYPE), 0);
    }
}
