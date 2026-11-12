package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

public record PlayerInputs(
        ProgressInputs progress, InventoryInputs inventory, PlayerLevels levels, PlayerHealth health) {}
