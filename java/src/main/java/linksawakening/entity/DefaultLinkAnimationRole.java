package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Delegates animation operations to state owners used by ROM {@code UpdateLinkAnimation}. */
final class DefaultLinkAnimationRole implements LinkAnimationRole {
    private final LinkAnimationState linkAnimationState;
    private final LinkAnimator linkAnimator;

    DefaultLinkAnimationRole(LinkAssembly assembly) {
        linkAnimationState = assembly.linkAnimationState.get();
        linkAnimator = assembly.linkAnimator.get();
    }

    public boolean isTarinShieldPresentationVisible() {
        return linkAnimator.accessIsTarinShieldPresentationVisible();
    }

    public int[] tarinShieldPalette() {
        return linkAnimator.accessTarinShieldPalette();
    }

    public boolean isMarinWakeUpBedVisible() {
        return linkAnimator.accessIsMarinWakeUpBedVisible();
    }

    public int marinWakeUpBedVariant() {
        return linkAnimator.accessMarinWakeUpBedVariant();
    }

    public int romEntityZ() {
        return linkAnimationState.accessRomEntityZ();
    }

    public int direction() {
        return linkAnimationState.accessDirection();
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

    public boolean grassGroundEffectFlipX() {
        return linkAnimationState.accessGrassGroundEffectFlipX();
    }

    public int romConsecutiveStepsCount() {
        return linkAnimationState.accessRomConsecutiveStepsCount();
    }

    public void tickAnimation() {
        linkAnimationState.accessTickAnimation();
    }

    public int romAnimationState() {
        return linkAnimator.resolveAnimationState();
    }

    public void showRomFallenPose() {
        linkAnimator.showRomFallenPose();
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

    public void showHeldItemPose() {
        linkAnimator.showHeldItemPose();
    }

    public void showPowerUpPickupPose() {
        linkAnimator.showPowerUpPickupPose();
    }

    public void showTarinShieldPresentation(int[] palette) {
        linkAnimator.showTarinShieldPresentation(palette);
    }

    public void clearTarinShieldPresentation() {
        linkAnimator.clearTarinShieldPresentation();
    }

    public void showStandingShieldDownPose() {
        linkAnimator.showStandingShieldDownPose();
    }

    public void showSwordAcquisitionSpinPose(int countdown) {
        linkAnimator.showSwordAcquisitionSpinPose(countdown);
    }

    public void showSwordAcquisitionFinalPose() {
        linkAnimator.showSwordAcquisitionFinalPose();
    }

    public void showMarinWakeUpBed(int spriteVariant) {
        linkAnimator.showMarinWakeUpBed(spriteVariant);
    }

    public void render(byte[] displayBuffer, int offsetX, int offsetY) {
        linkAnimator.render(displayBuffer, offsetX, offsetY);
    }

    public void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        linkAnimator.submitOam(sink, offsetX, offsetY);
    }
}
