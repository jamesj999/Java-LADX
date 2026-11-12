package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Inputs for UrchinMotion.advance. */
public record UrchinFrame(
        RoomEntity entity,
        int frameCounter,
        int physicsFlags,
        int linkEntityX,
        int linkEntityY,
        int linkDirection,
        boolean usingShield,
        boolean swordCollisionActive,
        boolean linkInteractive,
        RoomEntityBackgroundInteraction backgroundInteraction) {}
