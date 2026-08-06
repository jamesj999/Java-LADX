package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomEntityRuntimeHookshotTest {

    @Test
    void spawnUsesFreeEntitySlotAndCopiesRomProjectileState() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());

        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0x07, 3);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, slot);
        RoomEntity entity = runtime.snapshot().slots().get(slot);
        assertEquals(0x03, entity.type());
        assertEquals(EntityStatus.ACTIVE, entity.status());
        assertEquals(0x40, entity.x());
        assertEquals(0x50, entity.y());
        assertEquals(0x08, entity.z());
        assertEquals(0xC2, runtime.physicsFlags(slot));
        assertEquals(0x12, runtime.options1(slot));
        assertTrue(runtime.hookshotActive());
    }

    @Test
    void duplicateLaunchIsRejectedUntilTheExistingChainIsCleared() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());

        assertTrue(runtime.spawnHookshotChain(0x40, 0x50, 0, 0) >= 0);
        assertEquals(-1, runtime.spawnHookshotChain(0x40, 0x50, 0, 0));

        runtime.clearEntity(runtime.hookshotSlot());

        assertFalse(runtime.hookshotActive());
        assertTrue(runtime.spawnHookshotChain(0x40, 0x50, 0, 0) >= 0);
    }

    @Test
    void launchIsRejectedWhenEveryEntitySlotIsOccupied() {
        List<RoomEntity> slots = new ArrayList<>();
        EntitySpriteDefinition unsupported = EntitySpriteDefinition.unsupported(0xFF);
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(new RoomEntity(slot, 0, 0, 0, 0, EntityStatus.ACTIVE,
                unsupported, -1));
        }

        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots));

        assertEquals(-1, runtime.spawnHookshotChain(0x40, 0x50, 0, 0));
        assertFalse(runtime.hookshotActive());
    }

    @Test
    void outboundEntityAdvancesWithRomSpeed() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);

        runtime.tick(0, 0x40, 0x50, () -> 0);

        assertEquals(0x43, runtime.snapshot().slots().get(slot).x());
        assertEquals(0x29, runtime.hookshotTransitionCountdown(slot));
    }

    @Test
    void returnVectorBringsAnOpenRoomChainBackToLink() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());
        runtime.spawnHookshotChain(0x40, 0x50, 0, 0);

        for (int frame = 0; frame < 0x2A + 0x2A; frame++) {
            runtime.tick(frame, 0x40, 0x50, () -> 0);
        }

        assertFalse(runtime.hookshotActive());
    }

    @Test
    void sharedBackgroundCollisionStopsTheChainAndDefersTheWallPoke() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);

        runtime.tick(0, 0x40, 0x50, () -> 0,
            (entity, direction, nextX, nextY) -> true);

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(slot).status());
        assertTrue(runtime.hookshotActive());
        assertTrue(runtime.hookshotWallCollisionPending(slot));

        runtime.tick(1, 0x40, 0x50, () -> 0,
            (entity, direction, nextX, nextY) -> true);

        assertEquals(0, runtime.hookshotTransitionCountdown(slot));
        assertFalse(runtime.hookshotWallCollisionPending(slot));
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x07, events.getFirst().soundId());
    }

    @Test
    void richHookshotableCollisionEntersPullingStateBelowThePointBlankCutoff() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);
        boolean[] blocked = {false};
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) -> blocked[0]
            ? EntityBackgroundCollisionResult.blocked(direction, 0xA0, 0x60, nextX, nextY)
            : EntityBackgroundCollisionResult.passableWithObject(
                direction, EntityBackgroundCollisionResult.NO_OBJECT, 0, nextX, nextY));

        for (int frame = 0; frame < 5; frame++) {
            runtime.tick(frame, 0x40, 0x50, () -> 0);
        }
        blocked[0] = true;
        runtime.tick(5, 0x40, 0x50, () -> 0);

        assertTrue(runtime.hookshotActive());
        assertEquals(HookshotChainMotion.PULLING_STATE, runtime.hookshotEntityState(slot));
        assertEquals(0x4F, runtime.snapshot().slots().get(slot).x());
    }

    @Test
    void pointBlankHookshotableCollisionUnloadsAtTheRomCutoff() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) ->
            EntityBackgroundCollisionResult.blocked(direction, 0xA0, 0x60, nextX, nextY));

        runtime.tick(0, 0x40, 0x50, () -> 0);

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(slot).status());
        assertFalse(runtime.hookshotActive());
    }

    @Test
    void pullingStateEmitsInverseLinkMotionAfterTheChainStops() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());
        int slot = runtime.spawnHookshotChain(0x50, 0x50, 0, 0);
        boolean[] blocked = {false};
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) ->
            blocked[0]
                ? EntityBackgroundCollisionResult.blocked(direction, 0xA0, 0x60, nextX, nextY)
                : EntityBackgroundCollisionResult.passableWithObject(
                    direction, EntityBackgroundCollisionResult.NO_OBJECT, 0, nextX, nextY));
        for (int frame = 0; frame < 6; frame++) {
            runtime.tick(frame, 0x40, 0x50, () -> 0);
        }
        blocked[0] = true;
        runtime.tick(6, 0x40, 0x50, () -> 0);
        assertEquals(HookshotChainMotion.PULLING_STATE, runtime.hookshotEntityState(slot));

        List<EntityProjectileEvent> events = runtime.tickWithProjectileEvents(
            7, 0x40, 0x50, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 0, false));

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.HOOKSHOT_PULL, events.getFirst().kind());
        assertEquals(0x30, events.getFirst().linkSpeedX());
    }

    @Test
    void activeHookshotEmitsTheRomNoiseOnEveryFourthFrame() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());
        runtime.spawnHookshotChain(0x40, 0x50, 0, 0);

        runtime.tick(0, 0x40, 0x50, () -> 0);

        EntityCombatEvent event = runtime.consumePendingEntityEvents().getFirst();
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, event.soundChannel());
        assertEquals(0x0B, event.soundId());
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return new RoomEntitySnapshot(slots);
    }
}
