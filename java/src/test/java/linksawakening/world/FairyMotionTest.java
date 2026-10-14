package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        FairyMotion.Update far = motion.advance(entity, 0x00, 0x60, 0x50,
            () -> {
                reads.incrementAndGet();
                return 0xFF;
            }, 0);

        assertEquals(0x40, far.entity().x());
        assertEquals(0x40, far.entity().y());
        assertEquals(0x09, motion.speedX(0));
        assertEquals(0x09, motion.speedY(0));
        assertEquals(0, reads.get());

        IntSupplier random = () -> reads.getAndIncrement() == 0 ? 0x0F : 0x00;
        FairyMotion.Update close = motion.advance(far.entity(), 0x01, 0x50, 0x40,
            random, far.transitionCountdown());

        assertEquals(0x30, close.transitionCountdown());
        assertEquals(0x07, motion.speedX(0));
        assertEquals(0xF8, motion.speedY(0));
        assertEquals(2, reads.get());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 32, true", "31, 32, true", "32, 0, true", "32, 31, true",
        "32, 32, false", "255, 32, false", "32, 255, false",
        "255, 31, true", "31, 255, true"
    })
    void eitherUnsignedAxisWindowStartsRandomHover(int dx, int dy, boolean hover) {
        FairyMotion motion = new FairyMotion();
        AtomicInteger reads = new AtomicInteger();
        // GetEntityYDistanceToLink_03 measures from Y - Z; $40 - $10 = $30.
        FairyMotion.Update update = motion.advance(entity(0x40, 0x40, 0x10), 1,
            (0x40 + dx) & 0xFF, (0x30 + dy) & 0xFF,
            () -> { reads.incrementAndGet(); return 0x0F; }, 0);

        assertEquals(hover ? 0x30 : 0, update.transitionCountdown());
        assertEquals(hover ? 2 : 0, reads.get());
        if (hover) {
            assertEquals(7, motion.speedX(0));
            assertEquals(7, motion.speedY(0));
        }
    }

    @Test
    void oneCloseAxisKeepsExistingVelocityWhileHoverCountdownRuns() {
        FairyMotion motion = new FairyMotion();
        motion.setSpeed(0, 0x07, 0xF8);
        FairyMotion.Update update = motion.advance(entity(0x40, 0x40, 0x10), 1,
            0x50, 0x70, () -> { throw new AssertionError("Hover timer is still active"); }, 5);

        assertEquals(5, update.transitionCountdown());
        assertEquals(0x07, motion.speedX(0));
        assertEquals(0xF8, motion.speedY(0));
    }

    @ParameterizedTest
    @CsvSource({"96, 80, 9, 9", "96, 0, 6, 247", "16, 80, 247, 6"})
    void attractionUsesTheFairysVisualHeight(int linkX, int linkY, int speedX, int speedY) {
        FairyMotion motion = new FairyMotion();
        motion.advance(entity(0x40, 0x40, 0x10), 1, linkX, linkY,
            () -> { throw new AssertionError("Both axes are outside the hover window"); }, 0);

        assertEquals(speedX, motion.speedX(0));
        assertEquals(speedY, motion.speedY(0));
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
