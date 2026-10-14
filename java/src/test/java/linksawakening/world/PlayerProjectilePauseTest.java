package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlayerProjectilePauseTest {
    @ParameterizedTest
    @CsvSource({"1, dialog", "4, dialog", "0xDF, dialog",
        "1, inventory", "4, inventory", "0xDF, inventory",
        "1, transition", "4, transition", "0xDF, transition",
        "1, gameplay", "4, gameplay", "0xDF, gameplay",
        "1, sequence", "4, sequence", "0xDF, sequence",
        "1, got_item", "4, got_item", "0xDF, got_item"})
    void gameplayGatesStopMovementAndObjectLookup(int type, String gate) {
        RoomEntityRuntime runtime = runtime();
        int slot = spawn(runtime, type);
        RoomEntity before = runtime.snapshot().slots().get(slot);
        setGate(runtime, gate, true);
        runtime.setObjectIntersectionQuery(entity -> {
            throw new AssertionError("Paused projectile reached object lookup: " + gate);
        });
        tick(runtime, 8, 0);
        RoomEntity paused = runtime.snapshot().slots().get(slot);
        assertEquals(before.x(), paused.x());
        assertEquals(before.y(), paused.y());
        assertEquals(before.z(), paused.z());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        if (type == 1) {
            // wC1A8 gates the handler, but UpdateEntityTimers does not read it.
            assertEquals(gate.equals("got_item") ? 0x27 : 0x28,
                runtime.boomerangTransitionCountdown(slot));
        }

        setGate(runtime, gate, false);
        runtime.setObjectIntersectionQuery(entity -> RoomEntityObjectSample.none());
        tick(runtime, 9, 0);
        assertEquals(before.x() + speed(type), runtime.snapshot().slots().get(slot).x());
    }

    @ParameterizedTest
    @CsvSource({"1, 2", "4, 2", "0xDF, 2", "1, 5", "4, 5", "0xDF, 5"})
    void linksMotionStateAloneDoesNotPauseProjectiles(int type, int motionState) {
        RoomEntityRuntime runtime = runtime();
        int slot = spawn(runtime, type);
        int beforeX = runtime.snapshot().slots().get(slot).x();
        tick(runtime, 8, motionState);
        assertEquals(beforeX + speed(type), runtime.snapshot().slots().get(slot).x());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void wallFlameUsesTheTimerGateWhileItsPaletteStillUpdates(String gate) {
        RoomEntityRuntime runtime = runtime();
        int slot = spawn(runtime, 4);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(1, 1, 0x40, 0x40));
        tick(runtime, 0, 0);
        assertEquals(0x30, runtime.magicRodFireballPrivateCountdown1(slot));
        setGate(runtime, gate, true);
        tick(runtime, 8, 0);
        tick(runtime, 9, 0);
        assertEquals(gate.equals("got_item") ? 0x2E : 0x30,
            runtime.magicRodFireballPrivateCountdown1(slot));
        // The fireball writes its next frame before ReturnIfNonInteractive.
        assertEquals(1, runtime.snapshot().slots().get(slot).spriteVariant());
    }

    @Test
    void ocarinaStopsTheBoomerangTimerWithoutStoppingItsHandler() {
        RoomEntityRuntime runtime = runtime();
        int slot = spawn(runtime, 1);
        int beforeX = runtime.snapshot().slots().get(slot).x();
        runtime.setOcarinaPlaybackForTest(1, 0, 0);
        tick(runtime, 8, 0);
        assertEquals(0x28, runtime.boomerangTransitionCountdown(slot));
        assertEquals(beforeX + 2, runtime.snapshot().slots().get(slot).x());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 0xDF})
    void creditsBypassTheWorldGameplayAndTransitionSequenceChecks(int type) {
        RoomEntityRuntime runtime = runtime();
        int slot = spawn(runtime, type);
        int beforeX = runtime.snapshot().slots().get(slot).x();
        runtime.setGameplayWorldForTest(false);
        runtime.setTransitionSequenceCounterForTest(3);
        runtime.tick(8, 0, 0, () -> 0, true);
        assertEquals(beforeX + speed(type), runtime.snapshot().slots().get(slot).x());
    }

    private static RoomEntityRuntime runtime() {
        var slots = new ArrayList<RoomEntity>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        try (var stream = PlayerProjectilePauseTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing shipped ROM");
            }
            return RoomEntityRuntime.from(new RoomEntitySnapshot(slots), false, () -> 0,
                new EntitySpriteHandlerCatalog(stream.readAllBytes()));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static int spawn(RoomEntityRuntime runtime, int type) {
        return switch (type) {
            case 1 -> runtime.spawnBoomerang(0x40, 0x50, 0, 0, 0);
            case 4 -> runtime.spawnMagicRodFireball(0x40, 0x50, 0, 0);
            case 0xDF -> {
                int slot = runtime.spawnSwordBeam(0x40, 0x50, 0, 0);
                tick(runtime, 0, 0); // State zero precedes the ROM pause check.
                runtime.consumePendingEntityEvents();
                yield slot;
            }
            default -> throw new IllegalArgumentException("Unexpected projectile");
        };
    }

    private static int speed(int type) {
        return type == 1 ? 2 : type == 4 ? 3 : 4;
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int motionState) {
        runtime.tickWithProjectileEvents(frame, 0, 0, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0, 0, 0, motionState, 0, false));
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
