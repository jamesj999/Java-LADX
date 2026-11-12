package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public record UiInputs(
        boolean dialogActive,
        boolean inventoryAppearing,
        int dialogCooldown,
        int windowY,
        int selectionIndex,
        boolean musicActive) {}
