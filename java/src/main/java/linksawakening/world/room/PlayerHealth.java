package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public record PlayerHealth(int maxHearts, int health, boolean powerUp, boolean lowHealth) {
    public PlayerHealth(int maxHearts, int health, linksawakening.state.PowerUp powerUp, boolean lowHealth) {
        this(maxHearts, health, powerUp != linksawakening.state.PowerUp.NONE, lowHealth);
    }
}
