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
    void wallResponseUsesTheRomNegateAndQuarterSpeed() {
        ThrownEntityMotion motion = new ThrownEntityMotion();
        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT, 0x05, false);
        RoomEntity entity = entity(0, 0x05, 0x20, 0x30, 0);

        RoomEntity updated = motion.advance(entity, false,
            (candidate, direction, nextX, nextY) -> direction == 0).entity();

        assertEquals(0x20, updated.x());
        assertEquals(0xFA, motion.speedX(0));
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
