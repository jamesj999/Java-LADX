package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntitySlotsTest {
    @Test
    void slotViewsTrackReplacementWithoutExposingMutableStorage() {
        var initial = new ArrayList<>(List.of(RoomEntity.disabled(0), RoomEntity.disabled(1)));
        var slots = new EntitySlots(initial);
        var view = slots.view();
        var entity = TestEntities.active(0x09).slot(1).build();
        initial.set(1, entity);
        assertFalse(slots.get(1).loaded());
        slots.replace(1, entity);
        assertSame(entity, view.get(1));
        assertThrows(UnsupportedOperationException.class, () -> view.set(1, RoomEntity.disabled(1)));
        slots.clear(1);
        assertFalse(view.get(1).loaded());
        assertEquals(1, view.get(1).slot());
    }

    @Test
    void allocationSearchIsDescendingAndResetsOnlyTheSelectedHitbox() {
        var slots = new EntitySlots(List.of(RoomEntity.disabled(0), RoomEntity.disabled(1), RoomEntity.disabled(2)));
        var combat = slots.combat();
        combat.writeEnemyHitboxFlags(0, 7);
        combat.writeEnemyHitboxFlags(1, 8);
        combat.writeEnemyHitboxFlags(2, 9);
        assertEquals(2, slots.findFree(15));
        assertEquals(0, combat.readEnemyHitboxFlags(2));
        assertEquals(8, combat.readEnemyHitboxFlags(1));
        slots.replace(2, TestEntities.active(0x09).slot(2).build());
        assertEquals(1, slots.findFree(2));
        assertEquals(0, combat.readEnemyHitboxFlags(1));
        assertEquals(7, combat.readEnemyHitboxFlags(0));
        assertEquals(-1, slots.findFree(-1));
    }
}
