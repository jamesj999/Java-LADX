package linksawakening.app;

import java.util.function.IntUnaryOperator;
import linksawakening.entity.Link;
import linksawakening.equipment.Bomb;
import linksawakening.world.room.RoomPlayerItems;

/** Room-owned placement with Link's successful-placement animation writes. */
final class RoomBombPlacement implements Bomb.PlacementTarget {
    private final Link link;
    private final RoomPlayerItems items;
    private final IntUnaryOperator direction;
    private boolean bump;

    RoomBombPlacement(Link link, RoomPlayerItems items, IntUnaryOperator direction) {
        this.link = link;
        this.items = items;
        this.direction = direction;
    }

    public boolean placeBomb() {
        if (link == null || items == null) {
            bump = false;
            return false;
        }
        boolean placed = items.placeBomb(
                link.romEntityX(), link.romEntityY(), link.romEntityZ(), direction.applyAsInt(link.direction()));
        bump = placed && items.lastBombPlacementPlayedBump();
        if (placed) link.startRomItemAttackStep();
        return placed;
    }

    public boolean playBumpForLastPlacement() {
        return bump;
    }

    public boolean bombActive() {
        return items != null && items.bombActive();
    }
}
