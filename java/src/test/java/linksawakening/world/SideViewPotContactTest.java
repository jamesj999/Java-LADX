package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SideViewPotContactTest {
    @Test
    void collisionHitboxUsesExactExclusiveBoundaries() {
        RoomEntity pot = pot(0x50, 0x60);

        assertTrue(SideViewPotContact.overlaps(pot, 0x50 + 0x0B, 0x60));
        assertFalse(SideViewPotContact.overlaps(pot, 0x50 + 0x0C, 0x60));
        assertTrue(SideViewPotContact.overlaps(pot, 0x50, 0x50));
        assertFalse(SideViewPotContact.overlaps(pot, 0x50, 0x6A));
    }

    @Test
    void groundedContactPushesRightAtZeroAndLeftForNegativeRawX() {
        RoomEntity pot = pot(0x50, 0x60);

        SideViewPotContact.Result right = SideViewPotContact.resolve(
            pot, 0x50, 0x60, false, 0);
        SideViewPotContact.Result left = SideViewPotContact.resolve(
            pot, 0x4F, 0x60, false, 0);

        assertEquals(0x10, right.speedX());
        assertEquals(0xF0, left.speedX());
        assertEquals(0x02, right.ignoreCollisionCountdown());
        assertTrue(right.resetPegasusBoots());
    }

    @Test
    void signedYWindowIncludesMinusThreeThroughPlusTwoOnly() {
        RoomEntity pot = pot(0x50, 0x60);

        assertTrue(SideViewPotContact.resolve(pot, 0x50, 0x5D, false, 0).resetPegasusBoots());
        assertTrue(SideViewPotContact.resolve(pot, 0x50, 0x62, false, 0).resetPegasusBoots());
        assertFalse(SideViewPotContact.resolve(pot, 0x50, 0x5C, false, 0).resetPegasusBoots());
        assertFalse(SideViewPotContact.resolve(pot, 0x50, 0x63, false, 0).resetPegasusBoots());
    }

    @Test
    void airborneContactRestoresOnlyFinalXAndDoesNotPush() {
        SideViewPotContact.Result result = SideViewPotContact.resolve(
            pot(0x50, 0x60), 0x50, 0x60, true, 0);

        assertTrue(result.restoreFinalPositionX());
        assertTrue(result.resetPegasusBoots());
        assertEquals(0, result.speedX());
    }

    @Test
    void topSnapWritesPotMinusTenAndStandingSpeed() {
        SideViewPotContact.Result result = SideViewPotContact.resolve(
            pot(0x50, 0x60), 0x50, 0x57, false, 0);

        assertTrue(result.snapTop());
        assertEquals(0x50, result.positionY());
        assertEquals(0x02, result.speedY());
    }

    @Test
    void topSnapIsSuppressedWhileLinkMovesUp() {
        SideViewPotContact.Result result = SideViewPotContact.resolve(
            pot(0x50, 0x60), 0x50, 0x57, false, 0x80);

        assertFalse(result.snapTop());
    }

    @Test
    void liftWindowIsIndependentAndUsesRawMinusTwelveThroughPlusSeventeen() {
        RoomEntity pot = pot(0x50, 0x60);

        assertTrue(SideViewPotContact.liftWindow(pot, 0x3E, 0x60));
        assertFalse(SideViewPotContact.liftWindow(pot, 0x3D, 0x60));
        assertTrue(SideViewPotContact.liftWindow(pot, 0x61, 0x60));
        assertFalse(SideViewPotContact.liftWindow(pot, 0x62, 0x60));
    }

    private static RoomEntity pot(int x, int y) {
        EntitySpriteDefinition definition = new EntitySpriteDefinition(
            0xD6, 0x03, 0x5B65, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x14, 0x02),
                new EntitySpriteDefinition.OamAttribute(0x14, 0x22))));
        return new RoomEntity(0, 0, 0xD6, x, y,
            EntityStatus.ACTIVE, definition, 0);
    }
}
