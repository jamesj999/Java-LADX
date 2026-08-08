package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FishMotionTest {

    @Test
    void followsTheRomHiddenSwimLaunchAndLandingCycle() {
        FishMotion motion = new FishMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);

        FishMotion.Update started = motion.advance(entity, 0, () -> 0, null);
        assertEquals(1, motion.state(0));
        assertEquals(0x08, motion.speedX(0));
        assertEquals(0x40, started.transitionCountdown());
        assertEquals(0, started.entity().spriteVariant());
        assertEquals(0x52, started.physicsFlags());
        assertFalse(started.waterSplash());

        FishMotion.Update swimming = motion.advance(
            started.entity(), started.transitionCountdown() - 1, () -> 0, null);
        assertEquals(6, swimming.entity().spriteVariant());

        FishMotion.Update launched = motion.advance(swimming.entity(), 0, () -> 0, null);
        assertEquals(2, motion.state(0));
        assertEquals(0x10, motion.speedX(0));
        assertEquals(0x18, motion.speedZ(0));
        assertEquals(0x12, launched.physicsFlags());
        assertTrue(launched.waterSplash());

        FishMotion.Update update = launched;
        boolean landed = false;
        for (int frame = 0; frame < 0x80; frame++) {
            update = motion.advance(update.entity(), 0, () -> 0, null);
            if (update.waterSplash()) {
                landed = true;
                break;
            }
        }

        assertTrue(landed);
        assertEquals(1, motion.state(0));
        assertEquals(0x08, motion.speedX(0));
        assertEquals(0x00, update.entity().z());
        assertEquals(0x50, update.transitionCountdown());
        assertEquals(0x52, update.physicsFlags());
    }

    @Test
    void reversesHorizontalSpeedOnlyWhenTheRomCollisionByteIsSet() {
        FishMotion motion = new FishMotion();
        FishMotion.Update started = motion.advance(entity(0x40, 0x50, 0),
            0, () -> 0, null);
        FishMotion.Update swimming = motion.advance(started.entity(), 1,
            () -> 0, null);

        FishMotion.Update blocked = motion.advance(swimming.entity(), 1,
            () -> 0, (entity, direction, nextX, nextY) -> {
                assertEquals(EntityBackgroundCollisionResult.RIGHT, direction);
                return true;
            });

        assertEquals(0xF8, motion.speedX(0));
        assertEquals(5, blocked.entity().spriteVariant());
        assertEquals(0x40, blocked.entity().x());
    }

    private static RoomEntity entity(int x, int y, int variant) {
        return new RoomEntity(0, 0, FishMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, definition(), variant);
    }

    private static EntitySpriteDefinition definition() {
        EntitySpriteDefinition.OamAttribute first =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        EntitySpriteDefinition.OamAttribute second =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        return new EntitySpriteDefinition(FishMotion.ENTITY_TYPE, 0x15, 0x449F,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, null),
                new EntitySpriteDefinition.Variant(first, null),
                new EntitySpriteDefinition.Variant(first, null),
                new EntitySpriteDefinition.Variant(first, null)));
    }
}
