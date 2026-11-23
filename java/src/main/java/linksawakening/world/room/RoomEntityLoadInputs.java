package linksawakening.world.room;

import linksawakening.world.entity.EntityRoomLoader;

record RoomEntityLoadInputs(
        EntityRoomLoader.RoomTable table,
        int roomId,
        int clearedEntitiesMask,
        int mapId,
        byte[] roomStatus,
        boolean hasBirdKey,
        boolean sideScrolling,
        RoomNpcPaletteInputs palettes) {}
