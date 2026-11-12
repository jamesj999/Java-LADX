package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityObjectQuery;

/** Inputs for ZombieMotion.advance. */
public record ZombieFrame(
        RoomEntity entity,
        int frameCounter,
        int transitionCountdown,
        int physicsFlags,
        int linkEntityX,
        int linkEntityY,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityObjectQuery objectQuery,
        RoomEntityBackgroundInteraction backgroundInteraction,
        int ignoreHitsCountdown) {}
