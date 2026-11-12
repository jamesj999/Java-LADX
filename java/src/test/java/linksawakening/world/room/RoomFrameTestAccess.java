package linksawakening.world.room;

import linksawakening.world.entity.event.HeartContainerRewardEvent;

/** Test-only event injection at the room-owned outbox, without entity internals. */
public final class RoomFrameTestAccess {
    private RoomFrameTestAccess() {}

    public static linksawakening.world.entity.RoomEntityRuntime runtime(RoomSession room) {
        return room.components.entityBridge().entityRuntime;
    }

    public static void music(RoomSession room, int track) {
        room.components.entityBridge().outbox.pendingRoomMusicTrack = track;
    }

    public static void heart(RoomSession room) {
        room.components.entityBridge().outbox.pendingHeartContainerRewards.add(new HeartContainerRewardEvent(0));
    }
}
