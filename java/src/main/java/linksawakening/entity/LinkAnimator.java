package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.equipment.EquipmentItem;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** Pose selection and scripted presentation for ROM UpdateLinkAnimation ($02:47BC). */
final class LinkAnimator {
    private final LinkSpriteComposer sprites;

    private final LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    private final LinkComponentRef<LinkEquipment> linkEquipment;
    private final LinkComponentRef<LinkItemState> linkItemState;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    static final class Dependencies {
        LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkEntityEffects> linkEntityEffects;
        LinkComponentRef<LinkEquipment> linkEquipment;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

    private boolean tarinShieldPresentation;
    private int[] tarinShieldPalette;
    private int romAnimationStateOverride = -1;
    private int swordAcquisitionBaseRomDirection = -1;
    private int swordAcquisitionAnimationState = -1;
    private int marinWakeUpBedVariant = -1;
    private int bedJumpRenderZ = -1;
    /** DrawLinkSprite uses wC145, cached before ledge motion ($00:1033). */
    private int ledgeJumpRenderY = Integer.MIN_VALUE;

    private int jumpAnimationCounter;
    private int jumpAnimationFrame;

    private int pushingAnimationState = -1;

    LinkAnimator(Dependencies dependencies) {
        dialogGotItemSequence = dependencies.dialogGotItemSequence;
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkDamage = dependencies.linkDamage;
        linkEntityEffects = dependencies.linkEntityEffects;
        linkEquipment = dependencies.linkEquipment;
        linkItemState = dependencies.linkItemState;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkStateMachine = dependencies.linkStateMachine;
        sprites = new LinkSpriteComposer(linkAirborne, linkAnimationState, linkMotion, linkServices, linkStateMachine);
    }

    int resolveAnimationState() {
        if (romAnimationStateOverride >= 0) {
            return romAnimationStateOverride;
        }
        if (swordAcquisitionAnimationState >= 0) {
            return swordAcquisitionAnimationState;
        }
        if (linkAirborne.get().pit().fallingIntoPit()) {
            int frame = Math.min(
                    LinkPoseTables.FALL_ANIMATION_STATE.length - 1,
                    linkAirborne.get().pit().fallingFrameCounter() / LinkRomConstants.FALL_FRAME_TICKS);
            return LinkPoseTables.FALL_ANIMATION_STATE[frame];
        }

        if (linkStateMachine.get().status().motionState() == LinkMotionState.SWIMMING) {
            return linkStateMachine.get().roomPhysics().physicsModifier() != 0
                    ? LinkPoseTables.DIVING_ANIMATION_STATE[linkAnimationState.get().walkFrame]
                    : LinkPoseTables.SWIMMING_ANIMATION_STATE[
                            linkAnimationState.get().direction.javaIndex()][linkAnimationState.get().walkFrame];
        }

        if (linkEntityEffects.get().grabbingObject) {
            return 0x36
                    + 2 * linkAnimationState.get().direction.romIndex()
                    + (linkEntityEffects.get().pullingObject ? 1 : 0);
        }
        // func_002_4338 copies the intermediate ROM carry animation state
        // directly (for example $37/$39/$3B/$3D). The final carry state is
        // the regular two-frame lifting list below.
        if (linkEntityEffects.get().carryingLiftedObjectState >= 2) {
            return linkEntityEffects.get().carryingLiftedObjectState;
        }
        return resolveNormalAnimation();
    }

    private int resolveNormalAnimation() {
        // ApplyLinkGroundMotion ($02:4503) sets the step counter to $FF
        // while airborne, selecting the alternate normal pose after the flip.
        int normalFrame = linkAirborne.get().elevation().airborne()
                        || (ledgeJumpRenderY != Integer.MIN_VALUE
                                && linkAnimationState.get().romConsecutiveStepsCount == 0xFF)
                ? 1
                : linkAnimationState.get().walkFrame;
        int baseState = LinkPoseTables.ANIMATION_STATE[
                linkAnimationState.get().direction.javaIndex()][normalFrame];
        if (linkEntityEffects.get().carryingLiftedObjectState == 1) {
            return LinkPoseTables.LIFTING_ANIMATION_STATE[
                    linkAnimationState.get().direction.javaIndex()][linkAnimationState.get().walkFrame];
        }
        int override = queryOverride(linkEquipment.get().itemInSlotA());
        if (override >= 0) {
            return override;
        }
        override = queryOverride(linkEquipment.get().itemInSlotB());
        if (override >= 0) {
            return override;
        }
        if (linkAirborne.get().elevation().airborne()
                && !linkAirborne.get().spinningPitWarpArrival()
                && jumpAnimationFrame < 3
                && !linkAirborne.get().swordSuppressesJumpAnimation()) {
            return LinkPoseTables.JUMP_ANIMATION_STATE[
                    linkAnimationState.get().direction.javaIndex()][jumpAnimationFrame];
        }
        if (pushingAnimationState >= 0) return pushingAnimationState;
        if (linkServices.get().spriteSheet != null) {
            int carriedShield = !linkStateMachine.get().status().gelClinging()
                            && linkServices.get().playerState != null
                            && (linkServices.get().playerState.inventory().itemA() == InventoryItem.SHIELD.romValue()
                                    || linkServices
                                                    .get()
                                                    .playerState
                                                    .inventory()
                                                    .itemB()
                                            == InventoryItem.SHIELD.romValue())
                    ? linkServices.get().playerState.equipmentLevels().shieldLevel()
                    : 0;
            return linkServices
                    .get()
                    .spriteSheet
                    .walkingAnimationState(
                            linkAnimationState.get().direction.romIndex(),
                            normalFrame,
                            carriedShield,
                            linkEquipment.get().isUsingShield());
        }
        if (linkEquipment.get().isUsingShield()
                && linkServices.get().playerState != null
                && linkServices.get().playerState.equipmentLevels().shieldLevel() > 0) {
            int[][] shieldTable =
                    linkServices.get().playerState.equipmentLevels().shieldLevel() >= 2
                            ? LinkPoseTables.MIRROR_SHIELD_USE_ANIMATION_STATE
                            : LinkPoseTables.SHIELD_USE_ANIMATION_STATE;
            return shieldTable[linkAnimationState.get().direction.javaIndex()][normalFrame];
        }
        return baseState;
    }

    /** $02:42C7 selects walking poses before this frame's movement/collisions. */
    void cachePushingAnimation() {
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
                                    linkAnimationState.get().direction.romIndex(), frame)
                    : LinkPoseTables.UNSHIELDED_WALK_BASE[
                                    linkAnimationState.get().direction.javaIndex()]
                            + frame;
        }
    }

    int queryOverride(EquipmentItem item) {
        if (item == null) {
            return -1;
        }
        return item.overrideAnimationState(
                linkAnimationState.get().direction.javaIndex(), linkAnimationState.get().walkFrame);
    }

    /** Applies the ROM's explicit hLinkAnimationState=$6A fallen-pose write. */
    void showRomFallenPose() {
        linkStateMachine.get().status().romInteractiveMotionBlocked(true);
        romAnimationStateOverride = 0x6A;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Applies HoldEntityAboveLink's got-item pose and interaction lock. */
    void showHeldItemPose() {
        linkStateMachine.get().status().romInteractiveMotionBlocked(true);
        romAnimationStateOverride = 0x6C;
        linkAnimationState.get().direction = Direction.DOWN;
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** HandleGotItemB's power-up pose, distinct from lifting a physical object. */
    void showPowerUpPickupPose() {
        linkStateMachine.get().status().romInteractiveMotionBlocked(true);
        romAnimationStateOverride = 0x6B;
        linkDamage.get().collisionIgnoreFramesRemaining = 0;
        linkMotion.get().forcedSpeedPending = false;
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Applies TarinShield2Handler's shield-specific got-item presentation. */
    void showTarinShieldPresentation(int[] palette) {
        if (palette == null || palette.length < 4) {
            throw new IllegalArgumentException("Tarin shield palette must contain four colors");
        }
        showHeldItemPose();
        tarinShieldPresentation = true;
        tarinShieldPalette = palette.clone();
    }

    void clearTarinShieldPresentation() {
        tarinShieldPresentation = false;
    }

    /** Mirrors TarinShield2Handler's final LINK_ANIMATION_STATE_STANDING_SHIELD_DOWN write. */
    void showStandingShieldDownPose() {
        tarinShieldPresentation = false;
        romAnimationStateOverride = 0x22;
        linkStateMachine.get().status().romInteractiveMotionBlocked(false);
        linkAnimationState.get().direction = Direction.DOWN;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Applies one ROM spin sector while obtaining the beach sword. */
    void showSwordAcquisitionSpinPose(int countdown) {
        if (countdown < 0 || countdown > 0x20) {
            throw new IllegalArgumentException("Sword-spin countdown must be between 0 and 32");
        }
        if (linkServices.get().linkMotionTables == null) {
            throw new IllegalStateException("Sword acquisition spin requires ROM tables");
        }
        if (swordAcquisitionBaseRomDirection < 0) {
            swordAcquisitionBaseRomDirection =
                    linkAnimationState.get().direction.romIndex();
            swordAcquisitionAnimationState = resolveAnimationState();
        }
        // The spin handler writes a fresh hLinkAnimationState. Do not let a
        // preceding generic motion-block request leave its $6A butt pose in
        // front of this ROM-driven sword pose.
        romAnimationStateOverride = -1;
        int sector = Math.min(7, countdown >> 2);
        int swordState =
                linkServices.get().swordTables.swordSpinAnimationState(swordAcquisitionBaseRomDirection, sector);
        int absoluteRomDirection =
                linkServices.get().swordTables.swordSpinAbsoluteDirection(swordAcquisitionBaseRomDirection, sector);
        int animationState = linkServices.get().swordTables.swordDirectionAnimState(absoluteRomDirection, swordState);
        // UpdateSpinAttackAnimation leaves hLinkAnimationState unchanged for
        // LINK_ANIMATION_STATE_HIDDEN, so retain the previous ROM pose.
        if (animationState != 0xFF) {
            swordAcquisitionAnimationState = animationState;
        }
        linkStateMachine.get().status().romInteractiveMotionBlocked(true);
        linkAnimationState.get().direction = Direction.fromRom(absoluteRomDirection);
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Mirrors the sword handler's final hLinkAnimationState=$6B write. */
    void showSwordAcquisitionFinalPose() {
        linkStateMachine.get().status().romInteractiveMotionBlocked(true);
        swordAcquisitionBaseRomDirection = -1;
        swordAcquisitionAnimationState = -1;
        romAnimationStateOverride = 0x6B;
        linkAnimationState.get().direction = Direction.DOWN;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Hides ordinary Link OAM and selects Marin's dedicated bed display list. */
    void showMarinWakeUpBed(int spriteVariant) {
        if (spriteVariant < 0 || spriteVariant > 4) {
            throw new IllegalArgumentException("Bed sprite variant must be between 0 and 4");
        }
        marinWakeUpBedVariant = spriteVariant;
    }

    int renderY() {
        return ledgeJumpRenderY != Integer.MIN_VALUE
                ? ledgeJumpRenderY
                : linkMotion.get().accessPixelY()
                        - (bedJumpRenderZ >= 0
                                ? bedJumpRenderZ
                                : linkAirborne.get().accessZPixels());
    }

    boolean accessIsTarinShieldPresentationVisible() {
        return this.tarinShieldPresentation;
    }

    int[] accessTarinShieldPalette() {
        return this.tarinShieldPalette == null ? null : this.tarinShieldPalette.clone();
    }

    boolean accessIsMarinWakeUpBedVisible() {
        return this.marinWakeUpBedVariant >= 0;
    }

    int accessMarinWakeUpBedVariant() {
        return this.marinWakeUpBedVariant;
    }

    boolean accessDialogGotItemActive() {
        return dialogGotItemSequence.get().item() != 0;
    }

    int accessDialogGotItem() {
        return dialogGotItemSequence.get().item();
    }

    int accessDialogGotItemCountdown() {
        return dialogGotItemSequence.get().countdown();
    }

    int accessPresentedDialogGotItem() {
        return dialogGotItemSequence.get().presentedItem();
    }

    void accessPresentToadstool() {
        if (linkAnimationState.get().accessRomEntityZ() != 0) return;
        dialogGotItemSequence.get().start(2, 0x2A);
    }

    void accessClearDialogGotItem() {
        dialogGotItemSequence.get().clear();
    }

    boolean tarinShieldPresentation() {
        return tarinShieldPresentation;
    }

    boolean tarinShieldPresentation(boolean value) {
        return tarinShieldPresentation = value;
    }

    int[] tarinShieldPalette() {
        return tarinShieldPalette;
    }

    int[] tarinShieldPalette(int[] value) {
        return tarinShieldPalette = value;
    }

    int romAnimationStateOverride() {
        return romAnimationStateOverride;
    }

    int romAnimationStateOverride(int value) {
        return romAnimationStateOverride = value;
    }

    int swordAcquisitionBaseRomDirection() {
        return swordAcquisitionBaseRomDirection;
    }

    int swordAcquisitionBaseRomDirection(int value) {
        return swordAcquisitionBaseRomDirection = value;
    }

    int swordAcquisitionAnimationState() {
        return swordAcquisitionAnimationState;
    }

    int swordAcquisitionAnimationState(int value) {
        return swordAcquisitionAnimationState = value;
    }

    int marinWakeUpBedVariant() {
        return marinWakeUpBedVariant;
    }

    int marinWakeUpBedVariant(int value) {
        return marinWakeUpBedVariant = value;
    }

    int bedJumpRenderZ() {
        return bedJumpRenderZ;
    }

    int bedJumpRenderZ(int value) {
        return bedJumpRenderZ = value;
    }

    int ledgeJumpRenderY() {
        return ledgeJumpRenderY;
    }

    int ledgeJumpRenderY(int value) {
        return ledgeJumpRenderY = value;
    }

    int jumpAnimationCounter() {
        return jumpAnimationCounter;
    }

    int jumpAnimationCounter(int value) {
        return jumpAnimationCounter = value;
    }

    int jumpAnimationFrame() {
        return jumpAnimationFrame;
    }

    int jumpAnimationFrame(int value) {
        return jumpAnimationFrame = value;
    }

    int pushingAnimationState() {
        return pushingAnimationState;
    }

    int pushingAnimationState(int value) {
        return pushingAnimationState = value;
    }

    void render(byte[] displayBuffer, int offsetX, int offsetY) {
        if (linkEntityEffects.get().likeLikeCaptured) return;
        int state = resolveAnimationState();
        if (state == 0xFF) return;
        sprites.render(displayBuffer, offsetX, offsetY, state, renderY(), swordAcquisitionAnimationState >= 0);
    }

    void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        if (linkEntityEffects.get().likeLikeCaptured) return;
        int state = resolveAnimationState();
        if (state == 0xFF) return;
        sprites.submitOam(sink, offsetX, offsetY, state, renderY(), swordAcquisitionAnimationState >= 0);
    }
}
