package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public record PlayerInputs(
        ProgressInputs progress, InventoryInputs inventory, PlayerLevels levels, PlayerHealth health) {}
