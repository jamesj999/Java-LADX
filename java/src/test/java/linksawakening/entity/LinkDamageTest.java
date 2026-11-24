package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.state.PlayerState;
import linksawakening.testing.TestLinks;
import org.junit.jupiter.api.Test;

final class LinkDamageTest {
    private Link link(PlayerState player) {
        Link link = TestLinks.link().player(player).build();
        link.position().setPixelPosition(64, 64);
        return link;
    }

    @Test
    void recoilConsumesPendingSignedSpeedAndPreservesFractionalPosition() {
        Link link = link(new PlayerState());
        link.position().applyRomSpeed(0xFF, 0x08);
        link.status().setCollisionIgnoreFrames(2);
        link.linkDamage.tickRecoil(0);
        assertEquals(63, link.position().pixelX());
        assertEquals(64, link.position().pixelY());
        assertEquals(0xFF, link.position().romSpeedX());
        assertFalse(link.linkMotion.position().forcedSpeedPending());
        assertTrue(link.linkDamage.hasRecoil());
        link.linkDamage.tickRecoil(0);
        assertEquals(65, link.position().pixelY());
        assertFalse(link.linkDamage.hasRecoil());
    }

    @Test
    void roomBoundaryCancelsRecoilAndRestoresBothAxes() {
        Link link = link(new PlayerState());
        link.position().setPixelPosition(80, 115);
        link.position().applyRomSpeed(8, 32);
        link.status().setCollisionIgnoreFrames(2);
        link.linkDamage.tickRecoil(0);
        assertEquals(80, link.position().pixelX());
        assertEquals(115, link.position().pixelY());
        assertEquals(0, link.position().romSpeedX());
        assertEquals(0, link.position().romSpeedY());
        assertFalse(link.linkDamage.hasRecoil());
    }

    @Test
    void lastRecoilStepAtTheNorthEdgeRequiresAnUpInputAndRetainsFractionsWhenRejected() {
        for (int mask : new int[] {0, 1, 4, 5}) {
            Link link = link(new PlayerState());
            link.position().setPixelPosition(89, -4);
            link.position().applyRomSpeed(0xF9, 0xEE);
            link.status().setCollisionIgnoreFrames(1);
            link.linkDamage.tickRecoil(mask);
            boolean upward = (mask & 4) != 0;
            assertEquals(upward ? 96 : 97, link.position().romEntityX());
            assertEquals(upward ? 10 : 12, link.position().romEntityY());
            assertEquals(9, link.linkMotion.position().subX() & 15);
            assertEquals(14, link.linkMotion.position().subY() & 15);
            assertEquals(upward ? 0xEE : 0, link.position().romSpeedY());
            assertFalse(link.linkDamage.hasRecoil());
        }
    }

    @Test
    void invincibilityTicksIndependentlyOfRecoil() {
        PlayerState player = new PlayerState();
        Link link = link(player);
        player.vitals().setInvincibilityCounter(2);
        link.linkDamage.tickInvincibility();
        assertEquals(1, player.vitals().invincibilityCounter());
        assertFalse(link.linkDamage.hasRecoil());
    }
}
