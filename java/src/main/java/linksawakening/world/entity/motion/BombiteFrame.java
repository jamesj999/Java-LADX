package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for BombiteMotion.advance. */
public record BombiteFrame(
        RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        int transitionCountdown,
        int slowTransitionCountdown,
        int privateCountdown1,
        int enemyIgnoreHitsCountdown,
        boolean runningWithPegasusBoots,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision) {}
