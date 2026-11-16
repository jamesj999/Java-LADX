package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import linksawakening.world.entity.motion.GhiniMotion;
import org.junit.jupiter.api.Test;

class GhiniDeathCascadeTest {
    private static RoomEntity entity(int slot, int type, EntityStatus status) {
        return new RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(slot)
                .type(type)
                .at(64, 64)
                .status(status)
                .spriteDefinition(linksawakening.entity.EntitySpriteDefinition.unsupported(type))
                .build();
    }

    @Test
    void cascadeDrawsOnlyForVisibleNonzeroSlotsInDescendingOrder() {
        var runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(java.util.stream.IntStream.range(0, 16)
                        .mapToObj(slot -> entity(slot, 0, EntityStatus.DISABLED))
                        .toList()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .build();
        var killed = entity(8, 0x12, EntityStatus.DYING);
        runtime.slots.replace(8, killed);
        runtime.slots.replace(15, entity(15, 0x10, EntityStatus.ACTIVE));
        runtime.slots.replace(12, entity(12, 0x11, EntityStatus.STUNNED));
        runtime.slots.replace(9, entity(9, 0x10, EntityStatus.DISABLED));
        runtime.slots.replace(7, entity(7, 0x10, EntityStatus.ACTIVE));
        runtime.entityState
                .motions
                .get(GhiniMotion.class, GhiniMotion::new)
                .initialize(7, 0x10); // Hidden state 1 is excluded.
        runtime.slots.replace(3, entity(3, 0x12, EntityStatus.ACTIVE));
        var draws = new AtomicInteger();
        runtime.bossRewards.cascadeGhiniDeath(killed, draws::getAndIncrement);
        assertEquals(2, draws.get());
        assertEquals(0x2D, runtime.bossRewards.state.readDroppedItemBySlot(15));
        assertEquals(0x2E, runtime.bossRewards.state.readDroppedItemBySlot(12));
        assertEquals(0x2E, runtime.bossRewards.state.readDroppedItemBySlot(8));
        for (int slot : new int[] {15, 12}) {
            assertEquals(EntityStatus.DYING, runtime.slots.get(slot).status());
            assertEquals(0x1F, runtime.entityState.combatTables.readDyingCountdown(slot));
        }
        assertEquals(EntityStatus.ACTIVE, runtime.slots.get(7).status());
        assertEquals(EntityStatus.ACTIVE, runtime.slots.get(3).status());
        assertEquals(EntityStatus.DISABLED, runtime.slots.get(9).status());
    }
}
