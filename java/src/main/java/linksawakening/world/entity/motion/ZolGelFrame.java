package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Inputs for ZolGelMotion.advance. */
public record ZolGelFrame(
        RoomEntity entity,
        int linkEntityX,
        int linkEntityY,
        int linkEntityZ,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundInteraction backgroundInteraction,
        int frameCounter,
        int ignoreHitsCountdown,
        boolean joypadHeld) {}
