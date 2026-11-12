package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for BushCrawlerMotion.advance. */
public record BushCrawlerFrame(
        RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision,
        int transitionCountdown,
        boolean linkCollision,
        boolean actionButtonAHeld,
        boolean actionButtonBHeld,
        int linkItemA,
        int linkItemB) {}
