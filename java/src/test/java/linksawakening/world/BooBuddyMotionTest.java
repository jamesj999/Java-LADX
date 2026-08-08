package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BooBuddyMotionTest {

    @Test
    void driftsWithRomRandomVectorAndFourUnitAcceleration() {
        BooBuddyMotion motion = new BooBuddyMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);

        BooBuddyMotion.Update first = motion.advance(entity, 0, 0x60, 0x50,
            0, 0, false, () -> 0);

        assertEquals(4, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0x40, first.entity().x());
        assertEquals(0, first.entity().spriteVariant());
        assertFalse(first.unloaded());
        assertTrue(motion.allowsEnemyCollision(0, 0, 0, false));
        assertFalse(motion.allowsSwordCollision(0, 0));

        BooBuddyMotion.Update second = motion.advance(first.entity(), 1, 0x60, 0x50,
            0, 0, false, () -> 0);
        assertEquals(8, motion.speedX(0));
        assertEquals(0x40, second.entity().x());
    }

    @Test
    void roomTriggerMovesToFleeStateAndAimsAwayFromLink() {
        BooBuddyMotion motion = new BooBuddyMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);

        BooBuddyMotion.Update triggered = motion.advance(entity, 0, 0x60, 0x50,
            0, 1, false, () -> 0);
        assertEquals(1, motion.state(0));
        assertFalse(motion.allowsEnemyCollision(0, 0, 0, false));
        assertTrue(motion.allowsSwordCollision(0, 1));
        assertEquals(-1, triggered.healthOverride());

        BooBuddyMotion.Update fleeing = motion.advance(triggered.entity(), 1,
            0x60, 0x50, 0, 1, false, () -> 0);
        assertEquals(0xFC, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(4, fleeing.entity().spriteVariant());
        assertEquals(1, fleeing.healthOverride());
        assertTrue(motion.allowsEnemyCollision(0, 1, 0, false));
    }

    @Test
    void transitionAnimationUsesTheHiddenFrameAndClearsPrivateState() {
        BooBuddyMotion motion = new BooBuddyMotion();
        motion.setPrivateStateForTest(0, 1);
        RoomEntity entity = entity(0x30, 0x40, 0);

        BooBuddyMotion.Update hidden = motion.advance(entity, 0, 0x60, 0x40,
            1, 0, false, () -> 0);
        assertEquals(-1, hidden.entity().spriteVariant());
        assertEquals(0x30, hidden.entity().x());
        assertEquals(0x40, hidden.entity().y());
        assertEquals(0, motion.privateCountdown1(0));
    }

    @Test
    void unloadsWhenTheSharedBankSixBoundsHelperIsReached() {
        BooBuddyMotion motion = new BooBuddyMotion();
        motion.setSpeedForTest(0, 0x10, 0, 0);
        RoomEntity entity = entity(0xA7, 0x50, 0);

        BooBuddyMotion.Update update = motion.advance(entity, 0, 0x40, 0x50,
            0, 0, false, () -> 0);

        assertTrue(update.unloaded());
    }

    private static RoomEntity entity(int x, int y, int variant) {
        return new RoomEntity(0, 0, BooBuddyMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, definition(), variant);
    }

    private static EntitySpriteDefinition definition() {
        EntitySpriteDefinition.OamAttribute first =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        EntitySpriteDefinition.OamAttribute second =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        return new EntitySpriteDefinition(BooBuddyMotion.ENTITY_TYPE, 0x06, 0x79A9,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second)));
    }
}
