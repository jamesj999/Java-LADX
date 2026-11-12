package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public record ButtonInputs(
        boolean aHeld, boolean bHeld, boolean joypadHeld, int pressedMask, boolean braceletHeld, boolean bombHeld) {}
