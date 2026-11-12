package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for WingedOctorokMotion.advance. */
public record WingedOctorokInteractionFrame(
        RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision,
        int transitionCountdown,
        int ignoreHitsCountdown,
        boolean actionButtonAHeld,
        boolean actionButtonBHeld,
        int linkItemA,
        int linkItemB) {}
