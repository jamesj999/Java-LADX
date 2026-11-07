package linksawakening.world.entity;



import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.RoomEntitySnapshot;

import linksawakening.world.entity.motion.HookshotBridgeMotion;

import linksawakening.world.entity.event.HookshotBridgeUpdate;

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
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x9E, 0, 0x20, 0x20));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x40, () -> 0));

        RoomEntity bridge = runtime.inspection().snapshot().loadedEntities().stream()
            .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
            .findFirst().orElseThrow();
        assertEquals(0x28, bridge.x());
        assertEquals(0x30, bridge.y());
        assertEquals(1, runtime.inspection().hookshotBridgeUpdates().size());
        HookshotBridgeUpdate update = runtime.inspection().hookshotBridgeUpdates().get(0);
        assertEquals(0x20, update.objectLeft());
        assertEquals(0x20, update.objectTop());
        assertEquals(0, update.direction());
        assertEquals(3, runtime.inspection().snapshot().hookshotChainOam().size());
    }

    @Test
    void wallCollisionDoesNotTriggerApassableBridgePath() {
        RoomEntityRuntime runtime = emptyIndoorRuntime();
        runtime.spawnHookshotChain(0x40, 0x40, 0, 2);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x9E, 0, 0x20, 0x20));
        runtime.configuration().setBackgroundInteraction((entity, direction, nextX, nextY) ->
            EntityBackgroundCollisionResult.blocked(direction, 0x01, 0, nextX, nextY));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x40, () -> 0));

        assertTrue(runtime.inspection().hookshotBridgeUpdates().isEmpty());
        assertFalse(runtime.inspection().snapshot().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE));
        assertTrue(runtime.inspection().hookshotWallCollisionPending(runtime.inspection().hookshotSlot()));
    }

    @Test
    void positiveHookshotYSpeedSelectsTheRomPullUpBridgeObject() {
        RoomEntityRuntime runtime = emptyIndoorRuntime();
        runtime.spawnHookshotChain(0x40, 0x40, 0, 3);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x9F, 0, 0x20, 0x20));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x40, () -> 0));

        HookshotBridgeUpdate update = runtime.inspection().hookshotBridgeUpdates().get(0);
        assertEquals(1, update.direction());
        RoomEntity bridge = findBridge(runtime);
        assertEquals(0x30, bridge.y());
    }

    @Test
    void bridgeMovesThenClearsWhenTheRewrittenObjectIsGone() {
        RoomEntityRuntime runtime = emptyIndoorRuntime();
        runtime.spawnHookshotChain(0x40, 0x40, 0, 2);
        AtomicBoolean firstChainSample = new AtomicBoolean(true);
        runtime.configuration().setObjectQuery(entity -> {
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

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x40, () -> 0));
        RoomEntity firstBridge = findBridge(runtime);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x40, () -> 0));
        RoomEntity movedBridge = findBridge(runtime);
        assertEquals((firstBridge.y() + 0x03) & 0xFF, movedBridge.y());
        assertEquals(1, runtime.inspection().hookshotBridgeUpdates().size());

        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x00, 0, 0x20, 0x20));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x40, 0x40, () -> 0));
        assertFalse(runtime.inspection().snapshot().loadedEntities().stream()
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
        return runtime.inspection().snapshot().loadedEntities().stream()
            .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
            .findFirst().orElseThrow();
    }
}
