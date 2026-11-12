package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

public record LinkInputs(
        LinkPosition position,
        LinkMotion motion,
        int attackCountdown,
        int finalX,
        boolean freeMovement,
        int velocityZ) {}
