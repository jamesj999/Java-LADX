package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;
import linksawakening.state.Tunic;

/** ROM pose selection and sprite data for both rendering paths. */
final class LinkAnimator {
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
    }

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

    static final int[][] ANIMATION_STATE = {
        {0x00, 0x01}, // DOWN:  standing, walking
        {0x04, 0x05}, // UP
        {0x06, 0x07}, // LEFT
        {0x0A, 0x0B}, // RIGHT
    };

    static final int[][] SHIELD_USE_ANIMATION_STATE = {
        {0x24, 0x25}, // DOWN
        {0x30, 0x31}, // UP
        {0x28, 0x29}, // LEFT
        {0x2A, 0x2B}, // RIGHT
    };

    static final int[][] MIRROR_SHIELD_USE_ANIMATION_STATE = {
        {0x26, 0x27}, // DOWN
        {0x32, 0x33}, // UP
        {0x28, 0x29}, // LEFT
        {0x2E, 0x2F}, // RIGHT
    };

    static final int[][] LIFTING_ANIMATION_STATE = {
        {0x44, 0x45}, // DOWN
        {0x42, 0x43}, // UP
        {0x40, 0x41}, // LEFT
        {0x3E, 0x3F}, // RIGHT
    };

    static final int[][] SWIMMING_ANIMATION_STATE = {
        {0x4C, 0x4D}, // DOWN
        {0x4A, 0x4B}, // UP
        {0x48, 0x49}, // LEFT
        {0x46, 0x47}, // RIGHT
    };

    static final int[] DIVING_ANIMATION_STATE = {0x4E, 0x4F};

    static final int[][] JUMP_ANIMATION_STATE = {
        {0x64, 0x65, 0x66}, // DOWN
        {0x67, 0x68, 0x69}, // UP
        {0x5E, 0x5F, 0x60}, // LEFT
        {0x61, 0x62, 0x63}, // RIGHT
    };

    static final int[] FALL_ANIMATION_STATE = {0x55, 0x56, 0x57, 0x57};

    final Tile[] composedTiles = new Tile[4];
    boolean tarinShieldPresentation;
    int[] tarinShieldPalette;
    int romAnimationStateOverride = -1;
    int swordAcquisitionBaseRomDirection = -1;
    int swordAcquisitionAnimationState = -1;
    int marinWakeUpBedVariant = -1;
    int bedJumpRenderZ = -1;
    /** DrawLinkSprite uses wC145, cached before ledge motion ($00:1033). */
    int ledgeJumpRenderY = Integer.MIN_VALUE;

    int jumpAnimationCounter;
    int jumpAnimationFrame;

    void render(byte[] displayBuffer, int offsetX, int offsetY) {
        if (linkEntityEffects.get().likeLikeCaptured) {
            return;
        }
        int animationState = resolveAnimationState();
        if (animationState == 0xFF) return;
        linkServices.get().spriteSheet.resolveTiles(animationState, composedTiles);

        boolean leftFlipX = linkServices.get().spriteSheet.leftHalfFlipX(animationState);
        boolean leftFlipY = linkServices.get().spriteSheet.leftHalfFlipY(animationState);
        boolean rightFlipX = linkServices.get().spriteSheet.rightHalfFlipX(animationState);
        boolean rightFlipY = linkServices.get().spriteSheet.rightHalfFlipY(animationState);

        int originX = linkMotion.get().pixelX() + offsetX;
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

        drawTile(displayBuffer, leftTop, originX, originY, leftFlipX, leftFlipY, palette);
        drawTile(displayBuffer, leftBottom, originX, originY + 8, leftFlipX, leftFlipY, palette);
        drawTile(displayBuffer, rightTop, originX + 8, originY, rightFlipX, rightFlipY, palette);
        drawTile(displayBuffer, rightBottom, originX + 8, originY + 8, rightFlipX, rightFlipY, palette);

        // Let equipped items paint their own sprites on top (e.g. the sword
        // blade during a swing). Matches the disassembly's ordering where
        // DrawLinkSprite writes Link's body to wLinkOAMBuffer slots 1-2 and
        // ApplyLinkMotionState/func_020_4AB3 writes the sword into slots 4-5.
        renderEquippedItems(displayBuffer, offsetX, offsetY);
    }

    void renderEquippedItems(byte[] displayBuffer, int offsetX, int offsetY) {
        if (linkServices.get().itemRegistry == null || linkServices.get().playerState == null) {
            return;
        }
        EquipmentItem a = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.itemA());
        if (a != null) {
            a.render(
                    displayBuffer,
                    linkMotion.get().pixelX(),
                    linkMotion.get().pixelY() - linkAirborne.get().accessZPixels(),
                    linkAnimationState.get().direction,
                    offsetX,
                    offsetY);
        }
        EquipmentItem b = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.itemB());
        if (b != null && b != a) {
            b.render(
                    displayBuffer,
                    linkMotion.get().pixelX(),
                    linkMotion.get().pixelY() - linkAirborne.get().accessZPixels(),
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
                acquisitionSword.render(
                        displayBuffer,
                        linkMotion.get().pixelX(),
                        linkMotion.get().pixelY() - linkAirborne.get().accessZPixels(),
                        linkAnimationState.get().direction,
                        offsetX,
                        offsetY);
            }
        }
    }

    static void drawTile(
            byte[] buffer, Tile tile, int screenX, int screenY, boolean flipX, boolean flipY, int[] palette) {
        if (tile == null) {
            return;
        }
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = screenX + tx;
                int py = screenY + ty;
                if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                    continue;
                }
                int sx = flipX ? 7 - tx : tx;
                int sy = flipY ? 7 - ty : ty;
                int colorIndex = tile.getPixel(sx, sy);
                if (colorIndex == 0) {
                    continue; // transparent
                }
                int color = palette[colorIndex];
                int offset = (py * Framebuffer.WIDTH + px) * 4;
                buffer[offset] = (byte) ((color >> 16) & 0xFF);
                buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
                buffer[offset + 2] = (byte) (color & 0xFF);
                buffer[offset + 3] = (byte) 0xFF;
            }
        }
    }

    int resolveAnimationState() {
        if (romAnimationStateOverride >= 0) {
            return romAnimationStateOverride;
        }
        if (swordAcquisitionAnimationState >= 0) {
            return swordAcquisitionAnimationState;
        }
        if (linkAirborne.get().fallingIntoPit) {
            int frame = Math.min(
                    LinkAnimator.FALL_ANIMATION_STATE.length - 1,
                    linkAirborne.get().fallingFrameCounter / LinkRomConstants.FALL_FRAME_TICKS);
            return LinkAnimator.FALL_ANIMATION_STATE[frame];
        }

        if (linkStateMachine.get().motionState == LinkMotionState.SWIMMING) {
            return linkStateMachine.get().physicsModifier != 0
                    ? LinkAnimator.DIVING_ANIMATION_STATE[linkAnimationState.get().walkFrame]
                    : LinkAnimator.SWIMMING_ANIMATION_STATE[linkAnimationState.get().direction][
                            linkAnimationState.get().walkFrame];
        }

        if (linkEntityEffects.get().grabbingObject) {
            return 0x36
                    + 2 * Direction.fromJava(linkAnimationState.get().direction).romIndex()
                    + (linkEntityEffects.get().pullingObject ? 1 : 0);
        }
        // func_002_4338 copies the intermediate ROM carry animation state
        // directly (for example $37/$39/$3B/$3D). The final carry state is
        // the regular two-frame lifting list below.
        if (linkEntityEffects.get().carryingLiftedObjectState >= 2) {
            return linkEntityEffects.get().carryingLiftedObjectState;
        }
        // ApplyLinkGroundMotion ($02:4503) sets the step counter to $FF
        // while linkAirborne.get().airborne, selecting the alternate normal pose after the flip.
        int normalFrame = linkAirborne.get().airborne
                        || (ledgeJumpRenderY != Integer.MIN_VALUE
                                && linkAnimationState.get().romConsecutiveStepsCount == 0xFF)
                ? 1
                : linkAnimationState.get().walkFrame;
        int baseState = LinkAnimator.ANIMATION_STATE[linkAnimationState.get().direction][normalFrame];
        if (linkEntityEffects.get().carryingLiftedObjectState == 1) {
            return LinkAnimator.LIFTING_ANIMATION_STATE[linkAnimationState.get().direction][
                    linkAnimationState.get().walkFrame];
        }
        int override = queryOverride(linkEquipment.get().itemInSlotA());
        if (override >= 0) {
            return override;
        }
        override = queryOverride(linkEquipment.get().itemInSlotB());
        if (override >= 0) {
            return override;
        }
        if (linkAirborne.get().airborne
                && !linkAirborne.get().spinningPitWarpArrival
                && jumpAnimationFrame < 3
                && !linkAirborne.get().swordSuppressesJumpAnimation()) {
            return LinkAnimator.JUMP_ANIMATION_STATE[linkAnimationState.get().direction][jumpAnimationFrame];
        }
        if (pushingAnimationState >= 0) return pushingAnimationState;
        if (linkServices.get().spriteSheet != null) {
            int carriedShield = !linkStateMachine.get().gelClinging
                            && linkServices.get().playerState != null
                            && (linkServices.get().playerState.itemA() == InventoryItem.SHIELD.romValue()
                                    || linkServices.get().playerState.itemB() == InventoryItem.SHIELD.romValue())
                    ? linkServices.get().playerState.shieldLevel()
                    : 0;
            return linkServices
                    .get()
                    .spriteSheet
                    .walkingAnimationState(
                            Direction.fromJava(linkAnimationState.get().direction)
                                    .romIndex(),
                            normalFrame,
                            carriedShield,
                            linkEquipment.get().isUsingShield());
        }
        if (linkEquipment.get().isUsingShield()
                && linkServices.get().playerState != null
                && linkServices.get().playerState.shieldLevel() > 0) {
            int[][] shieldTable = linkServices.get().playerState.shieldLevel() >= 2
                    ? LinkAnimator.MIRROR_SHIELD_USE_ANIMATION_STATE
                    : LinkAnimator.SHIELD_USE_ANIMATION_STATE;
            return shieldTable[linkAnimationState.get().direction][normalFrame];
        }
        return baseState;
    }

    int pushingAnimationState = -1;

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
                                    Direction.fromJava(linkAnimationState.get().direction)
                                            .romIndex(),
                                    frame)
                    : new int[] {0x1A, 0x1C, 0x1E, 0x20}[linkAnimationState.get().direction] + frame;
        }
    }

    int queryOverride(EquipmentItem item) {
        if (item == null) {
            return -1;
        }
        return item.overrideAnimationState(linkAnimationState.get().direction, linkAnimationState.get().walkFrame);
    }

    /** Applies the ROM's explicit hLinkAnimationState=$6A fallen-pose write. */
    void showRomFallenPose() {
        linkStateMachine.get().romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6A;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Applies HoldEntityAboveLink's got-item pose and interaction lock. */
    void showHeldItemPose() {
        linkStateMachine.get().romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6C;
        linkAnimationState.get().direction = Direction.DOWN.javaIndex();
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkAnimationState.get().movingThisFrame = false;
    }

    /** HandleGotItemB's power-up pose, distinct from lifting a physical object. */
    void showPowerUpPickupPose() {
        linkStateMachine.get().romInteractiveMotionBlocked = true;
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
        linkStateMachine.get().romInteractiveMotionBlocked = false;
        linkAnimationState.get().direction = Direction.DOWN.javaIndex();
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Applies one ROM spin sector while obtaining the beach sword. */
    void showSwordAcquisitionSpinPose(int countdown) {
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
    void showSwordAcquisitionFinalPose() {
        linkStateMachine.get().romInteractiveMotionBlocked = true;
        swordAcquisitionBaseRomDirection = -1;
        swordAcquisitionAnimationState = -1;
        romAnimationStateOverride = 0x6B;
        linkAnimationState.get().direction = Direction.DOWN.javaIndex();
        linkAnimationState.get().movingThisFrame = false;
    }

    /** Hides ordinary Link OAM and selects Marin's dedicated bed display list. */
    void showMarinWakeUpBed(int spriteVariant) {
        if (spriteVariant < 0 || spriteVariant > 4) {
            throw new IllegalArgumentException("Bed sprite variant must be between 0 and 4");
        }
        marinWakeUpBedVariant = spriteVariant;
    }

    void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
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

    void submitEquippedOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
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

    private int renderY() {
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

    int accessRomAnimationState() {
        return this.resolveAnimationState();
    }

    void accessShowRomFallenPose() {
        this.showRomFallenPose();
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

    void accessShowHeldItemPose() {
        this.showHeldItemPose();
    }

    void accessShowPowerUpPickupPose() {
        this.showPowerUpPickupPose();
    }

    void accessShowTarinShieldPresentation(int[] palette) {
        this.showTarinShieldPresentation(palette);
    }

    void accessClearTarinShieldPresentation() {
        this.clearTarinShieldPresentation();
    }

    void accessShowStandingShieldDownPose() {
        this.showStandingShieldDownPose();
    }

    void accessShowSwordAcquisitionSpinPose(int countdown) {
        this.showSwordAcquisitionSpinPose(countdown);
    }

    void accessShowSwordAcquisitionFinalPose() {
        this.showSwordAcquisitionFinalPose();
    }

    void accessShowMarinWakeUpBed(int spriteVariant) {
        this.showMarinWakeUpBed(spriteVariant);
    }

    void accessRender(byte[] displayBuffer, int offsetX, int offsetY) {
        this.render(displayBuffer, offsetX, offsetY);
    }

    void accessSubmitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        this.submitOam(sink, offsetX, offsetY);
    }
}
