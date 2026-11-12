package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectSample;

/** Inputs for SecretSeashellMotion.advance. */
public record SecretSeashellInteractionFrame(
        RoomEntity entity,
        int roomId,
        int frameCounter,
        boolean roomTransitionActive,
        boolean screenShakeActive,
        boolean pegasusCollisionActive,
        boolean pegasusCollisionCoordinatesKnown,
        int pegasusCollisionX,
        int pegasusCollisionY,
        int linkEntityX,
        int linkEntityY,
        RoomEntityObjectSample objectUnderEntity) {}
