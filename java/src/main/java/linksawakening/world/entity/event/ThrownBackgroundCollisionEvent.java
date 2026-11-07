package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** Collision payload consumed by EntityCheckThrowAtTriggers in bank $03. */
public record ThrownBackgroundCollisionEvent(int slot, int entityType,
                                              int direction, int objectId) implements EntityRequest {
}
