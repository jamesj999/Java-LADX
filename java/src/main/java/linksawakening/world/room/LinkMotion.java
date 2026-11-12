package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public record LinkMotion(boolean airborne, int state, boolean shield, int invincibility, int speedX, int speedY) {}
