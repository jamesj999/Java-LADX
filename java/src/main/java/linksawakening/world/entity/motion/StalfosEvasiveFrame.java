package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for StalfosEvasiveMotion.advance. */
public record StalfosEvasiveFrame(
        RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        boolean actionButtonsHeld,
        int transitionCountdown,
        int ignoreHitsCountdown,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision,
        int mapId) {}
