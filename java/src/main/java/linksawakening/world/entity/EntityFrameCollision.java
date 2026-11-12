package linksawakening.world.entity;

import static linksawakening.world.entity.EntityTypeRules.checksThrownRoomEventTriggers;

import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;

/** Stable adapters for a frame's current rich and boolean background probes. */
final class EntityFrameCollision {
    private final EntityFrameExecution state;
    private final RoomEntityBackgroundCollision booleanProbe = this::probe;
    private final RoomEntityBackgroundInteraction richProbe = this::probeBooleanInput;

    EntityFrameCollision(EntityFrameExecution state) {
        this.state = state;
    }

    RoomEntityBackgroundCollision booleanProbe() {
        return booleanProbe;
    }

    RoomEntityBackgroundInteraction richProbe() {
        return richProbe;
    }

    private boolean probe(RoomEntity entity, int direction, int nextX, int nextY) {
        EntityBackgroundCollisionResult result = state.physicsTables()
                .readBackgroundInteraction()
                .probe(
                        entity,
                        direction,
                        nextX,
                        nextY,
                        state.combatTables().readEnemyIgnoreHitsCountdown(entity.slot()),
                        state.inputs().frame());
        if (result.blocked()
                && entity.status() == EntityStatus.THROWN
                && checksThrownRoomEventTriggers(entity.type())) {
            state.events()
                    .pendingThrownBackgroundCollisions
                    .add(new ThrownBackgroundCollisionEvent(
                            entity.slot(), entity.type(), direction, result.objectId()));
        }
        return result.blocked();
    }

    private EntityBackgroundCollisionResult probeBooleanInput(RoomEntity entity, int direction, int nextX, int nextY) {
        return EntityBackgroundCollisionResult.fromBoolean(
                state.inputs().backgroundCollision().blocks(entity, direction, nextX, nextY), direction, nextX, nextY);
    }
}
