package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectSample;

/** Inputs for SecretSeashellMotion.advance. */
record SecretSeashellFrame(
        RoomEntity entity,
        int roomId,
        int frameCounter,
        boolean roomTransitionActive,
        boolean screenShakeActive,
        boolean pegasusCollisionActive,
        int linkEntityX,
        int linkEntityY,
        RoomEntityObjectSample objectUnderEntity) {}
