package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArrowPauseTest {
    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void gameplayGatesStopAllThreeArrowPathProjectiles(String gate) throws IOException {
        for (int type : new int[] {0, 0x0A, 0x0C}) {
            RoomEntityRuntime runtime = runtime(type);
            int slot = slot(type);
            RoomEntity before = runtime.snapshot().slots().get(slot);
            setGate(runtime, gate, true);
            tick(runtime, 0, (entity, direction, x, y) -> {
                throw new AssertionError("Paused projectile reached collision probe");
            });
            RoomEntity paused = runtime.snapshot().slots().get(slot);
            assertEquals(before.x(), paused.x());
            assertEquals(before.y(), paused.y());
            assertEquals(before.z(), paused.z());
            assertEquals(0, countdown(runtime, type));
            setGate(runtime, gate, false);
            tick(runtime, 1, null);
            assertEquals(before.x() + (type == 0 ? 3 : 2),
                runtime.snapshot().slots().get(slot).x());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void reboundMovementAndTimersUseTheirSeparateRomGates(String gate) throws IOException {
        for (int type : new int[] {0, 0x0A, 0x0C}) {
            RoomEntityRuntime runtime = runtime(type);
            tick(runtime, 0, (entity, direction, x, y) -> true);
            assertEquals(0x18, countdown(runtime, type));
            int slot = slot(type);
            RoomEntity before = runtime.snapshot().slots().get(slot);
            setGate(runtime, gate, true);
            tick(runtime, 1, null);
            RoomEntity paused = runtime.snapshot().slots().get(slot);
            assertEquals(before.x(), paused.x());
            assertEquals(before.y(), paused.y());
            assertEquals(before.z(), paused.z());
            assertEquals(gate.equals("got_item") ? 0x17 : 0x18, countdown(runtime, type));
            assertTrue(runtime.consumePendingEntityEvents().isEmpty());
            setGate(runtime, gate, false);
            tick(runtime, 2, null);
            assertEquals(gate.equals("got_item") ? 0x16 : 0x17, countdown(runtime, type));
            assertEquals((before.z() + 1) & 0xFF, runtime.snapshot().slots().get(slot).z());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 0x0A, 0x0C})
    void ocarinaFreezesReboundTimerButNotGravity(int type) throws IOException {
        RoomEntityRuntime runtime = runtime(type);
        tick(runtime, 0, (entity, direction, x, y) -> true);
        RoomEntity before = runtime.snapshot().slots().get(slot(type));
        runtime.setOcarinaPlaybackForTest(1, 0, 0);
        tick(runtime, 1, null);
        assertEquals(0x18, countdown(runtime, type));
        assertEquals(before.z() + 1, runtime.snapshot().slots().get(slot(type)).z());
    }

    @ParameterizedTest
    @ValueSource(ints = {0x0A, 0x0C})
    void shieldCollisionBeforePauseIsConsumedWhenTheHandlerResumes(int type) throws IOException {
        RoomEntityRuntime runtime = runtime(type);
        runtime.setDialogActive(true);
        var events = runtime.tickWithProjectileEvents(0, 0x40, 0x50, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 1, true));
        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.SHIELD_BLOCK, events.getFirst().kind());
        assertEquals(0, countdown(runtime, type));
        assertEquals(0x40, runtime.snapshot().slots().get(0).x());

        runtime.setDialogActive(false);
        tick(runtime, 1, null);
        assertEquals(0x18, countdown(runtime, type));
        assertEquals(0x42, runtime.snapshot().slots().get(0).x());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        assertEquals(4, runtime.swordMoblinAlertingSoundCounter());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 0x0A, 0x0C})
    void countdownOneUnloadsOnlyWhenTheHandlerCanRun(int type) throws IOException {
        RoomEntityRuntime runtime = runtime(type);
        tick(runtime, 0, (entity, direction, x, y) -> true);
        for (int frame = 1; frame <= 22; frame++) {
            tick(runtime, frame, null);
        }
        assertEquals(2, countdown(runtime, type));
        runtime.setWitchGotItemPresentationActiveForTest(true);
        tick(runtime, 23, null);
        assertEquals(1, countdown(runtime, type));
        assertTrue(runtime.snapshot().slots().get(slot(type)).loaded());

        runtime.setWitchGotItemPresentationActiveForTest(false);
        runtime.setOcarinaPlaybackForTest(1, 0, 0);
        tick(runtime, 24, null);
        assertFalse(runtime.snapshot().slots().get(slot(type)).loaded());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 0x0C})
    void pausedReboundRendersTheStoredSpinFrameWithoutAdvancingIt(int type) throws IOException {
        RoomEntityRuntime runtime = runtime(type);
        tick(runtime, 0, (entity, direction, x, y) -> true);
        tick(runtime, 1, null);
        assertEquals(0, runtime.snapshot().slots().get(slot(type)).spriteVariant());
        runtime.setDialogActive(true);
        for (int frame = 2; frame <= 10; frame++) {
            tick(runtime, frame, null);
            assertEquals(1, runtime.snapshot().slots().get(slot(type)).spriteVariant());
            assertEquals(0x17, countdown(runtime, type));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 0x0A, 0x0C})
    void creditsAllowMovementOutsideOrdinaryWorldGameplay(int type) throws IOException {
        RoomEntityRuntime runtime = runtime(type);
        int startX = runtime.snapshot().slots().get(slot(type)).x();
        runtime.setGameplayWorldForTest(false);
        runtime.setTransitionSequenceCounterForTest(3);
        runtime.tick(0, 0, 0, () -> 0, true);
        assertEquals(startX + (type == 0 ? 3 : 2),
            runtime.snapshot().slots().get(slot(type)).x());
    }

    private static RoomEntityRuntime runtime(int type) throws IOException {
        byte[] rom;
        try (var stream = ArrowPauseTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing shipped ROM");
            }
            rom = stream.readAllBytes();
        }
        var catalog = new EntitySpriteHandlerCatalog(rom);
        var slots = new ArrayList<RoomEntity>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        if (type != 0) {
            slots.set(0, new RoomEntity(0, -1, type, 0x40, 0x50, EntityStatus.ACTIVE,
                catalog.forEntityType(type, EntityRoomLoader.RoomTable.OVERWORLD, -1), 0));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots),
            false, () -> 0, catalog);
        if (type == 0) {
            assertEquals(15, runtime.spawnArrow(0x40, 0x50, 0, 0));
        }
        return runtime;
    }

    private static int slot(int type) {
        return type == 0 ? 15 : 0;
    }

    private static int countdown(RoomEntityRuntime runtime, int type) {
        return type == 0 ? runtime.playerArrowTransitionCountdown(15)
            : runtime.enemyProjectileTransitionCountdown(0);
    }

    private static void tick(RoomEntityRuntime runtime, int frame,
                             RoomEntityBackgroundCollision collision) {
        runtime.tickWithProjectileEvents(frame, 0, 0, () -> 0, collision,
            EnemyProjectileCollision.LinkState.nonInteractive());
    }

    private static void setGate(RoomEntityRuntime runtime, String gate, boolean active) {
        switch (gate) {
            case "dialog" -> runtime.setDialogActive(active);
            case "inventory" -> runtime.setTalkState(active, 0, 0x80);
            case "transition" -> runtime.setRoomTransitionStateForTest(active);
            case "gameplay" -> runtime.setGameplayWorldForTest(!active);
            case "sequence" -> runtime.setTransitionSequenceCounterForTest(active ? 3 : 4);
            case "got_item" -> runtime.setWitchGotItemPresentationActiveForTest(active);
            default -> throw new IllegalArgumentException("Unexpected gate");
        }
    }
}
