package linksawakening.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RomValuesTest {
    @Test
    void legacySelectionClampingIsPreserved() {
        PlayerState player = new PlayerState();
        for (int value : new int[] {Integer.MIN_VALUE, -1, 0, 1, 2, 3, 255, Integer.MAX_VALUE}) {
            int expected = Math.max(0, Math.min(2, value));
            player.equipmentLevels().setTunicType(value);
            player.equipmentLevels().setActivePowerUp(value);
            assertEquals(expected, player.equipmentLevels().tunicType());
            assertEquals(expected, player.equipmentLevels().activePowerUp());
            assertEquals(Tunic.fromRom(expected), player.equipmentLevels().tunic());
            assertEquals(PowerUp.fromRom(expected), player.equipmentLevels().powerUp());
        }
    }

    @Test
    void directionUsesExplicitOppositeOrderings() {
        int[] rom = {3, 2, 1, 0};
        for (int i = 0; i < 4; i++) {
            assertEquals(rom[i], Direction.fromJava(i).romIndex());
            assertEquals(i, Direction.fromRom(rom[i]).javaIndex());
        }
        assertThrows(IllegalArgumentException.class, () -> Direction.fromJava(4));
        assertThrows(AssertionError.class, () -> Direction.fromRom(-1));
        assertEquals(Direction.DOWN, Direction.fromJavaOrDown(-1));
        assertEquals(Direction.DOWN, Direction.fromJavaOrDown(256));
    }

    @Test
    void knownValuesRoundTrip() {
        for (InventoryItem value : InventoryItem.values()) assertEquals(value, InventoryItem.fromRom(value.romValue()));
        for (Tunic value : Tunic.values()) assertEquals(value, Tunic.fromRom(value.romValue()));
        for (PowerUp value : PowerUp.values()) assertEquals(value, PowerUp.fromRom(value.romValue()));
        for (OcarinaSong value : OcarinaSong.values()) assertEquals(value, OcarinaSong.fromRom(value.romValue()));
        for (LinkMotionState value : LinkMotionState.values())
            assertEquals(value, LinkMotionState.fromRom(value.romValue()));
        for (GroundStatus value : GroundStatus.values()) assertEquals(value, GroundStatus.fromRom(value.romValue()));
    }

    @Test
    void closedValuesRejectUnknownBytes() {
        assertThrows(IllegalArgumentException.class, () -> Tunic.fromRom(3));
        assertThrows(IllegalArgumentException.class, () -> PowerUp.fromRom(3));
        assertThrows(IllegalArgumentException.class, () -> OcarinaSong.fromRom(3));
        assertThrows(IllegalArgumentException.class, () -> LinkMotionState.fromRom(11));
        assertThrows(IllegalArgumentException.class, () -> GroundStatus.fromRom(2));
    }

    @Test
    void unknownInventoryBytesRemainAvailableToSaveCodec() {
        PlayerState player = new PlayerState();
        for (int raw = 14; raw <= 255; raw++) {
            player.inventory().setItemA(raw);
            assertEquals(raw, player.inventory().itemA());
            assertEquals(InventoryItem.UNKNOWN, InventoryItem.fromRom(raw));
        }
    }

    @Test
    void songFlagsAreIndependent() {
        for (int flags = 0; flags < 8; flags++) {
            assertEquals((flags & 1) != 0, OcarinaSong.FROGS_SONG_OF_THE_SOUL.isSet(flags));
            assertEquals((flags & 2) != 0, OcarinaSong.MANBO_MAMBO.isSet(flags));
            assertEquals((flags & 4) != 0, OcarinaSong.BALLAD_OF_THE_WIND_FISH.isSet(flags));
        }
    }
}
