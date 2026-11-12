package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Inputs for DogMotion.advance. */
public record DogFrame(
        RoomEntity entity,
        int frameCounter,
        int transitionCountdown,
        int physicsFlags,
        int linkEntityX,
        int linkEntityY,
        int linkDirection,
        boolean actionButtonAHeld,
        boolean dialogActive,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundInteraction backgroundInteraction) {}
