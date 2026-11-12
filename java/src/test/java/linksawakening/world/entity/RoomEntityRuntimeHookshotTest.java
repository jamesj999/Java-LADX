package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.HookshotChainMotion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class RoomEntityRuntimeHookshotTest {

    @Test
    void spawnUsesFreeEntitySlotAndCopiesRomProjectileState() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();

        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0x07, 3);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, slot);
        RoomEntity entity = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x03, entity.type());
        assertEquals(EntityStatus.ACTIVE, entity.status());
        assertEquals(0x40, entity.x());
        assertEquals(0x50, entity.y());
        assertEquals(0x08, entity.z());
        assertEquals(0xC2, runtime.enemies().physicsFlags(slot));
        assertEquals(0x12, runtime.options1(slot));
        assertTrue(runtime.inspection().hookshotActive());
    }

    @Test
    void duplicateLaunchIsRejectedUntilTheExistingChainIsCleared() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();

        assertTrue(runtime.spawnHookshotChain(0x40, 0x50, 0, 0) >= 0);
        assertEquals(-1, runtime.spawnHookshotChain(0x40, 0x50, 0, 0));

        runtime.clearEntity(runtime.inspection().hookshotSlot());

        assertFalse(runtime.inspection().hookshotActive());
        assertTrue(runtime.spawnHookshotChain(0x40, 0x50, 0, 0) >= 0);
    }

    @Test
    void launchIsRejectedWhenEveryEntitySlotIsOccupied() {
        List<RoomEntity> slots = new ArrayList<>();
        EntitySpriteDefinition unsupported = EntitySpriteDefinition.unsupported(0xFF);
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(new RoomEntityBuilder()
                    .slot(slot)
                    .sourceLoadOrder(0)
                    .spriteDefinition(unsupported)
                    .spriteVariant(-1)
                    .build());
        }

        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(new RoomEntitySnapshot(slots)).build();

        assertEquals(-1, runtime.spawnHookshotChain(0x40, 0x50, 0, 0));
        assertFalse(runtime.inspection().hookshotActive());
    }

    @Test
    void outboundEntityAdvancesWithRomSpeed() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertEquals(0x43, runtime.inspection().snapshot().slots().get(slot).x());
        assertEquals(0x29, new EntityRoomInspectionQueries(runtime.inspection()).hookshotTransitionCountdown(slot));
    }

    @Test
    void returnVectorBringsAnOpenRoomChainBackToLink() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        runtime.spawnHookshotChain(0x40, 0x50, 0, 0);

        for (int frame = 0; frame < 0x2A + 0x2A; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x40, 0x50, () -> 0));
        }

        assertFalse(runtime.inspection().hookshotActive());
    }

    @Test
    void sharedBackgroundCollisionStopsTheChainAndDefersTheWallPoke() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);

        runtime.tick(
                EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0, (entity, direction, nextX, nextY) -> true));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(slot).status());
        assertTrue(runtime.inspection().hookshotActive());
        assertTrue(new EntityRoomInspectionQueries(runtime.inspection()).hookshotWallCollisionPending(slot));

        runtime.tick(
                EntityTestFrames.frame(runtime, 1, 0x40, 0x50, () -> 0, (entity, direction, nextX, nextY) -> true));

        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).hookshotTransitionCountdown(slot));
        assertFalse(new EntityRoomInspectionQueries(runtime.inspection()).hookshotWallCollisionPending(slot));
        // On the next pass the source moves before checking the latched
        // collision, then emits the poke at the cached pre-movement position.
        assertEquals(0x43, runtime.inspection().snapshot().slots().get(slot).x());
        assertEquals(0x40, runtime.enemies().transientVfxRequests().getFirst().worldX());
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x07, events.getFirst().soundId());
    }

    @Test
    void richHookshotableCollisionEntersPullingStateBelowThePointBlankCutoff() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);
        boolean[] blocked = {false};
        runtime.configuration()
                .setBackgroundInteraction((entity, direction, nextX, nextY) -> blocked[0]
                        ? EntityBackgroundCollisionResult.blocked(direction, 0xA0, 0x60, nextX, nextY)
                        : EntityBackgroundCollisionResult.passableWithObject(
                                direction, EntityBackgroundCollisionResult.NO_OBJECT, 0, nextX, nextY));

        for (int frame = 0; frame < 5; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x40, 0x50, () -> 0));
        }
        blocked[0] = true;
        runtime.tick(EntityTestFrames.frame(runtime, 5, 0x40, 0x50, () -> 0));

        assertTrue(runtime.inspection().hookshotActive());
        assertEquals(
                HookshotChainMotion.PULLING_STATE,
                new EntityRoomInspectionQueries(runtime.inspection()).hookshotEntityState(slot));
        assertEquals(0x4F, runtime.inspection().snapshot().slots().get(slot).x());
    }

    @Test
    void pointBlankHookshotableCollisionUnloadsAtTheRomCutoff() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);
        runtime.configuration()
                .setBackgroundInteraction((entity, direction, nextX, nextY) ->
                        EntityBackgroundCollisionResult.blocked(direction, 0xA0, 0x60, nextX, nextY));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(slot).status());
        assertFalse(runtime.inspection().hookshotActive());
    }

    @Test
    void pullingStateEmitsInverseLinkMotionAfterTheChainStops() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        int slot = runtime.spawnHookshotChain(0x50, 0x50, 0, 0);
        boolean[] blocked = {false};
        runtime.configuration()
                .setBackgroundInteraction((entity, direction, nextX, nextY) -> blocked[0]
                        ? EntityBackgroundCollisionResult.blocked(direction, 0xA0, 0x60, nextX, nextY)
                        : EntityBackgroundCollisionResult.passableWithObject(
                                direction, EntityBackgroundCollisionResult.NO_OBJECT, 0, nextX, nextY));
        for (int frame = 0; frame < 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x40, 0x50, () -> 0));
        }
        blocked[0] = true;
        runtime.tick(EntityTestFrames.frame(runtime, 6, 0x40, 0x50, () -> 0));
        assertEquals(
                HookshotChainMotion.PULLING_STATE,
                new EntityRoomInspectionQueries(runtime.inspection()).hookshotEntityState(slot));

        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                7,
                0x40,
                0x50,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 0, false)));

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.HOOKSHOT_PULL, events.getFirst().kind());
        assertEquals(0x30, events.getFirst().linkSpeedX());
    }

    @Test
    void activeHookshotEmitsTheRomNoiseOnEveryFourthFrame() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        runtime.spawnHookshotChain(0x40, 0x50, 0, 0);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        EntityCombatEvent event = runtime.consumePendingEntityEvents().getFirst();
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, event.soundChannel());
        assertEquals(0x0B, event.soundId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void pauseGatesStopHookshotMovementSoundAndObjectProbes(String gate) {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);
        setGate(runtime, gate, true);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x20, () -> 0, (entity, direction, x, y) -> {
            throw new AssertionError("Paused hookshot reached background collision");
        }));
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(slot).x());
        assertEquals(
                gate.equals("got_item") ? 0x29 : 0x2A,
                new EntityRoomInspectionQueries(runtime.inspection()).hookshotTransitionCountdown(slot));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        setGate(runtime, gate, false);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x20, () -> 0));
        assertEquals(0x43, runtime.inspection().snapshot().slots().get(slot).x());
    }

    @Test
    void creditsTickTheTimerButSkipTheBankEighteenHandler() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x20, () -> 0, true));
        assertEquals(0x29, new EntityRoomInspectionQueries(runtime.inspection()).hookshotTransitionCountdown(slot));
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(slot).x());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ocarinaCancelsTheChainEvenDuringADialog(boolean dialog) {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot()).build();
        runtime.spawnHookshotChain(0x40, 0x50, 0, 0);
        runtime.configuration().setDialogActive(dialog);
        EntityRuntimeFixture.debug(runtime).setOcarinaPlaybackForTest(1, 0, 0);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertFalse(runtime.inspection().hookshotActive());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
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

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return new RoomEntitySnapshot(slots);
    }
}
