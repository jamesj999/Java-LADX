package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BooBuddyPauseTest {
    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void pausedRenderingUsesTheFrameSavedByTheLastInteractiveHandler(String gate) throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.tick(16, 0x70, 0x50, () -> 0);
        assertEquals(0, runtime.snapshot().slots().getFirst().spriteVariant());
        setGate(runtime, gate, true);
        for (int frame = 17; frame < 40; frame++) {
            runtime.tick(frame, 0x20, 0x50, () -> 0);
            assertEquals(1, runtime.snapshot().slots().getFirst().spriteVariant());
        }
    }

    @Test
    void recoilDisplaysTheSavedVariantWithoutAdvancingAnimation() throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.tick(16, 0x70, 0x50, () -> 0);
        runtime.configureEnemyRecoilForTest(0, 0x20, 0x50, 8);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 2);
        runtime.tick(32, 0x20, 0x50, () -> 0);
        assertEquals(1, runtime.snapshot().slots().getFirst().spriteVariant());
        runtime.tick(33, 0x20, 0x50, () -> 0);
        assertEquals(1, runtime.snapshot().slots().getFirst().spriteVariant());
        runtime.tick(34, 0x20, 0x50, () -> 0);
        assertEquals(1, runtime.snapshot().slots().getFirst().spriteVariant());
        runtime.tick(35, 0x20, 0x50, () -> 0);
        assertEquals(2, runtime.snapshot().slots().getFirst().spriteVariant());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void pausesMovementAndKeepsTheSeparateSharedTimerGate(String gate) throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.tick(0, 0x70, 0x50, () -> 0);
        runtime.setTransitionCountdownForTest(0, 8);
        setGate(runtime, gate, true);
        RoomEntity before = runtime.snapshot().slots().getFirst();
        runtime.tick(16, 0x20, 0x50,
            () -> { throw new AssertionError("Paused handler must skip steering"); });
        assertEquals(before, runtime.snapshot().slots().getFirst());
        assertEquals(4, runtime.booBuddySpeedX(0));
        assertEquals(gate.equals("got_item") ? 7 : 8, runtime.booBuddyTransitionCountdown(0));
        setGate(runtime, gate, false);
        runtime.tick(17, 0x20, 0x50, () -> 0);
        assertEquals(0, runtime.booBuddySpeedX(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void recoilWaitsForTheHandlerEvenWhenSharedTimersCanTick(String gate) throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.configureEnemyRecoilForTest(0, 0x20, 0x50, 0x20);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 2);
        runtime.setBooBuddyTriggerCountForTest(1);
        setGate(runtime, gate, true);
        runtime.tick(0, 0x20, 0x50, () -> 0);
        assertEquals(0x40, runtime.snapshot().slots().getFirst().x());
        assertEquals(2, runtime.enemyIgnoreHitsCountdown(0));
        setGate(runtime, gate, false);
        runtime.tickWithProjectileEvents(1, 0x20, 0x50, () -> 0,
            (entity, direction, x, y) -> {
                throw new AssertionError("Boo Buddy recoil must skip background collision");
            }, new EnemyProjectileCollision.LinkState(0x20, 0x50, 0, 0, 0, false));
        assertEquals(0x42, runtime.snapshot().slots().getFirst().x());
        assertEquals(1, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(0, runtime.booBuddyState(0));
        assertEquals(0, runtime.booBuddySpeedX(0));
    }

    @Test
    void reappearanceTimerTicksDuringGotItemPresentationWhileTheHandlerWaits() throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.setTransitionCountdownForTest(0, 2);
        runtime.tick(0, 0x70, 0x50, () -> 0);
        assertEquals(0x60, runtime.snapshot().slots().getFirst().x());
        runtime.setWitchGotItemPresentationActiveForTest(true);
        for (int frame = 1; frame <= 32; frame++) {
            runtime.tick(frame, 0x70, 0x50,
                () -> { throw new AssertionError("Got-item handler must remain paused"); });
        }
        runtime.setWitchGotItemPresentationActiveForTest(false);
        runtime.tick(33, 0x70, 0x50, () -> 0);
        assertEquals(4, runtime.booBuddySpeedX(0));
    }

    @Test
    void reappearanceTimerRemainsFrozenThroughoutDialog() throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.setTransitionCountdownForTest(0, 2);
        runtime.tick(0, 0x70, 0x50, () -> 0);
        runtime.setDialogActive(true);
        for (int frame = 1; frame <= 32; frame++) {
            runtime.tick(frame, 0x70, 0x50, () -> 0);
        }
        runtime.setDialogActive(false);
        for (int frame = 33; frame < 64; frame++) {
            runtime.tick(frame, 0x70, 0x50,
                () -> { throw new AssertionError("Reappearance timer expired too early"); });
        }
        assertEquals(0, runtime.booBuddySpeedX(0));
        runtime.tick(64, 0x70, 0x50, () -> 0);
        assertEquals(4, runtime.booBuddySpeedX(0));
    }

    @Test
    void finalRecoilTickSkipsStateDispatchEvenWithoutAConfiguredVelocity() throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 1);
        runtime.setBooBuddyTriggerCountForTest(1);
        runtime.tick(0, 0x20, 0x50, () -> 0);
        assertEquals(0, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(0, runtime.booBuddyState(0));
        runtime.tick(1, 0x20, 0x50, () -> 0);
        assertEquals(1, runtime.booBuddyState(0));
    }

    @Test
    void ocarinaFreezesTheTransitionTimerButDoesNotPauseRecoil() throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.configureEnemyRecoilForTest(0, 0x20, 0x50, 0x20);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 2);
        runtime.setTransitionCountdownForTest(0, 8);
        runtime.setOcarinaPlaybackForTest(1, 0, 0);
        runtime.tick(0, 0x20, 0x50, () -> 0);
        assertEquals(0x42, runtime.snapshot().slots().getFirst().x());
        assertEquals(1, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(8, runtime.booBuddyTransitionCountdown(0));
    }

    @Test
    void creditsRunTheHandlerOutsideTheOrdinaryWorldSequence() throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.setGameplayWorldForTest(false);
        runtime.setTransitionSequenceCounterForTest(3);
        runtime.tick(0, 0x70, 0x50, () -> 0, true);
        assertEquals(4, runtime.booBuddySpeedX(0));
    }

    private static void setGate(RoomEntityRuntime runtime, String gate, boolean active) {
        switch (gate) {
            case "dialog" -> runtime.setDialogActive(active);
            case "inventory" -> runtime.setTalkState(active, 0, 0x80);
            case "transition" -> runtime.setRoomTransitionStateForTest(active);
            case "gameplay" -> runtime.setGameplayWorldForTest(!active);
            case "sequence" -> runtime.setTransitionSequenceCounterForTest(active ? 3 : 4);
            case "got_item" -> runtime.setWitchGotItemPresentationActiveForTest(active);
            default -> throw new IllegalArgumentException(gate);
        }
    }

    private static RoomEntityRuntime runtime() throws IOException {
        byte[] rom;
        try (var stream = BooBuddyPauseTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
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
        slots.set(0, new RoomEntity(0, 0, BooBuddyMotion.ENTITY_TYPE, 0x40, 0x50,
            EntityStatus.ACTIVE, catalog.forEntityType(BooBuddyMotion.ENTITY_TYPE,
                EntityRoomLoader.RoomTable.INDOORS_A), 0));
        return RoomEntityRuntime.from(new RoomEntitySnapshot(slots), true, null, catalog);
    }
}
