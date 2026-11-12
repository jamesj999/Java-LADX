package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for BowWowMotion.advanceWithTargetScan. */
record BowWowTargetFrame(
        RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        int linkEntityZ,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision,
        java.util.List<RoomEntity> entities,
        java.util.function.IntPredicate canEatEntity) {}
