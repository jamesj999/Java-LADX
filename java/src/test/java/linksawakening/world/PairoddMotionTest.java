package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PairoddMotionTest {

    @Test
    void initializerConsumesOneRandomDirectionByteAndRestingAnimationUsesFrameBitFour() {
        PairoddMotion motion = new PairoddMotion();
        AtomicInteger randomCalls = new AtomicInteger();
        IntSupplier random = () -> {
            randomCalls.incrementAndGet();
            return 0x03;
        };
        RoomEntity entity = entity(0x40, 0x50);

        motion.initialize(0, random);
        PairoddMotion.Update frame0 = motion.advance(entity, 0, 0, 0, random, false);
        PairoddMotion.Update frame16 = motion.advance(frame0.entity(), 0x10,
            0, 0, random, false);

        assertEquals(1, randomCalls.get());
        assertEquals(0, frame0.entity().spriteVariant());
        assertEquals(1, frame16.entity().spriteVariant());
        assertFalse(frame0.spawnProjectile());
        assertEquals(0, motion.state(0));
        assertEquals(0, motion.transitionCountdown(0));
    }

    @Test
    void restingPairoddStartsDisappearingOnlyInsideBothSignedLinkWindows() {
        PairoddMotion motion = new PairoddMotion();
        RoomEntity entity = entity(0x40, 0x50);
        motion.initialize(0, () -> 0);

        PairoddMotion.Update outside = motion.advance(entity, 0, 0x60, 0x70,
            () -> 0, false);
        assertEquals(0, motion.state(0));
        assertEquals(0, motion.transitionCountdown(0));

        PairoddMotion.Update inside = motion.advance(outside.entity(), 1, 0x5F, 0x6F,
            () -> 0, false);

        assertEquals(1, motion.state(0));
        assertEquals(0x20, motion.transitionCountdown(0));
        assertEquals(0x40, inside.entity().x());
        assertEquals(0x50, inside.entity().y());
    }

    @Test
    void projectileFrameAndFlashThresholdsDoNotStartTeleportAtTheWrongTime() {
        PairoddMotion motion = new PairoddMotion();
        RoomEntity entity = entity(0x40, 0x50);
        motion.initialize(0, () -> 0);

        PairoddMotion.Update flashed = motion.advance(entity, 0, 0x40, 0x50,
            () -> 0, true);
        assertEquals(0, motion.state(0));
        assertEquals(0, motion.transitionCountdown(0));
        assertFalse(flashed.spawnProjectile());

        PairoddMotion.Update started = motion.advance(flashed.entity(), 1, 0x40, 0x50,
            () -> 0, false);
        assertEquals(1, motion.state(0));
        assertEquals(0x20, motion.transitionCountdown(0));

        PairoddMotion.Update update = started;
        while (motion.state(0) == 1) {
            update = motion.advance(update.entity(), 0x20, 0x00, 0x00,
                () -> 0, false);
        }

        while (motion.state(0) == 2) {
            update = motion.advance(update.entity(), 0x20, 0x00, 0x00,
                () -> 0, false);
        }
        assertEquals(0, motion.state(0));
        assertEquals(0x30, motion.transitionCountdown(0));

        PairoddMotion.Update atSpawn = update;
        for (int frame = 0; frame < 24; frame++) {
            atSpawn = motion.advance(atSpawn.entity(), frame, 0x00, 0x00,
                () -> 0, false);
        }
        assertEquals(0x18, motion.transitionCountdown(0),
            () -> "timer=" + Integer.toHexString(motion.transitionCountdown(0))
                + " state=" + motion.state(0));
        assertTrue(atSpawn.spawnProjectile());
    }

    @Test
    void enemyCollisionIsOnlyEnabledDuringTheRomVisiblePairoddPhases() {
        PairoddMotion motion = new PairoddMotion();
        RoomEntity entity = entity(0x40, 0x50);
        motion.initialize(0, () -> 0);

        PairoddMotion.Update update = motion.advance(entity, 0, 0x40, 0x50,
            () -> 0, false);
        assertEquals(1, motion.state(0));
        assertEquals(0x20, motion.transitionCountdown(0));
        assertTrue(motion.allowsEnemyCollision(0));

        while (motion.transitionCountdown(0) >= 0x18) {
            update = motion.advance(update.entity(), 1, 0, 0, () -> 0, false);
        }
        assertEquals(0x17, motion.transitionCountdown(0));
        assertFalse(motion.allowsEnemyCollision(0));

        while (motion.state(0) == 1) {
            update = motion.advance(update.entity(), 1, 0, 0, () -> 0, false);
        }
        assertEquals(2, motion.state(0));
        assertFalse(motion.allowsEnemyCollision(0));
    }

    @Test
    void disappearingAndReappearingUseRomVariantTablesAndMirroredCoordinates() {
        PairoddMotion motion = new PairoddMotion();
        RoomEntity entity = entity(0x40, 0x50);
        motion.initialize(0, () -> 0);

        PairoddMotion.Update update = motion.advance(entity, 0, 0x40, 0x50,
            () -> 0, false);
        for (int frame = 1; frame <= 9; frame++) {
            update = motion.advance(update.entity(), frame, 0, 0, () -> 0, false);
        }
        assertEquals(2, update.entity().spriteVariant());
        assertEquals(1, motion.state(0));
        assertEquals(0x17, motion.transitionCountdown(0));

        while (motion.state(0) == 1) {
            update = motion.advance(update.entity(), 0x40, 0, 0, () -> 0, false);
        }

        assertEquals(-1, update.entity().spriteVariant());
        assertEquals(2, motion.state(0));
        assertEquals(0x60, update.entity().x());
        assertEquals(0x40, update.entity().y());
        assertEquals(0x40, motion.transitionCountdown(0));

        while (motion.state(0) == 2 && motion.transitionCountdown(0) >= 0x18) {
            update = motion.advance(update.entity(), 0x40, 0, 0, () -> 0, false);
        }
        assertEquals(4, update.entity().spriteVariant());

        while (motion.state(0) == 2) {
            update = motion.advance(update.entity(), 0x40, 0, 0, () -> 0, false);
        }
        assertEquals(0, motion.state(0));
        assertEquals(0x30, motion.transitionCountdown(0));
        assertEquals(-1, update.entity().spriteVariant());

        update = motion.advance(update.entity(), 0x00, 0, 0, () -> 0, false);
        assertEquals(0, update.entity().spriteVariant());
    }

    @Test
    void clearResetsPairoddState() {
        PairoddMotion motion = new PairoddMotion();
        motion.initialize(0, () -> 0);
        motion.advance(entity(0x40, 0x50), 0, 0x40, 0x50, () -> 0, false);

        motion.clear(0);

        assertEquals(0, motion.state(0));
        assertEquals(0, motion.transitionCountdown(0));
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, 0, 0x57, x, y, EntityStatus.ACTIVE,
            pairDefinition(), 0);
    }

    private static EntitySpriteDefinition pairDefinition() {
        return new EntitySpriteDefinition(0x57, 0x04, 0x5DD1,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(
                new EntitySpriteDefinition.Variant(attribute(0x70), attribute(0x72)),
                new EntitySpriteDefinition.Variant(attribute(0x72), attribute(0x70)),
                new EntitySpriteDefinition.Variant(attribute(0x74), attribute(0x74)),
                new EntitySpriteDefinition.Variant(attribute(0x00), attribute(0x00)),
                new EntitySpriteDefinition.Variant(attribute(0x7A), attribute(0x7A)),
                new EntitySpriteDefinition.Variant(attribute(0xFF), attribute(0xFF)),
                new EntitySpriteDefinition.Variant(attribute(0x76), attribute(0x78)),
                new EntitySpriteDefinition.Variant(attribute(0x78), attribute(0x76))));
    }

    private static EntitySpriteDefinition.OamAttribute attribute(int tile) {
        return new EntitySpriteDefinition.OamAttribute(tile, 0);
    }
}
