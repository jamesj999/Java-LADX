package linksawakening.world.room;

import java.util.Objects;

/** Mutable room data and uniquely room-scoped puzzle/override state, prepared on load. */
final class RoomContents {
    private final ActiveRoom room;
    private final RoomPuzzleState.PerRoom puzzles;
    private final RoomTerrainState.Overrides overrides;

    RoomContents(ActiveRoom room, RoomPuzzleState.PerRoom puzzles, RoomTerrainState.Overrides overrides) {
        this.room = Objects.requireNonNull(room);
        this.puzzles = Objects.requireNonNull(puzzles);
        this.overrides = Objects.requireNonNull(overrides);
    }

    RoomPuzzleState.PerRoom puzzles() {
        return this.puzzles;
    }

    RoomTerrainState.Overrides overrides() {
        return this.overrides;
    }

    ActiveRoom room() {
        return this.room;
    }
}
