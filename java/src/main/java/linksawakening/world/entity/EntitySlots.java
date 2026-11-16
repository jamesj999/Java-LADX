package linksawakening.world.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Owns the ROM's fixed slot table; replacement never changes slot identity or visit order. */
final class EntitySlots {
    private final RoomEntity[] values;
    private final List<RoomEntity> view;

    EntitySlots(List<RoomEntity> initial) {
        values = initial.toArray(RoomEntity[]::new);
        view = Collections.unmodifiableList(Arrays.asList(values));
    }

    int size() {
        return values.length;
    }

    RoomEntity get(int slot) {
        return values[slot];
    }

    void replace(int slot, RoomEntity entity) {
        values[slot] = Objects.requireNonNull(entity);
    }

    void clear(int slot) {
        values[slot] = RoomEntity.disabled(slot);
    }

    List<RoomEntity> view() {
        return view;
    }

    /** Descending SpawnNewEntity search also resets the hitbox flag on reuse. */
    int findFree(EntityCombatTables combat, int highestSlot) {
        for (int slot = Math.min(highestSlot, values.length - 1); slot >= 0; slot--) {
            if (!values[slot].loaded()) {
                combat.writeEnemyHitboxFlags(slot, 0);
                return slot;
            }
        }
        return -1;
    }
}
