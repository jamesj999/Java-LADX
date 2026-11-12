package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Inputs for MaskedMimicMotion.advance. */
public record MaskedMimicFrame(
        RoomEntity entity,
        int linkEntityX,
        int linkEntityY,
        int romLinkDirection,
        int pressedButtonsMask,
        int collisionType,
        RoomEntityBackgroundInteraction backgroundInteraction,
        int ignoreHitsCountdown,
        int currentFrame) {}
