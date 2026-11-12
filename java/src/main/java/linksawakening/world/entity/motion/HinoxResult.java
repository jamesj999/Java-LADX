package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;

/** Inputs for HinoxMotion.update. */
public record HinoxResult(
        RoomEntity entity,
        int x,
        int y,
        int transition,
        int spriteVariant,
        boolean blocked,
        int heldX,
        int heldY,
        int heldZ,
        int linkSpeedX,
        int linkSpeedY,
        int linkVelocityZ,
        int airborne,
        int linkDamage,
        int jingle,
        int wave,
        boolean dust,
        boolean heldLinkPositionWritten,
        boolean applyHeldLinkPose,
        HinoxMotion.BombSpawn bomb) {}
