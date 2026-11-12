package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public record InventoryInputs(int itemA, int itemB, boolean redTunic, boolean pieceOfPower) {
    public InventoryInputs(
            linksawakening.state.InventoryItem itemA,
            linksawakening.state.InventoryItem itemB,
            linksawakening.state.Tunic tunic,
            linksawakening.state.PowerUp powerUp) {
        this(
                itemA.romValue(),
                itemB.romValue(),
                tunic == linksawakening.state.Tunic.RED,
                powerUp == linksawakening.state.PowerUp.PIECE_OF_POWER);
    }

    public linksawakening.state.InventoryItem itemAType() {
        return linksawakening.state.InventoryItem.fromRom(itemA);
    }

    public linksawakening.state.InventoryItem itemBType() {
        return linksawakening.state.InventoryItem.fromRom(itemB);
    }
}
