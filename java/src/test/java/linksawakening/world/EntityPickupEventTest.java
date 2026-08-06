package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class EntityPickupEventTest {

    @Test
    void sourceVariantIsReservedForFloatingItemsAndThreeArgConstructorStaysOrdinary() {
        EntityPickupEvent ordinary = new EntityPickupEvent(0, 0x2D, 0);

        assertEquals(-1, ordinary.sourceVariant());
        assertThrows(IllegalArgumentException.class,
            () -> new EntityPickupEvent(0, 0x2D, 0, 0));
        assertEquals(0, new EntityPickupEvent(0, 0x86, 0, 0).sourceVariant());
        assertEquals(5, new EntityPickupEvent(0, 0xE5, 0, 5).sourceVariant());
    }
}
