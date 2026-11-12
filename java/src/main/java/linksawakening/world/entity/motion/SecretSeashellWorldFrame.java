package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectSample;

/** Inputs for SecretSeashellMotion.advance. */
record SecretSeashellWorldFrame(
        RoomEntity entity,
        int roomId,
        int frameCounter,
        boolean roomTransitionActive,
        boolean screenShakeActive,
        boolean pegasusCollisionActive,
        int pegasusCollisionX,
        int pegasusCollisionY,
        int linkEntityX,
        int linkEntityY,
        RoomEntityObjectSample objectUnderEntity) {}
