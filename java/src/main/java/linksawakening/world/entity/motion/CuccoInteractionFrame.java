package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for CuccoMotion.advance. */
public record CuccoInteractionFrame(
        RoomEntity entity,
        int frameCounter,
        int transitionCountdown,
        int flashCountdown,
        int linkEntityX,
        int linkEntityY,
        int linkZ,
        boolean linkInteractive,
        boolean powerBraceletHeld,
        boolean linkAttackStepActive,
        java.util.function.IntSupplier randomByteSupplier,
        RoomEntityBackgroundCollision backgroundCollision,
        boolean indoorRoom,
        boolean marinFollowing,
        int transitionSequenceCounter,
        boolean angrySpawnSlotAvailable) {}
