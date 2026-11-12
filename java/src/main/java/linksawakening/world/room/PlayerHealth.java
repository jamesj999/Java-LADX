package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public record PlayerHealth(int maxHearts, int health, boolean powerUp, boolean lowHealth) {}
