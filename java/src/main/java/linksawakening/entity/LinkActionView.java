package linksawakening.entity;

import static linksawakening.entity.Link.*;

import java.util.function.BooleanSupplier;

/** @deprecated Temporary external API migration layer. */
@Deprecated(forRemoval = true)
abstract class LinkActionView extends LinkMovementView {

    /** @deprecated Use position().romEntityY instead. */
    @Deprecated(forRemoval = true)
    public int romEntityY() {
        return legacyRoles().position().romEntityY();
    }

    /** @deprecated Use actions().setCarryingLiftedObjectState instead. */
    @Deprecated(forRemoval = true)
    public void setCarryingLiftedObjectState(int carryState, int romDirection) {
        legacyRoles().actions().setCarryingLiftedObjectState(carryState, romDirection);
    }

    /** @deprecated Use actions().leaveMarinWakeUpBed instead. */
    @Deprecated(forRemoval = true)
    public void leaveMarinWakeUpBed() {
        legacyRoles().actions().leaveMarinWakeUpBed();
    }

    /** @deprecated Use actions().clearRoosterCarryState instead. */
    @Deprecated(forRemoval = true)
    public void clearRoosterCarryState() {
        legacyRoles().actions().clearRoosterCarryState();
    }

    /** @deprecated Use animation().dialogGotItemActive instead. */
    @Deprecated(forRemoval = true)
    public boolean dialogGotItemActive() {
        return legacyRoles().animation().dialogGotItemActive();
    }

    /** @deprecated Use animation().dialogGotItem instead. */
    @Deprecated(forRemoval = true)
    public int dialogGotItem() {
        return legacyRoles().animation().dialogGotItem();
    }

    /** @deprecated Use animation().dialogGotItemCountdown instead. */
    @Deprecated(forRemoval = true)
    public int dialogGotItemCountdown() {
        return legacyRoles().animation().dialogGotItemCountdown();
    }

    /** @deprecated Use animation().presentedDialogGotItem instead. */
    @Deprecated(forRemoval = true)
    public int presentedDialogGotItem() {
        return legacyRoles().animation().presentedDialogGotItem();
    }

    /** @deprecated Use animation().presentToadstool instead. */
    @Deprecated(forRemoval = true)
    public void presentToadstool() {
        legacyRoles().animation().presentToadstool();
    }

    /** @deprecated Use animation().clearDialogGotItem instead. */
    @Deprecated(forRemoval = true)
    public void clearDialogGotItem() {
        legacyRoles().animation().clearDialogGotItem();
    }

    /** @deprecated Use airborne().tickDialogGotItem instead. */
    @Deprecated(forRemoval = true)
    public int tickDialogGotItem(boolean dialogActive) {
        return legacyRoles().airborne().tickDialogGotItem(dialogActive);
    }

    /** @deprecated Use animation().showTarinShieldPresentation instead. */
    @Deprecated(forRemoval = true)
    public void showTarinShieldPresentation(int[] palette) {
        legacyRoles().animation().showTarinShieldPresentation(palette);
    }

    /** @deprecated Use animation().clearTarinShieldPresentation instead. */
    @Deprecated(forRemoval = true)
    public void clearTarinShieldPresentation() {
        legacyRoles().animation().clearTarinShieldPresentation();
    }

    /** @deprecated Use animation().showStandingShieldDownPose instead. */
    @Deprecated(forRemoval = true)
    public void showStandingShieldDownPose() {
        legacyRoles().animation().showStandingShieldDownPose();
    }

    /** @deprecated Use animation().showMarinWakeUpBed instead. */
    @Deprecated(forRemoval = true)
    public void showMarinWakeUpBed(int spriteVariant) {
        legacyRoles().animation().showMarinWakeUpBed(spriteVariant);
    }

    /** @deprecated Use equipment().startRomItemAttackStep instead. */
    @Deprecated(forRemoval = true)
    public void startRomItemAttackStep() {
        legacyRoles().equipment().startRomItemAttackStep();
    }

    /** @deprecated Use equipment().startRomMagicPowderAttackStep instead. */
    @Deprecated(forRemoval = true)
    public void startRomMagicPowderAttackStep() {
        legacyRoles().equipment().startRomMagicPowderAttackStep();
    }

    /** @deprecated Use equipment().startRomMagicRodAttackStep instead. */
    @Deprecated(forRemoval = true)
    public void startRomMagicRodAttackStep() {
        legacyRoles().equipment().startRomMagicRodAttackStep();
    }

    /** @deprecated Use equipment().clearRomAttackStepAnimationCountdown instead. */
    @Deprecated(forRemoval = true)
    public void clearRomAttackStepAnimationCountdown() {
        legacyRoles().equipment().clearRomAttackStepAnimationCountdown();
    }

    /** @deprecated Use equipment().canUseItems instead. */
    @Deprecated(forRemoval = true)
    public boolean canUseItems() {
        return legacyRoles().equipment().canUseItems();
    }

    /** @deprecated Use equipment().setShieldInputAllowed instead. */
    @Deprecated(forRemoval = true)
    public void setShieldInputAllowed(BooleanSupplier allowed) {
        legacyRoles().equipment().setShieldInputAllowed(allowed);
    }

    /** @deprecated Use equipment().isUsingShield instead. */
    @Deprecated(forRemoval = true)
    public boolean isUsingShield() {
        return legacyRoles().equipment().isUsingShield();
    }

    /** @deprecated Use equipment().applyRomItemDirectionFromInput instead. */
    @Deprecated(forRemoval = true)
    public int applyRomItemDirectionFromInput() {
        return legacyRoles().equipment().applyRomItemDirectionFromInput();
    }

    /** @deprecated Use motion().setDebugNoClip instead. */
    @Deprecated(forRemoval = true)
    public void setDebugNoClip(boolean enabled) {
        legacyRoles().motion().setDebugNoClip(enabled);
    }

    /** @deprecated Use motion().resetTransientStateForDebug instead. */
    @Deprecated(forRemoval = true)
    public void resetTransientStateForDebug() {
        legacyRoles().motion().resetTransientStateForDebug();
    }

    /** @deprecated Use motion().finishRoomScroll instead. */
    @Deprecated(forRemoval = true)
    public void finishRoomScroll(boolean downward) {
        legacyRoles().motion().finishRoomScroll(downward);
    }

    /** @deprecated Use motion().romInteractiveMotionBlockValue instead. */
    @Deprecated(forRemoval = true)
    public int romInteractiveMotionBlockValue() {
        return legacyRoles().motion().romInteractiveMotionBlockValue();
    }

    /** @deprecated Use motion().romSlowWalkingSpeed instead. */
    @Deprecated(forRemoval = true)
    public int romSlowWalkingSpeed() {
        return legacyRoles().motion().romSlowWalkingSpeed();
    }

    /** @deprecated Use motion().romItemUsageContext instead. */
    @Deprecated(forRemoval = true)
    public int romItemUsageContext() {
        return legacyRoles().motion().romItemUsageContext();
    }

    /** @deprecated Use sideView().romItemAnimationFrameBlocked instead. */
    @Deprecated(forRemoval = true)
    public boolean romItemAnimationFrameBlocked() {
        return legacyRoles().sideView().romItemAnimationFrameBlocked();
    }

    /** @deprecated Use actions().setGrabbingObject instead. */
    @Deprecated(forRemoval = true)
    public void setGrabbingObject(boolean grabbing, boolean pulling) {
        legacyRoles().actions().setGrabbingObject(grabbing, pulling);
    }

    /** @deprecated Use motion().blockNextRomMotionFrame instead. */
    @Deprecated(forRemoval = true)
    public void blockNextRomMotionFrame() {
        legacyRoles().motion().blockNextRomMotionFrame();
    }

    /** @deprecated Use motion().initializeRoomPhysicsForRoom instead. */
    @Deprecated(forRemoval = true)
    public void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        legacyRoles().motion().initializeRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    /** @deprecated Use motion().setRoomPhysicsForRoom instead. */
    @Deprecated(forRemoval = true)
    public void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        legacyRoles().motion().setRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }
}
