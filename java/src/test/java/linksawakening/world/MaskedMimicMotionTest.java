package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MaskedMimicMotionTest {

    @Test
    void selectsRomButtonSpeedsAndMovesOnTheSameFrame() {
        MaskedMimicMotion motion = new MaskedMimicMotion();
        RoomEntity entity = entity(0x40, 0x50);

        MaskedMimicMotion.Update update = motion.advance(
            entity, 0x50, 0x50, 0x00, 0x01, 0, null, 0, 0);

        assertEquals(0x3F, update.entity().x());
        assertEquals(0x50, update.entity().y());
        assertEquals(0xF4, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
        assertEquals(0x48, update.options1());
        assertEquals(0x02, update.entity().spriteVariant());
        assertEquals(0x01, motion.direction(0));
        assertEquals(0x01, motion.inertia(0));
    }

    @Test
    void supportsDiagonalInputAndLeavesPositionStillForZeroInput() {
        MaskedMimicMotion motion = new MaskedMimicMotion();
        RoomEntity entity = entity(0x40, 0x50);

        MaskedMimicMotion.Update diagonal = motion.advance(
            entity, 0x40, 0x60, 0x03, 0x05, 0, null, 0, 0);
        assertEquals(0xF4, motion.speedX(0));
        assertEquals(0x0C, motion.speedY(0));
        assertEquals(0x3F, diagonal.entity().x());
        assertEquals(0x50, diagonal.entity().y());

        MaskedMimicMotion.Update stopped = motion.advance(
            diagonal.entity(), 0x40, 0x60, 0x03, 0x00, 0, null, 0, 1);
        assertEquals(diagonal.entity().x(), stopped.entity().x());
        assertEquals(diagonal.entity().y(), stopped.entity().y());
        assertEquals(0xF4, motion.speedX(0));
        assertEquals(0x0C, motion.speedY(0));
        assertEquals(0x01, motion.inertia(0));
    }

    @Test
    void preservesTheUncheckedFourthEntriesOfBothRomSpeedTables() {
        MaskedMimicMotion motion = new MaskedMimicMotion();
        RoomEntity entity = entity(0x40, 0x50);

        motion.advance(entity, 0x40, 0x50, 0x00, 0x02, 0, null, 0, 0);
        assertEquals(0x0C, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));

        motion.clear(0);
        motion.advance(entity, 0x40, 0x50, 0x00, 0x08, 0, null, 0, 0);
        assertEquals(0x00, motion.speedX(0));
        assertEquals(0xF4, motion.speedY(0));

        motion.clear(0);
        motion.advance(entity, 0x40, 0x50, 0x00, 0x0C, 0, null, 0, 0);
        assertEquals(0xF0, motion.speedY(0));

        motion.clear(0);
        motion.advance(entity, 0x40, 0x50, 0x00, 0x03, 0, null, 0, 0);
        assertEquals(0x00, motion.speedX(0));
    }

    @Test
    void collisionByteReturnsBeforeSelectingNewInput() {
        MaskedMimicMotion motion = new MaskedMimicMotion();
        RoomEntity entity = entity(0x40, 0x50);

        MaskedMimicMotion.Update update = motion.advance(
            entity, 0x50, 0x50, 0x00, 0x01, 0x01, null, 0, 0);

        assertEquals(entity.x(), update.entity().x());
        assertEquals(0x00, motion.speedX(0));
        assertEquals(0x00, motion.inertia(0));
    }

    @Test
    void usesOldDirectionForOptionsAndTogglesTheRomInertiaBit() {
        MaskedMimicMotion motion = new MaskedMimicMotion();
        RoomEntity entity = entity(0x40, 0x50);

        MaskedMimicMotion.Update update = motion.advance(
            entity, 0x50, 0x50, 0x00, 0x01, 0, null, 0, 0);
        assertEquals(0x48, update.options1());

        update = motion.advance(update.entity(), 0x50, 0x50, 0x00,
            0x00, 0, null, 0, 1);
        assertEquals(0x08, update.options1());
        assertEquals(0x01, motion.direction(0));
        assertEquals(0x01, motion.inertia(0));

        for (int frame = 1; frame < 16; frame++) {
            update = motion.advance(update.entity(), 0x50, 0x50, 0x00,
                0x01, 0, null, 0, frame + 1);
        }
        assertEquals(0x03, update.entity().spriteVariant());
        assertEquals(0x10, motion.inertia(0));
    }

    @Test
    void backgroundBlockRollsBackCoordinatesButDoesNotSkipHandlerTail() {
        MaskedMimicMotion motion = new MaskedMimicMotion();
        RoomEntity entity = entity(0x40, 0x50);

        MaskedMimicMotion.Update update = motion.advance(
            entity, 0x50, 0x50, 0x00, 0x01, 0,
            (candidate, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0xFF, 0, nextX, nextY),
            0, 0);

        assertEquals(0x40, update.entity().x());
        assertEquals(0x50, update.entity().y());
        assertTrue(update.blocked());
        assertEquals(0x01, motion.direction(0));
        assertEquals(0x01, motion.inertia(0));
    }

    private static RoomEntity entity(int x, int y) {
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>();
        for (int variant = 0; variant < 8; variant++) {
            variants.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x60, 0x02),
                new EntitySpriteDefinition.OamAttribute(0x62, 0x02)));
        }
        EntitySpriteDefinition definition = new EntitySpriteDefinition(
            0x8F, 0x19, 0x4796, EntitySpriteDefinition.Shape.PAIR, 0, variants);
        return new RoomEntity(0, 0, 0x8F, x, y, EntityStatus.ACTIVE, definition, 0);
    }
}
