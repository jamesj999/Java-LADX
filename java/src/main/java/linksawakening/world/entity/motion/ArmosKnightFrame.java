package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;

/** Inputs for ArmosKnightMotion.advance. */
public record ArmosKnightFrame(
        RoomEntity entity,
        int linkEntityX,
        int linkEntityY,
        int linkZ,
        int transitionCountdown,
        int physicsFlags,
        int hitboxFlags,
        int options1,
        int health) {}
