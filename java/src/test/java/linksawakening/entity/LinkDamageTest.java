package linksawakening.entity;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LinkDamageTest {
    private Link link(PlayerState player) {
        Link link = new Link(null, null, null, null, null, player, null);
        link.setPixelPosition(64, 64);
        return link;
    }

    @Test void recoilConsumesPendingSignedSpeedAndPreservesFractionalPosition() {
        Link link = link(null);
        link.applyRomSpeed(0xFF, 0x08);
        link.setCollisionIgnoreFrames(2);
        link.linkDamage.tickRecoil();
        assertEquals(63, link.pixelX());
        assertEquals(64, link.pixelY());
        assertEquals(0xFF, link.romSpeedX());
        assertFalse(link.linkMotion.forcedSpeedPending);
        assertTrue(link.linkDamage.hasRecoil());
        link.linkDamage.tickRecoil();
        assertEquals(65, link.pixelY());
        assertFalse(link.linkDamage.hasRecoil());
    }

    @Test void roomBoundaryCancelsRecoilAndRestoresBothAxes() {
        Link link = link(null);
        link.setPixelPosition(80, 115);
        link.applyRomSpeed(8, 32);
        link.setCollisionIgnoreFrames(2);
        link.linkDamage.tickRecoil();
        assertEquals(80, link.pixelX());
        assertEquals(115, link.pixelY());
        assertEquals(0, link.romSpeedX());
        assertEquals(0, link.romSpeedY());
        assertFalse(link.linkDamage.hasRecoil());
    }

    @Test void invincibilityTicksIndependentlyOfRecoil() {
        PlayerState player = new PlayerState();
        Link link = link(player);
        player.setInvincibilityCounter(2);
        link.linkDamage.tickInvincibility();
        assertEquals(1, player.invincibilityCounter());
        assertFalse(link.linkDamage.hasRecoil());
    }
}
