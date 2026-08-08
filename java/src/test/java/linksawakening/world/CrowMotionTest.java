package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrowMotionTest {

    @Test
    void waitsForLinkThenStartsTheRomTakeoffSequence() {
        CrowMotion motion = new CrowMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);

        CrowMotion.Update triggered = motion.advance(entity, 0, 0x44, 0x50);

        assertEquals(1, motion.state(0));
        assertEquals(0x22, triggered.transitionCountdown());
        assertEquals(0x12, triggered.physicsFlags());
        assertEquals(0x4C, triggered.entity().y());
        assertEquals(2, triggered.entity().spriteVariant());
        assertFalse(triggered.unloaded());
        assertFalse(triggered.boomerangSound());
    }

    @Test
    void countdownClimbThenHomingAndOutwardFlightUseRomFixedPointSpeeds() {
        CrowMotion motion = new CrowMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);

        CrowMotion.Update update = motion.advance(entity, 0, 0x44, 0x50);
        update = motion.advance(update.entity(), 0x21, 0x44, 0x50);
        assertEquals(0x08, motion.speedZ(0));
        assertTrue(update.boomerangSound());
        assertEquals(0, update.entity().z());

        update = motion.advance(update.entity(), 0, 0x44, 0x50);
        assertEquals(2, motion.state(0));
        assertEquals(0x30, update.transitionCountdown());

        update = motion.advance(update.entity(), 0x2F, 0x60, 0x50);
        assertEquals(0x00, motion.speedX(0));
        assertEquals(0x40, update.entity().x());
        assertTrue(update.boomerangSound());

        update = motion.advance(update.entity(), 0x2E, 0x60, 0x50);
        assertEquals(0x01, motion.speedX(0));
        assertEquals(0x01, motion.speedY(0));
        assertEquals(0x40, update.entity().x());
        assertTrue(update.boomerangSound());

        motion.setStateForTest(0, 3);
        motion.setSpeedForTest(0, 0x20, 0, 0);
        RoomEntity outwardStart = new RoomEntity(0, 0, CrowMotion.ENTITY_TYPE,
            0xA6, 0x50, EntityStatus.ACTIVE, definition(), 0);
        CrowMotion.Update unloaded = motion.advance(outwardStart, 0, 0x40, 0x50);
        assertTrue(unloaded.unloaded());
    }

    @Test
    void vectorSteeringApproachesTargetByOneRomSpeedUnit() {
        CrowMotion motion = new CrowMotion();
        motion.setStateForTest(0, 2);
        motion.setSpeedForTest(0, 0, 0, 0);
        RoomEntity entity = entity(0x40, 0x50, 0);

        CrowMotion.Update update = motion.advance(entity, 0x02, 0x60, 0x60);

        assertEquals(1, motion.speedX(0));
        assertEquals(1, motion.speedY(0));
        assertEquals(0x40, update.entity().x());
        assertEquals(0x50, update.entity().y());
    }

    private static RoomEntity entity(int x, int y, int variant) {
        return new RoomEntity(0, 0, CrowMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, definition(), variant);
    }

    private static EntitySpriteDefinition definition() {
        EntitySpriteDefinition.OamAttribute first =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        EntitySpriteDefinition.OamAttribute second =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        return new EntitySpriteDefinition(CrowMotion.ENTITY_TYPE, 0x06, 0x5C89,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second)));
    }
}
