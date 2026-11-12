package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

public record PlayerHealth(int maxHearts, int health, boolean powerUp, boolean lowHealth) {}
