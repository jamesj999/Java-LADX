package linksawakening.world.room;

import java.util.List;
import java.util.Objects;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Owns room publication and exposes independently wired role contracts and the entity frame sequence. */
public final class RoomSession {
    private final LoadedRoomHandle loadedRooms = new LoadedRoomHandle();
    private final RoomRoleViews roles;
    private final RoomDoors doors;
    private final RoomTransitions transitions;
    private final RoomPlayerItems playerItems;
    private final RoomStory story;
    private final RoomFrameInputApplier frameInputs;
    private final EntityFrameResults frameResults;
    private final RoomDungeonLiveView dungeonView;
    private final RoomLoadListener roomLoadListener;

    public RoomSession(RoomSessionConfig config) {
        roomLoadListener = config.presentation().roomLoadListener() == null
                ? room -> {}
                : config.presentation().roomLoadListener();
        var wiring = RoomSessionContext.create(config, loadedRooms, this::installRoom, this::roomLoaded);
        roles = wiring.roles();
        doors = wiring.doors();
        transitions = wiring.transitions();
        playerItems = wiring.playerItems();
        story = wiring.story();
        frameInputs = wiring.frameInputs();
        frameResults = wiring.frameResults();
        dungeonView = wiring.dungeonView();
    }

    public boolean hasRoom() {
        return loadedRooms.hasRoom();
    }

    public RoomDoors doors() {
        return doors;
    }

    public RoomTransitions transitions() {
        return transitions;
    }

    public RoomPlayerItems playerItems() {
        return playerItems;
    }

    public RoomStory story() {
        return story;
    }

    public RoomEntities entities() {
        return roles.entities();
    }

    public RoomEntityState entityState() {
        return roles.entityState();
    }

    public RoomRooms rooms() {
        return roles.rooms();
    }

    public RoomDungeon dungeon() {
        return roles.dungeon();
    }

    public RoomProgress progress() {
        return roles.progress();
    }

    public RoomTerrain terrain() {
        return roles.terrain();
    }

    public EntityFrameResults entityFrameResults() {
        return frameResults;
    }

    public List<EntityProjectileEvent> tickEntities(EntityFrameValues inputs) {
        return frameInputs.tick(inputs);
    }

    public List<EntityProjectileEvent> tickEntities(EntityTickValues inputs) {
        return frameInputs.tick(inputs);
    }

    void installRoom(RoomRuntime room) {
        loadedRooms.install(room);
        dungeonView.setRoomMap(room.room().mapId());
    }

    void clearRoom() {
        loadedRooms.clear();
        dungeonView.setRoomMap(-1);
    }

    RoomRuntime roomLoaded(RoomRuntime room) {
        roomLoadListener.roomLoaded(room.room());
        // Notification may synchronously load another room; retain the original publication protocol.
        return Objects.requireNonNull(loadedRooms.currentOrNull());
    }
}
