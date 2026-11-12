package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Animation operations from LinkMotionDefault and its related ROM routines. */
public interface LinkAnimationRole {
    boolean isTarinShieldPresentationVisible();

    int[] tarinShieldPalette();

    boolean isMarinWakeUpBedVisible();

    int marinWakeUpBedVariant();

    int romEntityZ();

    int direction();

    void setDirection(int newDirection);

    void setRomDirection(int romDirection);

    void faceRomDirectionPreservingWalkPhase(int romDirection);

    boolean grassGroundEffectFlipX();

    int romConsecutiveStepsCount();

    void tickAnimation();

    int romAnimationState();

    void showRomFallenPose();

    boolean dialogGotItemActive();

    int dialogGotItem();

    int dialogGotItemCountdown();

    int presentedDialogGotItem();

    void presentToadstool();

    void clearDialogGotItem();

    void showHeldItemPose();

    void showPowerUpPickupPose();

    void showTarinShieldPresentation(int[] palette);

    void clearTarinShieldPresentation();

    void showStandingShieldDownPose();

    void showSwordAcquisitionSpinPose(int countdown);

    void showSwordAcquisitionFinalPose();

    void showMarinWakeUpBed(int spriteVariant);

    void render(byte[] displayBuffer, int offsetX, int offsetY);

    void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY);
}
