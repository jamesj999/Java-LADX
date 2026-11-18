package linksawakening.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

class EmptyEquipmentTest {
    @Test
    void unknownSlotsHaveNoActionAndLegacyLookupKeepsItsContract() {
        var registry = new ItemRegistry();
        var empty = registry.item(0xFF);
        assertSame(EquipmentItem.none(), empty);
        assertFalse(empty.blocksMotion());
        assertFalse(empty.locksFacing());
        assertEquals(-1, empty.overrideAnimationState(0, 1));
        assertFalse(empty.enemyCollisionBox(20, 30, 0).active());
        empty.onPress();
        empty.tick(true, 99);
        empty.onRelease();
        assertSame(empty, registry.activeSwordOrEmpty(new PlayerState()));
        assertNull(registry.lookup(0xFF));
    }

    @Test
    void removingAnItemRestoresTheEmptySlotWithoutTickingTheOldHandler() {
        var registry = new ItemRegistry();
        int[] ticks = {0};
        registry.register(1, new EquipmentItem() {
            @Override
            public void tick(boolean held) {
                ticks[0]++;
            }
        });
        registry.tickAll(false, false, 0, 0);
        registry.register(1, null); // Legacy unregistration boundary.
        registry.tickAll(false, false, 0, 0);
        assertEquals(1, ticks[0]);
        assertSame(EquipmentItem.none(), registry.item(1));
    }
}
