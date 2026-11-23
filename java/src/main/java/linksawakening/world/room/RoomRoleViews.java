package linksawakening.world.room;

/** Immutable wiring output, containing only the public role contracts. */
record RoomRoleViews(
        RoomEntities entities,
        RoomEntityState entityState,
        RoomRooms rooms,
        RoomDungeon dungeon,
        RoomProgress progress,
        RoomTerrain terrain) {}
