package linksawakening.entity;

/** Complete Link-side effects, independent of world event record locations. */
public interface LinkCommand {
    void applyTo(Link link);
    /** Immutable facing values. */
    public record Facing(int romDirection, boolean preserveWalkingPhase) implements LinkCommand {
        public void applyTo(Link link) {
            if (preserveWalkingPhase) link.animation().faceRomDirectionPreservingWalkPhase(romDirection);
            else link.animation().setRomDirection(romDirection);
        }
    }
    /** Immutable final position values. */
    public record FinalPosition(boolean clearIncrement, boolean markPushing, boolean resetBoots)
            implements LinkCommand {
        public void applyTo(Link link) {
            link.position().restoreRomFinalPosition();
            if (clearIncrement) link.position().clearRomPositionIncrement();
            if (markPushing) link.equipment().markRomLinkPushing(0x03);
            if (resetBoots) link.motion().resetPegasusBoots();
        }
    }
    /** Immutable platform contact values. */
    public record PlatformContact(int horizontalDelta, int positionY, int speedY, boolean standing)
            implements LinkCommand {
        public void applyTo(Link link) {
            link.sideView().applySideViewPlatformContact(horizontalDelta, positionY, speedY, standing);
        }
    }
    /** Immutable pot contact values. */
    public record PotContact(
            boolean restoreFinalX,
            boolean resetBoots,
            int speedX,
            int ignoreCollisionCountdown,
            boolean snapTop,
            int positionY,
            int speedY)
            implements LinkCommand {
        public void applyTo(Link link) {
            if (restoreFinalX) link.position().restoreRomFinalPositionX();
            if (resetBoots) link.motion().resetPegasusBoots();
            if (speedX != 0) link.position().applyRomSpeedX(speedX);
            if (ignoreCollisionCountdown != 0) link.status().setCollisionIgnoreFrames(ignoreCollisionCountdown);
            if (snapTop) link.sideView().applySideViewPotContact(positionY, speedY);
        }
    }
    /** Immutable motion block values. */
    public record MotionBlock(int deltaX, int deltaY, int value) implements LinkCommand {
        public MotionBlock(int deltaX, int deltaY) {
            this(deltaX, deltaY, 2);
        }

        public void applyTo(Link link) {
            link.position().applyRomFinalPosition((deltaX * 16) & 0xFF, (deltaY * 16) & 0xFF);
            link.motion().writeRomInteractiveMotionBlock(value);
        }
    }
    /** Immutable warp values. */
    public record Warp(int positionX, int visualPositionY, int immunityCountdown) implements LinkCommand {
        public void applyTo(Link link) {
            link.actions().applyWarpState3(positionX, visualPositionY, immunityCountdown);
        }
    }
    /** Immutable held item pose values. */
    public record HeldItemPose(boolean powerUp) implements LinkCommand {
        public void applyTo(Link link) {
            if (powerUp) link.animation().showPowerUpPickupPose();
            else link.animation().showHeldItemPose();
        }
    }
    /** Immutable sword spin pose values. */
    public record SwordSpinPose(int countdown) implements LinkCommand {
        public void applyTo(Link link) {
            link.animation().showSwordAcquisitionSpinPose(countdown);
        }
    }
    /** Immutable rooster flight values. */
    public record RoosterFlight(int positionZ, int velocityZ, int speedX, int speedY, int romDirection)
            implements LinkCommand {
        public void applyTo(Link link) {
            link.actions().applyRoosterFlightState(positionZ, velocityZ, speedX, speedY, romDirection);
        }
    }
    /** Immutable hinox grab values. */
    public record HinoxGrab(
            int positionX,
            int positionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            int airborneState,
            boolean motionBlocked,
            boolean heldPose)
            implements LinkCommand {
        /** @deprecated Use the overload accepting Link.GrabFlags. */
        @Deprecated(forRemoval = true)
        public HinoxGrab {}

        public HinoxGrab(
                int positionX,
                int positionY,
                int positionZ,
                int speedX,
                int speedY,
                int velocityZ,
                Link.GrabFlags flags) {
            this(
                    positionX,
                    positionY,
                    positionZ,
                    speedX,
                    speedY,
                    velocityZ,
                    flags.airborneState(),
                    flags.motionBlocked(),
                    flags.applyHeldLinkPose());
        }

        public void applyTo(Link link) {
            link.actions()
                    .applyHinoxGrabState(
                            positionX,
                            positionY,
                            positionZ,
                            speedX,
                            speedY,
                            velocityZ,
                            Link.GrabFlags.of(airborneState, motionBlocked, heldPose));
        }
    }
    /** Immutable capture values. */
    public record Capture(int positionX, int positionY, boolean pinPosition) implements LinkCommand {
        public void applyTo(Link link) {
            link.actions().applyLikeLikeCapture(positionX, positionY, pinPosition);
        }
    }
    /** Immutable contact recoil values. */
    public record ContactRecoil(int speedX, int speedY, int ignoreCollisionCountdown) implements LinkCommand {
        public void applyTo(Link link) {
            link.position().applyRomSpeed(speedX, speedY);
            link.status().setCollisionIgnoreFrames(ignoreCollisionCountdown);
        }
    }
    /** Immutable projectile response values. */
    public record ProjectileResponse(int speedX, int speedY, int ignoreCollisionCountdown, boolean immediatePosition)
            implements LinkCommand {
        public void applyTo(Link link) {
            if (immediatePosition) link.position().applyRomFinalPosition(speedX, speedY);
            else {
                link.position().applyRomSpeed(speedX, speedY);
                link.status().setCollisionIgnoreFrames(ignoreCollisionCountdown);
            }
        }
    }
    /** Named action values. */
    public enum Action implements LinkCommand {
        BLOCK_MOTION,
        FALLEN_POSE,
        CLEAR_ATTACK,
        START_ATTACK,
        SWORD_FINAL_POSE,
        RELEASE_CAPTURE;

        public void applyTo(Link link) {
            switch (this) {
                case BLOCK_MOTION -> link.motion().blockNextRomMotionFrame();
                case FALLEN_POSE -> link.animation().showRomFallenPose();
                case CLEAR_ATTACK -> link.equipment().clearRomAttackStepAnimationCountdown();
                case START_ATTACK -> link.equipment().startRomItemAttackStep();
                case SWORD_FINAL_POSE -> link.animation().showSwordAcquisitionFinalPose();
                case RELEASE_CAPTURE -> link.actions().releaseLikeLikeCapture();
            }
        }
    }
}
