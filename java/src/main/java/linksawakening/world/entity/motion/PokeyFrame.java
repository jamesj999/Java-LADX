package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for PokeyMotion.advanceMain. */
public record PokeyFrame(
        RoomEntity entity,
        int frameCounter,
        int inertia,
        int transitionCountdown,
        int flashCountdown,
        int linkEntityX,
        int linkEntityY,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision) {}
