package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Inputs for MoblinSwordMotion.advance. */
public record MoblinSwordFrame(
        RoomEntity entity,
        int linkEntityX,
        int linkEntityY,
        RoomEntityBackgroundInteraction backgroundInteraction,
        int ignoreHitsCountdown,
        int alertingSoundCounter,
        int mapId,
        int transitionSequenceCounter,
        int frameCounter) {}
