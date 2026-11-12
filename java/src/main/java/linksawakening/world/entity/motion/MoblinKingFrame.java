package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for MoblinKingMotion.advance. */
public record MoblinKingFrame(
        RoomEntity entity,
        int linkX,
        int linkY,
        int transitionCountdown,
        int slowTransitionCountdown,
        int frameCounter,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision,
        boolean touchingLink) {}
