package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkAnimator.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gpu.Tile;
import linksawakening.state.Direction;
import linksawakening.state.Tunic;

/** ROM pose selection and sprite data for both rendering paths. */

/** LinkAnimationSelection behavior on the shared LinkAnimator state. */
abstract class LinkAnimationSelection extends LinkSpriteRendering {
    protected LinkAnimationSelection(Dependencies dependencies) {
        super(dependencies);
    }

    /** $02:42C7 selects walking poses before this frame's movement/collisions. */
    protected void cachePushingAnimation() {
        pushingAnimationState = -1;
        if (linkItemState.get().accessIsRomLinkPushing()
                && (linkServices.get().collision == null
                        || !linkServices
                                .get()
                                .collision
                                .linkOnSlowGround(
                                        linkMotion.get().accessPixelX(),
                                        linkMotion.get().accessPixelY()))) {
            int frame = (linkAnimationState.get().romConsecutiveStepsCount >> 3) & 1;
            pushingAnimationState = linkServices.get().spriteSheet != null
                    ? linkServices
                            .get()
                            .spriteSheet
                            .pushingAnimationState(
                                    Direction.fromJava(linkAnimationState.get().direction)
                                            .romIndex(),
                                    frame)
                    : new int[] {0x1A, 0x1C, 0x1E, 0x20}[linkAnimationState.get().direction] + frame;
        }
    }

    protected int queryOverride(EquipmentItem item) {
        if (item == null) {
            return -1;
        }
        return item.overrideAnimationState(linkAnimationState.get().direction, linkAnimationState.get().walkFrame);
    }

    /** Applies the ROM's explicit hLinkAnimationState=$6A fallen-pose write. */
    protected void showRomFallenPose() {
        linkStateMachine.get().romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6A;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Applies HoldEntityAboveLink's got-item pose and interaction lock. */
    protected void showHeldItemPose() {
        linkStateMachine.get().romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6C;
        linkAnimationState.get().direction = Direction.DOWN.javaIndex();
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** HandleGotItemB's power-up pose, distinct from lifting a physical object. */
    protected void showPowerUpPickupPose() {
        linkStateMachine.get().romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6B;
        linkDamage.get().collisionIgnoreFramesRemaining = 0;
        linkMotion.get().forcedSpeedPending = false;
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Applies TarinShield2Handler's shield-specific got-item presentation. */
    protected void showTarinShieldPresentation(int[] palette) {
        if (palette == null || palette.length < 4) {
            throw new IllegalArgumentException("Tarin shield palette must contain four colors");
        }
        showHeldItemPose();
        tarinShieldPresentation = true;
        tarinShieldPalette = palette.clone();
    }

    protected void clearTarinShieldPresentation() {
        tarinShieldPresentation = false;
    }

    /** Mirrors TarinShield2Handler's final LINK_ANIMATION_STATE_STANDING_SHIELD_DOWN write. */
    protected void showStandingShieldDownPose() {
        tarinShieldPresentation = false;
        romAnimationStateOverride = 0x22;
        linkStateMachine.get().romInteractiveMotionBlocked = false;
        linkAnimationState.get().direction = Direction.DOWN.javaIndex();
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Applies one ROM spin sector while obtaining the beach sword. */
    protected void showSwordAcquisitionSpinPose(int countdown) {
        if (countdown < 0 || countdown > 0x20) {
            throw new IllegalArgumentException("Sword-spin countdown must be between 0 and 32");
        }
        if (linkServices.get().romTables == null) {
            throw new IllegalStateException("Sword acquisition spin requires ROM tables");
        }
        if (swordAcquisitionBaseRomDirection < 0) {
            swordAcquisitionBaseRomDirection =
                    Direction.fromJava(linkAnimationState.get().direction).romIndex();
            swordAcquisitionAnimationState = resolveAnimationState();
        }
        // The spin handler writes a fresh hLinkAnimationState. Do not let a
        // preceding generic motion-block request leave its $6A butt pose in
        // front of this ROM-driven sword pose.
        romAnimationStateOverride = -1;
        int sector = Math.min(7, countdown >> 2);
        int swordState = linkServices.get().romTables.swordSpinAnimationState(swordAcquisitionBaseRomDirection, sector);
        int absoluteRomDirection =
                linkServices.get().romTables.swordSpinAbsoluteDirection(swordAcquisitionBaseRomDirection, sector);
        int animationState = linkServices.get().romTables.swordDirectionAnimState(absoluteRomDirection, swordState);
        // UpdateSpinAttackAnimation leaves hLinkAnimationState unchanged for
        // LINK_ANIMATION_STATE_HIDDEN, so retain the previous ROM pose.
        if (animationState != 0xFF) {
            swordAcquisitionAnimationState = animationState;
        }
        linkStateMachine.get().romInteractiveMotionBlocked = true;
        linkAnimationState.get().direction =
                Direction.fromRom(absoluteRomDirection).javaIndex();
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Mirrors the sword handler's final hLinkAnimationState=$6B write. */
    protected void showSwordAcquisitionFinalPose() {
        linkStateMachine.get().romInteractiveMotionBlocked = true;
        swordAcquisitionBaseRomDirection = -1;
        swordAcquisitionAnimationState = -1;
        romAnimationStateOverride = 0x6B;
        linkAnimationState.get().direction = Direction.DOWN.javaIndex();
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Hides ordinary Link OAM and selects Marin's dedicated bed display list. */
    protected void showMarinWakeUpBed(int spriteVariant) {
        if (spriteVariant < 0 || spriteVariant > 4) {
            throw new IllegalArgumentException("Bed sprite variant must be between 0 and 4");
        }
        marinWakeUpBedVariant = spriteVariant;
    }

    protected void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        if (linkEntityEffects.get().likeLikeCaptured) {
            return;
        }
        Tile[] composedTiles = new Tile[4];
        int animationState = resolveAnimationState();
        if (animationState == 0xFF) return;
        linkServices.get().spriteSheet.resolveTiles(animationState, composedTiles);

        boolean leftFlipX = linkServices.get().spriteSheet.leftHalfFlipX(animationState);
        boolean leftFlipY = linkServices.get().spriteSheet.leftHalfFlipY(animationState);
        boolean rightFlipX = linkServices.get().spriteSheet.rightHalfFlipX(animationState);
        boolean rightFlipY = linkServices.get().spriteSheet.rightHalfFlipY(animationState);

        int originX = linkMotion.get().accessPixelX() + offsetX;
        int renderY = renderY();
        int originY = renderY + offsetY;
        int[] palette = linkServices.get().playerState != null
                        && (linkServices.get().playerState.invincibilityCounter() & 0x04) != 0
                // DrawLinkSprite's GBC path selects object palette 4 from bit 2
                // of wInvincibilityCounter while Link is flashing.
                ? linkServices.get().tunicPalette.forObjectPalette(4)
                : linkServices
                        .get()
                        .tunicPalette
                        .forTunic(
                                linkServices.get().playerState == null
                                        ? Tunic.GREEN.romValue()
                                        : linkServices.get().playerState.tunicType());

        if (linkStateMachine.get().revolvingDoorPaletteVariant >= 0
                && linkStateMachine.get().revolvingDoorFrame < 0x38) {
            palette = linkServices
                    .get()
                    .romTables
                    .revolvingDoorPalette(linkStateMachine.get().revolvingDoorPaletteVariant);
        }

        // Column-major tile layout: [0]=UL, [1]=LL, [2]=UR, [3]=LR.
        // GB 8x16 flipY swaps the two stacked tiles inside a column and flips
        // each vertically.
        Tile leftTop = composedTiles[leftFlipY ? 1 : 0];
        Tile leftBottom = composedTiles[leftFlipY ? 0 : 1];
        Tile rightTop = composedTiles[rightFlipY ? 3 : 2];
        Tile rightBottom = composedTiles[rightFlipY ? 2 : 3];

        // DrawLinkSprite ($00:1D4F-$1D59) rejects both body columns when
        // the byte-sized visual OAM Y is >= $88. Equipped items have their
        // own OAM writers and do not inherit this body-only return.
        if (((originY + 0x10) & 0xFF) < 0x88) {
            sink.append8x16(leftTop, leftBottom, originX, originY, leftFlipX, leftFlipY, palette, false);
            sink.append8x16(rightTop, rightBottom, originX + 8, originY, rightFlipX, rightFlipY, palette, false);
        }

        // Let equipped items paint their own sprites on top (e.g. the sword
        // blade during a swing). Matches the disassembly's ordering where
        // DrawLinkSprite writes Link's body to wLinkOAMBuffer slots 1-2 and
        // ApplyLinkMotionState/func_020_4AB3 writes the sword into slots 4-5.
        submitEquippedOam(sink, offsetX, offsetY);
    }
}
