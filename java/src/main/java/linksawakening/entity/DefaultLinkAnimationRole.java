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
        return linkAnimator.accessRomAnimationState();
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

    public void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        linkAnimator.accessSubmitOam(sink, offsetX, offsetY);
    }
}
