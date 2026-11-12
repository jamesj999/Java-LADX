package linksawakening.world.entity;

import java.util.AbstractList;
import java.util.Objects;
import java.util.RandomAccess;

/** Read-only slot projection; loaded filtering preserves ascending snapshot order without copying. */
final class EntityLiveSlotList extends AbstractList<RoomEntity> implements RandomAccess {
    private final EntityRoomInspectionServices runtime;
    private final boolean loadedOnly;

    EntityLiveSlotList(EntityRoomInspectionServices runtime, boolean loadedOnly) {
        this.runtime = runtime;
        this.loadedOnly = loadedOnly;
    }

    @Override
    public int size() {
        if (!loadedOnly) return runtime.slots().length;
        int count = 0;
        for (RoomEntity entity : runtime.slots()) if (entity.loaded()) count++;
        return count;
    }

    @Override
    public RoomEntity get(int index) {
        Objects.checkIndex(index, size());
        if (!loadedOnly) return EntityRoomQueries.presentedEntity(runtime, index);
        for (int slot = 0; slot < runtime.slots().length; slot++) {
            if (runtime.slots()[slot].loaded() && index-- == 0) return EntityRoomQueries.presentedEntity(runtime, slot);
        }
        throw new IndexOutOfBoundsException();
    }
}
