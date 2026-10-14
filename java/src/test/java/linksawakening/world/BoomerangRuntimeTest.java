package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BoomerangRuntimeTest {

    @Test
    void spawnPublishesTheRomEntityAndLaunchesThenReturns() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables combatTables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot(), false, () -> 0,
            catalog, combatTables, new ChestContentsTable(rom));

        int slot = runtime.spawnBoomerang(0x40, 0x40, 0, 0, 0);

        assertEquals(0x0F, slot);
        assertTrue(runtime.boomerangActive());
        assertEquals(0x19, runtime.snapshot().slots().get(slot).spriteDefinition().bank());
        assertEquals(0x4451, runtime.snapshot().slots().get(slot).spriteDefinition().address());
        assertEquals(0x28, runtime.boomerangTransitionCountdown(slot));

        tickInteractive(runtime, 0, 0x20, 0x20);

        RoomEntity outbound = runtime.snapshot().slots().get(slot);
        assertEquals(0x42, outbound.x());
        assertEquals(0x27, runtime.boomerangTransitionCountdown(slot));
        assertEquals(0, runtime.boomerangState(slot));
        assertFalse(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x2D));

        for (int frame = 1; frame < 40; frame++) {
            tickInteractive(runtime, frame, 0x20, 0x20);
            outbound = runtime.snapshot().slots().get(slot);
        }

        assertTrue(runtime.boomerangActive());
        assertEquals(1, runtime.boomerangState(slot));

        for (int frame = 40; frame < 120 && runtime.boomerangActive(); frame++) {
            tickInteractive(runtime, frame, 0x20, 0x20);
        }

        assertFalse(runtime.boomerangActive());
    }

    @Test
    void airborneReturnSteeringDoesNotFollowLinksJumpHeight() throws IOException {
        for (int linkZ : new int[] {0, 24}) {
            RoomEntityRuntime runtime = runtime(false);
            int slot = runtime.spawnBoomerang(0x10, 0x50, 7, 0, 0);
            for (int frame = 0; frame < 40; frame++) {
                runtime.tickWithProjectileEvents(frame, 0x20, 0x50, () -> 0, null,
                    new EnemyProjectileCollision.LinkState(0x20, 0x50, linkZ, 0, 0, false));
            }
            assertEquals(8, runtime.snapshot().slots().get(slot).z());
            assertEquals(0xF8, runtime.boomerangSpeedX(slot));
            assertEquals(1, runtime.boomerangSpeedY(slot));

            runtime.tickWithProjectileEvents(40, 0x20, 0x50, () -> 0, null,
                new EnemyProjectileCollision.LinkState(0x20, 0x50, linkZ, 0, 0, false));
            assertEquals(0xE0, runtime.boomerangSpeedX(slot));
            assertEquals(4, runtime.boomerangSpeedY(slot));
        }
    }

    @Test
    void bushIntersectionRestoresPositionAndUsesThePaddedLocation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot(), false, () -> 0,
            catalog, new RomEnemyCombatTables(rom), new ChestContentsTable(rom));
        int slot = runtime.spawnBoomerang(0x20, 0x40, 0, 0, 0);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0x5C, 0x10, 0x30, 0x40));

        runtime.tick(0, 0x20, 0x40, () -> 0);
        assertEquals(1, runtime.boomerangObjectRequests().size());
        assertEquals(0x20, runtime.snapshot().slots().get(slot).x());
        assertEquals(0x27, runtime.boomerangTransitionCountdown(slot));
        RoomEntityRuntime.BoomerangObjectRequest request =
            runtime.boomerangObjectRequests().get(0);

        runtime.setObjectIntersectionQuery(entity -> RoomEntityObjectSample.none());
        runtime.tick(1, 0x20, 0x40, () -> 0);
        runtime.tick(2, 0x20, 0x40, () -> 0);

        assertEquals(slot, request.sourceSlot());
        assertEquals(0x43, request.location());
        assertEquals(0x30, request.objectLeft());
        assertEquals(0x40, request.objectTop());
    }

    @Test
    void playerProjectileGateMatchesTheRomCounter() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot(), false, () -> 0,
            catalog, new RomEnemyCombatTables(rom), new ChestContentsTable(rom));

        int boomerangSlot = runtime.spawnBoomerang(0x40, 0x40, 0, 0, 0);

        assertEquals(1, runtime.activeProjectileCount());
        assertEquals(0x0E, runtime.spawnArrow(0x40, 0x40, 0, 0));
        assertEquals(-1, runtime.spawnBoomerang(0x40, 0x40, 0, 0, 0));

        runtime.clearEntity(boomerangSlot);
        assertEquals(1, runtime.activeProjectileCount());
        assertEquals(-1, runtime.spawnBoomerang(0x40, 0x40, 0, 0, 0));
    }

    @ParameterizedTest
    @CsvSource({"false, 1", "false, 0x10", "false, 0x90", "false, 0xFF",
        "true, 1", "true, 0x10", "true, 0x90", "true, 0xFF"})
    void solidObjectsStopTheOutboundBoomerangImmediatelyAfterLaunch(
        boolean indoor, int physics) throws IOException {
        RoomEntityRuntime runtime = runtime(indoor);
        int slot = runtime.spawnBoomerang(0x40, 0x50, 0, 0, 0);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            1, physics, 0x40, 0x40));
        tickInteractive(runtime, 0, 0, 0);

        assertTrue(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(0x40, runtime.snapshot().slots().get(slot).x());
        assertEquals(0, runtime.boomerangTransitionCountdown(slot));
        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundId() == 7));
        // A zero outbound countdown permits the next pass through the same
        // object, and the going handler then starts the return state.
        tickInteractive(runtime, 1, 0, 0);
        assertEquals(0x42, runtime.snapshot().slots().get(slot).x());
        assertEquals(1, runtime.boomerangState(slot));
    }

    @ParameterizedTest
    @CsvSource({"0, 0xD0", "1, 0xD1", "2, 0xD2", "3, 0xD3"})
    void matchingDirectionalBarrierRequiresTheBoomerangToBeAboveGround(
        int direction, int physics) throws IOException {
        RoomEntityRuntime grounded = runtime(false);
        // SpawnPlayerProjectile increments Link's Z byte, so $FF yields Z=0.
        int slot = grounded.spawnBoomerang(0x40, 0x50, 0xFF, direction, 0);
        grounded.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            1, physics, 0x40, 0x40));
        tickInteractive(grounded, 0, 0, 0);
        assertEquals(0, grounded.boomerangTransitionCountdown(slot));
        assertEquals(0x40, grounded.snapshot().slots().get(slot).x());
        assertEquals(0x50, grounded.snapshot().slots().get(slot).y());

        RoomEntityRuntime airborne = runtime(false);
        slot = airborne.spawnBoomerang(0x40, 0x50, 0, direction, 0);
        airborne.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            1, physics, 0x40, 0x40));
        tickInteractive(airborne, 0, 0, 0);
        assertEquals(0x27, airborne.boomerangTransitionCountdown(slot));
    }

    @ParameterizedTest
    @CsvSource({"true, 3", "false, 4"})
    void directionalCrossingCounterAccumulatesAndDecaysOnTheRoomCadence(
        boolean indoor, int blockingFrame) throws IOException {
        RoomEntityRuntime runtime = runtime(indoor);
        int slot = runtime.spawnBoomerang(0x40, 0x50, 0, 0, 0);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            1, 0xD0, 0x40, 0x40));
        tickInteractive(runtime, 0, 0, 0); // Matching direction raises counter 1 to 2.
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            1, 0xD1, 0x40, 0x40));
        for (int frame = 1; frame < blockingFrame; frame++) {
            tickInteractive(runtime, frame, 0, 0);
            assertEquals(0x28 - frame - 1, runtime.boomerangTransitionCountdown(slot));
        }
        int beforeX = runtime.snapshot().slots().get(slot).x();
        tickInteractive(runtime, blockingFrame, 0, 0);
        assertEquals(0, runtime.boomerangTransitionCountdown(slot));
        assertEquals(beforeX, runtime.snapshot().slots().get(slot).x());
    }

    @ParameterizedTest
    @CsvSource({"0", "1", "2", "3"})
    void spinDisplaysTheStoredVariantBeforeEachFourFrameUpdate(int direction)
            throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnBoomerang(0x60, 0x60, 0, direction, 0);
        for (int frame = 0; frame <= 17; frame++) {
            tickInteractive(runtime, frame, 0, 0);
            int expected = (direction + (frame + 3) / 4) & 3;
            assertEquals(expected, runtime.snapshot().slots().get(slot).spriteVariant(),
                "frame " + frame);
        }
        runtime.clearEntity(slot);
        int newDirection = (direction + 1) & 3;
        assertEquals(slot, runtime.spawnBoomerang(0x60, 0x60, 0, newDirection, 0));
        tickInteractive(runtime, 20, 0, 0);
        assertEquals(newDirection, runtime.snapshot().slots().get(slot).spriteVariant());
    }

    @ParameterizedTest
    @CsvSource({"8, false", "-9, true"})
    void returningCatchUsesThePositionCachedBeforeMovement(int linkOffsetX,
                                                          boolean remainsActive)
            throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnBoomerang(0x10, 0x50, 0, 0, 0);
        for (int frame = 0; frame < 40; frame++) {
            tickInteractive(runtime, frame, 0x20, 0x20);
        }
        assertEquals(1, runtime.boomerangState(slot));
        RoomEntity before = runtime.snapshot().slots().get(slot);

        // Slot $0F checks Link on frame 40. The returning vector moves left
        // one pixel, crossing the catch window in opposite ways for these cases.
        tickInteractive(runtime, 40, before.x() + linkOffsetX,
            before.y() - before.z());
        assertEquals(remainsActive, runtime.boomerangActive());
        if (remainsActive) {
            assertEquals(before.x() - 1, runtime.snapshot().slots().get(slot).x());
        }
    }

    @Test
    void soundClockRunsBeforeLaunchAndDuringDialogPauses() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        // World audio advances without any entities, independently of hFrameCounter.
        for (int frame = 0; frame < 25; frame++) {
            tickInteractive(runtime, 0x80 + frame, 0, 0);
        }
        int slot = runtime.spawnBoomerang(0x40, 0x50, 0, 0, 0);
        tickInteractive(runtime, 0, 0, 0);
        assertTrue(hasBoomerangSound(runtime.consumePendingEntityEvents()));
        runtime.setDialogActive(true);
        int beforeX = runtime.snapshot().slots().get(slot).x();
        for (int frame = 1; frame < 26; frame++) {
            tickInteractive(runtime, frame, 0, 0);
            assertFalse(hasBoomerangSound(runtime.consumePendingEntityEvents()));
        }
        assertEquals(beforeX, runtime.snapshot().slots().get(slot).x());
        runtime.setDialogActive(false);
        tickInteractive(runtime, 26, 0, 0);
        assertTrue(hasBoomerangSound(runtime.consumePendingEntityEvents()));
        tickInteractive(runtime, 27, 0, 0);
        assertFalse(hasBoomerangSound(runtime.consumePendingEntityEvents()));
    }

    @Test
    void soundClockDoesNotRunOutsideWorldGameplay() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        for (int frame = 0; frame < 25; frame++) {
            tickInteractive(runtime, frame, 0, 0);
        }
        runtime.spawnBoomerang(0x40, 0x50, 0, 0, 0);
        runtime.setGameplayWorldForTest(false);
        for (int frame = 25; frame < 30; frame++) {
            tickInteractive(runtime, frame, 0, 0);
        }
        assertFalse(hasBoomerangSound(runtime.consumePendingEntityEvents()));
        runtime.setGameplayWorldForTest(true);
        tickInteractive(runtime, 30, 0, 0);
        assertTrue(hasBoomerangSound(runtime.consumePendingEntityEvents()));
    }

    private static boolean hasBoomerangSound(List<EntityCombatEvent> events) {
        return events.stream().anyMatch(event ->
            event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x2D);
    }

    private static RoomEntityRuntime runtime(boolean indoor) throws IOException {
        byte[] rom = loadRom();
        return RoomEntityRuntime.from(emptySnapshot(), indoor, () -> 0,
            new EntitySpriteHandlerCatalog(rom), new RomEnemyCombatTables(rom),
            new ChestContentsTable(rom));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }

    private static void tickInteractive(RoomEntityRuntime runtime, int frame,
                                         int linkX, int linkY) {
        runtime.tickWithProjectileEvents(frame, linkX, linkY, () -> 0, null,
            new EnemyProjectileCollision.LinkState(linkX, linkY, 0, 0, 0, false));
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = BoomerangRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
