package linksawakening.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AmmunitionTest {
    @Test
    void preservesOwnedBoundaryRules() {
        Inventory inventory = new Inventory();
        Ammunition state = new Ammunition(inventory);
        state.setMaxArrows(25);
        state.setArrowCount(99);
        assertEquals(25, state.arrowCount());
        state.setMaxArrows(3);
        assertEquals(3, state.arrowCount());
        state.setMaxMagicPowder(10);
        state.setMagicPowderCount(1);
        inventory.setItemA(InventoryItem.MAGIC_POWDER.romValue());
        assertTrue(state.consumeMagicPowder());
        assertEquals(0, inventory.itemA());
        assertFalse(state.consumeMagicPowder());
    }
}
