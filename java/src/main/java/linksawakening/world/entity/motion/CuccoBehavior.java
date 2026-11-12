package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.CuccoMotion.*;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Focused behavior using the state owned by CuccoMotion. */
final class CuccoBehavior {
    static CuccoMotion.Update advance(CuccoMotion owner, CuccoFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int transitionCountdown = inputs.transitionCountdown();
        int flashCountdown = inputs.flashCountdown();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkZ = inputs.linkZ();
        boolean linkInteractive = inputs.linkInteractive();
        boolean powerBraceletHeld = inputs.powerBraceletHeld();
        boolean linkAttackStepActive = inputs.linkAttackStepActive();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        boolean indoorRoom = inputs.indoorRoom();
        boolean marinFollowing = inputs.marinFollowing();
        int transitionSequenceCounter = inputs.transitionSequenceCounter();
        return owner.advance(new CuccoInteractionFrame(
                entity,
                frameCounter,
                transitionCountdown,
                flashCountdown,
                linkEntityX,
                linkEntityY,
                linkZ,
                linkInteractive,
                powerBraceletHeld,
                linkAttackStepActive,
                randomByteSupplier,
                backgroundCollision,
                indoorRoom,
                marinFollowing,
                transitionSequenceCounter,
                true));
    }

    static CuccoMotion.Update advance(CuccoMotion owner, CuccoInteractionFrame inputs) {
        return owner.cuccoStep.run(owner, inputs);
    }
}
