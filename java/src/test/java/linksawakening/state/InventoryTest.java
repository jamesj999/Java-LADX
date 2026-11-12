package linksawakening.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InventoryTest {
    @Test
    void preservesOwnedBoundaryRules() {
        Inventory state = new Inventory();
        state.setItemA(0x1FF);
        state.setSubscreenItems(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        state.swapItemAWithSubscreen(0);
        assertEquals(1, state.itemA());
        assertEquals(255, state.subscreenItem(0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> state.swapItemBWithSubscreen(10));
    }
}
