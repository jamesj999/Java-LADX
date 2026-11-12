package linksawakening.app;

import linksawakening.entity.Link;
import linksawakening.equipment.Bomb;
import linksawakening.state.Direction;
import linksawakening.world.room.RoomPlayerItems;

/** Room-owned placement with Link's successful-placement animation writes. */
final class RoomBombPlacement implements Bomb.PlacementTarget {
    private final Link link;
    private final RoomPlayerItems items;
    private boolean bump;

    RoomBombPlacement(Link link, RoomPlayerItems items) {
        this.link = link;
        this.items = items;
    }

    public boolean placeBomb() {
        if (link == null || items == null) {
            bump = false;
            return false;
        }
        boolean placed = items.placeBomb(
                link.romEntityX(),
                link.romEntityY(),
                link.romEntityZ(),
                Direction.fromJava(link.direction()).romIndex());
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
