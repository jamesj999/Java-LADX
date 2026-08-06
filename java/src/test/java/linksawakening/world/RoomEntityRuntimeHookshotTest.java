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
    void sharedBackgroundCollisionClearsTheChainAtTheBlockedPosition() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot());
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 0);

        runtime.tick(0, 0x40, 0x50, () -> 0,
            (entity, direction, nextX, nextY) -> true);

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(slot).status());
        assertFalse(runtime.hookshotActive());
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return new RoomEntitySnapshot(slots);
    }
}
