package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BombLiftThrowRuntimeTest {

    @Test
    void liftingRestoresTheNormalRomBombAfterAWarningSnapshot() throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        runtime.setBombTransitionCountdownForTest(slot, 0x19);
        runtime.tick(0, 0x40, 0x50, () -> 0);

        RoomEntity warning = runtime.snapshot().slots().get(slot);
        assertEquals(0x5484, warning.spriteDefinition().address());
        runtime.setEnemyFlashCountdownForTest(slot, 0x20);

        assertTrue(runtime.beginLift(slot, ThrownEntityMotion.ROM_DIRECTION_RIGHT));

        RoomEntity lifted = runtime.snapshot().slots().get(slot);
        assertEquals(slot, lifted.slot());
        assertEquals(0x02, lifted.type());
        assertEquals(EntityStatus.LIFTED, lifted.status());
        assertNormalBombDefinition(lifted);
        assertEquals(0, runtime.enemyFlashCountdown(slot));
        assertTrue(runtime.bombActive());
    }

    @Test
    void liftedBombConsumesItsSharedTimerWithoutRunningFusePresentation() throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        runtime.setBombTransitionCountdownForTest(slot, 0x19);
        runtime.tick(0, 0x40, 0x50, () -> 0);
        assertTrue(runtime.beginLift(slot, ThrownEntityMotion.ROM_DIRECTION_RIGHT));

        for (int frame = 1; frame < 8; frame++) {
            runtime.tick(frame, 0x40, 0x50, () -> 0);
            RoomEntity lifted = runtime.snapshot().slots().get(slot);
            assertEquals(EntityStatus.LIFTED, lifted.status());
            assertNormalBombDefinition(lifted);
            assertTrue(runtime.transitionCountdown(slot) < 0x18);
            assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        }
    }

    @Test
    void equippedBombButtonLiftsAActiveBombWithoutTheGrabbablePhysicsBit() throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        runtime.setBombButtonHeld(true);

        runtime.tick(0, 0x40, 0x50, () -> 0);

        RoomEntity lifted = runtime.snapshot().slots().get(slot);
        assertEquals(EntityStatus.LIFTED, lifted.status());
        assertNormalBombDefinition(lifted);
        assertEquals(0xD2, runtime.physicsFlags(slot));
        assertEquals(1, runtime.snapshot().loadedEntities().size());
    }

    @Test
    void bombButtonCannotLiftWarningOrExplosionPresentations() throws IOException {
        RoomEntityRuntime warningRuntime = bombRuntime();
        int warningSlot = warningRuntime.spawnBomb(0x40, 0x50, 0, 0);
        warningRuntime.setBombTransitionCountdownForTest(warningSlot, 0x22);
        warningRuntime.setBombButtonHeld(true);
        warningRuntime.tick(0, 0x40, 0x50, () -> 0);
        assertEquals(EntityStatus.ACTIVE,
            warningRuntime.snapshot().slots().get(warningSlot).status());
        assertEquals(0x5484,
            warningRuntime.snapshot().slots().get(warningSlot).spriteDefinition().address());

        RoomEntityRuntime explosionRuntime = bombRuntime();
        int explosionSlot = explosionRuntime.spawnBomb(0x40, 0x50, 0, 0);
        explosionRuntime.setBombTransitionCountdownForTest(explosionSlot, 0x18);
        explosionRuntime.setBombButtonHeld(true);
        explosionRuntime.tick(0, 0x40, 0x50, () -> 0);
        assertEquals(EntityStatus.ACTIVE,
            explosionRuntime.snapshot().slots().get(explosionSlot).status());
        assertEquals(0x6530,
            explosionRuntime.snapshot().slots().get(explosionSlot).spriteDefinition().address());
    }

    @Test
    void finalExplosionPresentationCannotBeRevivedByTheLiftStateBridge() throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        runtime.setBombTransitionCountdownForTest(slot, 0x01);
        RoomEntity beforeExplosion = runtime.snapshot().slots().get(slot);
        runtime.tick(0, 0x40, 0x50, () -> 0);

        assertFalse(runtime.beginLift(slot, ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        RoomEntity afterExplosion = runtime.snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, afterExplosion.status());
        assertEquals(beforeExplosion.x(), afterExplosion.x());
        assertEquals(beforeExplosion.y(), afterExplosion.y());
        assertEquals(beforeExplosion.z(), afterExplosion.z());
        assertFalse(runtime.bombActive());
    }

    @Test
    void throwingBombResetsFuseAndKeepsTheRomBombTrajectoryInOneSlot() throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        fullyLift(runtime, slot);

        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        RoomEntity thrown = runtime.snapshot().slots().get(slot);
        assertEquals(slot, thrown.slot());
        assertEquals(0x02, thrown.type());
        assertEquals(EntityStatus.THROWN, thrown.status());
        assertEquals(ThrownEntityMotion.ROM_DIRECTION_RIGHT, runtime.thrownDirection(slot));
        assertEquals(0xA0, runtime.transitionCountdown(slot));
        assertTrue(runtime.bombActive());

        int xBeforeMotion = thrown.x();
        int zBeforeMotion = thrown.z();
        runtime.tick(100, thrown.x(), thrown.y(), () -> 0);
        RoomEntity afterMotion = runtime.snapshot().slots().get(slot);
        assertEquals(EntityStatus.THROWN, afterMotion.status());
        assertEquals((xBeforeMotion + 1) & 0xFF, afterMotion.x());
        assertEquals((zBeforeMotion + 1) & 0xFF, afterMotion.z());
        assertNormalBombDefinition(afterMotion);
    }

    @Test
    void thrownBombLiftRunsPostActiveBounceWithoutASecondThrowStep() throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        fullyLift(runtime, slot);
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));

        RoomEntity thrown = runtime.snapshot().slots().get(slot);
        int expectedLiftedX = (thrown.x() + 0x10) & 0xFF;
        AtomicInteger wallQueries = new AtomicInteger();
        List<EntityStatus> observedStatuses = new ArrayList<>();
        List<Integer> proposedX = new ArrayList<>();
        runtime.setBombButtonHeld(true);
        runtime.tick(100, thrown.x(), thrown.y(), () -> 0,
            (entity, direction, nextX, nextY) -> {
                wallQueries.incrementAndGet();
                observedStatuses.add(entity.status());
                proposedX.add(nextX);
                return false;
            }, null, 0, 3, 0);

        RoomEntity lifted = runtime.snapshot().slots().get(slot);
        assertEquals(1, wallQueries.get());
        assertEquals(List.of(EntityStatus.LIFTED), observedStatuses);
        assertEquals((expectedLiftedX + 1) & 0xFF, lifted.x());
        assertEquals(proposedX.get(0), lifted.x());
        assertEquals(slot, lifted.slot());
        assertEquals(EntityStatus.LIFTED, lifted.status());
        assertEquals(thrown.y(), lifted.y());
        assertEquals(0x01, lifted.z());
        assertNormalBombDefinition(lifted);
    }

    @Test
    void stunnedBombLiftRunsPostActiveBounceWithoutASecondBounceStep()
        throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        fullyLift(runtime, slot);
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        runtime.setBombTransitionCountdownForTest(slot, 0xA0);

        RoomEntityBackgroundCollision alwaysBlocked = (entity, direction, nextX, nextY) -> true;
        int frame = 0;
        while (runtime.snapshot().slots().get(slot).status() != EntityStatus.STUNNED
            && frame < 12) {
            runtime.tick(frame++, 0, 0, () -> 0, alwaysBlocked);
        }
        assertEquals(EntityStatus.STUNNED, runtime.snapshot().slots().get(slot).status());

        RoomEntity stunned = runtime.snapshot().slots().get(slot);
        int expectedLiftedX = (stunned.x() + 0x10) & 0xFF;
        runtime.setEnemyIgnoreHitsCountdownForTest(slot, 0x02);
        AtomicInteger wallQueries = new AtomicInteger();
        List<EntityStatus> observedStatuses = new ArrayList<>();
        runtime.setBombButtonHeld(true);
        runtime.tick(frame, stunned.x(), stunned.y(), () -> 0,
            (entity, direction, nextX, nextY) -> {
                wallQueries.incrementAndGet();
                observedStatuses.add(entity.status());
                return true;
            },
            null, 0, 3, 0);

        RoomEntity lifted = runtime.snapshot().slots().get(slot);
        // The thrown motion is already stopped when EntityBecomeStunned runs, so the
        // source bounce has no wall probe here. The runtime tick prepass accounts for
        // the one source countdown decrement when no recoil motion is active.
        assertEquals(0, wallQueries.get());
        assertEquals(List.of(), observedStatuses);
        assertEquals(0x01, runtime.enemyIgnoreHitsCountdown(slot));
        assertEquals(slot, lifted.slot());
        assertEquals(EntityStatus.LIFTED, lifted.status());
        assertEquals(expectedLiftedX, lifted.x());
        assertEquals(stunned.y(), lifted.y());
        assertEquals(0, lifted.z());
        assertNormalBombDefinition(lifted);
    }

    @Test
    void stunnedBombLiftRetainsTheReturnedRomRecoilPosition() throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        fullyLift(runtime, slot);
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        runtime.setBombTransitionCountdownForTest(slot, 0xA0);

        RoomEntityBackgroundCollision alwaysBlocked = (entity, direction, nextX, nextY) -> true;
        int frame = 0;
        while (runtime.snapshot().slots().get(slot).status() != EntityStatus.STUNNED
            && frame < 12) {
            runtime.tick(frame++, 0, 0, () -> 0, alwaysBlocked);
        }
        assertEquals(EntityStatus.STUNNED, runtime.snapshot().slots().get(slot).status());

        RoomEntity stunned = runtime.snapshot().slots().get(slot);
        int expectedLiftedX = (stunned.x() + 0x10) & 0xFF;
        runtime.configureEnemyRecoilForTest(slot, stunned.x() + 0x10, stunned.y(), 0x10);
        runtime.setEnemyIgnoreHitsCountdownForTest(slot, 0x02);
        runtime.setBombButtonHeld(true);
        runtime.tick(frame, stunned.x(), stunned.y(), () -> 0,
            (entity, direction, nextX, nextY) -> false,
            null, 0, 3, 0);

        RoomEntity lifted = runtime.snapshot().slots().get(slot);
        assertEquals((expectedLiftedX - 1) & 0xFF, lifted.x());
        assertEquals(0x01, runtime.enemyIgnoreHitsCountdown(slot));
        assertEquals(slot, lifted.slot());
        assertEquals(EntityStatus.LIFTED, lifted.status());
        assertNormalBombDefinition(lifted);
    }

    @Test
    void placedBombUsesSpawnProjectileSpeedsAndPreservesWallBounce() throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, ThrownEntityMotion.ROM_DIRECTION_RIGHT);
        AtomicInteger wallQueries = new AtomicInteger();

        runtime.tick(0, 0x40, 0x50, () -> 0,
            (entity, direction, nextX, nextY) -> {
                wallQueries.incrementAndGet();
                assertEquals(ThrownEntityMotion.ROM_DIRECTION_RIGHT, direction);
                assertEquals(0x42, nextX);
                return true;
            });

        RoomEntity afterWall = runtime.snapshot().slots().get(slot);
        assertEquals(1, wallQueries.get());
        assertEquals(0x40, afterWall.x());
        assertEquals(0x50, afterWall.y());
        assertEquals(0x01, afterWall.z());
        assertEquals(EntityStatus.ACTIVE, afterWall.status());

        runtime.tick(1, 0x40, 0x50, () -> 0,
            (entity, direction, nextX, nextY) -> false);

        RoomEntity afterBounce = runtime.snapshot().slots().get(slot);
        assertEquals(0x3F, afterBounce.x());
        assertEquals(0x50, afterBounce.y());
        assertEquals(0x00, afterBounce.z());
        assertEquals(EntityStatus.ACTIVE, afterBounce.status());
    }

    @Test
    void thrownThenStunnedBombRunsFuseBeforeMotionAndUnloadsAfterExplosion() throws IOException {
        RoomEntityRuntime runtime = bombRuntime();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        fullyLift(runtime, slot);
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        runtime.setBombTransitionCountdownForTest(slot, 0x19);

        RoomEntityBackgroundCollision alwaysBlocked = (entity, direction, nextX, nextY) -> true;
        runtime.tick(0, 0x40, 0x50, () -> 0, alwaysBlocked);
        RoomEntity warningWhileThrown = runtime.snapshot().slots().get(slot);
        assertEquals(EntityStatus.THROWN, warningWhileThrown.status());
        assertEquals(0x5484, warningWhileThrown.spriteDefinition().address());

        int stunnedFrame = 1;
        while (runtime.snapshot().slots().get(slot).status() != EntityStatus.STUNNED
            && stunnedFrame < 12) {
            runtime.tick(stunnedFrame++, 0x40, 0x50, () -> 0, alwaysBlocked);
        }
        RoomEntity explosionWhileStunned = runtime.snapshot().slots().get(slot);
        assertEquals(EntityStatus.STUNNED, explosionWhileStunned.status());
        assertEquals(0x6530, explosionWhileStunned.spriteDefinition().address());
        assertEquals(3, explosionWhileStunned.spriteVariant());

        for (int frame = 2; frame < 40 && runtime.snapshot().slots().get(slot).loaded(); frame++) {
            runtime.tick(frame, 0x40, 0x50, () -> 0, alwaysBlocked);
        }

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(slot).status());
        assertFalse(runtime.bombActive());
    }

    @Test
    void genericPowerBraceletLiftStillRequiresTheExistingGrabbablePath() {
        EntitySpriteDefinition definition = new EntitySpriteDefinition(
            0x05, 0x03, 0x5B65, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x14, 0x02),
                new EntitySpriteDefinition.OamAttribute(0x14, 0x22))));
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, 0x05, 0x20, 0x30, EntityStatus.STUNNED,
            definition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots));
        runtime.setPowerBraceletButtonHeld(true);

        runtime.tick(0, 0x20, 0x30, () -> 0);

        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
    }

    private static void fullyLift(RoomEntityRuntime runtime, int slot) {
        assertTrue(runtime.beginLift(slot, ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x40, 0x50, () -> 0);
        }
        assertEquals(0x01, runtime.liftedEntityState().carryState());
    }

    private static RoomEntityRuntime bombRuntime() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return RoomEntityRuntime.from(new RoomEntitySnapshot(slots), false, null, catalog);
    }

    private static void assertNormalBombDefinition(RoomEntity entity) {
        assertEquals(EntitySpriteDefinition.Shape.SINGLE, entity.spriteDefinition().shape());
        assertEquals(0x03, entity.spriteDefinition().bank());
        assertEquals(0x652E, entity.spriteDefinition().address());
        assertEquals(0, entity.spriteVariant());
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = BombLiftThrowRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
