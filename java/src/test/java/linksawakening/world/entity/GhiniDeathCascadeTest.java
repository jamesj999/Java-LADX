package linksawakening.world.entity;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class GhiniDeathCascadeTest {
    private static RoomEntity entity(int slot, int type, EntityStatus status) {
        return new RoomEntity(slot, slot, type, 64, 64, status,
            linksawakening.entity.EntitySpriteDefinition.unsupported(type), 0, 0, 0, 0, -1, false);
    }

    @Test
    void cascadeDrawsOnlyForVisibleNonzeroSlotsInDescendingOrder() {
        var runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(java.util.stream.IntStream.range(0, 16)
            .mapToObj(slot -> entity(slot, 0, EntityStatus.DISABLED)).toList()), false, () -> 0);
        var killed = entity(8, 0x12, EntityStatus.DYING);
        runtime.slots[8] = killed;
        runtime.slots[15] = entity(15, 0x10, EntityStatus.ACTIVE);
        runtime.slots[12] = entity(12, 0x11, EntityStatus.STUNNED);
        runtime.slots[9] = entity(9, 0x10, EntityStatus.DISABLED);
        runtime.slots[7] = entity(7, 0x10, EntityStatus.ACTIVE);
        runtime.ghiniMotion.initialize(7, 0x10); // Hidden state 1 is excluded.
        runtime.slots[3] = entity(3, 0x12, EntityStatus.ACTIVE);
        var draws = new AtomicInteger();
        runtime.bossRewards.cascadeGhiniDeath(killed, draws::getAndIncrement);
        assertEquals(2, draws.get());
        assertEquals(0x2D, runtime.droppedItemBySlot[15]);
        assertEquals(0x2E, runtime.droppedItemBySlot[12]);
        assertEquals(0x2E, runtime.droppedItemBySlot[8]);
        for (int slot : new int[]{15, 12}) {
            assertEquals(EntityStatus.DYING, runtime.slots[slot].status());
            assertEquals(0x1F, runtime.dyingCountdown[slot]);
        }
        assertEquals(EntityStatus.ACTIVE, runtime.slots[7].status());
        assertEquals(EntityStatus.ACTIVE, runtime.slots[3].status());
        assertEquals(EntityStatus.DISABLED, runtime.slots[9].status());
    }
}
