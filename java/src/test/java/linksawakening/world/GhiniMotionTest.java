package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GhiniMotionTest {
    private static final int HIDING_GHINI = 0x10;
    private static final int GIANT_GHINI = 0x11;
    private static final int ORDINARY_GHINI = 0x12;

    @Test
    void initializesHidingAndGiantGhinisHiddenButOrdinaryGhiniVisible() {
        GhiniMotion motion = new GhiniMotion();

        motion.initialize(0, HIDING_GHINI);
        assertTrue(motion.hidden(0));
        assertEquals(1, motion.state(0));

        motion.initialize(1, GIANT_GHINI);
        assertTrue(motion.hidden(1));
        assertEquals(1, motion.state(1));

        motion.initialize(2, ORDINARY_GHINI);
        assertFalse(motion.hidden(2));
        assertEquals(0, motion.state(2));

        motion.initialize(3);
        assertFalse(motion.hidden(3));
    }

    @Test
    void hiddenGhiniStaysStationaryAndInvisibleWithoutARevealSignal() {
        GhiniMotion motion = new GhiniMotion();
        motion.initialize(0, HIDING_GHINI);
        RoomEntity source = entity(0, HIDING_GHINI, 0x50, 0x50, 0x08,
            pairDefinition(HIDING_GHINI), 0x02);

        RoomEntity far = motion.advance(source, 0, HIDING_GHINI,
            0x80, 0x80, 0, noRandom());
        assertHidden(source, far);
        assertTrue(motion.hidden(0));

        RoomEntity nearWithoutCollision = motion.advance(far, 1, HIDING_GHINI,
            0x5F, 0x5F, 0, noRandom());
        assertHidden(source, nearWithoutCollision);
        assertTrue(motion.hidden(0));

        RoomEntity exactlyAtWindowEdge = motion.advance(nearWithoutCollision, 2,
            HIDING_GHINI, 0x60, 0x5F, 1, noRandom());
        assertHidden(source, exactlyAtWindowEdge);
        assertTrue(motion.hidden(0));
    }

    @Test
    void hiddenGhiniRevealsOnWrappedNearCollisionButRevealTickRemainsHidden() {
        GhiniMotion motion = new GhiniMotion();
        motion.initialize(0, HIDING_GHINI);
        RoomEntity source = entity(0, HIDING_GHINI, 0xFE, 0xFE, 0x08,
            pairDefinition(HIDING_GHINI), 0x02);

        RoomEntity revealTick = motion.advance(source, 0, HIDING_GHINI,
            0x02, 0x02, 1, noRandom());

        assertEquals(-1, revealTick.spriteVariant());
        assertFalse(motion.hidden(0));
        assertEquals(0, motion.state(0));
        assertEquals(0x30, motion.privateCountdown2(0));
        assertEquals(source.x(), revealTick.x());
        assertEquals(source.y(), revealTick.y());
        assertEquals(source.z(), revealTick.z());
    }

    @Test
    void firstVisibleTickAfterRevealMovesAndCorrectsZButSkipsTargetRefresh() {
        GhiniMotion motion = new GhiniMotion();
        motion.initialize(0, HIDING_GHINI);
        RoomEntity source = entity(0, HIDING_GHINI, 0x50, 0x50, 0x0E,
            pairDefinition(HIDING_GHINI), 0x02);

        RoomEntity revealTick = motion.advance(source, 0, HIDING_GHINI,
            0x50, 0x50, 1, noRandom());
        RoomEntity visibleTick = motion.advance(revealTick, 4, HIDING_GHINI,
            0x50, 0x50, 0, noRandom());

        assertEquals(0x2F, motion.privateCountdown2(0));
        assertEquals(0x0F, visibleTick.z());
        assertEquals(source.x(), visibleTick.x());
        assertEquals(source.y(), visibleTick.y());
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0, motion.transitionCountdown(0));
        assertEquals(0, motion.privateCountdown1(0));
        assertEquals(0, visibleTick.spriteVariant());
    }

    @Test
    void visibleGhiniKeepsRomTargetTimersDirectionsAndFirstSpeedIncrements() {
        GhiniMotion motion = new GhiniMotion();
        motion.initialize(0, ORDINARY_GHINI);
        RoomEntity source = entity(0, ORDINARY_GHINI, 0x50, 0x50, 0x10,
            pairDefinition(ORDINARY_GHINI), 0x02);
        IntSupplier random = sequence(0x00, 0x00);

        RoomEntity first = motion.advance(source, 0, ORDINARY_GHINI,
            0, 0, 0, random);
        assertEquals(0x20, motion.transitionCountdown(0));
        assertEquals(0x18, motion.privateCountdown1(0));
        assertEquals(0, motion.targetXDirection(0));
        assertEquals(0, motion.targetYDirection(0));
        assertEquals(0x01, motion.speedX(0));
        assertEquals(0x01, motion.speedY(0));

        motion.advance(first, 1, ORDINARY_GHINI, 0, 0, 0, random);
        assertEquals(0x1F, motion.transitionCountdown(0));
        assertEquals(0x17, motion.privateCountdown1(0));
        assertEquals(0x01, motion.speedX(0));
        assertEquals(0x01, motion.speedY(0));
    }

    @Test
    void targetXSpeedSignSelectsXFlipAgainstTheBaseAttribute() {
        RoomEntity source = entity(0, ORDINARY_GHINI, 0x50, 0x50, 0x10,
            pairDefinition(ORDINARY_GHINI), 0x02);

        GhiniMotion negativeMotion = new GhiniMotion();
        negativeMotion.initialize(0, ORDINARY_GHINI);
        RoomEntity negative = negativeMotion.advance(source, 0, ORDINARY_GHINI,
            0, 0, 0, sequence(0x01, 0x00));
        assertEquals(1, negativeMotion.targetXDirection(0));
        assertTrue((negativeMotion.speedX(0) & 0x80) != 0);
        assertEquals(0x02, negative.entityFlipAttribute());
        assertEquals(0, negative.entityFlipAttribute() & 0x20);

        GhiniMotion positiveMotion = new GhiniMotion();
        positiveMotion.initialize(0, ORDINARY_GHINI);
        RoomEntity positive = positiveMotion.advance(source, 0, ORDINARY_GHINI,
            0, 0, 0, sequence(0x00, 0x00));
        assertEquals(0, positiveMotion.targetXDirection(0));
        assertEquals(0x01, positiveMotion.speedX(0));
        assertEquals(0x22, positive.entityFlipAttribute());
        assertNotEquals(0, positive.entityFlipAttribute() & 0x20);
    }

    @Test
    void giantVariantAddsOrientationOffsetAndStripsGenericXFlip() {
        GhiniMotion motion = new GhiniMotion();
        motion.initialize(0, GIANT_GHINI);
        RoomEntity current = entity(0, GIANT_GHINI, 0x50, 0x50, 0x10,
            giantDefinition(), 0x02);

        current = motion.advance(current, 0, GIANT_GHINI,
            0x50, 0x50, 1, noRandom());
        assertEquals(-1, current.spriteVariant());
        assertEquals(0x30, motion.privateCountdown2(0));

        IntSupplier random = sequence(0x00, 0x00);
        for (int frame = 1; frame <= 0x40; frame++) {
            current = motion.advance(current, frame, GIANT_GHINI,
                0x50, 0x50, 1, random);
        }

        assertEquals(2, motion.giantRectangleVariant(0));
        assertEquals(2, current.spriteVariant());
        assertEquals(0, current.entityFlipAttribute() & 0x20);
        assertEquals(0x02, current.entityFlipAttribute());

        current = motion.advance(current, 0x50, GIANT_GHINI,
            0x50, 0x50, 1, random);
        assertEquals(3, motion.giantRectangleVariant(0));
        assertEquals(3, current.spriteVariant());
        assertEquals(0, current.entityFlipAttribute() & 0x20);
    }

    private static void assertHidden(RoomEntity source, RoomEntity result) {
        assertEquals(-1, result.spriteVariant());
        assertEquals(source.x(), result.x());
        assertEquals(source.y(), result.y());
        assertEquals(source.z(), result.z());
    }

    private static RoomEntity entity(int slot, int type, int x, int y, int z,
                                     EntitySpriteDefinition definition, int flipAttribute) {
        return new RoomEntity(slot, slot, type, x, y, EntityStatus.ACTIVE,
            definition, definition.initialVariant(), flipAttribute, 0, z);
    }

    private static EntitySpriteDefinition pairDefinition(int type) {
        return new EntitySpriteDefinition(type, 0x04, 0x5BFC,
            EntitySpriteDefinition.Shape.PAIR, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x58, 0x02),
                    new EntitySpriteDefinition.OamAttribute(0x5A, 0x02)),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5C, 0x02),
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x02))));
    }

    private static EntitySpriteDefinition giantDefinition() {
        return new EntitySpriteDefinition(GIANT_GHINI, 0x04, 0x5D26,
            EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(), List.of(
                giantVariant(0x60),
                giantVariant(0x62),
                giantVariant(0x64),
                giantVariant(0x66)));
    }

    private static List<EntitySpriteDefinition.RectangleSprite> giantVariant(int tile) {
        return List.of(new EntitySpriteDefinition.RectangleSprite(-8, -8,
            new EntitySpriteDefinition.OamAttribute(tile, 0x02)));
    }

    private static IntSupplier noRandom() {
        return () -> {
            throw new AssertionError("hidden Ghini must not consume random bytes");
        };
    }

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }
}
