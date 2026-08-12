package linksawakening.startup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TarinShieldMotionTest {
    @Test
    void blocksTheDoorAndOpensTheShieldOfferWhenLinkTalks() {
        TarinShieldMotion motion = new TarinShieldMotion();

        TarinShieldMotion.Update door = motion.tick(false, 0x7B, false, 0);
        assertEquals(0x79, door.linkY());
        assertEquals(0x00, door.dialogLowId());
        assertTrue(door.linkMotionBlocked());

        TarinShieldMotion.Update offer = motion.tick(false, 0x60, true, 0);
        assertEquals(0x54, offer.dialogLowId());
        assertTrue(offer.linkMotionBlocked());
        assertEquals(TarinShieldMotion.State.OFFER, motion.state());
    }

    @Test
    void grantsShieldOnlyAfterOfferDialogAndRaisesItDuringTheItemState() {
        TarinShieldMotion motion = new TarinShieldMotion();
        motion.tick(false, 0x60, true, 0);
        motion.tick(true, 0x60, false, 0);
        TarinShieldMotion.Update itemStart = motion.tick(false, 0x60, false, 0);
        assertTrue(itemStart.playObtainItemMusic());
        assertTrue(!itemStart.presentShield());
        TarinShieldMotion.Update held = motion.tick(false, 0x60, false, 0);
        assertTrue(!held.playObtainItemMusic());
        assertTrue(held.presentShield());

        TarinShieldMotion.Update update = null;
        for (int frame = 1; frame < 0x80; frame++) {
            update = motion.tick(false, 0x60, false, 0);
        }

        assertTrue(update.grantShield());
        assertTrue(!update.presentShield());
        assertEquals(0x91, update.dialogLowId());
        assertEquals(TarinShieldMotion.State.COMPLETE, motion.state());
    }
}
