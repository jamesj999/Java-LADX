package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import java.util.function.BooleanSupplier;

/** Source-compatible Link operations delegated to their state owners. */
abstract class LinkActionView extends LinkMovementView {
    public int romEntityY() {
        return linkMotion.accessRomEntityY();
    }

    public void applyLikeLikeCapture(int romEntityX, int romEntityY) {
        linkEntityEffects.accessApplyLikeLikeCapture(romEntityX, romEntityY);
    }

    public void applyLikeLikeCapture(int romEntityX, int romEntityY, boolean pinPosition) {
        linkEntityEffects.accessApplyLikeLikeCapture(romEntityX, romEntityY, pinPosition);
    }

    public void releaseLikeLikeCapture() {
        linkEntityEffects.accessReleaseLikeLikeCapture();
    }

    public void setCarryingLiftedObjectState(int carryState, int romDirection) {
        linkEntityEffects.accessSetCarryingLiftedObjectState(carryState, romDirection);
    }

    public void applyRoosterFlightState(int positionZ, int velocityZ, int speedX, int speedY, int romDirection) {
        linkEntityEffects.accessApplyRoosterFlightState(positionZ, velocityZ, speedX, speedY, romDirection);
    }

    public void applyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            int airborneState) {
        linkEntityEffects.accessApplyHinoxGrabState(
                romPositionX, romPositionY, positionZ, speedX, speedY, velocityZ, airborneState);
    }
    /** @deprecated Use the Link.GrabFlags argument bundle overload. */
    @Deprecated(forRemoval = true)
    public void applyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            int airborneState,
            boolean motionBlocked,
            boolean applyHeldLinkPose) {
        applyHinoxGrabState(
                romPositionX,
                romPositionY,
                positionZ,
                speedX,
                speedY,
                velocityZ,
                new Link.GrabFlags(airborneState, motionBlocked, applyHeldLinkPose));
    }

    public void applyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            Link.GrabFlags arguments) {
        int airborneState = arguments.airborneState();
        boolean motionBlocked = arguments.motionBlocked();
        boolean applyHeldLinkPose = arguments.applyHeldLinkPose();

        linkEntityEffects.accessApplyHinoxGrabState(
                romPositionX,
                romPositionY,
                positionZ,
                speedX,
                speedY,
                velocityZ,
                new Link.GrabFlags(airborneState, motionBlocked, applyHeldLinkPose));
    }

    public void applyWarpState3(int romPositionX, int visualRomPositionY, int immunityCountdown) {
        linkEntityEffects.accessApplyWarpState3(romPositionX, visualRomPositionY, immunityCountdown);
    }

    public void leaveMarinWakeUpBed() {
        linkEntityEffects.accessLeaveMarinWakeUpBed();
    }

    public void clearRoosterCarryState() {
        linkEntityEffects.accessClearRoosterCarryState();
    }

    public void showRomFallenPose() {
        linkAnimator.accessShowRomFallenPose();
    }

    public boolean dialogGotItemActive() {
        return linkAnimator.accessDialogGotItemActive();
    }

    public int dialogGotItem() {
        return linkAnimator.accessDialogGotItem();
    }

    public int dialogGotItemCountdown() {
        return linkAnimator.accessDialogGotItemCountdown();
    }

    public int presentedDialogGotItem() {
        return linkAnimator.accessPresentedDialogGotItem();
    }

    public void presentToadstool() {
        linkAnimator.accessPresentToadstool();
    }

    public void clearDialogGotItem() {
        linkAnimator.accessClearDialogGotItem();
    }

    public int tickDialogGotItem(boolean dialogActive) {
        return linkAirborne.accessTickDialogGotItem(dialogActive);
    }

    public void showHeldItemPose() {
        linkAnimator.accessShowHeldItemPose();
    }

    public void showPowerUpPickupPose() {
        linkAnimator.accessShowPowerUpPickupPose();
    }

    public void showTarinShieldPresentation(int[] palette) {
        linkAnimator.accessShowTarinShieldPresentation(palette);
    }

    public void clearTarinShieldPresentation() {
        linkAnimator.accessClearTarinShieldPresentation();
    }

    public void showStandingShieldDownPose() {
        linkAnimator.accessShowStandingShieldDownPose();
    }

    public void showSwordAcquisitionSpinPose(int countdown) {
        linkAnimator.accessShowSwordAcquisitionSpinPose(countdown);
    }

    public void showSwordAcquisitionFinalPose() {
        linkAnimator.accessShowSwordAcquisitionFinalPose();
    }

    public void showMarinWakeUpBed(int spriteVariant) {
        linkAnimator.accessShowMarinWakeUpBed(spriteVariant);
    }

    public void render(byte[] displayBuffer, int offsetX, int offsetY) {
        linkAnimator.accessRender(displayBuffer, offsetX, offsetY);
    }

    public void startRomItemAttackStep() {
        linkEquipment.accessStartRomItemAttackStep();
    }

    public void startRomMagicPowderAttackStep() {
        linkEquipment.accessStartRomMagicPowderAttackStep();
    }

    public void startRomMagicRodAttackStep() {
        linkEquipment.accessStartRomMagicRodAttackStep();
    }

    public void clearRomAttackStepAnimationCountdown() {
        linkEquipment.accessClearRomAttackStepAnimationCountdown();
    }

    public boolean canUseItems() {
        return linkEquipment.accessCanUseItems();
    }

    public void setShieldInputAllowed(BooleanSupplier allowed) {
        linkEquipment.accessSetShieldInputAllowed(allowed);
    }

    public boolean isUsingShield() {
        return linkEquipment.accessIsUsingShield();
    }

    public int applyRomItemDirectionFromInput() {
        return linkEquipment.accessApplyRomItemDirectionFromInput();
    }

    public void setDebugNoClip(boolean enabled) {
        linkStateMachine.accessSetDebugNoClip(enabled);
    }

    public void resetTransientStateForDebug() {
        linkStateMachine.accessResetTransientStateForDebug();
    }

    public void finishRoomScroll(boolean downward) {
        linkStateMachine.accessFinishRoomScroll(downward);
    }

    public void writeRomInteractiveMotionBlock(int value) {
        linkStateMachine.accessWriteRomInteractiveMotionBlock(value);
    }

    public int romInteractiveMotionBlockValue() {
        return linkStateMachine.accessRomInteractiveMotionBlockValue();
    }

    public int romSlowWalkingSpeed() {
        return linkStateMachine.accessRomSlowWalkingSpeed();
    }

    public int romItemUsageContext() {
        return linkStateMachine.accessRomItemUsageContext();
    }

    public boolean romItemAnimationFrameBlocked() {
        return linkSideView.accessRomItemAnimationFrameBlocked();
    }

    public void setGrabbingObject(boolean grabbing, boolean pulling) {
        linkEntityEffects.accessSetGrabbingObject(grabbing, pulling);
    }

    public void blockNextRomMotionFrame() {
        linkStateMachine.accessBlockNextRomMotionFrame();
    }

    public void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.accessInitializeRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    public void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.accessSetRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }
}
