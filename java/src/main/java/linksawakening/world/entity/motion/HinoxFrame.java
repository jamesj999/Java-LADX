package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Inputs for HinoxMotion.advance. */
public record HinoxFrame(
        RoomEntity entity,
        int transitionCountdown,
        int frameCounter,
        int linkX,
        int linkY,
        int linkZ,
        int linkMotionState,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundInteraction backgroundInteraction,
        int entityCollisionFlagsForFrame,
        int flashCountdown) {}
