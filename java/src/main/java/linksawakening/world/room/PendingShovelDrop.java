package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

record PendingShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {}
