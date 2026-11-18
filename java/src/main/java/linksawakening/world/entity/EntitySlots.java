package linksawakening.world.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Owns the fixed ROM slot table and its combat/physics columns, which share slot identity. */
final class EntitySlots {
    private final EntityCombatTables combat = new EntityCombatTables();
    private final EntityPhysicsTables physics = new EntityPhysicsTables();
    private final RoomEntity[] values;
    private final List<RoomEntity> view;
    private RoomEntity[] roamingShadowOam = new RoomEntity[RoomEntity.SLOT_COUNT];
    private RoomEntity[] roamingDisplayOam = new RoomEntity[RoomEntity.SLOT_COUNT];

    void transferRoamingOam() {
        RoomEntity[] previous = roamingDisplayOam;
        roamingDisplayOam = roamingShadowOam;
        roamingShadowOam = previous;
        Arrays.fill(roamingShadowOam, null);
    }

    void captureRoamingOam(RoomEntity entity) {
        if (entity.status() == EntityStatus.ACTIVE
                && (entity.type() == EntityRomConstants.ENTITY_OCTOROK
                        || entity.type() == EntityRomConstants.ENTITY_MOBLIN)) {
            roamingShadowOam[entity.slot()] = entity;
        }
    }

    RoomEntity roamingDisplayPose(int slot) {
        return roamingDisplayOam[slot];
    }

    private void clearRoamingOam(int slot) {
        roamingDisplayOam[slot] = null;
        roamingShadowOam[slot] = null;
    }

    EntitySlots(List<RoomEntity> initial) {
        values = initial.toArray(RoomEntity[]::new);
        view = Collections.unmodifiableList(Arrays.asList(values));
    }

    EntityCombatTables combat() {
        return combat;
    }

    EntityPhysicsTables physics() {
        return physics;
    }

    int size() {
        return values.length;
    }

    RoomEntity get(int slot) {
        return values[slot];
    }

    void replace(int slot, RoomEntity entity) {
        Objects.requireNonNull(entity);
        if (values[slot].type() != entity.type() || !entity.loaded()) clearRoamingOam(slot);
        values[slot] = entity;
    }

    void clear(int slot) {
        clearRoamingOam(slot);
        values[slot] = RoomEntity.disabled(slot);
    }

    List<RoomEntity> view() {
        return view;
    }

    /** Descending SpawnNewEntity search also resets the hitbox flag on reuse. */
    int findFree(int highestSlot) {
        for (int slot = Math.min(highestSlot, values.length - 1); slot >= 0; slot--) {
            if (!values[slot].loaded()) {
                combat.writeEnemyHitboxFlags(slot, 0);
                return slot;
            }
        }
        return -1;
    }
}
