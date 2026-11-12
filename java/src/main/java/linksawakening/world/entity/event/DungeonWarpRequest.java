package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

/** A cleared entity-$61 handler request for the ROM dungeon warp table. */
public record DungeonWarpRequest(int sourceSlot, linksawakening.world.entity.event.WarpDestination destination)
        implements EntityRequest {
    public DungeonWarpRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT || destination == null) {
            throw new IllegalArgumentException("Invalid dungeon warp request");
        }
    }
}
