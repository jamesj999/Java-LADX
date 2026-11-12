package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

/** Source-compatible Link operations delegated to their state owners. */
abstract class LinkMovementView extends LinkStateView {
    LinkMovementView(LinkServices services) {
        super(services);
    }

    public int direction() {
        return linkAnimationState.accessDirection();
    }

    public boolean isCollisionDamageImmune() {
        return linkDamage.accessIsCollisionDamageImmune();
    }

    public boolean isRoosterCarryActive() {
        return linkEntityEffects.accessIsRoosterCarryActive();
    }

    public int carryingLiftedObjectState() {
        return linkEntityEffects.accessCarryingLiftedObjectState();
    }

    public boolean isCarryingLiftedObject() {
        return linkEntityEffects.accessIsCarryingLiftedObject();
    }

    public void setDirection(int newDirection) {
        linkAnimationState.accessSetDirection(newDirection);
    }

    public void setRomDirection(int romDirection) {
        linkAnimationState.accessSetRomDirection(romDirection);
    }

    public void faceRomDirectionPreservingWalkPhase(int romDirection) {
        linkAnimationState.accessFaceRomDirectionPreservingWalkPhase(romDirection);
    }

    public boolean isAirborne() {
        return linkAirborne.accessIsAirborne();
    }

    public int zVelocity() {
        return linkAirborne.accessZVelocity();
    }

    public boolean isFallingIntoPit() {
        return linkAirborne.accessIsFallingIntoPit();
    }

    public void useRocsFeather() {
        linkAirborne.accessUseRocsFeather();
    }

    public void bounceFromGoomba() {
        linkAirborne.accessBounceFromGoomba();
    }

    public void update() {
        linkGroundMotion.accessUpdate();
    }

    public boolean grassGroundEffectActive() {
        return linkGroundMotion.accessGrassGroundEffectActive();
    }

    public boolean grassGroundEffectFlipX() {
        return linkAnimationState.accessGrassGroundEffectFlipX();
    }

    public int romConsecutiveStepsCount() {
        return linkAnimationState.accessRomConsecutiveStepsCount();
    }

    void clearGroundEffects() {
        linkGroundMotion.accessClearGroundEffects();
    }

    public void tickAnimation() {
        linkAnimationState.accessTickAnimation();
    }

    public boolean consumeRevolvingDoorTransition() {
        return linkStateMachine.accessConsumeRevolvingDoorTransition();
    }

    boolean shouldApplyGroundMotion() {
        return linkGroundMotion.accessShouldApplyGroundMotion();
    }

    boolean buttonWasPressed(int key) {
        return linkControls.accessButtonWasPressed(key);
    }

    boolean playerHasFlippers() {
        return linkControls.accessPlayerHasFlippers();
    }

    int buildJoypadMask() {
        return linkControls.accessBuildJoypadMask();
    }

    public int romPressedButtonsMask() {
        return linkControls.accessRomPressedButtonsMask();
    }

    int zPixels() {
        return linkAirborne.accessZPixels();
    }

    public int romAnimationState() {
        return linkAnimator.accessRomAnimationState();
    }

    boolean isLiftTransitionBlockingMotion() {
        return linkEntityEffects.accessIsLiftTransitionBlockingMotion();
    }

    public void setPixelPosition(int pixelX, int pixelY) {
        linkMotion.accessSetPixelPosition(pixelX, pixelY);
    }

    public void setRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        linkMotion.accessSetRoomScrollEntryPixelPosition(pixelX, pixelY);
    }

    public void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        linkMotion.accessSetRoomEntryPixelPosition(pixelX, pixelY);
    }

    public void markRoomEntryPosition() {
        linkMotion.accessMarkRoomEntryPosition();
    }

    public int roomEntryPixelX() {
        return linkMotion.accessRoomEntryPixelX();
    }

    public int roomEntryPixelY() {
        return linkMotion.accessRoomEntryPixelY();
    }

    public int roomEntryRomPositionX() {
        return linkMotion.accessRoomEntryRomPositionX();
    }

    public int roomEntryRomPositionY() {
        return linkMotion.accessRoomEntryRomPositionY();
    }

    public void setRoomEntryRomPosition(int romPositionX, int romPositionY) {
        linkMotion.accessSetRoomEntryRomPosition(romPositionX, romPositionY);
    }

    public void applyRomSpeed(int speedX, int speedY) {
        linkMotion.accessApplyRomSpeed(speedX, speedY);
    }

    public void applyRomSpeedX(int speedX) {
        linkMotion.accessApplyRomSpeedX(speedX);
    }

    public void applyRomFinalPosition(int speedX, int speedY) {
        linkMotion.accessApplyRomFinalPosition(speedX, speedY);
    }

    public void captureRomFinalPosition() {
        linkMotion.accessCaptureRomFinalPosition();
    }

    public void restoreRomFinalPosition() {
        linkMotion.accessRestoreRomFinalPosition();
    }

    public void restoreRomFinalPositionX() {
        linkMotion.accessRestoreRomFinalPositionX();
    }

    public void clearRomPositionIncrement() {
        linkMotion.accessClearRomPositionIncrement();
    }

    public int pixelX() {
        return linkMotion.accessPixelX();
    }

    public int pixelY() {
        return linkMotion.accessPixelY();
    }

    public int romEntityX() {
        return linkMotion.accessRomEntityX();
    }

    public int romFinalPositionX() {
        return linkMotion.accessRomFinalPositionX();
    }
}
