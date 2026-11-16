package linksawakening.world.room;

import java.util.Objects;

/** Session-owned lifecycle cell; operations borrow a generation once and preserve their no-room policy. */
final class LoadedRoomHandle {
    // No generation exists before the first successful load.
    private RoomRuntime current;

    RoomRuntime currentOrNull() {
        return current;
    }

    void install(RoomRuntime room) {
        current = Objects.requireNonNull(room, "room");
    }

    boolean hasRoom() {
        return current != null;
    }
}
