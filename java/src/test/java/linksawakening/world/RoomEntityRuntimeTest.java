package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
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
    void butterflyUsesTheRomRatioForDiagonalAttractionVectors() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x6E, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // GetVectorTowardsLink(2) turns dx=16, dy=8 into (2,1), not (2,2).
        runtime.tick(0, 80, 72, sequence(0x00, 0x00));

        assertEquals(2, runtime.butterflyPrivateStateX(0));
        assertEquals(1, runtime.butterflyPrivateStateY(0));
    }

    @Test
    void keeseSleepsOutsideTheRomWakeWindowAndWakesInsideIt() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 96, 64, sequence(0x00));
        assertEquals(0, runtime.keeseState(0));
        assertEquals(0, runtime.keeseTransitionCountdown(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(1, 72, 64, sequence(0x00));
        assertEquals(1, runtime.keeseState(0));
        assertEquals(0x50, runtime.keeseTransitionCountdown(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
    }

    @Test
    void keeseRetunesItsAngleAndLoadsTheRomSpeedTablesEveryTenFlightFrames() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00, 0x01, 0x01);

        runtime.tick(0, 72, 64, randomBytes);
        for (int frame = 1; frame <= 10; frame++) {
            runtime.tick(frame, 72, 64, randomBytes);
        }

        assertEquals(1, runtime.keeseState(0));
        assertEquals(0x0D, runtime.keeseAngle(0));
        assertEquals(0x05, runtime.keeseSpeedX(0));
        assertEquals(0xF3, runtime.keeseSpeedY(0));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void entityHandlersConsumeSharedRandomBytesInRomReverseSlotOrder() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0),
            new RoomEntity(1, 1, 0x19, 64, 80, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // AnimateEntities starts at MAX_ENTITIES - 1 and decrements. Slot 1
        // therefore consumes the first wake timer byte.
        runtime.tick(0, 72, 72, sequence(0x00, 0x3F));

        assertEquals(0x8F, runtime.keeseTransitionCountdown(0));
        assertEquals(0x50, runtime.keeseTransitionCountdown(1));
    }

    @Test
    void keeseUsesTheRomEnemyHitboxCadenceAndContactDamage() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> skipped = runtime.resolveCombat(
            0, 64, 64, false, true, false, 0, 0, 0, 0);
        assertTrue(skipped.isEmpty());

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0, contact.get(0).slot());
        assertEquals(0x19, contact.get(0).type());
        assertEquals(4, contact.get(0).linkDamage());
        assertFalse(contact.get(0).swordHit());

        assertTrue(runtime.resolveCombat(1, 73, 64, false, true,
            false, 0, 0, 0, 0).isEmpty());
        assertTrue(runtime.resolveCombat(1, 64, 64, true, true,
            false, 0, 0, 0, 0).isEmpty());
        assertTrue(runtime.resolveCombat(1, 64, 64, false, false,
            false, 0, 0, 0, 0).isEmpty());
    }

    @Test
    void aBasicSwordHitPutsKeeseIntoTheRomDyingState() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, events.get(0).linkDamage());
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(0x40, runtime.dyingCountdown(0));

        for (int frame = 1; frame < 0x40; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertTrue(runtime.snapshot().slots().get(0).loaded());
        runtime.tick(0x40, 120, 120, sequence(0x00));
        assertFalse(runtime.snapshot().slots().get(0).loaded());
    }

    @Test
    void octorokUsesTheSharedRoamingEnemyTimersAndDirectionSpeeds() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x02, 0x03);

        // State 0 pauses for ($10 + random & $0F) frames, then state 1
        // selects direction 3 (down) and the ROM's +8/16 fixed-point speed.
        runtime.tick(0, 200, 32, randomBytes);
        assertEquals(1, runtime.octorokState(0));
        assertEquals(0x12, runtime.octorokTransitionCountdown(0));
        assertEquals(0, runtime.octorokSpeedY(0));

        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(frame, 200, 32, randomBytes);
        }
        assertEquals(0, runtime.octorokState(0),
            () -> "state=" + runtime.octorokState(0)
                + " timer=" + runtime.octorokTransitionCountdown(0));
        assertEquals(0x23, runtime.octorokTransitionCountdown(0));
        assertEquals(3, runtime.octorokDirection(0));
        assertEquals(8, runtime.octorokSpeedY(0));
        assertEquals(64, runtime.snapshot().slots().get(0).y());

        runtime.tick(0x13, 200, 32, randomBytes);
        runtime.tick(0x14, 200, 32, randomBytes);
        assertEquals(65, runtime.snapshot().slots().get(0).y());
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void octorokStopsAtTheRoamingEnemyBackgroundCollisionPoint() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00, 0x03, 0x01);

        runtime.tick(0, 200, 32, randomBytes);
        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(frame, 200, 32, randomBytes);
        }

        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 3;
        runtime.tick(0x13, 200, 32, randomBytes, wall);
        runtime.tick(0x14, 200, 32, randomBytes, wall);
        runtime.tick(0x15, 200, 32, randomBytes, wall);

        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertEquals(0, runtime.octorokSpeedY(0));
    }

    @Test
    void octorokUsesTheNormalEnemyHitboxAndOneBasicSwordDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        RoomEntityRuntime swordRuntime = RoomEntityRuntime.from(initial);
        List<EntityCombatEvent> events = swordRuntime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(EntityStatus.DYING, swordRuntime.snapshot().slots().get(0).status());
    }

    @Test
    void dynamicGhostRuntimeAdvancesZBobEvenWhenItsVariantIsUnchanged() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forFollowerEntityType(0xD4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, -1, 0xD4, 0x40, 0x40, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, false, () -> 0);
        runtime.setFollowingNpcState(new FollowingNpcState(false, 1, false, false,
            0, 0, false));

        for (int frame = 0; frame <= 8; frame++) {
            runtime.tick(frame, 0x40, 0x30, () -> 0);
        }

        RoomEntity ghost = runtime.snapshot().slots().get(0);
        assertEquals(0x0D, ghost.z());
        assertEquals(4, ghost.spriteVariant());
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

    private static EntitySpriteDefinition keeseDefinition() {
        return new EntitySpriteDefinition(0x19, 0x06, 0x6708,
            EntitySpriteDefinition.Shape.PAIR, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x42, 0x00),
                    new EntitySpriteDefinition.OamAttribute(0x42, 0x20)),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x40, 0x00),
                    new EntitySpriteDefinition.OamAttribute(0x40, 0x20))));
    }

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }

    private static byte[] syntheticRom() {
        return new byte[0x100000];
    }

    private static RoomEntitySnapshot snapshot(RoomEntity... entities) {
        List<RoomEntity> slots = new ArrayList<>(List.of(entities));
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }
}
