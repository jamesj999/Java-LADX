package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for LikeLikeMotion.advance. */
public record LikeLikeFrame(
        RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        int linkZ,
        int linkMotionState,
        boolean actionButtonsHeld,
        int linkItemA,
        int linkItemB,
        int shieldLevel,
        int slowTransitionCountdown,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision) {}
