package linksawakening.entity;

import linksawakening.world.transition.Warp;

/** Complete Link-side effects, independent of world event record locations. */
public interface LinkCommand {
    void applyTo(Link link);

    record Facing(int romDirection, boolean preserveWalkingPhase) implements LinkCommand {
        public void applyTo(Link link) {
            if (preserveWalkingPhase) link.faceRomDirectionPreservingWalkPhase(romDirection);
            else link.setRomDirection(romDirection);
        }
    }

    record FinalPosition(boolean clearIncrement, boolean markPushing, boolean resetBoots)
            implements LinkCommand {
        public void applyTo(Link link) {
            link.restoreRomFinalPosition();
            if (clearIncrement) link.clearRomPositionIncrement();
            if (markPushing) link.markRomLinkPushing(0x03);
            if (resetBoots) link.resetPegasusBoots();
        }
    }

    record PlatformContact(int horizontalDelta, int positionY, int speedY, boolean standing)
            implements LinkCommand {
        public void applyTo(Link link) {
            link.applySideViewPlatformContact(horizontalDelta, positionY, speedY, standing);
        }
    }

    record PotContact(boolean restoreFinalX, boolean resetBoots, int speedX,
                      int ignoreCollisionCountdown, boolean snapTop, int positionY, int speedY)
            implements LinkCommand {
        public void applyTo(Link link) {
            if (restoreFinalX) link.restoreRomFinalPositionX();
            if (resetBoots) link.resetPegasusBoots();
            if (speedX != 0) link.applyRomSpeedX(speedX);
            if (ignoreCollisionCountdown != 0) link.setCollisionIgnoreFrames(ignoreCollisionCountdown);
            if (snapTop) link.applySideViewPotContact(positionY, speedY);
        }
    }

    record MotionBlock(int deltaX, int deltaY) implements LinkCommand {
        public void applyTo(Link link) {
            link.applyRomFinalPosition((deltaX * 16) & 0xFF, (deltaY * 16) & 0xFF);
            link.blockNextRomMotionFrame();
        }
    }

    record Warp(int positionX, int visualPositionY, int immunityCountdown) implements LinkCommand {
        public void applyTo(Link link) { link.applyWarpState3(positionX, visualPositionY, immunityCountdown); }
    }

    record HeldItemPose(boolean powerUp) implements LinkCommand {
        public void applyTo(Link link) {
            if (powerUp) link.showPowerUpPickupPose();
            else link.showHeldItemPose();
        }
    }

    record SwordSpinPose(int countdown) implements LinkCommand {
        public void applyTo(Link link) { link.showSwordAcquisitionSpinPose(countdown); }
    }

    record RoosterFlight(int positionZ, int velocityZ, int speedX, int speedY, int romDirection)
            implements LinkCommand {
        public void applyTo(Link link) {
            link.applyRoosterFlightState(positionZ, velocityZ, speedX, speedY, romDirection);
        }
    }

    record HinoxGrab(int positionX, int positionY, int positionZ, int speedX, int speedY,
                     int velocityZ, int airborneState, boolean motionBlocked, boolean heldPose)
            implements LinkCommand {
        public void applyTo(Link link) {
            link.applyHinoxGrabState(positionX, positionY, positionZ, speedX, speedY,
                velocityZ, airborneState, motionBlocked, heldPose);
        }
    }

    record Capture(int positionX, int positionY, boolean pinPosition) implements LinkCommand {
        public void applyTo(Link link) { link.applyLikeLikeCapture(positionX, positionY, pinPosition); }
    }

    record ProjectileResponse(int speedX, int speedY, int ignoreCollisionCountdown,
                              boolean immediatePosition) implements LinkCommand {
        public void applyTo(Link link) {
            if (immediatePosition) link.applyRomFinalPosition(speedX, speedY);
            else {
                link.applyRomSpeed(speedX, speedY);
                link.setCollisionIgnoreFrames(ignoreCollisionCountdown);
            }
        }
    }

    enum Action implements LinkCommand {
        BLOCK_MOTION, FALLEN_POSE, CLEAR_ATTACK, START_ATTACK, SWORD_FINAL_POSE, RELEASE_CAPTURE;
        public void applyTo(Link link) {
            switch (this) {
                case BLOCK_MOTION -> link.blockNextRomMotionFrame();
                case FALLEN_POSE -> link.showRomFallenPose();
                case CLEAR_ATTACK -> link.clearRomAttackStepAnimationCountdown();
                case START_ATTACK -> link.startRomItemAttackStep();
                case SWORD_FINAL_POSE -> link.showSwordAcquisitionFinalPose();
                case RELEASE_CAPTURE -> link.releaseLikeLikeCapture();
            }
        }
    }
}
