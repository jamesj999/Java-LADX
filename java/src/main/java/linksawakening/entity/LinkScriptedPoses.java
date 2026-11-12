package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkAnimator.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.equipment.EquipmentItem;
import linksawakening.state.InventoryItem;

/** LinkScriptedPoses behavior on the shared LinkAnimator state. */
abstract class LinkScriptedPoses extends LinkAnimationSelection {
    protected LinkScriptedPoses(Dependencies dependencies) {
        super(dependencies);
    }

    protected void submitEquippedOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        if (linkServices.get().itemRegistry == null || linkServices.get().playerState == null) {
            return;
        }
        EquipmentItem a = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.itemA());
        if (a != null) {
            a.submitOam(
                    sink,
                    linkMotion.get().accessPixelX(),
                    linkMotion.get().accessPixelY() - linkAirborne.get().accessZPixels(),
                    linkAnimationState.get().direction,
                    offsetX,
                    offsetY);
        }
        EquipmentItem b = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.itemB());
        if (b != null && b != a) {
            b.submitOam(
                    sink,
                    linkMotion.get().accessPixelX(),
                    linkMotion.get().accessPixelY() - linkAirborne.get().accessZPixels(),
                    linkAnimationState.get().direction,
                    offsetX,
                    offsetY);
        }
        if (swordAcquisitionAnimationState >= 0) {
            // ApplyLinkMotionState draws the sword during the pickup spin
            // before GiveInventoryItem awards it to slot A/B. Reuse the ROM
            // sword handler from the registry, but do not draw it twice if a
            // save already has the sword equipped.
            EquipmentItem acquisitionSword = linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue());
            if (acquisitionSword != null && acquisitionSword != a && acquisitionSword != b) {
                acquisitionSword.submitOam(
                        sink,
                        linkMotion.get().accessPixelX(),
                        linkMotion.get().accessPixelY() - linkAirborne.get().accessZPixels(),
                        linkAnimationState.get().direction,
                        offsetX,
                        offsetY);
            }
        }
    }

    protected int renderY() {
        return ledgeJumpRenderY != Integer.MIN_VALUE
                ? ledgeJumpRenderY
                : linkMotion.get().accessPixelY()
                        - (bedJumpRenderZ >= 0
                                ? bedJumpRenderZ
                                : linkAirborne.get().accessZPixels());
    }

    protected boolean accessIsTarinShieldPresentationVisible() {
        return this.tarinShieldPresentation;
    }

    protected int[] accessTarinShieldPalette() {
        return this.tarinShieldPalette == null ? null : this.tarinShieldPalette.clone();
    }

    protected boolean accessIsMarinWakeUpBedVisible() {
        return this.marinWakeUpBedVariant >= 0;
    }

    protected int accessMarinWakeUpBedVariant() {
        return this.marinWakeUpBedVariant;
    }

    protected int accessRomAnimationState() {
        return this.resolveAnimationState();
    }

    protected void accessShowRomFallenPose() {
        this.showRomFallenPose();
    }

    protected boolean accessDialogGotItemActive() {
        return dialogGotItemSequence.get().item() != 0;
    }

    protected int accessDialogGotItem() {
        return dialogGotItemSequence.get().item();
    }

    protected int accessDialogGotItemCountdown() {
        return dialogGotItemSequence.get().countdown();
    }

    protected int accessPresentedDialogGotItem() {
        return dialogGotItemSequence.get().presentedItem();
    }

    protected void accessPresentToadstool() {
        if (linkAnimationState.get().accessRomEntityZ() != 0) return;
        dialogGotItemSequence.get().start(2, 0x2A);
    }

    protected void accessClearDialogGotItem() {
        dialogGotItemSequence.get().clear();
    }

    protected void accessShowHeldItemPose() {
        this.showHeldItemPose();
    }

    protected void accessShowPowerUpPickupPose() {
        this.showPowerUpPickupPose();
    }

    protected void accessShowTarinShieldPresentation(int[] palette) {
        this.showTarinShieldPresentation(palette);
    }

    protected void accessClearTarinShieldPresentation() {
        this.clearTarinShieldPresentation();
    }

    protected void accessShowStandingShieldDownPose() {
        this.showStandingShieldDownPose();
    }

    protected void accessShowSwordAcquisitionSpinPose(int countdown) {
        this.showSwordAcquisitionSpinPose(countdown);
    }

    protected void accessShowSwordAcquisitionFinalPose() {
        this.showSwordAcquisitionFinalPose();
    }

    protected void accessShowMarinWakeUpBed(int spriteVariant) {
        this.showMarinWakeUpBed(spriteVariant);
    }

    protected void accessRender(byte[] displayBuffer, int offsetX, int offsetY) {
        this.render(displayBuffer, offsetX, offsetY);
    }

    protected void accessSubmitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        this.submitOam(sink, offsetX, offsetY);
    }
}
