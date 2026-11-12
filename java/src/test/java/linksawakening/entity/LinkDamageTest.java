package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

final class LinkDamageTest {
    private Link link(PlayerState player) {
        Link link = new LinkTestBuilder()
                .input(null)
                .config(null)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(player)
                .items(null)
                .build();
        link.position().setPixelPosition(64, 64);
        return link;
    }

    @Test
    void recoilConsumesPendingSignedSpeedAndPreservesFractionalPosition() {
        Link link = link(null);
        link.position().applyRomSpeed(0xFF, 0x08);
        link.status().setCollisionIgnoreFrames(2);
        link.linkDamage.tickRecoil();
        assertEquals(63, link.position().pixelX());
        assertEquals(64, link.position().pixelY());
        assertEquals(0xFF, link.position().romSpeedX());
        assertFalse(link.linkMotion.position().forcedSpeedPending());
        assertTrue(link.linkDamage.hasRecoil());
        link.linkDamage.tickRecoil();
        assertEquals(65, link.position().pixelY());
        assertFalse(link.linkDamage.hasRecoil());
    }

    @Test
    void roomBoundaryCancelsRecoilAndRestoresBothAxes() {
        Link link = link(null);
        link.position().setPixelPosition(80, 115);
        link.position().applyRomSpeed(8, 32);
        link.status().setCollisionIgnoreFrames(2);
        link.linkDamage.tickRecoil();
        assertEquals(80, link.position().pixelX());
        assertEquals(115, link.position().pixelY());
        assertEquals(0, link.position().romSpeedX());
        assertEquals(0, link.position().romSpeedY());
        assertFalse(link.linkDamage.hasRecoil());
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
