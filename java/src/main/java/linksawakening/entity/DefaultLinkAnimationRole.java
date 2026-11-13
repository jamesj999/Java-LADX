package linksawakening.entity;

/** Delegates animation operations to state owners used by ROM {@code UpdateLinkAnimation}. */
final class DefaultLinkAnimationRole implements LinkAnimationRole {
    private final LinkAnimationState linkAnimationState;
    private final LinkAnimator linkAnimator;

    DefaultLinkAnimationRole(LinkAssembly assembly) {
        linkAnimationState = assembly.linkAnimationState.get();
        linkAnimator = assembly.linkAnimator.get();
    }

    @Override
    public boolean isTarinShieldPresentationVisible() {
        return linkAnimator.accessIsTarinShieldPresentationVisible();
    }

    @Override
    public int[] tarinShieldPalette() {
        return linkAnimator.accessTarinShieldPalette();
    }

    @Override
    public boolean isMarinWakeUpBedVisible() {
        return linkAnimator.accessIsMarinWakeUpBedVisible();
    }

    @Override
    public int marinWakeUpBedVariant() {
        return linkAnimator.accessMarinWakeUpBedVariant();
    }

    @Override
    public int romEntityZ() {
        return linkAnimationState.accessRomEntityZ();
    }

    @Override
    public int direction() {
        return linkAnimationState.accessDirection();
    }

    @Override
    public void setDirection(int newDirection) {
        linkAnimationState.accessSetDirection(newDirection);
    }

    @Override
    public void setRomDirection(int romDirection) {
        linkAnimationState.accessSetRomDirection(romDirection);
    }

    @Override
    public void faceRomDirectionPreservingWalkPhase(int romDirection) {
        linkAnimationState.accessFaceRomDirectionPreservingWalkPhase(romDirection);
    }

    @Override
    public boolean grassGroundEffectFlipX() {
        return linkAnimationState.accessGrassGroundEffectFlipX();
    }

    @Override
    public int romConsecutiveStepsCount() {
        return linkAnimationState.accessRomConsecutiveStepsCount();
    }

    @Override
    public void tickAnimation() {
        linkAnimationState.accessTickAnimation();
    }

    @Override
    public int romAnimationState() {
        return linkAnimator.resolveAnimationState();
    }

    @Override
    public void showRomFallenPose() {
        linkAnimator.showRomFallenPose();
    }

    @Override
    public boolean dialogGotItemActive() {
        return linkAnimator.accessDialogGotItemActive();
    }

    @Override
    public int dialogGotItem() {
        return linkAnimator.accessDialogGotItem();
    }

    @Override
    public int dialogGotItemCountdown() {
        return linkAnimator.accessDialogGotItemCountdown();
    }

    @Override
    public int presentedDialogGotItem() {
        return linkAnimator.accessPresentedDialogGotItem();
    }

    @Override
    public void presentToadstool() {
        linkAnimator.accessPresentToadstool();
    }

    @Override
    public void clearDialogGotItem() {
        linkAnimator.accessClearDialogGotItem();
    }

    @Override
    public void showHeldItemPose() {
        linkAnimator.showHeldItemPose();
    }

    @Override
    public void showPowerUpPickupPose() {
        linkAnimator.showPowerUpPickupPose();
    }

    @Override
    public void showTarinShieldPresentation(int[] palette) {
        linkAnimator.showTarinShieldPresentation(palette);
    }

    @Override
    public void clearTarinShieldPresentation() {
        linkAnimator.clearTarinShieldPresentation();
    }

    @Override
    public void showStandingShieldDownPose() {
        linkAnimator.showStandingShieldDownPose();
    }

    @Override
    public void showSwordAcquisitionSpinPose(int countdown) {
        linkAnimator.showSwordAcquisitionSpinPose(countdown);
    }

    @Override
    public void showSwordAcquisitionFinalPose() {
        linkAnimator.showSwordAcquisitionFinalPose();
    }

    @Override
    public void showMarinWakeUpBed(int spriteVariant) {
        linkAnimator.showMarinWakeUpBed(spriteVariant);
    }

    @Override
    public void render(byte[] displayBuffer, int offsetX, int offsetY) {
        linkAnimator.render(displayBuffer, offsetX, offsetY);
    }

    @Override
    public void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        linkAnimator.submitOam(sink, offsetX, offsetY);
    }
}
