package linksawakening.world.room;

import java.util.Objects;

/** Mutable room data and uniquely room-scoped puzzle/override state, prepared on load. */
final class RoomContents {
    final ActiveRoom room;
    final RoomPuzzleState.PerRoom puzzles;
    final RoomTerrainState.Overrides overrides;

    RoomContents(ActiveRoom room, RoomPuzzleState.PerRoom puzzles, RoomTerrainState.Overrides overrides) {
        this.room = Objects.requireNonNull(room);
        this.puzzles = Objects.requireNonNull(puzzles);
        this.overrides = Objects.requireNonNull(overrides);
    }
}
