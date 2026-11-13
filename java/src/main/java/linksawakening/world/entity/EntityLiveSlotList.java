package linksawakening.world.entity;

import java.util.AbstractList;
import java.util.Objects;
import java.util.RandomAccess;

/** Read-only slot projection; loaded filtering preserves ascending snapshot order without copying. */
final class EntityLiveSlotList extends AbstractList<RoomEntity> implements RandomAccess {
    private final EntityRoomInspection runtime;
    private final boolean loadedOnly;
    // Cache seeds never match a live slot; later hits require source identity.
    private final RoomEntity[] sources = new RoomEntity[RoomEntity.SLOT_COUNT];
    // Cached immutable presentation paired with each source identity.
    private final RoomEntity[] presentations = new RoomEntity[RoomEntity.SLOT_COUNT];

    EntityLiveSlotList(EntityRoomInspection runtime, boolean loadedOnly) {
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.loadedOnly = loadedOnly;
        java.util.Arrays.fill(sources, EntityRoomInspection.UNREAD_PRESENTATION);
        java.util.Arrays.fill(presentations, EntityRoomInspection.UNREAD_PRESENTATION);
    }

    @Override
    public int size() {
        if (!loadedOnly) return runtime.slots.length;
        int count = 0;
        for (RoomEntity entity : runtime.slots) if (entity.loaded()) count++;
        return count;
    }

    @Override
    public RoomEntity get(int index) {
        Objects.checkIndex(index, size());
        if (!loadedOnly) return presented(index);
        for (int slot = 0; slot < runtime.slots.length; slot++) {
            if (runtime.slots[slot].loaded() && index-- == 0) return presented(slot);
        }
        throw new IndexOutOfBoundsException();
    }

    private RoomEntity presented(int slot) {
        RoomEntity result = EntityRoomInspection.entityRoomQueriesPresentedEntity(
                runtime, slot, sources[slot], presentations[slot]);
        sources[slot] = runtime.slots[slot];
        presentations[slot] = result;
        return result;
    }
}
