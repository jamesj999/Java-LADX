package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FairyMotionTest {

    @Test
    void followsTheRomCloseWindowAndRandomHoverSpeeds() {
        FairyMotion motion = new FairyMotion();
        motion.initialize(0);
        RoomEntity entity = entity(0x40, 0x40, 0x10);

        AtomicInteger reads = new AtomicInteger();
        FairyMotion.Update far = motion.advance(entity, 0x00, 0x60, 0x40,
            () -> {
                reads.incrementAndGet();
                return 0xFF;
            }, 0);

        assertEquals(0x40, far.entity().x());
        assertEquals(0x40, far.entity().y());
        assertEquals(0x09, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
        assertEquals(0, reads.get());

        IntSupplier random = () -> reads.getAndIncrement() == 0 ? 0x0F : 0x00;
        FairyMotion.Update close = motion.advance(far.entity(), 0x01, 0x50, 0x40,
            random, far.transitionCountdown());

        assertEquals(0x30, close.transitionCountdown());
        assertEquals(0x07, motion.speedX(0));
        assertEquals(0xF8, motion.speedY(0));
        assertEquals(2, reads.get());
    }

    @Test
    void bobsZTowardTheRomSixteenPixelTargetEveryFourthFrame() {
        FairyMotion motion = new FairyMotion();
        motion.initialize(0);
        RoomEntity entity = entity(0x40, 0x40, 0x0F);

        FairyMotion.Update up = motion.advance(entity, 0x00, 0x40, 0x40,
            () -> 0, 0);
        assertEquals(0x10, up.entity().z());

        FairyMotion.Update held = motion.advance(up.entity(), 0x01, 0x40, 0x40,
            () -> 0, 0);
        assertEquals(0x10, held.entity().z());

        FairyMotion.Update down = motion.advance(
            new RoomEntity(0, 0, FairyMotion.ENTITY_TYPE, 0x40, 0x40,
                EntityStatus.ACTIVE, definition(), 0, 0, 0, 0x11),
            0x04, 0x40, 0x40, () -> 0, 0);
        assertEquals(0x10, down.entity().z());
    }

    @Test
    void usesThePreMovementXSpeedBitForTheTwoRomSpriteVariants() {
        FairyMotion motion = new FairyMotion();
        motion.initialize(0);
        motion.setSpeedForTest(0, 0xF7, 0, 0);

        FairyMotion.Update update = motion.advance(entity(0x40, 0x40, 0x10),
            0x01, 0x40, 0x40, () -> 0, 0);

        assertEquals(1, update.entity().spriteVariant());
    }

    private static RoomEntity entity(int x, int y, int z) {
        return new RoomEntity(0, -1, FairyMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, definition(), 0, 0, 0, z);
    }

    private static EntitySpriteDefinition definition() {
        return new EntitySpriteDefinition(FairyMotion.ENTITY_TYPE, 0x03, 0x6157,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x21),
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x01)),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x21),
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x01))));
    }
}
