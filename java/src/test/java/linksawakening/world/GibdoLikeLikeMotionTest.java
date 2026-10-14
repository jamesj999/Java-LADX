package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GibdoLikeLikeMotionTest {
    @ParameterizedTest
    @CsvSource({"0, 1, 0, 248, 0", "1, 2, 0, 8, 0",
        "2, 0, 0, 0, 8", "3, 0, 1, 0, 248"})
    void sharedWalkBouncesEvenWhenMovementHasNotAccumulatedAWholePixel(
        int direction, int firstRandom, int secondRandom, int speedX, int speedY) {
        GibdoMotion motion = new GibdoMotion();
        AtomicInteger randomCalls = new AtomicInteger();
        RoomEntity entity = motion.advance(entity(0x1F),
            () -> randomCalls.getAndIncrement() == 0 ? firstRandom : secondRandom, null);
        List<Integer> probes = new ArrayList<>();

        RoomEntity blocked = motion.advance(entity, () -> 1, (source, dir, x, y) -> {
            probes.add(dir);
            return true;
        });

        assertEquals(List.of(direction), probes);
        assertEquals(0x40, blocked.x());
        assertEquals(0x40, blocked.y());
        assertEquals(speedX, motion.speedX(0));
        assertEquals(speedY, motion.speedY(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0x1F, 0x23})
    void roomRuntimeAnimatesTheSharedWalkFromTheEntityCounter(int type) {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(slot == 0 ? entity(type) : RoomEntity.disabled(slot));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots));

        for (int pass = 0; pass <= 33; pass++) {
            // The global frame is deliberately offset from the new entity's
            // zeroed privateState4 counter. Rendering precedes $7F05's write.
            runtime.tick(0x88 + pass, 0xA0, 0xA0, () -> 1);
            assertEquals(pass >= 17 && pass <= 32 ? 1 : 0,
                runtime.snapshot().slots().get(0).spriteVariant(), "Handler pass " + pass);
        }
    }

    @Test
    void likeLikeCarriesItsWalkingAnimationCounterIntoCapture() {
        LikeLikeMotion motion = new LikeLikeMotion();
        RoomEntity entity = entity(0x23);
        for (int pass = 0; pass < 15; pass++) {
            LikeLikeMotion.Update update = motion.advance(entity, pass, 0xA0, 0xA0,
                0, 0, false, 0, 0, 1, 0, () -> 1, null);
            entity = update.entity();
        }
        LikeLikeMotion.Update caught = motion.advance(entity, 15, entity.x(), entity.y(),
            0, 0, false, 0, 0, 1, 0, () -> 1, null);
        assertEquals(1, motion.state(0));
        // Sixteen walking passes leave counter $10. Capture calls $7F05 twice,
        // selecting frame 1 for the following pass from the same counter.
        LikeLikeMotion.Update first = motion.advance(caught.entity(), 16, 0x40, 0x40,
            0, 0, false, 0, 0, 1, 0, () -> 1, null);
        assertEquals(0, first.spriteVariant());
        LikeLikeMotion.Update second = motion.advance(first.entity(), 17, 0x40, 0x40,
            0, 0, false, 0, 0, 1, 0, () -> 1, null);
        assertEquals(1, second.spriteVariant());
    }

    @ParameterizedTest
    @CsvSource({"1, 4, 4, 1, 0", "2, 4, 0, 1, 1", "5, 0, 4, 1, 0",
        "6, 0, 4, 2, -1", "1, 0, 0, 1, -1"})
    void captureHidesAndStealsBeforeCheckingLinksMotionState(
        int motionState, int itemA, int itemB, int shieldLevel, int stolenSlot) {
        LikeLikeMotion motion = new LikeLikeMotion();
        RoomEntity entity = entity(0x23);
        LikeLikeMotion.Update caught = motion.advance(entity, 1, 0x40, 0x40,
            0, 0, false, itemA, itemB, shieldLevel, 0, () -> 1, null);
        assertEquals(1, motion.state(0));

        LikeLikeMotion.Update hidden = motion.advance(caught.entity(), 2, 0x50, 0x60,
            0, motionState, false, itemA, itemB, shieldLevel, 0, () -> 1, null);
        assertEquals(RoomEntityRuntime.LikeLikeEvent.Kind.CAPTURE, hidden.event().kind());
        assertFalse(hidden.event().pinLinkPosition());
        assertEquals(stolenSlot, hidden.event().stolenInventorySlot());
        assertEquals(stolenSlot >= 0 ? shieldLevel : 0, motion.privateState1(0));

        LikeLikeMotion.Update resumed = motion.advance(hidden.entity(), 3, 0x50, 0x60,
            0, 0, false, itemA, itemB, shieldLevel, 0, () -> 1, null);
        assertTrue(resumed.event().pinLinkPosition());
        assertEquals(-1, resumed.event().stolenInventorySlot());
    }

    private static RoomEntity entity(int type) {
        EntitySpriteDefinition.OamAttribute tile =
            new EntitySpriteDefinition.OamAttribute(0, 0);
        EntitySpriteDefinition.Variant pair = new EntitySpriteDefinition.Variant(tile, tile);
        EntitySpriteDefinition definition = new EntitySpriteDefinition(type, 6,
            type == 0x1F ? 0x7E6F : 0x7DD4, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(pair, pair));
        return new RoomEntity(0, 0, type, 0x40, 0x40, EntityStatus.ACTIVE, definition, 0);
    }
}
