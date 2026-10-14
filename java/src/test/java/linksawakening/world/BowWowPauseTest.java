package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class BowWowPauseTest {
    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "sequence", "got_item"})
    void pausesMovementGravityAndIgnoreHitsButUsesTheSharedTimerGate(String gate) throws IOException {
        RoomEntityRuntime runtime = wanderingBowWow();
        RoomEntity before = runtime.snapshot().slots().getFirst();
        runtime.setBowWowTransitionCountdownForTest(0, 8);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 3);
        setGate(runtime, gate, true);
        runtime.tick(16, 0x44, 0x58,
            () -> { throw new AssertionError("Paused BowWow must not select targets"); });
        assertEquals(before, runtime.snapshot().slots().getFirst());
        assertEquals(gate.equals("got_item") ? 7 : 8, runtime.bowWowTransitionCountdown(0));
        assertEquals(3, runtime.enemyIgnoreHitsCountdown(0));
        setGate(runtime, gate, false);
        runtime.tick(17, 0x44, 0x58, () -> 0);
        assertEquals(2, runtime.enemyIgnoreHitsCountdown(0));
        assertNotEquals(before.y(), runtime.snapshot().slots().getFirst().y());
    }

    @Test
    void ocarinaPausesSharedTimersWhileBowWowStillMoves() throws IOException {
        RoomEntityRuntime runtime = wanderingBowWow();
        RoomEntity before = runtime.snapshot().slots().getFirst();
        runtime.setBowWowTransitionCountdownForTest(0, 8);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 3);
        runtime.setOcarinaPlaybackForTest(1, 0, 0);
        runtime.tick(16, 0x44, 0x58, () -> 0);
        assertEquals(8, runtime.bowWowTransitionCountdown(0));
        assertEquals(2, runtime.enemyIgnoreHitsCountdown(0));
        assertNotEquals(before.y(), runtime.snapshot().slots().getFirst().y());
    }

    @Test
    void creditsDoNotBypassBowWowsExplicitSequenceCheck() throws IOException {
        RoomEntityRuntime runtime = wanderingBowWow();
        RoomEntity before = runtime.snapshot().slots().getFirst();
        runtime.setBowWowTransitionCountdownForTest(0, 8);
        runtime.setGameplayWorldForTest(false);
        runtime.setTransitionSequenceCounterForTest(3);
        runtime.tick(16, 0x44, 0x58, () -> 0, true);
        assertEquals(before, runtime.snapshot().slots().getFirst());
        assertEquals(7, runtime.bowWowTransitionCountdown(0));
    }

    private static void setGate(RoomEntityRuntime runtime, String gate, boolean active) {
        switch (gate) {
            case "dialog" -> runtime.setDialogActive(active);
            case "inventory" -> runtime.setTalkState(active, 0, 0x80);
            case "transition" -> runtime.setRoomTransitionStateForTest(active);
            case "sequence" -> runtime.setTransitionSequenceCounterForTest(active ? 3 : 4);
            case "got_item" -> runtime.setWitchGotItemPresentationActiveForTest(active);
            default -> throw new IllegalArgumentException(gate);
        }
    }

    private static RoomEntityRuntime wanderingBowWow() throws IOException {
        byte[] rom;
        try (var stream = BowWowPauseTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
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
        slots.set(0, new RoomEntity(0, -1, 0x6D, 0x40, 0x50, EntityStatus.ACTIVE,
            catalog.forFollowerEntityType(0x6D), 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.setFollowingNpcState(new FollowingNpcState(false, 0, false, true, 0, 0, false));
        for (int frame = 0; frame < 6; frame++) {
            runtime.tick(frame, 0x44, 0x58, () -> 0);
        }
        return runtime;
    }
}
