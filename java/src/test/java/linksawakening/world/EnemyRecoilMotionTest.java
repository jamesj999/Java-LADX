package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnemyRecoilMotionTest {

    @Test
    void configureMirrorsRomVectorAwayFromLinkAcrossTheDominantAxes() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();

        motion.configure(0, 64, 64, 0, 100, 64, 0x30);
        assertEquals(0xD0, motion.recoilSpeedX(0));
        assertEquals(0x00, motion.recoilSpeedY(0));

        motion.configure(0, 64, 64, 0, 64, 28, 0x30);
        assertEquals(0x00, motion.recoilSpeedX(0));
        assertEquals(0x30, motion.recoilSpeedY(0));

        motion.configure(0, 64, 64, 0, 100, 100, 0x30);
        assertEquals(0xD0, motion.recoilSpeedX(0));
        assertEquals(0xD0, motion.recoilSpeedY(0));

        motion.configure(0, 64, 64, 0, 96, 80, 0x30);
        assertEquals(0xD0, motion.recoilSpeedX(0));
        assertEquals(0xE8, motion.recoilSpeedY(0));
    }

    @Test
    void configureUsesEntityZInTheRomYDistance() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();

        motion.configure(0, 64, 64, 8, 64, 64, 0x30);

        // Link is eight pixels below the entity's Z-adjusted Y coordinate;
        // recoil therefore points eight pixels upward in the ROM vector.
        assertEquals(0x00, motion.recoilSpeedX(0));
        assertEquals(0xD0, motion.recoilSpeedY(0));
    }

    @Test
    void configureUsesRomTieBreakWhenEntityAndLinkOverlap() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();

        motion.configure(0, 64, 64, 0, 64, 64, 0x30);

        assertEquals(0xD0, motion.recoilSpeedX(0));
        assertEquals(0x30, motion.recoilSpeedY(0));
    }

    @Test
    void advanceUsesSignedSixteenSubpixelMovementAndUnsignedWrapping() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();
        RoomEntity entity = entity(0, 0x02, 0x20, 0);

        motion.configure(0, entity.x(), entity.y(), entity.z(), 0x0A, 0x20, 0x08);

        RoomEntity first = motion.advance(entity, null).entity();
        RoomEntity second = motion.advance(first, null).entity();
        RoomEntity third = motion.advance(second, null).entity();

        // A speed of -$08 is -0.5 pixels/frame in the ROM accumulator.
        assertEquals(0x01, first.x());
        assertEquals(0x01, second.x());
        assertEquals(0x00, third.x());
        assertEquals(entity.y(), first.y());
        assertTrue(motion.isActive(0));

        motion.configure(0, 0x00, 0x40, 0, 0x40, 0x40, 0x30);
        RoomEntity wrapped = motion.advance(entity(0, 0x00, 0x40, 0), null).entity();
        assertEquals(0xFD, wrapped.x());
    }

    @Test
    void advanceStopsRecoilWhenTheDominantAxisHitsBackground() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();
        RoomEntity entity = entity(0, 0x40, 0x40, 0);
        motion.configure(0, entity.x(), entity.y(), entity.z(), 0x70, 0x40, 0x30);

        EnemyRecoilMotion.Update update = motion.advance(
            entity,
            (candidate, direction, nextX, nextY) -> direction == 1);

        assertEquals(entity.x(), update.entity().x());
        assertEquals(entity.y(), update.entity().y());
        assertTrue(update.blocked());
        assertFalse(motion.isActive(0));
    }

    @Test
    void clearIsIdempotent() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();
        motion.configure(0, 64, 64, 0, 96, 64, 0x30);

        motion.clear(0);
        motion.clear(0);

        assertFalse(motion.isActive(0));
        assertEquals(0, motion.recoilSpeedX(0));
        assertEquals(0, motion.recoilSpeedY(0));
    }

    private static RoomEntity entity(int slot, int x, int y, int z) {
        return new RoomEntity(slot, 0, 0x09, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x09), -1, 0, 0, z);
    }
}
