package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomEntityRuntimeTest {

    @Test
    void followsPieceOfPowerFrameDrivenPaletteVariant() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x33, 24, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0x00);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(0x08);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(0x10);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void staggersButterflyWingVariantsByEntitySlot() {
        EntitySpriteDefinition definition = new EntitySpriteDefinition(0x6E, 0x06, 0x6BBD,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x01), null),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x41), null)));
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x6E, 24, 32, EntityStatus.INIT, definition, 0),
            new RoomEntity(1, 1, 0x6E, 40, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0);

        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.snapshot().slots().get(1).spriteVariant());
    }

    @Test
    void butterflyUsesBankSixSignedFixedPointPositionUpdates() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x6E, 24, 32, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // State 0 selects +4 X speed; state 16 selects -4 Y speed.
        IntSupplier randomBytes = sequence(0x00, 0x01);
        for (int frame = 0; frame <= 17; frame++) {
            runtime.tick(frame, 200, 32, randomBytes);
        }

        RoomEntity butterfly = runtime.snapshot().slots().get(0);
        assertEquals(28, butterfly.x());
        assertEquals(31, butterfly.y());
    }

    @Test
    void butterflyAppliesTheTwoPixelVectorTowardLinkOnItsSixtyFourFramePhase() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x6E, 24, 32, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // The initial speed is zero. At state 0 the handler chooses a speed,
        // then writes the vector toward Link into its private state. The
        // subsequent 32-frame speed selection adds that private X component.
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00);
        runtime.tick(64, 40, 32, randomBytes);
        for (int frame = 65; frame <= 99; frame++) {
            runtime.tick(frame, 40, 32, randomBytes);
        }

        // The attraction vector is stored in private state at frame 64. The
        // frame-96 speed refresh therefore selects 4 + 2 = 6, which produces
        // its first pixel one frame earlier than a plain +4 speed.
        RoomEntity butterfly = runtime.snapshot().slots().get(0);
        assertEquals(33, butterfly.x());
        assertEquals(36, butterfly.y());
    }

    @Test
    void kidHandlersAnimateTheirTwoWalkingFramesEverySixteenFrames() {
        EntitySpriteDefinition definition = pairDefinition(0x70, 4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x70, 24, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(8);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(16);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(32);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void onlyTheFirstEightLoadOrdersContributeToThePersistentClearMask() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 3, 0x33, 24, 32, EntityStatus.ACTIVE, definition, 0),
            new RoomEntity(1, 8, 0x33, 40, 32, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        assertEquals(1 << 3, runtime.clearEntity(0));
        assertEquals(0, runtime.clearEntity(1));
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(1).status());
    }

    @Test
    void collectionUsesTheRomPickableTableAndFrameSlotCadence() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x2D, 24, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0);

        assertNull(runtime.collectIfNeeded(0, 24, 34, false, true));
        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);

        assertNotNull(pickup);
        assertEquals(0, pickup.slot());
        assertEquals(0x2D, pickup.type());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void collectionUsesThePickupHitboxEdgesFromHitboxPositions() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x2D, 24, 32, EntityStatus.ACTIVE, definition, 0));

        RoomEntityRuntime xInside = RoomEntityRuntime.from(initial);
        assertNotNull(xInside.collectIfNeeded(1, 34, 34, false, true));

        RoomEntityRuntime xOutside = RoomEntityRuntime.from(initial);
        assertNull(xOutside.collectIfNeeded(1, 35, 34, false, true));

        RoomEntityRuntime yInside = RoomEntityRuntime.from(initial);
        assertNotNull(yInside.collectIfNeeded(1, 24, 43, false, true));

        RoomEntityRuntime yOutside = RoomEntityRuntime.from(initial);
        assertNull(yOutside.collectIfNeeded(1, 24, 44, false, true));
    }

    @Test
    void collectionSkipsAirborneAndNonInteractiveLinkStates() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x2D, 24, 32, EntityStatus.ACTIVE, definition, 0));

        assertNull(RoomEntityRuntime.from(initial).collectIfNeeded(1, 24, 34, true, true));
        assertNull(RoomEntityRuntime.from(initial).collectIfNeeded(1, 24, 34, false, false));
    }

    @Test
    void transitionPickupsRemainInTheHeldStateAfterCollection() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x33, 24, 32, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);

        assertNotNull(pickup);
        assertEquals(1, pickup.persistentClearMask());
        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
        assertNull(runtime.collectIfNeeded(1, 24, 34, false, true));
    }

    @Test
    void indoorDroppablesUseTheRomSlowFadeAndUnloadAtZero() {
        EntitySpriteDefinition definition = pairDefinition(0x37, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x37, 24, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, true);

        runtime.tick(0);

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0x80, runtime.slowTransitionCountdown(0));

        int frame = 1;
        while (runtime.slowTransitionCountdown(0) > 3) {
            runtime.tick(frame++);
        }
        assertEquals(3, runtime.slowTransitionCountdown(0));

        while ((frame & 0x03) != 0) {
            runtime.tick(frame++);
        }
        runtime.tick(frame);

        assertEquals(2, runtime.slowTransitionCountdown(0));
        assertEquals(-1, runtime.snapshot().slots().get(0).spriteVariant());

        while (runtime.snapshot().slots().get(0).loaded()) {
            runtime.tick(frame++);
        }
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void swordCollectionTableMatchesTheSeventeenPickableEntries() {
        int[] canBeCollected = {0x2D, 0x2E, 0x31, 0x33, 0x34, 0x37, 0x38, 0x3B};
        int[] cannotBeCollected = {0x2F, 0x30, 0x32, 0x35, 0x36, 0x39, 0x3A, 0x3C, 0x3D};

        for (int type : canBeCollected) {
            assertTrue(RoomEntityPickupRules.canBeCollectedBySword(type),
                "type 0x" + Integer.toHexString(type));
        }
        for (int type : cannotBeCollected) {
            assertFalse(RoomEntityPickupRules.canBeCollectedBySword(type),
                "type 0x" + Integer.toHexString(type));
        }
    }

    private static EntitySpriteDefinition pairDefinition(int type, int variants) {
        List<EntitySpriteDefinition.Variant> displayList = new ArrayList<>();
        for (int i = 0; i < variants; i++) {
            displayList.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x14, 0x02),
                new EntitySpriteDefinition.OamAttribute(0x14, 0x22)));
        }
        return new EntitySpriteDefinition(type, 0x03, 0x5B65,
            EntitySpriteDefinition.Shape.PAIR, 0, displayList);
    }

    private static EntitySpriteDefinition butterflyDefinition() {
        return new EntitySpriteDefinition(0x6E, 0x06, 0x6BBD,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x01), null),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x41), null)));
    }

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }

    private static RoomEntitySnapshot snapshot(RoomEntity... entities) {
        List<RoomEntity> slots = new ArrayList<>(List.of(entities));
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }
}
