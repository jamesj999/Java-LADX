package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

public record ProgressInputs(
        int heartPieces,
        boolean birdKey,
        boolean tailKey,
        int tradeItem,
        boolean hasToadstool,
        int powder,
        int leaves) {}
