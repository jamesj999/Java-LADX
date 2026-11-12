package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** @deprecated Temporary external API migration layer. */
@Deprecated(forRemoval = true)
abstract class LinkMovementView extends LinkStateView {

    /** @deprecated Use animation().direction instead. */
    @Deprecated(forRemoval = true)
    public int direction() {
        return legacyRoles().animation().direction();
    }

    /** @deprecated Use status().isCollisionDamageImmune instead. */
    @Deprecated(forRemoval = true)
    public boolean isCollisionDamageImmune() {
        return legacyRoles().status().isCollisionDamageImmune();
    }

    /** @deprecated Use actions().isRoosterCarryActive instead. */
    @Deprecated(forRemoval = true)
    public boolean isRoosterCarryActive() {
        return legacyRoles().actions().isRoosterCarryActive();
    }

    /** @deprecated Use actions().carryingLiftedObjectState instead. */
    @Deprecated(forRemoval = true)
    public int carryingLiftedObjectState() {
        return legacyRoles().actions().carryingLiftedObjectState();
    }

    /** @deprecated Use actions().isCarryingLiftedObject instead. */
    @Deprecated(forRemoval = true)
    public boolean isCarryingLiftedObject() {
        return legacyRoles().actions().isCarryingLiftedObject();
    }

    /** @deprecated Use animation().setDirection instead. */
    @Deprecated(forRemoval = true)
    public void setDirection(int newDirection) {
        legacyRoles().animation().setDirection(newDirection);
    }

    /** @deprecated Use animation().setRomDirection instead. */
    @Deprecated(forRemoval = true)
    public void setRomDirection(int romDirection) {
        legacyRoles().animation().setRomDirection(romDirection);
    }

    /** @deprecated Use animation().faceRomDirectionPreservingWalkPhase instead. */
    @Deprecated(forRemoval = true)
    public void faceRomDirectionPreservingWalkPhase(int romDirection) {
        legacyRoles().animation().faceRomDirectionPreservingWalkPhase(romDirection);
    }

    /** @deprecated Use airborne().isAirborne instead. */
    @Deprecated(forRemoval = true)
    public boolean isAirborne() {
        return legacyRoles().airborne().isAirborne();
    }

    /** @deprecated Use airborne().zVelocity instead. */
    @Deprecated(forRemoval = true)
    public int zVelocity() {
        return legacyRoles().airborne().zVelocity();
    }

    /** @deprecated Use airborne().isFallingIntoPit instead. */
    @Deprecated(forRemoval = true)
    public boolean isFallingIntoPit() {
        return legacyRoles().airborne().isFallingIntoPit();
    }

    /** @deprecated Use airborne().useRocsFeather instead. */
    @Deprecated(forRemoval = true)
    public void useRocsFeather() {
        legacyRoles().airborne().useRocsFeather();
    }

    /** @deprecated Use airborne().bounceFromGoomba instead. */
    @Deprecated(forRemoval = true)
    public void bounceFromGoomba() {
        legacyRoles().airborne().bounceFromGoomba();
    }

    /** @deprecated Use motion().update instead. */
    @Deprecated(forRemoval = true)
    public void update() {
        legacyRoles().motion().update();
    }

    /** @deprecated Use motion().grassGroundEffectActive instead. */
    @Deprecated(forRemoval = true)
    public boolean grassGroundEffectActive() {
        return legacyRoles().motion().grassGroundEffectActive();
    }

    /** @deprecated Use animation().grassGroundEffectFlipX instead. */
    @Deprecated(forRemoval = true)
    public boolean grassGroundEffectFlipX() {
        return legacyRoles().animation().grassGroundEffectFlipX();
    }

    /** @deprecated Use animation().romConsecutiveStepsCount instead. */
    @Deprecated(forRemoval = true)
    public int romConsecutiveStepsCount() {
        return legacyRoles().animation().romConsecutiveStepsCount();
    }

    /** @deprecated Use animation().tickAnimation instead. */
    @Deprecated(forRemoval = true)
    public void tickAnimation() {
        legacyRoles().animation().tickAnimation();
    }

    /** @deprecated Use motion().consumeRevolvingDoorTransition instead. */
    @Deprecated(forRemoval = true)
    public boolean consumeRevolvingDoorTransition() {
        return legacyRoles().motion().consumeRevolvingDoorTransition();
    }

    /** @deprecated Use controls().romPressedButtonsMask instead. */
    @Deprecated(forRemoval = true)
    public int romPressedButtonsMask() {
        return legacyRoles().controls().romPressedButtonsMask();
    }

    /** @deprecated Use animation().romAnimationState instead. */
    @Deprecated(forRemoval = true)
    public int romAnimationState() {
        return legacyRoles().animation().romAnimationState();
    }

    /** @deprecated Use position().setPixelPosition instead. */
    @Deprecated(forRemoval = true)
    public void setPixelPosition(int pixelX, int pixelY) {
        legacyRoles().position().setPixelPosition(pixelX, pixelY);
    }

    /** @deprecated Use position().setRoomScrollEntryPixelPosition instead. */
    @Deprecated(forRemoval = true)
    public void setRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        legacyRoles().position().setRoomScrollEntryPixelPosition(pixelX, pixelY);
    }

    /** @deprecated Use position().setRoomEntryPixelPosition instead. */
    @Deprecated(forRemoval = true)
    public void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        legacyRoles().position().setRoomEntryPixelPosition(pixelX, pixelY);
    }

    /** @deprecated Use position().markRoomEntryPosition instead. */
    @Deprecated(forRemoval = true)
    public void markRoomEntryPosition() {
        legacyRoles().position().markRoomEntryPosition();
    }

    /** @deprecated Use position().roomEntryPixelX instead. */
    @Deprecated(forRemoval = true)
    public int roomEntryPixelX() {
        return legacyRoles().position().roomEntryPixelX();
    }

    /** @deprecated Use position().roomEntryPixelY instead. */
    @Deprecated(forRemoval = true)
    public int roomEntryPixelY() {
        return legacyRoles().position().roomEntryPixelY();
    }

    /** @deprecated Use position().roomEntryRomPositionX instead. */
    @Deprecated(forRemoval = true)
    public int roomEntryRomPositionX() {
        return legacyRoles().position().roomEntryRomPositionX();
    }

    /** @deprecated Use position().roomEntryRomPositionY instead. */
    @Deprecated(forRemoval = true)
    public int roomEntryRomPositionY() {
        return legacyRoles().position().roomEntryRomPositionY();
    }

    /** @deprecated Use position().setRoomEntryRomPosition instead. */
    @Deprecated(forRemoval = true)
    public void setRoomEntryRomPosition(int romPositionX, int romPositionY) {
        legacyRoles().position().setRoomEntryRomPosition(romPositionX, romPositionY);
    }

    /** @deprecated Use position().applyRomSpeed instead. */
    @Deprecated(forRemoval = true)
    public void applyRomSpeed(int speedX, int speedY) {
        legacyRoles().position().applyRomSpeed(speedX, speedY);
    }

    /** @deprecated Use position().applyRomSpeedX instead. */
    @Deprecated(forRemoval = true)
    public void applyRomSpeedX(int speedX) {
        legacyRoles().position().applyRomSpeedX(speedX);
    }

    /** @deprecated Use position().applyRomFinalPosition instead. */
    @Deprecated(forRemoval = true)
    public void applyRomFinalPosition(int speedX, int speedY) {
        legacyRoles().position().applyRomFinalPosition(speedX, speedY);
    }

    /** @deprecated Use position().captureRomFinalPosition instead. */
    @Deprecated(forRemoval = true)
    public void captureRomFinalPosition() {
        legacyRoles().position().captureRomFinalPosition();
    }

    /** @deprecated Use position().restoreRomFinalPosition instead. */
    @Deprecated(forRemoval = true)
    public void restoreRomFinalPosition() {
        legacyRoles().position().restoreRomFinalPosition();
    }

    /** @deprecated Use position().restoreRomFinalPositionX instead. */
    @Deprecated(forRemoval = true)
    public void restoreRomFinalPositionX() {
        legacyRoles().position().restoreRomFinalPositionX();
    }

    /** @deprecated Use position().clearRomPositionIncrement instead. */
    @Deprecated(forRemoval = true)
    public void clearRomPositionIncrement() {
        legacyRoles().position().clearRomPositionIncrement();
    }

    /** @deprecated Use position().pixelX instead. */
    @Deprecated(forRemoval = true)
    public int pixelX() {
        return legacyRoles().position().pixelX();
    }

    /** @deprecated Use position().pixelY instead. */
    @Deprecated(forRemoval = true)
    public int pixelY() {
        return legacyRoles().position().pixelY();
    }

    /** @deprecated Use position().romEntityX instead. */
    @Deprecated(forRemoval = true)
    public int romEntityX() {
        return legacyRoles().position().romEntityX();
    }

    /** @deprecated Use position().romFinalPositionX instead. */
    @Deprecated(forRemoval = true)
    public int romFinalPositionX() {
        return legacyRoles().position().romFinalPositionX();
    }
}
