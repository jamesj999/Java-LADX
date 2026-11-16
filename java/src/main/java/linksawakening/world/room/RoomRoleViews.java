package linksawakening.world.room;

record RoomRoleViews(
        RoomEntities entities,
        RoomEntityState entityState,
        RoomRooms rooms,
        RoomDungeon dungeon,
        RoomProgress progress,
        RoomTerrain terrain) {
    static RoomRoleViews create(RoomSessionComponents components, RoomSession session, LoadedRoomHandle loadedRooms) {
        return new RoomRoleViews(
                new RoomEntitiesView(new RoomEntitiesView.Dependencies(session)),
                new RoomEntityStateView(new RoomEntityStateView.Dependencies(
                        components.entityBridge(),
                        components.entityInputs(),
                        components.followers(),
                        components.roomSessionViews(),
                        session)),
                components.roomLoadCoordinator(),
                components.roomState(),
                new RoomProgression(
                        new RoomProgression.Dependencies(
                                components.entityFrame(),
                                components.location(),
                                components.terrain(),
                                components.roomLoadCoordinator()),
                        loadedRooms),
                components.roomObjectMutations());
    }
}
