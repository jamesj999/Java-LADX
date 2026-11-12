package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.Rom;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.motion.MotionTestAccess;
import linksawakening.world.room.ChestContentsTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@linksawakening.testing.RequiresRom
final class MagicRodRuntimeTest {

    @Test
    void spawnUsesTheRomFireballDisplayAndMovesOnTheSharedProjectileTables() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);

        assertEquals(0x0F, slot);
        assertEquals(1, runtime.inspection().activeProjectileCount());
        assertEquals(
                0x03,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(slot)
                        .spriteDefinition()
                        .bank());
        assertEquals(
                0x69AA,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(slot)
                        .spriteDefinition()
                        .address());
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));

        tick(runtime, 0, 0x20, 0x20);
        RoomEntity first = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x47, first.x());
        assertEquals(0, first.spriteVariant());

        tick(runtime, 8, 0x20, 0x20);
        assertEquals(0x4A, runtime.inspection().snapshot().slots().get(slot).x());
        assertEquals(0, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
        tick(runtime, 9, 0x20, 0x20);
        assertEquals(1, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
    }

    @Test
    void solidObjectStartsTheSharedFireDisplayAndUnloadsAtPrivateCountdownOne() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x01, 0x01, 0x40, 0x40));

        tick(runtime, 0, 0x20, 0x20);

        assertEquals(
                MotionTestAccess.MagicRodFireballMotion_FIRE_TRANSITION_COUNTDOWN,
                new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        assertEquals(
                0x69AA,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(slot)
                        .spriteDefinition()
                        .address());
        assertEquals(0, runtime.inspection().snapshot().slots().get(slot).spriteVariant());

        tick(runtime, 1, 0x20, 0x20);
        assertEquals(
                0x2F, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        assertEquals(
                0x4C44,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(slot)
                        .spriteDefinition()
                        .address());
        assertEquals(0, runtime.inspection().snapshot().slots().get(slot).spriteVariant());

        for (int frame = 2; frame <= 0x2E; frame++) {
            tick(runtime, frame, 0x20, 0x20);
            assertTrue(runtime.inspection().snapshot().slots().get(slot).loaded());
        }
        assertEquals(2, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        tick(runtime, 0x2F, 0x20, 0x20);
        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(0, runtime.inspection().activeProjectileCount());
    }

    @Test
    void outdoorBushRequestsRomObjectRevealSmokeAndDestroyNoise() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x20, 0x40, 0, 0);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x5C, 0x01, 0x30, 0x40));

        tick(runtime, 0, 0x20, 0x20);

        assertEquals(1, runtime.enemies().magicRodObjectRequests().size());
        MagicRodObjectRequest request =
                runtime.enemies().magicRodObjectRequests().getFirst();
        assertEquals(slot, request.sourceSlot());
        assertEquals(0x43, request.location());
        assertEquals(0x30, request.objectLeft());
        assertEquals(0x40, request.objectTop());
    }

    @Test
    void paletteFrameLagsTheCounterForBothFlightAndWallFire() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);
        tick(runtime, 7, 0, 0);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x01, 0x01, 0x40, 0x40));
        tick(runtime, 8, 0, 0);
        assertEquals(0, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
        for (int frame = 9; frame <= 17; frame++) {
            tick(runtime, frame, 0, 0);
            RoomEntity fire = runtime.inspection().snapshot().slots().get(slot);
            assertEquals(0x4C44, fire.spriteDefinition().address());
            assertEquals(frame == 17 ? 0 : 1, fire.spriteVariant(), "frame " + frame);
        }
    }

    @Test
    void normalEnemyHitUnloadsTheFireballBeforeObjectLookup() throws IOException {
        RoomEntityRuntime runtime = runtimeWithTarget(0x09, 0x40, 0x3F);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);
        runtime.configuration().setObjectIntersectionQuery(entity -> {
            throw new AssertionError("A consumed fireball must not reach object lookup");
        });
        tick(runtime, 0, 0, 0);

        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(0, runtime.inspection().activeProjectileCount());
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event -> event.slot() == 0 && event.type() == 0x09));
    }

    @Test
    void wallFlameCanHitAnEnemyWithoutBeingConsumed() throws IOException {
        RoomEntityRuntime runtime = runtimeWithTarget(0x09, 0x40, 0x3F);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x01, 0x01, 0x40, 0x40));
        // Slot 0 is skipped by the collision pass on odd frames.
        tick(runtime, 1, 0, 0);
        assertEquals(
                0x30, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        tick(runtime, 2, 0, 0);

        assertTrue(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(
                0x2F, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event -> event.slot() == 0 && event.type() == 0x09));
    }

    @Test
    void hitboxImmunityBlocksFireballEvenDuringTheIgnoreHitsCountdown() throws IOException {
        RoomEntityRuntime runtime = runtimeWithTarget(0x09, 0x40, 0x3F);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);
        EntityRuntimeFixture.debug(runtime).setHitboxFlagsForTest(0, 0x80);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 8);
        int health = runtime.enemies().enemyHealth(0);
        tick(runtime, 0, 0, 0);

        assertTrue(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(
                0x30, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        assertEquals(health, runtime.enemies().enemyHealth(0));
    }

    @Test
    void ignoreHitsCountdownAloneLetsTheFireballPassThrough() throws IOException {
        RoomEntityRuntime runtime = runtimeWithTarget(0x09, 0x40, 0x3F);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 8);
        tick(runtime, 0, 0, 0);

        assertTrue(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
    }

    @ParameterizedTest
    @CsvSource({"1, true", "0, false"})
    void ironMaskBlocksOnlyWhenItsAttachedMaskFacesTheFireball(int fireballDirection, boolean blocked)
            throws IOException {
        RoomEntityRuntime runtime = runtimeWithTarget(0x24, 0x40, 0x3F);
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).ironMaskDirection(0));
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, fireballDirection);
        tick(runtime, 0, 0, 0);

        assertEquals(blocked, runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(
                blocked ? 0x30 : 0,
                new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
    }

    @Test
    void nonInteractiveLinkAloneDoesNotPauseMovementOrTheStoredPaletteFrame() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);
        for (int frame = 8; frame <= 9; frame++) {
            runtime.tick(EntityTestFrames.projectileFrame(
                    runtime, frame, 0, 0, () -> 0, null, EnemyProjectileCollision.LinkState.nonInteractive()));
            RoomEntity fireball = runtime.inspection().snapshot().slots().get(slot);
            assertEquals(0x44 + 3 * (frame - 7), fireball.x());
            assertEquals(frame == 8 ? 0 : 1, fireball.spriteVariant());
        }
        runtime.clearEntity(slot);
        assertEquals(slot, runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0));
        tick(runtime, 10, 0, 0);
        assertEquals(0, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
    }

    @ParameterizedTest
    @CsvSource({"0x50, 0x43, true", "0x38, 0x43, false", "0x44, 0x37, true", "0x44, 0x4F, false"})
    void targetCollisionPreservesTheRomsAsymmetricTwelvePixelBoundary(int targetX, int targetY, boolean hits)
            throws IOException {
        RoomEntityRuntime runtime = runtimeWithTarget(0x09, targetX, targetY);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);
        tick(runtime, 0, 0, 0);
        assertEquals(!hits, runtime.inspection().snapshot().slots().get(slot).loaded());
    }

    @ParameterizedTest
    @CsvSource({
        "0, false, 4, 4, 3, 0",
        "1, false, -4, 4, -3, 0",
        "2, false, -4, -4, 0, -3",
        "3, false, 4, 4, 0, 3",
        "0, true, 4, 4, 4, 0",
        "1, true, -4, 4, -4, 0",
        "2, true, -4, -4, 0, -4",
        "3, true, 4, 4, 0, 4"
    })
    void launchAppliesDirectionOffsetsAndPowerSpeedBeforeTheFirstHandlerPass(
            int direction, boolean power, int offsetX, int offsetY, int dx, int dy) throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x50, 7, direction, power);
        RoomEntity spawn = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x40 + offsetX, spawn.x());
        assertEquals(0x50 + offsetY, spawn.y());
        assertEquals(8, spawn.z());
        assertEquals(0, spawn.spriteVariant());

        tick(runtime, 0, 0, 0);
        RoomEntity moved = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(spawn.x() + dx, moved.x());
        assertEquals(spawn.y() + dy, moved.y());
        assertEquals(8, moved.z());
    }

    @ParameterizedTest
    @CsvSource({"0, false", "1, false", "2, false", "3, false", "0, true", "1, true", "2, true", "3, true"})
    void solidImpactRestoresTheFireballsPositionBeforeLeavingAStationaryFlame(int direction, boolean power)
            throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x50, 7, direction, power);
        RoomEntity before = runtime.inspection().snapshot().slots().get(slot);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x01, 0x01, 0x40, 0x40));
        tick(runtime, 0, 0, 0);
        assertEquals(
                0x30, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        for (int frame = 1; frame <= 3; frame++) {
            RoomEntity after = runtime.inspection().snapshot().slots().get(slot);
            assertEquals(before.x(), after.x());
            assertEquals(before.y(), after.y());
            assertEquals(before.z(), after.z());
            tick(runtime, frame, 0, 0);
        }
    }

    @ParameterizedTest
    @CsvSource({"false, 0x5C", "false, 0xD3", "true, 0x8A"})
    void burningAnObjectKeepsTheRestoredPositionThenResumesTravel(boolean indoor, int objectId) throws IOException {
        RoomEntityRuntime runtime = runtime(indoor);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x50, 0, 0);
        RoomEntity before = runtime.inspection().snapshot().slots().get(slot);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(objectId, 1, 0x40, 0x40));
        tick(runtime, 0, 0, 0);
        assertEquals(
                before.x(), runtime.inspection().snapshot().slots().get(slot).x());
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        assertEquals(1, runtime.enemies().magicRodObjectRequests().size());

        runtime.configuration().setObjectIntersectionQuery(entity -> RoomEntityObjectSample.none());
        tick(runtime, 1, 0, 0);
        assertEquals(
                before.x() + 3,
                runtime.inspection().snapshot().slots().get(slot).x());
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void oppositeDirectionalBarrierBlocksOnThePassAfterCrossingCounterExpires(boolean indoor) throws IOException {
        RoomEntityRuntime runtime = runtime(indoor);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x50, 0, 0);
        int startX = runtime.inspection().snapshot().slots().get(slot).x();
        runtime.configuration().setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(1, 0xD1, 0x40, 0x40));
        // Initial counter one is consumed on frame 1. Frame 2 must block
        // even outdoors, where a nonzero counter would skip this even frame.
        tick(runtime, 1, 0, 0);
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        assertEquals(
                startX + 3, runtime.inspection().snapshot().slots().get(slot).x());
        tick(runtime, 2, 0, 0);
        assertEquals(
                0x30, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        assertEquals(
                startX + 3, runtime.inspection().snapshot().slots().get(slot).x());
    }

    @Test
    void physicsFFUnloadsImmediatelyBeforeBurningOrStartingAFlame() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x50, 0, 0);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x5C, 0xFF, 0x40, 0x40));
        tick(runtime, 0, 0, 0);
        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).magicRodFireballPrivateCountdown1(slot));
        assertTrue(runtime.enemies().magicRodObjectRequests().isEmpty());
    }

    private static RoomEntityRuntime runtimeWithTarget(int type, int x, int y) throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        List<RoomEntity> slots = new ArrayList<>(emptySnapshot().slots());
        slots.set(
                0,
                new RoomEntityBuilder()
                        .type(type)
                        .at(x, y)
                        .spriteDefinition(catalog.forEntityType(type, EntityRoomLoader.RoomTable.OVERWORLD, -1))
                        .build());
        return RoomEntityRuntime.builder(new RoomEntitySnapshot(slots))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .chestContentsTable(new ChestContentsTable((rom == null ? (Rom) null : TestRoms.partial(rom))))
                .build();
    }

    private static RoomEntityRuntime runtime(boolean indoor) throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        return RoomEntityRuntime.builder(emptySnapshot())
                .indoorRoom(indoor)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .chestContentsTable(new ChestContentsTable((rom == null ? (Rom) null : TestRoms.partial(rom))))
                .build();
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int linkX, int linkY) {
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                frame,
                linkX,
                linkY,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(linkX, linkY, 0, 0, 0, false)));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
