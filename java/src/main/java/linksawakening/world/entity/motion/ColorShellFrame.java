package linksawakening.world.entity.motion;

import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for ColorShellMotion.advance. */
public record ColorShellFrame(
        RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision,
        int ignoreHitsCountdown,
        ColorShellWorld world,
        java.util.List<RoomEntity> allEntities) {}
