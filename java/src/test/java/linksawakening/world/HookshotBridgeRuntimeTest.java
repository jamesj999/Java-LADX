package linksawakening.world;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HookshotBridgeRuntimeTest {

    @Test
    void passableBridgeCellSpawnsTheRomBridgeAndEmitsItsRoomRewrite() {
        RoomEntityRuntime runtime = emptyIndoorRuntime();
        runtime.spawnHookshotChain(0x40, 0x40, 0, 2);
        runtime.setObjectQuery(entity -> new RoomEntityObjectSample(0x9E, 0, 0x20, 0x20));

        runtime.tick(0, 0x40, 0x40, () -> 0);

        RoomEntity bridge = runtime.snapshot().loadedEntities().stream()
            .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
            .findFirst().orElseThrow();
        assertEquals(0x28, bridge.x());
        assertEquals(0x30, bridge.y());
        assertEquals(1, runtime.hookshotBridgeUpdates().size());
        RoomEntityRuntime.HookshotBridgeUpdate update = runtime.hookshotBridgeUpdates().get(0);
        assertEquals(0x20, update.objectLeft());
        assertEquals(0x20, update.objectTop());
        assertEquals(0, update.direction());
        assertEquals(3, runtime.snapshot().hookshotChainOam().size());
    }

    @Test
    void wallCollisionDoesNotTriggerApassableBridgePath() {
        RoomEntityRuntime runtime = emptyIndoorRuntime();
        runtime.spawnHookshotChain(0x40, 0x40, 0, 2);
        runtime.setObjectQuery(entity -> new RoomEntityObjectSample(0x9E, 0, 0x20, 0x20));
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) ->
            EntityBackgroundCollisionResult.blocked(direction, 0x01, 0, nextX, nextY));

        runtime.tick(0, 0x40, 0x40, () -> 0);

        assertTrue(runtime.hookshotBridgeUpdates().isEmpty());
        assertFalse(runtime.snapshot().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE));
        assertTrue(runtime.hookshotWallCollisionPending(runtime.hookshotSlot()));
    }

    @Test
    void positiveHookshotYSpeedSelectsTheRomPullUpBridgeObject() {
        RoomEntityRuntime runtime = emptyIndoorRuntime();
        runtime.spawnHookshotChain(0x40, 0x40, 0, 3);
        runtime.setObjectQuery(entity -> new RoomEntityObjectSample(0x9F, 0, 0x20, 0x20));

        runtime.tick(0, 0x40, 0x40, () -> 0);

        RoomEntityRuntime.HookshotBridgeUpdate update = runtime.hookshotBridgeUpdates().get(0);
        assertEquals(1, update.direction());
        RoomEntity bridge = findBridge(runtime);
        assertEquals(0x30, bridge.y());
    }

    @Test
    void bridgeMovesThenClearsWhenTheRewrittenObjectIsGone() {
        RoomEntityRuntime runtime = emptyIndoorRuntime();
        runtime.spawnHookshotChain(0x40, 0x40, 0, 2);
        AtomicBoolean firstChainSample = new AtomicBoolean(true);
        runtime.setObjectQuery(entity -> {
            if (entity.type() == HookshotBridgeMotion.ENTITY_TYPE) {
                return firstChainSample.get()
                    ? new RoomEntityObjectSample(0x9E, 0, 0x20, 0x20)
                    : new RoomEntityObjectSample(0x9D, 0, 0x20, 0x20);
            }
            if (firstChainSample.getAndSet(false)) {
                return new RoomEntityObjectSample(0x9E, 0, 0x20, 0x20);
            }
            return new RoomEntityObjectSample(0x9D, 0, 0x20, 0x20);
        });

        runtime.tick(0, 0x40, 0x40, () -> 0);
        RoomEntity firstBridge = findBridge(runtime);
        runtime.tick(1, 0x40, 0x40, () -> 0);
        RoomEntity movedBridge = findBridge(runtime);
        assertEquals((firstBridge.y() + 0x03) & 0xFF, movedBridge.y());
        assertEquals(1, runtime.hookshotBridgeUpdates().size());

        runtime.setObjectQuery(entity -> new RoomEntityObjectSample(0x00, 0, 0x20, 0x20));
        runtime.tick(2, 0x40, 0x40, () -> 0);
        assertFalse(runtime.snapshot().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE));
    }

    private static RoomEntityRuntime emptyIndoorRuntime() {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return RoomEntityRuntime.from(new RoomEntitySnapshot(slots), true, () -> 0);
    }

    private static RoomEntity findBridge(RoomEntityRuntime runtime) {
        return runtime.snapshot().loadedEntities().stream()
            .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
            .findFirst().orElseThrow();
    }
}
