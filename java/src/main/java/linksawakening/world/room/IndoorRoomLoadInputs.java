package linksawakening.world.room;

record IndoorRoomLoadInputs(
        int mapId,
        int roomId,
        int[][] fallbackPalettes,
        int mapCategory,
        int clearedEntitiesMask,
        byte[] indoorRoomStatus,
        boolean hasBirdKey,
        RoomNpcPaletteInputs palettes) {}
