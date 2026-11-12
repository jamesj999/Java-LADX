package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

/** Active sword collision, including both immutable outcomes and the owned drain. */
class EntityCombatAllocationTest {
    @Test
    void overlappingTwoEnemies() {
        var first = TestEntities.active(9).slot(0).at(64, 64).build();
        var second = TestEntities.active(9).slot(1).at(72, 64).build();
        var runtime =
                RoomEntityRuntime.builder(TestEntities.room(first, second)).build();
        var frame =
                new EnemyCombatFrame(0, 120, 120, false, 0, true, true, 56, 32, 56, 24, EnemyAttackContext.standard());
        Runnable hit = () -> {
            runtime.slots[0] = first;
            runtime.slots[1] = second;
            for (int slot = 0; slot < 2; slot++) {
                runtime.entityState.combatTables.writeEnemyHealth(slot, 4);
                runtime.entityState.combatTables.writeEnemyFlashCountdown(slot, 0);
                runtime.entityState.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
            }
            var events = runtime.resolveCombat(frame);
            assertEquals(2, events.size());
            assertTrue(events.get(0).swordHit());
            assertTrue(events.get(1).swordHit());
            assertEquals(1, events.get(0).slot());
            assertEquals(0, events.get(1).slot());
        };
        double bytes = AllocationProbe.bytesPerIteration(4000, 1000, hit);
        System.out.println("Two-enemy combat bytes/frame: " + bytes);
        // EA-off baseline 1992; achieved 456, with 25% headroom.
        assertTrue(bytes <= 570, "Combat bytes/frame: " + bytes);
    }
}
