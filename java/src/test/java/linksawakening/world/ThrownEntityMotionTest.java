package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ThrownEntityMotionTest {

    @Test
    void normalTopViewThrowUsesTheFourRomDirectionWindows() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT, 0x05, false);
        assertEquals(0x30, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
        assertEquals(0x04, motion.speedZ(0));

        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_UP, 0x05, false);
        assertEquals(0x00, motion.speedX(0));
        assertEquals(0xD0, motion.speedY(0));
        assertEquals(0x04, motion.speedZ(0));
    }

    @Test
    void bombAndSideScrollOffsetsSelectTheLaterRomWindows() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_LEFT, 0x02, false);
        assertEquals(0xE8, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
        assertEquals(0x10, motion.speedZ(0));

        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT, 0x05, true);
        assertEquals(0x30, motion.speedX(0));
        assertEquals(0xF4, motion.speedY(0));
        assertEquals(0x00, motion.speedZ(0));
    }

    @Test
    void speedUsesTheRomSixteenFrameFixedPointAccumulatorAndGravity() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT, 0x05, false);
        RoomEntity entity = entity(0, 0x05, 0x20, 0x30, 0x10);

        RoomEntity updated = motion.advance(entity, false, null).entity();
        assertEquals(0x23, updated.x());
        assertEquals(0x30, updated.y());
        assertEquals(0x10, updated.z());
        assertEquals(0x02, motion.speedZ(0));

        for (int frame = 1; frame < 8; frame++) {
            updated = motion.advance(updated, false, null).entity();
        }
        assertEquals(0x38, updated.x());
    }

    @Test
    void wallResponseUsesTheRomNegateAndEighthSpeed() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT, 0x05, false);
        RoomEntity entity = entity(0, 0x05, 0x20, 0x30, 0);

        RoomEntity updated = motion.advance(entity, false,
            (candidate, direction, nextX, nextY) -> direction == 0).entity();

        assertEquals(0x20, updated.x());
        assertEquals(0xFA, motion.speedX(0));
    }

    @Test
    void topViewLandingBouncesAndHalvesHorizontalSpeed() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.startEnemyBomb(0, 0x18, 0xE8, 0xE0);
        RoomEntity landed = motion.advance(entity(0, 2, 0x40, 0x50, 1), false, null).entity();
        assertEquals(0, landed.z());
        // $60FB: SRA then CPL of the post-gravity speed $DE.
        assertEquals(0x10, motion.speedZ(0));
        assertEquals(0x0C, motion.speedX(0));
        assertEquals(0xF4, motion.speedY(0));
    }

    @Test
    void weakLandingAndShallowWaterStopAllMotion() {
        for (int groundStatus : new int[] {0, 2}) {
            ThrownEntityMotion motion = new ThrownEntityMotion();
            motion.startEnemyBomb(0, 0x18, 0xE8, groundStatus == 0 ? 0xFE : 0xE0);
            RoomEntity landed = motion.advance(entity(0, 2, 0x40, 0x50, 0),
                false, null, 0, groundStatus).entity();
            assertEquals(0, landed.z());
            assertEquals(0, motion.speedX(0));
            assertEquals(0, motion.speedY(0));
            assertEquals(0, motion.speedZ(0));
        }
    }

    @Test
    void sideViewThrowFallsAfterItsInitialUpwardArc() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT, 0x05, true);
        RoomEntity thrown = entity(0, 5, 0x20, 0x50, 0);
        for (int frame = 0; frame < 32; frame++) {
            thrown = motion.advance(thrown, true, null, frame, 0).entity();
        }
        assertEquals(0x34, motion.speedY(0));
        assertEquals(0x76, thrown.y());
        assertEquals(0, thrown.z());
    }

    @Test
    void sideViewFloorBounceUsesComplementThenShiftAndDoesNotReverseAgain() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.startEnemyBomb(0, 0x18, 0x1E, 0);
        RoomEntity bounced = motion.advance(entity(0, 2, 0x40, 0x53, 0), true,
            (candidate, direction, x, y) -> direction == 3).entity();
        assertEquals(0x55, bounced.y());
        assertEquals(0xEF, motion.speedY(0));
        assertEquals(0x0C, motion.speedX(0));
    }

    @Test
    void sideViewFloorDetectsContactEvenBeforeFractionalSpeedMovesOnePixel() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.startEnemyBomb(0, 0x18, 0, 0);
        RoomEntity settled = motion.advance(entity(0, 2, 0x40, 0x55, 0), true,
            (candidate, direction, x, y) -> direction == 3).entity();
        assertEquals(0x55, settled.y());
        assertEquals(0, motion.speedY(0));
        assertEquals(0, motion.speedX(0));
    }

    @Test
    void wallNegationWrapsAsAByteBeforeArithmeticShifts() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.startEnemyBomb(0, 0x80, 0, 0);
        motion.advance(entity(0, 2, 0x40, 0x50, 0x10), false,
            (candidate, direction, x, y) -> direction == 1);
        assertEquals(0xF0, motion.speedX(0));
    }

    private static RoomEntity entity(int slot, int type, int x, int y, int z) {
        EntitySpriteDefinition definition = new EntitySpriteDefinition(
            type, 0, 0, EntitySpriteDefinition.Shape.PAIR, 0,
            java.util.List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0, 0),
                new EntitySpriteDefinition.OamAttribute(2, 0))));
        return new RoomEntity(slot, 0, type, x, y, EntityStatus.THROWN,
            definition, 0, 0, 0, z);
    }
}
