package linksawakening.world.room;

record RoomRoleViews(
        RoomEntities entities,
        RoomEntityState entityState,
        RoomRooms rooms,
        RoomDungeon dungeon,
        RoomProgress progress,
        RoomTerrain terrain) {
    static RoomRoleViews create(RoomSessionComponents components, RoomSession session) {
        return new RoomRoleViews(
                new RoomEntitiesView(new RoomEntitiesView.Dependencies(session)),
                new RoomEntityStateView(new RoomEntityStateView.Dependencies(
                        components.entityBridge(),
                        components.entityInputs(),
                        components.followers(),
                        components.roomSessionViews(),
                        session)),
                new RoomRoomsView(new RoomRoomsView.Dependencies(components.roomLoadCoordinator()), session),
                new RoomDungeonView(
                        new RoomDungeonView.Dependencies(components.roomSessionViews(), components.roomState()),
                        session),
                new RoomProgressView(
                        new RoomProgressView.Dependencies(
                                components.entityFrame(), components.location(), components.terrain()),
                        session),
                components.roomObjectMutations());
    }
}
