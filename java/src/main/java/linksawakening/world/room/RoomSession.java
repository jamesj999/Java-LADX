package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Public room API delegating to room-scoped collaborators. */
public final class RoomSession {

    /** Applies the live frame inputs in source order before ticking slots. */
    public List<EntityProjectileEvent> tickEntities(EntityFrameInputs inputs) {
        return frameInputs.tick(inputs);
    }

    public EntityFrameResults entityFrameResults() {
        return frameResults;
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

    // The sole optional current generation; room-only code receives a non-null value.
    private RoomRuntime loadedRoom;

    RoomRuntime loadedRoomOrNull() {
        return loadedRoom;
    }

    void installRoom(RoomRuntime room) {
        java.util.Objects.requireNonNull(room, "room");
        // Transitional aliases retained only while collaborators migrate to explicit arguments.
        components.location().activeRoom = room.room;
        components.entityFrame().runtime = room.entities;
        loadedRoom = room;
    }

    public boolean hasRoom() {
        return loadedRoom != null;
    }

    final RoomSessionComponents components;
    private final RoomRoleViews roles;
    private final RoomFrameInputApplier frameInputs;
    private final EntityFrameResults frameResults;
    private final RoomDoors doors;
    private final RoomTransitions transitions;
    private final RoomPlayerItems playerItems;
    private final RoomStory story;

    static long nextRoomSessionId;

    public RoomSession(RoomSessionConfig config) {
        components = RoomSessionContext.create(config, this::installRoom);

        roles = RoomRoleViews.create(components, this);
        doors = new RoomDoorsView(components.roomDoorInteractions());
        transitions = new RoomTransitionsView(components.roomTransitionController());
        playerItems = new RoomPlayerItemsView(components.roomPlayerItemActions());
        story = new RoomStoryView(components.roomStoryState());
        frameInputs = new RoomFrameInputApplier(
                components.entityInputs(),
                components.entityBridge(),
                components.entityFrame(),
                components.roomSessionViews(),
                roles.entityState(),
                story,
                components.roomLoadCoordinator());
        frameResults = new EntityFrameResults(
                new LinkOutputs(components.linkOutputsDependencies()),
                new RewardOutputs(components.rewardOutputsDependencies()),
                new PresentationOutputs(components.presentationOutputsDependencies()));
    }

    public List<EntityProjectileEvent> tickEntities(EntityTickInputs inputs) {
        return components.entityBridge().tickEntities(inputs);
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

    boolean inBossBattle() {
        return loadedRoom != null && loadedRoom.entities.inspection().inBossBattle();
    }

    java.util.List<linksawakening.world.entity.event.EntityCombatEvent> resolveEntityCombat(EntityCombatInputs inputs) {
        java.util.Objects.requireNonNull(inputs.sword(), "sword");
        return loadedRoom == null
                ? java.util.List.of()
                : components.entityCombatBridge().resolveEntityCombat(loadedRoom, inputs);
    }

    boolean deferContactRecoil(int slot, int x, int y) {
        return loadedRoom != null && components.entityCombatBridge().deferContactRecoil(loadedRoom, slot, x, y);
    }

    int clearEntity(int slot) {
        return loadedRoom == null ? 0 : components.entityCombatBridge().clearEntity(loadedRoom, slot);
    }

    linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(EntityPickupInputs inputs) {
        java.util.Objects.requireNonNull(inputs.link(), "link");
        java.util.Objects.requireNonNull(inputs.sword(), "sword");
        return loadedRoom == null ? null : components.entityCombatBridge().collectEntityIfNeeded(loadedRoom, inputs);
    }

    linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive) {
        return collectEntityIfNeeded(
                frame,
                x,
                y,
                airborne,
                interactive,
                components.followers().followingLinkDirection,
                components.followers().followingLinkZ);
    }

    linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive, int direction) {
        return collectEntityIfNeeded(
                frame, x, y, airborne, interactive, direction, components.followers().followingLinkZ);
    }

    linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive, int direction, int z) {
        return collectEntityIfNeeded(new EntityPickupInputs(
                frame,
                new EntityPickupInputs.Link(x, y, airborne, interactive, direction, z),
                new SwordBox(false, 0, 0, 0, 0)));
    }
}
