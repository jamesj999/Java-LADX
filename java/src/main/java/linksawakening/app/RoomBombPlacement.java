package linksawakening.app;

import java.util.Objects;
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
        this.link = Objects.requireNonNull(link);
        this.items = Objects.requireNonNull(items);
    }

    public boolean placeBomb() {
        boolean placed = items.placeBomb(
                link.position().romEntityX(),
                link.position().romEntityY(),
                link.animation().romEntityZ(),
                Direction.fromJava(link.animation().direction()).romIndex());
        bump = placed && items.lastBombPlacementPlayedBump();
        if (placed) link.equipment().startRomItemAttackStep();
        return placed;
    }

    public boolean playBumpForLastPlacement() {
        return bump;
    }

    public boolean bombActive() {
        return items.bombActive();
    }
}
