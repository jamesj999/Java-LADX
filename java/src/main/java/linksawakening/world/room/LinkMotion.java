package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

public record LinkMotion(boolean airborne, int state, boolean shield, int invincibility, int speedX, int speedY) {}
