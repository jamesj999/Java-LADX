package linksawakening.world.room;

record RoomRoleViews(
        RoomEntities entities,
        RoomEntityState entityState,
        RoomRooms rooms,
        RoomDungeon dungeon,
        RoomProgress progress,
        RoomTerrain terrain) {
    static RoomRoleViews create(RoomSessionComponents components) {
        return new RoomRoleViews(
                new RoomEntitiesView(new RoomEntitiesView.Dependencies(components.entityBridge())),
                new RoomEntityStateView(new RoomEntityStateView.Dependencies(
                        components.entityBridge(),
                        components.entityFrame(),
                        components.entityInputs(),
                        components.followers(),
                        components.roomSessionViews())),
                new RoomRoomsView(new RoomRoomsView.Dependencies(
                        components.roomLoadCoordinator(), components.roomSessionViews())),
                new RoomDungeonView(
                        new RoomDungeonView.Dependencies(components.roomSessionViews(), components.roomState())),
                new RoomProgressView(new RoomProgressView.Dependencies(
                        components.entityFrame(),
                        components.location(),
                        components.roomLoadCoordinator(),
                        components.terrain())),
                new RoomTerrainView(new RoomTerrainView.Dependencies(components.roomObjectMutations())));
    }
}
