package linksawakening.world.entity.event;

/** Collision payload consumed by EntityCheckThrowAtTriggers in bank $03. */
public record ThrownBackgroundCollisionEvent(int slot, int entityType,
                                              int direction, int objectId) implements EntityRequest {
}
