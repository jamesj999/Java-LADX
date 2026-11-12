package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkRoomBehaviorTest extends LinkFixture {

    @Test
    void activeRecoilCannotKnockLinkAcrossANormalRoomBoundary() throws Exception {
        Link link = linkWithEmptyRoom(new InputState());
        link.setPixelPosition(80, 115); // ROM y=$83, just inside the south edge.
        link.applyRomSpeed(0, 0x20);
        link.setCollisionIgnoreFrames(16);
        link.update();
        assertEquals(115, link.pixelY());
        assertEquals(0, link.romSpeedY());
        link.update();
        assertEquals(115, link.pixelY());
    }

    @Test
    void keepsRoomEntryCoordinatesInBothHostAndRomConventions() {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();

        link.setRoomEntryPixelPosition(0x30, 0x40);
        assertEquals(0x30, link.roomEntryPixelX());
        assertEquals(0x40, link.roomEntryPixelY());
        assertEquals(0x38, link.roomEntryRomPositionX());
        assertEquals(0x50, link.roomEntryRomPositionY());

        link.setRoomEntryRomPosition(0x52, 0x63);
        assertEquals(0x4A, link.roomEntryPixelX());
        assertEquals(0x53, link.roomEntryPixelY());
        assertEquals(0x52, link.roomEntryRomPositionX());
        assertEquals(0x63, link.roomEntryRomPositionY());
    }

    @Test
    void roomEntryClearsSideViewPlatformStateBeforeNextMotion() throws Exception {
        Link link = linkWithEmptyRoom(new InputState());
        link.setPixelPosition(0x40, 0x20);
        link.applySideViewPlatformContact(0, 0x50, 0x02, true);

        link.setRoomEntryPixelPosition(0x20, 0x30);
        assertFalse(link.standingOnSideViewEntity());

        link.update();
        assertEquals(0x30, link.pixelY());
        assertEquals(0, link.romSpeedY());
    }

    @Test
    void warpState3KeepsCollisionImmunitySeparateFromInvincibilityCounter() {
        PlayerState playerState = new PlayerState();
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(new ItemRegistry())
                .build();

        link.applyWarpState3(0x48, 0x40, 0x20);
        assertEquals(0, playerState.invincibilityCounter());
        assertTrue(link.isCollisionDamageImmune());

        link.applyWarpState3(0x48, 0x40, 0);
        assertEquals(0, playerState.invincibilityCounter());
        assertTrue(link.isCollisionDamageImmune());

        link.initializeRoomPhysicsForRoom(1L, 0x01, 0x36, 1);
        assertFalse(link.isCollisionDamageImmune());
        link.applyWarpState3(0x48, 0x40, 0x20);
        link.resetTransientStateForDebug();
        assertFalse(link.isCollisionDamageImmune());
    }
}
