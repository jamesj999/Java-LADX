package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FloatingItemMotionTest {

    @Test
    void derivesAllFloatingItemPositionVariantsFromTheRomInitHandlers() {
        assertEquals(0, FloatingItemMotion.initialVariant(0x86, 0x08, 0x10));
        assertEquals(1, FloatingItemMotion.initialVariant(0x86, 0x18, 0x10));
        assertEquals(2, FloatingItemMotion.initialVariant(0x86, 0x08, 0x20));
        assertEquals(3, FloatingItemMotion.initialVariant(0x86, 0x18, 0x20));
        assertEquals(4, FloatingItemMotion.initialVariant(0xE5, 0x08, 0x10));
        assertEquals(5, FloatingItemMotion.initialVariant(0xE5, 0x18, 0x10));
        assertEquals(0x13, FloatingItemMotion.initialZ(0x86));
        assertEquals(0x13, FloatingItemMotion.initialZ(0xE5));
    }

    @Test
    void usesTheExactTopDownAndSideScrollingZTables() {
        int[] topDown = {0x0F, 0x0F, 0x10, 0x11, 0x11, 0x11, 0x10, 0x0F};
        int[] sideScrolling = {0x00, 0x00, 0x01, 0x02, 0x02, 0x02, 0x01, 0x00};
        for (int index = 0; index < 8; index++) {
            assertEquals(topDown[index], FloatingItemMotion.zForFrame(false, index << 3));
            assertEquals(sideScrolling[index], FloatingItemMotion.zForFrame(true, index << 3));
        }
    }

    @Test
    void mapsEachSourceVariantToItsRomPickupEffectAndItemType() {
        assertEquals(FloatingItemMotion.PickupEffect.TEN_RUPEES,
            FloatingItemMotion.pickupEffect(0x86, 0));
        assertEquals(FloatingItemMotion.PickupEffect.MAGIC_POWDER,
            FloatingItemMotion.pickupEffect(0x86, 1));
        assertEquals(FloatingItemMotion.PickupEffect.TEN_BOMBS,
            FloatingItemMotion.pickupEffect(0x86, 2));
        assertEquals(FloatingItemMotion.PickupEffect.TEN_RUPEES,
            FloatingItemMotion.pickupEffect(0x86, 3));
        assertEquals(FloatingItemMotion.PickupEffect.HEALTH_18,
            FloatingItemMotion.pickupEffect(0xE5, 4));
        assertEquals(FloatingItemMotion.PickupEffect.TEN_ARROWS,
            FloatingItemMotion.pickupEffect(0xE5, 5));

        assertEquals(0x2E, FloatingItemMotion.pickupEntityType(0x86, 0));
        assertEquals(0x3B, FloatingItemMotion.pickupEntityType(0x86, 1));
        assertEquals(0x38, FloatingItemMotion.pickupEntityType(0x86, 2));
        assertEquals(0x2E, FloatingItemMotion.pickupEntityType(0x86, 3));
        assertEquals(0x2D, FloatingItemMotion.pickupEntityType(0xE5, 4));
        assertEquals(0x37, FloatingItemMotion.pickupEntityType(0xE5, 5));
    }

    @Test
    void appliesTheRomTopDownLinkZGateButAllowsSideScrolling() {
        assertFalse(FloatingItemMotion.linkZAllowsCollection(false, 0x0B));
        assertTrue(FloatingItemMotion.linkZAllowsCollection(false, 0x0C));
        assertTrue(FloatingItemMotion.linkZAllowsCollection(false, 0xFF));
        assertTrue(FloatingItemMotion.linkZAllowsCollection(true, 0x00));
    }
}
