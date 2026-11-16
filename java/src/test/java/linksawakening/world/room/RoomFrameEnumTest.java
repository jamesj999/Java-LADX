package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;
import linksawakening.state.OcarinaSong;
import linksawakening.state.PowerUp;
import linksawakening.state.Tunic;
import org.junit.jupiter.api.Test;

class RoomFrameEnumTest {
    @Test
    void knownFacingCarriesItsEnumAndExactEngineIndex() {
        var p = new LinkPosition(10, 20, 3, Direction.RIGHT.javaIndex(), 4);
        assertEquals(Direction.RIGHT, p.direction());
        assertEquals(3, p.rawDirection());
        assertEquals(10, p.x());
        assertEquals(4, p.collisionType());
    }

    @Test
    void unknownLegacyFacingBytesAreRetainedWithoutInventingAFacing() {
        var p = new LinkPosition(1, 2, 3, 0xFF, 4);
        assertEquals(Direction.UNKNOWN, p.direction());
        assertEquals(0xFF, p.rawDirection());
        var pickup = new EntityPickupInputs.Link(1, 2, false, true, 99, 3);
        assertEquals(99, pickup.rawDirection());
        assertEquals(Direction.UNKNOWN, pickup.direction());
    }

    @Test
    void motionStateEnumsDoNotNarrowRawRomGates() {
        var known = new LinkMotion(false, LinkMotionState.FALLING_DOWN.romValue(), false, 0, 1, 2);
        assertEquals(6, known.rawState());
        assertEquals(LinkMotionState.FALLING_DOWN, known.state());
        var unknown = new LinkMotion(false, 0xFF, false, 0, 1, 2);
        assertNull(unknown.state());
        assertEquals(0xFF, unknown.rawState());
    }

    @Test
    void inventoryClassificationKeepsUnknownSavedBytes() {
        var i = new InventoryInputs(0xF0, 0xFE, false, false);
        assertEquals(InventoryItem.UNKNOWN, i.itemAType());
        assertEquals(InventoryItem.UNKNOWN, i.itemBType());
        assertEquals(0xF0, i.itemA());
        assertEquals(0xFE, i.itemB());
    }

    @Test
    void typedPlayerValuesPreserveTheExistingFlagOnlyInputs() {
        var i = new InventoryInputs(InventoryItem.SWORD, InventoryItem.SHIELD, Tunic.RED, PowerUp.PIECE_OF_POWER);
        assertTrue(i.redTunic());
        assertTrue(i.pieceOfPower());
        assertEquals(InventoryItem.SWORD, i.itemAType());
        assertTrue(new PlayerHealth(3, 24, PowerUp.GUARDIAN_ACORN, false).powerUp());
        assertFalse(new PlayerHealth(3, 24, PowerUp.NONE, false).powerUp());
    }

    @Test
    void ocarinaUnknownSelectionsStayRawAndCannotBecomeManbo() {
        var s = new RoomItemUseState();
        s.selectedSongIndex = 1;
        assertEquals(OcarinaSong.MANBO_MAMBO, s.selectedSong());
        s.selectedSongIndex = 0xFF;
        assertNull(s.selectedSong());
        assertEquals(0xFF, s.selectedSongIndex);
    }
}
