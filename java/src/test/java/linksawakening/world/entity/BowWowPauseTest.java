package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.ArrayList;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@linksawakening.testing.RequiresRom
final class BowWowPauseTest {
    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "sequence", "got_item"})
    void pausesMovementGravityAndIgnoreHitsButUsesTheSharedTimerGate(String gate) throws IOException {
        RoomEntityRuntime runtime = wanderingBowWow();
        RoomEntity before = runtime.inspection().snapshot().slots().getFirst();
        EntityRuntimeFixture.debug(runtime).setBowWowTransitionCountdownForTest(0, 8);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 3);
        setGate(runtime, gate, true);
        runtime.tick(EntityTestFrames.frame(runtime, 16, 0x44, 0x58, () -> {
            throw new AssertionError("Paused BowWow must not select targets");
        }));
        assertEquals(before, runtime.inspection().snapshot().slots().getFirst());
        assertEquals(
                gate.equals("got_item") ? 7 : 8,
                new EntityRoomInspectionQueries(runtime.inspection()).bowWowTransitionCountdown(0));
        assertEquals(3, runtime.enemies().enemyIgnoreHitsCountdown(0));
        setGate(runtime, gate, false);
        runtime.tick(EntityTestFrames.frame(runtime, 17, 0x44, 0x58, () -> 0));
        assertEquals(2, runtime.enemies().enemyIgnoreHitsCountdown(0));
        // Speeds use four fractional bits; one tick need not cross a pixel.
        for (int frame = 18; frame < 22; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x44, 0x58, () -> 0));
        }
        assertNotEquals(
                before.y(), runtime.inspection().snapshot().slots().getFirst().y());
    }

    @Test
    void ocarinaPausesSharedTimersWhileBowWowStillMoves() throws IOException {
        RoomEntityRuntime runtime = wanderingBowWow();
        RoomEntity before = runtime.inspection().snapshot().slots().getFirst();
        EntityRuntimeFixture.debug(runtime).setBowWowTransitionCountdownForTest(0, 8);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 3);
        EntityRuntimeFixture.debug(runtime).setOcarinaPlaybackForTest(1, 0, 0);
        runtime.tick(EntityTestFrames.frame(runtime, 16, 0x44, 0x58, () -> 0));
        assertEquals(8, new EntityRoomInspectionQueries(runtime.inspection()).bowWowTransitionCountdown(0));
        assertEquals(2, runtime.enemies().enemyIgnoreHitsCountdown(0));
        // Speeds use four fractional bits; one tick need not cross a pixel.
        for (int frame = 17; frame < 21; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x44, 0x58, () -> 0));
        }
        assertNotEquals(
                before.y(), runtime.inspection().snapshot().slots().getFirst().y());
    }

    @Test
    void creditsDoNotBypassBowWowsExplicitSequenceCheck() throws IOException {
        RoomEntityRuntime runtime = wanderingBowWow();
        RoomEntity before = runtime.inspection().snapshot().slots().getFirst();
        EntityRuntimeFixture.debug(runtime).setBowWowTransitionCountdownForTest(0, 8);
        EntityRuntimeFixture.debug(runtime).setGameplayWorldForTest(false);
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(3);
        runtime.tick(EntityTestFrames.frame(runtime, 16, 0x44, 0x58, () -> 0, true));
        assertEquals(before, runtime.inspection().snapshot().slots().getFirst());
        assertEquals(7, new EntityRoomInspectionQueries(runtime.inspection()).bowWowTransitionCountdown(0));
    }

    private static void setGate(RoomEntityRuntime runtime, String gate, boolean active) {
        switch (gate) {
            case "dialog" -> runtime.configuration().setDialogActive(active);
            case "inventory" -> runtime.configuration().setTalkState(active, 0, 0x80);
            case "transition" -> EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(active);
            case "sequence" -> EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(active ? 3 : 4);
            case "got_item" -> EntityRuntimeFixture.debug(runtime).setWitchGotItemPresentationActiveForTest(active);
            default -> throw new IllegalArgumentException(gate);
        }
    }

    private static RoomEntityRuntime wanderingBowWow() throws IOException {
        byte[] rom;
        try (var stream = linksawakening.testing.TestRom.open()) {
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
        slots.set(
                0,
                new RoomEntityBuilder()
                        .type(0x6D)
                        .at(0x40, 0x50)
                        .spriteDefinition(catalog.forFollowerEntityType(0x6D))
                        .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(slots))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();
        runtime.configuration().setFollowingNpcState(new FollowingNpcState(false, 0, false, true, 0, 0, false));
        for (int frame = 0; frame < 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x44, 0x58, () -> 0));
        }
        return runtime;
    }
}
