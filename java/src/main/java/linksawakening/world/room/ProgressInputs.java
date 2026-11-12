package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public record ProgressInputs(
        int heartPieces,
        boolean birdKey,
        boolean tailKey,
        int tradeItem,
        boolean hasToadstool,
        int powder,
        int leaves) {}
