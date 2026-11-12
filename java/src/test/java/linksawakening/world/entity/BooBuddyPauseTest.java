package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.motion.BooBuddyMotion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@linksawakening.testing.RequiresRom
final class BooBuddyPauseTest {
    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void pausedRenderingUsesTheFrameSavedByTheLastInteractiveHandler(String gate) throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.tick(EntityTestFrames.frame(runtime, 16, 0x70, 0x50, () -> 0));
        assertEquals(0, runtime.inspection().snapshot().slots().getFirst().spriteVariant());
        setGate(runtime, gate, true);
        for (int frame = 17; frame < 40; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x20, 0x50, () -> 0));
            assertEquals(1, runtime.inspection().snapshot().slots().getFirst().spriteVariant());
        }
    }

    @Test
    void recoilDisplaysTheSavedVariantWithoutAdvancingAnimation() throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.tick(EntityTestFrames.frame(runtime, 16, 0x70, 0x50, () -> 0));
        EntityRuntimeFixture.debug(runtime).configureEnemyRecoilForTest(0, 0x20, 0x50, 8);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 2);
        runtime.tick(EntityTestFrames.frame(runtime, 32, 0x20, 0x50, () -> 0));
        assertEquals(1, runtime.inspection().snapshot().slots().getFirst().spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 33, 0x20, 0x50, () -> 0));
        assertEquals(1, runtime.inspection().snapshot().slots().getFirst().spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 34, 0x20, 0x50, () -> 0));
        assertEquals(1, runtime.inspection().snapshot().slots().getFirst().spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 35, 0x20, 0x50, () -> 0));
        assertEquals(2, runtime.inspection().snapshot().slots().getFirst().spriteVariant());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void pausesMovementAndKeepsTheSeparateSharedTimerGate(String gate) throws IOException {
        RoomEntityRuntime runtime = runtime();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, () -> 0));
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 8);
        setGate(runtime, gate, true);
        RoomEntity before = runtime.inspection().snapshot().slots().getFirst();
        runtime.tick(EntityTestFrames.frame(runtime, 16, 0x20, 0x50, () -> {
            throw new AssertionError("Paused handler must skip steering");
        }));
        assertEquals(before, runtime.inspection().snapshot().slots().getFirst());
        assertEquals(4, runtime.flyingEnemies().booBuddySpeedX(0));
        assertEquals(gate.equals("got_item") ? 7 : 8, runtime.flyingEnemies().booBuddyTransitionCountdown(0));
        setGate(runtime, gate, false);
        runtime.tick(EntityTestFrames.frame(runtime, 17, 0x20, 0x50, () -> 0));
        assertEquals(0, runtime.flyingEnemies().booBuddySpeedX(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void recoilWaitsForTheHandlerEvenWhenSharedTimersCanTick(String gate) throws IOException {
        RoomEntityRuntime runtime = runtime();
        EntityRuntimeFixture.debug(runtime).configureEnemyRecoilForTest(0, 0x20, 0x50, 0x20);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 2);
        EntityRuntimeFixture.debug(runtime).setBooBuddyTriggerCountForTest(1);
        setGate(runtime, gate, true);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x50, () -> 0));
        assertEquals(0x40, runtime.inspection().snapshot().slots().getFirst().x());
        assertEquals(2, runtime.enemies().enemyIgnoreHitsCountdown(0));
        setGate(runtime, gate, false);
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                0x20,
                0x50,
                () -> 0,
                (entity, direction, x, y) -> {
                    throw new AssertionError("Boo Buddy recoil must skip background collision");
                },
                new EnemyProjectileCollision.LinkState(0x20, 0x50, 0, 0, 0, false)));
        assertEquals(0x42, runtime.inspection().snapshot().slots().getFirst().x());
        assertEquals(1, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0, runtime.flyingEnemies().booBuddyState(0));
        assertEquals(0, runtime.flyingEnemies().booBuddySpeedX(0));
    }

    @Test
    void reappearanceTimerTicksDuringGotItemPresentationWhileTheHandlerWaits() throws IOException {
        RoomEntityRuntime runtime = runtime();
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 2);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, () -> 0));
        assertEquals(0x60, runtime.inspection().snapshot().slots().getFirst().x());
        EntityRuntimeFixture.debug(runtime).setWitchGotItemPresentationActiveForTest(true);
        for (int frame = 1; frame <= 32; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> {
                throw new AssertionError("Got-item handler must remain paused");
            }));
        }
        EntityRuntimeFixture.debug(runtime).setWitchGotItemPresentationActiveForTest(false);
        runtime.tick(EntityTestFrames.frame(runtime, 33, 0x70, 0x50, () -> 0));
        assertEquals(4, runtime.flyingEnemies().booBuddySpeedX(0));
    }

    @Test
    void reappearanceTimerRemainsFrozenThroughoutDialog() throws IOException {
        RoomEntityRuntime runtime = runtime();
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 2);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, () -> 0));
        runtime.configuration().setDialogActive(true);
        for (int frame = 1; frame <= 32; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> 0));
        }
        runtime.configuration().setDialogActive(false);
        for (int frame = 33; frame < 64; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> {
                throw new AssertionError("Reappearance timer expired too early");
            }));
        }
        assertEquals(0, runtime.flyingEnemies().booBuddySpeedX(0));
        runtime.tick(EntityTestFrames.frame(runtime, 64, 0x70, 0x50, () -> 0));
        assertEquals(4, runtime.flyingEnemies().booBuddySpeedX(0));
    }

    @Test
    void finalRecoilTickSkipsStateDispatchEvenWithoutAConfiguredVelocity() throws IOException {
        RoomEntityRuntime runtime = runtime();
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setBooBuddyTriggerCountForTest(1);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x50, () -> 0));
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0, runtime.flyingEnemies().booBuddyState(0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x50, () -> 0));
        assertEquals(1, runtime.flyingEnemies().booBuddyState(0));
    }

    @Test
    void ocarinaFreezesTheTransitionTimerButDoesNotPauseRecoil() throws IOException {
        RoomEntityRuntime runtime = runtime();
        EntityRuntimeFixture.debug(runtime).configureEnemyRecoilForTest(0, 0x20, 0x50, 0x20);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 2);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 8);
        EntityRuntimeFixture.debug(runtime).setOcarinaPlaybackForTest(1, 0, 0);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x50, () -> 0));
        assertEquals(0x42, runtime.inspection().snapshot().slots().getFirst().x());
        assertEquals(1, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(8, runtime.flyingEnemies().booBuddyTransitionCountdown(0));
    }

    @Test
    void creditsRunTheHandlerOutsideTheOrdinaryWorldSequence() throws IOException {
        RoomEntityRuntime runtime = runtime();
        EntityRuntimeFixture.debug(runtime).setGameplayWorldForTest(false);
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(3);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, () -> 0, true));
        assertEquals(4, runtime.flyingEnemies().booBuddySpeedX(0));
    }

    private static void setGate(RoomEntityRuntime runtime, String gate, boolean active) {
        switch (gate) {
            case "dialog" -> runtime.configuration().setDialogActive(active);
            case "inventory" -> runtime.configuration().setTalkState(active, 0, 0x80);
            case "transition" -> EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(active);
            case "gameplay" -> EntityRuntimeFixture.debug(runtime).setGameplayWorldForTest(!active);
            case "sequence" -> EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(active ? 3 : 4);
            case "got_item" -> EntityRuntimeFixture.debug(runtime).setWitchGotItemPresentationActiveForTest(active);
            default -> throw new IllegalArgumentException(gate);
        }
    }

    private static RoomEntityRuntime runtime() throws IOException {
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
                new linksawakening.world.entity.RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(BooBuddyMotion.ENTITY_TYPE)
                        .at(0x40, 0x50)
                        .spriteDefinition(
                                catalog.forEntityType(BooBuddyMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.INDOORS_A))
                        .build());
        return RoomEntityRuntime.builder(new RoomEntitySnapshot(slots))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
    }
}
