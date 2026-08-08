package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MimicMotionTest {

    @Test
    void horizontalInputUsesTheRomSpeedTableAndFrameVariantBit() {
        RoomEntity entity = entity(0x40, 0x50, 0);
        MimicMotion motion = new MimicMotion();
        motion.initialize(entity.slot());

        MimicMotion.Update left = motion.advance(entity, 0x00, 0x01, 0, null);
        assertEquals(0x40, left.entity().x());
        assertEquals(0x50, left.entity().y());
        assertEquals(0xF0, motion.speedX(entity.slot()));
        assertEquals(0x00, motion.speedY(entity.slot()));
        assertEquals(4, left.entity().spriteVariant());

        MimicMotion.Update animated = motion.advance(left.entity(), 0x08, 0x01, 0, null);
        assertEquals(0x3F, animated.entity().x());
        assertEquals(5, animated.entity().spriteVariant());
    }

    @Test
    void verticalInputUsesTheRomBitManipulationForDirectionAndVariant() {
        RoomEntity entity = entity(0x40, 0x50, 0);
        MimicMotion motion = new MimicMotion();
        motion.initialize(entity.slot());

        MimicMotion.Update up = motion.advance(entity, 0x00, 0x04, 0, null);
        assertEquals(0x10, motion.speedY(entity.slot()));
        assertEquals(0x00, motion.speedX(entity.slot()));
        assertEquals(0, up.entity().spriteVariant());

        MimicMotion.Update down = motion.advance(up.entity(), 0x08, 0x08, 0, null);
        assertEquals(0xF0, motion.speedY(entity.slot()));
        assertEquals(3, down.entity().spriteVariant());
    }

    @Test
    void collisionClearsSpeedAfterThePositionStepAndDoesNotReadNewInput() {
        RoomEntity entity = entity(0x40, 0x50, 4);
        MimicMotion motion = new MimicMotion();
        motion.initialize(entity.slot());
        motion.setSpeedForTest(entity.slot(), 0xF0, 0x00);

        MimicMotion.Update update = motion.advance(entity, 0x00, 0x02, 0x01, null);

        assertEquals(0x3F, update.entity().x());
        assertEquals(0x50, update.entity().y());
        assertEquals(0, motion.speedX(entity.slot()));
        assertEquals(0, motion.speedY(entity.slot()));
        assertEquals(4, update.entity().spriteVariant());
    }

    @Test
    void backgroundBlockRollsBackTheFixedPointStepBeforeClearingSpeed() {
        RoomEntity entity = entity(0x40, 0x50, 4);
        MimicMotion motion = new MimicMotion();
        motion.initialize(entity.slot());
        motion.setSpeedForTest(entity.slot(), 0x10, 0x00);

        MimicMotion.Update update = motion.advance(entity, 0x00, 0x00, 0,
            (candidate, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction,
                    EntityBackgroundCollisionResult.NO_OBJECT, 0, nextX, nextY));

        assertEquals(0x40, update.entity().x());
        assertEquals(0x50, update.entity().y());
        assertEquals(0, motion.speedX(entity.slot()));
        assertEquals(0, motion.speedY(entity.slot()));
        assertEquals(4, update.entity().spriteVariant());
    }

    private static RoomEntity entity(int x, int y, int variant) {
        return new RoomEntity(0, 0, 0x28, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x28), variant);
    }
}
